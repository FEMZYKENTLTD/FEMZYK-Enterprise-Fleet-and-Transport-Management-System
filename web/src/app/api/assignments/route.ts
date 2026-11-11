import { NextResponse } from 'next/server';
import { handler, guarded, query, readJson } from '@/lib/route';
import { assignVehicle, listAssignments, reassignVehicle } from '@/lib/services/assignments';

export const GET = handler(async (_session, request) => {
  const params = query(request);
  const assignments = await listAssignments({
    status: params.get('status') ?? undefined,
    q: params.get('q') ?? undefined,
    vehicleId: params.get('vehicleId') ? Number(params.get('vehicleId')) : undefined,
    driverId: params.get('driverId') ? Number(params.get('driverId')) : undefined,
  });
  return NextResponse.json({ assignments });
});

export const POST = guarded('MANAGE_ASSIGNMENTS', async (session, request) => {
  const body = await readJson<Record<string, unknown>>(request);
  const assignment = body.mode === 'reassign'
    ? await reassignVehicle(session, {
        vehicle_id: Number(body.vehicle_id), driver_id: Number(body.driver_id),
        purpose: body.purpose as string, notes: body.notes as string,
      })
    : await assignVehicle(session, {
        vehicle_id: Number(body.vehicle_id), driver_id: Number(body.driver_id),
        purpose: body.purpose as string, notes: body.notes as string, assigned_at: body.assigned_at as string,
      });
  return NextResponse.json({ assignment }, { status: 201 });
});
