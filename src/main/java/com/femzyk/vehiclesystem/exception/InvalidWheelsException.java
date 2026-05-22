package com.femzyk.vehiclesystem.exception;

/**
 * InvalidWheelsException
 * Thrown when wheel count is not 2 or 3 for a motorcycle.
 */
public class InvalidWheelsException extends VehicleException {
    private static final long serialVersionUID = 1L;
    public InvalidWheelsException(int wheels) {
        super("Invalid wheel count: " + wheels +
              ". Motorcycles must have 2 or 3 wheels.");
    }
}