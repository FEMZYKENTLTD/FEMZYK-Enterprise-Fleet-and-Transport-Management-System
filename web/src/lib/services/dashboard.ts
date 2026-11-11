import 'server-only';
import { db, unwrap } from '../db';
import type { DashboardData } from '../types';
import { monthEnd, monthStart, monthsAgo, yearStart } from './common';
import { listTrips } from './trips';
import { maintenanceDue } from './operations';
import { listDrivers } from './drivers';
import { todayIso, addDays } from './common';
import type { AuditLog } from '../types';

/**
 * Live dashboard aggregation — the web counterpart of DashboardService.
 * Every figure is computed from the database; nothing is hard-coded.
 */
export async function dashboardSnapshot(): Promise<DashboardData> {
  const today = todayIso();
  const start = monthStart();
  const end = monthEnd();

  const [vehiclesRes, employeesRes, driversRes, tripsRes, assignmentsRes, fuelRes, maintenanceRes, expensesRes, due, drivers, recentTrips] =
    await Promise.all([
      db().from('vehicles').select('id, status, vehicle_type').is('deleted_at', null),
      db().from('employees').select('id, status, department').is('deleted_at', null),
      db().from('drivers').select('id, status').is('deleted_at', null),
      db().from('trips').select('id, status, start_mileage, end_mileage, departure_time').limit(2000),
      db().from('vehicle_assignments').select('id', { count: 'exact', head: true }).eq('status', 'ACTIVE'),
      db().from('fuel_records').select('total_cost, fuel_date'),
      db().from('maintenance_records').select('cost, service_date, status'),
      db().from('expenses').select('amount, category, expense_date'),
      maintenanceDue(),
      listDrivers(),
      listTrips({ limit: 6 }),
    ]);

  const vehicles = unwrap<{ id: number; status: string; vehicle_type: string }[]>(vehiclesRes, 'dashboard vehicles');
  const employees = unwrap<{ id: number; status: string; department: string }[]>(employeesRes, 'dashboard employees');
  const driverRows = unwrap<{ id: number; status: string }[]>(driversRes, 'dashboard drivers');
  const trips = unwrap<{ id: number; status: string; start_mileage: number | null; end_mileage: number | null; departure_time: string }[]>(tripsRes, 'dashboard trips');
  const fuel = unwrap<{ total_cost: number; fuel_date: string }[]>(fuelRes, 'dashboard fuel');
  const maintenance = unwrap<{ cost: number; service_date: string; status: string }[]>(maintenanceRes, 'dashboard maintenance');
  const expenses = unwrap<{ amount: number; category: string; expense_date: string }[]>(expensesRes, 'dashboard expenses');

  const byStatus: Record<string, number> = {};
  const byType: Record<string, number> = {};
  for (const v of vehicles) {
    byStatus[v.status] = (byStatus[v.status] ?? 0) + 1;
    byType[v.vehicle_type] = (byType[v.vehicle_type] ?? 0) + 1;
  }

  const monthTrips = trips.filter((t) => t.departure_time >= `${start}T00:00` && t.departure_time <= `${end}T23:59`);
  const completedThisMonth = monthTrips.filter((t) => t.status === 'COMPLETED');
  const monthDistance = completedThisMonth.reduce(
    (sum, t) => sum + Math.max(0, Number(t.end_mileage ?? 0) - Number(t.start_mileage ?? 0)), 0);

  const monthFuel = fuel.filter((f) => f.fuel_date >= start && f.fuel_date <= end).reduce((s, f) => s + Number(f.total_cost), 0);
  const monthMaintenance = maintenance
    .filter((m) => m.service_date >= start && m.service_date <= end && m.status !== 'CANCELLED')
    .reduce((s, m) => s + Number(m.cost), 0);
  const monthExpenses = expenses.filter((e) => e.expense_date >= start && e.expense_date <= end).reduce((s, e) => s + Number(e.amount), 0);
  const yearExpenses = expenses.filter((e) => e.expense_date >= yearStart() && e.expense_date <= end).reduce((s, e) => s + Number(e.amount), 0);

  // Six-month cost history
  const from = monthsAgo(5);
  const months: string[] = [];
  {
    const cursor = new Date(`${from}T00:00:00Z`);
    const last = new Date(`${end}T00:00:00Z`);
    while (cursor <= last) {
      months.push(cursor.toISOString().slice(0, 7));
      cursor.setUTCMonth(cursor.getUTCMonth() + 1);
    }
  }
  const expensesByMonth = months.map((month) => {
    const inMonth = (d: string) => d.slice(0, 7) === month;
    const fuelCost = fuel.filter((f) => inMonth(f.fuel_date)).reduce((s, f) => s + Number(f.total_cost), 0);
    const maintCost = maintenance.filter((m) => inMonth(m.service_date) && m.status !== 'CANCELLED').reduce((s, m) => s + Number(m.cost), 0);
    const other = expenses.filter((e) => inMonth(e.expense_date) && e.category !== 'FUEL' && e.category !== 'MAINTENANCE').reduce((s, e) => s + Number(e.amount), 0);
    return { month, fuel: fuelCost, maintenance: maintCost, other, total: fuelCost + maintCost + other };
  });

  const categoryMap = new Map<string, number>();
  for (const e of expenses.filter((x) => x.expense_date >= yearStart() && x.expense_date <= end)) {
    categoryMap.set(e.category, (categoryMap.get(e.category) ?? 0) + Number(e.amount));
  }

  const deptMap = new Map<string, number>();
  for (const e of employees) deptMap.set(e.department, (deptMap.get(e.department) ?? 0) + 1);

  const licenceAlerts = drivers
    .filter((d) => d.status === 'ACTIVE' && d.licence_expiry_date <= addDays(today, 30))
    .map((d) => ({
      driver_id: d.id,
      driver_code: d.driver_code,
      driver_name: d.employee_full_name,
      licence_category: d.licence_category,
      licence_expiry_date: d.licence_expiry_date,
      days_remaining: Math.round((new Date(`${d.licence_expiry_date}T00:00:00Z`).getTime() - new Date(`${today}T00:00:00Z`).getTime()) / 86_400_000),
      licence_status: (d.licence_expiry_date < today ? 'EXPIRED' : 'EXPIRING SOON') as 'EXPIRED' | 'EXPIRING SOON',
    }))
    .sort((a, b) => a.licence_expiry_date.localeCompare(b.licence_expiry_date));

  const { data: costRows } = await db()
    .from('v_vehicle_utilisation')
    .select('registration_number, make, model, total_cost, distance_km, cost_per_km')
    .order('total_cost', { ascending: false })
    .limit(5);

  const { data: auditRows } = await db()
    .from('audit_logs')
    .select('*')
    .order('timestamp', { ascending: false })
    .limit(6);

  const operational = vehicles.filter((v) => ['ASSIGNED', 'IN_SERVICE'].includes(v.status)).length;
  const assignable = vehicles.filter((v) => v.status !== 'RETIRED').length;

  return {
    kpis: {
      totalVehicles: vehicles.length,
      availableVehicles: byStatus.AVAILABLE ?? 0,
      assignedVehicles: byStatus.ASSIGNED ?? 0,
      inServiceVehicles: byStatus.IN_SERVICE ?? 0,
      maintenanceVehicles: byStatus.MAINTENANCE ?? 0,
      outOfServiceVehicles: byStatus.OUT_OF_SERVICE ?? 0,
      retiredVehicles: byStatus.RETIRED ?? 0,
      totalEmployees: employees.length,
      activeEmployees: employees.filter((e) => e.status === 'ACTIVE').length,
      totalDrivers: driverRows.length,
      activeDrivers: driverRows.filter((d) => d.status === 'ACTIVE').length,
      activeTrips: trips.filter((t) => t.status === 'ACTIVE').length,
      plannedTrips: trips.filter((t) => t.status === 'PLANNED').length,
      completedTripsThisMonth: completedThisMonth.length,
      activeAssignments: assignmentsRes.count ?? 0,
      overdueMaintenance: due.filter((d) => d.due_status === 'OVERDUE').length,
      dueSoonMaintenance: due.filter((d) => d.due_status === 'DUE SOON').length,
      expiringLicences: licenceAlerts.filter((l) => l.licence_status === 'EXPIRING SOON').length,
      expiredLicences: licenceAlerts.filter((l) => l.licence_status === 'EXPIRED').length,
      monthFuelCost: monthFuel,
      monthMaintenanceCost: monthMaintenance,
      monthTotalExpenses: monthExpenses,
      yearTotalExpenses: yearExpenses,
      monthDistanceKm: monthDistance,
      fleetUtilisationPct: assignable ? Math.round((operational / assignable) * 1000) / 10 : 0,
    },
    vehiclesByStatus: byStatus,
    vehiclesByType: byType,
    expensesByMonth,
    expensesByCategory: [...categoryMap.entries()].map(([category, amount]) => ({ category, amount })).sort((a, b) => b.amount - a.amount),
    employeesByDepartment: [...deptMap.entries()].map(([department, count]) => ({ department, count })).sort((a, b) => b.count - a.count),
    maintenanceAlerts: due.slice(0, 8),
    licenceAlerts: licenceAlerts.slice(0, 8),
    recentTrips,
    recentAudit: (auditRows ?? []) as AuditLog[],
    topCostVehicles: ((costRows ?? []) as { registration_number: string; make: string; model: string; total_cost: number; distance_km: number; cost_per_km: number | null }[])
      .map((r) => ({
        registration_number: r.registration_number,
        vehicle: `${r.make} ${r.model}`,
        total_cost: Number(r.total_cost),
        distance_km: Number(r.distance_km),
        cost_per_km: r.cost_per_km === null ? null : Number(r.cost_per_km),
      })),
  };
}
