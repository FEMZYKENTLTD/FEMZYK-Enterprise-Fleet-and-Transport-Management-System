import { randomBytes, pbkdf2Sync, timingSafeEqual } from 'node:crypto';

/**
 * PBKDF2WithHmacSHA256 password hashing — byte-for-byte compatible with
 * com.femzyk.fleetmanagement.security.PasswordUtil so a user row exported from
 * the desktop SQLite database verifies here unchanged (and vice versa).
 */
const ITERATIONS = 65_536;
const KEY_LENGTH = 32; // 256 bits
const SALT_BYTES = 16;

export function generateSalt(): string {
  return randomBytes(SALT_BYTES).toString('base64');
}

export function hashPassword(password: string, salt: string): string {
  return pbkdf2Sync(Buffer.from(password, 'utf8'), Buffer.from(salt, 'base64'), ITERATIONS, KEY_LENGTH, 'sha256').toString('base64');
}

export function verifyPassword(entered: string, salt: string, expectedHash: string): boolean {
  if (!entered || !salt || !expectedHash) return false;
  const actual = Buffer.from(hashPassword(entered, salt));
  const expected = Buffer.from(expectedHash);
  if (actual.length !== expected.length) return false;
  return timingSafeEqual(actual, expected);
}

/** Same policy as PasswordUtil.checkPolicy(): 8+ chars, at least one letter and one digit. */
export function checkPolicy(password: string | null | undefined): string | null {
  if (!password || password.length < 8) return 'Password must be at least 8 characters long.';
  if (!/[A-Za-z]/.test(password) || !/\d/.test(password)) return 'Password must contain at least one letter and one digit.';
  return null;
}
