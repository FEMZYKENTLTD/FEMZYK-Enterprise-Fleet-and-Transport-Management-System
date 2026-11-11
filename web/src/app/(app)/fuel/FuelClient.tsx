'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Plus, RefreshCw, Trash2, Download, Fuel as FuelIcon } from 'lucide-react';
import { api } from '@/lib/api';
import type { FuelRecord } from '@/lib/types';
import { FUEL_TYPES, label } from '@/lib/domain';
import { date, money, num } from '@/lib/format';
import { Badge, Button, Card, ConfirmDialog, EmptyState, Field, LoadingBlock, Modal, SearchInput, Select, TextArea, TextInput, toast } from '@/components/ui';
import { FormGrid, useFormSubmit } from '@/components/FormGrid';
import { DataTable, exportDataset, type Column } from '@/components/DataTable';

export function FuelClient({
  canWrite, initial, vehicles, drivers,
}: { canWrite: boolean; initial: FuelRecord[]; vehicles: { value: string; label: string }[]; drivers: { value: string; label: string }[] }) {
  const [rows, setRows] = useState<FuelRecord[]>(initial);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [vehicleId, setVehicleId] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<FuelRecord | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<FuelRecord | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    const params = new URLSearchParams();
    if (vehicleId) params.set('vehicleId', vehicleId);
    const result = await api.get<{ fuel: FuelRecord[] }>(`/api/fuel?${params}`);
    setLoading(false);
    if (!result.ok) { toast(result.error, 'error'); return; }
    setRows(result.data.fuel);
  }, [vehicleId]);

  useEffect(() => { void load(); }, [load]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter((r) => [r.registration_number, r.station, r.receipt_reference, r.driver_name].some((v) => (v ?? '').toLowerCase().includes(q)));
  }, [rows, search]);

  const columns: Column<FuelRecord>[] = [
    {
      key: 'registration_number', header: 'Vehicle',
      render: (r) => (
        <div>
          <p className="font-semibold text-slate-900">{r.registration_number}</p>
          <p className="text-[11px] text-slate-500">{r.station ?? '—'}{r.driver_name ? ` · ${r.driver_name}` : ''}</p>
        </div>
      ),
    },
    { key: 'fuel_date', header: 'Date', render: (r) => date(r.fuel_date), sortValue: (r) => r.fuel_date },
    { key: 'quantity_litres', header: 'Litres', align: 'right', render: (r) => num(r.quantity_litres, 2), sortValue: (r) => Number(r.quantity_litres) },
    { key: 'unit_price', header: 'Unit price', align: 'right', render: (r) => money(r.unit_price, true), hideOnMobile: true },
    { key: 'total_cost', header: 'Total', align: 'right', render: (r) => <span className="font-semibold">{money(r.total_cost, true)}</span>, sortValue: (r) => Number(r.total_cost) },
    { key: 'mileage', header: 'Odometer', align: 'right', render: (r) => (r.mileage ? num(r.mileage) : '—'), hideOnMobile: true },
    { key: 'receipt_reference', header: 'Receipt', render: (r) => <span className="font-mono text-[11px]">{r.receipt_reference ?? '—'}</span>, hideOnMobile: true },
    ...(canWrite ? [{
      key: '__actions', header: '', align: 'right' as const,
      render: (r: FuelRecord) => (
        <div className="flex justify-end" onClick={(e) => e.stopPropagation()}>
          <button title="Delete" onClick={() => setDeleteTarget(r)} className="rounded p-1.5 text-slate-400 hover:bg-slate-100 hover:text-rose-600"><Trash2 className="h-3.5 w-3.5" /></button>
        </div>
      ),
    }] : []),
  ];

  const monthStart = `${new Date().toISOString().slice(0, 7)}-01`;
  const monthCost = rows.filter((r) => r.fuel_date >= monthStart).reduce((s, r) => s + Number(r.total_cost), 0);
  const totalLitres = rows.reduce((s, r) => s + Number(r.quantity_litres), 0);
  const totalCost = rows.reduce((s, r) => s + Number(r.total_cost), 0);
  const avgPrice = totalLitres ? totalCost / totalLitres : 0;

  return (
    <div>
      <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="mb-1 text-[10px] font-bold uppercase tracking-[0.14em] text-blue-700">Operations</p>
          <h1 className="text-[22px] font-bold tracking-tight text-slate-900">Fuel</h1>
          <p className="mt-1 max-w-2xl text-[13px] leading-relaxed text-slate-500">
            Every purchase records litres, unit price and total (litres × price). The odometer may only move forward, and an
            automatic expense is posted for every fill-up.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" size="sm" icon={<Download className="h-3.5 w-3.5" />} onClick={() => exportDataset('fuel').catch((e) => toast((e as Error).message, 'error'))}>Export</Button>
          <Button variant="secondary" size="sm" icon={<RefreshCw className="h-3.5 w-3.5" />} onClick={load}>Refresh</Button>
          {canWrite && <Button size="sm" icon={<Plus className="h-3.5 w-3.5" />} onClick={() => { setEditing(null); setFormOpen(true); }}>Log fuel</Button>}
        </div>
      </div>

      <div className="mb-4 grid gap-2.5 sm:grid-cols-2 lg:grid-cols-4">
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Fill-ups</p><p className="text-lg font-bold tabular-nums">{rows.length}</p></Card>
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Litres</p><p className="text-lg font-bold tabular-nums">{num(totalLitres, 1)}</p></Card>
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">This month</p><p className="text-lg font-bold tabular-nums">{money(monthCost)}</p></Card>
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Average price / L</p><p className="text-lg font-bold tabular-nums">{money(avgPrice, true)}</p></Card>
      </div>

      <Card>
        <div className="flex flex-wrap items-center gap-2 border-b border-slate-100 px-4 py-3">
          <SearchInput value={search} onChange={setSearch} placeholder="Search fuel records…" />
          <div className="w-64"><Select value={vehicleId} placeholder="All vehicles" options={vehicles} onChange={(e) => setVehicleId(e.target.value)} /></div>
          <span className="ml-auto text-[11px] text-slate-500">{filtered.length} record{filtered.length === 1 ? '' : 's'}</span>
        </div>
        {loading ? <LoadingBlock /> : filtered.length === 0 ? (
          <EmptyState title="No fuel records" message="Log the first fuel purchase to start tracking consumption and cost." icon={<FuelIcon className="h-6 w-6" />}
            action={canWrite ? <Button size="sm" onClick={() => { setEditing(null); setFormOpen(true); }}>Log fuel</Button> : undefined} />
        ) : <DataTable columns={columns} rows={filtered} rowKey={(r) => r.id} exportName="fuel" onRowClick={canWrite ? (r) => { setEditing(r); setFormOpen(true); } : undefined} />}
      </Card>

      <Modal open={formOpen} onClose={() => setFormOpen(false)} title={editing ? 'Edit fuel record' : 'Log a fuel purchase'}>
        <FuelForm record={editing} vehicles={vehicles} drivers={drivers} close={() => { setFormOpen(false); void load(); }} />
      </Modal>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        busy={busy}
        title="Delete fuel record?"
        confirmLabel="Delete"
        message={<>The fill-up of <b>{num(deleteTarget?.quantity_litres ?? 0, 1)} L</b> on <b>{date(deleteTarget?.fuel_date)}</b> and its linked automatic expense will be deleted.</>}
        onConfirm={async () => {
          if (!deleteTarget) return;
          setBusy(true);
          const result = await api.del(`/api/fuel/${deleteTarget.id}`);
          setBusy(false);
          setDeleteTarget(null);
          if (!result.ok) { toast(result.error, 'error'); return; }
          toast('Fuel record deleted.');
          void load();
        }}
      />
    </div>
  );
}

function FuelForm({ record, vehicles, drivers, close }: { record: FuelRecord | null; vehicles: { value: string; label: string }[]; drivers: { value: string; label: string }[]; close: () => void }) {
  const [form, setForm] = useState({
    vehicle_id: record ? String(record.vehicle_id) : '',
    driver_id: record?.driver_id ? String(record.driver_id) : '',
    fuel_date: record?.fuel_date ?? new Date().toISOString().slice(0, 10),
    quantity_litres: record ? String(record.quantity_litres) : '',
    unit_price: record ? String(record.unit_price) : '',
    mileage: record?.mileage ? String(record.mileage) : '',
    station: record?.station ?? '',
    receipt_reference: record?.receipt_reference ?? '',
    fuel_type: record?.fuel_type ?? '',
    notes: record?.notes ?? '',
  });
  const { busy, submit } = useFormSubmit(close);
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    setForm((p) => ({ ...p, [k]: e.target.value }));
  const total = Number(form.quantity_litres || 0) * Number(form.unit_price || 0);

  return (
    <div>
      <FormGrid>
        <Field label="Vehicle" required className="sm:col-span-2"><Select value={form.vehicle_id} placeholder="Select a vehicle…" options={vehicles} onChange={set('vehicle_id')} disabled={Boolean(record)} /></Field>
        <Field label="Driver"><Select value={form.driver_id} placeholder="Not recorded" options={drivers} onChange={set('driver_id')} /></Field>
        <Field label="Fuel date" required><TextInput type="date" value={form.fuel_date} onChange={set('fuel_date')} /></Field>
        <Field label="Quantity (litres)" required><TextInput type="number" step="0.01" value={form.quantity_litres} onChange={set('quantity_litres')} min={0.01} /></Field>
        <Field label="Unit price (₦/L)" required><TextInput type="number" step="0.01" value={form.unit_price} onChange={set('unit_price')} min={0} /></Field>
        <Field label="Odometer (km)" hint="Cannot be below the vehicle's current reading"><TextInput type="number" value={form.mileage} onChange={set('mileage')} min={0} /></Field>
        <Field label="Fuel type"><Select value={form.fuel_type} placeholder="Vehicle default" options={FUEL_TYPES.map((f) => ({ value: f, label: label(f) }))} onChange={set('fuel_type')} /></Field>
        <Field label="Station"><TextInput value={form.station} onChange={set('station')} placeholder="NNPC Ikeja" /></Field>
        <Field label="Receipt reference"><TextInput value={form.receipt_reference} onChange={set('receipt_reference')} placeholder="RCP-1001" /></Field>
        <Field label="Notes" className="sm:col-span-2"><TextArea value={form.notes} onChange={set('notes')} /></Field>
      </FormGrid>

      <div className="mt-3 flex items-center justify-between rounded-lg bg-slate-50 px-4 py-2.5">
        <span className="text-[12px] text-slate-500">Calculated total</span>
        <span className="text-sm font-bold text-slate-900">{money(total, true)}</span>
      </div>

      <div className="mt-5 flex justify-end gap-2 border-t border-slate-100 pt-4">
        <Button variant="secondary" onClick={close} disabled={busy}>Cancel</Button>
        <Button loading={busy} disabled={!form.vehicle_id || !form.quantity_litres || !form.unit_price}
          onClick={() => submit(record ? 'PUT' : 'POST', record ? `/api/fuel/${record.id}` : '/api/fuel', {
            ...form,
            vehicle_id: Number(form.vehicle_id),
            driver_id: form.driver_id === '' ? null : Number(form.driver_id),
            quantity_litres: Number(form.quantity_litres),
            unit_price: Number(form.unit_price),
            mileage: form.mileage === '' ? null : Number(form.mileage),
            fuel_type: form.fuel_type === '' ? null : form.fuel_type,
          }, record ? 'Fuel record updated.' : 'Fuel purchase logged.')}>
          {record ? 'Save changes' : 'Log fuel'}
        </Button>
      </div>
    </div>
  );
}
