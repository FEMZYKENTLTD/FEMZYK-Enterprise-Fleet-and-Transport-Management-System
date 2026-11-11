import { NextResponse } from 'next/server';
import { db, isConfigured } from '@/lib/db';
/** Never let a CDN or the browser replay a stale probe result. */
const NO_STORE = { 'Cache-Control': 'no-store, max-age=0, must-revalidate' } as const;


/**
 * Public health probe: proves the deployment can reach PostgreSQL and that the
 * operational tables are populated. Read-only, exposes no credentials and no
 * row-level detail — safe to leave public.
 */
export const dynamic = 'force-dynamic';

export async function GET() {
  const started = Date.now();
  if (!isConfigured()) {
    return NextResponse.json({ ok: false, database: 'not configured' }, { status: 503, headers: NO_STORE });
  }
  const tables = ['vehicles', 'employees', 'drivers', 'trips', 'fuel_records', 'maintenance_records', 'expenses', 'users'];
  try {
    const counts: Record<string, number> = {};
    for (const table of tables) {
      const { count, error } = await db().from(table).select('id', { count: 'exact', head: true });
      if (error) throw new Error(`${table}: ${error.message}`);
      counts[table] = count ?? 0;
    }
    return NextResponse.json({
      ok: true,
      database: 'connected',
      seeded: counts.vehicles > 0 && counts.users > 0,
      counts,
      latencyMs: Date.now() - started,
      checkedAt: new Date().toISOString(),
    }, { headers: NO_STORE });
  } catch (error) {
    return NextResponse.json({ ok: false, database: 'error', error: (error as Error).message }, { status: 500, headers: NO_STORE });
  }
}
