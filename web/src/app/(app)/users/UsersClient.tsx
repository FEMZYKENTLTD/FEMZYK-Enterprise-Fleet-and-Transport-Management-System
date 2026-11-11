'use client';

import { useState } from 'react';
import { Plus, Trash2, KeyRound, ShieldCheck } from 'lucide-react';
import { api } from '@/lib/api';
import type { UserSummary } from '@/lib/types';
import { label, ROLES, ROLE_PERMISSIONS, type Role } from '@/lib/domain';
import { dateTime } from '@/lib/format';
import { Badge, Button, Card, ConfirmDialog, Field, Modal, Select, TextInput, toast } from '@/components/ui';
import { FormGrid } from '@/components/FormGrid';

export function UsersClient({ initial, currentUserId }: { initial: UserSummary[]; currentUserId: number }) {
  const [rows, setRows] = useState<UserSummary[]>(initial);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<UserSummary | null>(null);
  const [resetTarget, setResetTarget] = useState<UserSummary | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<UserSummary | null>(null);
  const [newPassword, setNewPassword] = useState('');
  const [busy, setBusy] = useState(false);

  function replaceUsers(payload: { users?: UserSummary[] }) {
    if (payload.users) setRows(payload.users);
  }

  return (
    <div>
      <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="mb-1 text-[10px] font-bold uppercase tracking-[0.14em] text-blue-700">Administration</p>
          <h1 className="text-[22px] font-bold tracking-tight text-slate-900">Users &amp; roles</h1>
          <p className="mt-1 max-w-2xl text-[13px] leading-relaxed text-slate-500">
            Four roles with twenty permissions enforced in the service layer. At least one active administrator must always remain.
          </p>
        </div>
        <Button size="sm" icon={<Plus className="h-3.5 w-3.5" />} onClick={() => { setEditing(null); setFormOpen(true); }}>Add user</Button>
      </div>

      <div className="mb-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {ROLES.map((r) => (
          <Card key={r.value} className="px-4 py-3">
            <div className="flex items-center justify-between">
              <p className="text-[11px] font-semibold text-slate-700">{r.label}</p>
              <Badge tone="bg-slate-100 text-slate-600 ring-slate-500/20">{ROLE_PERMISSIONS[r.value].length}</Badge>
            </div>
            <p className="mt-1 text-lg font-bold tabular-nums text-slate-900">{rows.filter((u) => u.role === r.value && u.active).length}</p>
            <p className="text-[10.5px] leading-snug text-slate-400">{ROLE_PERMISSIONS[r.value].slice(0, 4).map((p) => p.replaceAll('_', ' ').toLowerCase()).join(', ')}…</p>
          </Card>
        ))}
      </div>

      <Card className="overflow-hidden">
        <div className="thin-scroll overflow-x-auto">
          <table className="w-full min-w-[720px] border-collapse text-sm">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50/80">
                {['User', 'Role', 'Status', 'Last sign-in', 'Password', ''].map((h) => (
                  <th key={h} className="px-4 py-2.5 text-left text-[10px] font-bold uppercase tracking-wider text-slate-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map((u) => (
                <tr key={u.id} className="border-b border-slate-100 last:border-0 hover:bg-slate-50/60">
                  <td className="px-4 py-2.5">
                    <p className="font-semibold text-slate-900">{u.full_name} {u.id === currentUserId && <span className="text-[10px] font-medium text-blue-600">(you)</span>}</p>
                    <p className="text-[11px] text-slate-500">{u.username} · {u.email}</p>
                  </td>
                  <td className="px-4 py-2.5"><Badge tone="bg-blue-50 text-blue-700 ring-blue-600/20">{label(u.role)}</Badge></td>
                  <td className="px-4 py-2.5">
                    <Badge tone={u.active ? 'bg-emerald-50 text-emerald-700 ring-emerald-600/20' : 'bg-rose-50 text-rose-700 ring-rose-600/20'} dot>
                      {u.active ? 'Active' : 'Disabled'}
                    </Badge>
                  </td>
                  <td className="px-4 py-2.5 text-[12px] text-slate-500">{u.last_login_at ? dateTime(u.last_login_at) : 'Never'}</td>
                  <td className="px-4 py-2.5">{u.must_change_password && <Badge tone="bg-amber-50 text-amber-800 ring-amber-600/20">Must change</Badge>}</td>
                  <td className="px-4 py-2.5">
                    <div className="flex justify-end gap-1">
                      <button title="Reset password" onClick={() => { setResetTarget(u); setNewPassword(''); }} className="rounded p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-700"><KeyRound className="h-3.5 w-3.5" /></button>
                      <button title="Edit" onClick={() => { setEditing(u); setFormOpen(true); }} className="rounded p-1.5 text-slate-400 hover:bg-slate-100 hover:text-blue-600"><ShieldCheck className="h-3.5 w-3.5" /></button>
                      {u.id !== currentUserId && <button title="Delete" onClick={() => setDeleteTarget(u)} className="rounded p-1.5 text-slate-400 hover:bg-slate-100 hover:text-rose-600"><Trash2 className="h-3.5 w-3.5" /></button>}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <Modal open={formOpen} onClose={() => setFormOpen(false)} title={editing ? 'Edit user' : 'Add user'} width="max-w-lg">
        <UserForm user={editing} close={() => setFormOpen(false)} onSaved={replaceUsers} />
      </Modal>

      <ConfirmDialog
        open={Boolean(resetTarget)}
        onClose={() => setResetTarget(null)}
        busy={busy}
        title="Reset password"
        confirmLabel="Reset password"
        tone="primary"
        message={
          <div className="space-y-3">
            <p>A temporary password will be set for <b>{resetTarget?.full_name}</b>; they must change it at next sign-in.</p>
            <Field label="New password" required hint="8+ characters, one letter and one digit.">
              <TextInput type="text" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} />
            </Field>
          </div>
        }
        onConfirm={async () => {
          if (!resetTarget) return;
          setBusy(true);
          const result = await api.put<{ users: UserSummary[] }>(`/api/users/${resetTarget.id}`, { action: 'reset-password', newPassword });
          setBusy(false);
          if (!result.ok) { toast(result.error, 'error'); return; }
          setResetTarget(null);
          replaceUsers(result.data);
          toast('Password reset.');
        }}
      />

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        busy={busy}
        title="Delete user?"
        confirmLabel="Delete user"
        message={<>The account <b>{deleteTarget?.username}</b> will be deactivated and removed from the list. The last administrator cannot be deleted.</>}
        onConfirm={async () => {
          if (!deleteTarget) return;
          setBusy(true);
          const result = await api.del<{ users: UserSummary[] }>(`/api/users/${deleteTarget.id}`);
          setBusy(false);
          setDeleteTarget(null);
          if (!result.ok) { toast(result.error, 'error'); return; }
          replaceUsers(result.data);
          toast('User deleted.');
        }}
      />
    </div>
  );
}

function UserForm({ user, close, onSaved }: { user: UserSummary | null; close: () => void; onSaved: (p: { users?: UserSummary[] }) => void }) {
  const [form, setForm] = useState({
    username: user?.username ?? '',
    fullName: user?.full_name ?? '',
    email: user?.email ?? '',
    role: (user?.role ?? 'OPERATIONS_OFFICER') as Role,
    password: '',
    active: user?.active ?? true,
  });
  const [busy, setBusy] = useState(false);
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm((p) => ({ ...p, [k]: e.target.value }));

  async function save() {
    setBusy(true);
    const result = user
      ? await api.put<{ users: UserSummary[] }>(`/api/users/${user.id}`, { email: form.email, fullName: form.fullName, role: form.role, active: form.active })
      : await api.post<{ users: UserSummary[] }>('/api/users', form);
    setBusy(false);
    if (!result.ok) { toast(result.error, 'error'); return; }
    onSaved(result.data);
    toast(user ? 'User updated.' : 'User created.');
    close();
  }

  return (
    <div>
      <FormGrid>
        <Field label="Username" required><TextInput value={form.username} onChange={set('username')} disabled={Boolean(user)} placeholder="chiamaka" /></Field>
        <Field label="Full name" required><TextInput value={form.fullName} onChange={set('fullName')} placeholder="Chiamaka Eze" /></Field>
        <Field label="E-mail" required><TextInput type="email" value={form.email} onChange={set('email')} placeholder="name@company.com" /></Field>
        <Field label="Role" required>
          <Select value={form.role} options={ROLES.map((r) => ({ value: r.value, label: r.label }))} onChange={set('role')} />
        </Field>
        {!user && <Field label="Initial password" required hint="The user must change it at first sign-in." className="sm:col-span-2"><TextInput value={form.password} onChange={set('password')} placeholder="At least 8 characters, letters + digits" /></Field>}
        {user && (
          <label className="flex items-center gap-2 self-end text-[12px] font-medium text-slate-600 sm:col-span-2">
            <input type="checkbox" checked={form.active} onChange={(e) => setForm((p) => ({ ...p, active: e.target.checked }))} className="rounded border-slate-300" />
            Account is active
          </label>
        )}
      </FormGrid>
      <div className="mt-4 rounded-lg bg-slate-50 px-3 py-2 text-[11px] leading-relaxed text-slate-500">
        <b className="text-slate-700">{label(form.role)}</b> permissions: {ROLE_PERMISSIONS[form.role].map((p) => p.replaceAll('_', ' ').toLowerCase()).join(' · ')}
      </div>
      <div className="mt-5 flex justify-end gap-2 border-t border-slate-100 pt-4">
        <Button variant="secondary" onClick={close} disabled={busy}>Cancel</Button>
        <Button loading={busy} disabled={!form.email || !form.fullName || (!user && (!form.username || !form.password))} onClick={save}>
          {user ? 'Save changes' : 'Create user'}
        </Button>
      </div>
    </div>
  );
}
