import { NextResponse } from 'next/server';
import { handler, guarded, query, readJson } from '@/lib/route';
import { createTrip, listTrips } from '@/lib/services/trips';

export const GET = handler(async (_session, request) => {
  const params = query(request);
  const trips = await listTrips({
    q: params.get('q') ?? undefined,
    status: params.get('status') ?? undefined,
    vehicleId: params.get('vehicleId') ? Number(params.get('vehicleId')) : undefined,
    driverId: params.get('driverId') ? Number(params.get('driverId')) : undefined,
    from: params.get('from') ?? undefined,
    to: params.get('to') ?? undefined,
    limit: params.get('limit') ? Number(params.get('limit')) : undefined,
  });
  return NextResponse.json({ trips });
});

export const POST = guarded('MANAGE_TRIPS', async (session, request) => {
  const body = await readJson<Record<string, unknown>>(request);
  const trip = await createTrip(session, body, Boolean(body.start_now));
  return NextResponse.json({ trip }, { status: 201 });
});
