'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Plus, RefreshCw, Trash2, Download, ReceiptText, Lock } from 'lucide-react';
import { api } from '@/lib/api';
import type { Expense } from '@/lib/types';
import { EXPENSE_CATEGORIES, label } from '@/lib/domain';
import { date, money } from '@/lib/format';
import { Badge, Button, Card, ConfirmDialog, EmptyState, Field, LoadingBlock, Modal, SearchInput, Select, TextArea, TextInput, toast } from '@/components/ui';
import { FormGrid, useFormSubmit } from '@/components/FormGrid';
import { DataTable, exportDataset, type Column } from '@/components/DataTable';

export function ExpensesClient({
  canWrite, initial, vehicles,
}: { canWrite: boolean; initial: Expense[]; vehicles: { value: string; label: string }[] }) {
  const [rows, setRows] = useState<Expense[]>(initial);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('');
  const [manualOnly, setManualOnly] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Expense | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Expense | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    const params = new URLSearchParams();
    if (category) params.set('category', category);
    if (manualOnly) params.set('manual', '1');
    const result = await api.get<{ expenses: Expense[] }>(`/api/expenses?${params}`);
    setLoading(false);
    if (!result.ok) { toast(result.error, 'error'); return; }
    setRows(result.data.expenses);
  }, [category, manualOnly]);

  useEffect(() => { void load(); }, [load]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter((r) => [r.description, r.vendor, r.reference, r.registration_number, r.category].some((v) => (v ?? '').toLowerCase().includes(q)));
  }, [rows, search]);

  const columns: Column<Expense>[] = [
    {
      key: 'description', header: 'Description',
      render: (r) => (
        <div className="flex items-start gap-1.5">
          <div>
            <p className="font-semibold text-slate-900">{r.description}</p>
            <p className="text-[11px] text-slate-500">{r.registration_number ?? 'Fleet-wide'}{r.vendor ? ` · ${r.vendor}` : ''}</p>
          </div>
          {r.system_generated && <span title="Automatically generated"><Lock className="mt-0.5 h-3 w-3 shrink-0 text-slate-400" /></span>}
        </div>
      ),
    },
    { key: 'category', header: 'Category', render: (r) => <Badge tone="bg-slate-100 text-slate-700 ring-slate-500/20">{label(r.category)}</Badge> },
    { key: 'expense_date', header: 'Date', render: (r) => date(r.expense_date), sortValue: (r) => r.expense_date },
    { key: 'reference', header: 'Reference', render: (r) => <span className="font-mono text-[11px]">{r.reference ?? '—'}</span>, hideOnMobile: true },
    { key: 'amount', header: 'Amount', align: 'right', render: (r) => <span className="font-semibold">{money(r.amount, true)}</span>, sortValue: (r) => Number(r.amount) },
    ...(canWrite ? [{
      key: '__actions', header: '', align: 'right' as const,
      render: (r: Expense) => (
        <div className="flex justify-end" onClick={(e) => e.stopPropagation()}>
          {r.system_generated
            ? <span className="px-1 text-[10px] text-slate-300" title="Edit the fuel or maintenance record instead">auto</span>
            : <button title="Delete" onClick={() => setDeleteTarget(r)} className="rounded p-1.5 text-slate-400 hover:bg-slate-100 hover:text-rose-600"><Trash2 className="h-3.5 w-3.5" /></button>}
        </div>
      ),
    }] : []),
  ];

  const total = rows.reduce((s, r) => s + Number(r.amount), 0);
  const month = `${new Date().toISOString().slice(0, 7)}`;
  const monthTotal = rows.filter((r) => r.expense_date.startsWith(month)).reduce((s, r) => s + Number(r.amount), 0);
  const auto = rows.filter((r) => r.system_generated).length;

  return (
    <div>
      <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="mb-1 text-[10px] font-bold uppercase tracking-[0.14em] text-blue-700">Operations</p>
          <h1 className="text-[22px] font-bold tracking-tight text-slate-900">Expenses</h1>
          <p className="mt-1 max-w-2xl text-[13px] leading-relaxed text-slate-500">
            All running costs in one ledger. Fuel and maintenance costs are mirrored here automatically and are read-only —
            edit the source record instead.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" size="sm" icon={<Download className="h-3.5 w-3.5" />} onClick={() => exportDataset('expenses').catch((e) => toast((e as Error).message, 'error'))}>Export</Button>
          <Button variant="secondary" size="sm" icon={<RefreshCw className="h-3.5 w-3.5" />} onClick={load}>Refresh</Button>
          {canWrite && <Button size="sm" icon={<Plus className="h-3.5 w-3.5" />} onClick={() => { setEditing(null); setFormOpen(true); }}>Add expense</Button>}
        </div>
      </div>

      <div className="mb-4 grid gap-2.5 sm:grid-cols-2 lg:grid-cols-4">
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Records</p><p className="text-lg font-bold tabular-nums">{rows.length}</p></Card>
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">This month</p><p className="text-lg font-bold tabular-nums">{money(monthTotal)}</p></Card>
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Total shown</p><p className="text-lg font-bold tabular-nums">{money(total)}</p></Card>
        <Card className="px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Auto-generated</p><p className="text-lg font-bold tabular-nums text-sky-700">{auto}</p></Card>
      </div>

      <Card>
        <div className="flex flex-wrap items-center gap-2 border-b border-slate-100 px-4 py-3">
          <SearchInput value={search} onChange={setSearch} placeholder="Search expenses…" />
          <div className="w-48"><Select value={category} placeholder="All categories" options={EXPENSE_CATEGORIES.map((c) => ({ value: c, label: label(c) }))} onChange={(e) => setCategory(e.target.value)} /></div>
          <label className="ml-auto flex items-center gap-1.5 text-[11px] text-slate-500">
            <input type="checkbox" checked={manualOnly} onChange={(e) => setManualOnly(e.target.checked)} className="rounded border-slate-300" />
            Manual only
          </label>
        </div>
        {loading ? <LoadingBlock /> : filtered.length === 0 ? (
          <EmptyState title="No expenses" message="Record insurance, registration, tolls, fines and other running costs here." icon={<ReceiptText className="h-6 w-6" />}
            action={canWrite ? <Button size="sm" onClick={() => { setEditing(null); setFormOpen(true); }}>Add expense</Button> : undefined} />
        ) : <DataTable columns={columns} rows={filtered} rowKey={(r) => r.id} exportName="expenses"
          onRowClick={canWrite ? (r) => { if (r.system_generated) { toast('Automatic expense — edit the fuel or maintenance record instead.', 'info'); return; } setEditing(r); setFormOpen(true); } : undefined} />}
      </Card>

      <Modal open={formOpen} onClose={() => setFormOpen(false)} title={editing ? 'Edit expense' : 'Add an expense'} width="max-w-xl">
        <ExpenseForm expense={editing} vehicles={vehicles} close={() => { setFormOpen(false); void load(); }} />
      </Modal>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        busy={busy}
        title="Delete expense?"
        confirmLabel="Delete"
        message={<>The expense <b>{deleteTarget?.description}</b> ({money(deleteTarget?.amount ?? 0, true)}) will be removed permanently.</>}
        onConfirm={async () => {
          if (!deleteTarget) return;
          setBusy(true);
          const result = await api.del(`/api/expenses/${deleteTarget.id}`);
          setBusy(false);
          setDeleteTarget(null);
          if (!result.ok) { toast(result.error, 'error'); return; }
          toast('Expense deleted.');
          void load();
        }}
      />
    </div>
  );
}

function ExpenseForm({ expense, vehicles, close }: { expense: Expense | null; vehicles: { value: string; label: string }[]; close: () => void }) {
  const [form, setForm] = useState({
    vehicle_id: expense?.vehicle_id ? String(expense.vehicle_id) : '',
    expense_date: expense?.expense_date ?? new Date().toISOString().slice(0, 10),
    category: expense?.category ?? 'MISCELLANEOUS',
    amount: expense ? String(expense.amount) : '',
    vendor: expense?.vendor ?? '',
    description: expense?.description ?? '',
    reference: expense?.reference ?? '',
    notes: expense?.notes ?? '',
  });
  const { busy, submit } = useFormSubmit(close);
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    setForm((p) => ({ ...p, [k]: e.target.value }));

  return (
    <div>
      <FormGrid>
        <Field label="Description" required className="sm:col-span-2"><TextInput value={form.description} onChange={set('description')} placeholder="Comprehensive insurance renewal" /></Field>
        <Field label="Category" required><Select value={form.category} options={EXPENSE_CATEGORIES.map((c) => ({ value: c, label: label(c) }))} onChange={set('category')} /></Field>
        <Field label="Amount (₦)" required><TextInput type="number" step="0.01" value={form.amount} onChange={set('amount')} min={0.01} /></Field>
        <Field label="Date" required><TextInput type="date" value={form.expense_date} onChange={set('expense_date')} /></Field>
        <Field label="Vehicle" hint="Leave blank for fleet-wide costs"><Select value={form.vehicle_id} placeholder="Fleet-wide" options={vehicles} onChange={set('vehicle_id')} /></Field>
        <Field label="Vendor"><TextInput value={form.vendor} onChange={set('vendor')} placeholder="Leadway Assurance" /></Field>
        <Field label="Reference"><TextInput value={form.reference} onChange={set('reference')} placeholder="INS-2024-0042" /></Field>
        <Field label="Notes" className="sm:col-span-2"><TextArea value={form.notes} onChange={set('notes')} /></Field>
      </FormGrid>
      <div className="mt-5 flex justify-end gap-2 border-t border-slate-100 pt-4">
        <Button variant="secondary" onClick={close} disabled={busy}>Cancel</Button>
        <Button loading={busy} disabled={!form.description || !form.amount || !form.expense_date}
          onClick={() => submit(expense ? 'PUT' : 'POST', expense ? `/api/expenses/${expense.id}` : '/api/expenses', {
            ...form,
            vehicle_id: form.vehicle_id === '' ? null : Number(form.vehicle_id),
            amount: Number(form.amount),
          }, expense ? 'Expense updated.' : 'Expense recorded.')}>
          {expense ? 'Save changes' : 'Add expense'}
        </Button>
      </div>
    </div>
  );
}
