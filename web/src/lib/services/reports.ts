import 'server-only';
import { db, unwrap } from '../db';
import { toCsv } from '../format';
import { monthEnd, monthStart, yearStart } from './common';

/**
 * Report catalogue — the same ten report kinds as ReportService in the Java
 * application, with CSV / HTML output generated server-side.
 */
export const REPORT_KINDS = [
  { id: 'fleet', label: 'Fleet register', description: 'Every vehicle with status, mileage and assignment.' },
  { id: 'utilisation', label: 'Vehicle utilisation & cost', description: 'Distance, fuel, maintenance and cost per km per vehicle.' },
  { id: 'employees', label: 'Employees', description: 'Master list with department, position and status.' },
  { id: 'drivers', label: 'Drivers & licences', description: 'Licence class, expiry and days remaining.' },
  { id: 'assignments', label: 'Assignments', description: 'Vehicle ↔ driver assignment history.' },
  { id: 'trips', label: 'Trips', description: 'Trip log with odometer readings and distance.' },
  { id: 'maintenance', label: 'Maintenance', description: 'Service and repair history with costs.' },
  { id: 'fuel', label: 'Fuel', description: 'Fuel purchases, litres, prices and totals.' },
  { id: 'expenses', label: 'Expenses', description: 'All expenses, including system-generated ones.' },
  { id: 'monthly', label: 'Monthly cost summary', description: 'Fuel, maintenance and other costs by month.' },
  { id: 'audit', label: 'Audit log', description: 'Security and data-change trail.' },
] as const;

export type ReportKind = (typeof REPORT_KINDS)[number]['id'];

type Row = Record<string, unknown>;

export type ReportResult = {
  kind: ReportKind;
  title: string;
  generatedAt: string;
  columns: { key: string; label: string; align?: 'left' | 'right' }[];
  rows: Row[];
  totals?: Record<string, number>;
};

export async function buildReport(kind: ReportKind): Promise<ReportResult> {
  switch (kind) {
    case 'fleet': {
      const rows = unwrap<Row[]>(
        await db().from('v_vehicle_utilisation')
          .select('vehicle_code, registration_number, vehicle_type, make, model, year, status, current_mileage, trip_count, distance_km, fuel_cost, maintenance_cost, total_cost, cost_per_km')
          .order('registration_number'),
        'fleet report');
      return {
        kind, title: 'Fleet register', generatedAt: new Date().toISOString(),
        columns: [
          { key: 'vehicle_code', label: 'Code' }, { key: 'registration_number', label: 'Registration' },
          { key: 'vehicle_type', label: 'Type' }, { key: 'make', label: 'Make' }, { key: 'model', label: 'Model' },
          { key: 'year', label: 'Year' }, { key: 'status', label: 'Status' },
          { key: 'current_mileage', label: 'Mileage (km)', align: 'right' }, { key: 'trip_count', label: 'Trips', align: 'right' },
          { key: 'distance_km', label: 'Distance (km)', align: 'right' }, { key: 'total_cost', label: 'Total cost (₦)', align: 'right' },
          { key: 'cost_per_km', label: 'Cost / km (₦)', align: 'right' },
        ],
        rows: rows.map((r) => ({
          ...r,
          distance_km: Number(r.distance_km), total_cost: Number(r.total_cost),
          cost_per_km: r.cost_per_km === null ? '' : Number(r.cost_per_km),
        })),
        totals: {
          vehicles: rows.length,
          distance: rows.reduce((s, r) => s + Number(r.distance_km ?? 0), 0),
          cost: rows.reduce((s, r) => s + Number(r.total_cost ?? 0), 0),
        },
      };
    }
    case 'utilisation': {
      const [utilisation, efficiency] = await Promise.all([
        db().from('v_vehicle_utilisation').select('*').order('cost_per_km', { ascending: false, nullsFirst: false }),
        db().from('v_fuel_efficiency').select('*'),
      ]);
      const rows = unwrap<Row[]>(utilisation, 'utilisation report');
      const eff = new Map(unwrap<Row[]>(efficiency, 'efficiency report').map((e) => [Number(e.vehicle_id), e]));
      return {
        kind, title: 'Vehicle utilisation & cost', generatedAt: new Date().toISOString(),
        columns: [
          { key: 'registration_number', label: 'Registration' }, { key: 'make', label: 'Make' }, { key: 'model', label: 'Model' },
          { key: 'distance_km', label: 'Distance (km)', align: 'right' }, { key: 'fuel_litres', label: 'Fuel (L)', align: 'right' },
          { key: 'fuel_cost', label: 'Fuel ₦', align: 'right' }, { key: 'maintenance_cost', label: 'Maintenance ₦', align: 'right' },
          { key: 'other_cost', label: 'Other ₦', align: 'right' }, { key: 'total_cost', label: 'Total ₦', align: 'right' },
          { key: 'cost_per_km', label: 'Cost / km ₦', align: 'right' }, { key: 'litres_per_100km', label: 'L / 100 km', align: 'right' },
        ],
        rows: rows.map((r) => ({
          ...r,
          distance_km: Number(r.distance_km), fuel_litres: Number(r.fuel_litres), fuel_cost: Number(r.fuel_cost),
          maintenance_cost: Number(r.maintenance_cost), other_cost: Number(r.other_cost), total_cost: Number(r.total_cost),
          cost_per_km: r.cost_per_km === null ? '' : Number(r.cost_per_km),
          litres_per_100km: eff.get(Number(r.id))?.litres_per_100km ?? '',
        })),
        totals: {
          distance: rows.reduce((s, r) => s + Number(r.distance_km ?? 0), 0),
          fuel: rows.reduce((s, r) => s + Number(r.fuel_cost ?? 0), 0),
          maintenance: rows.reduce((s, r) => s + Number(r.maintenance_cost ?? 0), 0),
          total: rows.reduce((s, r) => s + Number(r.total_cost ?? 0), 0),
        },
      };
    }
    case 'employees': {
      const rows = unwrap<Row[]>(await db().from('employees').select('employee_code, first_name, last_name, department, position, phone, email, hire_date, salary, status').is('deleted_at', null).order('last_name'), 'employees report');
      return {
        kind, title: 'Employees', generatedAt: new Date().toISOString(),
        columns: [
          { key: 'employee_code', label: 'Code' }, { key: 'first_name', label: 'First name' }, { key: 'last_name', label: 'Last name' },
          { key: 'department', label: 'Department' }, { key: 'position', label: 'Position' }, { key: 'phone', label: 'Phone' },
          { key: 'email', label: 'E-mail' }, { key: 'hire_date', label: 'Hired' }, { key: 'salary', label: 'Salary ₦', align: 'right' },
          { key: 'status', label: 'Status' },
        ],
        rows: rows.map((r) => ({ ...r, salary: r.salary === null ? '' : Number(r.salary) })),
        totals: { employees: rows.length, payroll: rows.reduce((s, r) => s + Number(r.salary ?? 0), 0) },
      };
    }
    case 'drivers': {
      const rows = unwrap<Row[]>(
        await db().from('v_licence_alerts').select('driver_code, driver_name, licence_number, licence_category, licence_expiry_date, days_remaining, licence_status').order('licence_expiry_date'),
        'drivers report');
      return {
        kind, title: 'Drivers & licences', generatedAt: new Date().toISOString(),
        columns: [
          { key: 'driver_code', label: 'Code' }, { key: 'driver_name', label: 'Driver' }, { key: 'licence_number', label: 'Licence no.' },
          { key: 'licence_category', label: 'Class' }, { key: 'licence_expiry_date', label: 'Expires' },
          { key: 'days_remaining', label: 'Days left', align: 'right' }, { key: 'licence_status', label: 'Status' },
        ],
        rows,
        totals: { drivers: rows.length },
      };
    }
    case 'assignments': {
      const rows = unwrap<Row[]>(
        await db().from('vehicle_assignments')
          .select('assigned_at, returned_at, status, purpose, assigned_by, vehicles(registration_number), drivers(driver_code, employees(first_name, last_name))')
          .order('assigned_at', { ascending: false }).limit(1000),
        'assignments report');
      return {
        kind, title: 'Assignments', generatedAt: new Date().toISOString(),
        columns: [
          { key: 'assigned_at', label: 'Assigned' }, { key: 'returned_at', label: 'Returned' }, { key: 'status', label: 'Status' },
          { key: 'registration_number', label: 'Vehicle' }, { key: 'driver_code', label: 'Driver code' },
          { key: 'driver_name', label: 'Driver' }, { key: 'purpose', label: 'Purpose' }, { key: 'assigned_by', label: 'Assigned by' },
        ],
        rows: rows.map((r) => {
          const vehicle = r.vehicles as { registration_number?: string } | null;
          const driver = r.drivers as { driver_code?: string; employees?: { first_name: string; last_name: string } | null } | null;
          return {
            ...r,
            registration_number: vehicle?.registration_number ?? '—',
            driver_code: driver?.driver_code ?? '—',
            driver_name: driver?.employees ? `${driver.employees.first_name} ${driver.employees.last_name}`.trim() : '—',
          };
        }),
        totals: { assignments: rows.length },
      };
    }
    case 'trips': {
      const rows = unwrap<Row[]>(
        await db().from('trips')
          .select('trip_code, departure_time, return_time, origin, destination, purpose, start_mileage, end_mileage, fuel_used_litres, status, vehicles(registration_number), drivers(employees(first_name, last_name))')
          .order('departure_time', { ascending: false }).limit(1000),
        'trips report');
      return {
        kind, title: 'Trips', generatedAt: new Date().toISOString(),
        columns: [
          { key: 'trip_code', label: 'Trip' }, { key: 'departure_time', label: 'Departure' }, { key: 'return_time', label: 'Return' },
          { key: 'origin', label: 'Origin' }, { key: 'destination', label: 'Destination' }, { key: 'registration_number', label: 'Vehicle' },
          { key: 'driver_name', label: 'Driver' }, { key: 'start_mileage', label: 'Start km', align: 'right' },
          { key: 'end_mileage', label: 'End km', align: 'right' }, { key: 'distance', label: 'Distance', align: 'right' },
          { key: 'fuel_used_litres', label: 'Fuel (L)', align: 'right' }, { key: 'status', label: 'Status' },
        ],
        rows: rows.map((r) => {
          const vehicle = r.vehicles as { registration_number?: string } | null;
          const driver = r.drivers as { employees?: { first_name: string; last_name: string } | null } | null;
          const distance = r.end_mileage !== null && r.start_mileage !== null ? Number(r.end_mileage) - Number(r.start_mileage) : '';
          return {
            ...r,
            registration_number: vehicle?.registration_number ?? '—',
            driver_name: driver?.employees ? `${driver.employees.first_name} ${driver.employees.last_name}`.trim() : '—',
            distance,
          };
        }),
        totals: { trips: rows.length },
      };
    }
    case 'maintenance': {
      const rows = unwrap<Row[]>(
        await db().from('maintenance_records')
          .select('service_date, maintenance_type, description, provider, cost, mileage_at_service, next_service_date, next_service_mileage, status, vehicles(registration_number)')
          .order('service_date', { ascending: false }).limit(1000),
        'maintenance report');
      return {
        kind, title: 'Maintenance', generatedAt: new Date().toISOString(),
        columns: [
          { key: 'service_date', label: 'Date' }, { key: 'registration_number', label: 'Vehicle' },
          { key: 'maintenance_type', label: 'Type' }, { key: 'description', label: 'Description' }, { key: 'provider', label: 'Provider' },
          { key: 'cost', label: 'Cost ₦', align: 'right' }, { key: 'mileage_at_service', label: 'Odometer', align: 'right' },
          { key: 'next_service_date', label: 'Next service' }, { key: 'status', label: 'Status' },
        ],
        rows: rows.map((r) => ({ ...r, registration_number: (r.vehicles as { registration_number?: string } | null)?.registration_number ?? '—', cost: Number(r.cost) })),
        totals: { records: rows.length, cost: rows.reduce((s, r) => s + Number(r.cost ?? 0), 0) },
      };
    }
    case 'fuel': {
      const rows = unwrap<Row[]>(
        await db().from('fuel_records')
          .select('fuel_date, quantity_litres, unit_price, total_cost, mileage, station, receipt_reference, fuel_type, vehicles(registration_number), drivers(employees(first_name, last_name))')
          .order('fuel_date', { ascending: false }).limit(1500),
        'fuel report');
      return {
        kind, title: 'Fuel', generatedAt: new Date().toISOString(),
        columns: [
          { key: 'fuel_date', label: 'Date' }, { key: 'registration_number', label: 'Vehicle' }, { key: 'driver_name', label: 'Driver' },
          { key: 'quantity_litres', label: 'Litres', align: 'right' }, { key: 'unit_price', label: 'Unit ₦', align: 'right' },
          { key: 'total_cost', label: 'Total ₦', align: 'right' }, { key: 'mileage', label: 'Odometer', align: 'right' },
          { key: 'station', label: 'Station' }, { key: 'receipt_reference', label: 'Receipt' }, { key: 'fuel_type', label: 'Fuel' },
        ],
        rows: rows.map((r) => {
          const driver = r.drivers as { employees?: { first_name: string; last_name: string } | null } | null;
          return {
            ...r,
            registration_number: (r.vehicles as { registration_number?: string } | null)?.registration_number ?? '—',
            driver_name: driver?.employees ? `${driver.employees.first_name} ${driver.employees.last_name}`.trim() : '—',
            quantity_litres: Number(r.quantity_litres), unit_price: Number(r.unit_price), total_cost: Number(r.total_cost),
          };
        }),
        totals: {
          litres: rows.reduce((s, r) => s + Number(r.quantity_litres ?? 0), 0),
          cost: rows.reduce((s, r) => s + Number(r.total_cost ?? 0), 0),
        },
      };
    }
    case 'expenses': {
      const rows = unwrap<Row[]>(
        await db().from('expenses')
          .select('expense_date, category, description, vendor, reference, amount, source_fuel_record_id, source_maintenance_record_id, vehicles(registration_number)')
          .order('expense_date', { ascending: false }).limit(2000),
        'expenses report');
      return {
        kind, title: 'Expenses', generatedAt: new Date().toISOString(),
        columns: [
          { key: 'expense_date', label: 'Date' }, { key: 'category', label: 'Category' }, { key: 'registration_number', label: 'Vehicle' },
          { key: 'description', label: 'Description' }, { key: 'vendor', label: 'Vendor' }, { key: 'reference', label: 'Reference' },
          { key: 'amount', label: 'Amount ₦', align: 'right' }, { key: 'source', label: 'Source' },
        ],
        rows: rows.map((r) => ({
          ...r,
          registration_number: (r.vehicles as { registration_number?: string } | null)?.registration_number ?? '—',
          amount: Number(r.amount),
          source: r.source_fuel_record_id ? 'Fuel' : r.source_maintenance_record_id ? 'Maintenance' : 'Manual',
        })),
        totals: { records: rows.length, amount: rows.reduce((s, r) => s + Number(r.amount ?? 0), 0) },
      };
    }
    case 'monthly': {
      const rows = unwrap<Row[]>(
        await db().from('v_monthly_costs').select('*').gte('month', yearStart().slice(0, 7)).order('month'),
        'monthly report');
      return {
        kind, title: 'Monthly cost summary', generatedAt: new Date().toISOString(),
        columns: [
          { key: 'month', label: 'Month' }, { key: 'fuel_cost', label: 'Fuel ₦', align: 'right' },
          { key: 'maintenance_cost', label: 'Maintenance ₦', align: 'right' }, { key: 'other_cost', label: 'Other ₦', align: 'right' },
          { key: 'total_expenses', label: 'Total ₦', align: 'right' },
        ],
        rows: rows.map((r) => ({
          ...r,
          fuel_cost: Number(r.fuel_cost), maintenance_cost: Number(r.maintenance_cost),
          other_cost: Number(r.other_cost), total_expenses: Number(r.total_expenses),
        })),
        totals: {
          fuel: rows.reduce((s, r) => s + Number(r.fuel_cost ?? 0), 0),
          maintenance: rows.reduce((s, r) => s + Number(r.maintenance_cost ?? 0), 0),
          other: rows.reduce((s, r) => s + Number(r.other_cost ?? 0), 0),
          total: rows.reduce((s, r) => s + Number(r.total_expenses ?? 0), 0),
        },
      };
    }
    case 'audit': {
      const rows = unwrap<Row[]>(
        await db().from('audit_logs').select('timestamp, username, action, module, reference, description').order('timestamp', { ascending: false }).limit(2000),
        'audit report');
      return {
        kind, title: 'Audit log', generatedAt: new Date().toISOString(),
        columns: [
          { key: 'timestamp', label: 'When' }, { key: 'username', label: 'User' }, { key: 'action', label: 'Action' },
          { key: 'module', label: 'Module' }, { key: 'reference', label: 'Reference' }, { key: 'description', label: 'Description' },
        ],
        rows,
        totals: { events: rows.length },
      };
    }
    default:
      throw new Error(`Unknown report kind: ${kind as string}`);
  }
}

export function reportToCsv(report: ReportResult): string {
  return toCsv(report.rows, report.columns);
}

export function reportToHtml(report: ReportResult): string {
  const esc = (v: unknown) => String(v ?? '').replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c] as string));
  const money = (n: number) => new Intl.NumberFormat('en-NG', { style: 'currency', currency: 'NGN', maximumFractionDigits: 2 }).format(n);
  const head = report.columns.map((c) => `<th class="${c.align === 'right' ? 'r' : ''}">${esc(c.label)}</th>`).join('');
  const body = report.rows.map((r) => `<tr>${report.columns.map((c) => `<td class="${c.align === 'right' ? 'r' : ''}">${esc(r[c.key])}</td>`).join('')}</tr>`).join('\n');
  const totals = report.totals
    ? `<p class="totals">${Object.entries(report.totals).map(([k, v]) => `<span>${esc(k)}: <b>${money(Number(v))}</b></span>`).join(' · ')}</p>`
    : '';
  return `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><title>${esc(report.title)} — FEMZYK Fleet</title>
<style>
  body{font-family:system-ui,Segoe UI,Arial,sans-serif;color:#0f172a;margin:32px}
  h1{font-size:22px;margin:0 0 4px} .sub{color:#64748b;font-size:12px;margin-bottom:20px}
  table{border-collapse:collapse;width:100%;font-size:12px}
  th,td{border-bottom:1px solid #e2e8f0;padding:7px 8px;text-align:left}
  th{background:#f1f5f9;text-transform:uppercase;font-size:10px;letter-spacing:.06em;color:#475569}
  td.r,th.r{text-align:right;font-variant-numeric:tabular-nums}
  tr:nth-child(even) td{background:#fafbfc}
  .totals{margin-top:16px;font-size:12px;color:#334155}
  .brand{font-weight:800;letter-spacing:.12em;color:#1d4ed8;font-size:11px;text-transform:uppercase}
  @media print{body{margin:12mm}}
</style></head>
<body>
  <div class="brand">FEMZYK Enterprise Fleet &amp; Transport Management System</div>
  <h1>${esc(report.title)}</h1>
  <div class="sub">Generated ${new Date(report.generatedAt).toLocaleString('en-GB')} · ${report.rows.length} record${report.rows.length === 1 ? '' : 's'}</div>
  <table><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table>
  ${totals}
</body></html>`;
}

export function currentPeriodLabel(): string {
  return `${monthStart()} → ${monthEnd()}`;
}
