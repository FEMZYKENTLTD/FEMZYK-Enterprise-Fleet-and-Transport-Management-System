package com.femzyk.vehiclesystem.model;

import com.femzyk.vehiclesystem.interfaces.Vehicle;

/** Legacy shadow class for deserialising v4.x fleet-data.dat files. Field names/SUID must not change. */
public class Truck implements Vehicle {
    private static final long serialVersionUID = 1L;
    private String make;
    private String model;
    private int year;
    private double cargoCapacity;
    private String transmissionType;
    private String renterName;
    private String renterPhone;

    public String getMake() { return make; }
    public String getModel() { return model; }
    public int getYear() { return year; }
    public double getCargoCapacity() { return cargoCapacity; }
    public String getTransmissionType() { return transmissionType; }
    public String getRenterName() { return renterName; }
    public String getRenterPhone() { return renterPhone; }
    public String legacyDetail() { return cargoCapacity + " t cargo, " + transmissionType; }
}
