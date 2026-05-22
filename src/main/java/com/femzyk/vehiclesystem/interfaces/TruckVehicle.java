package com.femzyk.vehiclesystem.interfaces;

/**
 * TruckVehicle Interface â€" Truck-Specific Contract
 *
 * PURPOSE:
 * Defines the contract for truck-specific attributes. Implementing
 * classes manage cargo capacity in metric tons and transmission type.
 *
 * WHY double FOR CARGO CAPACITY:
 * Using double instead of int supports fractional ton values (e.g., 2.5 tons),
 * reflecting precision required in commercial freight operations where
 * half-ton differences affect load planning and regulatory compliance.
 *
 * LEGAL SIGNIFICANCE:
 * Transmission type tracking is a legal requirement in many jurisdictions â€"
 * commercial driver's licenses specify manual or automatic authorization.
 * Rental agencies must match vehicle transmission to renter's license class.
 *
 * @author  Femzyk Enterprise Systems
 * @version 4.0 â€" Premium GUI Edition
 * @since   2024
 */
public interface TruckVehicle {

    /** Transmission type: manual gear shift */
    String TRANSMISSION_MANUAL    = "MANUAL";

    /** Transmission type: automatic gear shift */
    String TRANSMISSION_AUTOMATIC = "AUTOMATIC";

    /** All valid transmission types for validation and combo box population */
    String[] TRANSMISSION_TYPES = { TRANSMISSION_MANUAL, TRANSMISSION_AUTOMATIC };

    /**
     * Sets the cargo capacity of this truck in metric tons.
     * Must be a positive value greater than zero.
     *
     * @param capacityInTons Cargo capacity â€" must be > 0
     * @throws com.femzyk.vehiclesystem.exception.InvalidCargoException if invalid
     */
    void setCargoCapacity(double capacityInTons);

    /**
     * Returns this truck's cargo capacity in metric tons.
     *
     * @return Positive double representing tons
     */
    double getCargoCapacity();

    /**
     * Sets the transmission type for this truck.
     * Must match TRANSMISSION_MANUAL or TRANSMISSION_AUTOMATIC.
     *
     * @param transmissionType Transmission classification
     * @throws com.femzyk.vehiclesystem.exception.InvalidTransmissionException if invalid
     */
    void setTransmissionType(String transmissionType);

    /**
     * Returns this truck's transmission type.
     *
     * @return String matching one of the TRANSMISSION_* constants
     */
    String getTransmissionType();
}