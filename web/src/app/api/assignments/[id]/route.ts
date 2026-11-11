import { NextResponse } from 'next/server';
import { guarded, readJson } from '@/lib/route';
import { cancelAssignment, returnVehicle } from '@/lib/services/assignments';

type Ctx = { params: Promise<{ id: string }> };

export const PUT = guarded<Ctx>('MANAGE_ASSIGNMENTS', async (session, request, context) => {
  const { id } = await context.params;
  const body = await readJson<Record<string, unknown>>(request);
  if (body.action === 'return') {
    return NextResponse.json({ assignment: await returnVehicle(session, Number(id), body.notes as string) });
  }
  if (body.action === 'cancel') {
    return NextResponse.json({ assignment: await cancelAssignment(session, Number(id), String(body.reason ?? '')) });
  }
  return NextResponse.json({ error: 'Unknown action.' }, { status: 400 });
});
