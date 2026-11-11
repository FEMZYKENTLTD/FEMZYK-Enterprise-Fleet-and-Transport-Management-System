'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { RefreshCw, ScrollText, Download } from 'lucide-react';
import { api } from '@/lib/api';
import type { AuditLog } from '@/lib/types';
import { dateTime } from '@/lib/format';
import { Badge, Button, Card, EmptyState, LoadingBlock, SearchInput, toast } from '@/components/ui';

const ACTION_TONE: Record<string, string> = {
  LOGIN: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  LOGOUT: 'bg-slate-100 text-slate-600 ring-slate-500/20',
  LOGIN_FAILED: 'bg-rose-50 text-rose-700 ring-rose-600/20',
  CREATE: 'bg-blue-50 text-blue-700 ring-blue-600/20',
  UPDATE: 'bg-sky-50 text-sky-700 ring-sky-600/20',
  DELETE: 'bg-rose-50 text-rose-700 ring-rose-600/20',
  STATUS: 'bg-amber-50 text-amber-800 ring-amber-600/20',
  ASSIGN: 'bg-indigo-50 text-indigo-700 ring-indigo-600/20',
  RETURN: 'bg-teal-50 text-teal-700 ring-teal-600/20',
  COMPLETE: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  PASSWORD_CHANGE: 'bg-violet-50 text-violet-700 ring-violet-600/20',
  PASSWORD_RESET: 'bg-violet-50 text-violet-700 ring-violet-600/20',
};

export function AuditClient({ canView, initial }: { canView: boolean; initial: AuditLog[] }) {
  const [rows, setRows] = useState<AuditLog[]>(initial);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);
  const [limit, setLimit] = useState(300);

  const load = useCallback(async () => {
    setLoading(true);
    const result = await api.get<{ audit: AuditLog[] }>(`/api/audit?limit=${limit}`);
    setLoading(false);
    if (!result.ok) { toast(result.error, 'error'); return; }
    setRows(result.data.audit);
  }, [limit]);

  useEffect(() => { if (canView) void load(); }, [load, canView]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter((r) => [r.username, r.action, r.module, r.reference, r.description].some((v) => (v ?? '').toLowerCase().includes(q)));
  }, [rows, search]);

  if (!canView) {
    return <Card className="p-8 text-center text-sm text-slate-500">The audit log is restricted to administrators.</Card>;
  }

  return (
    <div>
      <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="mb-1 text-[10px] font-bold uppercase tracking-[0.14em] text-blue-700">Insight</p>
          <h1 className="text-[22px] font-bold tracking-tight text-slate-900">Audit log</h1>
          <p className="mt-1 max-w-2xl text-[13px] leading-relaxed text-slate-500">
            Every sign-in, failed attempt, record change, status transition and export is recorded with the user, module and
            reference. Entries are written by the service layer, not the UI.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" size="sm" icon={<Download className="h-3.5 w-3.5" />}
            onClick={() => window.open('/api/reports?kind=audit&format=csv', '_blank', 'noopener')}>Export CSV</Button>
          <Button variant="secondary" size="sm" icon={<RefreshCw className="h-3.5 w-3.5" />} onClick={load}>Refresh</Button>
        </div>
      </div>

      <Card>
        <div className="flex flex-wrap items-center gap-2 border-b border-slate-100 px-4 py-3">
          <SearchInput value={search} onChange={setSearch} placeholder="Search by user, action, module…" />
          <select value={limit} onChange={(e) => setLimit(Number(e.target.value))} className="h-9 rounded-lg border border-slate-300 bg-white px-2 text-xs">
            <option value={100}>Last 100</option>
            <option value={300}>Last 300</option>
            <option value={1000}>Last 1,000</option>
          </select>
          <span className="ml-auto text-[11px] text-slate-500">{filtered.length} event{filtered.length === 1 ? '' : 's'}</span>
        </div>

        {loading ? <LoadingBlock /> : filtered.length === 0 ? (
          <EmptyState title="No audit events" message="Activity will appear here as users work." icon={<ScrollText className="h-6 w-6" />} />
        ) : (
          <div className="thin-scroll max-h-[70vh] overflow-auto">
            <table className="w-full min-w-[720px] border-collapse text-[12px]">
              <thead className="sticky top-0 z-10">
                <tr className="border-b border-slate-200 bg-slate-50">
                  {['When', 'User', 'Action', 'Module', 'Reference', 'Description'].map((h) => (
                    <th key={h} className="whitespace-nowrap px-3 py-2 text-left text-[10px] font-bold uppercase tracking-wider text-slate-500">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {filtered.map((r) => (
                  <tr key={r.id} className="border-b border-slate-100 last:border-0 hover:bg-slate-50/60">
                    <td className="whitespace-nowrap px-3 py-1.5 text-slate-500">{dateTime(r.timestamp)}</td>
                    <td className="whitespace-nowrap px-3 py-1.5 font-medium text-slate-800">{r.username}</td>
                    <td className="whitespace-nowrap px-3 py-1.5">
                      <Badge tone={ACTION_TONE[r.action] ?? 'bg-slate-100 text-slate-600 ring-slate-500/20'}>{r.action}</Badge>
                    </td>
                    <td className="whitespace-nowrap px-3 py-1.5 text-slate-600">{r.module}</td>
                    <td className="whitespace-nowrap px-3 py-1.5 font-mono text-[11px] text-slate-500">{r.reference ?? '—'}</td>
                    <td className="px-3 py-1.5 text-slate-600">{r.description ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}
