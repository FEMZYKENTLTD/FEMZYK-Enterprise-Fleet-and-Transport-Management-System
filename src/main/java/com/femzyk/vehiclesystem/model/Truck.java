package com.femzyk.vehiclesystem.model;

import com.femzyk.vehiclesystem.exception.*;
import com.femzyk.vehiclesystem.interfaces.TruckVehicle;
import com.femzyk.vehiclesystem.interfaces.Vehicle;

/**
 * Truck
 *
 * PURPOSE:
 * Represents a commercial truck in the rental fleet.
 *
 * STORED DATA:
 * - make
 * - model
 * - year
 * - cargo capacity
 * - transmission type
 * - optional renter name
 * - optional renter phone
 */
public class Truck implements Vehicle, TruckVehicle {

    private static final long serialVersionUID = 1L;

    private final String make;
    private final String model;
    private final int year;

    private double cargoCapacity;
    private String transmissionType;

    private String renterName;
    private String renterPhone;

    public Truck(String make, String model, int year,
                 double cargoCapacity, String transmissionType) {

        this(make, model, year, cargoCapacity, transmissionType, "", "");
    }

    public Truck(String make, String model, int year,
                 double cargoCapacity, String transmissionType,
                 String renterName, String renterPhone) {

        validateString(make, "Make");
        validateString(model, "Model");
        validateYear(year);
        validateCargo(cargoCapacity);
        validateTransmission(transmissionType);
        validateOptionalPhone(renterPhone);

        this.make = make.trim();
        this.model = model.trim();
        this.year = year;
        this.cargoCapacity = cargoCapacity;
        this.transmissionType = transmissionType.toUpperCase().trim();
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
        return "Truck";
    }

    @Override
    public String getKeyAttribute() {
        return String.format("Cargo: %.1f tons", cargoCapacity);
    }

    @Override
    public void setCargoCapacity(double capacityInTons) {
        validateCargo(capacityInTons);
        this.cargoCapacity = capacityInTons;
    }

    @Override
    public double getCargoCapacity() {
        return cargoCapacity;
    }

    @Override
    public void setTransmissionType(String transmissionType) {
        validateTransmission(transmissionType);
        this.transmissionType = transmissionType.toUpperCase().trim();
    }

    @Override
    public String getTransmissionType() {
        return transmissionType;
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
            "Make: %s | Model: %s | Year: %d | Cargo: %.1f tons | Trans: %s | Renter: %s",
            make, model, year, cargoCapacity, transmissionType, getRenterDisplay()
        );
    }

    public String toHtmlDetail() {
        return String.format(
            "<html>" +
            "<b>Make:</b> %s<br>" +
            "<b>Model:</b> %s<br>" +
            "<b>Year:</b> %d<br>" +
            "<b>Cargo Capacity:</b> %.1f tons<br>" +
            "<b>Transmission:</b> %s<br>" +
            "<b>Renter:</b> %s<br>" +
            "<b>Phone:</b> %s" +
            "</html>",
            make,
            model,
            year,
            cargoCapacity,
            transmissionType,
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

    private void validateCargo(double cargo) {
        if (cargo <= 0) {
            throw new InvalidCargoException(cargo);
        }
    }

    private void validateTransmission(String transmission) {
        if (transmission == null) {
            throw new InvalidTransmissionException("null");
        }

        String upper = transmission.toUpperCase().trim();

        if (!upper.equals(TRANSMISSION_MANUAL) &&
            !upper.equals(TRANSMISSION_AUTOMATIC)) {
            throw new InvalidTransmissionException(transmission);
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