import 'server-only';
import { NextResponse } from 'next/server';
import { getSession, type SessionUser } from './session';
import { errorResponse } from './errors';
import { requirePermission } from './auth';
import type { Permission } from './domain';

/** Wraps an authenticated route handler: resolves the session and maps errors. */
export function handler<T>(fn: (session: SessionUser, request: Request, context: T) => Promise<unknown>) {
  return async (request: Request, context: T) => {
    try {
      const session = await getSession();
      const data = await fn(session as SessionUser, request, context);
      if (data instanceof NextResponse) return data;
      return NextResponse.json(data ?? { ok: true });
    } catch (error) {
      const { message, code, details } = errorResponse(error);
      const status = (error as { status?: number }).status ?? 500;
      return NextResponse.json({ error: message, code, details }, { status });
    }
  };
}

/** Wraps a handler that must be signed in and hold a specific permission. */
export function guarded<T>(permission: Permission, fn: (session: SessionUser, request: Request, context: T) => Promise<unknown>) {
  return handler<T>(async (session, request, context) => {
    requirePermission(session, permission);
    return fn(session, request, context);
  });
}

export async function readJson<T = Record<string, unknown>>(request: Request): Promise<T> {
  try {
    return (await request.json()) as T;
  } catch {
    return {} as T;
  }
}

export function query(request: Request): URLSearchParams {
  return new URL(request.url).searchParams;
}
