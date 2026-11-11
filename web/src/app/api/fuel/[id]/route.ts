import { NextResponse } from 'next/server';
import { handler, guarded, readJson } from '@/lib/route';
import { deleteFuel, getFuel, updateFuel } from '@/lib/services/operations';

type Ctx = { params: Promise<{ id: string }> };

export const GET = handler<Ctx>(async (_session, _request, context) => {
  const { id } = await context.params;
  return NextResponse.json({ fuel: await getFuel(Number(id)) });
});

export const PUT = guarded<Ctx>('MANAGE_FUEL', async (session, request, context) => {
  const { id } = await context.params;
  return NextResponse.json({ fuel: await updateFuel(session, Number(id), await readJson(request)) });
});

export const DELETE = guarded<Ctx>('MANAGE_FUEL', async (session, _request, context) => {
  const { id } = await context.params;
  await deleteFuel(session, Number(id));
  return NextResponse.json({ ok: true });
});
