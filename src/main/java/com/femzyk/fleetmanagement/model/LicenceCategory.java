package com.femzyk.fleetmanagement.model;

/** Nigerian FRSC driver's licence classes (simplified). */
public enum LicenceCategory {
    A("Motorcycle"),
    B("Car / light vehicle"),
    C("Light truck / van"),
    D("Heavy truck / bus"),
    E("Articulated / heavy duty");

    private final String description;
    LicenceCategory(String description) { this.description = description; }
    public String getDescription() { return description; }

    /** Whether this licence class permits driving the given vehicle type. */
    public boolean permits(VehicleType type) {
        switch (type) {
            case MOTORCYCLE: return this == A || this.ordinal() >= C.ordinal();
            case CAR: return this.ordinal() >= B.ordinal();
            case TRUCK: return this.ordinal() >= C.ordinal();
            case BUS: return this.ordinal() >= D.ordinal();
            case VAN: return this.ordinal() >= B.ordinal();
            default: return this.ordinal() >= B.ordinal();
        }
    }
}
