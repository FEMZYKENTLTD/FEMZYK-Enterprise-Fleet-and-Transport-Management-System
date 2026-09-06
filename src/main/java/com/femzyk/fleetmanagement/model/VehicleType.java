package com.femzyk.fleetmanagement.model;

public enum VehicleType {
    CAR("Car"), MOTORCYCLE("Motorcycle"), TRUCK("Truck"), VAN("Van"), BUS("Bus");

    private final String label;
    VehicleType(String label) { this.label = label; }
    public String getLabel() { return label; }
    @Override public String toString() { return label; }
}
