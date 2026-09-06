package com.femzyk.fleetmanagement.model;

import java.time.LocalDateTime;

public class Trip extends BaseEntity {

    public enum Status { PLANNED, ACTIVE, COMPLETED, CANCELLED }

    private String tripCode;          // TRP-0001
    private Long vehicleId;
    private Long driverId;
    private String origin;
    private String destination;
    private String purpose;
    private LocalDateTime departureTime;
    private LocalDateTime returnTime;
    private Long startMileage;
    private Long endMileage;
    private Double fuelUsedLitres;
    private Status status = Status.PLANNED;
    private String notes;

    private String vehicleDisplay;
    private String driverDisplay;

    public String getTripCode() { return tripCode; }
    public void setTripCode(String tripCode) { this.tripCode = tripCode; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }
    public LocalDateTime getReturnTime() { return returnTime; }
    public void setReturnTime(LocalDateTime returnTime) { this.returnTime = returnTime; }
    public Long getStartMileage() { return startMileage; }
    public void setStartMileage(Long startMileage) { this.startMileage = startMileage; }
    public Long getEndMileage() { return endMileage; }
    public void setEndMileage(Long endMileage) { this.endMileage = endMileage; }
    public Double getFuelUsedLitres() { return fuelUsedLitres; }
    public void setFuelUsedLitres(Double fuelUsedLitres) { this.fuelUsedLitres = fuelUsedLitres; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getVehicleDisplay() { return vehicleDisplay; }
    public void setVehicleDisplay(String vehicleDisplay) { this.vehicleDisplay = vehicleDisplay; }
    public String getDriverDisplay() { return driverDisplay; }
    public void setDriverDisplay(String driverDisplay) { this.driverDisplay = driverDisplay; }

    /** Distance derived from odometer readings; null until both readings exist. */
    public Long getDistanceKm() {
        if (startMileage == null || endMileage == null) return null;
        return endMileage - startMileage;
    }

    /** km per litre, only when both distance and fuel are known and positive. */
    public Double getFuelEfficiencyKmPerLitre() {
        Long d = getDistanceKm();
        if (d == null || d <= 0 || fuelUsedLitres == null || fuelUsedLitres <= 0) return null;
        return d / fuelUsedLitres;
    }
}
