import { createClient, type SupabaseClient } from '@supabase/supabase-js';

/**
 * Server-only Supabase client using the service-role key.
 *
 * The browser never receives this client or any database credentials: every
 * read and write goes through Next.js server code (route handlers / server
 * actions), exactly like the desktop application's service layer. RLS is
 * enabled on all tables with no policies, so the publishable/anon keys can do
 * nothing even if one leaks.
 */

let cached: SupabaseClient | null = null;

export function supabaseUrl(): string {
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  if (!url) throw new Error('NEXT_PUBLIC_SUPABASE_URL is not configured.');
  return url;
}

export function serviceKey(): string {
  const key = process.env.SUPABASE_SERVICE_ROLE_KEY;
  if (!key) throw new Error('SUPABASE_SERVICE_ROLE_KEY is not configured.');
  return key;
}

export function db(): SupabaseClient {
  if (cached) return cached;
  cached = createClient(supabaseUrl(), serviceKey(), {
    auth: { persistSession: false, autoRefreshToken: false },
    db: { schema: 'public' },
    global: { headers: { 'x-application-name': 'femzyk-fleet-web' } },
  });
  return cached;
}

export function isConfigured(): boolean {
  return Boolean(process.env.NEXT_PUBLIC_SUPABASE_URL && process.env.SUPABASE_SERVICE_ROLE_KEY);
}

/** Unwraps a Supabase response, throwing a readable error on failure. */
export function unwrap<T>(res: { data: T | null; error: { message: string; code?: string; details?: string } | null }, context: string): T {
  if (res.error) {
    const err = new Error(`${context}: ${res.error.message}`) as Error & { code?: string; details?: string };
    err.code = res.error.code;
    err.details = res.error.details;
    throw err;
  }
  if (res.data === null) throw new Error(`${context}: no data returned`);
  return res.data;
}
