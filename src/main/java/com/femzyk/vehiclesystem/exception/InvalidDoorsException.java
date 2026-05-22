package com.femzyk.vehiclesystem.exception;

/**
 * InvalidDoorsException
 * Thrown when door count is outside the valid range of 2 to 5.
 */
public class InvalidDoorsException extends VehicleException {
    private static final long serialVersionUID = 1L;
    public InvalidDoorsException(int doors) {
        super("Invalid door count: " + doors + ". Must be between 2 and 5.");
    }
}