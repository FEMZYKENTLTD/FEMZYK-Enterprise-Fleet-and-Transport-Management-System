import { NextResponse } from 'next/server';
import { isConfigured } from '@/lib/db';
import { errorResponse } from '@/lib/errors';
import { submitRegistrationRequest, REQUESTABLE_ROLES } from '@/lib/services/registration';

/**
 * Public: request an account. Creates a PENDING request only — never a user,
 * never a session. An administrator must approve it under Users & roles before
 * the account exists.
 *
 * No-store and no secret required (people cannot sign in yet), but every call
 * is audit-logged and the acknowledgement is deliberately identical whether or
 * not a matching pending request already existed.
 */
export const dynamic = 'force-dynamic';

export async function GET() {
  // Lets the form render the allowed role list without duplicating it.
  return NextResponse.json({ roles: REQUESTABLE_ROLES }, { headers: { 'Cache-Control': 'no-store' } });
}

export async function POST(request: Request) {
  if (!isConfigured()) {
    return NextResponse.json({ error: 'This deployment is not configured yet. Try later.' }, { status: 503 });
  }
  try {
    const body = (await request.json()) as Record<string, unknown>;
    const { id } = await submitRegistrationRequest({
      username: String(body.username ?? ''),
      email: String(body.email ?? ''),
      fullName: String(body.fullName ?? ''),
      requestedRole: body.requestedRole ? String(body.requestedRole) : undefined,
      department: body.department ? String(body.department) : undefined,
      reason: body.reason ? String(body.reason) : undefined,
    });
    return NextResponse.json(
      {
        ok: true,
        id,
        message: 'Request received. An administrator will review it and create your account.',
      },
      { status: 201, headers: { 'Cache-Control': 'no-store' } },
    );
  } catch (error) {
    const { message, code } = errorResponse(error);
    const status = (error as { status?: number }).status ?? 400;
    return NextResponse.json({ error: message, code }, { status, headers: { 'Cache-Control': 'no-store' } });
  }
}
