package com.femzyk.fleetmanagement.exception;

/** A uniqueness rule was violated (registration number, licence number, employee code, username...). */
public class DuplicateRecordException extends ValidationException {
    public DuplicateRecordException(String message) { super(message); }
}
