import { NextResponse } from 'next/server';
import { guarded, query } from '@/lib/route';
import { toCsv } from '@/lib/format';
import { listVehicles } from '@/lib/services/vehicles';
import { listEmployees } from '@/lib/services/employees';
import { listDrivers } from '@/lib/services/drivers';
import { listTrips } from '@/lib/services/trips';
import { listFuel, listExpenses, listMaintenance } from '@/lib/services/operations';

/** CSV export of the current list views (the desktop edition's Export ▸ CSV). */
export const dynamic = 'force-dynamic';

export const GET = guarded('EXPORT_DATA', async (_session, request) => {
  const params = query(request);
  const dataset = params.get('dataset') ?? 'vehicles';
  let rows: Record<string, unknown>[] = [];
  let columns: { key: string; label: string }[] = [];

  switch (dataset) {
    case 'vehicles': {
      const vehicles = await listVehicles({});
      columns = [
        { key: 'vehicle_code', label: 'Code' }, { key: 'registration_number', label: 'Registration' },
        { key: 'vehicle_type', label: 'Type' }, { key: 'make', label: 'Make' }, { key: 'model', label: 'Model' },
        { key: 'year', label: 'Year' }, { key: 'color', label: 'Colour' }, { key: 'fuel_type', label: 'Fuel' },
        { key: 'capacity', label: 'Capacity' }, { key: 'current_mileage', label: 'Mileage' },
        { key: 'status', label: 'Status' }, { key: 'assigned_driver_name', label: 'Assigned driver' },
        { key: 'acquisition_date', label: 'Acquired' }, { key: 'acquisition_cost', label: 'Acquisition cost' },
      ];
      rows = vehicles as unknown as Record<string, unknown>[];
      break;
    }
    case 'employees': {
      const employees = await listEmployees({});
      columns = [
        { key: 'employee_code', label: 'Code' }, { key: 'first_name', label: 'First name' }, { key: 'last_name', label: 'Last name' },
        { key: 'department', label: 'Department' }, { key: 'position', label: 'Position' }, { key: 'phone', label: 'Phone' },
        { key: 'email', label: 'E-mail' }, { key: 'hire_date', label: 'Hire date' }, { key: 'salary', label: 'Salary' },
        { key: 'status', label: 'Status' },
      ];
      rows = employees as unknown as Record<string, unknown>[];
      break;
    }
    case 'drivers': {
      const drivers = await listDrivers({});
      columns = [
        { key: 'driver_code', label: 'Code' }, { key: 'employee_full_name', label: 'Driver' },
        { key: 'licence_number', label: 'Licence no.' }, { key: 'licence_category', label: 'Class' },
        { key: 'licence_expiry_date', label: 'Expires' }, { key: 'status', label: 'Status' },
        { key: 'employee_phone', label: 'Phone' },
      ];
      rows = drivers as unknown as Record<string, unknown>[];
      break;
    }
    case 'trips': {
      const trips = await listTrips({ limit: 2000 });
      columns = [
        { key: 'trip_code', label: 'Trip' }, { key: 'departure_time', label: 'Departure' }, { key: 'return_time', label: 'Return' },
        { key: 'origin', label: 'Origin' }, { key: 'destination', label: 'Destination' }, { key: 'registration_number', label: 'Vehicle' },
        { key: 'driver_name', label: 'Driver' }, { key: 'start_mileage', label: 'Start km' }, { key: 'end_mileage', label: 'End km' },
        { key: 'distance_km', label: 'Distance' }, { key: 'status', label: 'Status' },
      ];
      rows = trips as unknown as Record<string, unknown>[];
      break;
    }
    case 'fuel': {
      const fuel = await listFuel({});
      columns = [
        { key: 'fuel_date', label: 'Date' }, { key: 'registration_number', label: 'Vehicle' }, { key: 'driver_name', label: 'Driver' },
        { key: 'quantity_litres', label: 'Litres' }, { key: 'unit_price', label: 'Unit price' }, { key: 'total_cost', label: 'Total' },
        { key: 'mileage', label: 'Odometer' }, { key: 'station', label: 'Station' }, { key: 'receipt_reference', label: 'Receipt' },
      ];
      rows = fuel as unknown as Record<string, unknown>[];
      break;
    }
    case 'maintenance': {
      const maintenance = await listMaintenance({});
      columns = [
        { key: 'service_date', label: 'Date' }, { key: 'registration_number', label: 'Vehicle' }, { key: 'maintenance_type', label: 'Type' },
        { key: 'description', label: 'Description' }, { key: 'provider', label: 'Provider' }, { key: 'cost', label: 'Cost' },
        { key: 'mileage_at_service', label: 'Odometer' }, { key: 'next_service_date', label: 'Next service' }, { key: 'status', label: 'Status' },
      ];
      rows = maintenance as unknown as Record<string, unknown>[];
      break;
    }
    case 'expenses': {
      const expenses = await listExpenses({});
      columns = [
        { key: 'expense_date', label: 'Date' }, { key: 'category', label: 'Category' }, { key: 'registration_number', label: 'Vehicle' },
        { key: 'description', label: 'Description' }, { key: 'vendor', label: 'Vendor' }, { key: 'reference', label: 'Reference' },
        { key: 'amount', label: 'Amount' }, { key: 'system_generated', label: 'Auto-generated' },
      ];
      rows = expenses as unknown as Record<string, unknown>[];
      break;
    }
    default:
      return NextResponse.json({ error: `Unknown dataset: ${dataset}` }, { status: 400 });
  }

  const csv = toCsv(rows, columns);
  return new NextResponse(`\uFEFF${csv}`, {
    headers: {
      'Content-Type': 'text/csv; charset=utf-8',
      'Content-Disposition': `attachment; filename="femzyk-${dataset}-${new Date().toISOString().slice(0, 10)}.csv"`,
      'Cache-Control': 'no-store',
    },
  });
});
