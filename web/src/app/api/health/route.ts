import { NextResponse } from 'next/server';
import { db, isConfigured } from '@/lib/db';

/**
 * Public health probe: proves the deployment can reach PostgreSQL and that the
 * operational tables are populated. Read-only, exposes no credentials and no
 * row-level detail — safe to leave public.
 */
export const dynamic = 'force-dynamic';

export async function GET() {
  const started = Date.now();
  if (!isConfigured()) {
    return NextResponse.json({ ok: false, database: 'not configured' }, { status: 503 });
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
    });
  } catch (error) {
    return NextResponse.json({ ok: false, database: 'error', error: (error as Error).message }, { status: 500 });
  }
}
