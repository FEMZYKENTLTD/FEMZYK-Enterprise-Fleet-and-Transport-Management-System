package com.femzyk.fleetmanagement.model;

import java.time.LocalDate;

public class FuelRecord extends BaseEntity {

    private Long vehicleId;
    private Long driverId;
    private LocalDate date;
    private double quantityLitres;
    private double unitPrice;
    private double totalCost;
    private Long mileage;
    private String station;
    private String receiptReference;
    private FuelType fuelType;
    private String notes;

    private String vehicleDisplay;
    private String driverDisplay;

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public double getQuantityLitres() { return quantityLitres; }
    public void setQuantityLitres(double quantityLitres) { this.quantityLitres = quantityLitres; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
    public Long getMileage() { return mileage; }
    public void setMileage(Long mileage) { this.mileage = mileage; }
    public String getStation() { return station; }
    public void setStation(String station) { this.station = station; }
    public String getReceiptReference() { return receiptReference; }
    public void setReceiptReference(String receiptReference) { this.receiptReference = receiptReference; }
    public FuelType getFuelType() { return fuelType; }
    public void setFuelType(FuelType fuelType) { this.fuelType = fuelType; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getVehicleDisplay() { return vehicleDisplay; }
    public void setVehicleDisplay(String vehicleDisplay) { this.vehicleDisplay = vehicleDisplay; }
    public String getDriverDisplay() { return driverDisplay; }
    public void setDriverDisplay(String driverDisplay) { this.driverDisplay = driverDisplay; }

    /** total = quantity × unit price, rounded to 2 dp. */
    public void computeTotal() {
        this.totalCost = com.femzyk.fleetmanagement.util.MoneyUtil.round(quantityLitres * unitPrice);
    }
}
