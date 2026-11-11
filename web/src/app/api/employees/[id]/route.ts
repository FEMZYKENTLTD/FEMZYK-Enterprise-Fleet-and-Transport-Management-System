import { NextResponse } from 'next/server';
import { handler, guarded, readJson } from '@/lib/route';
import { deleteEmployee, getEmployee, restoreEmployee, updateEmployee } from '@/lib/services/employees';

type Ctx = { params: Promise<{ id: string }> };

export const GET = handler<Ctx>(async (_session, _request, context) => {
  const { id } = await context.params;
  return NextResponse.json({ employee: await getEmployee(Number(id)) });
});

export const PUT = guarded<Ctx>('MANAGE_EMPLOYEES', async (session, request, context) => {
  const { id } = await context.params;
  const body = await readJson(request);
  if (body.action === 'restore') {
    await restoreEmployee(session, Number(id));
    return NextResponse.json({ employee: await getEmployee(Number(id)) });
  }
  return NextResponse.json({ employee: await updateEmployee(session, Number(id), body) });
});

export const DELETE = guarded<Ctx>('MANAGE_EMPLOYEES', async (session, _request, context) => {
  const { id } = await context.params;
  await deleteEmployee(session, Number(id));
  return NextResponse.json({ ok: true });
});
