package com.femzyk.vehiclesystem.exception;

/**
 * InvalidFuelTypeException
 * Thrown when a fuel type string does not match PETROL, DIESEL, or ELECTRIC.
 */
public class InvalidFuelTypeException extends VehicleException {
    private static final long serialVersionUID = 1L;
    public InvalidFuelTypeException(String fuelType) {
        super("Invalid fuel type: '" + fuelType +
              "'. Must be PETROL, DIESEL, or ELECTRIC.");
    }
}