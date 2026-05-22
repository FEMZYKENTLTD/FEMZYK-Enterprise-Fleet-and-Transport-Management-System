package com.femzyk.vehiclesystem.model;

import com.femzyk.vehiclesystem.exception.*;
import com.femzyk.vehiclesystem.interfaces.CarVehicle;
import com.femzyk.vehiclesystem.interfaces.Vehicle;

/**
 * Car
 *
 * PURPOSE:
 * Represents a passenger car in the rental fleet.
 *
 * STORED DATA:
 * - make
 * - model
 * - year
 * - number of doors
 * - fuel type
 * - optional renter name
 * - optional renter phone
 *
 * STORAGE ROLE:
 * Car implements Vehicle, and Vehicle extends Serializable. Therefore Car
 * objects can be saved to and loaded from the application's .dat storage files.
 */
public class Car implements Vehicle, CarVehicle {

    private static final long serialVersionUID = 1L;

    private final String make;
    private final String model;
    private final int year;

    private int numberOfDoors;
    private String fuelType;

    /** Optional renter name. Blank means no renter is assigned. */
    private String renterName;

    /** Optional renter phone. Blank means no renter phone is assigned. */
    private String renterPhone;

    public Car(String make, String model, int year,
               int numberOfDoors, String fuelType) {

        this(make, model, year, numberOfDoors, fuelType, "", "");
    }

    public Car(String make, String model, int year,
               int numberOfDoors, String fuelType,
               String renterName, String renterPhone) {

        validateString(make, "Make");
        validateString(model, "Model");
        validateYear(year);
        validateDoors(numberOfDoors);
        validateFuelType(fuelType);
        validateOptionalPhone(renterPhone);

        this.make = make.trim();
        this.model = model.trim();
        this.year = year;
        this.numberOfDoors = numberOfDoors;
        this.fuelType = fuelType.toUpperCase().trim();
        this.renterName = normalizeOptional(renterName);
        this.renterPhone = normalizeOptional(renterPhone);
    }

    @Override
    public String getMake() {
        return make;
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public int getYear() {
        return year;
    }

    @Override
    public String getVehicleType() {
        return "Car";
    }

    @Override
    public String getKeyAttribute() {
        return "Fuel: " + fuelType;
    }

    @Override
    public void setNumberOfDoors(int numberOfDoors) {
        validateDoors(numberOfDoors);
        this.numberOfDoors = numberOfDoors;
    }

    @Override
    public int getNumberOfDoors() {
        return numberOfDoors;
    }

    @Override
    public void setFuelType(String fuelType) {
        validateFuelType(fuelType);
        this.fuelType = fuelType.toUpperCase().trim();
    }

    @Override
    public String getFuelType() {
        return fuelType;
    }

    @Override
    public String getRenterName() {
        return renterName;
    }

    @Override
    public String getRenterPhone() {
        return renterPhone;
    }

    @Override
    public void setRenterName(String renterName) {
        this.renterName = normalizeOptional(renterName);
    }

    @Override
    public void setRenterPhone(String renterPhone) {
        validateOptionalPhone(renterPhone);
        this.renterPhone = normalizeOptional(renterPhone);
    }

    public void setRentalInfo(String renterName, String renterPhone) {
        setRenterName(renterName);
        setRenterPhone(renterPhone);
    }

    @Override
    public String toString() {
        return String.format(
            "Make: %s | Model: %s | Year: %d | Doors: %d | Fuel: %s | Renter: %s",
            make, model, year, numberOfDoors, fuelType, getRenterDisplay()
        );
    }

    public String toHtmlDetail() {
        return String.format(
            "<html>" +
            "<b>Make:</b> %s<br>" +
            "<b>Model:</b> %s<br>" +
            "<b>Year:</b> %d<br>" +
            "<b>Doors:</b> %d<br>" +
            "<b>Fuel Type:</b> %s<br>" +
            "<b>Renter:</b> %s<br>" +
            "<b>Phone:</b> %s" +
            "</html>",
            make,
            model,
            year,
            numberOfDoors,
            fuelType,
            getDisplayRenterName(),
            getDisplayRenterPhone()
        );
    }

    private String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }

    private String getDisplayRenterName() {
        return renterName == null || renterName.isBlank() ? "Not assigned" : renterName;
    }

    private String getDisplayRenterPhone() {
        return renterPhone == null || renterPhone.isBlank() ? "N/A" : renterPhone;
    }

    private void validateString(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new VehicleException(fieldName + " cannot be null or empty.");
        }
    }

    private void validateYear(int year) {
        int current = java.time.Year.now().getValue();

        if (year < 1886 || year > current + 1) {
            throw new InvalidYearException(year);
        }
    }

    private void validateDoors(int doors) {
        if (doors < 2 || doors > 5) {
            throw new InvalidDoorsException(doors);
        }
    }

    private void validateFuelType(String fuelType) {
        if (fuelType == null) {
            throw new InvalidFuelTypeException("null");
        }

        String upper = fuelType.toUpperCase().trim();

        if (!upper.equals(FUEL_PETROL) &&
            !upper.equals(FUEL_DIESEL) &&
            !upper.equals(FUEL_ELECTRIC)) {
            throw new InvalidFuelTypeException(fuelType);
        }
    }

    /**
     * Renter phone is optional. If entered, it must look like a phone number.
     */
    private void validateOptionalPhone(String renterPhone) {
        if (renterPhone == null || renterPhone.trim().isEmpty()) {
            return;
        }

        if (!renterPhone.trim().matches("[0-9+()\\-\\s]{7,20}")) {
            throw new VehicleException(
                "Invalid phone number. Use digits, spaces, +, -, or parentheses."
            );
        }
    }
}