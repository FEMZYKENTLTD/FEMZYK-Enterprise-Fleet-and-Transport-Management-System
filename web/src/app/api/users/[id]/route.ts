import { NextResponse } from 'next/server';
import { guarded, readJson } from '@/lib/route';
import { adminResetPassword, deleteUser, listUsers, updateUser } from '@/lib/auth';

type Ctx = { params: Promise<{ id: string }> };

export const PUT = guarded<Ctx>('MANAGE_USERS', async (session, request, context) => {
  const { id } = await context.params;
  const body = await readJson<Record<string, unknown>>(request);
  if (body.action === 'reset-password') {
    await adminResetPassword(session, Number(id), String(body.newPassword ?? ''));
  } else {
    await updateUser(session, Number(id), {
      email: String(body.email ?? ''),
      fullName: String(body.fullName ?? ''),
      role: body.role as never,
      active: Boolean(body.active),
    });
  }
  return NextResponse.json({ users: await listUsers(session) });
});

export const DELETE = guarded<Ctx>('MANAGE_USERS', async (session, _request, context) => {
  const { id } = await context.params;
  await deleteUser(session, Number(id));
  return NextResponse.json({ users: await listUsers(session) });
});
