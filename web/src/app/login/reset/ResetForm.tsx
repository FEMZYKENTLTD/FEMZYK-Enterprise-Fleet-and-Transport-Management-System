'use client';

import { useState } from 'react';
import Link from 'next/link';
import { AlertCircle, ArrowLeft, KeyRound } from 'lucide-react';
import { api } from '@/lib/api';
import { Button, Field, TextInput } from '@/components/ui';

/**
 * Self-service password reset. The API only needs the username plus the
 * registered e-mail address, then the user picks a new password immediately —
 * there is no mail server in this deployment, so nothing is sent by e-mail.
 */
export function ResetForm() {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (password !== confirm) {
      setError('The two passwords do not match.');
      return;
    }
    setBusy(true);
    setError(null);
    const result = await api.post<{ ok: boolean }>('/api/auth/reset', { username, email, newPassword: password });
    setBusy(false);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    setDone(true);
  }

  if (done) {
    return (
      <div className="mt-5 space-y-4">
        <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2.5 text-[12px] leading-relaxed text-emerald-800">
          <p className="font-semibold">Password updated</p>
          <p className="mt-0.5 opacity-90">You can now sign in with your new password.</p>
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
        Confirm the username and e-mail address on your account, then choose a new password.
      </p>

      <form onSubmit={submit} className="mt-5 space-y-3.5">
        <Field label="Username" required>
          <TextInput value={username} onChange={(e) => setUsername(e.target.value)} placeholder="e.g. manager" autoComplete="username" autoFocus />
        </Field>
        <Field label="Registered e-mail" required>
          <TextInput type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="name@femzyk.local" autoComplete="email" />
        </Field>
        <Field label="New password" required hint="At least 8 characters, one letter and one digit.">
          <TextInput type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" />
        </Field>
        <Field label="Confirm new password" required>
          <TextInput type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} autoComplete="new-password" />
        </Field>

        {error && (
          <div role="alert" className="flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-[12px] font-medium text-rose-700">
            <AlertCircle className="mt-0.5 h-3.5 w-3.5 shrink-0" /> {error}
          </div>
        )}

        <Button type="submit" loading={busy} disabled={!username || !email || !password || !confirm} icon={<KeyRound className="h-4 w-4" />} className="w-full">
          Reset password
        </Button>
      </form>

      <Link href="/login" className="mt-4 flex items-center justify-center gap-1.5 text-[12px] font-medium text-slate-500 hover:text-slate-700">
        <ArrowLeft className="h-3.5 w-3.5" /> Back to sign in
      </Link>

      <p className="mt-4 text-[10px] leading-relaxed text-slate-400">
        This deployment has no mail server, so the reset is completed here rather than by e-mail link.
        If you no longer know the registered address, an administrator can reset the password from Users &amp; roles.
      </p>
    </>
  );
}
