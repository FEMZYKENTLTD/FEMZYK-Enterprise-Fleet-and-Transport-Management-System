package com.femzyk.fleetmanagement.model;

import com.femzyk.fleetmanagement.config.AppConfig;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Driver profile. Always linked to an Employee (composition: a driver is an employee with a licence). */
public class Driver extends BaseEntity {

    private String driverCode;        // DRV-0001, unique
    private Long employeeId;
    private String licenceNumber;     // unique
    private LicenceCategory licenceCategory = LicenceCategory.B;
    private LocalDate licenceIssueDate;
    private LocalDate licenceExpiryDate;
    private DriverStatus status = DriverStatus.ACTIVE;
    private String notes;

    // Denormalised for display (populated by repository joins, not persisted on drivers table)
    private String fullName;
    private String phone;
    private String address;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String assignedVehicle;

    public String getDriverCode() { return driverCode; }
    public void setDriverCode(String driverCode) { this.driverCode = driverCode; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getLicenceNumber() { return licenceNumber; }
    public void setLicenceNumber(String licenceNumber) { this.licenceNumber = licenceNumber; }
    public LicenceCategory getLicenceCategory() { return licenceCategory; }
    public void setLicenceCategory(LicenceCategory licenceCategory) { this.licenceCategory = licenceCategory; }
    public LocalDate getLicenceIssueDate() { return licenceIssueDate; }
    public void setLicenceIssueDate(LocalDate licenceIssueDate) { this.licenceIssueDate = licenceIssueDate; }
    public LocalDate getLicenceExpiryDate() { return licenceExpiryDate; }
    public void setLicenceExpiryDate(LocalDate licenceExpiryDate) { this.licenceExpiryDate = licenceExpiryDate; }
    public DriverStatus getStatus() { return status; }
    public void setStatus(DriverStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String v) { this.emergencyContactName = v; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String v) { this.emergencyContactPhone = v; }
    public String getAssignedVehicle() { return assignedVehicle; }
    public void setAssignedVehicle(String assignedVehicle) { this.assignedVehicle = assignedVehicle; }

    public boolean isLicenceExpired() {
        return licenceExpiryDate != null && licenceExpiryDate.isBefore(LocalDate.now());
    }

    public boolean isLicenceExpiringSoon() {
        if (licenceExpiryDate == null || isLicenceExpired()) return false;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), licenceExpiryDate);
        return days <= AppConfig.LICENCE_EXPIRY_WARNING_DAYS;
    }

    public String getLicenceWarning() {
        if (isLicenceExpired()) return "EXPIRED";
        if (isLicenceExpiringSoon()) return "Expires in " + ChronoUnit.DAYS.between(LocalDate.now(), licenceExpiryDate) + " days";
        return "";
    }

    @Override public String toString() { return driverCode + " - " + (fullName == null ? "" : fullName); }
}
