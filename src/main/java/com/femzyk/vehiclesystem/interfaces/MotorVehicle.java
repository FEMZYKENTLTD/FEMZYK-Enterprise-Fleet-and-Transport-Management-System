package com.femzyk.vehiclesystem.interfaces;

/**
 * MotorVehicle Interface â€" Motorcycle-Specific Contract
 *
 * PURPOSE:
 * Defines the contract for motorcycle-specific attributes. Implementing
 * classes manage wheel count and motorcycle classification type.
 *
 * COMMERCIAL SIGNIFICANCE:
 * Motorcycle type classification affects insurance rates, license
 * requirements, and safety equipment specifications â€" making accurate
 * classification a business and legal necessity for rental agencies,
 * not merely a UI feature.
 *
 * INTERFACE CONSTANTS:
 * TYPE_* constants standardize classification terminology across the
 * entire system, preventing inconsistencies like "offroad" vs "off-road"
 * vs "Off Road" that would corrupt fleet reports and filtering.
 *
 * @author  Femzyk Enterprise Systems
 * @version 4.0 â€" Premium GUI Edition
 * @since   2024
 */
public interface MotorVehicle {

    /** Motorcycle classification: sport/performance bikes */
    String TYPE_SPORT   = "SPORT";

    /** Motorcycle classification: cruiser/touring bikes */
    String TYPE_CRUISER = "CRUISER";

    /** Motorcycle classification: off-road/dirt bikes */
    String TYPE_OFFROAD = "OFF-ROAD";

    /** All valid motorcycle types for validation and combo box population */
    String[] MOTORCYCLE_TYPES = { TYPE_SPORT, TYPE_CRUISER, TYPE_OFFROAD };

    /**
     * Sets the number of wheels on this motorcycle.
     * Standard motorcycles: 2 wheels. Trikes: 3 wheels.
     *
     * @param numberOfWheels Wheel count â€" must be 2 or 3
     * @throws com.femzyk.vehiclesystem.exception.InvalidWheelsException if invalid
     */
    void setNumberOfWheels(int numberOfWheels);

    /**
     * Returns the number of wheels on this motorcycle.
     *
     * @return Integer 2 or 3
     */
    int getNumberOfWheels();

    /**
     * Sets the motorcycle's classification type.
     * Must match TYPE_SPORT, TYPE_CRUISER, or TYPE_OFFROAD.
     *
     * @param motorcycleType Classification string
     * @throws com.femzyk.vehiclesystem.exception.VehicleException if unrecognized
     */
    void setMotorcycleType(String motorcycleType);

    /**
     * Returns this motorcycle's classification type.
     *
     * @return String matching one of the TYPE_* constants
     */
    String getMotorcycleType();
}