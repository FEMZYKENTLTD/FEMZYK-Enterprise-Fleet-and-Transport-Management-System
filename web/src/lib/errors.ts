/**
 * Error types carried over from com.femzyk.fleetmanagement.exception so the web
 * API returns the same categories of failure the desktop services throw.
 */

export class AppError extends Error {
  constructor(message: string, public readonly status = 400, public readonly code = 'BAD_REQUEST') {
    super(message);
    this.name = new.target.name;
  }
}

export class ValidationError extends AppError {
  constructor(message: string, public readonly errors: string[] = [message]) {
    super(message, 422, 'VALIDATION');
  }
}

export class BusinessRuleError extends AppError {
  constructor(message: string) {
    super(message, 409, 'BUSINESS_RULE');
  }
}

export class DuplicateRecordError extends AppError {
  constructor(message: string) {
    super(message, 409, 'DUPLICATE');
  }
}

export class AuthError extends AppError {
  constructor(message: string) {
    super(message, 401, 'UNAUTHENTICATED');
  }
}

export class ForbiddenError extends AppError {
  constructor(message: string) {
    super(message, 403, 'FORBIDDEN');
  }
}

export class NotFoundError extends AppError {
  constructor(entity: string, id?: number | string) {
    super(`${entity}${id !== undefined ? ` #${id}` : ''} was not found.`, 404, 'NOT_FOUND');
  }
}

/** Maps a raw Postgres/Supabase error to a friendly message where possible. */
export function translateDbError(error: unknown): AppError {
  const e = error as { message?: string; details?: string; code?: string };
  const message = e?.message ?? 'Unexpected database error.';
  if (e?.code === '23505' || /duplicate key/i.test(message)) {
    if (/uq_assignment_active_vehicle/.test(message)) return new BusinessRuleError('This vehicle already has an active assignment.');
    if (/uq_assignment_active_driver/.test(message)) return new BusinessRuleError('This driver already has a vehicle assigned.');
    const field = /users_username|users_email|employees_email|drivers_licence|vehicles_registration|_key/i.exec(message);
    return new DuplicateRecordError(
      field ? `A record with the same ${guessField(message)} already exists.` : 'A record with the same unique value already exists.',
    );
  }
  if (e?.code === '23503' || /foreign key/i.test(message)) {
    return new BusinessRuleError('Another record depends on this one, so it cannot be removed.');
  }
  if (e?.code === '23514' || /violates check constraint/i.test(message)) {
    return new ValidationError('A value was rejected by the database rules. Please check the form and try again.');
  }
  return new AppError(message, 500, 'DATABASE');
}

function guessField(message: string): string {
  if (/username/i.test(message)) return 'username';
  if (/email/i.test(message)) return 'email';
  if (/licence/i.test(message)) return 'licence number';
  if (/registration/i.test(message)) return 'registration number';
  if (/employee_code/i.test(message)) return 'employee code';
  if (/driver_code/i.test(message)) return 'driver code';
  return 'value';
}

export function errorResponse(error: unknown): { message: string; code: string; details?: string[] } {
  if (error instanceof ValidationError) return { message: error.message, code: error.code, details: error.errors };
  if (error instanceof AppError) return { message: error.message, code: error.code };
  const translated = translateDbError(error);
  return { message: translated.message, code: translated.code };
}
