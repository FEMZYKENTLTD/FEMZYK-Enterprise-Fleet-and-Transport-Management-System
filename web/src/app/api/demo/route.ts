import { NextResponse } from 'next/server';
import { db, isConfigured } from '@/lib/db';
import { seedDemoData } from '@/lib/services/demo';

/**
 * Demo-data loader.
 *
 * Seeds the same kind of data set as DemoDataService in the desktop edition
 * (employees, drivers, vehicles, assignments, trips, maintenance, fuel,
 * expenses) through the normal service functions, so every rule and audit
 * entry is exercised. Idempotent: a marker row in app_settings stops a second
 * load. Requires the `x-bootstrap-secret` header when BOOTSTRAP_SECRET is set.
 */

const MARKER = '[DEMO DATA]';

function secretOk(request: Request): boolean {
  const expected = process.env.BOOTSTRAP_SECRET;
  if (!expected) return true;
  const provided = request.headers.get('x-bootstrap-secret') ?? new URL(request.url).searchParams.get('secret');
  return provided === expected;
}

export async function POST(request: Request) {
  if (!isConfigured()) return NextResponse.json({ error: 'Database is not configured.' }, { status: 503 });
  if (!secretOk(request)) return NextResponse.json({ error: 'Invalid bootstrap secret.' }, { status: 401 });

  const { data: loaded } = await db().from('app_settings').select('value').eq('key', 'demo.loaded_at').maybeSingle();
  if (loaded) return NextResponse.json({ ok: true, loaded: false, message: 'Demo data is already loaded.' });

  try {
    const result = await seedDemoData();
    await db().from('app_settings').upsert({ key: 'demo.loaded_at', value: new Date().toISOString() });
    await db().from('audit_logs').insert({
      username: 'system', action: 'DEMO_DATA', module: 'SYSTEM', reference: MARKER, description: 'Demonstration data set loaded',
    });
    return NextResponse.json({ ok: true, loaded: true, ...result });
  } catch (error) {
    return NextResponse.json({ error: (error as Error).message }, { status: 500 });
  }
}

export async function GET() {
  if (!isConfigured()) return NextResponse.json({ configured: false });
  const { data } = await db().from('app_settings').select('value').eq('key', 'demo.loaded_at').maybeSingle();
  return NextResponse.json({ configured: true, loaded: Boolean(data), loadedAt: data?.value ?? null, marker: MARKER });
}
