package com.femzyk.fleetmanagement.model;

/** Application roles. Ordered from most to least privileged. */
public enum Role {
    ADMINISTRATOR("Administrator", "Full system access including user management, backup and audit log."),
    FLEET_MANAGER("Fleet Manager", "Manages vehicles, drivers, assignments, trips, maintenance and reports."),
    OPERATIONS_OFFICER("Operations Officer", "Records operational data (trips, fuel, expenses, maintenance) and runs reports."),
    VIEWER("Viewer", "Read-only access to all records and reports.");

    private final String displayName;
    private final String description;

    Role(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    @Override public String toString() { return displayName; }
}
