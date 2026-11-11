'use client';

import { useState } from 'react';
import { Pencil, Trash2, RotateCcw } from 'lucide-react';
import type { Vehicle } from '@/lib/types';
import { FUEL_TYPES, LABELS, VEHICLE_STATUSES, VEHICLE_TYPES, label, statusTone, vehicleTone, VEHICLE_TRANSITIONS, type VehicleStatus, type VehicleType } from '@/lib/domain';
import { date, km, money, num } from '@/lib/format';
import { Badge, Button, ConfirmDialog, Field, Select, TextArea, TextInput, toast } from '@/components/ui';
import { useRouter } from 'next/navigation';
import { DataPage } from '@/components/DataPage';
import { FormGrid, useFormSubmit } from '@/components/FormGrid';
import type { Column } from '@/components/DataTable';

const typeOptions = VEHICLE_TYPES.map((v) => ({ value: v, label: LABELS[v] ?? v }));
const statusOptions = VEHICLE_STATUSES.map((v) => ({ value: v, label: LABELS[v] ?? v }));
const fuelOptions = FUEL_TYPES.map((v) => ({ value: v, label: LABELS[v] ?? v }));

export function VehiclesClient({ canWrite }: { canWrite: boolean }) {
  const router = useRouter();
  const [deleteTarget, setDeleteTarget] = useState<Vehicle | null>(null);
  const [deleteBusy, setDeleteBusy] = useState(false);

  const columns: Column<Vehicle>[] = [
    {
      key: 'registration_number', header: 'Registration',
      render: (v) => (
        <div>
          <p className="font-semibold text-slate-900">{v.registration_number}</p>
          <p className="text-[11px] text-slate-500">{v.vehicle_code}</p>
        </div>
      ),
    },
    {
      key: 'vehicle_type', header: 'Type',
      render: (v) => (
        <div>
          <Badge tone={vehicleTone(v.vehicle_type)}>{label(v.vehicle_type)}</Badge>
          <p className="mt-0.5 text-[11px] text-slate-500">{v.year} {v.make} {v.model}</p>
        </div>
      ),
      sortValue: (v) => v.vehicle_type,
      hideOnMobile: true,
    },
    { key: 'fuel_type', header: 'Fuel', render: (v) => <span className="text-[12px] text-slate-600">{label(v.fuel_type)}</span>, hideOnMobile: true },
    { key: 'current_mileage', header: 'Odometer', align: 'right', render: (v) => km(Number(v.current_mileage)), sortValue: (v) => Number(v.current_mileage) },
    {
      key: 'assigned_driver_name', header: 'Driver',
      render: (v) => (v.assigned_driver_name ? <span className="text-[12px] text-slate-700">{v.assigned_driver_name}</span> : <span className="text-[12px] text-slate-400">—</span>),
      hideOnMobile: true,
    },
    { key: 'status', header: 'Status', render: (v) => <Badge tone={statusTone(v.status)} dot>{label(v.status)}</Badge> },
  ];

  return (
    <>
      <DataPage<Vehicle>
        eyebrow="Fleet"
        title="Vehicles"
        description="Cars, trucks, buses, vans and motorcycles. Status follows a strict state machine and the odometer can never run backwards."
        endpoint="/api/vehicles"
        listKey="vehicles"
        rowKey={(v) => v.id}
        columns={columns}
        canWrite={canWrite}
        writeHint={canWrite ? undefined : 'Your role can view but not modify the fleet'}
        createLabel="Add vehicle"
        exportDataset="vehicles"
        filters={[
          { key: 'type', label: 'Type', options: typeOptions },
          { key: 'status', label: 'Status', options: statusOptions },
          { key: 'fuel', label: 'Fuel', options: fuelOptions },
        ]}
        stats={(rows) => [
          { label: 'Vehicles', value: num(rows.length) },
          { label: 'Available', value: num(rows.filter((r) => r.status === 'AVAILABLE').length) },
          { label: 'Assigned / in service', value: num(rows.filter((r) => ['ASSIGNED', 'IN_SERVICE'].includes(r.status)).length) },
          { label: 'Book value', value: money(rows.reduce((s, r) => s + Number(r.acquisition_cost), 0)) },
        ]}
        rowActions={canWrite ? (row, refresh) => (
          <>
            <IconAction title="Change status" onClick={() => changeStatus(row, refresh)}><Pencil className="h-3.5 w-3.5" /></IconAction>
            <IconAction title="Delete" onClick={() => setDeleteTarget(row)}><Trash2 className="h-3.5 w-3.5" /></IconAction>
          </>
        ) : undefined}
        renderForm={(vehicle, close) => <VehicleForm vehicle={vehicle} close={close} />}
      />

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        busy={deleteBusy}
        title="Move vehicle to recycle bin?"
        confirmLabel="Delete vehicle"
        message={<>Vehicle <b>{deleteTarget?.registration_number}</b> will be moved to the recycle bin. Records with trips or fuel history can be restored later; the database refuses permanent deletion while history exists.</>}
        onConfirm={async () => {
          if (!deleteTarget) return;
          setDeleteBusy(true);
          const result = await apiDelete(`/api/vehicles/${deleteTarget.id}`);
          setDeleteBusy(false);
          setDeleteTarget(null);
          if (!result.ok) { toast(result.error, 'error'); return; }
          toast('Vehicle moved to the recycle bin.');
          router.refresh();
        }}
      />
    </>
  );
}

function IconAction({ children, onClick, title }: { children: React.ReactNode; onClick: () => void; title: string }) {
  return (
    <button onClick={onClick} title={title} className="rounded-md p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700">
      {children}
    </button>
  );
}

async function apiDelete(url: string) {
  const res = await fetch(url, { method: 'DELETE' });
  const json = await res.json().catch(() => ({}));
  if (!res.ok) return { ok: false as const, error: json.error ?? 'Delete failed.' };
  return { ok: true as const, data: json };
}

function VehicleForm({ vehicle, close }: { vehicle: Vehicle | null; close: () => void }) {
  const [form, setForm] = useState({
    registration_number: vehicle?.registration_number ?? '',
    vehicle_type: vehicle?.vehicle_type ?? 'CAR',
    make: vehicle?.make ?? '',
    model: vehicle?.model ?? '',
    year: String(vehicle?.year ?? new Date().getFullYear()),
    color: vehicle?.color ?? '',
    vin: vehicle?.vin ?? '',
    engine_number: vehicle?.engine_number ?? '',
    fuel_type: vehicle?.fuel_type ?? 'PETROL',
    capacity: String(vehicle?.capacity ?? 4),
    current_mileage: String(vehicle?.current_mileage ?? 0),
    acquisition_cost: String(vehicle?.acquisition_cost ?? 0),
    acquisition_date: vehicle?.acquisition_date ?? '',
    status: vehicle?.status ?? 'AVAILABLE',
    attribute_number: String(vehicle?.attribute_number ?? (vehicle?.vehicle_type === 'TRUCK' ? 10 : vehicle?.vehicle_type === 'MOTORCYCLE' ? 2 : 4)),
    attribute_text: vehicle?.attribute_text ?? 'MANUAL',
    notes: vehicle?.notes ?? '',
  });
  const { busy, submit } = useFormSubmit(close);
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    setForm((prev) => ({ ...prev, [k]: e.target.value }));

  return (
    <div>
      <FormGrid>
        <Field label="Registration number" required hint="e.g. LND-482-KJA"><TextInput value={form.registration_number} onChange={set('registration_number')} placeholder="LND-482-KJA" /></Field>
        <Field label="Vehicle type" required>
          <Select value={form.vehicle_type} options={typeOptions} onChange={(e) => setForm((p) => ({
            ...p,
            vehicle_type: e.target.value as VehicleType,
            attribute_number: e.target.value === 'TRUCK' ? '10' : e.target.value === 'MOTORCYCLE' ? '2' : p.attribute_number,
            attribute_text: e.target.value === 'MOTORCYCLE' ? 'STANDARD' : p.attribute_text,
          }))} />
        </Field>
        <Field label="Make" required><TextInput value={form.make} onChange={set('make')} placeholder="Toyota" /></Field>
        <Field label="Model" required><TextInput value={form.model} onChange={set('model')} placeholder="Corolla" /></Field>
        <Field label="Year" required><TextInput type="number" value={form.year} onChange={set('year')} min={1950} max={new Date().getFullYear() + 1} /></Field>
        <Field label="Colour"><TextInput value={form.color} onChange={set('color')} placeholder="Silver" /></Field>
        <Field label="Fuel type" required><Select value={form.fuel_type} options={fuelOptions} onChange={set('fuel_type')} /></Field>
        <Field label="Capacity (seats)" hint="0–120"><TextInput type="number" value={form.capacity} onChange={set('capacity')} min={0} max={120} /></Field>
        <Field label="Current odometer (km)" required><TextInput type="number" value={form.current_mileage} onChange={set('current_mileage')} min={0} /></Field>
        <Field label="Acquisition cost (₦)"><TextInput type="number" value={form.acquisition_cost} onChange={set('acquisition_cost')} min={0} /></Field>
        <Field label="Acquisition date"><TextInput type="date" value={form.acquisition_date ?? ''} onChange={set('acquisition_date')} /></Field>
        <Field label="Status">
          <Select value={form.status} options={statusOptions} onChange={set('status')} />
        </Field>

        {form.vehicle_type === 'CAR' && <Field label="Doors" hint="2–6"><TextInput type="number" value={form.attribute_number} onChange={set('attribute_number')} min={2} max={6} /></Field>}
        {form.vehicle_type === 'CAR' && (
          <Field label="Transmission"><Select value={form.attribute_text ?? 'MANUAL'} options={[{ value: 'MANUAL', label: 'Manual' }, { value: 'AUTOMATIC', label: 'Automatic' }]} onChange={set('attribute_text')} /></Field>
        )}
        {form.vehicle_type === 'MOTORCYCLE' && <Field label="Wheels" hint="2 or 3"><TextInput type="number" value={form.attribute_number} onChange={set('attribute_number')} min={2} max={3} /></Field>}
        {form.vehicle_type === 'MOTORCYCLE' && (
          <Field label="Style">
            <Select value={form.attribute_text ?? 'STANDARD'} options={['SPORT', 'CRUISER', 'OFF_ROAD', 'STANDARD'].map((s) => ({ value: s, label: s.replace('_', ' ') }))} onChange={set('attribute_text')} />
          </Field>
        )}
        {form.vehicle_type === 'TRUCK' && <Field label="Cargo capacity (tons)" hint="Up to 60"><TextInput type="number" value={form.attribute_number} onChange={set('attribute_number')} min={1} max={60} /></Field>}
        {form.vehicle_type === 'TRUCK' && (
          <Field label="Transmission"><Select value={form.attribute_text ?? 'MANUAL'} options={[{ value: 'MANUAL', label: 'Manual' }, { value: 'AUTOMATIC', label: 'Automatic' }]} onChange={set('attribute_text')} /></Field>
        )}

        <Field label="VIN / chassis number" hint="5–20 characters"><TextInput value={form.vin ?? ''} onChange={set('vin')} /></Field>
        <Field label="Engine number"><TextInput value={form.engine_number ?? ''} onChange={set('engine_number')} /></Field>
        <Field label="Notes" className="sm:col-span-2"><TextArea value={form.notes ?? ''} onChange={set('notes')} placeholder="Optional notes (insurance policy, special fittings…)" /></Field>
      </FormGrid>

      <div className="mt-5 flex justify-end gap-2 border-t border-slate-100 pt-4">
        <Button variant="secondary" onClick={close} disabled={busy}>Cancel</Button>
        <Button
          loading={busy}
          onClick={() => submit(
            vehicle ? 'PUT' : 'POST',
            vehicle ? `/api/vehicles/${vehicle.id}` : '/api/vehicles',
            {
              ...form,
              year: Number(form.year), capacity: Number(form.capacity),
              current_mileage: Number(form.current_mileage), acquisition_cost: Number(form.acquisition_cost),
              attribute_number: form.attribute_number === '' ? null : Number(form.attribute_number),
            },
            vehicle ? 'Vehicle updated.' : 'Vehicle added to the fleet.',
          )}
        >
          {vehicle ? 'Save changes' : 'Add vehicle'}
        </Button>
      </div>
    </div>
  );
}

async function changeStatus(vehicle: Vehicle, refresh: () => void) {
  const allowed = VEHICLE_TRANSITIONS[vehicle.status as VehicleStatus] ?? [];
  const options = allowed.filter((s) => s !== 'ASSIGNED');
  if (!options.length) { toast('This vehicle is retired; no further status changes are allowed.', 'error'); return; }
  const next = window.prompt(
    `Change status of ${vehicle.registration_number} (currently ${label(vehicle.status)}).\n\nAllowed: ${options.map(label).join(', ')}\n\nType one of: ${options.join(', ')}`,
    options[0],
  );
  if (!next) return;
  if (!(options as string[]).includes(next.toUpperCase())) { toast('That status is not a legal transition from the current one.', 'error'); return; }
  const reason = window.prompt('Reason (recorded in the audit log):', '') ?? '';
  const result = await fetch(`/api/vehicles/${vehicle.id}`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ action: 'status', status: next.toUpperCase(), reason }),
  });
  const json = await result.json().catch(() => ({}));
  if (!result.ok) { toast(json.error ?? 'Status change failed.', 'error'); return; }
  toast(`Status changed to ${label(next.toUpperCase())}.`);
  refresh();
}
