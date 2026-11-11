'use client';

import { useRef, useState } from 'react';
import { Database, Download, Upload, ShieldCheck, Server, Sparkles, AlertTriangle, FileText } from 'lucide-react';
import { api, downloadCsv } from '@/lib/api';
import { Badge, Button, Card, LoadingBlock, toast } from '@/components/ui';

type Stats = { table: string; label: string; rows: number }[];

const DATASETS = [
  { id: 'vehicles', label: 'Vehicles', hint: 'Registration, type, status, mileage, acquisition cost' },
  { id: 'employees', label: 'Employees', hint: 'Code, name, department, contact, salary' },
  { id: 'drivers', label: 'Drivers', hint: 'Licence number, class, expiry, status' },
  { id: 'trips', label: 'Trips', hint: 'Route, departure, odometer, distance' },
  { id: 'fuel', label: 'Fuel', hint: 'Litres, unit price, total, station' },
  { id: 'maintenance', label: 'Maintenance', hint: 'Work done, cost, next service' },
  { id: 'expenses', label: 'Expenses', hint: 'Category, amount, vendor, reference' },
];

export function SystemClient({
  canExport, canImport, configured, stats, auditRows, demoLoadedAt, region, commit,
}: {
  canExport: boolean; canImport: boolean; configured: boolean; stats: Stats;
  auditRows: number; demoLoadedAt: string | null; region: string; commit: string;
}) {
  const [busy, setBusy] = useState<string | null>(null);
  const [importResult, setImportResult] = useState<{ imported: number; created: string[]; failed: { row: number; reference: string; error: string }[] } | null>(null);
  const [dataset, setDataset] = useState('vehicles');
  const fileRef = useRef<HTMLInputElement>(null);

  async function download(id: string) {
    setBusy(id);
    try {
      await downloadCsv(`/api/export?dataset=${id}`, `femzyk-${id}-${new Date().toISOString().slice(0, 10)}.csv`);
      toast(`${id} exported.`);
    } catch (e) {
      toast((e as Error).message, 'error');
    } finally {
      setBusy(null);
    }
  }

  async function importFile(file: File) {
    setBusy('import');
    try {
      const text = await file.text();
      const rows = parseCsv(text);
      if (!rows.length) { toast('The file has no data rows.', 'error'); return; }
      const result = await api.post<{ imported: number; created: string[]; failed: { row: number; reference: string; error: string }[] }>('/api/import', { dataset, rows });
      if (!result.ok) { toast(result.error, 'error'); return; }
      setImportResult(result.data);
      toast(`Imported ${result.data.imported} row${result.data.imported === 1 ? '' : 's'}.`, result.data.failed.length ? 'info' : 'success');
    } catch (e) {
      toast((e as Error).message, 'error');
    } finally {
      setBusy(null);
      if (fileRef.current) fileRef.current.value = '';
    }
  }

  const total = stats.reduce((s, r) => s + r.rows, 0);

  return (
    <div>
      <div className="mb-5">
        <p className="mb-1 text-[10px] font-bold uppercase tracking-[0.14em] text-blue-700">Insight</p>
        <h1 className="text-[22px] font-bold tracking-tight text-slate-900">Data &amp; system</h1>
        <p className="mt-1 max-w-2xl text-[13px] leading-relaxed text-slate-500">
          The web counterpart of the desktop edition&rsquo;s System module: dataset export, CSV import, database health and the
          runtime facts behind this deployment.
        </p>
      </div>

      <div className="mb-4 grid gap-2.5 sm:grid-cols-2 lg:grid-cols-4">
        <Card className="px-4 py-3">
          <p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Database</p>
          <p className="mt-0.5 flex items-center gap-1.5 text-sm font-bold text-slate-900">
            <span className={`h-1.5 w-1.5 rounded-full ${configured ? 'bg-emerald-500' : 'bg-rose-500'}`} />
            {configured ? 'Connected' : 'Not configured'}
          </p>
          <p className="text-[10.5px] text-slate-400">Supabase PostgreSQL · service role</p>
        </Card>
        <Card className="px-4 py-3">
          <p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Records</p>
          <p className="text-lg font-bold tabular-nums">{total.toLocaleString()}</p>
          <p className="text-[10.5px] text-slate-400">{stats.length} tables · {auditRows.toLocaleString()} audit events</p>
        </Card>
        <Card className="px-4 py-3">
          <p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Demo data</p>
          <p className="mt-0.5 text-sm font-bold text-slate-900">{demoLoadedAt ? 'Loaded' : 'Not loaded'}</p>
          <p className="text-[10.5px] text-slate-400">{demoLoadedAt ? new Date(demoLoadedAt).toLocaleString('en-GB') : 'Empty database'}</p>
        </Card>
        <Card className="px-4 py-3">
          <p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">Deployment</p>
          <p className="mt-0.5 text-sm font-bold text-slate-900">{region}</p>
          <p className="text-[10.5px] text-slate-400">commit {commit}</p>
        </Card>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card className="p-4">
          <div className="mb-3 flex items-center gap-2">
            <span className="grid h-8 w-8 place-items-center rounded-lg bg-blue-50 text-blue-700"><Download className="h-4 w-4" /></span>
            <div>
              <h2 className="text-sm font-semibold text-slate-900">Export datasets</h2>
              <p className="text-[11px] text-slate-500">CSV opens directly in Excel and Google Sheets.</p>
            </div>
          </div>
          <div className="divide-y divide-slate-100">
            {DATASETS.map((d) => (
              <div key={d.id} className="flex items-center justify-between gap-3 py-2">
                <div className="min-w-0">
                  <p className="text-[12.5px] font-medium text-slate-800">{d.label}</p>
                  <p className="truncate text-[10.5px] text-slate-500">{d.hint}</p>
                </div>
                <Button size="sm" variant="secondary" disabled={!canExport || busy === d.id} loading={busy === d.id} onClick={() => download(d.id)}>
                  CSV
                </Button>
              </div>
            ))}
          </div>
          {!canExport && <p className="mt-2 flex items-center gap-1 text-[11px] text-amber-700"><AlertTriangle className="h-3 w-3" /> Your role cannot export data.</p>}
        </Card>

        <div className="space-y-4">
          <Card className="p-4">
            <div className="mb-3 flex items-center gap-2">
              <span className="grid h-8 w-8 place-items-center rounded-lg bg-emerald-50 text-emerald-700"><Upload className="h-4 w-4" /></span>
              <div>
                <h2 className="text-sm font-semibold text-slate-900">Import CSV</h2>
                <p className="text-[11px] text-slate-500">Vehicles or employees — every row passes normal validation.</p>
              </div>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <select value={dataset === 'vehicles' || dataset === 'employees' ? dataset : 'vehicles'} onChange={(e) => setDataset(e.target.value)} className="h-9 rounded-lg border border-slate-300 bg-white px-2 text-xs">
                <option value="vehicles">Vehicles</option>
                <option value="employees">Employees</option>
              </select>
              <input ref={fileRef} type="file" accept=".csv,text/csv" disabled={!canImport || busy === 'import'} onChange={(e) => { const f = e.target.files?.[0]; if (f) void importFile(f); }}
                className="block w-full max-w-[240px] flex-1 text-[11px] file:mr-2 file:rounded-md file:border-0 file:bg-slate-100 file:px-2.5 file:py-1.5 file:text-[11px] file:font-medium file:text-slate-700 hover:file:bg-slate-200" />
            </div>
            <p className="mt-2 text-[10.5px] leading-snug text-slate-400">
              Expected headers match the export columns, e.g. <code className="rounded bg-slate-100 px-1">registration_number, vehicle_type, make, model, year</code>.
              Up to 500 rows per import.
            </p>
            {importResult && (
              <div className="mt-3 rounded-lg bg-slate-50 p-3 text-[11.5px]">
                <p className="font-semibold text-slate-700">{importResult.imported} imported · {importResult.failed.length} failed</p>
                {importResult.created.length > 0 && <p className="mt-1 text-slate-500">Created: {importResult.created.join(', ')}</p>}
                {importResult.failed.length > 0 && (
                  <ul className="mt-1 space-y-0.5 text-rose-700">
                    {importResult.failed.slice(0, 8).map((f) => <li key={f.row}>Row {f.row} ({f.reference || 'unnamed'}): {f.error}</li>)}
                  </ul>
                )}
              </div>
            )}
          </Card>

          <Card className="p-4">
            <div className="mb-2 flex items-center gap-2">
              <span className="grid h-8 w-8 place-items-center rounded-lg bg-slate-100 text-slate-700"><Database className="h-4 w-4" /></span>
              <div>
                <h2 className="text-sm font-semibold text-slate-900">Row counts</h2>
                <p className="text-[11px] text-slate-500">Live totals across the operational tables.</p>
              </div>
            </div>
            {!configured ? <p className="text-xs text-rose-600">Supabase environment variables are missing.</p>
              : stats.length === 0 ? <LoadingBlock label="Reading tables…" /> : (
                <div className="grid grid-cols-3 gap-1.5">
                  {stats.map((s) => (
                    <div key={s.table} className="rounded-md bg-slate-50 px-2 py-1.5">
                      <p className="text-[10px] text-slate-500">{s.label}</p>
                      <p className="text-[13px] font-bold tabular-nums text-slate-800">{s.rows.toLocaleString()}</p>
                    </div>
                  ))}
                </div>
              )}
          </Card>
        </div>
      </div>

      <Card className="mt-4 p-4">
        <div className="mb-2 flex items-center gap-2">
          <span className="grid h-8 w-8 place-items-center rounded-lg bg-indigo-50 text-indigo-700"><ShieldCheck className="h-4 w-4" /></span>
          <div>
            <h2 className="text-sm font-semibold text-slate-900">Security &amp; data model</h2>
            <p className="text-[11px] text-slate-500">How this deployment compares with the desktop edition.</p>
          </div>
        </div>
        <div className="grid gap-3 text-[12px] leading-relaxed text-slate-600 sm:grid-cols-2 lg:grid-cols-4">
          <div className="rounded-lg bg-slate-50 p-3">
            <p className="mb-1 flex items-center gap-1 text-[11px] font-semibold text-slate-800"><Server className="h-3 w-3" /> Server-only database</p>
            Row-level security is on for all tables with no policies: only the service-role key used inside Next.js route handlers can read or write.
          </div>
          <div className="rounded-lg bg-slate-50 p-3">
            <p className="mb-1 flex items-center gap-1 text-[11px] font-semibold text-slate-800"><ShieldCheck className="h-3 w-3" /> Sessions</p>
            HMAC-SHA256 signed, http-only cookies with an 8-hour expiry; passwords stay PBKDF2-HMAC-SHA256 (65,536 iterations).
          </div>
          <div className="rounded-lg bg-slate-50 p-3">
            <p className="mb-1 flex items-center gap-1 text-[11px] font-semibold text-slate-800"><FileText className="h-3 w-3" /> Audit trail</p>
            Every create, update, delete, status change, sign-in and export writes an <code className="rounded bg-white px-1">audit_logs</code> row.
          </div>
          <div className="rounded-lg bg-slate-50 p-3">
            <p className="mb-1 flex items-center gap-1 text-[11px] font-semibold text-slate-800"><Sparkles className="h-3 w-3" /> Same rules</p>
            Assignment, trip, fuel, maintenance and vehicle attribute rules are ported 1:1 from the Java services, including partial unique indexes.
          </div>
        </div>
        <div className="mt-3 flex flex-wrap gap-1.5">
          {['PBKDF2 · 65,536', 'RLS enabled', 'Partial unique indexes', 'CHECK constraints', 'Audit triggers', '5 reporting views'].map((t) => (
            <Badge key={t} tone="bg-white text-slate-600 ring-slate-200">{t}</Badge>
          ))}
        </div>
      </Card>
    </div>
  );
}

/** Minimal RFC-4180-ish CSV parser (quotes, embedded commas and newlines). */
function parseCsv(text: string): Record<string, string>[] {
  const clean = text.replace(/^\uFEFF/, '').replace(/\r\n?/g, '\n');
  const rows: string[][] = [];
  let row: string[] = [];
  let cell = '';
  let quoted = false;

  for (let i = 0; i < clean.length; i++) {
    const ch = clean[i];
    if (quoted) {
      if (ch === '"') {
        if (clean[i + 1] === '"') { cell += '"'; i++; } else quoted = false;
      } else cell += ch;
      continue;
    }
    if (ch === '"') { quoted = true; continue; }
    if (ch === ',') { row.push(cell); cell = ''; continue; }
    if (ch === '\n') { row.push(cell); rows.push(row); row = []; cell = ''; continue; }
    cell += ch;
  }
  if (cell !== '' || row.length) { row.push(cell); rows.push(row); }
  if (rows.length < 2) return [];

  const header = rows[0].map((h) => h.trim());
  return rows.slice(1)
    .filter((r) => r.some((v) => v.trim() !== ''))
    .map((r) => Object.fromEntries(header.map((h, i) => [h, (r[i] ?? '').trim()])));
}
