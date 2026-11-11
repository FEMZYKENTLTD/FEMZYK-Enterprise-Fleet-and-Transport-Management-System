import 'server-only';
import { db, unwrap } from '../db';
import { audit, requirePermission } from '../auth';
import { BusinessRuleError, DuplicateRecordError, NotFoundError } from '../errors';
import type { Driver } from '../types';
import type { SessionUser } from '../session';
import { dateOnly, errors, str } from '../validation';
import { addDays, likeFilter, nowIso, todayIso } from './common';

const TABLE = 'drivers';

type Input = Record<string, unknown>;
type DriverJoinRow = Omit<Driver, 'employee_full_name' | 'employee_email' | 'employee_phone' | 'employee_department'> & {
  employees: { first_name: string; last_name: string; email: string | null; phone: string | null; department: string } | null;
};

function mapJoin(row: DriverJoinRow): Driver {
  const e = row.employees;
  const { employees: _e, ...rest } = row;
  return {
    ...rest,
    employee_full_name: e ? `${e.first_name} ${e.last_name}`.trim() : '—',
    employee_email: e?.email ?? null,
    employee_phone: e?.phone ?? null,
    employee_department: e?.department ?? '—',
  };
}

function mapInput(input: Input) {
  return {
    employee_id: Number(input.employee_id) || 0,
    licence_number: str(input.licence_number)?.toUpperCase() ?? '',
    licence_category: str(input.licence_category) ?? '',
    licence_issue_date: dateOnly(input.licence_issue_date),
    licence_expiry_date: dateOnly(input.licence_expiry_date) ?? '',
    status: str(input.status) ?? 'ACTIVE',
    notes: str(input.notes),
  };
}

function validate(d: ReturnType<typeof mapInput>): void {
  errors()
    .check(d.employee_id > 0, 'Employee is required.')
    .required(d.licence_number, 'Licence number')
    .check(['A', 'B', 'C', 'D', 'E'].includes(d.licence_category), 'Licence category must be A, B, C, D or E.')
    .required(d.licence_expiry_date, 'Licence expiry date')
    .check(['ACTIVE', 'ON_LEAVE', 'SUSPENDED', 'INACTIVE'].includes(d.status), 'Invalid driver status.')
    .maxLength(d.notes, 2000, 'Notes')
    .throwIfAny();
}

export async function listDrivers(filters: { q?: string; status?: string; includeDeleted?: boolean } = {}): Promise<Driver[]> {
  let query = db().from(TABLE)
    .select('*, employees(first_name, last_name, email, phone, department)')
    .order('driver_code');
  if (!filters.includeDeleted) query = query.is('deleted_at', null);
  if (filters.status) query = query.eq('status', filters.status);
  const rows = unwrap<DriverJoinRow[]>(await query, 'list drivers');
  let drivers = rows.map(mapJoin);
  if (filters.q?.trim()) {
    const q = filters.q.trim().toLowerCase();
    drivers = drivers.filter((d) =>
      [d.driver_code, d.licence_number, d.employee_full_name, d.employee_email, d.licence_category]
        .some((v) => (v ?? '').toLowerCase().includes(q)),
    );
  }
  return drivers;
}

export async function getDriver(id: number): Promise<Driver> {
  const { data, error } = await db().from(TABLE).select('*, employees(first_name, last_name, email, phone, department)').eq('id', id).maybeSingle();
  if (error) throw new Error(error.message);
  if (!data) throw new NotFoundError('Driver', id);
  return mapJoin(data as DriverJoinRow);
}

export async function createDriver(session: SessionUser | null, input: Input): Promise<Driver> {
  requirePermission(session, 'MANAGE_DRIVERS');
  const d = mapInput(input);
  validate(d);
  const employee = await db().from('employees').select('id, first_name, last_name').eq('id', d.employee_id).is('deleted_at', null).maybeSingle();
  if (!employee.data) throw new BusinessRuleError('Select an existing employee for this driver profile.');
  const existing = await db().from(TABLE).select('id').eq('employee_id', d.employee_id).is('deleted_at', null).maybeSingle();
  if (existing.data) throw new DuplicateRecordError('That employee already has a driver profile.');
  const licenceClash = await db().from(TABLE).select('id').ilike('licence_number', d.licence_number).maybeSingle();
  if (licenceClash.data) throw new DuplicateRecordError(`Licence number ${d.licence_number} is already registered.`);
  const { data: code } = await db().rpc('next_code', { p_prefix: 'DRV' });
  const { data, error } = await db().from(TABLE).insert({ ...d, driver_code: (code as string) ?? `DRV-${Date.now()}` }).select('id').single();
  if (error) throw error;
  const created = await getDriver((data as { id: number }).id);
  await audit(session?.username, 'CREATE', 'DRIVER', created.driver_code, `Added driver ${created.employee_full_name} (licence ${d.licence_category})`);
  return created;
}

export async function updateDriver(session: SessionUser | null, id: number, input: Input): Promise<Driver> {
  requirePermission(session, 'MANAGE_DRIVERS');
  const existing = await getDriver(id);
  if (existing.deleted_at) throw new BusinessRuleError('Restore this driver from the recycle bin before editing.');
  const d = mapInput({ ...existing, ...input });
  validate(d);
  const licenceClash = await db().from(TABLE).select('id').ilike('licence_number', d.licence_number).neq('id', id).maybeSingle();
  if (licenceClash.data) throw new DuplicateRecordError(`Licence number ${d.licence_number} is already registered.`);
  const { error } = await db().from(TABLE).update({ ...d, employee_id: existing.employee_id }).eq('id', id);
  if (error) throw error;
  const updated = await getDriver(id);
  await audit(session?.username, 'UPDATE', 'DRIVER', updated.driver_code, `Updated driver ${updated.employee_full_name} (status ${d.status})`);
  return updated;
}

export async function deleteDriver(session: SessionUser | null, id: number): Promise<void> {
  requirePermission(session, 'MANAGE_DRIVERS');
  const existing = await getDriver(id);
  const { data: assignment } = await db().from('vehicle_assignments').select('id').eq('driver_id', id).eq('status', 'ACTIVE').maybeSingle();
  if (assignment) throw new BusinessRuleError('This driver has a vehicle assigned. Return the vehicle first.');
  const { data: trip } = await db().from('trips').select('id').eq('driver_id', id).eq('status', 'ACTIVE').maybeSingle();
  if (trip) throw new BusinessRuleError('This driver is on an active trip.');
  await db().from(TABLE).update({ deleted_at: nowIso() }).eq('id', id);
  await audit(session?.username, 'DELETE', 'DRIVER', existing.driver_code, `Moved driver ${existing.employee_full_name} to recycle bin`);
}

export async function restoreDriver(session: SessionUser | null, id: number): Promise<void> {
  requirePermission(session, 'MANAGE_DRIVERS');
  const existing = await getDriver(id);
  await db().from(TABLE).update({ deleted_at: null }).eq('id', id);
  await audit(session?.username, 'RESTORE', 'DRIVER', existing.driver_code, 'Restored driver from recycle bin');
}

/** Drivers entering the licence-expiry window (AppConfig.LICENCE_EXPIRY_WARNING_DAYS = 30). */
export function licenceAlerts(drivers: Driver[]) {
  const horizon = addDays(todayIso(), 30);
  return drivers
    .filter((d) => d.status === 'ACTIVE' && d.licence_expiry_date <= horizon)
    .map((d) => ({
      ...d,
      expired: d.licence_expiry_date < todayIso(),
    }))
    .sort((a, b) => a.licence_expiry_date.localeCompare(b.licence_expiry_date));
}

export function isLicenceExpired(driver: { licence_expiry_date: string }): boolean {
  return driver.licence_expiry_date < todayIso();
}
