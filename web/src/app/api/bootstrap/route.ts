import { NextResponse } from 'next/server';
import { db, isConfigured } from '@/lib/db';
import { ensureDefaultAdministrator } from '@/lib/auth';

/**
 * One-time bootstrap: creates the default administrator if the users table is
 * empty. Safe to call repeatedly. If BOOTSTRAP_SECRET is set, the header
 * `x-bootstrap-secret` must match (or the request must come from a signed-in
 * administrator).
 */
export async function POST(request: Request) {
  if (!isConfigured()) {
    return NextResponse.json({ error: 'Supabase environment variables are not configured.' }, { status: 503 });
  }
  const expected = process.env.BOOTSTRAP_SECRET;
  const provided = request.headers.get('x-bootstrap-secret') ?? new URL(request.url).searchParams.get('secret');
  if (expected && provided !== expected) {
    return NextResponse.json({ error: 'Invalid bootstrap secret.' }, { status: 401 });
  }
  try {
    const { count } = await db().from('users').select('id', { count: 'exact', head: true });
    const result = await ensureDefaultAdministrator();
    return NextResponse.json({
      ok: true,
      created: result.created,
      users: (count ?? 0) + (result.created ? 1 : 0),
      message: result.created ? 'Default administrator created (admin / Admin@2024).' : 'Administrator already exists.',
    });
  } catch (error) {
    return NextResponse.json({ error: (error as Error).message }, { status: 500 });
  }
}

export async function GET() {
  if (!isConfigured()) return NextResponse.json({ configured: false });
  try {
    const { count, error } = await db().from('users').select('id', { count: 'exact', head: true });
    if (error) throw error;
    return NextResponse.json({ configured: true, users: count ?? 0, ready: (count ?? 0) > 0 });
  } catch (error) {
    return NextResponse.json({ configured: true, ready: false, error: (error as Error).message });
  }
}
