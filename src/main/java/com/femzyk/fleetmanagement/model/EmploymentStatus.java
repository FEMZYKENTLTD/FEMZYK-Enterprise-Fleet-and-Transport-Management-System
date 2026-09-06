package com.femzyk.fleetmanagement.model;

public enum EmploymentStatus {
    ACTIVE, ON_LEAVE, SUSPENDED, TERMINATED;

    public boolean isWorking() { return this == ACTIVE; }
}
