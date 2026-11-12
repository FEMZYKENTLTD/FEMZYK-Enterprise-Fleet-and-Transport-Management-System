import { NextResponse } from 'next/server';
import { guarded, readJson } from '@/lib/route';
import { approveRegistrationRequest, rejectRegistrationRequest } from '@/lib/services/registration';
import type { Role } from '@/lib/domain';

type Ctx = { params: Promise<{ id: string }> };

/**
 * Administrator decision on a pending request.
 *
 *   PUT { action: 'approve', role, temporaryPassword, note? }
 *   PUT { action: 'reject', note? }
 *
 * Approving is the only way a request turns into an account, and the role is
 * taken from this request body (an administrator's choice), never from the
 * requester's stated preference.
 */
export const PUT = guarded<Ctx>('MANAGE_USERS', async (session, request, context) => {
  const { id } = await context.params;
  const body = await readJson<Record<string, unknown>>(request);

  if (body.action === 'approve') {
    await approveRegistrationRequest(session, Number(id), {
      role: String(body.role ?? 'VIEWER') as Role,
      temporaryPassword: String(body.temporaryPassword ?? ''),
      note: body.note ? String(body.note) : undefined,
    });
  } else if (body.action === 'reject') {
    await rejectRegistrationRequest(session, Number(id), body.note ? String(body.note) : undefined);
  } else {
    return NextResponse.json({ error: "action must be 'approve' or 'reject'." }, { status: 400 });
  }

  return NextResponse.json({ ok: true }, { headers: { 'Cache-Control': 'no-store' } });
});
