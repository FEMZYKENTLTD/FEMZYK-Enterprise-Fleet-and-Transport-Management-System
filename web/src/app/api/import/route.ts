import { NextResponse } from 'next/server';
import { guarded, readJson } from '@/lib/route';
import { createVehicle } from '@/lib/services/vehicles';
import { createEmployee } from '@/lib/services/employees';

/**
 * CSV import (vehicles or employees) — the web counterpart of
 * CsvImportExportService. The browser parses the file and posts clean rows;
 * each row goes through the normal validation and audit path.
 */
export const POST = guarded('IMPORT_DATA', async (session, request) => {
  const body = await readJson<{ dataset?: string; rows?: Record<string, string>[] }>(request);
  const dataset = body.dataset ?? 'employees';
  const rows = Array.isArray(body.rows) ? body.rows : [];
  if (!rows.length) return NextResponse.json({ error: 'No rows to import.' }, { status: 400 });
  if (rows.length > 500) return NextResponse.json({ error: 'Import at most 500 rows at a time.' }, { status: 400 });

  const created: string[] = [];
  const failed: { row: number; reference: string; error: string }[] = [];

  for (let i = 0; i < rows.length; i++) {
    const row = rows[i];
    try {
      if (dataset === 'vehicles') {
        const vehicle = await createVehicle(session, {
          registration_number: row.registration_number ?? row['Registration'] ?? row['registration'] ?? '',
          vehicle_type: (row.vehicle_type ?? row['Type'] ?? 'CAR').toUpperCase(),
          make: row.make ?? row['Make'] ?? '',
          model: row.model ?? row['Model'] ?? '',
          year: row.year ?? row['Year'] ?? '',
          color: row.color ?? row['Colour'] ?? null,
          fuel_type: (row.fuel_type ?? row['Fuel'] ?? 'PETROL').toUpperCase(),
          capacity: row.capacity ?? row['Capacity'] ?? 0,
          current_mileage: row.current_mileage ?? row['Mileage'] ?? 0,
          acquisition_cost: row.acquisition_cost ?? 0,
          acquisition_date: row.acquisition_date ?? null,
          vin: row.vin ?? null,
          attribute_number: row.attribute_number ?? (String(row.vehicle_type ?? row['Type'] ?? 'CAR').toUpperCase() === 'CAR' ? 4 : null),
          attribute_text: row.attribute_text ?? null,
          status: 'AVAILABLE',
        });
        created.push(vehicle.registration_number);
      } else {
        const hire = row.hire_date ?? row['Hire date'] ?? row['hire'] ?? new Date().toISOString().slice(0, 10);
        const employee = await createEmployee(session, {
          first_name: row.first_name ?? row['First name'] ?? '',
          last_name: row.last_name ?? row['Last name'] ?? '',
          department: row.department ?? row['Department'] ?? 'Operations',
          position: row.position ?? row['Position'] ?? null,
          phone: row.phone ?? row['Phone'] ?? null,
          email: row.email ?? row['E-mail'] ?? null,
          hire_date: hire,
          salary: row.salary ?? row['Salary'] ?? null,
          status: (row.status ?? 'ACTIVE').toUpperCase(),
        });
        created.push(employee.employee_code);
      }
    } catch (error) {
      failed.push({
        row: i + 1,
        reference: row.registration_number ?? `${row.first_name ?? ''} ${row.last_name ?? ''}`.trim(),
        error: (error as Error).message,
      });
    }
  }
  return NextResponse.json({ imported: created.length, created, failed });
});
