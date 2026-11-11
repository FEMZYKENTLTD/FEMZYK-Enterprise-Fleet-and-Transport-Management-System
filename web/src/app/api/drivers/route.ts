import { NextResponse } from 'next/server';
import { handler, guarded, query, readJson } from '@/lib/route';
import { createDriver, listDrivers } from '@/lib/services/drivers';

export const GET = handler(async (_session, request) => {
  const params = query(request);
  const drivers = await listDrivers({
    q: params.get('q') ?? undefined,
    status: params.get('status') ?? undefined,
    includeDeleted: params.get('deleted') === '1',
  });
  return NextResponse.json({ drivers });
});

export const POST = guarded('MANAGE_DRIVERS', async (session, request) => {
  const driver = await createDriver(session, await readJson(request));
  return NextResponse.json({ driver }, { status: 201 });
});
