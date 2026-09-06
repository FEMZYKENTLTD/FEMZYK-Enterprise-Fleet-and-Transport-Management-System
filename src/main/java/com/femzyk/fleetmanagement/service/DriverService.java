package com.femzyk.fleetmanagement.service;

import com.femzyk.fleetmanagement.audit.AuditService;
import com.femzyk.fleetmanagement.config.AppConfig;
import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.exception.BusinessRuleException;
import com.femzyk.fleetmanagement.exception.DuplicateRecordException;
import com.femzyk.fleetmanagement.exception.RecordNotFoundException;
import com.femzyk.fleetmanagement.model.Driver;
import com.femzyk.fleetmanagement.model.DriverStatus;
import com.femzyk.fleetmanagement.model.Employee;
import com.femzyk.fleetmanagement.model.Permission;
import com.femzyk.fleetmanagement.repository.AssignmentRepository;
import com.femzyk.fleetmanagement.repository.CodeSequenceRepository;
import com.femzyk.fleetmanagement.repository.DriverRepository;
import com.femzyk.fleetmanagement.repository.EmployeeRepository;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.validation.Validators;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class DriverService {

    public static final String CODE_PREFIX = "DRV";

    private final DatabaseManager db;
    private final DriverRepository drivers;
    private final EmployeeRepository employees;
    private final AssignmentRepository assignments;
    private final CodeSequenceRepository codes;
    private final AuditService audit;

    public DriverService(DatabaseManager db, DriverRepository drivers, EmployeeRepository employees,
                         AssignmentRepository assignments, CodeSequenceRepository codes, AuditService audit) {
        this.db = db;
        this.drivers = drivers;
        this.employees = employees;
        this.assignments = assignments;
        this.codes = codes;
        this.audit = audit;
    }

    public List<Driver> findAll() {
        SessionContext.require(Permission.VIEW_RECORDS);
        return drivers.findAll();
    }

    public List<Driver> search(String text, DriverStatus status) {
        SessionContext.require(Permission.VIEW_RECORDS);
        return drivers.search(text, status);
    }

    public Optional<Driver> findById(long id) { return drivers.findById(id); }

    public Driver getById(long id) {
        return drivers.findById(id).orElseThrow(() -> new RecordNotFoundException("Driver", id));
    }

    public List<Driver> recycleBin() {
        SessionContext.require(Permission.MANAGE_DRIVERS);
        return drivers.findDeleted();
    }

    /** Employees that do not yet have a driver profile (candidates for the "new driver" form). */
    public List<Employee> employeesWithoutDriverProfile() {
        return employees.findAll(false).stream().filter(e -> !e.isDriver()).toList();
    }

    public Driver create(Driver d) {
        SessionContext.require(Permission.MANAGE_DRIVERS);
        validate(d);
        Employee emp = employees.findById(d.getEmployeeId()).orElseThrow(() -> new RecordNotFoundException("Employee", d.getEmployeeId()));
        if (emp.isDeleted()) throw new BusinessRuleException("Cannot create a driver profile for a deleted employee.");
        if (drivers.findByEmployeeId(emp.getId()).isPresent()) throw new DuplicateRecordException(emp.getFullName() + " already has a driver profile.");
        drivers.findByLicence(d.getLicenceNumber().trim()).ifPresent(o -> { throw new DuplicateRecordException("Licence number " + d.getLicenceNumber() + " is already registered to " + o.getFullName() + "."); });
        return db.inTransaction(c -> {
            if (Validators.isBlank(d.getDriverCode())) d.setDriverCode(codes.next(CODE_PREFIX));
            d.setLicenceNumber(d.getLicenceNumber().trim().toUpperCase());
            drivers.save(d);
            audit.record("CREATE", "DRIVER", d.getDriverCode(), "Created driver profile for " + emp.getFullName() + ", licence " + d.getLicenceNumber());
            return drivers.findById(d.getId()).orElse(d);
        });
    }

    public Driver update(Driver d) {
        SessionContext.require(Permission.MANAGE_DRIVERS);
        if (d.isNew()) throw new BusinessRuleException("Cannot update an unsaved driver.");
        validate(d);
        Driver existing = getById(d.getId());
        drivers.findByLicence(d.getLicenceNumber().trim()).filter(o -> !o.getId().equals(d.getId()))
                .ifPresent(o -> { throw new DuplicateRecordException("Licence number " + d.getLicenceNumber() + " is already registered to " + o.getFullName() + "."); });
        if (!d.getStatus().canBeAssigned() && assignments.findActiveByDriver(d.getId()).isPresent()) {
            throw new BusinessRuleException("Driver has an active vehicle assignment. Return the vehicle before changing status to " + d.getStatus() + ".");
        }
        d.setEmployeeId(existing.getEmployeeId()); // the employee link is immutable
        d.setDriverCode(existing.getDriverCode());
        d.setLicenceNumber(d.getLicenceNumber().trim().toUpperCase());
        drivers.save(d);
        audit.record("UPDATE", "DRIVER", d.getDriverCode(), "Updated driver " + existing.getFullName() + " (status " + d.getStatus() + ")");
        return drivers.findById(d.getId()).orElse(d);
    }

    public void delete(long id) {
        SessionContext.require(Permission.MANAGE_DRIVERS);
        Driver d = getById(id);
        if (assignments.findActiveByDriver(id).isPresent()) {
            throw new BusinessRuleException("Driver " + d.getFullName() + " has an active vehicle assignment. Return the vehicle first.");
        }
        drivers.softDelete(id);
        audit.record("DELETE", "DRIVER", d.getDriverCode(), "Moved driver " + d.getFullName() + " to recycle bin");
    }

    public void restore(long id) {
        SessionContext.require(Permission.MANAGE_DRIVERS);
        Driver d = getById(id);
        drivers.restore(id);
        audit.record("RESTORE", "DRIVER", d.getDriverCode(), "Restored driver " + d.getFullName());
    }

    public List<Driver> licenceAlerts() {
        return drivers.findLicenceExpiringBefore(LocalDate.now().plusDays(AppConfig.LICENCE_EXPIRY_WARNING_DAYS));
    }

    public long countExpiredLicences() {
        return drivers.findLicenceExpiringBefore(LocalDate.now().minusDays(1)).size();
    }

    void validate(Driver d) {
        Validators.Errors errors = Validators.errors()
                .required(d.getEmployeeId(), "Employee")
                .required(d.getLicenceNumber(), "Licence number")
                .required(d.getLicenceCategory(), "Licence category")
                .required(d.getLicenceExpiryDate(), "Licence expiry date")
                .required(d.getStatus(), "Status")
                .maxLength(d.getNotes(), 2000, "Notes");
        if (d.getLicenceNumber() != null) {
            errors.check(d.getLicenceNumber().trim().length() >= 5, "Licence number must be at least 5 characters.");
        }
        if (d.getLicenceIssueDate() != null && d.getLicenceExpiryDate() != null) {
            errors.check(d.getLicenceExpiryDate().isAfter(d.getLicenceIssueDate()), "Licence expiry date must be after the issue date.");
        }
        errors.notFuture(d.getLicenceIssueDate(), "Licence issue date");
        if (d.getLicenceExpiryDate() != null && d.getLicenceExpiryDate().isBefore(LocalDate.now()) && d.getStatus() == DriverStatus.ACTIVE && d.isNew()) {
            errors.add("Licence has already expired; a new driver cannot be created as ACTIVE with an expired licence.");
        }
        errors.throwIfAny();
    }
}
