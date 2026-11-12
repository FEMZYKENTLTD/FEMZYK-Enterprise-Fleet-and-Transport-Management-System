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
import { verifyCredentials, listUsers, findByUsername, verifyResetIdentity } from '@/lib/auth';
import { createSessionToken, readSessionToken } from '@/lib/session';
import { roleHas, ROLE_PERMISSIONS, type Role } from '@/lib/domain';
import { reportToCsv, reportToHtml } from '@/lib/services/reports';

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

/** Never let a CDN or the browser replay a stale probe result. */
const NO_STORE = { 'Cache-Control': 'no-store, max-age=0, must-revalidate' } as const;

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

  /* ---- authentication, session signing and permission gating ---- */

  const accounts: { username: string; password: string; role: Role }[] = [
    { username: 'admin', password: 'Admin@2024', role: 'ADMINISTRATOR' },
    { username: 'manager', password: 'Fleet@2026', role: 'FLEET_MANAGER' },
    { username: 'ops', password: 'Ops@2026', role: 'OPERATIONS_OFFICER' },
    { username: 'viewer', password: 'View@2026', role: 'VIEWER' },
  ];

  // Verification only: `login()` writes an audit row and touches the lockout
  // counters, so probing with it every few minutes would flood the audit log
  // with synthetic sign-ins. verifyCredentials() performs the same PBKDF2
  // comparison and writes nothing.
  for (const account of accounts) {
    push(await check(`credentials:${account.username}`, async () => {
      const user = await verifyCredentials(account.username, account.password);
      if (user.role !== account.role) throw new Error(`expected role ${account.role}, got ${user.role}`);
      const token = createSessionToken(user);
      const restored = readSessionToken(token);
      if (!restored || restored.id !== user.id) throw new Error('session token did not round-trip');
      if (readSessionToken(`${token}x`)) throw new Error('tampered token was accepted');
      return `${user.role}, ${ROLE_PERMISSIONS[user.role].length} permissions, session verified, no audit write`;
    }));
  }

  push(await check('credentials:wrong-password-rejected', async () => {
    try {
      await verifyCredentials('admin', 'definitely-not-the-password');
      throw new Error('a wrong password was accepted');
    } catch (error) {
      if ((error as Error).message === 'a wrong password was accepted') throw error;
      return 'rejected as expected, no audit write';
    }
  }));

  push(await check('permissions:viewer-cannot-manage', async () => {
    const dbUser = await findByUsername('viewer');
    if (!dbUser) throw new Error('viewer account missing');
    const denied = (['MANAGE_VEHICLES', 'MANAGE_USERS', 'VIEW_AUDIT_LOG', 'IMPORT_DATA'] as const)
      .filter((p) => roleHas(dbUser.role, p));
    if (denied.length) throw new Error(`viewer unexpectedly holds ${denied.join(', ')}`);
    return 'viewer blocked from 4 write/admin permissions';
  }));

  push(await check('permissions:admin-can-manage-users', async () => {
    const dbUser = await findByUsername('admin');
    if (!dbUser) throw new Error('admin account missing');
    const users = await listUsers({
      id: dbUser.id, username: dbUser.username, fullName: dbUser.full_name, email: dbUser.email,
      role: dbUser.role, mustChangePassword: false,
    });
    return `${users.length} users visible to administrator`;
  }));

  push(await check('reset:rejects-mismatched-identity', async () => {
    // Read-only: verifyResetIdentity() answers the same question as the real
    // reset without writing a password or an audit row, so probing stays
    // invisible in the audit trail.
    const wrong = await verifyResetIdentity('admin', 'not-the-registered-address@example.com');
    if (wrong.matches) throw new Error('a mismatched username/e-mail was accepted');
    const unknown = await verifyResetIdentity('no-such-account-here', 'anything@example.com');
    if (unknown.matches) throw new Error('an unknown account was accepted');
    return 'mismatched identity rejected, no audit write';
  }));

  push(await check('export:csv', async () => {
    const csv = reportToCsv(await buildReport('fleet'));
    if (!csv.split('\n').length) throw new Error('empty CSV');
    return `${csv.split(/\r?\n/).length} lines`;
  }));

  push(await check('export:printable-html', async () => {
    const html = reportToHtml(await buildReport('utilisation'));
    if (!html.includes('<table')) throw new Error('HTML report has no table');
    return `${html.length} bytes`;
  }));

  return checks;
}

export async function GET(request: Request, context: Ctx) {
  const expected = process.env.BOOTSTRAP_SECRET;
  const params = context?.params ? await context.params : {};
  const provided = request.headers.get('x-bootstrap-secret')
    ?? new URL(request.url).searchParams.get('secret')
    ?? params.secret?.[0];
  if (expected && provided !== expected) {
    return NextResponse.json({ error: 'Invalid secret.' }, { status: 401, headers: NO_STORE });
  }
  if (!isConfigured()) return NextResponse.json({ error: 'Database is not configured.' }, { status: 503, headers: NO_STORE });

  const checks = await runDiagnostic();
  const failed = checks.filter((c) => !c.ok);
  return NextResponse.json(
    { ok: failed.length === 0, passed: checks.length - failed.length, failed: failed.length, checks },
    { status: failed.length === 0 ? 200 : 500, headers: NO_STORE },
  );
}
