import { NextResponse } from 'next/server';
import { handler, guarded, query, readJson } from '@/lib/route';
import { createVehicle, listVehicles } from '@/lib/services/vehicles';

export const GET = handler(async (_session, request) => {
  const params = query(request);
  const vehicles = await listVehicles({
    q: params.get('q') ?? undefined,
    type: params.get('type') ?? undefined,
    status: params.get('status') ?? undefined,
    fuel: params.get('fuel') ?? undefined,
    includeDeleted: params.get('deleted') === '1',
  });
  return NextResponse.json({ vehicles });
});

export const POST = guarded('MANAGE_VEHICLES', async (session, request) => {
  const vehicle = await createVehicle(session, await readJson(request));
  return NextResponse.json({ vehicle }, { status: 201 });
});
