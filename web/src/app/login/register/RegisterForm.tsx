'use client';

import { useState } from 'react';
import Link from 'next/link';
import { AlertCircle, ArrowLeft, CheckCircle2, UserPlus } from 'lucide-react';
import { api } from '@/lib/api';
import { Button, Field, TextInput } from '@/components/ui';

const ROLE_OPTIONS = [
  { value: 'VIEWER', label: 'Viewer — read-only dashboards and reports' },
  { value: 'OPERATIONS_OFFICER', label: 'Operations Officer — trips, maintenance, fuel, expenses' },
  { value: 'FLEET_MANAGER', label: 'Fleet Manager — fleet, people, operations, reports' },
  { value: 'ADMINISTRATOR', label: 'Administrator — everything, including users and audit' },
];

/**
 * Account *request* form. Submitting this creates nothing that can sign in:
 * it files a PENDING request for an administrator to approve or reject. The
 * role chosen here is only a preference — the administrator sets the real one.
 */
export function RegisterForm() {
  const [username, setUsername] = useState('');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [department, setDepartment] = useState('');
  const [requestedRole, setRequestedRole] = useState('VIEWER');
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [submitted, setSubmitted] = useState<{ id: number } | null>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    const result = await api.post<{ ok: boolean; id: number }>('/api/register', {
      username, email, fullName, department, requestedRole, reason,
    });
    setBusy(false);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    setSubmitted({ id: result.data.id });
  }

  if (submitted) {
    return (
      <div className="mt-5 space-y-4">
        <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2.5 text-[12px] leading-relaxed text-emerald-800">
          <p className="flex items-center gap-1.5 font-semibold">
            <CheckCircle2 className="h-3.5 w-3.5" /> Request received
          </p>
          <p className="mt-1 opacity-90">
            Your reference is <b>REQ-{String(submitted.id).padStart(4, '0')}</b>. An administrator will review it
            under <b>Users &amp; roles</b>. You will be told when your account is ready — this page cannot sign you in.
          </p>
        </div>
        <Link
          href="/login"
          className="flex w-full items-center justify-center gap-2 rounded-lg bg-blue-700 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-blue-800"
        >
          <ArrowLeft className="h-4 w-4" /> Back to sign in
        </Link>
      </div>
    );
  }

  return (
    <>
      <p className="mt-1 text-sm text-slate-500">
        Ask for an account. An administrator approves every request, so nothing is created until then.
      </p>

      <form onSubmit={submit} className="mt-5 space-y-3.5">
        <Field label="Full name" required>
          <TextInput value={fullName} onChange={(e) => setFullName(e.target.value)} placeholder="e.g. Ada Obi" autoComplete="name" autoFocus />
        </Field>
        <Field label="Username" required hint="3–32 characters: letters, digits, dot, dash or underscore.">
          <TextInput value={username} onChange={(e) => setUsername(e.target.value)} placeholder="e.g. ada.obi" autoComplete="username" />
        </Field>
        <Field label="Work e-mail" required>
          <TextInput type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="name@company.com" autoComplete="email" />
        </Field>
        <Field label="Department">
          <TextInput value={department} onChange={(e) => setDepartment(e.target.value)} placeholder="e.g. Transport" />
        </Field>
        <Field label="Access you need" hint="A preference only — the administrator sets the real role.">
          <select
            value={requestedRole}
            onChange={(e) => setRequestedRole(e.target.value)}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-800 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
          >
            {ROLE_OPTIONS.map((r) => (
              <option key={r.value} value={r.value}>{r.label}</option>
            ))}
          </select>
        </Field>
        <Field label="Why do you need access?">
          <textarea
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={2}
            maxLength={500}
            placeholder="A sentence for the administrator reviewing this."
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-800 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
          />
        </Field>

        {error && (
          <div role="alert" className="flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-[12px] font-medium text-rose-700">
            <AlertCircle className="mt-0.5 h-3.5 w-3.5 shrink-0" /> {error}
          </div>
        )}

        <Button
          type="submit"
          loading={busy}
          disabled={!fullName || !username || !email}
          icon={<UserPlus className="h-4 w-4" />}
          className="w-full"
        >
          Submit request
        </Button>
      </form>

      <Link href="/login" className="mt-4 flex items-center justify-center gap-1.5 text-[12px] font-medium text-slate-500 hover:text-slate-700">
        <ArrowLeft className="h-3.5 w-3.5" /> Back to sign in
      </Link>

      <p className="mt-4 text-[10px] leading-relaxed text-slate-400">
        Requesting access never creates an account and never issues a session. Only an administrator can approve
        a request, and the temporary password they set must be changed at first sign-in.
      </p>
    </>
  );
}
