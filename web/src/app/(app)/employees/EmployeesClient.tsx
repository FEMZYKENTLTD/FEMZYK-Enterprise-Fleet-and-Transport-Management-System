'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Plus, RefreshCw, Trash2, Undo2, Download, Users, BadgeCheck } from 'lucide-react';
import { api } from '@/lib/api';
import type { Employee } from '@/lib/types';
import { EMPLOYEE_STATUSES, label, statusTone } from '@/lib/domain';
import { date, money, num } from '@/lib/format';
import { Badge, Button, Card, ConfirmDialog, Field, Modal, SearchInput, Select, TextArea, TextInput, LoadingBlock, EmptyState, toast } from '@/components/ui';
import { FormGrid, useFormSubmit } from '@/components/FormGrid';
import { DataTable, exportDataset, type Column } from '@/components/DataTable';

const statusOptions = EMPLOYEE_STATUSES.map((s) => ({ value: s, label: label(s) }));

export function EmployeesClient({ canWrite, initial, departments }: { canWrite: boolean; initial: Employee[]; departments: string[] }) {
  const [rows, setRows] = useState<Employee[]>(initial);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [department, setDepartment] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Employee | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Employee | null>(null);
  const [busy, setBusy] = useState(false);
  const [showDeleted, setShowDeleted] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    const params = new URLSearchParams();
    if (status) params.set('status', status);
    if (department) params.set('department', department);
    if (showDeleted) params.set('deleted', '1');
    const result = await api.get<{ employees: Employee[] }>(`/api/employees?${params}`);
    setLoading(false);
    if (!result.ok) { toast(result.error, 'error'); return; }
    setRows(result.data.employees);
  }, [status, department, showDeleted]);

  useEffect(() => { void load(); }, [load]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter((r) =>
      [r.first_name, r.last_name, r.employee_code, r.email, r.phone, r.department, r.position]
        .some((v) => (v ?? '').toLowerCase().includes(q)));
  }, [rows, search]);

  const departmentsList = useMemo(() => [...new Set([...departments, ...rows.map((r) => r.department)])].sort(), [departments, rows]);

  const columns: Column<Employee>[] = [
    {
      key: 'last_name', header: 'Employee',
      render: (r) => (
        <div>
          <p className="font-semibold text-slate-900">{r.first_name} {r.last_name}</p>
          <p className="text-[11px] text-slate-500">{r.employee_code}{r.position ? ` · ${r.position}` : ''}</p>
        </div>
      ),
      sortValue: (r) => `${r.last_name} ${r.first_name}`,
    },
    { key: 'department', header: 'Department', hideOnMobile: true },
    { key: 'phone', header: 'Phone', render: (r) => r.phone ?? '—', hideOnMobile: true },
    { key: 'email', header: 'E-mail', render: (r) => <span className="text-[12px] text-slate-600">{r.email ?? '—'}</span>, hideOnMobile: true },
    { key: 'hire_date', header: 'Hired', render: (r) => date(r.hire_date), hideOnMobile: true },
    { key: 'salary', header: 'Salary', align: 'right', render: (r) => (r.salary ? money(r.salary) : '—'), sortValue: (r) => Number(r.salary ?? 0) },
    { key: 'status', header: 'Status', render: (r) => <Badge tone={statusTone(r.status)} dot>{label(r.status)}</Badge> },
    ...(canWrite ? [{
      key: '__actions', header: '', align: 'right' as const,
      render: (r: Employee) => (
        <div className="flex justify-end gap-1" onClick={(e) => e.stopPropagation()}>
          {r.deleted_at ? (
            <button title="Restore" onClick={async () => {
              const res = await api.put(`/api/employees/${r.id}`, { action: 'restore' });
              if (!res.ok) { toast(res.error, 'error'); return; }
              toast('Employee restored.'); void load();
            }} className="rounded p-1.5 text-slate-400 hover:bg-slate-100 hover:text-emerald-600"><Undo2 className="h-3.5 w-3.5" /></button>
          ) : (
            <button title="Delete" onClick={() => setDeleteTarget(r)} className="rounded p-1.5 text-slate-400 hover:bg-slate-100 hover:text-rose-600"><Trash2 className="h-3.5 w-3.5" /></button>
          )}
        </div>
      ),
    }] : []),
  ];

  const active = rows.filter((r) => r.status === 'ACTIVE' && !r.deleted_at);
  const payroll = active.reduce((s, r) => s + Number(r.salary ?? 0), 0);
  const deptCount = new Set(rows.map((r) => r.department)).size;

  return (
    <div>
      <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="mb-1 text-[10px] font-bold uppercase tracking-[0.14em] text-blue-700">People</p>
          <h1 className="text-[22px] font-bold tracking-tight text-slate-900">Employees</h1>
          <p className="mt-1 max-w-2xl text-[13px] leading-relaxed text-slate-500">
            Master list of staff. Driver profiles are created from an existing employee, so every driver is also on the payroll record.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" size="sm" icon={<Download className="h-3.5 w-3.5" />} onClick={() => exportDataset('employees').catch((e) => toast((e as Error).message, 'error'))}>Export</Button>
          <Button variant="secondary" size="sm" icon={<RefreshCw className="h-3.5 w-3.5" />} onClick={load}>Refresh</Button>
          {canWrite && <Button size="sm" icon={<Plus className="h-3.5 w-3.5" />} onClick={() => { setEditing(null); setFormOpen(true); }}>Add employee</Button>}
        </div>
      </div>

      <div className="mb-4 grid gap-2.5 sm:grid-cols-2 lg:grid-cols-4">
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Employees</p><p className="text-lg font-bold tabular-nums">{num(rows.length)}</p></Card>
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Active</p><p className="text-lg font-bold tabular-nums text-emerald-700">{num(active.length)}</p></Card>
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Departments</p><p className="text-lg font-bold tabular-nums">{num(deptCount)}</p></Card>
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Monthly payroll</p><p className="text-lg font-bold tabular-nums">{money(payroll)}</p></Card>
      </div>

      <Card>
        <div className="flex flex-wrap items-center gap-2 border-b border-slate-100 px-4 py-3">
          <SearchInput value={search} onChange={setSearch} placeholder="Search employees…" />
          <div className="w-44"><Select value={status} placeholder="All statuses" options={statusOptions} onChange={(e) => setStatus(e.target.value)} /></div>
          <div className="w-44"><Select value={department} placeholder="All departments" options={departmentsList.map((d) => ({ value: d, label: d }))} onChange={(e) => setDepartment(e.target.value)} /></div>
          <label className="ml-auto flex items-center gap-1.5 text-[11px] text-slate-500">
            <input type="checkbox" checked={showDeleted} onChange={(e) => setShowDeleted(e.target.checked)} className="rounded border-slate-300" />
            Include deleted
          </label>
        </div>
        {loading ? <LoadingBlock /> : filtered.length === 0 ? (
          <EmptyState title="No employees found" message={search ? 'No match for your search.' : 'Add the first employee to get started.'}
            action={canWrite ? <Button size="sm" icon={<Plus className="h-3.5 w-3.5" />} onClick={() => { setEditing(null); setFormOpen(true); }}>Add employee</Button> : undefined}
            icon={<Users className="h-6 w-6" />} />
        ) : (
          <DataTable
            columns={columns}
            rows={filtered}
            rowKey={(r) => r.id}
            exportName="employees"
            onRowClick={canWrite ? (r) => { setEditing(r); setFormOpen(true); } : undefined}
          />
        )}
      </Card>

      <Modal open={formOpen} onClose={() => setFormOpen(false)} title={editing ? 'Edit employee' : 'Add employee'} subtitle="Employee codes are generated automatically (EMP-0001…).">
        <EmployeeForm employee={editing} close={() => setFormOpen(false)} onSaved={load} departments={departmentsList} />
      </Modal>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        busy={busy}
        title="Move employee to recycle bin?"
        confirmLabel="Delete employee"
        message={<>Employee <b>{deleteTarget?.first_name} {deleteTarget?.last_name}</b> will be moved to the recycle bin. Employees with a driver profile must have that profile removed first.</>}
        onConfirm={async () => {
          if (!deleteTarget) return;
          setBusy(true);
          const result = await api.del(`/api/employees/${deleteTarget.id}`);
          setBusy(false);
          setDeleteTarget(null);
          if (!result.ok) { toast(result.error, 'error'); return; }
          toast('Employee moved to the recycle bin.');
          void load();
        }}
      />
    </div>
  );
}

function EmployeeForm({ employee, close, onSaved, departments }: { employee: Employee | null; close: () => void; onSaved: () => void; departments: string[] }) {
  const [form, setForm] = useState({
    first_name: employee?.first_name ?? '',
    last_name: employee?.last_name ?? '',
    phone: employee?.phone ?? '',
    email: employee?.email ?? '',
    address: employee?.address ?? '',
    department: employee?.department ?? (departments[0] ?? 'Operations'),
    position: employee?.position ?? '',
    date_of_birth: employee?.date_of_birth ?? '',
    hire_date: employee?.hire_date ?? new Date().toISOString().slice(0, 10),
    salary: employee?.salary !== null && employee?.salary !== undefined ? String(employee.salary) : '',
    status: employee?.status ?? 'ACTIVE',
    emergency_contact_name: employee?.emergency_contact_name ?? '',
    emergency_contact_phone: employee?.emergency_contact_phone ?? '',
    notes: employee?.notes ?? '',
  });
  const { busy, submit } = useFormSubmit(() => { onSaved(); close(); });
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    setForm((p) => ({ ...p, [k]: e.target.value }));

  return (
    <div>
      <FormGrid>
        <Field label="First name" required><TextInput value={form.first_name} onChange={set('first_name')} /></Field>
        <Field label="Last name" required><TextInput value={form.last_name} onChange={set('last_name')} /></Field>
        <Field label="Department" required>
          <TextInput value={form.department} onChange={set('department')} list="departments" placeholder="Operations" />
          <datalist id="departments">{departments.map((d) => <option key={d} value={d} />)}</datalist>
        </Field>
        <Field label="Position"><TextInput value={form.position} onChange={set('position')} placeholder="Fleet Manager" /></Field>
        <Field label="Phone"><TextInput value={form.phone} onChange={set('phone')} placeholder="0803…" /></Field>
        <Field label="E-mail"><TextInput type="email" value={form.email} onChange={set('email')} placeholder="name@company.com" /></Field>
        <Field label="Hire date" required><TextInput type="date" value={form.hire_date} onChange={set('hire_date')} /></Field>
        <Field label="Date of birth"><TextInput type="date" value={form.date_of_birth ?? ''} onChange={set('date_of_birth')} /></Field>
        <Field label="Monthly salary (₦)"><TextInput type="number" value={form.salary} onChange={set('salary')} min={0} /></Field>
        <Field label="Employment status"><Select value={form.status} options={statusOptions} onChange={set('status')} /></Field>
        <Field label="Emergency contact"><TextInput value={form.emergency_contact_name} onChange={set('emergency_contact_name')} /></Field>
        <Field label="Emergency phone"><TextInput value={form.emergency_contact_phone} onChange={set('emergency_contact_phone')} /></Field>
        <Field label="Address" className="sm:col-span-2"><TextInput value={form.address} onChange={set('address')} placeholder="Street, city, state" /></Field>
        <Field label="Notes" className="sm:col-span-2"><TextArea value={form.notes} onChange={set('notes')} /></Field>
      </FormGrid>
      <div className="mt-5 flex items-center justify-end gap-2 border-t border-slate-100 pt-4">
        <Button variant="secondary" onClick={close} disabled={busy}>Cancel</Button>
        <Button loading={busy} onClick={() => submit(employee ? 'PUT' : 'POST', employee ? `/api/employees/${employee.id}` : '/api/employees', form, employee ? 'Employee updated.' : 'Employee added.')}>
          {employee ? 'Save changes' : 'Add employee'}
        </Button>
      </div>
    </div>
  );
}
