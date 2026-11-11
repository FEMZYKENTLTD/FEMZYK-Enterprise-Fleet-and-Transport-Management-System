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

type Ctx = { params: Promise<{ secret: string[] }> };

export async function GET(request: Request, context: Ctx) {
  const expected = process.env.BOOTSTRAP_SECRET;
  const { secret } = await context.params;
  const provided = request.headers.get('x-bootstrap-secret') ?? secret?.[0];
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
