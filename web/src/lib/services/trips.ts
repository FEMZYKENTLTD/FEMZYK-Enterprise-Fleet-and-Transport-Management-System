import 'server-only';
import { db, unwrap } from '../db';
import { audit, requirePermission } from '../auth';
import { BusinessRuleError, NotFoundError } from '../errors';
import { licencePermits, type LicenceCategory, type TripStatus, type VehicleType } from '../domain';
import type { Trip } from '../types';
import type { SessionUser } from '../session';
import { errors, int, num, str, timestamp } from '../validation';
import { nowIso, todayIso } from './common';

const TABLE = 'trips';

type TripRow = Omit<Trip, 'registration_number' | 'driver_name' | 'distance_km'> & {
  vehicles: { registration_number: string } | null;
  drivers: { employees: { first_name: string; last_name: string } | null } | null;
};

function mapTrip(row: TripRow): Trip {
  const { vehicles, drivers, ...rest } = row;
  const employee = drivers?.employees;
  const distance =
    rest.start_mileage !== null && rest.end_mileage !== null ? Number(rest.end_mileage) - Number(rest.start_mileage) : null;
  return {
    ...rest,
    registration_number: vehicles?.registration_number ?? '—',
    driver_name: employee ? `${employee.first_name} ${employee.last_name}`.trim() : '—',
    distance_km: distance,
  };
}

export async function listTrips(filters: { q?: string; status?: string; vehicleId?: number; driverId?: number; from?: string; to?: string; limit?: number } = {}): Promise<Trip[]> {
  let query = db().from(TABLE)
    .select('*, vehicles(registration_number), drivers(employees(first_name, last_name))')
    .order('departure_time', { ascending: false })
    .limit(filters.limit ?? 400);
  if (filters.status) query = query.eq('status', filters.status);
  if (filters.vehicleId) query = query.eq('vehicle_id', filters.vehicleId);
  if (filters.driverId) query = query.eq('driver_id', filters.driverId);
  if (filters.from) query = query.gte('departure_time', `${filters.from}T00:00:00`);
  if (filters.to) query = query.lte('departure_time', `${filters.to}T23:59:59`);
  const trips = unwrap<TripRow[]>(await query, 'list trips').map(mapTrip);
  if (!filters.q?.trim()) return trips;
  const q = filters.q.trim().toLowerCase();
  return trips.filter((t) => [t.trip_code, t.destination, t.origin, t.registration_number, t.driver_name, t.purpose].some((v) => (v ?? '').toLowerCase().includes(q)));
}

export async function getTrip(id: number): Promise<Trip> {
  const { data, error } = await db().from(TABLE).select('*, vehicles(registration_number), drivers(employees(first_name, last_name))').eq('id', id).maybeSingle();
  if (error) throw new Error(error.message);
  if (!data) throw new NotFoundError('Trip', id);
  return mapTrip(data as TripRow);
}

async function loadForValidation(vehicleId: number, driverId: number) {
  const { data: vehicleData } = await db()
    .from('vehicles').select('id, registration_number, vehicle_type, status, current_mileage, deleted_at').eq('id', vehicleId).maybeSingle();
  if (!vehicleData) throw new NotFoundError('Vehicle', vehicleId);
  const { data: driverData } = await db()
    .from('drivers').select('id, status, licence_category, licence_expiry_date, deleted_at, employees(first_name, last_name)').eq('id', driverId).maybeSingle();
  if (!driverData) throw new NotFoundError('Driver', driverId);
  return {
    vehicle: vehicleData as { id: number; registration_number: string; vehicle_type: VehicleType; status: string; current_mileage: number; deleted_at: string | null },
    driver: driverData as unknown as {
      id: number; status: string; licence_category: LicenceCategory; licence_expiry_date: string; deleted_at: string | null;
      employees: { first_name: string; last_name: string } | null;
    },
  };
}

function driverName(driver: { employees: { first_name: string; last_name: string } | null }): string {
  return driver.employees ? `${driver.employees.first_name} ${driver.employees.last_name}`.trim() : 'Driver';
}

export async function createTrip(
  session: SessionUser | null,
  input: Record<string, unknown>,
  startNow = false,
): Promise<Trip> {
  requirePermission(session, 'MANAGE_TRIPS');
  const vehicleId = int(input.vehicle_id) ?? 0;
  const driverId = int(input.driver_id) ?? 0;
  const destination = str(input.destination) ?? '';
  const departure = timestamp(input.departure_time) ?? nowIso();
  errors().check(vehicleId > 0, 'Vehicle is required.').required(destination, 'Destination').throwIfAny();

  const { vehicle, driver } = await loadForValidation(vehicleId, driverId);
  if (vehicle.deleted_at || ['RETIRED', 'OUT_OF_SERVICE', 'MAINTENANCE'].includes(vehicle.status)) {
    throw new BusinessRuleError(`Vehicle ${vehicle.registration_number} is ${vehicle.status.replaceAll('_', ' ')} and cannot be used for a trip.`);
  }
  if (driver.status !== 'ACTIVE') throw new BusinessRuleError(`Driver ${driverName(driver)} is ${driver.status.replaceAll('_', ' ')}.`);
  if (driver.licence_expiry_date < todayIso()) throw new BusinessRuleError(`Driver ${driverName(driver)}'s licence has expired.`);
  if (!licencePermits(driver.licence_category, vehicle.vehicle_type)) {
    throw new BusinessRuleError(`Licence class ${driver.licence_category} does not permit driving a ${vehicle.vehicle_type}.`);
  }
  const startMileage = int(input.start_mileage) ?? Number(vehicle.current_mileage);
  if (startMileage < Number(vehicle.current_mileage)) {
    throw new BusinessRuleError(`Starting mileage (${startMileage}) cannot be lower than the vehicle's current odometer (${vehicle.current_mileage} km).`);
  }
  const { data: code } = await db().rpc('next_code', { p_prefix: 'TRP' });
  const { data, error } = await db()
    .from(TABLE)
    .insert({
      trip_code: (code as string) ?? `TRP-${Date.now()}`,
      vehicle_id: vehicleId,
      driver_id: driverId,
      origin: str(input.origin),
      destination,
      purpose: str(input.purpose),
      departure_time: departure,
      start_mileage: startMileage,
      status: 'PLANNED',
      notes: str(input.notes),
    })
    .select('id')
    .single();
  if (error) throw error;
  const tripId = (data as { id: number }).id;
  await audit(session?.username, 'CREATE', 'TRIP', (code as string) ?? '', `Planned trip to ${destination} with ${vehicle.registration_number} / ${driverName(driver)}`);
  if (startNow) return startTrip(session, tripId);
  return getTrip(tripId);
}

export async function updateTrip(session: SessionUser | null, id: number, input: Record<string, unknown>): Promise<Trip> {
  requirePermission(session, 'MANAGE_TRIPS');
  const existing = await getTrip(id);
  if (existing.status === 'COMPLETED' || existing.status === 'CANCELLED') {
    throw new BusinessRuleError(`A ${existing.status} trip cannot be edited.`);
  }
  const startMileage = int(input.start_mileage) ?? existing.start_mileage;
  const endMileage = int(input.end_mileage) ?? existing.end_mileage;
  errors()
    .required(str(input.destination) ?? existing.destination, 'Destination')
    .check(startMileage === null || endMileage === null || endMileage >= startMileage, 'Ending mileage must be greater than or equal to starting mileage.')
    .maxLength(str(input.notes), 2000, 'Notes')
    .throwIfAny();
  await db()
    .from(TABLE)
    .update({
      origin: input.origin !== undefined ? str(input.origin) : existing.origin,
      destination: str(input.destination) ?? existing.destination,
      purpose: input.purpose !== undefined ? str(input.purpose) : existing.purpose,
      departure_time: timestamp(input.departure_time) ?? existing.departure_time,
      start_mileage: startMileage,
      fuel_used_litres: input.fuel_used_litres !== undefined ? num(input.fuel_used_litres) : existing.fuel_used_litres,
      notes: input.notes !== undefined ? str(input.notes) : existing.notes,
    })
    .eq('id', id);
  await audit(session?.username, 'UPDATE', 'TRIP', existing.trip_code, `Updated trip to ${str(input.destination) ?? existing.destination}`);
  return getTrip(id);
}

export async function startTrip(session: SessionUser | null, id: number): Promise<Trip> {
  requirePermission(session, 'MANAGE_TRIPS');
  const trip = await getTrip(id);
  if (trip.status !== 'PLANNED') throw new BusinessRuleError(`Only PLANNED trips can be started (trip is ${trip.status}).`);
  const { data: busyVehicle } = await db().from(TABLE).select('id').eq('vehicle_id', trip.vehicle_id).eq('status', 'ACTIVE').maybeSingle();
  if (busyVehicle) throw new BusinessRuleError('This vehicle is already on an active trip.');
  const { data: busyDriver } = await db().from(TABLE).select('id').eq('driver_id', trip.driver_id).eq('status', 'ACTIVE').maybeSingle();
  if (busyDriver) throw new BusinessRuleError('This driver is already on an active trip.');
  const { data: vehicleData } = await db().from('vehicles').select('status').eq('id', trip.vehicle_id).maybeSingle();
  const status = (vehicleData as { status: string } | null)?.status ?? '';
  if (!['AVAILABLE', 'ASSIGNED', 'IN_SERVICE'].includes(status)) throw new BusinessRuleError(`Vehicle is ${status.replaceAll('_', ' ')}.`);
  await db().from(TABLE).update({ status: 'ACTIVE', departure_time: trip.departure_time ?? nowIso() }).eq('id', id);
  await db().from('vehicles').update({ status: 'IN_SERVICE' }).eq('id', trip.vehicle_id);
  await audit(session?.username, 'START', 'TRIP', trip.trip_code, 'Trip started');
  return getTrip(id);
}

/** Completes a trip, bumping the vehicle odometer and restoring its status. */
export async function completeTrip(
  session: SessionUser | null,
  id: number,
  input: { end_mileage: number; fuel_used_litres?: number | null; return_time?: string | null; notes?: string | null },
): Promise<Trip> {
  requirePermission(session, 'MANAGE_TRIPS');
  const trip = await getTrip(id);
  if (!['ACTIVE', 'PLANNED'].includes(trip.status)) throw new BusinessRuleError(`Trip is already ${trip.status}.`);
  const endMileage = int(input.end_mileage);
  if (endMileage === null) throw new BusinessRuleError('Ending mileage is required.');
  const returnTime = timestamp(input.return_time) ?? nowIso();
  errors()
    .check(trip.start_mileage !== null, 'Starting mileage is missing.')
    .check(trip.start_mileage === null || endMileage >= Number(trip.start_mileage), `Ending mileage (${endMileage}) must be greater than or equal to starting mileage (${trip.start_mileage}).`)
    .check(input.fuel_used_litres == null || Number(input.fuel_used_litres) >= 0, 'Fuel used cannot be negative.')
    .check(!trip.departure_time || new Date(returnTime) >= new Date(trip.departure_time), 'Return time cannot be before departure time.')
    .throwIfAny();
  if (trip.start_mileage !== null && endMileage - Number(trip.start_mileage) > 5000) {
    throw new BusinessRuleError('A single trip of more than 5,000 km looks wrong. Please check the odometer readings.');
  }
  await db()
    .from(TABLE)
    .update({
      end_mileage: endMileage,
      fuel_used_litres: input.fuel_used_litres ?? null,
      return_time: returnTime,
      notes: input.notes?.trim() || trip.notes,
      status: 'COMPLETED',
    })
    .eq('id', id);

  const { data: vehicle } = await db().from('vehicles').select('id, current_mileage, status').eq('id', trip.vehicle_id).maybeSingle();
  const v = vehicle as { id: number; current_mileage: number; status: string } | null;
  if (v && endMileage > Number(v.current_mileage)) {
    await db().from('vehicles').update({ current_mileage: endMileage }).eq('id', v.id);
  }
  if (v?.status === 'IN_SERVICE') {
    const { data: assigned } = await db().from('vehicle_assignments').select('id').eq('vehicle_id', v.id).eq('status', 'ACTIVE').maybeSingle();
    await db().from('vehicles').update({ status: assigned ? 'ASSIGNED' : 'AVAILABLE' }).eq('id', v.id);
  }
  const distance = trip.start_mileage !== null ? endMileage - Number(trip.start_mileage) : null;
  await audit(session?.username, 'COMPLETE', 'TRIP', trip.trip_code, `Trip completed${distance !== null ? `, distance ${distance} km` : ''}`);
  return getTrip(id);
}

export async function cancelTrip(session: SessionUser | null, id: number, reason: string): Promise<Trip> {
  requirePermission(session, 'MANAGE_TRIPS');
  const trip = await getTrip(id);
  if (['COMPLETED', 'CANCELLED'].includes(trip.status)) throw new BusinessRuleError(`Trip is already ${trip.status}.`);
  const wasActive = trip.status === 'ACTIVE';
  const notes = `${trip.notes ? `${trip.notes}\n` : ''}Cancelled: ${reason ?? ''}`.trim();
  await db().from(TABLE).update({ status: 'CANCELLED', notes }).eq('id', id);
  if (wasActive) {
    const { data: vehicle } = await db().from('vehicles').select('id, status').eq('id', trip.vehicle_id).maybeSingle();
    const v = vehicle as { id: number; status: string } | null;
    if (v?.status === 'IN_SERVICE') {
      const { data: assigned } = await db().from('vehicle_assignments').select('id').eq('vehicle_id', v.id).eq('status', 'ACTIVE').maybeSingle();
      await db().from('vehicles').update({ status: assigned ? 'ASSIGNED' : 'AVAILABLE' }).eq('id', v.id);
    }
  }
  await audit(session?.username, 'CANCEL', 'TRIP', trip.trip_code, `Trip cancelled: ${reason}`);
  return getTrip(id);
}

export async function deleteTrip(session: SessionUser | null, id: number): Promise<void> {
  requirePermission(session, 'MANAGE_TRIPS');
  const trip = await getTrip(id);
  if (!['PLANNED', 'CANCELLED'].includes(trip.status)) {
    throw new BusinessRuleError('Only PLANNED or CANCELLED trips can be deleted; completed trips are part of the vehicle history.');
  }
  const { error } = await db().from(TABLE).delete().eq('id', id);
  if (error) throw error;
  await audit(session?.username, 'DELETE', 'TRIP', trip.trip_code, 'Trip deleted');
}

export async function distinctTripStatuses(): Promise<TripStatus[]> {
  const rows = unwrap<{ status: TripStatus }[]>(await db().from(TABLE).select('status'), 'trip statuses');
  return [...new Set(rows.map((r) => r.status))];
}

