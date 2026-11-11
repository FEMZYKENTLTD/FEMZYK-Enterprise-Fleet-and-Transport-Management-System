import { redirect } from 'next/navigation';
import { getSession } from '@/lib/session';
import { Shell } from '@/components/layout/Shell';

export default async function AppLayout({ children }: { children: React.ReactNode }) {
  const session = await getSession();
  if (!session) redirect('/login');
  return <Shell user={session}>{children}</Shell>;
}
