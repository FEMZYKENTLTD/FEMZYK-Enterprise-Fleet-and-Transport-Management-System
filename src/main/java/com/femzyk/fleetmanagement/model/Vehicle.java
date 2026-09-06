package com.femzyk.fleetmanagement.model;

import java.time.LocalDate;

/**
 * Fleet vehicle. Abstract base of {@link Car}, {@link Motorcycle}, {@link Truck} and {@link GenericVehicle}.
 *
 * <p>The type-specific attributes of the original Vehicle Management System (doors/fuel, wheels/style,
 * cargo/transmission) are preserved through subclasses; shared fleet attributes live here.</p>
 */
public abstract class Vehicle extends BaseEntity {

    private String vehicleCode;         // VEH-0001, unique
    private String registrationNumber;  // plate, unique
    private String make;
    private String model;
    private int year;
    private String color;
    private String vin;                 // chassis number, unique when present
    private String engineNumber;
    private LocalDate acquisitionDate;
    private double acquisitionCost;
    private long currentMileage;
    private FuelType fuelType = FuelType.PETROL;
    private int capacity;               // seats (or riders)
    private VehicleStatus status = VehicleStatus.AVAILABLE;
    private String notes;

    // Denormalised (joined) for display
    private String assignedDriverName;
    private Long assignedDriverId;

    public abstract VehicleType getVehicleType();

    /** Short human-readable description of the type-specific attribute, e.g. "4 doors" or "12.5 t". */
    public abstract String getKeyAttribute();

    public String getVehicleCode() { return vehicleCode; }
    public void setVehicleCode(String vehicleCode) { this.vehicleCode = vehicleCode; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }
    public String getEngineNumber() { return engineNumber; }
    public void setEngineNumber(String engineNumber) { this.engineNumber = engineNumber; }
    public LocalDate getAcquisitionDate() { return acquisitionDate; }
    public void setAcquisitionDate(LocalDate acquisitionDate) { this.acquisitionDate = acquisitionDate; }
    public double getAcquisitionCost() { return acquisitionCost; }
    public void setAcquisitionCost(double acquisitionCost) { this.acquisitionCost = acquisitionCost; }
    public long getCurrentMileage() { return currentMileage; }
    public void setCurrentMileage(long currentMileage) { this.currentMileage = currentMileage; }
    public FuelType getFuelType() { return fuelType; }
    public void setFuelType(FuelType fuelType) { this.fuelType = fuelType; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public VehicleStatus getStatus() { return status; }
    public void setStatus(VehicleStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getAssignedDriverName() { return assignedDriverName; }
    public void setAssignedDriverName(String assignedDriverName) { this.assignedDriverName = assignedDriverName; }
    public Long getAssignedDriverId() { return assignedDriverId; }
    public void setAssignedDriverId(Long assignedDriverId) { this.assignedDriverId = assignedDriverId; }

    public String getIdentityDisplay() {
        return year + " " + make + " " + model;
    }

    public String getDisplayName() {
        return registrationNumber + " (" + getIdentityDisplay() + ")";
    }

    // --- type-specific attribute storage (single-table inheritance) -----------------------------
    /** Numeric extra attribute (doors, wheels, cargo tons). */
    public abstract Double getAttributeNumber();
    public abstract void setAttributeNumber(Double value);
    /** Textual extra attribute (motorcycle style, transmission). */
    public abstract String getAttributeText();
    public abstract void setAttributeText(String value);

    public static Vehicle newOfType(VehicleType type) {
        switch (type) {
            case CAR: return new Car();
            case MOTORCYCLE: return new Motorcycle();
            case TRUCK: return new Truck();
            default: return new GenericVehicle(type);
        }
    }

    @Override public String toString() { return getDisplayName(); }
}
