package com.femzyk.vehiclesystem.exception;

/**
 * InvalidCargoException
 * Thrown when cargo capacity is zero or negative.
 */
public class InvalidCargoException extends VehicleException {
    private static final long serialVersionUID = 1L;
    public InvalidCargoException(double capacity) {
        super("Invalid cargo capacity: " + capacity +
              ". Must be greater than 0.");
    }
}