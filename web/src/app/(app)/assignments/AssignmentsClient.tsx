'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Plus, RefreshCw, RotateCcw, Ban, Undo2, ClipboardList } from 'lucide-react';
import { api } from '@/lib/api';
import type { Assignment } from '@/lib/types';
import { label, statusTone } from '@/lib/domain';
import { date, dateTime } from '@/lib/format';
import { Badge, Button, Card, ConfirmDialog, EmptyState, Field, LoadingBlock, Modal, SearchInput, Select, TextArea, TextInput, toast } from '@/components/ui';
import { FormGrid, useFormSubmit } from '@/components/FormGrid';
import { DataTable, type Column } from '@/components/DataTable';

export function AssignmentsClient({
  canWrite, initial, vehicles, drivers,
}: {
  canWrite: boolean;
  initial: Assignment[];
  vehicles: { value: string; label: string }[];
  drivers: { value: string; label: string }[];
}) {
  const [rows, setRows] = useState<Assignment[]>(initial);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [actionTarget, setActionTarget] = useState<{ assignment: Assignment; mode: 'return' | 'cancel' } | null>(null);
  const [actionNote, setActionNote] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    const params = new URLSearchParams();
    if (status) params.set('status', status);
    const result = await api.get<{ assignments: Assignment[] }>(`/api/assignments?${params}`);
    setLoading(false);
    if (!result.ok) { toast(result.error, 'error'); return; }
    setRows(result.data.assignments);
  }, [status]);

  useEffect(() => { void load(); }, [load]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter((r) => [r.registration_number, r.driver_name, r.purpose, r.assigned_by].some((v) => (v ?? '').toLowerCase().includes(q)));
  }, [rows, search]);

  const active = rows.filter((r) => r.status === 'ACTIVE');

  const columns: Column<Assignment>[] = [
    {
      key: 'vehicle_display', header: 'Vehicle',
      render: (r) => (
        <div>
          <p className="font-semibold text-slate-900">{r.registration_number}</p>
          <p className="text-[11px] text-slate-500">{r.vehicle_display.replace(r.registration_number, '').replace(/^ \(|\)$/g, '').trim()}</p>
        </div>
      ),
      sortValue: (r) => r.registration_number,
    },
    {
      key: 'driver_name', header: 'Driver',
      render: (r) => (
        <div>
          <p className="text-[13px] font-medium text-slate-800">{r.driver_name}</p>
          <p className="text-[11px] text-slate-500">{r.driver_code}</p>
        </div>
      ),
    },
    { key: 'assigned_at', header: 'Assigned', render: (r) => date(r.assigned_at), sortValue: (r) => r.assigned_at },
    { key: 'returned_at', header: 'Returned', render: (r) => (r.returned_at ? date(r.returned_at) : '—'), hideOnMobile: true },
    { key: 'purpose', header: 'Purpose', render: (r) => <span className="text-[12px] text-slate-600">{r.purpose ?? '—'}</span>, hideOnMobile: true },
    { key: 'status', header: 'Status', render: (r) => <Badge tone={statusTone(r.status)} dot>{label(r.status)}</Badge> },
    ...(canWrite ? [{
      key: '__actions', header: '', align: 'right' as const,
      render: (r: Assignment) => (
        <div className="flex justify-end gap-1" onClick={(e) => e.stopPropagation()}>
          {r.status === 'ACTIVE' ? (
            <>
              <button title="Return vehicle" onClick={() => { setActionTarget({ assignment: r, mode: 'return' }); setActionNote(''); }} className="rounded p-1.5 text-slate-400 hover:bg-slate-100 hover:text-emerald-600"><RotateCcw className="h-3.5 w-3.5" /></button>
              <button title="Cancel assignment" onClick={() => { setActionTarget({ assignment: r, mode: 'cancel' }); setActionNote(''); }} className="rounded p-1.5 text-slate-400 hover:bg-slate-100 hover:text-rose-600"><Ban className="h-3.5 w-3.5" /></button>
            </>
          ) : <span className="px-1 text-[11px] text-slate-300">closed</span>}
        </div>
      ),
    }] : []),
  ];

  return (
    <div>
      <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="mb-1 text-[10px] font-bold uppercase tracking-[0.14em] text-blue-700">Fleet</p>
          <h1 className="text-[22px] font-bold tracking-tight text-slate-900">Assignments</h1>
          <p className="mt-1 max-w-2xl text-[13px] leading-relaxed text-slate-500">
            Match an available vehicle to an active driver. One active assignment per vehicle and per driver — enforced by
            partial unique indexes in PostgreSQL, so no code path can break it.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" size="sm" icon={<RefreshCw className="h-3.5 w-3.5" />} onClick={load}>Refresh</Button>
          {canWrite && <Button size="sm" icon={<Plus className="h-3.5 w-3.5" />} onClick={() => setFormOpen(true)} disabled={!vehicles.length || !drivers.length}>Assign vehicle</Button>}
        </div>
      </div>

      <div className="mb-4 grid gap-2.5 sm:grid-cols-3">
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Active assignments</p><p className="text-lg font-bold tabular-nums text-sky-700">{active.length}</p></Card>
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Available vehicles</p><p className="text-lg font-bold tabular-nums text-emerald-700">{vehicles.length}</p></Card>
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Assignable drivers</p><p className="text-lg font-bold tabular-nums">{drivers.length}</p></Card>
      </div>

      <Card>
        <div className="flex flex-wrap items-center gap-2 border-b border-slate-100 px-4 py-3">
          <SearchInput value={search} onChange={setSearch} placeholder="Search assignments…" />
          <div className="w-44">
            <Select value={status} placeholder="All statuses" options={[{ value: 'ACTIVE', label: 'Active' }, { value: 'RETURNED', label: 'Returned' }, { value: 'CANCELLED', label: 'Cancelled' }]} onChange={(e) => setStatus(e.target.value)} />
          </div>
          <span className="ml-auto text-[11px] text-slate-500">{filtered.length} record{filtered.length === 1 ? '' : 's'}</span>
        </div>
        {loading ? <LoadingBlock /> : filtered.length === 0 ? (
          <EmptyState title="No assignments yet" message="Assign an available vehicle to a driver to begin." icon={<ClipboardList className="h-6 w-6" />}
            action={canWrite ? <Button size="sm" onClick={() => setFormOpen(true)} disabled={!vehicles.length || !drivers.length}>Assign vehicle</Button> : undefined} />
        ) : <DataTable columns={columns} rows={filtered} rowKey={(r) => r.id} exportName="assignments" />}
      </Card>

      <Modal open={formOpen} onClose={() => setFormOpen(false)} title="Assign a vehicle" subtitle="Only AVAILABLE vehicles and ACTIVE drivers with a valid licence class are accepted." width="max-w-xl">
        <AssignForm vehicles={vehicles} drivers={drivers} close={() => { setFormOpen(false); void load(); }} />
      </Modal>

      <ConfirmDialog
        open={Boolean(actionTarget)}
        onClose={() => setActionTarget(null)}
        busy={busy}
        title={actionTarget?.mode === 'return' ? 'Return this vehicle?' : 'Cancel this assignment?'}
        confirmLabel={actionTarget?.mode === 'return' ? 'Return vehicle' : 'Cancel assignment'}
        tone={actionTarget?.mode === 'return' ? 'primary' : 'danger'}
        message={
          <div className="space-y-3">
            <p>
              {actionTarget?.mode === 'return'
                ? <>Vehicle <b>{actionTarget?.assignment.registration_number}</b> will be returned by <b>{actionTarget?.assignment.driver_name}</b> and becomes AVAILABLE.</>
                : <>The assignment of <b>{actionTarget?.assignment.registration_number}</b> to <b>{actionTarget?.assignment.driver_name}</b> will be cancelled.</>}
            </p>
            <Field label={actionTarget?.mode === 'return' ? 'Return note (optional)' : 'Cancellation reason'}>
              <TextInput value={actionNote} onChange={(e) => setActionNote(e.target.value)} placeholder="e.g. contract ended" />
            </Field>
          </div>
        }
        onConfirm={async () => {
          if (!actionTarget) return;
          setBusy(true);
          const result = await api.put(`/api/assignments/${actionTarget.assignment.id}`, {
            action: actionTarget.mode,
            notes: actionNote,
            reason: actionNote || 'Not specified',
          });
          setBusy(false);
          setActionTarget(null);
          if (!result.ok) { toast(result.error, 'error'); return; }
          toast(actionTarget.mode === 'return' ? 'Vehicle returned.' : 'Assignment cancelled.');
          void load();
        }}
      />
    </div>
  );
}

function AssignForm({ vehicles, drivers, close }: { vehicles: { value: string; label: string }[]; drivers: { value: string; label: string }[]; close: () => void }) {
  const [form, setForm] = useState({ vehicle_id: '', driver_id: '', assigned_at: new Date().toISOString().slice(0, 10), purpose: '', notes: '' });
  const { busy, submit } = useFormSubmit(close);
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    setForm((p) => ({ ...p, [k]: e.target.value }));

  return (
    <div>
      <FormGrid>
        <Field label="Vehicle" required className="sm:col-span-2"><Select value={form.vehicle_id} placeholder="Select an available vehicle…" options={vehicles} onChange={set('vehicle_id')} /></Field>
        <Field label="Driver" required className="sm:col-span-2"><Select value={form.driver_id} placeholder="Select an active driver…" options={drivers} onChange={set('driver_id')} /></Field>
        <Field label="Assignment date" required><TextInput type="date" value={form.assigned_at} onChange={set('assigned_at')} /></Field>
        <Field label="Purpose"><TextInput value={form.purpose} onChange={set('purpose')} placeholder="Field operations — Lekki axis" /></Field>
        <Field label="Notes" className="sm:col-span-2"><TextArea value={form.notes} onChange={set('notes')} /></Field>
      </FormGrid>
      <div className="mt-5 flex justify-end gap-2 border-t border-slate-100 pt-4">
        <Button variant="secondary" onClick={close} disabled={busy}>Cancel</Button>
        <Button loading={busy} disabled={!form.vehicle_id || !form.driver_id}
          onClick={() => submit('POST', '/api/assignments', { ...form, vehicle_id: Number(form.vehicle_id), driver_id: Number(form.driver_id) }, 'Vehicle assigned.')}>
          Assign vehicle
        </Button>
      </div>
    </div>
  );
}
