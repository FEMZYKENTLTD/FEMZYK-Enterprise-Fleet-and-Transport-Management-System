import { NextResponse } from 'next/server';
import { handler, guarded, query, readJson } from '@/lib/route';
import { createFuel, listFuel } from '@/lib/services/operations';

export const GET = handler(async (_session, request) => {
  const params = query(request);
  const fuel = await listFuel({
    q: params.get('q') ?? undefined,
    vehicleId: params.get('vehicleId') ? Number(params.get('vehicleId')) : undefined,
    from: params.get('from') ?? undefined,
    to: params.get('to') ?? undefined,
  });
  return NextResponse.json({ fuel });
});

export const POST = guarded('MANAGE_FUEL', async (session, request) => {
  const record = await createFuel(session, await readJson(request));
  return NextResponse.json({ fuel: record }, { status: 201 });
});
