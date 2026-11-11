import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { changeOwnPassword, findById, toSessionUser } from '@/lib/auth';
import { getSession, createSessionToken, SESSION_COOKIE, sessionCookieOptions } from '@/lib/session';
import { errorResponse } from '@/lib/errors';

export async function POST(request: Request) {
  try {
    const session = await getSession();
    if (!session) return NextResponse.json({ error: 'You must be signed in.' }, { status: 401 });
    const body = (await request.json()) as { currentPassword?: string; newPassword?: string };
    await changeOwnPassword(session.id, body.currentPassword ?? '', body.newPassword ?? '');
    const updated = await findById(session.id);
    if (updated) {
      const store = await cookies();
      store.set(SESSION_COOKIE, createSessionToken(toSessionUser(updated)), sessionCookieOptions);
    }
    return NextResponse.json({ ok: true });
  } catch (error) {
    const { message, code } = errorResponse(error);
    return NextResponse.json({ error: message, code }, { status: (error as { status?: number }).status ?? 400 });
  }
}
