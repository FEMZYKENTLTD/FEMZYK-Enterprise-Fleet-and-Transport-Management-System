import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { login } from '@/lib/auth';
import { createSessionToken, SESSION_COOKIE, sessionCookieOptions } from '@/lib/session';
import { errorResponse } from '@/lib/errors';

export async function POST(request: Request) {
  try {
    const body = (await request.json().catch(() => ({}))) as { username?: string; password?: string };
    const { user } = await login(body.username ?? '', body.password ?? '');
    const token = createSessionToken(user);
    const store = await cookies();
    store.set(SESSION_COOKIE, token, sessionCookieOptions);
    return NextResponse.json({ user, mustChangePassword: user.mustChangePassword });
  } catch (error) {
    const { message, code } = errorResponse(error);
    return NextResponse.json({ error: message, code }, { status: (error as { status?: number }).status ?? 401 });
  }
}
