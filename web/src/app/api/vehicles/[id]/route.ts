import { NextResponse } from 'next/server';
import { handler, guarded, readJson } from '@/lib/route';
import { changeVehicleStatus, deleteVehicle, getVehicle, restoreVehicle, updateVehicle } from '@/lib/services/vehicles';
import type { VehicleStatus } from '@/lib/domain';

type Ctx = { params: Promise<{ id: string }> };

export const GET = handler<Ctx>(async (_session, _request, context) => {
  const { id } = await context.params;
  return NextResponse.json({ vehicle: await getVehicle(Number(id)) });
});

export const PUT = guarded<Ctx>('MANAGE_VEHICLES', async (session, request, context) => {
  const { id } = await context.params;
  const body = await readJson(request);
  if (body.action === 'status') {
    const vehicle = await changeVehicleStatus(session, Number(id), String(body.status) as VehicleStatus, String(body.reason ?? ''));
    return NextResponse.json({ vehicle });
  }
  if (body.action === 'restore') {
    await restoreVehicle(session, Number(id));
    return NextResponse.json({ vehicle: await getVehicle(Number(id)) });
  }
  return NextResponse.json({ vehicle: await updateVehicle(session, Number(id), body) });
});

export const DELETE = guarded<Ctx>('MANAGE_VEHICLES', async (session, _request, context) => {
  const { id } = await context.params;
  await deleteVehicle(session, Number(id));
  return NextResponse.json({ ok: true });
});
