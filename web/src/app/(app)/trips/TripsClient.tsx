'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Plus, RefreshCw, Play, CheckCircle2, Ban, Trash2, Download, Route } from 'lucide-react';
import { api } from '@/lib/api';
import type { Trip } from '@/lib/types';
import { label, statusTone, TRIP_STATUSES } from '@/lib/domain';
import { dateTime, km, num } from '@/lib/format';
import { Badge, Button, Card, ConfirmDialog, EmptyState, Field, LoadingBlock, Modal, SearchInput, Select, TextArea, TextInput, toast } from '@/components/ui';
import { FormGrid, useFormSubmit } from '@/components/FormGrid';
import { DataTable, exportDataset, type Column } from '@/components/DataTable';

type Dialog = { trip: Trip; mode: 'complete' | 'cancel' | 'delete' } | null;

export function TripsClient({
  canWrite, initial, vehicles, drivers,
}: { canWrite: boolean; initial: Trip[]; vehicles: { value: string; label: string }[]; drivers: { value: string; label: string }[] }) {
  const [rows, setRows] = useState<Trip[]>(initial);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [dialog, setDialog] = useState<Dialog>(null);
  const [endMileage, setEndMileage] = useState('');
  const [fuelUsed, setFuelUsed] = useState('');
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    const params = new URLSearchParams({ limit: '300' });
    if (status) params.set('status', status);
    const result = await api.get<{ trips: Trip[] }>(`/api/trips?${params}`);
    setLoading(false);
    if (!result.ok) { toast(result.error, 'error'); return; }
    setRows(result.data.trips);
  }, [status]);

  useEffect(() => { void load(); }, [load]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter((r) => [r.trip_code, r.destination, r.origin, r.registration_number, r.driver_name, r.purpose].some((v) => (v ?? '').toLowerCase().includes(q)));
  }, [rows, search]);

  async function act(trip: Trip, action: 'start') {
    setBusy(true);
    const result = await api.put(`/api/trips/${trip.id}`, { action });
    setBusy(false);
    if (!result.ok) { toast(result.error, 'error'); return; }
    toast(`Trip ${trip.trip_code} started — the vehicle is now IN SERVICE.`);
    void load();
  }

  const columns: Column<Trip>[] = [
    {
      key: 'trip_code', header: 'Trip',
      render: (r) => (
        <div>
          <p className="font-semibold text-slate-900">{r.origin ?? 'HQ'} → {r.destination}</p>
          <p className="text-[11px] text-slate-500">{r.trip_code} · {r.registration_number}</p>
        </div>
      ),
      sortValue: (r) => r.trip_code,
    },
    { key: 'driver_name', header: 'Driver', hideOnMobile: true },
    { key: 'departure_time', header: 'Departure', render: (r) => <span className="text-[12px]">{dateTime(r.departure_time)}</span>, sortValue: (r) => r.departure_time },
    {
      key: 'distance_km', header: 'Distance', align: 'right',
      render: (r) => (r.distance_km !== null ? km(r.distance_km) : <span className="text-slate-400">—</span>),
      sortValue: (r) => Number(r.distance_km ?? 0),
    },
    { key: 'fuel_used_litres', header: 'Fuel', align: 'right', render: (r) => (r.fuel_used_litres ? `${num(r.fuel_used_litres, 1)} L` : '—'), hideOnMobile: true },
    { key: 'status', header: 'Status', render: (r) => <Badge tone={statusTone(r.status)} dot>{label(r.status)}</Badge> },
    ...(canWrite ? [{
      key: '__actions', header: '', align: 'right' as const,
      render: (r: Trip) => (
        <div className="flex justify-end gap-0.5" onClick={(e) => e.stopPropagation()}>
          {r.status === 'PLANNED' && <button title="Start trip" disabled={busy} onClick={() => act(r, 'start')} className="rounded p-1.5 text-slate-400 hover:bg-slate-100 hover:text-emerald-600"><Play className="h-3.5 w-3.5" /></button>}
          {['PLANNED', 'ACTIVE'].includes(r.status) && (
            <>
              <button title="Complete trip" onClick={() => { setDialog({ trip: r, mode: 'complete' }); setEndMileage(String(r.start_mileage ?? '')); setFuelUsed(String(r.fuel_used_litres ?? '')); }} className="rounded p-1.5 text-slate-400 hover:bg-slate-100 hover:text-blue-600"><CheckCircle2 className="h-3.5 w-3.5" /></button>
              <button title="Cancel trip" onClick={() => { setDialog({ trip: r, mode: 'cancel' }); setReason(''); }} className="rounded p-1.5 text-slate-400 hover:bg-slate-100 hover:text-rose-600"><Ban className="h-3.5 w-3.5" /></button>
            </>
          )}
          {['PLANNED', 'CANCELLED'].includes(r.status) && <button title="Delete trip" onClick={() => setDialog({ trip: r, mode: 'delete' })} className="rounded p-1.5 text-slate-400 hover:bg-slate-100 hover:text-rose-600"><Trash2 className="h-3.5 w-3.5" /></button>}
        </div>
      ),
    }] : []),
  ];

  const active = rows.filter((r) => r.status === 'ACTIVE');
  const planned = rows.filter((r) => r.status === 'PLANNED');
  const distance = rows.filter((r) => r.status === 'COMPLETED').reduce((s, r) => s + Number(r.distance_km ?? 0), 0);

  return (
    <div>
      <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="mb-1 text-[10px] font-bold uppercase tracking-[0.14em] text-blue-700">Fleet</p>
          <h1 className="text-[22px] font-bold tracking-tight text-slate-900">Trips</h1>
          <p className="mt-1 max-w-2xl text-[13px] leading-relaxed text-slate-500">
            Plan → start → complete. Starting puts the vehicle IN SERVICE; completing captures the end odometer, updates the
            vehicle mileage and frees the vehicle again.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" size="sm" icon={<Download className="h-3.5 w-3.5" />} onClick={() => exportDataset('trips').catch((e) => toast((e as Error).message, 'error'))}>Export</Button>
          <Button variant="secondary" size="sm" icon={<RefreshCw className="h-3.5 w-3.5" />} onClick={load}>Refresh</Button>
          {canWrite && <Button size="sm" icon={<Plus className="h-3.5 w-3.5" />} onClick={() => setFormOpen(true)}>Plan trip</Button>}
        </div>
      </div>

      <div className="mb-4 grid gap-2.5 sm:grid-cols-2 lg:grid-cols-4">
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Active now</p><p className="text-lg font-bold tabular-nums text-sky-700">{active.length}</p></Card>
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Planned</p><p className="text-lg font-bold tabular-nums">{planned.length}</p></Card>
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Completed</p><p className="text-lg font-bold tabular-nums text-emerald-700">{rows.filter((r) => r.status === 'COMPLETED').length}</p></Card>
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Distance recorded</p><p className="text-lg font-bold tabular-nums">{num(distance)} km</p></Card>
      </div>

      <Card>
        <div className="flex flex-wrap items-center gap-2 border-b border-slate-100 px-4 py-3">
          <SearchInput value={search} onChange={setSearch} placeholder="Search trips…" />
          <div className="w-44">
            <Select value={status} placeholder="All statuses" options={TRIP_STATUSES.map((s) => ({ value: s, label: label(s) }))} onChange={(e) => setStatus(e.target.value)} />
          </div>
          <span className="ml-auto text-[11px] text-slate-500">{filtered.length} record{filtered.length === 1 ? '' : 's'}</span>
        </div>
        {loading ? <LoadingBlock /> : filtered.length === 0 ? (
          <EmptyState title="No trips yet" message="Plan a trip to start tracking journeys and distances." icon={<Route className="h-6 w-6" />}
            action={canWrite ? <Button size="sm" onClick={() => setFormOpen(true)}>Plan trip</Button> : undefined} />
        ) : <DataTable columns={columns} rows={filtered} rowKey={(r) => r.id} exportName="trips" />}
      </Card>

      <Modal open={formOpen} onClose={() => setFormOpen(false)} title="Plan a trip" subtitle="Starting mileage defaults to the vehicle's current odometer.">
        <TripForm vehicles={vehicles} drivers={drivers} close={() => { setFormOpen(false); void load(); }} />
      </Modal>

      {dialog?.mode === 'complete' && (
        <Modal open onClose={() => setDialog(null)} title={`Complete ${dialog.trip.trip_code}`} width="max-w-md"
          footer={<>
            <Button variant="secondary" onClick={() => setDialog(null)} disabled={busy}>Cancel</Button>
            <Button loading={busy} onClick={async () => {
              setBusy(true);
              const result = await api.put(`/api/trips/${dialog.trip.id}`, {
                action: 'complete', end_mileage: Number(endMileage),
                fuel_used_litres: fuelUsed === '' ? null : Number(fuelUsed),
              });
              setBusy(false);
              if (!result.ok) { toast(result.error, 'error'); return; }
              setDialog(null);
              toast('Trip completed; vehicle mileage updated.');
              void load();
            }}>Complete trip</Button>
          </>}>
          <div className="space-y-3">
            <p className="text-[12px] text-slate-500">
              {dialog.trip.origin ?? 'HQ'} → <b>{dialog.trip.destination}</b> · start odometer <b>{num(dialog.trip.start_mileage ?? 0)} km</b>
            </p>
            <Field label="Ending odometer (km)" required hint={`Must be ≥ ${num(dialog.trip.start_mileage ?? 0)} km and within 5,000 km of the start.`}>
              <TextInput type="number" value={endMileage} onChange={(e) => setEndMileage(e.target.value)} min={dialog.trip.start_mileage ?? 0} />
            </Field>
            <Field label="Fuel used (litres)"><TextInput type="number" step="0.1" value={fuelUsed} onChange={(e) => setFuelUsed(e.target.value)} /></Field>
          </div>
        </Modal>
      )}

      <ConfirmDialog
        open={dialog?.mode === 'cancel' || dialog?.mode === 'delete'}
        onClose={() => setDialog(null)}
        busy={busy}
        title={dialog?.mode === 'delete' ? 'Delete trip?' : 'Cancel trip?'}
        confirmLabel={dialog?.mode === 'delete' ? 'Delete trip' : 'Cancel trip'}
        message={
          <div className="space-y-3">
            <p>{dialog?.mode === 'delete' ? 'This planned or cancelled trip will be permanently removed.' : 'The trip will be marked CANCELLED and the vehicle released.'}</p>
            {dialog?.mode === 'cancel' && <Field label="Reason"><TextInput value={reason} onChange={(e) => setReason(e.target.value)} placeholder="e.g. client rescheduled" /></Field>}
          </div>
        }
        onConfirm={async () => {
          if (!dialog) return;
          setBusy(true);
          const result = dialog.mode === 'delete'
            ? await api.del(`/api/trips/${dialog.trip.id}`)
            : await api.put(`/api/trips/${dialog.trip.id}`, { action: 'cancel', reason: reason || 'Not specified' });
          setBusy(false);
          setDialog(null);
          if (!result.ok) { toast(result.error, 'error'); return; }
          toast(dialog.mode === 'delete' ? 'Trip deleted.' : 'Trip cancelled.');
          void load();
        }}
      />
    </div>
  );
}

function TripForm({ vehicles, drivers, close }: { vehicles: { value: string; label: string }[]; drivers: { value: string; label: string }[]; close: () => void }) {
  const now = new Date();
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
  const [form, setForm] = useState({ vehicle_id: '', driver_id: '', origin: 'Ikeja HQ', destination: '', purpose: '', departure_time: local, start_mileage: '', notes: '', start_now: true });
  const { busy, submit } = useFormSubmit(close);
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    setForm((p) => ({ ...p, [k]: e.target.value }));

  return (
    <div>
      <FormGrid>
        <Field label="Vehicle" required><Select value={form.vehicle_id} placeholder="Select a vehicle…" options={vehicles} onChange={set('vehicle_id')} /></Field>
        <Field label="Driver" required><Select value={form.driver_id} placeholder="Select a driver…" options={drivers} onChange={set('driver_id')} /></Field>
        <Field label="Origin"><TextInput value={form.origin} onChange={set('origin')} /></Field>
        <Field label="Destination" required><TextInput value={form.destination} onChange={set('destination')} placeholder="Lekki Phase 1" /></Field>
        <Field label="Departure" required><TextInput type="datetime-local" value={form.departure_time} onChange={set('departure_time')} /></Field>
        <Field label="Starting odometer (km)" hint="Leave blank to use the vehicle's current mileage."><TextInput type="number" value={form.start_mileage} onChange={set('start_mileage')} /></Field>
        <Field label="Purpose" className="sm:col-span-2"><TextInput value={form.purpose} onChange={set('purpose')} placeholder="Client visit" /></Field>
        <Field label="Notes" className="sm:col-span-2"><TextArea value={form.notes} onChange={set('notes')} /></Field>
      </FormGrid>
      <label className="mt-3 flex items-center gap-2 text-[12px] font-medium text-slate-600">
        <input type="checkbox" checked={form.start_now} onChange={(e) => setForm((p) => ({ ...p, start_now: e.target.checked }))} className="rounded border-slate-300" />
        Start immediately (vehicle goes IN SERVICE)
      </label>
      <div className="mt-5 flex justify-end gap-2 border-t border-slate-100 pt-4">
        <Button variant="secondary" onClick={close} disabled={busy}>Cancel</Button>
        <Button loading={busy} disabled={!form.vehicle_id || !form.driver_id || !form.destination}
          onClick={() => submit('POST', '/api/trips', {
            ...form,
            vehicle_id: Number(form.vehicle_id),
            driver_id: Number(form.driver_id),
            departure_time: new Date(form.departure_time).toISOString(),
            start_mileage: form.start_mileage === '' ? null : Number(form.start_mileage),
          }, form.start_now ? 'Trip started.' : 'Trip planned.')}>
          {form.start_now ? 'Plan & start trip' : 'Save planned trip'}
        </Button>
      </div>
    </div>
  );
}
