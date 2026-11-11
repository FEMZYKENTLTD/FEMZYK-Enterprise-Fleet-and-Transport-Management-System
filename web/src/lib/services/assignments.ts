import 'server-only';
import { db, unwrap } from '../db';
import { audit, requirePermission } from '../auth';
import { BusinessRuleError, NotFoundError } from '../errors';
import { licencePermits, type LicenceCategory, type VehicleType } from '../domain';
import type { Assignment } from '../types';
import type { SessionUser } from '../session';
import { todayIso } from './common';

const TABLE = 'vehicle_assignments';

type AssignmentRow = Omit<Assignment, 'registration_number' | 'vehicle_display' | 'driver_name' | 'driver_code'> & {
  vehicles: { registration_number: string; make: string; model: string; year: number } | null;
  drivers: { driver_code: string; employees: { first_name: string; last_name: string } | null } | null;
};

export function mapAssignment(row: AssignmentRow): Assignment {
  const v = row.vehicles;
  const driver = row.drivers;
  const employee = driver?.employees;
  const { vehicles: _v, drivers: _d, ...rest } = row;
  return {
    ...rest,
    registration_number: v?.registration_number ?? '—',
    vehicle_display: v ? `${v.registration_number} (${v.year} ${v.make} ${v.model})` : '—',
    driver_name: employee ? `${employee.first_name} ${employee.last_name}`.trim() : '—',
    driver_code: driver?.driver_code ?? '—',
  };
}

export async function listAssignments(filters: { status?: string; q?: string; vehicleId?: number; driverId?: number } = {}): Promise<Assignment[]> {
  let query = db().from(TABLE)
    .select('*, vehicles(registration_number, make, model, year), drivers(driver_code, employees(first_name, last_name))')
    .order('assigned_at', { ascending: false })
    .limit(500);
  if (filters.status) query = query.eq('status', filters.status);
  if (filters.vehicleId) query = query.eq('vehicle_id', filters.vehicleId);
  if (filters.driverId) query = query.eq('driver_id', filters.driverId);
  const rows = unwrap<AssignmentRow[]>(await query, 'list assignments').map(mapAssignment);
  if (!filters.q?.trim()) return rows;
  const q = filters.q.trim().toLowerCase();
  return rows.filter((a) => [a.registration_number, a.driver_name, a.purpose, a.assigned_by].some((v) => (v ?? '').toLowerCase().includes(q)));
}

export async function getAssignment(id: number): Promise<Assignment> {
  const { data, error } = await db()
    .from(TABLE)
    .select('*, vehicles(registration_number, make, model, year), drivers(driver_code, employees(first_name, last_name))')
    .eq('id', id)
    .maybeSingle();
  if (error) throw new Error(error.message);
  if (!data) throw new NotFoundError('Assignment', id);
  return mapAssignment(data as AssignmentRow);
}

/**
 * Assigns a vehicle to a driver, enforcing the same rules as AssignmentService:
 * AVAILABLE vehicle, assignable driver, valid licence class, no duplicate
 * active assignment. The partial unique indexes back this up in the database.
 */
export async function assignVehicle(
  session: SessionUser | null,
  input: { vehicle_id: number; driver_id: number; purpose?: string; notes?: string; assigned_at?: string },
): Promise<Assignment> {
  requirePermission(session, 'MANAGE_ASSIGNMENTS');
  const { data: vehicleData, error: vErr } = await db()
    .from('vehicles').select('id, registration_number, vehicle_type, status, deleted_at').eq('id', input.vehicle_id).maybeSingle();
  if (vErr) throw new Error(vErr.message);
  if (!vehicleData) throw new NotFoundError('Vehicle', input.vehicle_id);
  const vehicle = vehicleData as { id: number; registration_number: string; vehicle_type: VehicleType; status: string; deleted_at: string | null };

  const { data: driverData, error: dErr } = await db()
    .from('drivers').select('id, status, licence_category, licence_expiry_date, deleted_at, employees(first_name, last_name)').eq('id', input.driver_id).maybeSingle();
  if (dErr) throw new Error(dErr.message);
  if (!driverData) throw new NotFoundError('Driver', input.driver_id);
  const driver = driverData as unknown as {
    id: number; status: string; licence_category: LicenceCategory; licence_expiry_date: string; deleted_at: string | null;
    employees: { first_name: string; last_name: string } | null;
  };
  const driverName = driver.employees ? `${driver.employees.first_name} ${driver.employees.last_name}`.trim() : 'this driver';

  if (vehicle.deleted_at) throw new BusinessRuleError('Vehicle is in the recycle bin.');
  if (driver.deleted_at) throw new BusinessRuleError('Driver is in the recycle bin.');
  if (vehicle.status !== 'AVAILABLE') {
    throw new BusinessRuleError(`Vehicle ${vehicle.registration_number} is ${vehicle.status.replaceAll('_', ' ')} and cannot be assigned. Only AVAILABLE vehicles can be assigned.`);
  }
  if (!['ACTIVE', 'ON_LEAVE'].includes(driver.status)) {
    throw new BusinessRuleError(`Driver ${driverName} is ${driver.status.replaceAll('_', ' ')} and cannot be assigned a vehicle.`);
  }
  if (driver.status !== 'ACTIVE') throw new BusinessRuleError(`Driver ${driverName} is not ACTIVE and cannot be assigned a vehicle.`);
  if (driver.licence_expiry_date < todayIso()) {
    throw new BusinessRuleError(`Driver ${driverName}'s licence expired on ${driver.licence_expiry_date}.`);
  }
  if (!licencePermits(driver.licence_category, vehicle.vehicle_type)) {
    throw new BusinessRuleError(`Licence class ${driver.licence_category} does not permit driving a ${vehicle.vehicle_type}.`);
  }
  const { data: activeVehicle } = await db().from(TABLE).select('id').eq('vehicle_id', input.vehicle_id).eq('status', 'ACTIVE').maybeSingle();
  if (activeVehicle) throw new BusinessRuleError('Vehicle is already assigned.');
  const { data: activeDriver } = await db().from(TABLE).select('id').eq('driver_id', input.driver_id).eq('status', 'ACTIVE').maybeSingle();
  if (activeDriver) throw new BusinessRuleError('Driver already has a vehicle assigned.');

  const { data, error } = await db()
    .from(TABLE)
    .insert({
      vehicle_id: input.vehicle_id,
      driver_id: input.driver_id,
      assigned_at: input.assigned_at ?? todayIso(),
      status: 'ACTIVE',
      purpose: input.purpose?.trim() || null,
      notes: input.notes?.trim() || null,
      assigned_by: session?.username ?? 'system',
    })
    .select('id')
    .single();
  if (error) throw error;
  await db().from('vehicles').update({ status: 'ASSIGNED' }).eq('id', input.vehicle_id);
  const created = await getAssignment((data as { id: number }).id);
  await audit(
    session?.username, 'ASSIGN', 'ASSIGNMENT', vehicle.registration_number,
    `Assigned ${vehicle.registration_number} to ${driverName}${created.purpose ? ` for ${created.purpose}` : ''}`,
  );
  return created;
}

export async function returnVehicle(session: SessionUser | null, assignmentId: number, notes?: string): Promise<Assignment> {
  requirePermission(session, 'MANAGE_ASSIGNMENTS');
  const assignment = await getAssignment(assignmentId);
  if (assignment.status !== 'ACTIVE') throw new BusinessRuleError(`This assignment is already ${assignment.status}.`);
  const combinedNotes = notes?.trim()
    ? `${assignment.notes ? `${assignment.notes}\n` : ''}${notes.trim()}`
    : assignment.notes;
  await db().from(TABLE).update({ status: 'RETURNED', returned_at: todayIso(), notes: combinedNotes }).eq('id', assignmentId);
  const { data: vehicle } = await db().from('vehicles').select('id, status').eq('id', assignment.vehicle_id).maybeSingle();
  if (vehicle && ['ASSIGNED', 'IN_SERVICE'].includes((vehicle as { status: string }).status)) {
    await db().from('vehicles').update({ status: 'AVAILABLE' }).eq('id', assignment.vehicle_id);
  }
  await audit(session?.username, 'RETURN', 'ASSIGNMENT', assignment.registration_number, `Vehicle returned by ${assignment.driver_name}`);
  return getAssignment(assignmentId);
}

export async function reassignVehicle(
  session: SessionUser | null,
  input: { vehicle_id: number; driver_id: number; purpose?: string; notes?: string },
): Promise<Assignment> {
  requirePermission(session, 'MANAGE_ASSIGNMENTS');
  const { data: active } = await db().from(TABLE).select('id').eq('vehicle_id', input.vehicle_id).eq('status', 'ACTIVE').maybeSingle();
  if (active) await returnVehicle(session, (active as { id: number }).id, 'Reassigned');
  return assignVehicle(session, input);
}

export async function cancelAssignment(session: SessionUser | null, assignmentId: number, reason: string): Promise<Assignment> {
  requirePermission(session, 'MANAGE_ASSIGNMENTS');
  const assignment = await getAssignment(assignmentId);
  if (assignment.status !== 'ACTIVE') throw new BusinessRuleError('Only active assignments can be cancelled.');
  const notes = `${assignment.notes ? `${assignment.notes}\n` : ''}Cancelled: ${reason ?? ''}`;
  await db().from(TABLE).update({ status: 'CANCELLED', returned_at: todayIso(), notes }).eq('id', assignmentId);
  const { data: vehicle } = await db().from('vehicles').select('status').eq('id', assignment.vehicle_id).maybeSingle();
  if (vehicle && (vehicle as { status: string }).status === 'ASSIGNED') {
    await db().from('vehicles').update({ status: 'AVAILABLE' }).eq('id', assignment.vehicle_id);
  }
  await audit(session?.username, 'CANCEL', 'ASSIGNMENT', assignment.vehicle_display, `Assignment cancelled: ${reason}`);
  return getAssignment(assignmentId);
}

export async function countActiveAssignments(): Promise<number> {
  const { count } = await db().from(TABLE).select('id', { count: 'exact', head: true }).eq('status', 'ACTIVE');
  return count ?? 0;
}
