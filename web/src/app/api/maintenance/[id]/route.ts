import { NextResponse } from 'next/server';
import { handler, guarded, readJson } from '@/lib/route';
import { deleteMaintenance, getMaintenance, updateMaintenance } from '@/lib/services/operations';

type Ctx = { params: Promise<{ id: string }> };

export const GET = handler<Ctx>(async (_session, _request, context) => {
  const { id } = await context.params;
  return NextResponse.json({ maintenance: await getMaintenance(Number(id)) });
});

export const PUT = guarded<Ctx>('MANAGE_MAINTENANCE', async (session, request, context) => {
  const { id } = await context.params;
  return NextResponse.json({ maintenance: await updateMaintenance(session, Number(id), await readJson(request)) });
});

export const DELETE = guarded<Ctx>('MANAGE_MAINTENANCE', async (session, _request, context) => {
  const { id } = await context.params;
  await deleteMaintenance(session, Number(id));
  return NextResponse.json({ ok: true });
});
