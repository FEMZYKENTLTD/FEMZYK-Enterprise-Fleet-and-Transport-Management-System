package com.femzyk.fleetmanagement.model;

import java.time.LocalDate;

/**
 * Company personnel record. Integrated and expanded from the Employee Management System
 * (which held name, age, department, salary) into a full HR-style record.
 */
public class Employee extends BaseEntity {

    private String employeeCode;      // EMP-0001, unique
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String address;
    private String department;
    private String position;
    private LocalDate dateOfBirth;
    private LocalDate hireDate;
    private Double salary;
    private EmploymentStatus status = EmploymentStatus.ACTIVE;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private boolean driver;           // convenience flag: has a Driver profile
    private String notes;

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public Double getSalary() { return salary; }
    public void setSalary(Double salary) { this.salary = salary; }
    public EmploymentStatus getStatus() { return status; }
    public void setStatus(EmploymentStatus status) { this.status = status; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }
    public boolean isDriver() { return driver; }
    public void setDriver(boolean driver) { this.driver = driver; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getFullName() {
        return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
    }

    public int getAge() {
        return dateOfBirth == null ? 0 : java.time.Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    @Override public String toString() { return employeeCode + " - " + getFullName(); }
}
