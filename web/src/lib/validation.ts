import { ValidationError } from './errors';

/** Small validation kit mirroring com.femzyk.fleetmanagement.validation.Validators. */

export class Errors {
  private readonly list: string[] = [];
  required(value: unknown, name: string): this {
    if (value === null || value === undefined || (typeof value === 'string' && value.trim() === '')) {
      this.list.push(`${name} is required.`);
    }
    return this;
  }
  check(condition: boolean, message: string): this {
    if (!condition) this.list.push(message);
    return this;
  }
  nonNegative(value: number | null | undefined, name: string): this {
    if (value !== null && value !== undefined && value < 0) this.list.push(`${name} cannot be negative.`);
    return this;
  }
  range(value: number | null | undefined, min: number, max: number, name: string): this {
    if (value === null || value === undefined || value < min || value > max) this.list.push(`${name} must be between ${min} and ${max}.`);
    return this;
  }
  maxLength(value: string | null | undefined, max: number, name: string): this {
    if (value && value.length > max) this.list.push(`${name} must be at most ${max} characters.`);
    return this;
  }
  email(value: string | null | undefined, name = 'E-mail'): this {
    if (value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) this.list.push(`${name} is not a valid e-mail address.`);
    return this;
  }
  notFuture(date: string | null | undefined, name: string): this {
    if (date && new Date(date) > new Date(Date.now() + 86_400_000)) this.list.push(`${name} cannot be in the future.`);
    return this;
  }
  throwIfAny(): void {
    if (this.list.length) throw new ValidationError(this.list[0], this.list);
  }
  get errors(): string[] {
    return [...this.list];
  }
}

export const errors = (): Errors => new Errors();

export function isBlank(value: unknown): boolean {
  return value === null || value === undefined || (typeof value === 'string' && value.trim() === '');
}

export function str(value: unknown): string | null {
  if (value === null || value === undefined) return null;
  const s = String(value).trim();
  return s === '' ? null : s;
}

export function num(value: unknown): number | null {
  if (value === null || value === undefined || value === '') return null;
  const n = Number(value);
  return Number.isFinite(n) ? n : null;
}

export function int(value: unknown): number | null {
  const n = num(value);
  return n === null ? null : Math.trunc(n);
}

/** ISO date (yyyy-mm-dd) or null. Accepts Date, ISO strings and dd/mm/yyyy. */
export function dateOnly(value: unknown): string | null {
  if (value === null || value === undefined || value === '') return null;
  if (value instanceof Date) return value.toISOString().slice(0, 10);
  const s = String(value).trim();
  const iso = /^(\d{4})-(\d{2})-(\d{2})/.exec(s);
  if (iso) return `${iso[1]}-${iso[2]}-${iso[3]}`;
  const dmy = /^(\d{1,2})[/-](\d{1,2})[/-](\d{4})$/.exec(s);
  if (dmy) return `${dmy[3]}-${dmy[2].padStart(2, '0')}-${dmy[1].padStart(2, '0')}`;
  const parsed = new Date(s);
  return Number.isNaN(parsed.getTime()) ? null : parsed.toISOString().slice(0, 10);
}

/** Timestamp (ISO 8601) or null. */
export function timestamp(value: unknown): string | null {
  if (value === null || value === undefined || value === '') return null;
  const d = value instanceof Date ? value : new Date(String(value));
  return Number.isNaN(d.getTime()) ? null : d.toISOString();
}

export function bool(value: unknown): boolean {
  return value === true || value === 'true' || value === 1 || value === '1';
}

/** Registration numbers: 3–15 chars, letters/digits/spaces/hyphens, upper-cased. */
export function normaliseRegistration(value: string): string {
  return value.trim().toUpperCase().replace(/\s+/g, ' ');
}

export function isValidRegistration(value: string | null | undefined): boolean {
  return !!value && /^[A-Z0-9][A-Z0-9 -]{2,14}$/i.test(value.trim());
}

/** Numbers like 12,500.50 → 12500.5 */
export function money(value: unknown): number {
  const n = num(typeof value === 'string' ? value.replace(/[₦,\s]/g, '') : value);
  return n ?? 0;
}
