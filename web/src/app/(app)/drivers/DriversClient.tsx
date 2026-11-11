'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Plus, RefreshCw, Trash2, Download, BadgeCheck, AlertTriangle } from 'lucide-react';
import { api } from '@/lib/api';
import type { Driver } from '@/lib/types';
import { DRIVER_STATUSES, LICENCE_CATEGORIES, LICENCE_DESCRIPTIONS, label, statusTone, type LicenceCategory } from '@/lib/domain';
import { date } from '@/lib/format';
import { Badge, Button, Card, ConfirmDialog, EmptyState, Field, LoadingBlock, Modal, SearchInput, Select, TextArea, TextInput, toast } from '@/components/ui';
import { FormGrid, useFormSubmit } from '@/components/FormGrid';
import { DataTable, exportDataset, type Column } from '@/components/DataTable';

const statusOptions = DRIVER_STATUSES.map((s) => ({ value: s, label: label(s) }));
const categoryOptions = LICENCE_CATEGORIES.map((c) => ({ value: c, label: `${c} — ${LICENCE_DESCRIPTIONS[c]}` }));

export function DriversClient({ canWrite, initial, employees }: { canWrite: boolean; initial: Driver[]; employees: { value: string; label: string }[] }) {
  const [rows, setRows] = useState<Driver[]>(initial);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Driver | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Driver | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    const params = new URLSearchParams();
    if (status) params.set('status', status);
    const result = await api.get<{ drivers: Driver[] }>(`/api/drivers?${params}`);
    setLoading(false);
    if (!result.ok) { toast(result.error, 'error'); return; }
    setRows(result.data.drivers);
  }, [status]);

  useEffect(() => { void load(); }, [load]);

  const today = new Date().toISOString().slice(0, 10);
  const horizon = new Date(Date.now() + 30 * 86_400_000).toISOString().slice(0, 10);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter((r) => [r.driver_code, r.employee_full_name, r.licence_number, r.licence_category]
      .some((v) => (v ?? '').toLowerCase().includes(q)));
  }, [rows, search]);

  const columns: Column<Driver>[] = [
    {
      key: 'employee_full_name', header: 'Driver',
      render: (r) => (
        <div>
          <p className="font-semibold text-slate-900">{r.employee_full_name}</p>
          <p className="text-[11px] text-slate-500">{r.driver_code} · {r.employee_department}</p>
        </div>
      ),
    },
    { key: 'licence_number', header: 'Licence', render: (r) => <span className="font-mono text-[12px]">{r.licence_number}</span>, hideOnMobile: true },
    {
      key: 'licence_category', header: 'Class',
      render: (r) => (
        <div className="flex items-center gap-1.5">
          <Badge tone="bg-slate-100 text-slate-700 ring-slate-500/20">{r.licence_category}</Badge>
          <span className="hidden text-[11px] text-slate-500 lg:inline">{LICENCE_DESCRIPTIONS[r.licence_category as LicenceCategory]}</span>
        </div>
      ),
    },
    {
      key: 'licence_expiry_date', header: 'Expires',
      render: (r) => {
        const expired = r.licence_expiry_date < today;
        const soon = !expired && r.licence_expiry_date <= horizon;
        return (
          <span className="flex items-center gap-1.5">
            <span className={`text-[12px] font-medium ${expired ? 'text-rose-600' : soon ? 'text-amber-600' : 'text-slate-700'}`}>{date(r.licence_expiry_date)}</span>
            {expired && <Badge tone={statusTone('EXPIRED')}>Expired</Badge>}
            {soon && <Badge tone={statusTone('EXPIRING SOON')}>Soon</Badge>}
          </span>
        );
      },
      sortValue: (r) => r.licence_expiry_date,
    },
    { key: 'employee_phone', header: 'Phone', render: (r) => r.employee_phone ?? '—', hideOnMobile: true },
    { key: 'status', header: 'Status', render: (r) => <Badge tone={statusTone(r.status)} dot>{label(r.status)}</Badge> },
    ...(canWrite ? [{
      key: '__actions', header: '', align: 'right' as const,
      render: (r: Driver) => (
        <div className="flex justify-end" onClick={(e) => e.stopPropagation()}>
          <button title="Delete" onClick={() => setDeleteTarget(r)} className="rounded p-1.5 text-slate-400 hover:bg-slate-100 hover:text-rose-600"><Trash2 className="h-3.5 w-3.5" /></button>
        </div>
      ),
    }] : []),
  ];

  const activeDrivers = rows.filter((r) => r.status === 'ACTIVE' && !r.deleted_at);
  const expiring = activeDrivers.filter((r) => r.licence_expiry_date >= today && r.licence_expiry_date <= horizon);
  const expired = activeDrivers.filter((r) => r.licence_expiry_date < today);

  return (
    <div>
      <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="mb-1 text-[10px] font-bold uppercase tracking-[0.14em] text-blue-700">People</p>
          <h1 className="text-[22px] font-bold tracking-tight text-slate-900">Drivers</h1>
          <p className="mt-1 max-w-2xl text-[13px] leading-relaxed text-slate-500">
            Licence class controls which vehicles a driver may take: A motorcycles, B cars, C light trucks, D heavy trucks and buses, E articulated.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" size="sm" icon={<Download className="h-3.5 w-3.5" />} onClick={() => exportDataset('drivers').catch((e) => toast((e as Error).message, 'error'))}>Export</Button>
          <Button variant="secondary" size="sm" icon={<RefreshCw className="h-3.5 w-3.5" />} onClick={load}>Refresh</Button>
          {canWrite && <Button size="sm" icon={<Plus className="h-3.5 w-3.5" />} onClick={() => { setEditing(null); setFormOpen(true); }}>Add driver profile</Button>}
        </div>
      </div>

      <div className="mb-4 grid gap-2.5 sm:grid-cols-2 lg:grid-cols-4">
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Drivers</p><p className="text-lg font-bold tabular-nums">{rows.filter((r) => !r.deleted_at).length}</p></Card>
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Active</p><p className="text-lg font-bold tabular-nums text-emerald-700">{activeDrivers.length}</p></Card>
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Expiring ≤ 30 days</p><p className="text-lg font-bold tabular-nums text-amber-600">{expiring.length}</p></Card>
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Expired</p><p className="text-lg font-bold tabular-nums text-rose-600">{expired.length}</p></Card>
      </div>

      {(expiring.length > 0 || expired.length > 0) && (
        <div className="mb-4 flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 px-4 py-2.5 text-[12px] leading-relaxed text-amber-900">
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
          <div>
            <b>Licence attention required.</b>{' '}
            {expired.length > 0 && <>{expired.map((d) => d.employee_full_name).join(', ')} {expired.length === 1 ? 'has' : 'have'} an expired licence and cannot be assigned a vehicle. </>}
            {expiring.length > 0 && <>{expiring.map((d) => `${d.employee_full_name} (${date(d.licence_expiry_date)})`).join(', ')} expire{expiring.length === 1 ? 's' : ''} within 30 days.</>}
          </div>
        </div>
      )}

      <Card>
        <div className="flex flex-wrap items-center gap-2 border-b border-slate-100 px-4 py-3">
          <SearchInput value={search} onChange={setSearch} placeholder="Search drivers…" />
          <div className="w-44"><Select value={status} placeholder="All statuses" options={statusOptions} onChange={(e) => setStatus(e.target.value)} /></div>
          <span className="ml-auto text-[11px] text-slate-500">{filtered.length} record{filtered.length === 1 ? '' : 's'}</span>
        </div>
        {loading ? <LoadingBlock /> : filtered.length === 0 ? (
          <EmptyState title="No driver profiles" message="Create a driver profile from an active employee." icon={<BadgeCheck className="h-6 w-6" />}
            action={canWrite ? <Button size="sm" icon={<Plus className="h-3.5 w-3.5" />} onClick={() => { setEditing(null); setFormOpen(true); }}>Add driver profile</Button> : undefined} />
        ) : (
          <DataTable columns={columns} rows={filtered} rowKey={(r) => r.id} exportName="drivers"
            onRowClick={canWrite ? (r) => { setEditing(r); setFormOpen(true); } : undefined} />
        )}
      </Card>

      <Modal open={formOpen} onClose={() => setFormOpen(false)} title={editing ? 'Edit driver profile' : 'Add driver profile'}
        subtitle="Each employee can hold one driver profile. Codes are generated automatically (DRV-0001…).">
        <DriverForm driver={editing} close={() => setFormOpen(false)} onSaved={load} employees={employees} />
      </Modal>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        busy={busy}
        title="Remove driver profile?"
        confirmLabel="Remove profile"
        message={<>The driver profile for <b>{deleteTarget?.employee_full_name}</b> will be moved to the recycle bin. Drivers with an active assignment or trip cannot be removed.</>}
        onConfirm={async () => {
          if (!deleteTarget) return;
          setBusy(true);
          const result = await api.del(`/api/drivers/${deleteTarget.id}`);
          setBusy(false);
          setDeleteTarget(null);
          if (!result.ok) { toast(result.error, 'error'); return; }
          toast('Driver profile removed.');
          void load();
        }}
      />
    </div>
  );
}

function DriverForm({ driver, close, onSaved, employees }: { driver: Driver | null; close: () => void; onSaved: () => void; employees: { value: string; label: string }[] }) {
  const [form, setForm] = useState({
    employee_id: driver ? String(driver.employee_id) : '',
    licence_number: driver?.licence_number ?? '',
    licence_category: driver?.licence_category ?? 'B',
    licence_issue_date: driver?.licence_issue_date ?? '',
    licence_expiry_date: driver?.licence_expiry_date ?? '',
    status: driver?.status ?? 'ACTIVE',
    notes: driver?.notes ?? '',
  });
  const { busy, submit } = useFormSubmit(() => { onSaved(); close(); });
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    setForm((p) => ({ ...p, [k]: e.target.value }));

  return (
    <div>
      <FormGrid>
        <Field label="Employee" required hint={driver ? 'Linked employee cannot be changed' : undefined} className="sm:col-span-2">
          <Select value={form.employee_id} placeholder="Select an employee…" options={employees} onChange={set('employee_id')} disabled={Boolean(driver)} />
        </Field>
        <Field label="Licence number" required><TextInput value={form.licence_number} onChange={set('licence_number')} placeholder="LAG-2024-001122" /></Field>
        <Field label="Licence class" required>
          <Select value={form.licence_category} options={categoryOptions} onChange={set('licence_category')} />
        </Field>
        <Field label="Issue date"><TextInput type="date" value={form.licence_issue_date ?? ''} onChange={set('licence_issue_date')} /></Field>
        <Field label="Expiry date" required><TextInput type="date" value={form.licence_expiry_date} onChange={set('licence_expiry_date')} /></Field>
        <Field label="Driver status"><Select value={form.status} options={statusOptions} onChange={set('status')} /></Field>
        <Field label="Notes" className="sm:col-span-2"><TextArea value={form.notes} onChange={set('notes')} /></Field>
      </FormGrid>
      <div className="mt-5 flex justify-end gap-2 border-t border-slate-100 pt-4">
        <Button variant="secondary" onClick={close} disabled={busy}>Cancel</Button>
        <Button loading={busy} disabled={!form.employee_id || !form.licence_number || !form.licence_expiry_date}
          onClick={() => submit(driver ? 'PUT' : 'POST', driver ? `/api/drivers/${driver.id}` : '/api/drivers', { ...form, employee_id: Number(form.employee_id) }, driver ? 'Driver updated.' : 'Driver profile created.')}>
          {driver ? 'Save changes' : 'Add driver'}
        </Button>
      </div>
    </div>
  );
}
