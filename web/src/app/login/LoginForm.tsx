'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { AlertCircle, LogIn } from 'lucide-react';
import { api } from '@/lib/api';
import { Button, Field, TextInput } from '@/components/ui';

const DEMO_ACCOUNTS = [
  { username: 'admin', password: 'Admin@2024', role: 'Administrator' },
  { username: 'manager', password: 'Fleet@2026', role: 'Fleet Manager' },
  { username: 'ops', password: 'Ops@2026', role: 'Operations Officer' },
  { username: 'viewer', password: 'View@2026', role: 'Viewer' },
];

export function LoginForm({ demoMode }: { demoMode: boolean }) {
  const router = useRouter();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    const result = await api.post<{ user: unknown }>('/api/auth/login', { username, password });
    setBusy(false);
    if (!result.ok) {
      setError(result.error);
      return;
    }
    router.push('/dashboard');
    router.refresh();
  }

  return (
    <>
      <form onSubmit={submit} className="mt-5 space-y-3.5">
        <Field label="Username" required>
          <TextInput value={username} onChange={(e) => setUsername(e.target.value)} placeholder="e.g. admin" autoComplete="username" autoFocus />
        </Field>
        <Field label="Password" required>
          <TextInput type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" autoComplete="current-password" />
        </Field>

        <div className="-mt-1 text-right">
          <Link href="/login/reset" className="text-[11px] font-medium text-slate-500 hover:text-blue-700">
            Forgot your password?
          </Link>
        </div>

        {error && (
          <div role="alert" className="flex items-start gap-2 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-[12px] font-medium text-rose-700">
            <AlertCircle className="mt-0.5 h-3.5 w-3.5 shrink-0" /> {error}
          </div>
        )}

        <Button type="submit" loading={busy} disabled={!username || !password} icon={<LogIn className="h-4 w-4" />} className="w-full">
          Sign in
        </Button>
      </form>

      {demoMode && (
        <div className="mt-6 rounded-lg border border-dashed border-slate-300 bg-slate-50 px-3 py-3">
          <p className="text-[11px] font-semibold uppercase tracking-wider text-slate-500">Demo accounts — click to fill</p>
          <div className="mt-2 grid grid-cols-2 gap-1.5">
            {DEMO_ACCOUNTS.map((a) => (
              <button
                key={a.username}
                type="button"
                onClick={() => { setUsername(a.username); setPassword(a.password); setError(null); }}
                className="rounded-md border border-slate-200 bg-white px-2 py-1.5 text-left transition hover:border-blue-300 hover:bg-blue-50/40"
              >
                <span className="block text-[11px] font-semibold text-slate-800">{a.role}</span>
                <span className="block font-mono text-[10px] text-slate-500">{a.username} · {a.password}</span>
              </button>
            ))}
          </div>
          <p className="mt-2 text-[10px] leading-relaxed text-slate-400">
            The administrator is asked to set a new password on first sign-in. Demo accounts are created by the seed endpoint.
          </p>
        </div>
      )}
    </>
  );
}
