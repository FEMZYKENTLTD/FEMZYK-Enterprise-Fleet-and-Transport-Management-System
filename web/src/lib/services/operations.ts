import 'server-only';
import { db, unwrap } from '../db';
import { audit, requirePermission } from '../auth';
import { BusinessRuleError, NotFoundError, ValidationError } from '../errors';
import type { ExpenseCategory, FuelType, MaintenanceStatus, MaintenanceType } from '../domain';
import type { Expense, FuelRecord, MaintenanceRecord } from '../types';
import type { SessionUser } from '../session';
import { dateOnly, errors, int, money, num, str } from '../validation';
import { todayIso } from './common';

/* ========================================================================== *
 *  Maintenance                                                               *
 * ========================================================================== */

type MaintenanceRow = Omit<MaintenanceRecord, 'registration_number' | 'vehicle_display'> & {
  vehicles: { registration_number: string; make: string; model: string } | null;
};

function mapMaintenance(row: MaintenanceRow): MaintenanceRecord {
  const { vehicles, ...rest } = row;
  return {
    ...rest,
    cost: Number(rest.cost),
    registration_number: vehicles?.registration_number ?? '—',
    vehicle_display: vehicles ? `${vehicles.make} ${vehicles.model}` : '—',
  };
}

export async function listMaintenance(filters: { q?: string; status?: string; type?: string; vehicleId?: number } = {}): Promise<MaintenanceRecord[]> {
  let query = db().from('maintenance_records')
    .select('*, vehicles(registration_number, make, model)')
    .order('service_date', { ascending: false })
    .limit(400);
  if (filters.status) query = query.eq('status', filters.status);
  if (filters.type) query = query.eq('maintenance_type', filters.type);
  if (filters.vehicleId) query = query.eq('vehicle_id', filters.vehicleId);
  const rows = unwrap<MaintenanceRow[]>(await query, 'list maintenance').map(mapMaintenance);
  if (!filters.q?.trim()) return rows;
  const q = filters.q.trim().toLowerCase();
  return rows.filter((r) => [r.description, r.provider, r.registration_number, r.maintenance_type].some((v) => (v ?? '').toLowerCase().includes(q)));
}

/** Records maintenance. IN_PROGRESS work puts the vehicle into MAINTENANCE; cost > 0 posts a linked expense. */
export async function createMaintenance(session: SessionUser | null, input: Record<string, unknown>): Promise<MaintenanceRecord> {
  requirePermission(session, 'MANAGE_MAINTENANCE');
  const vehicleId = int(input.vehicle_id) ?? 0;
  const record = {
    vehicle_id: vehicleId,
    service_date: dateOnly(input.service_date) ?? todayIso(),
    maintenance_type: (str(input.maintenance_type) ?? 'ROUTINE_SERVICE') as MaintenanceType,
    description: str(input.description) ?? '',
    provider: str(input.provider),
    cost: money(input.cost),
    mileage_at_service: int(input.mileage_at_service),
    next_service_date: dateOnly(input.next_service_date),
    next_service_mileage: int(input.next_service_mileage),
    status: (str(input.status) ?? 'COMPLETED') as MaintenanceStatus,
    notes: str(input.notes),
  };
  errors()
    .check(vehicleId > 0, 'Vehicle is required.')
    .required(record.description, 'Description')
    .required(record.service_date, 'Service date')
    .nonNegative(record.cost, 'Cost')
    .check(['ROUTINE_SERVICE', 'REPAIR', 'INSPECTION', 'TYRE', 'BODYWORK', 'ELECTRICAL', 'OTHER'].includes(record.maintenance_type), 'Invalid maintenance type.')
    .check(['SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'].includes(record.status), 'Invalid maintenance status.')
    .throwIfAny();

  const { data: vehicleData } = await db().from('vehicles').select('id, registration_number, status, deleted_at').eq('id', vehicleId).maybeSingle();
  if (!vehicleData) throw new NotFoundError('Vehicle', vehicleId);
  const vehicle = vehicleData as { id: number; registration_number: string; status: string; deleted_at: string | null };
  if (vehicle.deleted_at) throw new BusinessRuleError('Vehicle is in the recycle bin.');

  const { data, error } = await db().from('maintenance_records').insert(record).select('id').single();
  if (error) throw error;
  const id = (data as { id: number }).id;

  if (record.status === 'IN_PROGRESS' && ['AVAILABLE', 'ASSIGNED', 'MAINTENANCE'].includes(vehicle.status)) {
    await db().from('vehicles').update({ status: 'MAINTENANCE' }).eq('id', vehicleId);
  }
  if (record.cost > 0 && record.status !== 'CANCELLED') await syncMaintenanceExpense(vehicleId, id, record.cost, record.description, record.provider, record.service_date);
  await audit(session?.username, 'CREATE', 'MAINTENANCE', vehicle.registration_number, `${record.maintenance_type}: ${record.description} (${record.status})`);
  return getMaintenance(id);
}

export async function updateMaintenance(session: SessionUser | null, id: number, input: Record<string, unknown>): Promise<MaintenanceRecord> {
  requirePermission(session, 'MANAGE_MAINTENANCE');
  const existing = await getMaintenance(id);
  const record = {
    service_date: dateOnly(input.service_date) ?? existing.service_date,
    maintenance_type: (str(input.maintenance_type) ?? existing.maintenance_type) as MaintenanceType,
    description: str(input.description) ?? existing.description,
    provider: input.provider !== undefined ? str(input.provider) : existing.provider,
    cost: input.cost !== undefined ? money(input.cost) : Number(existing.cost),
    mileage_at_service: input.mileage_at_service !== undefined ? int(input.mileage_at_service) : existing.mileage_at_service,
    next_service_date: input.next_service_date !== undefined ? dateOnly(input.next_service_date) : existing.next_service_date,
    next_service_mileage: input.next_service_mileage !== undefined ? int(input.next_service_mileage) : existing.next_service_mileage,
    status: (str(input.status) ?? existing.status) as MaintenanceStatus,
    notes: input.notes !== undefined ? str(input.notes) : existing.notes,
  };
  errors().required(record.description, 'Description').nonNegative(record.cost, 'Cost').throwIfAny();
  const { error } = await db().from('maintenance_records').update(record).eq('id', id);
  if (error) throw error;

  const { data: vehicleData } = await db().from('vehicles').select('id, status, registration_number').eq('id', existing.vehicle_id).maybeSingle();
  const vehicle = vehicleData as { id: number; status: string; registration_number: string } | null;
  if (vehicle) {
    if (record.status === 'IN_PROGRESS' && vehicle.status !== 'MAINTENANCE' && ['AVAILABLE', 'ASSIGNED'].includes(vehicle.status)) {
      await db().from('vehicles').update({ status: 'MAINTENANCE' }).eq('id', vehicle.id);
    } else if (['COMPLETED', 'CANCELLED'].includes(record.status) && vehicle.status === 'MAINTENANCE') {
      const { data: assigned } = await db().from('vehicle_assignments').select('id').eq('vehicle_id', vehicle.id).eq('status', 'ACTIVE').maybeSingle();
      await db().from('vehicles').update({ status: assigned ? 'ASSIGNED' : 'AVAILABLE' }).eq('id', vehicle.id);
    }
  }
  if (record.cost > 0 && record.status !== 'CANCELLED') {
    await syncMaintenanceExpense(existing.vehicle_id, id, record.cost, record.description, record.provider, record.service_date);
  } else {
    await db().from('expenses').delete().eq('source_maintenance_record_id', id);
  }
  await audit(session?.username, 'UPDATE', 'MAINTENANCE', vehicle?.registration_number ?? null, `${record.maintenance_type}: ${record.description} (${record.status})`);
  return getMaintenance(id);
}

export async function deleteMaintenance(session: SessionUser | null, id: number): Promise<void> {
  requirePermission(session, 'MANAGE_MAINTENANCE');
  const existing = await getMaintenance(id);
  await db().from('expenses').delete().eq('source_maintenance_record_id', id);
  const { error } = await db().from('maintenance_records').delete().eq('id', id);
  if (error) throw error;
  await audit(session?.username, 'DELETE', 'MAINTENANCE', existing.registration_number, `Deleted maintenance record: ${existing.description}`);
}

export async function getMaintenance(id: number): Promise<MaintenanceRecord> {
  const { data, error } = await db().from('maintenance_records').select('*, vehicles(registration_number, make, model)').eq('id', id).maybeSingle();
  if (error) throw new Error(error.message);
  if (!data) throw new NotFoundError('Maintenance record', id);
  return mapMaintenance(data as MaintenanceRow);
}

async function syncMaintenanceExpense(vehicleId: number, maintenanceId: number, cost: number, description: string, provider: string | null, serviceDate: string) {
  const { data: linked } = await db().from('expenses').select('id').eq('source_maintenance_record_id', maintenanceId).maybeSingle();
  if (linked) {
    await db().from('expenses').update({ amount: cost, description, vendor: provider, expense_date: serviceDate }).eq('id', (linked as { id: number }).id);
  } else {
    await db().from('expenses').insert({
      vehicle_id: vehicleId,
      expense_date: serviceDate,
      category: 'MAINTENANCE',
      amount: cost,
      vendor: provider,
      description,
      source_maintenance_record_id: maintenanceId,
      notes: 'Automatically generated from a maintenance record. Edit the maintenance record instead.',
    });
  }
}

/** Service-due alerts, same 14-day / 500 km windows as AppConfig. */
export async function maintenanceDue() {
  const rows = unwrap<{
    vehicle_id: number; registration_number: string; vehicle: string; current_mileage: number;
    next_service_date: string | null; next_service_mileage: number | null; due_status: 'OVERDUE' | 'DUE SOON' | 'OK';
  }[]>(await db().from('v_maintenance_due').select('*').order('next_service_date'), 'maintenance due');
  return rows.filter((r) => r.due_status !== 'OK');
}

/* ========================================================================== *
 *  Fuel                                                                      *
 * ========================================================================== */

type FuelRow = Omit<FuelRecord, 'registration_number' | 'driver_name'> & {
  vehicles: { registration_number: string } | null;
  drivers: { employees: { first_name: string; last_name: string } | null } | null;
};

function mapFuel(row: FuelRow): FuelRecord {
  const { vehicles, drivers, ...rest } = row;
  const employee = drivers?.employees;
  return {
    ...rest,
    quantity_litres: Number(rest.quantity_litres),
    unit_price: Number(rest.unit_price),
    total_cost: Number(rest.total_cost),
    registration_number: vehicles?.registration_number ?? '—',
    driver_name: employee ? `${employee.first_name} ${employee.last_name}`.trim() : null,
  };
}

export async function listFuel(filters: { q?: string; vehicleId?: number; from?: string; to?: string } = {}): Promise<FuelRecord[]> {
  let query = db().from('fuel_records')
    .select('*, vehicles(registration_number), drivers(employees(first_name, last_name))')
    .order('fuel_date', { ascending: false })
    .order('id', { ascending: false })
    .limit(400);
  if (filters.vehicleId) query = query.eq('vehicle_id', filters.vehicleId);
  if (filters.from) query = query.gte('fuel_date', filters.from);
  if (filters.to) query = query.lte('fuel_date', filters.to);
  const rows = unwrap<FuelRow[]>(await query, 'list fuel').map(mapFuel);
  if (!filters.q?.trim()) return rows;
  const q = filters.q.trim().toLowerCase();
  return rows.filter((r) => [r.registration_number, r.station, r.receipt_reference, r.driver_name].some((v) => (v ?? '').toLowerCase().includes(q)));
}

export async function getFuel(id: number): Promise<FuelRecord> {
  const { data, error } = await db().from('fuel_records').select('*, vehicles(registration_number), drivers(employees(first_name, last_name))').eq('id', id).maybeSingle();
  if (error) throw new Error(error.message);
  if (!data) throw new NotFoundError('Fuel record', id);
  return mapFuel(data as FuelRow);
}

/** Fuel purchase: total = ROUND(litres × unit price, 2); odometer may only move forward. */
export async function createFuel(session: SessionUser | null, input: Record<string, unknown>): Promise<FuelRecord> {
  requirePermission(session, 'MANAGE_FUEL');
  const vehicleId = int(input.vehicle_id) ?? 0;
  const litres = num(input.quantity_litres) ?? 0;
  const unitPrice = num(input.unit_price) ?? 0;
  const mileage = int(input.mileage);
  errors()
    .check(vehicleId > 0, 'Vehicle is required.')
    .check(litres > 0, 'Quantity (litres) must be greater than zero.')
    .nonNegative(unitPrice, 'Unit price')
    .nonNegative(mileage, 'Odometer')
    .throwIfAny();

  const { data: vehicleData } = await db().from('vehicles').select('id, registration_number, current_mileage, fuel_type, deleted_at').eq('id', vehicleId).maybeSingle();
  if (!vehicleData) throw new NotFoundError('Vehicle', vehicleId);
  const vehicle = vehicleData as { id: number; registration_number: string; current_mileage: number; fuel_type: FuelType; deleted_at: string | null };
  if (vehicle.deleted_at) throw new BusinessRuleError('Vehicle is in the recycle bin.');
  if (mileage !== null && mileage < Number(vehicle.current_mileage)) {
    throw new BusinessRuleError(`Odometer (${mileage} km) cannot be lower than the vehicle's recorded mileage (${vehicle.current_mileage} km).`);
  }
  const record = {
    vehicle_id: vehicleId,
    driver_id: int(input.driver_id) || null,
    fuel_date: dateOnly(input.fuel_date) ?? todayIso(),
    quantity_litres: litres,
    unit_price: unitPrice,
    total_cost: Math.round(litres * unitPrice * 100) / 100,
    mileage,
    station: str(input.station),
    receipt_reference: str(input.receipt_reference),
    fuel_type: (str(input.fuel_type) ?? vehicle.fuel_type) as FuelType,
    notes: str(input.notes),
  };
  const { data, error } = await db().from('fuel_records').insert(record).select('id').single();
  if (error) throw error;
  const id = (data as { id: number }).id;
  if (mileage !== null && mileage > Number(vehicle.current_mileage)) {
    await db().from('vehicles').update({ current_mileage: mileage }).eq('id', vehicleId);
  }
  await db().from('expenses').insert({
    vehicle_id: vehicleId,
    expense_date: record.fuel_date,
    category: 'FUEL',
    amount: record.total_cost,
    vendor: record.station,
    description: `${record.quantity_litres} L ${record.fuel_type.toLowerCase()} — ${vehicle.registration_number}`,
    reference: record.receipt_reference,
    source_fuel_record_id: id,
    notes: 'Automatically generated from a fuel record. Edit the fuel record instead.',
  });
  await audit(session?.username, 'CREATE', 'FUEL', vehicle.registration_number, `${record.quantity_litres.toFixed(2)} L @ ${record.unit_price.toFixed(2)} = ₦${record.total_cost.toFixed(2)}`);
  return getFuel(id);
}

export async function updateFuel(session: SessionUser | null, id: number, input: Record<string, unknown>): Promise<FuelRecord> {
  requirePermission(session, 'MANAGE_FUEL');
  const existing = await getFuel(id);
  const litres = input.quantity_litres !== undefined ? num(input.quantity_litres) ?? 0 : existing.quantity_litres;
  const unitPrice = input.unit_price !== undefined ? num(input.unit_price) ?? 0 : existing.unit_price;
  errors().check(litres > 0, 'Quantity (litres) must be greater than zero.').nonNegative(unitPrice, 'Unit price').throwIfAny();
  const total = Math.round(Number(litres) * Number(unitPrice) * 100) / 100;
  const record = {
    driver_id: input.driver_id !== undefined ? int(input.driver_id) || null : existing.driver_id,
    fuel_date: dateOnly(input.fuel_date) ?? existing.fuel_date,
    quantity_litres: litres,
    unit_price: unitPrice,
    total_cost: total,
    mileage: input.mileage !== undefined ? int(input.mileage) : existing.mileage,
    station: input.station !== undefined ? str(input.station) : existing.station,
    receipt_reference: input.receipt_reference !== undefined ? str(input.receipt_reference) : existing.receipt_reference,
    fuel_type: input.fuel_type !== undefined ? (str(input.fuel_type) as FuelType) : existing.fuel_type,
    notes: input.notes !== undefined ? str(input.notes) : existing.notes,
  };
  const { error } = await db().from('fuel_records').update(record).eq('id', id);
  if (error) throw error;
  await db().from('expenses').update({ amount: total, expense_date: record.fuel_date, vendor: record.station, reference: record.receipt_reference }).eq('source_fuel_record_id', id);
  await audit(session?.username, 'UPDATE', 'FUEL', existing.registration_number, `Fuel record updated: ₦${total.toFixed(2)}`);
  return getFuel(id);
}

export async function deleteFuel(session: SessionUser | null, id: number): Promise<void> {
  requirePermission(session, 'MANAGE_FUEL');
  const existing = await getFuel(id);
  await db().from('expenses').delete().eq('source_fuel_record_id', id);
  const { error } = await db().from('fuel_records').delete().eq('id', id);
  if (error) throw error;
  await audit(session?.username, 'DELETE', 'FUEL', existing.registration_number, `Deleted fuel record of ₦${Number(existing.total_cost).toFixed(2)}`);
}

/* ========================================================================== *
 *  Expenses                                                                  *
 * ========================================================================== */

type ExpenseRow = Omit<Expense, 'registration_number' | 'system_generated'> & {
  vehicles: { registration_number: string } | null;
};

function mapExpense(row: ExpenseRow): Expense {
  const { vehicles, ...rest } = row;
  return {
    ...rest,
    amount: Number(rest.amount),
    registration_number: vehicles?.registration_number ?? null,
    system_generated: Boolean(rest.source_fuel_record_id || rest.source_maintenance_record_id),
  };
}

export async function listExpenses(filters: { q?: string; category?: string; vehicleId?: number; from?: string; to?: string; excludeSystem?: boolean } = {}): Promise<Expense[]> {
  let query = db().from('expenses')
    .select('*, vehicles(registration_number)')
    .order('expense_date', { ascending: false })
    .order('id', { ascending: false })
    .limit(500);
  if (filters.category) query = query.eq('category', filters.category);
  if (filters.vehicleId) query = query.eq('vehicle_id', filters.vehicleId);
  if (filters.from) query = query.gte('expense_date', filters.from);
  if (filters.to) query = query.lte('expense_date', filters.to);
  const rows = unwrap<ExpenseRow[]>(await query, 'list expenses').map(mapExpense);
  let filtered = rows;
  if (filters.excludeSystem) filtered = filtered.filter((r) => !r.system_generated);
  if (!filters.q?.trim()) return filtered;
  const q = filters.q.trim().toLowerCase();
  return filtered.filter((r) => [r.description, r.vendor, r.reference, r.registration_number, r.category].some((v) => (v ?? '').toLowerCase().includes(q)));
}

export async function createExpense(session: SessionUser | null, input: Record<string, unknown>): Promise<Expense> {
  requirePermission(session, 'MANAGE_EXPENSES');
  const record = {
    vehicle_id: int(input.vehicle_id) || null,
    expense_date: dateOnly(input.expense_date) ?? todayIso(),
    category: (str(input.category) ?? 'MISCELLANEOUS') as ExpenseCategory,
    amount: money(input.amount),
    vendor: str(input.vendor),
    description: str(input.description) ?? '',
    reference: str(input.reference),
    notes: str(input.notes),
  };
  errors()
    .required(record.description, 'Description')
    .check(record.amount >= 0, 'Amount cannot be negative.')
    .check(record.amount > 0, 'Amount must be greater than zero.')
    .required(record.expense_date, 'Expense date')
    .throwIfAny();
  const { data, error } = await db().from('expenses').insert(record).select('id').single();
  if (error) throw error;
  const id = (data as { id: number }).id;
  await audit(session?.username, 'CREATE', 'EXPENSE', record.category, `Recorded ${record.category} expense of ₦${record.amount.toFixed(2)}: ${record.description}`);
  const { data: row, error: readError } = await db().from('expenses').select('*, vehicles(registration_number)').eq('id', id).single();
  if (readError) throw readError;
  return mapExpense(row as ExpenseRow);
}

export async function updateExpense(session: SessionUser | null, id: number, input: Record<string, unknown>): Promise<Expense> {
  requirePermission(session, 'MANAGE_EXPENSES');
  const { data: existing, error: readError } = await db().from('expenses').select('*').eq('id', id).maybeSingle();
  if (readError) throw readError;
  if (!existing) throw new NotFoundError('Expense', id);
  const row = existing as Expense;
  if (row.source_fuel_record_id || row.source_maintenance_record_id) {
    throw new BusinessRuleError('This expense was generated automatically. Edit the fuel or maintenance record instead.');
  }
  const record = {
    vehicle_id: input.vehicle_id !== undefined ? int(input.vehicle_id) || null : row.vehicle_id,
    expense_date: dateOnly(input.expense_date) ?? row.expense_date,
    category: (str(input.category) ?? row.category) as ExpenseCategory,
    amount: input.amount !== undefined ? money(input.amount) : Number(row.amount),
    vendor: input.vendor !== undefined ? str(input.vendor) : row.vendor,
    description: str(input.description) ?? row.description,
    reference: input.reference !== undefined ? str(input.reference) : row.reference,
    notes: input.notes !== undefined ? str(input.notes) : row.notes,
  };
  errors().required(record.description, 'Description').check(record.amount > 0, 'Amount must be greater than zero.').throwIfAny();
  const { error } = await db().from('expenses').update(record).eq('id', id);
  if (error) throw error;
  await audit(session?.username, 'UPDATE', 'EXPENSE', record.category, `Updated ${record.category} expense: ${record.description}`);
  const { data: updated, error: e2 } = await db().from('expenses').select('*, vehicles(registration_number)').eq('id', id).single();
  if (e2) throw e2;
  return mapExpense(updated as ExpenseRow);
}

export async function deleteExpense(session: SessionUser | null, id: number): Promise<void> {
  requirePermission(session, 'MANAGE_EXPENSES');
  const { data } = await db().from('expenses').select('*').eq('id', id).maybeSingle();
  if (!data) throw new NotFoundError('Expense', id);
  const row = data as Expense;
  if (row.source_fuel_record_id || row.source_maintenance_record_id) {
    throw new BusinessRuleError('This expense was generated automatically. Delete the fuel or maintenance record instead.');
  }
  const { error } = await db().from('expenses').delete().eq('id', id);
  if (error) throw error;
  await audit(session?.username, 'DELETE', 'EXPENSE', row.category, `Deleted expense: ${row.description}`);
}

export async function expenseTotals() {
  const { data, error } = await db().from('expenses').select('category, amount, expense_date');
  if (error) throw new Error(error.message);
  const rows = (data ?? []) as { category: string; amount: number; expense_date: string }[];
  const byCategory = new Map<string, number>();
  let month = 0;
  const start = `${todayIso().slice(0, 7)}-01`;
  for (const r of rows) {
    byCategory.set(r.category, (byCategory.get(r.category) ?? 0) + Number(r.amount));
    if (r.expense_date >= start) month += Number(r.amount);
  }
  return {
    total: rows.reduce((s, r) => s + Number(r.amount), 0),
    month,
    byCategory: [...byCategory.entries()].map(([category, amount]) => ({ category, amount })).sort((a, b) => b.amount - a.amount),
  };
}

