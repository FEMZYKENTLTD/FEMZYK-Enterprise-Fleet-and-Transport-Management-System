import { NextResponse } from 'next/server';
import { handler, guarded, readJson } from '@/lib/route';
import { cancelTrip, completeTrip, deleteTrip, getTrip, startTrip, updateTrip } from '@/lib/services/trips';

type Ctx = { params: Promise<{ id: string }> };

export const GET = handler<Ctx>(async (_session, _request, context) => {
  const { id } = await context.params;
  return NextResponse.json({ trip: await getTrip(Number(id)) });
});

export const PUT = guarded<Ctx>('MANAGE_TRIPS', async (session, request, context) => {
  const { id } = await context.params;
  const body = await readJson<Record<string, unknown>>(request);
  switch (body.action) {
    case 'start':
      return NextResponse.json({ trip: await startTrip(session, Number(id)) });
    case 'complete':
      return NextResponse.json({
        trip: await completeTrip(session, Number(id), {
          end_mileage: Number(body.end_mileage),
          fuel_used_litres: body.fuel_used_litres == null ? null : Number(body.fuel_used_litres),
          return_time: (body.return_time as string) ?? null,
          notes: (body.notes as string) ?? null,
        }),
      });
    case 'cancel':
      return NextResponse.json({ trip: await cancelTrip(session, Number(id), String(body.reason ?? '')) });
    default:
      return NextResponse.json({ trip: await updateTrip(session, Number(id), body) });
  }
});

export const DELETE = guarded<Ctx>('MANAGE_TRIPS', async (session, _request, context) => {
  const { id } = await context.params;
  await deleteTrip(session, Number(id));
  return NextResponse.json({ ok: true });
});
