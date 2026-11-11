import { getSession } from '@/lib/session';
import { redirect } from 'next/navigation';
import { roleHas } from '@/lib/domain';
import { ReportsClient } from './ReportsClient';

export const dynamic = 'force-dynamic';

export default async function ReportsPage() {
  const session = await getSession();
  if (!session) redirect('/login');
  return <ReportsClient canView={roleHas(session.role, 'VIEW_REPORTS')} />;
}
