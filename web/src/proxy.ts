import { NextResponse, type NextRequest } from 'next/server';
import { SESSION_COOKIE } from '@/lib/session-cookie';

/**
 * Cheap edge guard: unauthenticated visitors are redirected to /login before a
 * server component runs. The cookie is fully verified (HMAC + expiry) in the
 * server layer for every page and API route, so this is only UX.
 */
// '/' is an exact-match public landing page; everything else under /(app) needs a session.
const PUBLIC_PATHS = ['/', '/login', '/api/auth/login', '/api/auth/reset', '/api/bootstrap', '/api/demo'];

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  if (PUBLIC_PATHS.some((p) => pathname === p || pathname.startsWith(`${p}/`))) return NextResponse.next();
  if (pathname.startsWith('/_next') || pathname === '/favicon.ico' || pathname === '/robots.txt') return NextResponse.next();

  const hasSession = Boolean(request.cookies.get(SESSION_COOKIE)?.value);
  if (!hasSession && !pathname.startsWith('/api/')) {
    const url = request.nextUrl.clone();
    url.pathname = '/login';
    url.search = pathname === '/' ? '' : `?next=${encodeURIComponent(pathname)}`;
    return NextResponse.redirect(url);
  }
  if (hasSession && pathname === '/login') {
    const url = request.nextUrl.clone();
    url.pathname = '/dashboard';
    url.search = '';
    return NextResponse.redirect(url);
  }
  return NextResponse.next();
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
};
