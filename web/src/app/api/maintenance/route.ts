import { NextResponse } from 'next/server';
import { handler, guarded, query, readJson } from '@/lib/route';
import { createMaintenance, listMaintenance, maintenanceDue } from '@/lib/services/operations';

export const GET = handler(async (_session, request) => {
  const params = query(request);
  if (params.get('due') === '1') return NextResponse.json({ due: await maintenanceDue() });
  const maintenance = await listMaintenance({
    q: params.get('q') ?? undefined,
    status: params.get('status') ?? undefined,
    type: params.get('type') ?? undefined,
    vehicleId: params.get('vehicleId') ? Number(params.get('vehicleId')) : undefined,
  });
  return NextResponse.json({ maintenance });
});

export const POST = guarded('MANAGE_MAINTENANCE', async (session, request) => {
  const record = await createMaintenance(session, await readJson(request));
  return NextResponse.json({ maintenance: record }, { status: 201 });
});
