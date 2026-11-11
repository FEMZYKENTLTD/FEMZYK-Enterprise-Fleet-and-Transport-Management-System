import { getSession } from '@/lib/session';
import { redirect } from 'next/navigation';
import { roleHas } from '@/lib/domain';
import { VehiclesClient } from './VehiclesClient';

export const dynamic = 'force-dynamic';

export default async function VehiclesPage() {
  const session = await getSession();
  if (!session) redirect('/login');
  return <VehiclesClient canWrite={roleHas(session.role, 'MANAGE_VEHICLES')} />;
}
