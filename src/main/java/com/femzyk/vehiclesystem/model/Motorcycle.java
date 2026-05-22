package com.femzyk.vehiclesystem.model;

import com.femzyk.vehiclesystem.exception.*;
import com.femzyk.vehiclesystem.interfaces.MotorVehicle;
import com.femzyk.vehiclesystem.interfaces.Vehicle;

/**
 * Motorcycle
 *
 * PURPOSE:
 * Represents a motorcycle in the rental fleet.
 *
 * STORED DATA:
 * - make
 * - model
 * - year
 * - number of wheels
 * - motorcycle type
 * - optional renter name
 * - optional renter phone
 */
public class Motorcycle implements Vehicle, MotorVehicle {

    private static final long serialVersionUID = 1L;

    private final String make;
    private final String model;
    private final int year;

    private int numberOfWheels;
    private String motorcycleType;

    private String renterName;
    private String renterPhone;

    public Motorcycle(String make, String model, int year,
                      int numberOfWheels, String motorcycleType) {

        this(make, model, year, numberOfWheels, motorcycleType, "", "");
    }

    public Motorcycle(String make, String model, int year,
                      int numberOfWheels, String motorcycleType,
                      String renterName, String renterPhone) {

        validateString(make, "Make");
        validateString(model, "Model");
        validateYear(year);
        validateWheels(numberOfWheels);
        validateType(motorcycleType);
        validateOptionalPhone(renterPhone);

        this.make = make.trim();
        this.model = model.trim();
        this.year = year;
        this.numberOfWheels = numberOfWheels;
        this.motorcycleType = motorcycleType.toUpperCase().trim();
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
        return "Motorcycle";
    }

    @Override
    public String getKeyAttribute() {
        return "Type: " + motorcycleType;
    }

    @Override
    public void setNumberOfWheels(int numberOfWheels) {
        validateWheels(numberOfWheels);
        this.numberOfWheels = numberOfWheels;
    }

    @Override
    public int getNumberOfWheels() {
        return numberOfWheels;
    }

    @Override
    public void setMotorcycleType(String motorcycleType) {
        validateType(motorcycleType);
        this.motorcycleType = motorcycleType.toUpperCase().trim();
    }

    @Override
    public String getMotorcycleType() {
        return motorcycleType;
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
            "Make: %s | Model: %s | Year: %d | Wheels: %d | Type: %s | Renter: %s",
            make, model, year, numberOfWheels, motorcycleType, getRenterDisplay()
        );
    }

    public String toHtmlDetail() {
        return String.format(
            "<html>" +
            "<b>Make:</b> %s<br>" +
            "<b>Model:</b> %s<br>" +
            "<b>Year:</b> %d<br>" +
            "<b>Wheels:</b> %d<br>" +
            "<b>Type:</b> %s<br>" +
            "<b>Renter:</b> %s<br>" +
            "<b>Phone:</b> %s" +
            "</html>",
            make,
            model,
            year,
            numberOfWheels,
            motorcycleType,
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
            throw new VehicleException(fieldName + " cannot be empty.");
        }
    }

    private void validateYear(int year) {
        int current = java.time.Year.now().getValue();

        if (year < 1886 || year > current + 1) {
            throw new InvalidYearException(year);
        }
    }

    private void validateWheels(int wheels) {
        if (wheels < 2 || wheels > 3) {
            throw new InvalidWheelsException(wheels);
        }
    }

    private void validateType(String type) {
        if (type == null) {
            throw new VehicleException("Motorcycle type cannot be null.");
        }

        String upper = type.toUpperCase().trim();

        if (!upper.equals(TYPE_SPORT) &&
            !upper.equals(TYPE_CRUISER) &&
            !upper.equals(TYPE_OFFROAD)) {
            throw new VehicleException(
                "Invalid motorcycle type: '" + type +
                "'. Must be SPORT, CRUISER, or OFF-ROAD."
            );
        }
    }

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