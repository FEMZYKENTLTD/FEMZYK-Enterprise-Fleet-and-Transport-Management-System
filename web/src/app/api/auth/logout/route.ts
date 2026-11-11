import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { SESSION_COOKIE, getSession } from '@/lib/session';
import { audit } from '@/lib/auth';

export async function POST() {
  const session = await getSession();
  if (session) await audit(session.username, 'LOGOUT', 'AUTH', session.username, 'User signed out');
  const store = await cookies();
  store.delete(SESSION_COOKIE);
  return NextResponse.json({ ok: true });
}
