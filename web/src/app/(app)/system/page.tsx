import { getSession } from '@/lib/session';
import { redirect } from 'next/navigation';
import { roleHas } from '@/lib/domain';
import { isConfigured } from '@/lib/db';
import { db } from '@/lib/db';
import { SystemClient } from './SystemClient';

export const dynamic = 'force-dynamic';

async function countRows(table: string): Promise<number> {
  const { count } = await db().from(table).select('id', { count: 'exact', head: true });
  return count ?? 0;
}

export default async function SystemPage() {
  const session = await getSession();
  if (!session) redirect('/login');

  let stats: { table: string; label: string; rows: number }[] = [];
  let demoLoadedAt: string | null = null;
  let auditRows = 0;

  if (isConfigured()) {
    try {
      const [vehicles, employees, drivers, assignments, trips, maintenance, fuel, expenses, users, audit] = await Promise.all([
        countRows('vehicles'), countRows('employees'), countRows('drivers'), countRows('vehicle_assignments'),
        countRows('trips'), countRows('maintenance_records'), countRows('fuel_records'), countRows('expenses'),
        countRows('users'), countRows('audit_logs'),
      ]);
      auditRows = audit;
      stats = [
        { table: 'vehicles', label: 'Vehicles', rows: vehicles },
        { table: 'employees', label: 'Employees', rows: employees },
        { table: 'drivers', label: 'Drivers', rows: drivers },
        { table: 'assignments', label: 'Assignments', rows: assignments },
        { table: 'trips', label: 'Trips', rows: trips },
        { table: 'maintenance', label: 'Maintenance', rows: maintenance },
        { table: 'fuel', label: 'Fuel records', rows: fuel },
        { table: 'expenses', label: 'Expenses', rows: expenses },
        { table: 'users', label: 'Users', rows: users },
      ];
      const { data } = await db().from('app_settings').select('value').eq('key', 'demo.loaded_at').maybeSingle();
      demoLoadedAt = data?.value ?? null;
    } catch {
      stats = [];
    }
  }

  return (
    <SystemClient
      canExport={roleHas(session.role, 'EXPORT_DATA')}
      canImport={roleHas(session.role, 'IMPORT_DATA')}
      configured={isConfigured()}
      stats={stats}
      auditRows={auditRows}
      demoLoadedAt={demoLoadedAt}
      region={process.env.VERCEL_REGION ?? 'local'}
      commit={process.env.VERCEL_GIT_COMMIT_SHA?.slice(0, 7) ?? 'development'}
    />
  );
}
