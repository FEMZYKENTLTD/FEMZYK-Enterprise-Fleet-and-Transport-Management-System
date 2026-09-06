package com.femzyk.fleetmanagement.model;

/** Truck. Type attributes retained from the original system: cargo capacity (tons) and transmission. */
public class Truck extends Vehicle {

    private double cargoCapacityTons = 1.0;
    private String transmission = "MANUAL";

    @Override public VehicleType getVehicleType() { return VehicleType.TRUCK; }
    @Override public String getKeyAttribute() { return cargoCapacityTons + " t cargo, " + transmission.toLowerCase(); }

    public double getCargoCapacityTons() { return cargoCapacityTons; }
    public void setCargoCapacityTons(double cargoCapacityTons) { this.cargoCapacityTons = cargoCapacityTons; }
    public String getTransmission() { return transmission; }
    public void setTransmission(String transmission) { this.transmission = transmission; }

    @Override public Double getAttributeNumber() { return cargoCapacityTons; }
    @Override public void setAttributeNumber(Double value) { if (value != null) cargoCapacityTons = value; }
    @Override public String getAttributeText() { return transmission; }
    @Override public void setAttributeText(String value) { if (value != null) transmission = value; }
}
