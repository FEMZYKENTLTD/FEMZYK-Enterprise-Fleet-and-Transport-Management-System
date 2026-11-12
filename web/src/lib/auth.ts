import 'server-only';
import { db, unwrap } from './db';
import { hashPassword, verifyPassword, generateSalt, checkPolicy } from './password';
import { AuthError, BusinessRuleError, DuplicateRecordError, ForbiddenError, NotFoundError, ValidationError } from './errors';
import type { Permission, Role } from './domain';
import { roleHas } from './domain';
import type { SessionUser } from './session';

/** Mirrors AppConfig.MAX_FAILED_LOGINS / LOCKOUT_MINUTES. */
const MAX_FAILED_LOGINS = 5;
const LOCKOUT_MINUTES = 15;

export const DEFAULT_ADMIN = { username: 'admin', password: 'Admin@2024', email: 'admin@femzyk.local', fullName: 'System Administrator' };

export type UserRow = {
  id: number;
  username: string;
  email: string;
  full_name: string;
  password_salt: string;
  password_hash: string;
  role: Role;
  active: boolean;
  failed_login_attempts: number;
  locked_until: string | null;
  last_login_at: string | null;
  last_password_reset_at: string | null;
  must_change_password: boolean;
  created_at: string;
  updated_at: string;
  deleted_at: string | null;
};

export async function audit(username: string | null | undefined, action: string, module: string, reference: string | null, description: string): Promise<void> {
  await db().from('audit_logs').insert({
    username: username || 'system',
    action,
    module,
    reference,
    description,
  });
}

/** Service-layer guard — the web equivalent of SessionContext.require(). */
export function requirePermission(session: SessionUser | null, permission: Permission): SessionUser {
  if (!session) throw new AuthError('You must be signed in to perform this action.');
  if (!roleHas(session.role, permission)) {
    throw new ForbiddenError(`Your role (${session.role.replaceAll('_', ' ').toLowerCase()}) does not permit this action.`);
  }
  return session;
}

export async function findByUsername(username: string): Promise<UserRow | null> {
  const { data, error } = await db().from('users').select('*').ilike('username', username.trim()).is('deleted_at', null).maybeSingle();
  if (error) throw new Error(error.message);
  return (data as UserRow | null) ?? null;
}

export async function findById(id: number): Promise<UserRow | null> {
  const { data, error } = await db().from('users').select('*').eq('id', id).is('deleted_at', null).maybeSingle();
  if (error) throw new Error(error.message);
  return (data as UserRow | null) ?? null;
}

export function toSessionUser(row: UserRow): SessionUser {
  return {
    id: row.id,
    username: row.username,
    fullName: row.full_name,
    email: row.email,
    role: row.role,
    mustChangePassword: row.must_change_password,
  };
}

/**
 * Login with the same semantics as AuthService.login(): unknown user, disabled
 * account, lockout and wrong password each record an audit entry; five failures
 * lock the account for fifteen minutes.
 */
export async function login(username: string, password: string): Promise<{ user: SessionUser; mustChangePassword: boolean }> {
  if (!username?.trim() || !password) throw new AuthError('Username and password are required.');
  const user = await findByUsername(username);
  if (!user) {
    await audit(username, 'LOGIN_FAILED', 'AUTH', username, 'Unknown username');
    throw new AuthError('Invalid username or password.');
  }
  if (!user.active) {
    await audit(user.username, 'LOGIN_FAILED', 'AUTH', user.username, 'Account disabled');
    throw new AuthError('This account has been disabled. Contact an administrator.');
  }
  if (user.locked_until && new Date(user.locked_until) > new Date()) {
    await audit(user.username, 'LOGIN_FAILED', 'AUTH', user.username, 'Account locked');
    throw new AuthError('Account is temporarily locked after too many failed attempts. Try again later.');
  }
  const valid = verifyPassword(password, user.password_salt, user.password_hash);
  if (!valid) {
    const attempts = user.failed_login_attempts + 1;
    const locked = attempts >= MAX_FAILED_LOGINS;
    await db()
      .from('users')
      .update({
        failed_login_attempts: locked ? 0 : attempts,
        locked_until: locked ? new Date(Date.now() + LOCKOUT_MINUTES * 60_000).toISOString() : null,
      })
      .eq('id', user.id);
    await audit(user.username, 'LOGIN_FAILED', 'AUTH', user.username, `Incorrect password (attempt ${attempts})`);
    throw new AuthError('Invalid username or password.');
  }
  await db().from('users').update({ failed_login_attempts: 0, locked_until: null, last_login_at: new Date().toISOString() }).eq('id', user.id);
  await audit(user.username, 'LOGIN', 'AUTH', user.username, `Successful login as ${user.role}`);
  return { user: toSessionUser(user), mustChangePassword: user.must_change_password };
}

/**
 * Verifies a password WITHOUT modifying the account or the audit log.
 *
 * `login()` deliberately records a successful sign-in and increments/resets
 * the failed-attempt counters, which is correct for a real user but wrong for
 * a health probe: checking the deployment every few minutes would bury genuine
 * audit entries under thousands of synthetic logins. This function runs the
 * same PBKDF2 comparison and the same active/locked checks, and writes nothing.
 */
export async function verifyCredentials(username: string, password: string): Promise<SessionUser> {
  const user = await findByUsername(username);
  if (!user || !user.active) throw new AuthError('Invalid username or password.');
  if (user.locked_until && new Date(user.locked_until) > new Date()) {
    throw new AuthError('Account is temporarily locked.');
  }
  if (!verifyPassword(password, user.password_salt, user.password_hash)) {
    throw new AuthError('Invalid username or password.');
  }
  return toSessionUser(user);
}

/** Creates the default administrator on first run (idempotent). */
export async function ensureDefaultAdministrator(): Promise<{ created: boolean }> {
  const { count, error } = await db().from('users').select('id', { count: 'exact', head: true });
  if (error) throw new Error(error.message);
  if ((count ?? 0) > 0) return { created: false };
  const salt = generateSalt();
  const { error: insertError } = await db().from('users').insert({
    username: DEFAULT_ADMIN.username,
    email: DEFAULT_ADMIN.email,
    full_name: DEFAULT_ADMIN.fullName,
    password_salt: salt,
    password_hash: hashPassword(DEFAULT_ADMIN.password, salt),
    role: 'ADMINISTRATOR',
    active: true,
    must_change_password: true,
  });
  if (insertError) throw new Error(insertError.message);
  await audit('system', 'BOOTSTRAP', 'USER', DEFAULT_ADMIN.username, 'Default administrator account created');
  return { created: true };
}

export async function changeOwnPassword(userId: number, currentPassword: string, newPassword: string): Promise<void> {
  const user = await findById(userId);
  if (!user) throw new NotFoundError('User', userId);
  if (!verifyPassword(currentPassword, user.password_salt, user.password_hash)) {
    throw new AuthError('Current password is incorrect.');
  }
  const policy = checkPolicy(newPassword);
  if (policy) throw new ValidationError(policy);
  const salt = generateSalt();
  await db()
    .from('users')
    .update({
      password_salt: salt,
      password_hash: hashPassword(newPassword, salt),
      must_change_password: false,
      last_password_reset_at: new Date().toISOString(),
    })
    .eq('id', userId);
  await audit(user.username, 'PASSWORD_CHANGE', 'USER', user.username, 'Password changed by user');
}

export async function adminResetPassword(actor: SessionUser, userId: number, newPassword: string): Promise<void> {
  requirePermission(actor, 'MANAGE_USERS');
  const user = await findById(userId);
  if (!user) throw new NotFoundError('User', userId);
  const policy = checkPolicy(newPassword);
  if (policy) throw new ValidationError(policy);
  const salt = generateSalt();
  await db()
    .from('users')
    .update({
      password_salt: salt,
      password_hash: hashPassword(newPassword, salt),
      must_change_password: true,
      locked_until: null,
      failed_login_attempts: 0,
      last_password_reset_at: new Date().toISOString(),
    })
    .eq('id', userId);
  await audit(actor.username, 'PASSWORD_RESET', 'USER', user.username, 'Password reset by administrator');
}

export async function listUsers(actor: SessionUser): Promise<Omit<UserRow, 'password_hash' | 'password_salt'>[]> {
  requirePermission(actor, 'MANAGE_USERS');
  const rows = unwrap<UserRow[]>(
    await db().from('users').select('*').is('deleted_at', null).order('role').order('username'),
    'list users',
  );
  return rows.map(({ password_hash: _h, password_salt: _s, ...rest }) => rest);
}

export async function createUser(
  actor: SessionUser,
  input: { username: string; email: string; fullName: string; role: Role; password: string },
): Promise<void> {
  requirePermission(actor, 'MANAGE_USERS');
  if (!input.username?.trim()) throw new ValidationError('Username is required.');
  if (!input.email?.trim()) throw new ValidationError('E-mail is required.');
  if (!input.fullName?.trim()) throw new ValidationError('Full name is required.');
  const policy = checkPolicy(input.password);
  if (policy) throw new ValidationError(policy);
  const existing = await db().from('users').select('id').or(`username.ilike.${input.username.trim()},email.ilike.${input.email.trim()}`).maybeSingle();
  if (existing.data) throw new DuplicateRecordError('A user with that username or e-mail already exists.');
  const salt = generateSalt();
  const { error } = await db().from('users').insert({
    username: input.username.trim(),
    email: input.email.trim().toLowerCase(),
    full_name: input.fullName.trim(),
    password_salt: salt,
    password_hash: hashPassword(input.password, salt),
    role: input.role,
    active: true,
    must_change_password: true,
  });
  if (error) throw new Error(error.message);
  await audit(actor.username, 'CREATE', 'USER', input.username, `User created with role ${input.role}`);
}

export async function updateUser(
  actor: SessionUser,
  userId: number,
  input: { email: string; fullName: string; role: Role; active: boolean },
): Promise<void> {
  requirePermission(actor, 'MANAGE_USERS');
  const user = await findById(userId);
  if (!user) throw new NotFoundError('User', userId);
  if (user.role === 'ADMINISTRATOR' && (input.role !== 'ADMINISTRATOR' || !input.active)) {
    const { count } = await db().from('users').select('id', { count: 'exact', head: true }).eq('role', 'ADMINISTRATOR').eq('active', true).is('deleted_at', null);
    if ((count ?? 0) <= 1) throw new BusinessRuleError('At least one active administrator must remain.');
  }
  if (!input.email?.trim()) throw new ValidationError('E-mail is required.');
  if (!input.fullName?.trim()) throw new ValidationError('Full name is required.');
  await db()
    .from('users')
    .update({
      email: input.email.trim().toLowerCase(),
      full_name: input.fullName.trim(),
      role: input.role,
      active: input.active,
      ...(input.active ? { locked_until: null, failed_login_attempts: 0 } : {}),
    })
    .eq('id', userId);
  await audit(actor.username, 'UPDATE', 'USER', user.username, `User updated: role=${input.role}, active=${input.active}`);
}

export async function deleteUser(actor: SessionUser, userId: number): Promise<void> {
  requirePermission(actor, 'MANAGE_USERS');
  if (actor.id === userId) throw new BusinessRuleError('You cannot delete the account you are signed in with.');
  const user = await findById(userId);
  if (!user) throw new NotFoundError('User', userId);
  if (user.role === 'ADMINISTRATOR') {
    const { count } = await db().from('users').select('id', { count: 'exact', head: true }).eq('role', 'ADMINISTRATOR').is('deleted_at', null);
    if ((count ?? 0) <= 1) throw new BusinessRuleError('At least one administrator must remain.');
  }
  await db().from('users').update({ deleted_at: new Date().toISOString(), active: false }).eq('id', userId);
  await audit(actor.username, 'DELETE', 'USER', user.username, 'User account deactivated and removed');
}

/**
 * Read-only identity check for the reset flow.
 *
 * Answers "would resetPasswordWithEmail() accept this username/e-mail pair?"
 * without changing anything: no password write, and deliberately no audit row.
 * A failed reset attempt is a genuine security event in the real flow, but a
 * monitoring probe asking the same question is not, and logging it would let
 * health checks pollute the audit trail with events no user caused.
 */
export async function verifyResetIdentity(username: string, email: string): Promise<{ matches: boolean; reason?: string }> {
  const user = await findByUsername(username ?? '');
  if (!user || !user.email || user.email.toLowerCase() !== (email ?? '').trim().toLowerCase()) {
    return { matches: false, reason: 'Username and e-mail address do not match any account.' };
  }
  if (!user.active) return { matches: false, reason: 'This account has been disabled.' };
  return { matches: true };
}

/** Self-service reset: username + registered e-mail must match. */
export async function resetPasswordWithEmail(username: string, email: string, newPassword: string): Promise<void> {
  const user = await findByUsername(username ?? '');
  if (!user || !user.email || user.email.toLowerCase() !== (email ?? '').trim().toLowerCase()) {
    await audit(username, 'PASSWORD_RESET_FAILED', 'AUTH', username, 'Username/e-mail did not match');
    throw new AuthError('Username and e-mail address do not match any account.');
  }
  if (!user.active) throw new AuthError('This account has been disabled.');
  const policy = checkPolicy(newPassword);
  if (policy) throw new ValidationError(policy);
  const salt = generateSalt();
  await db()
    .from('users')
    .update({
      password_salt: salt,
      password_hash: hashPassword(newPassword, salt),
      must_change_password: false,
      locked_until: null,
      failed_login_attempts: 0,
      last_password_reset_at: new Date().toISOString(),
    })
    .eq('id', user.id);
  await audit(user.username, 'PASSWORD_RESET', 'AUTH', user.username, 'Password reset via registered e-mail');
}
