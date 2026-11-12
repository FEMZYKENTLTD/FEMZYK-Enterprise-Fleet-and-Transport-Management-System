import { NextResponse } from 'next/server';
import { guarded } from '@/lib/route';
import { listRegistrationRequests } from '@/lib/services/registration';

/** Administrator: the review queue. */
export const GET = guarded('MANAGE_USERS', async (session) => {
  const requests = await listRegistrationRequests(session);
  return NextResponse.json(
    {
      requests,
      pending: requests.filter((r) => r.status === 'PENDING').length,
    },
    { headers: { 'Cache-Control': 'no-store' } },
  );
});
