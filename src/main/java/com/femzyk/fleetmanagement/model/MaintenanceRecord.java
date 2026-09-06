package com.femzyk.fleetmanagement.model;

import com.femzyk.fleetmanagement.config.AppConfig;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class MaintenanceRecord extends BaseEntity {

    public enum Type { ROUTINE_SERVICE, REPAIR, INSPECTION, TYRE, BODYWORK, ELECTRICAL, OTHER }
    public enum Status { SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED }

    private Long vehicleId;
    private LocalDate serviceDate;
    private Type type = Type.ROUTINE_SERVICE;
    private String description;
    private String provider;
    private double cost;
    private Long mileageAtService;
    private LocalDate nextServiceDate;
    private Long nextServiceMileage;
    private Status status = Status.COMPLETED;
    private String notes;

    private String vehicleDisplay;
    private Long vehicleCurrentMileage;

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public LocalDate getServiceDate() { return serviceDate; }
    public void setServiceDate(LocalDate serviceDate) { this.serviceDate = serviceDate; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
    public Long getMileageAtService() { return mileageAtService; }
    public void setMileageAtService(Long mileageAtService) { this.mileageAtService = mileageAtService; }
    public LocalDate getNextServiceDate() { return nextServiceDate; }
    public void setNextServiceDate(LocalDate nextServiceDate) { this.nextServiceDate = nextServiceDate; }
    public Long getNextServiceMileage() { return nextServiceMileage; }
    public void setNextServiceMileage(Long nextServiceMileage) { this.nextServiceMileage = nextServiceMileage; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getVehicleDisplay() { return vehicleDisplay; }
    public void setVehicleDisplay(String vehicleDisplay) { this.vehicleDisplay = vehicleDisplay; }
    public Long getVehicleCurrentMileage() { return vehicleCurrentMileage; }
    public void setVehicleCurrentMileage(Long vehicleCurrentMileage) { this.vehicleCurrentMileage = vehicleCurrentMileage; }

    /** Overdue when the next service date has passed or the vehicle has exceeded next service mileage. */
    public boolean isOverdue(LocalDate today, Long currentMileage) {
        if (status == Status.CANCELLED) return false;
        if (nextServiceDate != null && nextServiceDate.isBefore(today)) return true;
        return nextServiceMileage != null && currentMileage != null && currentMileage > nextServiceMileage;
    }

    public boolean isDueSoon(LocalDate today, Long currentMileage) {
        if (status == Status.CANCELLED || isOverdue(today, currentMileage)) return false;
        if (nextServiceDate != null
                && ChronoUnit.DAYS.between(today, nextServiceDate) <= AppConfig.MAINTENANCE_DUE_WARNING_DAYS) return true;
        return nextServiceMileage != null && currentMileage != null
                && nextServiceMileage - currentMileage <= AppConfig.MAINTENANCE_DUE_WARNING_KM;
    }

    public String getDueStatus(LocalDate today, Long currentMileage) {
        if (isOverdue(today, currentMileage)) return "OVERDUE";
        if (isDueSoon(today, currentMileage)) return "DUE SOON";
        return "";
    }
}
