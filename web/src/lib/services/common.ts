import 'server-only';
import type { SessionUser } from '../session';

/** A date or timestamp expressed as an ISO string. */
export function nowIso(): string {
  return new Date().toISOString();
}

export function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

/** Adds n days to a yyyy-mm-dd date string. */
export function addDays(iso: string, days: number): string {
  const d = new Date(`${iso}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

export function monthStart(): string {
  const d = new Date();
  return new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), 1)).toISOString().slice(0, 10);
}

export function monthEnd(): string {
  const d = new Date();
  return new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth() + 1, 0)).toISOString().slice(0, 10);
}

export function yearStart(): string {
  return `${new Date().getUTCFullYear()}-01-01`;
}

export function monthsAgo(count: number): string {
  const d = new Date();
  return new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth() - count, 1)).toISOString().slice(0, 10);
}

/** Simple case-insensitive LIKE search across several columns. */
export function likeFilter(term: string, columns: string[]): string {
  const escaped = term.replace(/[%,]/g, ' ');
  return columns.map((c) => `${c}.ilike.%${escaped}%`).join(',');
}

export function actorName(session: SessionUser | null): string {
  return session?.username ?? 'system';
}

export function fullNameOf(row: { first_name: string; last_name: string }): string {
  return `${row.first_name} ${row.last_name}`.trim();
}
