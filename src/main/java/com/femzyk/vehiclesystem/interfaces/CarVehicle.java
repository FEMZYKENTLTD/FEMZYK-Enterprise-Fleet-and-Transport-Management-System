package com.femzyk.vehiclesystem.interfaces;

/**
 * CarVehicle Interface â€" Car-Specific Contract
 *
 * PURPOSE:
 * Defines the contract for car-specific attributes extending beyond the
 * base Vehicle interface. Implementing classes manage door count and
 * fuel type classification.
 *
 * INTERFACE SEGREGATION PRINCIPLE:
 * Car-specific behavior is isolated here. Motorcycle and Truck classes
 * are never forced to implement car-specific methods they have no use for.
 * This keeps each interface focused and coherent.
 *
 * INTERFACE CONSTANTS:
 * Fuel type values are defined as constants here (implicitly public static
 * final) to standardize values across all implementing classes and prevent
 * free-form strings like "gas", "petro", "Gas" that break comparison logic.
 *
 * GUI INTEGRATION:
 * The GUI uses these constants to populate fuel type combo boxes, ensuring
 * only valid values can be selected â€" making invalid fuel type exceptions
 * practically impossible through the GUI while still protected in code.
 *
 * @author  Femzyk Enterprise Systems
 * @version 4.0 â€" Premium GUI Edition
 * @since   2024
 */
public interface CarVehicle {

    /** Standard fuel type: petrol/gasoline powered */
    String FUEL_PETROL   = "PETROL";

    /** Standard fuel type: diesel powered */
    String FUEL_DIESEL   = "DIESEL";

    /** Standard fuel type: battery electric */
    String FUEL_ELECTRIC = "ELECTRIC";

    /** All valid fuel types for validation and combo box population */
    String[] FUEL_TYPES = { FUEL_PETROL, FUEL_DIESEL, FUEL_ELECTRIC };

    /**
     * Sets the number of doors on this car.
     * Valid range: 2 (coupe/convertible) to 5 (hatchback/SUV).
     *
     * @param numberOfDoors Door count â€" must be between 2 and 5 inclusive
     * @throws com.femzyk.vehiclesystem.exception.InvalidDoorsException if out of range
     */
    void setNumberOfDoors(int numberOfDoors);

    /**
     * Returns the number of doors on this car.
     *
     * @return Integer between 2 and 5
     */
    int getNumberOfDoors();

    /**
     * Sets the fuel type for this car.
     * Must match one of the FUEL_* constants (case-insensitive).
     *
     * @param fuelType Fuel classification string
     * @throws com.femzyk.vehiclesystem.exception.InvalidFuelTypeException if unrecognized
     */
    void setFuelType(String fuelType);

    /**
     * Returns the fuel type classification of this car.
     *
     * @return String matching one of the FUEL_* constants
     */
    String getFuelType();
}