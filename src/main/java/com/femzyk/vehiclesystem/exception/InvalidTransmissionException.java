package com.femzyk.vehiclesystem.exception;

/**
 * InvalidTransmissionException
 * Thrown when transmission type does not match MANUAL or AUTOMATIC.
 */
public class InvalidTransmissionException extends VehicleException {
    private static final long serialVersionUID = 1L;
    public InvalidTransmissionException(String type) {
        super("Invalid transmission type: '" + type +
              "'. Must be MANUAL or AUTOMATIC.");
    }
}