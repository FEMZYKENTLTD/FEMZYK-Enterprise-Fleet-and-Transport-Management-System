import { NextResponse } from 'next/server';
import { isConfigured } from '@/lib/db';
import { runDiagnostic } from '../route';

/**
 * Same diagnostic as /api/health/deep, with the secret carried in the path
 * instead of a header or query string:
 *
 *   GET /api/health/deep/<BOOTSTRAP_SECRET>
 */
export const dynamic = 'force-dynamic';
export const maxDuration = 60;

/** Never let a CDN or the browser replay a stale probe result. */
const NO_STORE = { 'Cache-Control': 'no-store, max-age=0, must-revalidate' } as const;

type Ctx = { params: Promise<{ secret: string[] }> };

export async function GET(request: Request, context: Ctx) {
  const expected = process.env.BOOTSTRAP_SECRET;
  const { secret } = await context.params;
  const provided = request.headers.get('x-bootstrap-secret') ?? secret?.[0];
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
