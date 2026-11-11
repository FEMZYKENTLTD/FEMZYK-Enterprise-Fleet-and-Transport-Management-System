import { NextResponse } from 'next/server';
import { guarded, readJson } from '@/lib/route';
import { createUser, listUsers } from '@/lib/auth';

export const GET = guarded('MANAGE_USERS', async (session) => {
  return NextResponse.json({ users: await listUsers(session) });
});

export const POST = guarded('MANAGE_USERS', async (session, request) => {
  await createUser(session, await readJson(request));
  return NextResponse.json({ users: await listUsers(session) }, { status: 201 });
});
