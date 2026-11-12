'use client';

import { useState } from 'react';
import { Check, Inbox, X } from 'lucide-react';
import { api } from '@/lib/api';
import { dateTime } from '@/lib/format';
import { label, ROLES, type Role } from '@/lib/domain';
import { Badge, Button, Card, Field, Modal, TextInput, toast } from '@/components/ui';

export type RegistrationRow = {
  id: number;
  username: string;
  email: string;
  full_name: string;
  requested_role: Role;
  department: string | null;
  reason: string | null;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  decided_by: string | null;
  decided_at: string | null;
  decision_note: string | null;
  created_at: string;
};

/**
 * Review queue for self-service account requests. Approving is what actually
 * creates the account, and the administrator chooses the role here — the
 * requester's stated preference is shown but never applied automatically.
 */
export function RegistrationQueue({ initial }: { initial: RegistrationRow[] }) {
  const [rows, setRows] = useState<RegistrationRow[]>(initial);
  const [approveTarget, setApproveTarget] = useState<RegistrationRow | null>(null);
  const [rejectTarget, setRejectTarget] = useState<RegistrationRow | null>(null);
  const [role, setRole] = useState<Role>('VIEWER');
  const [temporaryPassword, setTemporaryPassword] = useState('');
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);

  const pending = rows.filter((r) => r.status === 'PENDING');

  function openApprove(row: RegistrationRow) {
    setApproveTarget(row);
    // Default to the requester's preference, but the administrator decides.
    setRole(row.requested_role);
    setTemporaryPassword('');
    setNote('');
  }

  async function approve() {
    if (!approveTarget) return;
    setBusy(true);
    const result = await api.put<{ ok: boolean }>(`/api/registrations/${approveTarget.id}`, {
      action: 'approve', role, temporaryPassword, note,
    });
    setBusy(false);
    if (!result.ok) { toast(result.error, 'error'); return; }
    setRows((prev) => prev.map((r) => (r.id === approveTarget.id
      ? { ...r, status: 'APPROVED', decided_at: new Date().toISOString(), decision_note: note || null }
      : r)));
    toast(`Account created for ${approveTarget.username}`, 'success');
    setApproveTarget(null);
  }

  async function reject() {
    if (!rejectTarget) return;
    setBusy(true);
    const result = await api.put<{ ok: boolean }>(`/api/registrations/${rejectTarget.id}`, { action: 'reject', note });
    setBusy(false);
    if (!result.ok) { toast(result.error, 'error'); return; }
    setRows((prev) => prev.map((r) => (r.id === rejectTarget.id
      ? { ...r, status: 'REJECTED', decided_at: new Date().toISOString(), decision_note: note || null }
      : r)));
    toast('Request rejected', 'success');
    setRejectTarget(null);
  }

  return (
    <div className="mb-5">
      <div className="mb-2 flex items-center gap-2">
        <Inbox className="h-4 w-4 text-slate-400" />
        <h2 className="text-[13px] font-bold text-slate-800">Account requests</h2>
        {pending.length > 0 && (
          <Badge tone="bg-amber-50 text-amber-800 ring-amber-600/20">{pending.length} pending</Badge>
        )}
      </div>

      <Card className="overflow-hidden">
        {rows.length === 0 ? (
          <p className="px-4 py-5 text-[12px] text-slate-500">
            No requests yet. Anyone who needs an account can ask from the sign-in screen
            (<span className="font-mono text-[11px]">Request an account</span>).
          </p>
        ) : (
          <div className="thin-scroll overflow-x-auto">
            <table className="w-full min-w-[720px] border-collapse text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/80">
                  {['Requester', 'Wants', 'Reason', 'Status', ''].map((h) => (
                    <th key={h} className="px-4 py-2.5 text-left text-[10px] font-bold uppercase tracking-wider text-slate-500">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.id} className="border-b border-slate-100 last:border-0 hover:bg-slate-50/60">
                    <td className="px-4 py-2.5">
                      <p className="font-semibold text-slate-900">{r.full_name}</p>
                      <p className="text-[11px] text-slate-500">{r.username} · {r.email}</p>
                      <p className="text-[10px] text-slate-400">REQ-{String(r.id).padStart(4, '0')} · {dateTime(r.created_at)}</p>
                    </td>
                    <td className="px-4 py-2.5"><Badge tone="bg-slate-100 text-slate-600 ring-slate-500/20">{label(r.requested_role)}</Badge></td>
                    <td className="max-w-[220px] px-4 py-2.5 text-[11.5px] leading-snug text-slate-600">
                      {r.reason || <span className="text-slate-400">—</span>}
                      {r.department && <span className="block text-[10px] text-slate-400">{r.department}</span>}
                    </td>
                    <td className="px-4 py-2.5">
                      <Badge
                        dot
                        tone={r.status === 'PENDING'
                          ? 'bg-amber-50 text-amber-800 ring-amber-600/20'
                          : r.status === 'APPROVED'
                            ? 'bg-emerald-50 text-emerald-700 ring-emerald-600/20'
                            : 'bg-rose-50 text-rose-700 ring-rose-600/20'}
                      >
                        {r.status === 'PENDING' ? 'Pending' : r.status === 'APPROVED' ? 'Approved' : 'Rejected'}
                      </Badge>
                      {r.decision_note && <p className="mt-1 text-[10px] text-slate-400">{r.decision_note}</p>}
                    </td>
                    <td className="px-4 py-2.5">
                      {r.status === 'PENDING' && (
                        <div className="flex justify-end gap-1.5">
                          <Button size="sm" icon={<Check className="h-3.5 w-3.5" />} onClick={() => openApprove(r)}>Approve</Button>
                          <Button size="sm" variant="secondary" icon={<X className="h-3.5 w-3.5" />} onClick={() => { setRejectTarget(r); setNote(''); }}>Reject</Button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Modal
        open={Boolean(approveTarget)}
        onClose={() => setApproveTarget(null)}
        title="Approve account request"
        subtitle={approveTarget ? `${approveTarget.full_name} · ${approveTarget.username}` : ''}
        width="max-w-lg"
        footer={<>
          <Button variant="secondary" onClick={() => setApproveTarget(null)}>Cancel</Button>
          <Button onClick={approve} loading={busy} disabled={!temporaryPassword}>Create account</Button>
        </>}
      >
        <div className="space-y-3">
          <p className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-[11.5px] leading-relaxed text-slate-600">
            Approving creates the account and is the only thing that makes these credentials work.
            The new user must change the temporary password at first sign-in.
          </p>
          <Field label="Role" required hint="You choose the real role; their preference is only a request.">
            <select
              value={role}
              onChange={(e) => setRole(e.target.value as Role)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-800 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
            >
              {ROLES.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
            </select>
          </Field>
          <Field label="Temporary password" required hint="8+ characters, at least one letter and one digit.">
            <TextInput value={temporaryPassword} onChange={(e) => setTemporaryPassword(e.target.value)} placeholder="e.g. Temp@2026" />
          </Field>
          <Field label="Note">
            <TextInput value={note} onChange={(e) => setNote(e.target.value)} placeholder="Optional" />
          </Field>
        </div>
      </Modal>

      <Modal
        open={Boolean(rejectTarget)}
        onClose={() => setRejectTarget(null)}
        title="Reject account request"
        subtitle={rejectTarget ? `${rejectTarget.full_name} · ${rejectTarget.username}` : ''}
        width="max-w-md"
        footer={<>
          <Button variant="secondary" onClick={() => setRejectTarget(null)}>Cancel</Button>
          <Button variant="danger" onClick={reject} loading={busy}>Reject request</Button>
        </>}
      >
        <Field label="Reason (recorded in the audit log)">
          <TextInput value={note} onChange={(e) => setNote(e.target.value)} placeholder="Optional" />
        </Field>
      </Modal>
    </div>
  );
}
