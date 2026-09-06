package com.femzyk.fleetmanagement.exception;

/** Wraps SQLException and I/O problems from the persistence layer. Technical detail is logged, not shown. */
public class DataAccessException extends FleetException {
    public DataAccessException(String message, Throwable cause) { super(message, cause); }
}
