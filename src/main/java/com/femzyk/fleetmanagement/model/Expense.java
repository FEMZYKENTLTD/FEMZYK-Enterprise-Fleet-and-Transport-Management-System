package com.femzyk.fleetmanagement.model;

import java.time.LocalDate;

public class Expense extends BaseEntity {

    private Long vehicleId;           // optional: general expenses have no vehicle
    private LocalDate date;
    private ExpenseCategory category = ExpenseCategory.MISCELLANEOUS;
    private double amount;
    private String vendor;
    private String description;
    private String reference;
    private String notes;
    private Long sourceFuelRecordId;         // set when auto-created from a fuel record
    private Long sourceMaintenanceRecordId;  // set when auto-created from a maintenance record

    private String vehicleDisplay;

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public ExpenseCategory getCategory() { return category; }
    public void setCategory(ExpenseCategory category) { this.category = category; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Long getSourceFuelRecordId() { return sourceFuelRecordId; }
    public void setSourceFuelRecordId(Long sourceFuelRecordId) { this.sourceFuelRecordId = sourceFuelRecordId; }
    public Long getSourceMaintenanceRecordId() { return sourceMaintenanceRecordId; }
    public void setSourceMaintenanceRecordId(Long v) { this.sourceMaintenanceRecordId = v; }
    public String getVehicleDisplay() { return vehicleDisplay; }
    public void setVehicleDisplay(String vehicleDisplay) { this.vehicleDisplay = vehicleDisplay; }
    public boolean isSystemGenerated() { return sourceFuelRecordId != null || sourceMaintenanceRecordId != null; }
}
