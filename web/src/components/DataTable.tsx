'use client';

import { useMemo, useState, type ReactNode } from 'react';
import { ArrowDownUp, ChevronLeft, ChevronRight, Download, Inbox } from 'lucide-react';
import { EmptyState } from './ui';
import { downloadCsv } from '@/lib/api';

export type Column<T> = {
  key: string;
  header: string;
  render?: (row: T) => ReactNode;
  align?: 'left' | 'right' | 'center';
  sortValue?: (row: T) => string | number;
  className?: string;
  hideOnMobile?: boolean;
};

type Props<T> = {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string | number;
  onRowClick?: (row: T) => void;
  emptyTitle?: string;
  emptyMessage?: string;
  emptyAction?: ReactNode;
  pageSize?: number;
  exportName?: string;
  dense?: boolean;
};

export function DataTable<T>({
  columns, rows, rowKey, onRowClick, emptyTitle = 'Nothing to show', emptyMessage,
  emptyAction, pageSize = 25, exportName, dense,
}: Props<T>) {
  const [sort, setSort] = useState<{ key: string; dir: 'asc' | 'desc' } | null>(null);
  const [page, setPage] = useState(0);

  const sorted = useMemo(() => {
    if (!sort) return rows;
    const col = columns.find((c) => c.key === sort.key);
    if (!col) return rows;
    const value = (row: T) => (col.sortValue ? col.sortValue(row) : String((row as Record<string, unknown>)[col.key] ?? ''));
    return [...rows].sort((a, b) => {
      const va = value(a);
      const vb = value(b);
      if (typeof va === 'number' && typeof vb === 'number') return sort.dir === 'asc' ? va - vb : vb - va;
      return sort.dir === 'asc' ? String(va).localeCompare(String(vb)) : String(vb).localeCompare(String(va));
    });
  }, [rows, sort, columns]);

  const pageCount = Math.max(1, Math.ceil(sorted.length / pageSize));
  const safePage = Math.min(page, pageCount - 1);
  const visible = sorted.slice(safePage * pageSize, safePage * pageSize + pageSize);

  function toggleSort(key: string) {
    setSort((prev) => (prev?.key === key ? { key, dir: prev.dir === 'asc' ? 'desc' : 'asc' } : { key, dir: 'asc' }));
    setPage(0);
  }

  function exportCsv() {
    const data = sorted.map((row) => {
      const out: Record<string, unknown> = {};
      for (const c of columns) {
        const raw = (row as Record<string, unknown>)[c.key];
        out[c.key] = c.sortValue ? c.sortValue(row) : raw;
      }
      return out;
    });
    const csv = [columns.map((c) => c.header).join(','), ...data.map((r) => columns.map((c) => {
      const v = r[c.key];
      const s = v === null || v === undefined ? '' : String(v);
      return /[",\n]/.test(s) ? `"${s.replaceAll('"', '""')}"` : s;
    }).join(','))].join('\r\n');
    const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `${exportName ?? 'export'}-${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
  }

  if (!rows.length) {
    return <EmptyState title={emptyTitle} message={emptyMessage} action={emptyAction} icon={<Inbox className="h-6 w-6" />} />;
  }

  return (
    <div>
      <div className="thin-scroll overflow-x-auto">
        <table className="w-full min-w-[640px] border-collapse text-sm">
          <thead>
            <tr className="border-b border-slate-200 bg-slate-50/80">
              {columns.map((c) => (
                <th
                  key={c.key}
                  onClick={() => toggleSort(c.key)}
                  className={`cursor-pointer select-none whitespace-nowrap px-3 py-2.5 text-[11px] font-semibold uppercase tracking-wider text-slate-500 transition hover:text-slate-800 ${
                    c.align === 'right' ? 'text-right' : c.align === 'center' ? 'text-center' : 'text-left'
                  } ${c.hideOnMobile ? 'hidden md:table-cell' : ''}`}
                >
                  <span className="inline-flex items-center gap-1">
                    {c.header}
                    {sort?.key === c.key && <ArrowDownUp className="h-3 w-3 opacity-70" />}
                  </span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {visible.map((row) => (
              <tr
                key={rowKey(row)}
                onClick={onRowClick ? () => onRowClick(row) : undefined}
                className={`border-b border-slate-100 last:border-0 ${onRowClick ? 'cursor-pointer hover:bg-blue-50/40' : 'hover:bg-slate-50/60'}`}
              >
                {columns.map((c) => (
                  <td
                    key={c.key}
                    className={`px-3 align-middle text-slate-700 ${dense ? 'py-2' : 'py-2.5'} ${
                      c.align === 'right' ? 'text-right tabular-nums' : c.align === 'center' ? 'text-center' : ''
                    } ${c.hideOnMobile ? 'hidden md:table-cell' : ''} ${c.className ?? ''}`}
                  >
                    {c.render ? c.render(row) : String((row as Record<string, unknown>)[c.key] ?? '—')}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-2 border-t border-slate-100 px-3 py-2.5">
        <p className="text-[11px] text-slate-500">
          {sorted.length === 0 ? 'No records' : `${safePage * pageSize + 1}–${Math.min((safePage + 1) * pageSize, sorted.length)} of ${sorted.length}`}
        </p>
        <div className="flex items-center gap-1.5">
          {exportName && (
            <button onClick={exportCsv} className="mr-1 inline-flex items-center gap-1 rounded-md border border-slate-200 px-2 py-1 text-[11px] font-medium text-slate-600 hover:bg-slate-50">
              <Download className="h-3 w-3" /> CSV
            </button>
          )}
          <button
            onClick={() => setPage(Math.max(0, safePage - 1))}
            disabled={safePage === 0}
            className="rounded-md border border-slate-200 p-1 text-slate-500 transition hover:bg-slate-50 disabled:opacity-40"
            aria-label="Previous page"
          >
            <ChevronLeft className="h-3.5 w-3.5" />
          </button>
          <span className="px-1 text-[11px] font-medium text-slate-600">{safePage + 1} / {pageCount}</span>
          <button
            onClick={() => setPage(Math.min(pageCount - 1, safePage + 1))}
            disabled={safePage >= pageCount - 1}
            className="rounded-md border border-slate-200 p-1 text-slate-500 transition hover:bg-slate-50 disabled:opacity-40"
            aria-label="Next page"
          >
            <ChevronRight className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>
    </div>
  );
}

/** Convenience wrapper used by pages that load a file client-side. */
export async function exportDataset(dataset: string) {
  await downloadCsv(`/api/export?dataset=${dataset}`, `femzyk-${dataset}-${new Date().toISOString().slice(0, 10)}.csv`);
}
