package com.femzyk.vehiclesystem.exception;

/**
 * VehicleException â€" Base Custom Exception
 *
 * PURPOSE:
 * Root of the custom exception hierarchy for the Vehicle Information System.
 * All vehicle-related exceptions extend this class, allowing callers to
 * catch either a specific exception type or the broad VehicleException
 * for general vehicle validation failure.
 *
 * DESIGN RATIONALE:
 * Using a custom exception hierarchy instead of generic RuntimeException
 * or IllegalArgumentException provides:
 * 1. Precise error identification â€" catch exactly what failed
 * 2. Clean error messages for GUI dialog display
 * 3. Type-safe exception handling in service and GUI layers
 * 4. Industry-standard error architecture compatible with Spring/Jakarta EE
 *
 * @author  Femzyk Enterprise Systems
 * @version 4.0
 */
public class VehicleException extends RuntimeException {

    /** Serialization identifier */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a VehicleException with the specified detail message.
     *
     * @param message Human-readable description of what went wrong
     */
    public VehicleException(String message) {
        super(message);
    }

    /**
     * Constructs a VehicleException with message and root cause.
     *
     * @param message Human-readable description
     * @param cause   The underlying exception that triggered this one
     */
    public VehicleException(String message, Throwable cause) {
        super(message, cause);
    }
}