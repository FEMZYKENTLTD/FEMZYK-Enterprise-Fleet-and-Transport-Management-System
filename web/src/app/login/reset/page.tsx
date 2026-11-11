import type { Metadata } from 'next';
import Link from 'next/link';
import { redirect } from 'next/navigation';
import { getSession } from '@/lib/session';
import { ResetForm } from './ResetForm';

export const metadata: Metadata = { title: 'Reset password' };
export const dynamic = 'force-dynamic';

export default async function ResetPage() {
  const session = await getSession();
  if (session) redirect('/dashboard');

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#f4f6f9] px-5 py-10">
      <div className="w-full max-w-[420px]">
        <div className="mb-8 flex items-center gap-2.5">
          <span className="grid h-10 w-10 place-items-center rounded-lg bg-blue-700 text-sm font-black text-white">FE</span>
          <div>
            <p className="text-base font-bold text-slate-900">FEMZYK Fleet</p>
            <p className="text-[11px] uppercase tracking-wider text-slate-500">Transport Management</p>
          </div>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-7 shadow-sm">
          <h1 className="text-xl font-bold tracking-tight text-slate-900">Reset your password</h1>
          <ResetForm />
        </div>

        <p className="mt-5 text-center text-[11px] text-slate-400">
          <Link href="/" className="hover:text-slate-600">About this system</Link>
        </p>
      </div>
    </div>
  );
}
