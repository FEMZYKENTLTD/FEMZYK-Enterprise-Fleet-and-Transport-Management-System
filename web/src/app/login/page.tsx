import type { Metadata } from 'next';
import Link from 'next/link';
import { redirect } from 'next/navigation';
import { getSession } from '@/lib/session';
import { LoginForm } from './LoginForm';
import { db, isConfigured } from '@/lib/db';

export const metadata: Metadata = { title: 'Sign in' };
export const dynamic = 'force-dynamic';

export default async function LoginPage() {
  const session = await getSession();
  if (session) redirect('/dashboard');

  let ready = false;
  let userCount = 0;
  let dbError: string | null = null;
  if (isConfigured()) {
    try {
      const { count, error } = await db().from('users').select('id', { count: 'exact', head: true });
      if (error) throw error;
      userCount = count ?? 0;
      ready = userCount > 0;
    } catch (error) {
      dbError = (error as Error).message;
    }
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-[1.05fr_1fr]">
      {/* Brand panel */}
      <div className="relative hidden overflow-hidden bg-[#0f172a] p-12 lg:flex lg:flex-col lg:justify-between">
        <div className="absolute -right-32 -top-32 h-96 w-96 rounded-full bg-blue-600/20 blur-3xl" />
        <div className="absolute -bottom-40 -left-24 h-96 w-96 rounded-full bg-emerald-500/10 blur-3xl" />
        <div className="relative">
          <div className="flex items-center gap-3">
            <span className="grid h-11 w-11 place-items-center rounded-xl bg-blue-700 text-base font-black text-white">FE</span>
            <div>
              <p className="text-lg font-bold tracking-wide text-white">FEMZYK</p>
              <p className="text-[11px] font-medium uppercase tracking-[0.16em] text-slate-400">Enterprises Ltd</p>
            </div>
          </div>
        </div>
        <div className="relative max-w-md">
          <h1 className="text-[34px] font-bold leading-[1.15] tracking-tight text-white">
            Enterprise Fleet &amp;<br />Transport Management
          </h1>
          <p className="mt-4 text-sm leading-relaxed text-slate-300">
            One system for vehicles, drivers, assignments, trips, fuel, maintenance, expenses and reporting —
            now available in the browser as well as on the desktop.
          </p>
          <ul className="mt-8 grid grid-cols-2 gap-3 text-[13px] text-slate-300">
            {[
              'Live KPI dashboard', 'Role-based access', 'Trip & fuel logging', 'Cost per kilometre',
              'Service-due alerts', 'Licence expiry alerts', 'CSV & printable reports', 'Full audit trail',
            ].map((f) => (
              <li key={f} className="flex items-center gap-2">
                <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" /> {f}
              </li>
            ))}
          </ul>
        </div>
        <div className="relative flex items-center justify-between text-[11px] text-slate-500">
          <span>© {new Date().getFullYear()} FEMZYK ENTERPRISES LTD · Lagos, Nigeria</span>
          <span>v3.0.0</span>
        </div>
      </div>

      {/* Form panel */}
      <div className="flex items-center justify-center bg-[#f4f6f9] px-5 py-10">
        <div className="w-full max-w-[400px]">
          <div className="mb-8 lg:hidden">
            <div className="flex items-center gap-2.5">
              <span className="grid h-10 w-10 place-items-center rounded-lg bg-blue-700 text-sm font-black text-white">FE</span>
              <div>
                <p className="text-base font-bold text-slate-900">FEMZYK Fleet</p>
                <p className="text-[11px] uppercase tracking-wider text-slate-500">Transport Management</p>
              </div>
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-7 shadow-sm">
            <h2 className="text-xl font-bold tracking-tight text-slate-900">Sign in</h2>
            <p className="mt-1 text-sm text-slate-500">Use your fleet account credentials.</p>

            {!isConfigured() && (
              <Banner tone="danger" title="Database not configured">
                Set <code className="font-mono">NEXT_PUBLIC_SUPABASE_URL</code> and <code className="font-mono">SUPABASE_SERVICE_ROLE_KEY</code> in the environment, then reload.
              </Banner>
            )}
            {dbError && (
              <Banner tone="danger" title="Cannot reach the database">{dbError}</Banner>
            )}
            {isConfigured() && !dbError && !ready && (
              <Banner tone="warning" title="First-run setup required">
                No accounts exist yet. Call <code className="font-mono">POST /api/bootstrap</code> once to create the default administrator
                (<b>admin</b> / <b>Admin@2024</b>), then sign in.
              </Banner>
            )}

            <LoginForm demoMode={process.env.DEMO_MODE !== 'false'} />
          </div>

          <p className="mt-5 text-center text-[11px] leading-relaxed text-slate-400">
            PBKDF2-HMAC-SHA256 · account lockout after 5 failures · full audit trail<br />
            Lagos State University · CSC 392 SIWES project
          </p>
          <p className="mt-3 text-center text-[11px] leading-relaxed text-slate-500">
            New here? Accounts are issued by your fleet administrator under{' '}
            <span className="font-medium text-slate-600">Users &amp; roles</span> — there is no public sign-up,
            because this system holds company fleet and salary data.
          </p>
          <p className="mt-2 text-center text-[11px] text-slate-400">
            <Link href="/" className="hover:text-slate-600">About this system</Link>
          </p>
        </div>
      </div>
    </div>
  );
}

function Banner({ tone, title, children }: { tone: 'danger' | 'warning'; title: string; children: React.ReactNode }) {
  const styles = tone === 'danger'
    ? 'border-rose-200 bg-rose-50 text-rose-800'
    : 'border-amber-200 bg-amber-50 text-amber-900';
  return (
    <div className={`mt-4 rounded-lg border px-3 py-2.5 text-[12px] leading-relaxed ${styles}`}>
      <p className="font-semibold">{title}</p>
      <div className="mt-0.5 opacity-90">{children}</div>
    </div>
  );
}
