package com.femzyk.fleetmanagement.model;

/** Vans and buses: no special attributes beyond seating capacity. */
public class GenericVehicle extends Vehicle {

    private final VehicleType type;
    private Double attributeNumber;
    private String attributeText;

    public GenericVehicle(VehicleType type) { this.type = type; }

    @Override public VehicleType getVehicleType() { return type; }
    @Override public String getKeyAttribute() { return getCapacity() + " seats"; }
    @Override public Double getAttributeNumber() { return attributeNumber; }
    @Override public void setAttributeNumber(Double value) { this.attributeNumber = value; }
    @Override public String getAttributeText() { return attributeText; }
    @Override public void setAttributeText(String value) { this.attributeText = value; }
}
