import 'server-only';
import { db, unwrap } from '../db';
import { audit, requirePermission } from '../auth';
import { BusinessRuleError, DuplicateRecordError, NotFoundError } from '../errors';
import type { Employee } from '../types';
import type { SessionUser } from '../session';
import { dateOnly, errors, money, str } from '../validation';
import { likeFilter, nowIso } from './common';

const TABLE = 'employees';

type Input = Record<string, unknown>;

function mapInput(input: Input) {
  return {
    first_name: str(input.first_name) ?? '',
    last_name: str(input.last_name) ?? '',
    phone: str(input.phone),
    email: str(input.email)?.toLowerCase() ?? null,
    address: str(input.address),
    department: str(input.department) ?? '',
    position: str(input.position),
    date_of_birth: dateOnly(input.date_of_birth),
    hire_date: dateOnly(input.hire_date) ?? new Date().toISOString().slice(0, 10),
    salary: input.salary === null || input.salary === undefined || input.salary === '' ? null : money(input.salary),
    status: str(input.status) ?? 'ACTIVE',
    emergency_contact_name: str(input.emergency_contact_name),
    emergency_contact_phone: str(input.emergency_contact_phone),
    notes: str(input.notes),
  };
}

function validate(e: ReturnType<typeof mapInput>): void {
  errors()
    .required(e.first_name, 'First name')
    .required(e.last_name, 'Last name')
    .required(e.department, 'Department')
    .required(e.hire_date, 'Hire date')
    .email(e.email)
    .maxLength(e.notes, 2000, 'Notes')
    .nonNegative(e.salary, 'Salary')
    .check(['ACTIVE', 'ON_LEAVE', 'SUSPENDED', 'TERMINATED'].includes(e.status), 'Invalid employment status.')
    .throwIfAny();
}

export async function listEmployees(filters: { q?: string; department?: string; status?: string; includeDeleted?: boolean } = {}): Promise<Employee[]> {
  let query = db().from(TABLE).select('*').order('last_name').order('first_name');
  if (!filters.includeDeleted) query = query.is('deleted_at', null);
  if (filters.q?.trim()) query = query.or(likeFilter(filters.q.trim(), ['first_name', 'last_name', 'employee_code', 'email', 'phone', 'department', 'position']));
  if (filters.department) query = query.eq('department', filters.department);
  if (filters.status) query = query.eq('status', filters.status);
  return unwrap<Employee[]>(await query, 'list employees');
}

export async function getEmployee(id: number): Promise<Employee> {
  const { data, error } = await db().from(TABLE).select('*').eq('id', id).maybeSingle();
  if (error) throw new Error(error.message);
  if (!data) throw new NotFoundError('Employee', id);
  return data as Employee;
}

export async function createEmployee(session: SessionUser | null, input: Input): Promise<Employee> {
  requirePermission(session, 'MANAGE_EMPLOYEES');
  const e = mapInput(input);
  validate(e);
  if (e.email) {
    const clash = await db().from(TABLE).select('id').ilike('email', e.email).is('deleted_at', null).maybeSingle();
    if (clash.data) throw new DuplicateRecordError(`E-mail ${e.email} is already used by another employee.`);
  }
  const { data: code } = await db().rpc('next_code', { p_prefix: 'EMP' });
  const { data, error } = await db().from(TABLE).insert({ ...e, employee_code: (code as string) ?? `EMP-${Date.now()}` }).select('*').single();
  if (error) throw error;
  const created = data as Employee;
  await audit(session?.username, 'CREATE', 'EMPLOYEE', created.employee_code, `Added employee ${e.first_name} ${e.last_name} (${e.department})`);
  return created;
}

export async function updateEmployee(session: SessionUser | null, id: number, input: Input): Promise<Employee> {
  requirePermission(session, 'MANAGE_EMPLOYEES');
  const existing = await getEmployee(id);
  if (existing.deleted_at) throw new BusinessRuleError('Restore this employee from the recycle bin before editing.');
  const e = mapInput({ ...existing, ...input });
  validate(e);
  if (e.email) {
    const clash = await db().from(TABLE).select('id').ilike('email', e.email).neq('id', id).is('deleted_at', null).maybeSingle();
    if (clash.data) throw new DuplicateRecordError(`E-mail ${e.email} is already used by another employee.`);
  }
  const { data, error } = await db().from(TABLE).update({ ...e, employee_code: existing.employee_code }).eq('id', id).select('*').single();
  if (error) throw error;
  await audit(session?.username, 'UPDATE', 'EMPLOYEE', existing.employee_code, `Updated employee ${e.first_name} ${e.last_name}`);
  return data as Employee;
}

export async function deleteEmployee(session: SessionUser | null, id: number): Promise<void> {
  requirePermission(session, 'MANAGE_EMPLOYEES');
  const existing = await getEmployee(id);
  const { data: driver } = await db().from('drivers').select('id, driver_code').eq('employee_id', id).is('deleted_at', null).maybeSingle();
  if (driver) throw new BusinessRuleError(`This employee has driver profile ${(driver as { driver_code: string }).driver_code}. Remove the driver profile first.`);
  await db().from(TABLE).update({ deleted_at: nowIso() }).eq('id', id);
  await audit(session?.username, 'DELETE', 'EMPLOYEE', existing.employee_code, `Moved employee ${existing.first_name} ${existing.last_name} to recycle bin`);
}

export async function restoreEmployee(session: SessionUser | null, id: number): Promise<void> {
  requirePermission(session, 'MANAGE_EMPLOYEES');
  const existing = await getEmployee(id);
  await db().from(TABLE).update({ deleted_at: null }).eq('id', id);
  await audit(session?.username, 'RESTORE', 'EMPLOYEE', existing.employee_code, 'Restored employee from recycle bin');
}

export async function employeeStatistics(): Promise<{
  byDepartment: { department: string; count: number }[];
  byStatus: { status: string; count: number }[];
  total: number;
  active: number;
  monthlyPayroll: number;
}> {
  const rows = unwrap<{ department: string; status: string; salary: number | null }[]>(
    await db().from(TABLE).select('department, status, salary').is('deleted_at', null), 'employee statistics');
  const dept = new Map<string, number>();
  const status = new Map<string, number>();
  let payroll = 0;
  for (const r of rows) {
    dept.set(r.department, (dept.get(r.department) ?? 0) + 1);
    status.set(r.status, (status.get(r.status) ?? 0) + 1);
    if (r.status === 'ACTIVE') payroll += Number(r.salary ?? 0);
  }
  return {
    byDepartment: [...dept.entries()].map(([department, count]) => ({ department, count })).sort((a, b) => b.count - a.count),
    byStatus: [...status.entries()].map(([s, count]) => ({ status: s, count })),
    total: rows.length,
    active: rows.filter((r) => r.status === 'ACTIVE').length,
    monthlyPayroll: payroll,
  };
}

export async function departments(): Promise<string[]> {
  const rows = unwrap<{ department: string }[]>(await db().from(TABLE).select('department').is('deleted_at', null), 'departments');
  return [...new Set(rows.map((r) => r.department))].sort();
}
