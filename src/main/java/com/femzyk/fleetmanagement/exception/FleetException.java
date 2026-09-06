package com.femzyk.fleetmanagement.exception;

/** Root of the application exception hierarchy. Messages are safe to show to end users. */
public class FleetException extends RuntimeException {
    public FleetException(String message) { super(message); }
    public FleetException(String message, Throwable cause) { super(message, cause); }
}
