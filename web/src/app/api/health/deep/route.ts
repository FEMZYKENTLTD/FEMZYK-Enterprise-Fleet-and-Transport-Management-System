import { NextResponse } from 'next/server';
import { isConfigured } from '@/lib/db';
import { dashboardSnapshot } from '@/lib/services/dashboard';
import { buildReport, REPORT_KINDS } from '@/lib/services/reports';
import { listVehicles } from '@/lib/services/vehicles';
import { listEmployees } from '@/lib/services/employees';
import { listDrivers } from '@/lib/services/drivers';
import { listAssignments } from '@/lib/services/assignments';
import { listTrips } from '@/lib/services/trips';
import { listMaintenance, maintenanceDue, listFuel, listExpenses } from '@/lib/services/operations';

/**
 * Deep diagnostic: runs every read-side service the screens depend on and
 * reports counts or the failure message. Secret-gated (BOOTSTRAP_SECRET) and
 * returns no row content, only shapes — safe for a public deployment.
 *
 * The secret may be supplied as `?secret=…`, the `x-bootstrap-secret` header,
 * or the last path segment (`/api/health/deep/<secret>`) so it can be checked
 * from tools that cannot set headers.
 */
export const dynamic = 'force-dynamic';
export const maxDuration = 60;

type Check = { name: string; ok: boolean; detail: string };
type Ctx = { params: Promise<{ secret?: string[] }> };

async function check(name: string, fn: () => Promise<string>): Promise<Check> {
  try {
    return { name, ok: true, detail: await fn() };
  } catch (error) {
    return { name, ok: false, detail: (error as Error).message };
  }
}

export async function runDiagnostic() {
  const checks: Check[] = [];
  const push = (c: Check) => { checks.push(c); return c; };

  push(await check('dashboard', async () => {
    const d = await dashboardSnapshot();
    return `${d.kpis.totalVehicles} vehicles, ${d.kpis.activeAssignments} active assignments, ${d.kpis.activeTrips} active trips, ` +
      `${d.kpis.overdueMaintenance} overdue services, ${d.kpis.expiringLicences} licences expiring, ` +
      `${d.topCostVehicles.length} cost rows, ${d.recentAudit.length} audit rows`;
  }));

  push(await check('vehicles', async () => `${(await listVehicles({})).length} rows`));
  push(await check('employees', async () => `${(await listEmployees({})).length} rows`));
  push(await check('drivers', async () => `${(await listDrivers({})).length} rows`));
  push(await check('assignments', async () => `${(await listAssignments({})).length} rows`));
  push(await check('trips', async () => `${(await listTrips({ limit: 300 })).length} rows`));
  push(await check('maintenance', async () => `${(await listMaintenance({})).length} rows`));
  push(await check('maintenanceDue', async () => `${(await maintenanceDue()).length} alerts`));
  push(await check('fuel', async () => `${(await listFuel({})).length} rows`));
  push(await check('expenses', async () => `${(await listExpenses({})).length} rows`));

  for (const kind of REPORT_KINDS) {
    push(await check(`report:${kind.id}`, async () => {
      const report = await buildReport(kind.id);
      return `${report.rows.length} rows, ${report.columns.length} columns`;
    }));
  }
  return checks;
}

export async function GET(request: Request, context: Ctx) {
  const expected = process.env.BOOTSTRAP_SECRET;
  const params = context?.params ? await context.params : {};
  const provided = request.headers.get('x-bootstrap-secret')
    ?? new URL(request.url).searchParams.get('secret')
    ?? params.secret?.[0];
  if (expected && provided !== expected) {
    return NextResponse.json({ error: 'Invalid secret.' }, { status: 401 });
  }
  if (!isConfigured()) return NextResponse.json({ error: 'Database is not configured.' }, { status: 503 });

  const checks = await runDiagnostic();
  const failed = checks.filter((c) => !c.ok);
  return NextResponse.json(
    { ok: failed.length === 0, passed: checks.length - failed.length, failed: failed.length, checks },
    { status: failed.length === 0 ? 200 : 500 },
  );
}
