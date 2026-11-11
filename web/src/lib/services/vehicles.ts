import 'server-only';
import { db, unwrap } from '../db';
import { audit, requirePermission } from '../auth';
import { BusinessRuleError, DuplicateRecordError, NotFoundError, ValidationError } from '../errors';
import { VEHICLE_TRANSITIONS, type FuelType, type VehicleStatus, type VehicleType } from '../domain';
import type { Vehicle } from '../types';
import type { SessionUser } from '../session';
import { dateOnly, errors, isValidRegistration, money, normaliseRegistration, num, str, int } from '../validation';
import { likeFilter, nowIso } from './common';

const TABLE = 'vehicles';

type VehicleInput = Record<string, unknown>;

function mapInput(input: VehicleInput): Omit<Vehicle, 'id' | 'created_at' | 'updated_at' | 'deleted_at' | 'assigned_driver_id' | 'assigned_driver_name'> {
  return {
    vehicle_code: str(input.vehicle_code) ?? '',
    registration_number: normaliseRegistration(String(input.registration_number ?? '')),
    vehicle_type: String(input.vehicle_type ?? '') as VehicleType,
    make: str(input.make) ?? '',
    model: str(input.model) ?? '',
    year: int(input.year) ?? 0,
    color: str(input.color),
    vin: str(input.vin)?.toUpperCase() ?? null,
    engine_number: str(input.engine_number),
    acquisition_date: dateOnly(input.acquisition_date),
    acquisition_cost: money(input.acquisition_cost),
    current_mileage: int(input.current_mileage) ?? 0,
    fuel_type: String(input.fuel_type ?? 'PETROL') as FuelType,
    capacity: int(input.capacity) ?? 0,
    status: (str(input.status) ?? 'AVAILABLE') as VehicleStatus,
    attribute_number: num(input.attribute_number),
    attribute_text: str(input.attribute_text)?.toUpperCase() ?? null,
    notes: str(input.notes),
  };
}

function validate(v: ReturnType<typeof mapInput>): void {
  const maxYear = new Date().getFullYear() + 1;
  const e = errors()
    .required(v.registration_number, 'Registration number')
    .check(isValidRegistration(v.registration_number), 'Registration number must be 3–15 characters (letters, digits, spaces, hyphens).')
    .required(v.make, 'Make')
    .required(v.model, 'Model')
    .range(v.year, 1950, maxYear, 'Year')
    .range(v.capacity, 0, 120, 'Capacity')
    .nonNegative(v.current_mileage, 'Current mileage')
    .nonNegative(v.acquisition_cost, 'Acquisition cost')
    .maxLength(v.notes, 2000, 'Notes');
  if (!['CAR', 'MOTORCYCLE', 'TRUCK', 'VAN', 'BUS'].includes(v.vehicle_type)) e.check(false, 'Vehicle type is required.');
  if (!['PETROL', 'DIESEL', 'ELECTRIC', 'HYBRID', 'CNG'].includes(v.fuel_type)) e.check(false, 'Fuel type is required.');
  if (v.vin && (v.vin.length < 5 || v.vin.length > 20)) e.check(false, 'VIN/chassis number must be 5–20 characters.');
  if (v.vehicle_type === 'CAR') e.check((v.attribute_number ?? 0) >= 2 && (v.attribute_number ?? 0) <= 6, 'A car must have between 2 and 6 doors.');
  if (v.vehicle_type === 'MOTORCYCLE') {
    e.check(v.attribute_number === 2 || v.attribute_number === 3, 'A motorcycle must have 2 or 3 wheels.');
    e.check(['SPORT', 'CRUISER', 'OFF_ROAD', 'STANDARD'].includes(v.attribute_text ?? ''), 'Motorcycle style must be SPORT, CRUISER, OFF_ROAD or STANDARD.');
  }
  if (v.vehicle_type === 'TRUCK') e.check((v.attribute_number ?? 0) > 0 && (v.attribute_number ?? 0) <= 60, 'Cargo capacity must be between 0 and 60 tons.');
  e.throwIfAny();
}

/** Vehicle list with the active driver joined on, like VehicleRepository.search(). */
export async function listVehicles(filters: { q?: string; type?: string; status?: string; fuel?: string; includeDeleted?: boolean } = {}): Promise<Vehicle[]> {
  let query = db().from(TABLE).select('*').order('registration_number');
  if (!filters.includeDeleted) query = query.is('deleted_at', null);
  if (filters.q?.trim()) query = query.or(likeFilter(filters.q.trim(), ['registration_number', 'make', 'model', 'vehicle_code', 'vin']));
  if (filters.type) query = query.eq('vehicle_type', filters.type);
  if (filters.status) query = query.eq('status', filters.status);
  if (filters.fuel) query = query.eq('fuel_type', filters.fuel);
  const vehicles = unwrap<Vehicle[]>(await query, 'list vehicles');

  const ids = vehicles.map((v) => v.id);
  if (ids.length) {
    const { data: activeData, error: activeError } = await db()
      .from('vehicle_assignments').select('vehicle_id, driver_id, drivers(employee_id)').eq('status', 'ACTIVE').in('vehicle_id', ids);
    if (activeError) throw new Error(`list vehicle assignments: ${activeError.message}`);
    const active = (activeData ?? []) as unknown as { vehicle_id: number; driver_id: number; drivers: { employee_id: number } | null }[];
    const employeeIds = active.map((a) => a.drivers?.employee_id).filter((x): x is number => typeof x === 'number');
    const employees = employeeIds.length
      ? unwrap<{ id: number; first_name: string; last_name: string }[]>(
          await db().from('employees').select('id, first_name, last_name').in('id', employeeIds), 'list employees')
      : [];
    const names = new Map(employees.map((e) => [e.id, `${e.first_name} ${e.last_name}`.trim()]));
    const byVehicle = new Map(active.map((a) => [a.vehicle_id, { driver_id: a.driver_id, name: names.get(a.drivers?.employee_id ?? -1) ?? null }]));
    for (const v of vehicles) {
      const match = byVehicle.get(v.id);
      v.assigned_driver_id = match?.driver_id ?? null;
      v.assigned_driver_name = match?.name ?? null;
    }
  }
  return vehicles;
}

export async function getVehicle(id: number): Promise<Vehicle> {
  const { data, error } = await db().from(TABLE).select('*').eq('id', id).maybeSingle();
  if (error) throw new Error(error.message);
  if (!data) throw new NotFoundError('Vehicle', id);
  return data as Vehicle;
}

export async function createVehicle(session: SessionUser | null, input: VehicleInput): Promise<Vehicle> {
  requirePermission(session, 'MANAGE_VEHICLES');
  const v = mapInput(input);
  validate(v);
  const existing = await db().from(TABLE).select('id, make, model, year').ilike('registration_number', v.registration_number).is('deleted_at', null).maybeSingle();
  if (existing.data) {
    const e = existing.data as { make: string; model: string; year: number };
    throw new DuplicateRecordError(`Registration number ${v.registration_number} is already used by ${e.year} ${e.make} ${e.model}.`);
  }
  if (v.status === 'ASSIGNED') v.status = 'AVAILABLE';
  const { data: code } = await db().rpc('next_code', { p_prefix: 'VEH' });
  v.vehicle_code = (code as string) ?? `VEH-${Date.now()}`;
  const { data, error } = await db().from(TABLE).insert(v).select('*').single();
  if (error) throw error;
  await audit(session?.username, 'CREATE', 'VEHICLE', v.registration_number, `Added ${v.vehicle_type} ${v.year} ${v.make} ${v.model}`);
  return data as Vehicle;
}

export async function updateVehicle(session: SessionUser | null, id: number, input: VehicleInput): Promise<Vehicle> {
  requirePermission(session, 'MANAGE_VEHICLES');
  const existing = await getVehicle(id);
  if (existing.deleted_at) throw new BusinessRuleError('Restore this vehicle from the recycle bin before editing it.');
  const v = mapInput({ ...existing, ...input });
  validate(v);
  const clash = await db().from(TABLE).select('id, make, model, year').ilike('registration_number', v.registration_number).neq('id', id).is('deleted_at', null).maybeSingle();
  if (clash.data) {
    const c = clash.data as { make: string; model: string; year: number };
    throw new DuplicateRecordError(`Registration number ${v.registration_number} is already used by ${c.year} ${c.make} ${c.model}.`);
  }
  if (v.current_mileage < Number(existing.current_mileage)) {
    throw new BusinessRuleError(
      `Current mileage (${v.current_mileage} km) cannot be lower than the recorded mileage (${existing.current_mileage} km).`,
    );
  }
  if (existing.status !== v.status) {
    await assertTransition(existing, v.status);
  }
  const { data, error } = await db()
    .from(TABLE)
    .update({ ...v, vehicle_code: existing.vehicle_code })
    .eq('id', id)
    .select('*')
    .single();
  if (error) throw error;
  await audit(
    session?.username, 'UPDATE', 'VEHICLE', v.registration_number,
    `Updated vehicle ${v.year} ${v.make} ${v.model}${existing.status !== v.status ? ` (status ${existing.status} → ${v.status})` : ''}`,
  );
  return data as Vehicle;
}

export async function assertTransition(vehicle: Vehicle, target: VehicleStatus): Promise<void> {
  const current = vehicle.status as VehicleStatus;
  if (current === target) return;
  if (!VEHICLE_TRANSITIONS[current]?.includes(target)) {
    throw new BusinessRuleError(`A vehicle cannot move from ${current.replaceAll('_', ' ')} to ${target.replaceAll('_', ' ')}.`);
  }
  const { data: activeAssignment } = await db()
    .from('vehicle_assignments').select('id').eq('vehicle_id', vehicle.id).eq('status', 'ACTIVE').maybeSingle();
  if (target === 'ASSIGNED' && !activeAssignment) {
    throw new BusinessRuleError("Use 'Assign vehicle' to put a vehicle into ASSIGNED status.");
  }
  if (['AVAILABLE', 'RETIRED', 'OUT_OF_SERVICE'].includes(target) && activeAssignment) {
    throw new BusinessRuleError(`Vehicle has an active assignment; return it before marking ${target.replaceAll('_', ' ')}.`);
  }
  const { data: activeTrip } = await db().from('trips').select('id').eq('vehicle_id', vehicle.id).eq('status', 'ACTIVE').maybeSingle();
  if (activeTrip && target !== 'IN_SERVICE') {
    throw new BusinessRuleError('Vehicle is on an active trip; complete or cancel the trip first.');
  }
}

export async function changeVehicleStatus(session: SessionUser | null, id: number, target: VehicleStatus, reason?: string): Promise<Vehicle> {
  requirePermission(session, 'MANAGE_VEHICLES');
  const vehicle = await getVehicle(id);
  if (vehicle.status === target) return vehicle;
  await assertTransition(vehicle, target);
  const { data, error } = await db().from(TABLE).update({ status: target }).eq('id', id).select('*').single();
  if (error) throw error;
  await audit(
    session?.username, 'STATUS', 'VEHICLE', vehicle.registration_number,
    `Status ${vehicle.status} → ${target}${reason?.trim() ? `: ${reason.trim()}` : ''}`,
  );
  return data as Vehicle;
}

export async function deleteVehicle(session: SessionUser | null, id: number): Promise<void> {
  requirePermission(session, 'MANAGE_VEHICLES');
  const vehicle = await getVehicle(id);
  const { data: assignment } = await db().from('vehicle_assignments').select('id').eq('vehicle_id', id).eq('status', 'ACTIVE').maybeSingle();
  if (assignment) throw new BusinessRuleError('Vehicle is currently assigned. Return it before deleting.');
  const { data: trip } = await db().from('trips').select('id').eq('vehicle_id', id).eq('status', 'ACTIVE').maybeSingle();
  if (trip) throw new BusinessRuleError('Vehicle is on an active trip.');
  await db().from(TABLE).update({ deleted_at: nowIso() }).eq('id', id);
  await audit(session?.username, 'DELETE', 'VEHICLE', vehicle.registration_number, 'Moved vehicle to recycle bin');
}

export async function restoreVehicle(session: SessionUser | null, id: number): Promise<void> {
  requirePermission(session, 'MANAGE_VEHICLES');
  const vehicle = await getVehicle(id);
  await db().from(TABLE).update({ deleted_at: null }).eq('id', id);
  await audit(session?.username, 'RESTORE', 'VEHICLE', vehicle.registration_number, 'Restored vehicle from recycle bin');
}

/** Optional fields only for the quick "add vehicle" form. */
export function emptyVehicle(): Partial<Vehicle> {
  return {
    vehicle_type: 'CAR', fuel_type: 'PETROL', status: 'AVAILABLE', year: new Date().getFullYear(),
    capacity: 4, current_mileage: 0, acquisition_cost: 0, attribute_number: 4, attribute_text: 'MANUAL',
  };
}

export function isValidationError(e: unknown): e is ValidationError {
  return e instanceof ValidationError;
}
