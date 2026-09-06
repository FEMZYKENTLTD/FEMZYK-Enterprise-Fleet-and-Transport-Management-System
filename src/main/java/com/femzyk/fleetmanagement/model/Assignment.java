package com.femzyk.fleetmanagement.model;

import java.time.LocalDateTime;

/** Vehicle ↔ driver assignment. Only one ACTIVE assignment per vehicle and per driver at a time. */
public class Assignment extends BaseEntity {

    public enum Status { ACTIVE, RETURNED, CANCELLED }

    private Long vehicleId;
    private Long driverId;
    private LocalDateTime assignedAt;
    private LocalDateTime returnedAt;
    private Status status = Status.ACTIVE;
    private String purpose;
    private String notes;
    private String assignedBy;

    // joined
    private String vehicleDisplay;
    private String driverDisplay;

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }
    public String getVehicleDisplay() { return vehicleDisplay; }
    public void setVehicleDisplay(String vehicleDisplay) { this.vehicleDisplay = vehicleDisplay; }
    public String getDriverDisplay() { return driverDisplay; }
    public void setDriverDisplay(String driverDisplay) { this.driverDisplay = driverDisplay; }
}
