'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Plus, RefreshCw, Trash2, Download, Wrench, AlertTriangle } from 'lucide-react';
import { api } from '@/lib/api';
import type { MaintenanceRecord } from '@/lib/types';
import { label, MAINTENANCE_STATUSES, MAINTENANCE_TYPES, statusTone } from '@/lib/domain';
import { date, km, money, num } from '@/lib/format';
import { Badge, Button, Card, ConfirmDialog, EmptyState, Field, LoadingBlock, Modal, SearchInput, Select, TextArea, TextInput, toast } from '@/components/ui';
import { FormGrid, useFormSubmit } from '@/components/FormGrid';
import { DataTable, exportDataset, type Column } from '@/components/DataTable';

type Due = Awaited<ReturnType<typeof import('@/lib/services/operations').maintenanceDue>>;

export function MaintenanceClient({
  canWrite, initial, due, vehicles,
}: { canWrite: boolean; initial: MaintenanceRecord[]; due: Due; vehicles: { value: string; label: string }[] }) {
  const [rows, setRows] = useState<MaintenanceRecord[]>(initial);
  const [dueRows, setDueRows] = useState<Due>(due);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [type, setType] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<MaintenanceRecord | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<MaintenanceRecord | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    const params = new URLSearchParams();
    if (status) params.set('status', status);
    if (type) params.set('type', type);
    const [records, dueResult] = await Promise.all([
      api.get<{ maintenance: MaintenanceRecord[] }>(`/api/maintenance?${params}`),
      api.get<{ due: Due }>('/api/maintenance?due=1'),
    ]);
    setLoading(false);
    if (records.ok) setRows(records.data.maintenance);
    if (dueResult.ok) setDueRows(dueResult.data.due);
  }, [status, type]);

  useEffect(() => { void load(); }, [load]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter((r) => [r.description, r.provider, r.registration_number, r.maintenance_type].some((v) => (v ?? '').toLowerCase().includes(q)));
  }, [rows, search]);

  const columns: Column<MaintenanceRecord>[] = [
    {
      key: 'description', header: 'Work',
      render: (r) => (
        <div>
          <p className="font-semibold text-slate-900">{r.description}</p>
          <p className="text-[11px] text-slate-500">{r.registration_number} · {r.provider ?? 'No provider'}</p>
        </div>
      ),
    },
    { key: 'maintenance_type', header: 'Type', render: (r) => <Badge tone="bg-slate-100 text-slate-700 ring-slate-500/20">{label(r.maintenance_type)}</Badge>, hideOnMobile: true },
    { key: 'service_date', header: 'Service date', render: (r) => date(r.service_date), sortValue: (r) => r.service_date },
    { key: 'mileage_at_service', header: 'Odometer', align: 'right', render: (r) => (r.mileage_at_service ? km(Number(r.mileage_at_service)) : '—'), hideOnMobile: true },
    { key: 'cost', header: 'Cost', align: 'right', render: (r) => money(r.cost), sortValue: (r) => Number(r.cost) },
    {
      key: 'next_service_date', header: 'Next service',
      render: (r) => (r.next_service_date ? date(r.next_service_date) : r.next_service_mileage ? `${num(r.next_service_mileage)} km` : '—'),
      hideOnMobile: true,
    },
    { key: 'status', header: 'Status', render: (r) => <Badge tone={statusTone(r.status)} dot>{label(r.status)}</Badge> },
    ...(canWrite ? [{
      key: '__actions', header: '', align: 'right' as const,
      render: (r: MaintenanceRecord) => (
        <div className="flex justify-end" onClick={(e) => e.stopPropagation()}>
          <button title="Delete" onClick={() => setDeleteTarget(r)} className="rounded p-1.5 text-slate-400 hover:bg-slate-100 hover:text-rose-600"><Trash2 className="h-3.5 w-3.5" /></button>
        </div>
      ),
    }] : []),
  ];

  const totalCost = rows.reduce((s, r) => s + Number(r.cost), 0);

  return (
    <div>
      <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="mb-1 text-[10px] font-bold uppercase tracking-[0.14em] text-blue-700">Operations</p>
          <h1 className="text-[22px] font-bold tracking-tight text-slate-900">Maintenance</h1>
          <p className="mt-1 max-w-2xl text-[13px] leading-relaxed text-slate-500">
            Service and repair history with next-service reminders. Work marked IN PROGRESS puts the vehicle into MAINTENANCE;
            any cost is mirrored to an automatic, read-only expense record.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" size="sm" icon={<Download className="h-3.5 w-3.5" />} onClick={() => exportDataset('maintenance').catch((e) => toast((e as Error).message, 'error'))}>Export</Button>
          <Button variant="secondary" size="sm" icon={<RefreshCw className="h-3.5 w-3.5" />} onClick={load}>Refresh</Button>
          {canWrite && <Button size="sm" icon={<Plus className="h-3.5 w-3.5" />} onClick={() => { setEditing(null); setFormOpen(true); }}>Book maintenance</Button>}
        </div>
      </div>

      <div className="mb-4 grid gap-2.5 sm:grid-cols-2 lg:grid-cols-4">
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Records</p><p className="text-lg font-bold tabular-nums">{rows.length}</p></Card>
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Total cost</p><p className="text-lg font-bold tabular-nums">{money(totalCost)}</p></Card>
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Overdue</p><p className="text-lg font-bold tabular-nums text-rose-600">{dueRows.filter((d) => d.due_status === 'OVERDUE').length}</p></Card>
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Due soon</p><p className="text-lg font-bold tabular-nums text-amber-600">{dueRows.filter((d) => d.due_status === 'DUE SOON').length}</p></Card>
      </div>

      {dueRows.length > 0 && (
        <div className="mb-4 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3">
          <p className="mb-1.5 flex items-center gap-1.5 text-[12px] font-semibold text-amber-900"><AlertTriangle className="h-3.5 w-3.5" /> Service reminders</p>
          <ul className="space-y-1 text-[12px] text-amber-900">
            {dueRows.map((d) => (
              <li key={d.vehicle_id} className="flex flex-wrap items-center gap-2">
                <Badge tone={statusTone(d.due_status)}>{d.due_status === 'OVERDUE' ? 'Overdue' : 'Due soon'}</Badge>
                <b>{d.registration_number}</b>
                <span className="text-amber-800">
                  {d.next_service_date ? `date ${date(d.next_service_date)}` : ''}
                  {d.next_service_mileage ? ` · at ${num(d.next_service_mileage)} km (now ${num(d.current_mileage)} km)` : ''}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}

      <Card>
        <div className="flex flex-wrap items-center gap-2 border-b border-slate-100 px-4 py-3">
          <SearchInput value={search} onChange={setSearch} placeholder="Search maintenance…" />
          <div className="w-40"><Select value={status} placeholder="All statuses" options={MAINTENANCE_STATUSES.map((s) => ({ value: s, label: label(s) }))} onChange={(e) => setStatus(e.target.value)} /></div>
          <div className="w-44"><Select value={type} placeholder="All types" options={MAINTENANCE_TYPES.map((t) => ({ value: t, label: label(t) }))} onChange={(e) => setType(e.target.value)} /></div>
          <span className="ml-auto text-[11px] text-slate-500">{filtered.length} record{filtered.length === 1 ? '' : 's'}</span>
        </div>
        {loading ? <LoadingBlock /> : filtered.length === 0 ? (
          <EmptyState title="No maintenance records" message="Book routine service or repairs to build the vehicle history." icon={<Wrench className="h-6 w-6" />}
            action={canWrite ? <Button size="sm" onClick={() => { setEditing(null); setFormOpen(true); }}>Book maintenance</Button> : undefined} />
        ) : <DataTable columns={columns} rows={filtered} rowKey={(r) => r.id} exportName="maintenance" onRowClick={canWrite ? (r) => { setEditing(r); setFormOpen(true); } : undefined} />}
      </Card>

      <Modal open={formOpen} onClose={() => setFormOpen(false)} title={editing ? 'Edit maintenance record' : 'Book maintenance'}>
        <MaintenanceForm record={editing} vehicles={vehicles} close={() => { setFormOpen(false); void load(); }} />
      </Modal>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        busy={busy}
        title="Delete maintenance record?"
        confirmLabel="Delete"
        message={<>The record <b>{deleteTarget?.description}</b> and its linked automatic expense will be deleted. If the vehicle is in MAINTENANCE it returns to AVAILABLE (or ASSIGNED).</>}
        onConfirm={async () => {
          if (!deleteTarget) return;
          setBusy(true);
          const result = await api.del(`/api/maintenance/${deleteTarget.id}`);
          setBusy(false);
          setDeleteTarget(null);
          if (!result.ok) { toast(result.error, 'error'); return; }
          toast('Maintenance record deleted.');
          void load();
        }}
      />
    </div>
  );
}

function MaintenanceForm({ record, vehicles, close }: { record: MaintenanceRecord | null; vehicles: { value: string; label: string }[]; close: () => void }) {
  const [form, setForm] = useState({
    vehicle_id: record ? String(record.vehicle_id) : '',
    service_date: record?.service_date ?? new Date().toISOString().slice(0, 10),
    maintenance_type: record?.maintenance_type ?? 'ROUTINE_SERVICE',
    description: record?.description ?? '',
    provider: record?.provider ?? '',
    cost: record ? String(record.cost) : '0',
    mileage_at_service: record?.mileage_at_service !== null && record?.mileage_at_service !== undefined ? String(record.mileage_at_service) : '',
    next_service_date: record?.next_service_date ?? '',
    next_service_mileage: record?.next_service_mileage !== null && record?.next_service_mileage !== undefined ? String(record.next_service_mileage) : '',
    status: record?.status ?? 'COMPLETED',
    notes: record?.notes ?? '',
  });
  const { busy, submit } = useFormSubmit(close);
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    setForm((p) => ({ ...p, [k]: e.target.value }));

  return (
    <div>
      <FormGrid>
        <Field label="Vehicle" required className="sm:col-span-2"><Select value={form.vehicle_id} placeholder="Select a vehicle…" options={vehicles} onChange={set('vehicle_id')} disabled={Boolean(record)} /></Field>
        <Field label="Service date" required><TextInput type="date" value={form.service_date} onChange={set('service_date')} /></Field>
        <Field label="Type" required><Select value={form.maintenance_type} options={MAINTENANCE_TYPES.map((t) => ({ value: t, label: label(t) }))} onChange={set('maintenance_type')} /></Field>
        <Field label="Description" required className="sm:col-span-2"><TextInput value={form.description} onChange={set('description')} placeholder="Oil and filter change, brake inspection" /></Field>
        <Field label="Provider / workshop"><TextInput value={form.provider} onChange={set('provider')} placeholder="Femzyk Workshop" /></Field>
        <Field label="Cost (₦)" hint="Posts an automatic expense when above zero"><TextInput type="number" value={form.cost} onChange={set('cost')} min={0} /></Field>
        <Field label="Odometer at service (km)"><TextInput type="number" value={form.mileage_at_service} onChange={set('mileage_at_service')} min={0} /></Field>
        <Field label="Status" required>
          <Select value={form.status} options={MAINTENANCE_STATUSES.map((s) => ({ value: s, label: label(s) }))} onChange={set('status')} />
        </Field>
        <Field label="Next service date"><TextInput type="date" value={form.next_service_date} onChange={set('next_service_date')} /></Field>
        <Field label="Next service odometer (km)"><TextInput type="number" value={form.next_service_mileage} onChange={set('next_service_mileage')} min={0} /></Field>
        <Field label="Notes" className="sm:col-span-2"><TextArea value={form.notes} onChange={set('notes')} /></Field>
      </FormGrid>
      <div className="mt-5 flex justify-end gap-2 border-t border-slate-100 pt-4">
        <Button variant="secondary" onClick={close} disabled={busy}>Cancel</Button>
        <Button loading={busy} disabled={!form.vehicle_id || !form.description}
          onClick={() => submit(record ? 'PUT' : 'POST', record ? `/api/maintenance/${record.id}` : '/api/maintenance', {
            ...form,
            vehicle_id: Number(form.vehicle_id),
            cost: Number(form.cost || 0),
            mileage_at_service: form.mileage_at_service === '' ? null : Number(form.mileage_at_service),
            next_service_mileage: form.next_service_mileage === '' ? null : Number(form.next_service_mileage),
          }, record ? 'Maintenance updated.' : 'Maintenance recorded.')}>
          {record ? 'Save changes' : 'Record maintenance'}
        </Button>
      </div>
    </div>
  );
}
