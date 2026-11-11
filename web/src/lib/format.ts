/** Display helpers shared by server and client components. */

const naira = new Intl.NumberFormat('en-NG', { style: 'currency', currency: 'NGN', maximumFractionDigits: 0 });
const naira2 = new Intl.NumberFormat('en-NG', { style: 'currency', currency: 'NGN', minimumFractionDigits: 2, maximumFractionDigits: 2 });
const number = new Intl.NumberFormat('en-NG');

export function money(value: number | string | null | undefined, decimals = false): string {
  const n = typeof value === 'string' ? Number(value) : value ?? 0;
  if (!Number.isFinite(n)) return decimals ? naira2.format(0) : naira.format(0);
  return decimals ? naira2.format(n) : naira.format(n);
}

export function compactMoney(value: number | null | undefined): string {
  const n = value ?? 0;
  if (Math.abs(n) >= 1_000_000_000) return `₦${(n / 1_000_000_000).toFixed(1)}b`;
  if (Math.abs(n) >= 1_000_000) return `₦${(n / 1_000_000).toFixed(1)}m`;
  if (Math.abs(n) >= 1_000) return `₦${(n / 1_000).toFixed(0)}k`;
  return `₦${n.toFixed(0)}`;
}

export function num(value: number | string | null | undefined, digits = 0): string {
  const n = typeof value === 'string' ? Number(value) : value ?? 0;
  if (!Number.isFinite(n)) return '0';
  return new Intl.NumberFormat('en-NG', { minimumFractionDigits: digits, maximumFractionDigits: digits }).format(n);
}

export function km(value: number | null | undefined): string {
  return `${number.format(value ?? 0)} km`;
}

export function date(value: string | null | undefined): string {
  if (!value) return '—';
  const d = new Date(value.length === 10 ? `${value}T00:00:00` : value);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
}

export function dateTime(value: string | null | undefined): string {
  if (!value) return '—';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleString('en-GB', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

export function shortDate(value: string | null | undefined): string {
  if (!value) return '—';
  const d = new Date(value.length === 10 ? `${value}T00:00:00` : value);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short' });
}

export function monthLabel(yyyymm: string): string {
  const [y, m] = yyyymm.split('-');
  const d = new Date(Number(y), Number(m) - 1, 1);
  return d.toLocaleDateString('en-GB', { month: 'short', year: '2-digit' });
}

export function relativeDays(value: string | null | undefined): string {
  if (!value) return '—';
  const d = new Date(value.length === 10 ? `${value}T00:00:00` : value);
  const days = Math.round((d.getTime() - Date.now()) / 86_400_000);
  if (Number.isNaN(days)) return '—';
  if (days === 0) return 'today';
  if (days > 0) return `in ${days} day${days === 1 ? '' : 's'}`;
  return `${Math.abs(days)} day${days === -1 ? '' : 's'} ago`;
}

export function initials(name: string): string {
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map((p) => p[0]?.toUpperCase() ?? '').join('');
}

export function toCsv(rows: Record<string, unknown>[], columns?: { key: string; label: string }[]): string {
  const cols = columns ?? Object.keys(rows[0] ?? {}).map((k) => ({ key: k, label: k }));
  const escape = (v: unknown) => {
    const s = v === null || v === undefined ? '' : String(v);
    return /[",\n]/.test(s) ? `"${s.replaceAll('"', '""')}"` : s;
  };
  return [cols.map((c) => escape(c.label)).join(','), ...rows.map((r) => cols.map((c) => escape(r[c.key])).join(','))].join('\r\n');
}
