import { getSession } from '@/lib/session';
import { redirect } from 'next/navigation';
import { roleHas } from '@/lib/domain';
import { db } from '@/lib/db';
import type { AuditLog } from '@/lib/types';
import { AuditClient } from './AuditClient';

export const dynamic = 'force-dynamic';

export default async function AuditPage() {
  const session = await getSession();
  if (!session) redirect('/login');
  const allowed = roleHas(session.role, 'VIEW_AUDIT_LOG');

  let initial: AuditLog[] = [];
  if (allowed) {
    const { data } = await db().from('audit_logs').select('*').order('timestamp', { ascending: false }).limit(300);
    initial = (data ?? []) as AuditLog[];
  }
  return <AuditClient canView={allowed} initial={initial} />;
}
