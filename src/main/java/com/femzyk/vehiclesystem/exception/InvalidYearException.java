package com.femzyk.vehiclesystem.exception;

/**
 * InvalidYearException
 * Thrown when a manufacturing year is outside the valid range (1886 to current+1).
 */
public class InvalidYearException extends VehicleException {
    private static final long serialVersionUID = 1L;
    public InvalidYearException(int year) {
        super("Invalid manufacturing year: " + year +
              ". Must be between 1886 and " +
              (java.time.Year.now().getValue() + 1) + ".");
    }
}