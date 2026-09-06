package com.femzyk.fleetmanagement.exception;

public class RecordNotFoundException extends FleetException {
    public RecordNotFoundException(String entity, Object id) {
        super(entity + " with id " + id + " was not found.");
    }
}
