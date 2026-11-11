'use client';

import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { Plus, RefreshCw, Filter, Download } from 'lucide-react';
import { api } from '@/lib/api';
import { Badge, Button, Card, LoadingBlock, Modal, SearchInput, Select, toast } from './ui';
import { DataTable, exportDataset, type Column } from './DataTable';

/**
 * Generic list screen: search, filters, KPI strip, table, create/edit modal and
 * row actions. Every module screen is a thin configuration of this component.
 */
export type FilterDef = {
  key: string;
  label: string;
  options: { value: string; label: string }[];
};

type Props<T> = {
  title: string;
  description?: string;
  eyebrow: string;
  /** API path returning { [listKey]: T[] } */
  endpoint: string;
  listKey: string;
  columns: Column<T>[];
  rowKey: (row: T) => string | number;
  filters?: FilterDef[];
  canWrite: boolean;
  writeHint?: string;
  createLabel?: string;
  /** Rendered inside the create/edit modal. Return the payload or throw. */
  renderForm?: (row: T | null, close: () => void) => ReactNode;
  actions?: ReactNode;
  stats?: (rows: T[]) => { label: string; value: string; hint?: string }[];
  onRowClick?: (row: T) => void;
  exportDataset?: string;
  clientFilter?: (row: T, search: string) => boolean;
  rowActions?: (row: T, refresh: () => void) => ReactNode;
  descriptionFooter?: ReactNode;
};

export function DataPage<T>({
  title, description, eyebrow, endpoint, listKey, columns, rowKey, filters = [], canWrite, writeHint,
  createLabel = 'New record', renderForm, actions, stats, onRowClick, exportDataset: dataset, clientFilter, rowActions,
  descriptionFooter,
}: Props<T>) {
  const [rows, setRows] = useState<T[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [filterValues, setFilterValues] = useState<Record<string, string>>({});
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<T | null>(null);
  const [showFilters, setShowFilters] = useState(false);

  const load = useCallback(async () => {
    setError(null);
    const params = new URLSearchParams();
    for (const [k, v] of Object.entries(filterValues)) if (v) params.set(k, v);
    const q = search.trim();
    if (q) params.set('q', q);
    const url = `${endpoint}${endpoint.includes('?') ? '&' : '?'}${params.toString()}`;
    const result = await api.get<Record<string, T[]>>(url);
    if (!result.ok) {
      setError(result.error);
      setRows([]);
      return;
    }
    setRows(result.data[listKey] ?? []);
  }, [endpoint, listKey, filterValues, search]);

  useEffect(() => {
    const timer = setTimeout(load, search ? 250 : 0);
    return () => clearTimeout(timer);
  }, [load, search]);

  const filtered = useMemo(() => {
    if (!rows) return null;
    if (!clientFilter || !search.trim()) return rows;
    return rows.filter((r) => clientFilter(r, search.trim().toLowerCase()));
  }, [rows, clientFilter, search]);

  const statCards = useMemo(() => (filtered && stats ? stats(filtered) : []), [filtered, stats]);

  function openCreate() { setEditing(null); setFormOpen(true); }
  function openEdit(row: T) { setEditing(row); setFormOpen(true); }

  const allColumns: Column<T>[] = rowActions
    ? [...columns, {
        key: '__actions', header: '', align: 'right',
        render: (row) => (
          <div className="flex items-center justify-end gap-1" onClick={(e) => e.stopPropagation()}>
            {rowActions(row, load)}
          </div>
        ),
      }]
    : columns;

  return (
    <div>
      <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="mb-1 text-[10px] font-bold uppercase tracking-[0.14em] text-blue-700">{eyebrow}</p>
          <h1 className="text-[22px] font-bold tracking-tight text-slate-900">{title}</h1>
          {description && <p className="mt-1 max-w-2xl text-[13px] leading-relaxed text-slate-500">{description}</p>}
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {actions}
          {dataset && (
            <Button variant="secondary" size="sm" icon={<Download className="h-3.5 w-3.5" />} onClick={() => exportDataset(dataset).catch((e) => toast((e as Error).message, 'error'))}>
              Export
            </Button>
          )}
          <Button variant="secondary" size="sm" icon={<RefreshCw className="h-3.5 w-3.5" />} onClick={load}>Refresh</Button>
          {canWrite && renderForm && (
            <Button size="sm" icon={<Plus className="h-3.5 w-3.5" />} onClick={openCreate}>{createLabel}</Button>
          )}
        </div>
      </div>

      {statCards.length > 0 && (
        <div className="mb-4 grid gap-2.5 sm:grid-cols-2 lg:grid-cols-4">
          {statCards.map((s) => (
            <Card key={s.label} className="px-4 py-3">
              <p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">{s.label}</p>
              <p className="mt-0.5 text-lg font-bold tabular-nums text-slate-900">{s.value}</p>
              {s.hint && <p className="text-[11px] text-slate-400">{s.hint}</p>}
            </Card>
          ))}
        </div>
      )}

      <Card>
        <div className="flex flex-wrap items-center gap-2 border-b border-slate-100 px-4 py-3">
          <SearchInput value={search} onChange={setSearch} placeholder={`Search ${title.toLowerCase()}…`} />
          {filters.length > 0 && (
            <Button variant="ghost" size="sm" icon={<Filter className="h-3.5 w-3.5" />} onClick={() => setShowFilters((v) => !v)}>
              Filters
              {Object.values(filterValues).filter(Boolean).length > 0 && (
                <Badge tone="bg-blue-50 text-blue-700 ring-blue-600/20">{Object.values(filterValues).filter(Boolean).length}</Badge>
              )}
            </Button>
          )}
          <div className="ml-auto flex items-center gap-2">
            {!canWrite && <span className="hidden text-[11px] text-slate-400 sm:inline">{writeHint ?? 'Read-only access for your role'}</span>}
            {rows && <span className="text-[11px] text-slate-500">{filtered?.length ?? 0} record{(filtered?.length ?? 0) === 1 ? '' : 's'}</span>}
          </div>
        </div>

        {showFilters && filters.length > 0 && (
          <div className="flex flex-wrap gap-3 border-b border-slate-100 bg-slate-50/60 px-4 py-3">
            {filters.map((f) => (
              <div key={f.key} className="w-full sm:w-44">
                <label className="mb-1 block text-[10px] font-semibold uppercase tracking-wider text-slate-500">{f.label}</label>
                <Select
                  value={filterValues[f.key] ?? ''}
                  placeholder={`All ${f.label.toLowerCase()}`}
                  options={f.options}
                  onChange={(e) => setFilterValues((prev) => ({ ...prev, [f.key]: e.target.value }))}
                />
              </div>
            ))}
            <div className="flex items-end">
              <Button variant="ghost" size="sm" onClick={() => setFilterValues({})}>Clear</Button>
            </div>
          </div>
        )}

        {error && <div className="border-b border-rose-100 bg-rose-50 px-4 py-2.5 text-[12px] font-medium text-rose-700">{error}</div>}

        {filtered === null
          ? <LoadingBlock />
          : (
            <DataTable
              columns={allColumns}
              rows={filtered}
              rowKey={rowKey}
              exportName={dataset ?? title.toLowerCase()}
              onRowClick={onRowClick ?? (canWrite && renderForm ? openEdit : undefined)}
              emptyTitle={`No ${title.toLowerCase()} found`}
              emptyMessage={search ? 'No record matches your search. Try a different term or clear the filters.' : 'Records you add will appear here.'}
              emptyAction={canWrite && renderForm ? <Button size="sm" icon={<Plus className="h-3.5 w-3.5" />} onClick={openCreate}>{createLabel}</Button> : undefined}
            />
          )}
        {descriptionFooter}
      </Card>

      {renderForm && (
        <Modal
          open={formOpen}
          onClose={() => setFormOpen(false)}
          title={editing ? `Edit ${title.toLowerCase().replace(/s$/, '')}` : createLabel}
          subtitle={editing ? 'Changes are validated and written to the audit trail.' : 'All required fields are marked with an asterisk.'}
        >
          {renderForm(editing, () => { setFormOpen(false); load(); })}
        </Modal>
      )}
    </div>
  );
}
