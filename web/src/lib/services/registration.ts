import 'server-only';
import { db, unwrap } from '../db';
import { audit, createUser, findByUsername, requirePermission } from '../auth';
import { BusinessRuleError, NotFoundError, ValidationError } from '../errors';
import type { Role } from '../domain';
import type { SessionUser } from '../session';

/**
 * Account *requests* — the registration path for a system that must not have
 * open sign-up.
 *
 * Anyone may submit a request from /register. Nothing is created by that
 * submission: no account, no session, no role. An administrator reviews it
 * under Users & roles and either approves it (choosing the role, which creates
 * the account with a temporary password) or rejects it. This keeps the
 * "accounts are issued, not self-served" rule intact while giving people a
 * legitimate way to ask.
 */

export const REQUESTABLE_ROLES: Role[] = ['ADMINISTRATOR', 'FLEET_MANAGER', 'OPERATIONS_OFFICER', 'VIEWER'];

export type RegistrationRequest = {
  id: number;
  username: string;
  email: string;
  full_name: string;
  requested_role: Role;
  department: string | null;
  reason: string | null;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  decided_by: string | null;
  decided_at: string | null;
  decision_note: string | null;
  created_user_id: number | null;
  created_at: string;
  updated_at: string;
};

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const USERNAME_RE = /^[A-Za-z0-9._-]{3,32}$/;

export type RegistrationInput = {
  username: string;
  email: string;
  fullName: string;
  requestedRole?: string;
  department?: string;
  reason?: string;
};

/**
 * Public: submit (or re-submit) an account request. Never creates an account.
 * Returns the request id so the UI can show a reference.
 */
export async function submitRegistrationRequest(input: RegistrationInput): Promise<{ id: number }> {
  const username = (input.username ?? '').trim();
  const email = (input.email ?? '').trim().toLowerCase();
  const fullName = (input.fullName ?? '').trim();
  const department = (input.department ?? '').trim() || null;
  const reason = (input.reason ?? '').trim() || null;
  const requestedRole = (input.requestedRole ?? 'VIEWER') as Role;

  if (!USERNAME_RE.test(username)) {
    throw new ValidationError('Username must be 3-32 characters: letters, digits, dot, dash or underscore.');
  }
  if (!EMAIL_RE.test(email)) throw new ValidationError('Enter a valid e-mail address.');
  if (fullName.length < 3) throw new ValidationError('Enter your full name.');
  if (!REQUESTABLE_ROLES.includes(requestedRole)) throw new ValidationError('Unknown role requested.');
  if (reason && reason.length > 500) throw new ValidationError('Please keep the reason under 500 characters.');

  // An existing live account must not be requestable — that would be an
  // account-takeover vector, and the administrator could never approve it.
  const existingUser = await findByUsername(username);
  if (existingUser) throw new ValidationError('That username is already in use. Try signing in instead.');

  // One open request per username/e-mail: refresh the pending row instead of
  // stacking duplicates (the partial unique indexes enforce this too).
  const { data: pending } = await db()
    .from('registration_requests')
    .select('id, username, email')
    .eq('status', 'PENDING')
    .or(`username.ilike.${username},email.ilike.${email}`)
    .maybeSingle();

  if (pending) {
    const { error } = await db()
      .from('registration_requests')
      .update({ full_name: fullName, department, reason, requested_role: requestedRole, email })
      .eq('id', pending.id);
    if (error) throw new Error(error.message);
    await audit(null, 'REGISTER_UPDATE', 'AUTH', username, 'Pending account request updated');
    return { id: Number(pending.id) };
  }

  const { data, error } = await db()
    .from('registration_requests')
    .insert({
      username,
      email,
      full_name: fullName,
      requested_role: requestedRole,
      department,
      reason,
      status: 'PENDING',
    })
    .select('id')
    .single();
  if (error) throw new Error(error.message);
  await audit(null, 'REGISTER_REQUEST', 'AUTH', username, 'Account request submitted, awaiting approval');
  return { id: Number(data.id) };
}

/** Administrator: all requests, pending first. */
export async function listRegistrationRequests(session: SessionUser | null): Promise<RegistrationRequest[]> {
  requirePermission(session, 'MANAGE_USERS');
  return unwrap<RegistrationRequest[]>(
    await db().from('registration_requests').select('*').order('created_at', { ascending: false }),
    'list registration requests',
  );
}

/**
 * Administrator: approve a request. The role comes from the administrator, not
 * from the requester, and the account starts with `must_change_password` so the
 * temporary password cannot survive first use.
 */
export async function approveRegistrationRequest(
  session: SessionUser | null,
  requestId: number,
  options: { role: Role; temporaryPassword: string; note?: string },
): Promise<void> {
  const actor = requirePermission(session, 'MANAGE_USERS');
  const request = await loadRequest(requestId);
  if (request.status !== 'PENDING') throw new BusinessRuleError(`This request was already ${request.status.toLowerCase()}.`);
  if (!REQUESTABLE_ROLES.includes(options.role)) throw new ValidationError('Unknown role.');

  await createUser(actor, {
    username: request.username,
    email: request.email,
    fullName: request.full_name,
    role: options.role,
    password: options.temporaryPassword,
  });

  const created = await findByUsername(request.username);
  const { error } = await db()
    .from('registration_requests')
    .update({
      status: 'APPROVED',
      decided_by: actor.username,
      decided_at: new Date().toISOString(),
      decision_note: options.note?.trim() || null,
      created_user_id: created?.id ?? null,
    })
    .eq('id', requestId);
  if (error) throw new Error(error.message);
  await audit(actor.username, 'REGISTER_APPROVE', 'USER', request.username, `Account request approved as ${options.role}`);
}

/** Administrator: reject a request. No account is created. */
export async function rejectRegistrationRequest(
  session: SessionUser | null,
  requestId: number,
  note?: string,
): Promise<void> {
  const actor = requirePermission(session, 'MANAGE_USERS');
  const request = await loadRequest(requestId);
  if (request.status !== 'PENDING') throw new BusinessRuleError(`This request was already ${request.status.toLowerCase()}.`);

  const { error } = await db()
    .from('registration_requests')
    .update({
      status: 'REJECTED',
      decided_by: actor.username,
      decided_at: new Date().toISOString(),
      decision_note: note?.trim() || null,
    })
    .eq('id', requestId);
  if (error) throw new Error(error.message);
  await audit(actor.username, 'REGISTER_REJECT', 'USER', request.username, 'Account request rejected');
}

async function loadRequest(requestId: number): Promise<RegistrationRequest> {
  const { data } = await db().from('registration_requests').select('*').eq('id', requestId).maybeSingle();
  if (!data) throw new NotFoundError('Registration request', requestId);
  return data as RegistrationRequest;
}
