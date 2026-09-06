package com.femzyk.fleetmanagement.model;

public enum DriverStatus {
    ACTIVE, ON_LEAVE, SUSPENDED, INACTIVE;

    public boolean canBeAssigned() { return this == ACTIVE; }
}
