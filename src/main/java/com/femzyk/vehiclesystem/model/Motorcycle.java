package com.femzyk.vehiclesystem.model;

import com.femzyk.vehiclesystem.interfaces.Vehicle;

/** Legacy shadow class for deserialising v4.x fleet-data.dat files. Field names/SUID must not change. */
public class Motorcycle implements Vehicle {
    private static final long serialVersionUID = 1L;
    private String make;
    private String model;
    private int year;
    private int numberOfWheels;
    private String motorcycleType;
    private String renterName;
    private String renterPhone;

    public String getMake() { return make; }
    public String getModel() { return model; }
    public int getYear() { return year; }
    public int getNumberOfWheels() { return numberOfWheels; }
    public String getMotorcycleType() { return motorcycleType; }
    public String getRenterName() { return renterName; }
    public String getRenterPhone() { return renterPhone; }
    public String legacyDetail() { return numberOfWheels + " wheels, type " + motorcycleType; }
}
