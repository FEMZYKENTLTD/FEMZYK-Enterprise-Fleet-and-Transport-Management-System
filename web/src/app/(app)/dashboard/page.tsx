import type { Metadata } from 'next';
import { redirect } from 'next/navigation';
import Link from 'next/link';
import { getSession } from '@/lib/session';
import { dashboardSnapshot } from '@/lib/services/dashboard';
import { PageHeader } from '@/components/PageHeader';
import { DashboardView } from './DashboardView';

export const metadata: Metadata = { title: 'Dashboard' };
export const dynamic = 'force-dynamic';

export default async function DashboardPage() {
  const session = await getSession();
  if (!session) redirect('/login');

  let data = null;
  let error: string | null = null;
  try {
    data = await dashboardSnapshot();
  } catch (e) {
    error = (e as Error).message;
  }

  return (
    <>
      <PageHeader
        eyebrow="Overview"
        title={`Good ${greeting()}, ${session.fullName.split(' ')[0]}`}
        description="Every figure below is calculated live from the fleet database — vehicles, people, work in progress and money."
        actions={
          <>
            <Link href="/trips" className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50">Log a trip</Link>
            <Link href="/vehicles" className="rounded-lg bg-blue-700 px-3 py-2 text-xs font-semibold text-white shadow-sm hover:bg-blue-800">Manage fleet</Link>
          </>
        }
      />
      {error && (
        <div className="mb-4 rounded-lg border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
          <b>Could not load dashboard data.</b> {error}
        </div>
      )}
      {data ? <DashboardView data={data} role={session.role} /> : null}
    </>
  );
}

function greeting(): string {
  const h = new Date().getHours();
  if (h < 12) return 'morning';
  if (h < 17) return 'afternoon';
  return 'evening';
}
