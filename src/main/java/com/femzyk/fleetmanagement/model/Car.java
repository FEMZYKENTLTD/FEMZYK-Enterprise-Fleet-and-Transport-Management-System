package com.femzyk.fleetmanagement.model;

/** Passenger car. Type attributes retained from the original system: number of doors. */
public class Car extends Vehicle {

    private int numberOfDoors = 4;
    private String transmission = "AUTOMATIC";

    @Override public VehicleType getVehicleType() { return VehicleType.CAR; }
    @Override public String getKeyAttribute() { return numberOfDoors + " doors, " + transmission.toLowerCase(); }

    public int getNumberOfDoors() { return numberOfDoors; }
    public void setNumberOfDoors(int numberOfDoors) { this.numberOfDoors = numberOfDoors; }
    public String getTransmission() { return transmission; }
    public void setTransmission(String transmission) { this.transmission = transmission; }

    @Override public Double getAttributeNumber() { return (double) numberOfDoors; }
    @Override public void setAttributeNumber(Double value) { if (value != null) numberOfDoors = value.intValue(); }
    @Override public String getAttributeText() { return transmission; }
    @Override public void setAttributeText(String value) { if (value != null) transmission = value; }
}
