import { getSession } from '@/lib/session';
import { redirect } from 'next/navigation';
import { roleHas } from '@/lib/domain';
import { listUsers } from '@/lib/auth';
import { UsersClient } from './UsersClient';

export const dynamic = 'force-dynamic';

export default async function UsersPage() {
  const session = await getSession();
  if (!session) redirect('/login');
  if (!roleHas(session.role, 'MANAGE_USERS')) {
    return <div className="rounded-xl border border-slate-200 bg-white p-8 text-center text-sm text-slate-500">User management is restricted to administrators.</div>;
  }
  const users = await listUsers(session);
  return <UsersClient initial={users.map((u) => ({ ...u }))} currentUserId={session.id} />;
}
