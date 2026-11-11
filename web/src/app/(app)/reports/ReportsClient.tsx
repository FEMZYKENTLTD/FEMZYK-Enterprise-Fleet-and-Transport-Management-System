'use client';

import { useCallback, useEffect, useState } from 'react';
import { Download, FileSpreadsheet, Printer, RefreshCw, BarChart3 } from 'lucide-react';
import { api, downloadCsv } from '@/lib/api';
import { Badge, Button, Card, LoadingBlock, toast } from '@/components/ui';

type ReportKind = { id: string; label: string; description: string };
type Report = {
  kind: string;
  title: string;
  generatedAt: string;
  columns: { key: string; label: string; align?: 'left' | 'right' }[];
  rows: Record<string, unknown>[];
  totals?: Record<string, number>;
};

export function ReportsClient({ canView }: { canView: boolean }) {
  const [kinds, setKinds] = useState<ReportKind[]>([]);
  const [active, setActive] = useState<string>('fleet');
  const [report, setReport] = useState<Report | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async (kind: string) => {
    setLoading(true);
    const result = await api.get<{ report: Report; kinds: ReportKind[] }>(`/api/reports?kind=${kind}`);
    setLoading(false);
    if (!result.ok) { toast(result.error, 'error'); return; }
    setReport(result.data.report);
    setKinds(result.data.kinds);
  }, []);

  useEffect(() => { void load(active); }, [active, load]);

  const money = (n: number) => new Intl.NumberFormat('en-NG', { style: 'currency', currency: 'NGN', maximumFractionDigits: 2 }).format(n);

  if (!canView) {
    return <Card className="p-8 text-center text-sm text-slate-500">Your role does not include report access.</Card>;
  }

  return (
    <div>
      <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="mb-1 text-[10px] font-bold uppercase tracking-[0.14em] text-blue-700">Insight</p>
          <h1 className="text-[22px] font-bold tracking-tight text-slate-900">Reports</h1>
          <p className="mt-1 max-w-2xl text-[13px] leading-relaxed text-slate-500">
            Eleven live reports computed from the database. Download any report as CSV, or open the printable HTML version for
            PDF export via your browser.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" size="sm" icon={<RefreshCw className="h-3.5 w-3.5" />} onClick={() => load(active)}>Refresh</Button>
          <Button variant="secondary" size="sm" icon={<FileSpreadsheet className="h-3.5 w-3.5" />}
            onClick={() => downloadCsv(`/api/reports?kind=${active}&format=csv`, `femzyk-${active}-${new Date().toISOString().slice(0, 10)}.csv`).catch((e) => toast((e as Error).message, 'error'))}>
            CSV
          </Button>
          <Button size="sm" icon={<Printer className="h-3.5 w-3.5" />} onClick={() => window.open(`/api/reports?kind=${active}&format=html`, '_blank', 'noopener')}>
            Printable / PDF
          </Button>
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-[260px_1fr]">
        <Card className="h-max overflow-hidden">
          <p className="border-b border-slate-100 px-4 py-2.5 text-[10px] font-bold uppercase tracking-wider text-slate-500">Report catalogue</p>
          <nav className="p-1.5">
            {kinds.length === 0 && <p className="px-2 py-2 text-xs text-slate-400">Loading…</p>}
            {kinds.map((k) => (
              <button
                key={k.id}
                onClick={() => setActive(k.id)}
                className={`mb-0.5 block w-full rounded-lg px-2.5 py-2 text-left transition ${
                  active === k.id ? 'bg-blue-50 ring-1 ring-inset ring-blue-200' : 'hover:bg-slate-50'}`}
              >
                <span className={`block text-[12.5px] font-semibold ${active === k.id ? 'text-blue-800' : 'text-slate-700'}`}>{k.label}</span>
                <span className="mt-0.5 block text-[10.5px] leading-snug text-slate-500">{k.description}</span>
              </button>
            ))}
          </nav>
        </Card>

        <Card className="min-w-0">
          {loading || !report ? <LoadingBlock label="Building report…" /> : (
            <>
              <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 px-4 py-3">
                <div>
                  <h2 className="text-sm font-semibold text-slate-900">{report.title}</h2>
                  <p className="text-[11px] text-slate-500">
                    Generated {new Date(report.generatedAt).toLocaleString('en-GB')} · {report.rows.length} row{report.rows.length === 1 ? '' : 's'}
                  </p>
                </div>
                <Badge tone="bg-sky-50 text-sky-700 ring-sky-600/20" dot>Live data</Badge>
              </div>

              {report.totals && (
                <div className="flex flex-wrap gap-2 border-b border-slate-100 bg-slate-50/60 px-4 py-2.5">
                  {Object.entries(report.totals).map(([k, v]) => (
                    <span key={k} className="rounded-md bg-white px-2 py-1 text-[11px] font-medium text-slate-600 ring-1 ring-inset ring-slate-200">
                      {k.replaceAll('_', ' ')}: <b className="text-slate-900">{/cost|amount|fuel|maintenance|other|total|payroll|distance/.test(k) ? money(v) : v.toLocaleString()}</b>
                    </span>
                  ))}
                </div>
              )}

              <div className="thin-scroll max-h-[65vh] overflow-auto">
                <table className="w-full min-w-[720px] border-collapse text-[12px]">
                  <thead className="sticky top-0 z-10">
                    <tr className="border-b border-slate-200 bg-slate-50">
                      {report.columns.map((c) => (
                        <th key={c.key} className={`whitespace-nowrap px-3 py-2 text-[10px] font-bold uppercase tracking-wider text-slate-500 ${c.align === 'right' ? 'text-right' : 'text-left'}`}>
                          {c.label}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {report.rows.length === 0 && (
                      <tr><td colSpan={report.columns.length} className="px-3 py-10 text-center text-slate-400">No records for this report yet.</td></tr>
                    )}
                    {report.rows.map((row, i) => (
                      <tr key={i} className="border-b border-slate-100 last:border-0 hover:bg-slate-50/60">
                        {report.columns.map((c) => {
                          const value = row[c.key];
                          const display = typeof value === 'number' && c.align === 'right'
                            ? (/cost|amount|price|₦/.test(c.label) ? money(value) : value.toLocaleString())
                            : String(value ?? '');
                          return (
                            <td key={c.key} className={`whitespace-nowrap px-3 py-1.5 text-slate-700 ${c.align === 'right' ? 'text-right tabular-nums' : ''}`}>
                              {display || '—'}
                            </td>
                          );
                        })}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
          {!loading && report && (
            <div className="flex items-center gap-2 border-t border-slate-100 px-4 py-2.5 text-[11px] text-slate-400">
              <BarChart3 className="h-3 w-3" />
              Reports use the same aggregation rules as the desktop edition (completed trips, non-cancelled maintenance, posted expenses).
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
