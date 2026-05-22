package com.femzyk.vehiclesystem.interfaces;

import java.io.Serializable;

/**
 * Vehicle Interface
 *
 * PURPOSE:
 * Defines the foundational contract that all vehicle types must fulfill.
 *
 * SERIALIZATION ROLE:
 * Vehicle extends Serializable so Car, Motorcycle, and Truck objects can be
 * saved to local .dat files and loaded again when the program restarts.
 *
 * OOP PRINCIPLE:
 * This interface enables polymorphism. The GUI can store all vehicle types
 * in a List<Vehicle> while preserving their concrete behavior at runtime.
 */
public interface Vehicle extends Serializable {

    String getMake();

    String getModel();

    int getYear();

    String getVehicleType();

    String getKeyAttribute();

    /**
     * Returns the raw renter name value.
     *
     * This may be blank because renter information is optional.
     */
    String getRenterName();

    /**
     * Returns the raw renter phone value.
     *
     * This may be blank because renter information is optional.
     */
    String getRenterPhone();

    void setRenterName(String renterName);

    void setRenterPhone(String renterPhone);

    default String getIdentityDisplay() {
        return getYear() + " " + getMake() + " " + getModel();
    }

    /**
     * Returns renter information formatted for display.
     *
     * If renter information is blank, user-friendly fallback values are used.
     */
    default String getRenterDisplay() {
        String name = getRenterName();
        String phone = getRenterPhone();

        if (name == null || name.trim().isEmpty()) {
            name = "Not assigned";
        }

        if (phone == null || phone.trim().isEmpty()) {
            phone = "N/A";
        }

        return name + " | " + phone;
    }
}