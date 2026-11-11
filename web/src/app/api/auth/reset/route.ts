import { NextResponse } from 'next/server';
import { resetPasswordWithEmail } from '@/lib/auth';
import { errorResponse } from '@/lib/errors';

export async function POST(request: Request) {
  try {
    const body = (await request.json()) as { username?: string; email?: string; newPassword?: string };
    await resetPasswordWithEmail(body.username ?? '', body.email ?? '', body.newPassword ?? '');
    return NextResponse.json({ ok: true });
  } catch (error) {
    const { message, code } = errorResponse(error);
    return NextResponse.json({ error: message, code }, { status: (error as { status?: number }).status ?? 400 });
  }
}
