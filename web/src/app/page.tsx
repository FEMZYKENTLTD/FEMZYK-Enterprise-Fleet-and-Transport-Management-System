import Link from 'next/link';
import { redirect } from 'next/navigation';
import { getSession } from '@/lib/session';

export const dynamic = 'force-dynamic';

const MODULES = [
  ['Dashboard', 'Live KPIs, cost trends, service and licence alerts'],
  ['Vehicles', 'Cars, trucks, buses, vans and motorcycles with a status state machine'],
  ['Employees & drivers', 'Master data, licence classes and expiry tracking'],
  ['Assignments', 'One active vehicle per driver, one driver per vehicle — enforced in the database'],
  ['Trips', 'Plan → start → complete with odometer validation and distance capture'],
  ['Maintenance', 'Service history, next-service reminders, automatic expense posting'],
  ['Fuel', 'Litres, unit price, totals and efficiency per 100 km'],
  ['Expenses', 'Manual costs plus read-only records mirrored from fuel and maintenance'],
  ['Reports', 'Fleet, utilisation, monthly costs, audit — CSV and printable HTML'],
  ['Audit log', 'Every login, change and status transition is recorded'],
];

export default async function LandingPage() {
  const session = await getSession();
  if (session) redirect('/dashboard');

  return (
    <main className="min-h-screen bg-[#0f172a] text-white">
      <div className="mx-auto max-w-5xl px-6 py-16">
        <div className="flex items-center gap-3">
          <span className="grid h-11 w-11 place-items-center rounded-xl bg-blue-700 font-black">FE</span>
          <div>
            <p className="text-lg font-bold tracking-wide">FEMZYK</p>
            <p className="text-[11px] uppercase tracking-[0.16em] text-slate-400">Enterprises Ltd</p>
          </div>
        </div>

        <h1 className="mt-14 max-w-3xl text-4xl font-bold leading-tight tracking-tight sm:text-5xl">
          Enterprise Fleet &amp; Transport Management
        </h1>
        <p className="mt-5 max-w-2xl text-base leading-relaxed text-slate-300">
          The FEMZYK fleet system, now running in your browser. Same rules, same data model and same
          role-based security as the Java desktop edition — backed by PostgreSQL in the cloud, so the
          whole team works from one live source of truth.
        </p>

        <div className="mt-8 flex flex-wrap gap-3">
          <Link href="/login" className="rounded-lg bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-blue-900/40 transition hover:bg-blue-500">
            Sign in to the system
          </Link>
          <a href="/api/bootstrap" className="rounded-lg border border-white/20 px-5 py-2.5 text-sm font-semibold text-slate-200 transition hover:bg-white/5">
            System status
          </a>
        </div>

        <div className="mt-16 grid gap-3 sm:grid-cols-2">
          {MODULES.map(([title, text]) => (
            <div key={title} className="rounded-xl border border-white/10 bg-white/[0.03] p-4">
              <p className="text-sm font-semibold text-white">{title}</p>
              <p className="mt-1 text-[13px] leading-relaxed text-slate-400">{text}</p>
            </div>
          ))}
        </div>

        <div className="mt-14 flex flex-col gap-2 border-t border-white/10 pt-6 text-[11px] text-slate-500 sm:flex-row sm:items-center sm:justify-between">
          <span>© {new Date().getFullYear()} FEMZYK ENTERPRISES LTD · Lagos, Nigeria · v3.0.0</span>
          <span>CSC 392 SIWES · Lagos State University</span>
        </div>
      </div>
    </main>
  );
}
