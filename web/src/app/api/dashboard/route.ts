import { NextResponse } from 'next/server';
import { handler } from '@/lib/route';
import { dashboardSnapshot } from '@/lib/services/dashboard';

export const dynamic = 'force-dynamic';

export const GET = handler(async () => {
  return NextResponse.json({ dashboard: await dashboardSnapshot() });
});
