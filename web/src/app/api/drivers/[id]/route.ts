import { NextResponse } from 'next/server';
import { handler, guarded, readJson } from '@/lib/route';
import { deleteDriver, getDriver, restoreDriver, updateDriver } from '@/lib/services/drivers';

type Ctx = { params: Promise<{ id: string }> };

export const GET = handler<Ctx>(async (_session, _request, context) => {
  const { id } = await context.params;
  return NextResponse.json({ driver: await getDriver(Number(id)) });
});

export const PUT = guarded<Ctx>('MANAGE_DRIVERS', async (session, request, context) => {
  const { id } = await context.params;
  const body = await readJson(request);
  if (body.action === 'restore') {
    await restoreDriver(session, Number(id));
    return NextResponse.json({ driver: await getDriver(Number(id)) });
  }
  return NextResponse.json({ driver: await updateDriver(session, Number(id), body) });
});

export const DELETE = guarded<Ctx>('MANAGE_DRIVERS', async (session, _request, context) => {
  const { id } = await context.params;
  await deleteDriver(session, Number(id));
  return NextResponse.json({ ok: true });
});
