import { createHmac, timingSafeEqual } from 'node:crypto';
import { cookies } from 'next/headers';
import type { Role } from './domain';

/**
 * Stateless, signed session cookie (HMAC-SHA256 over a JSON payload).
 * No third-party dependency, no database round-trip for session lookup, and
 * the signature covers the expiry so cookies cannot be extended client-side.
 */
import { SESSION_COOKIE } from './session-cookie';
export { SESSION_COOKIE } from './session-cookie';
const MAX_AGE_SECONDS = 60 * 60 * 8; // 8-hour shift, like an office day

export type SessionUser = {
  id: number;
  username: string;
  fullName: string;
  email: string;
  role: Role;
  mustChangePassword: boolean;
};

type Payload = SessionUser & { exp: number };

function secret(): string {
  const value = process.env.SESSION_SECRET;
  if (!value || value.length < 16) {
    throw new Error('SESSION_SECRET is missing or too short (use at least 16 characters).');
  }
  return value;
}

function sign(data: string): string {
  return createHmac('sha256', secret()).update(data).digest('base64url');
}

export function createSessionToken(user: SessionUser): string {
  const payload: Payload = { ...user, exp: Math.floor(Date.now() / 1000) + MAX_AGE_SECONDS };
  const body = Buffer.from(JSON.stringify(payload), 'utf8').toString('base64url');
  return `${body}.${sign(body)}`;
}

export function readSessionToken(token: string | undefined | null): SessionUser | null {
  if (!token) return null;
  const [body, signature] = token.split('.');
  if (!body || !signature) return null;
  const expected = sign(body);
  const a = Buffer.from(signature);
  const b = Buffer.from(expected);
  if (a.length !== b.length || !timingSafeEqual(a, b)) return null;
  try {
    const payload = JSON.parse(Buffer.from(body, 'base64url').toString('utf8')) as Payload;
    if (!payload.exp || payload.exp < Math.floor(Date.now() / 1000)) return null;
    const { exp: _exp, ...user } = payload;
    return user as SessionUser;
  } catch {
    return null;
  }
}

/** Reads the current session inside a server component / route handler. */
export async function getSession(): Promise<SessionUser | null> {
  const store = await cookies();
  return readSessionToken(store.get(SESSION_COOKIE)?.value);
}

export const sessionCookieOptions = {
  httpOnly: true,
  sameSite: 'lax' as const,
  secure: process.env.NODE_ENV === 'production',
  path: '/',
  maxAge: MAX_AGE_SECONDS,
};
