package com.femzyk.fleetmanagement.service;

import com.femzyk.fleetmanagement.audit.AuditService;
import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.exception.BusinessRuleException;
import com.femzyk.fleetmanagement.exception.DuplicateRecordException;
import com.femzyk.fleetmanagement.exception.RecordNotFoundException;
import com.femzyk.fleetmanagement.model.Employee;
import com.femzyk.fleetmanagement.model.EmploymentStatus;
import com.femzyk.fleetmanagement.model.Permission;
import com.femzyk.fleetmanagement.repository.CodeSequenceRepository;
import com.femzyk.fleetmanagement.repository.DriverRepository;
import com.femzyk.fleetmanagement.repository.EmployeeRepository;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.validation.Validators;

import java.time.LocalDate;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Employee management. Integrated from the Employee Management System: CRUD, department grouping,
 * salary statistics (Stream API) and filtering, now with validation, audit and soft delete.
 */
public class EmployeeService {

    public static final String CODE_PREFIX = "EMP";

    private final DatabaseManager db;
    private final EmployeeRepository employees;
    private final DriverRepository drivers;
    private final CodeSequenceRepository codes;
    private final AuditService audit;

    public EmployeeService(DatabaseManager db, EmployeeRepository employees, DriverRepository drivers,
                           CodeSequenceRepository codes, AuditService audit) {
        this.db = db;
        this.employees = employees;
        this.drivers = drivers;
        this.codes = codes;
        this.audit = audit;
    }

    public List<Employee> findAll() {
        SessionContext.require(Permission.VIEW_RECORDS);
        return employees.findAll(false);
    }

    public List<Employee> search(String text, String department, EmploymentStatus status) {
        SessionContext.require(Permission.VIEW_RECORDS);
        return employees.search(text, department, status);
    }

    public Optional<Employee> findById(long id) {
        return employees.findById(id);
    }

    public Employee getById(long id) {
        return employees.findById(id).orElseThrow(() -> new RecordNotFoundException("Employee", id));
    }

    public List<String> departments() {
        return employees.distinctDepartments();
    }

    public List<Employee> recycleBin() {
        SessionContext.require(Permission.MANAGE_EMPLOYEES);
        return employees.findDeleted();
    }

    public Employee create(Employee e) {
        SessionContext.require(Permission.MANAGE_EMPLOYEES);
        validate(e);
        return db.inTransaction(c -> {
            if (Validators.isBlank(e.getEmployeeCode())) {
                e.setEmployeeCode(codes.next(CODE_PREFIX));
            } else if (employees.findByCode(e.getEmployeeCode().trim()).isPresent()) {
                throw new DuplicateRecordException("An employee with code " + e.getEmployeeCode() + " already exists.");
            }
            normalise(e);
            employees.save(e);
            audit.record("CREATE", "EMPLOYEE", e.getEmployeeCode(), "Created employee " + e.getFullName() + " (" + e.getDepartment() + ")");
            return e;
        });
    }

    public Employee update(Employee e) {
        SessionContext.require(Permission.MANAGE_EMPLOYEES);
        if (e.isNew()) throw new BusinessRuleException("Cannot update an unsaved employee.");
        validate(e);
        Employee existing = getById(e.getId());
        employees.findByCode(e.getEmployeeCode().trim())
                .filter(other -> !other.getId().equals(e.getId()))
                .ifPresent(other -> { throw new DuplicateRecordException("An employee with code " + e.getEmployeeCode() + " already exists."); });
        normalise(e);
        return db.inTransaction(c -> {
            employees.save(e);
            if (existing.getStatus() != e.getStatus() && !e.getStatus().isWorking()) {
                // Cascade: a non-working employee cannot remain an ACTIVE driver
                drivers.findByEmployeeId(e.getId()).ifPresent(d -> {
                    if (d.getStatus().canBeAssigned()) {
                        d.setStatus(com.femzyk.fleetmanagement.model.DriverStatus.INACTIVE);
                        drivers.save(d);
                        audit.record("UPDATE", "DRIVER", d.getDriverCode(), "Driver set INACTIVE because employee status became " + e.getStatus());
                    }
                });
            }
            audit.record("UPDATE", "EMPLOYEE", e.getEmployeeCode(), "Updated employee " + e.getFullName());
            return e;
        });
    }

    /** Soft delete (moves to recycle bin). Blocked while the employee is a driver with an active assignment. */
    public void delete(long id) {
        SessionContext.require(Permission.MANAGE_EMPLOYEES);
        Employee e = getById(id);
        drivers.findByEmployeeId(id).ifPresent(d -> {
            if (d.getAssignedVehicle() != null) {
                throw new BusinessRuleException("Employee " + e.getFullName() + " is a driver with an active vehicle assignment. Return the vehicle first.");
            }
        });
        db.inTransaction(c -> {
            drivers.findByEmployeeId(id).ifPresent(d -> drivers.softDelete(d.getId()));
            employees.softDelete(id);
            audit.record("DELETE", "EMPLOYEE", e.getEmployeeCode(), "Moved employee " + e.getFullName() + " to recycle bin");
        });
    }

    public void restore(long id) {
        SessionContext.require(Permission.MANAGE_EMPLOYEES);
        Employee e = getById(id);
        db.inTransaction(c -> {
            employees.restore(id);
            drivers.findByEmployeeId(id).ifPresent(d -> drivers.restore(d.getId()));
            audit.record("RESTORE", "EMPLOYEE", e.getEmployeeCode(), "Restored employee " + e.getFullName());
        });
    }

    public void deletePermanently(long id) {
        SessionContext.require(Permission.MANAGE_EMPLOYEES);
        Employee e = getById(id);
        if (!e.isDeleted()) throw new BusinessRuleException("Only records in the recycle bin can be permanently deleted.");
        if (drivers.findByEmployeeId(id).isPresent()) {
            throw new BusinessRuleException("This employee has a driver history (trips/assignments) and cannot be permanently deleted. It remains in the recycle bin.");
        }
        employees.hardDelete(id);
        audit.record("PURGE", "EMPLOYEE", e.getEmployeeCode(), "Permanently deleted employee " + e.getFullName());
    }

    // ---- statistics (Stream API, carried over from the Employee Management System) -------------

    public EmployeeStatistics statistics() {
        List<Employee> all = employees.findAll(false);
        DoubleSummaryStatistics salary = all.stream().filter(e -> e.getSalary() != null)
                .mapToDouble(Employee::getSalary).summaryStatistics();
        Map<String, Long> byDepartment = all.stream()
                .collect(Collectors.groupingBy(Employee::getDepartment, java.util.TreeMap::new, Collectors.counting()));
        Map<String, Double> avgSalaryByDepartment = all.stream().filter(e -> e.getSalary() != null)
                .collect(Collectors.groupingBy(Employee::getDepartment, java.util.TreeMap::new, Collectors.averagingDouble(Employee::getSalary)));
        Map<EmploymentStatus, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(Employee::getStatus, java.util.TreeMap::new, Collectors.counting()));
        Optional<Employee> topEarner = all.stream().filter(e -> e.getSalary() != null)
                .max(java.util.Comparator.comparingDouble(Employee::getSalary));
        long driverCount = all.stream().filter(Employee::isDriver).count();
        return new EmployeeStatistics(all.size(), salary, byDepartment, avgSalaryByDepartment, byStatus, topEarner.orElse(null), driverCount);
    }

    public record EmployeeStatistics(int total, DoubleSummaryStatistics salary, Map<String, Long> byDepartment,
                                     Map<String, Double> averageSalaryByDepartment, Map<EmploymentStatus, Long> byStatus,
                                     Employee topEarner, long drivers) {
        public double totalMonthlyPayroll() { return salary.getSum(); }
    }

    // ---- validation ---------------------------------------------------------------------------

    void validate(Employee e) {
        Validators.Errors errors = Validators.errors()
                .required(e.getFirstName(), "First name")
                .required(e.getLastName(), "Last name")
                .required(e.getDepartment(), "Department")
                .required(e.getHireDate(), "Hire date")
                .phone(e.getPhone(), "Phone", false)
                .email(e.getEmail(), "E-mail", false)
                .phone(e.getEmergencyContactPhone(), "Emergency contact phone", false)
                .nonNegative(e.getSalary(), "Salary")
                .notFuture(e.getHireDate(), "Hire date")
                .maxLength(e.getNotes(), 2000, "Notes");
        if (e.getDateOfBirth() != null) {
            errors.check(!e.getDateOfBirth().isAfter(LocalDate.now().minusYears(16)), "Employee must be at least 16 years old.");
            if (e.getHireDate() != null) errors.check(e.getHireDate().isAfter(e.getDateOfBirth()), "Hire date must be after date of birth.");
        }
        errors.throwIfAny();
        if (!Validators.isBlank(e.getEmail())) {
            employees.findByEmail(e.getEmail().trim()).filter(o -> !o.getId().equals(e.getId()))
                    .ifPresent(o -> { throw new DuplicateRecordException("E-mail " + e.getEmail() + " is already used by " + o.getFullName() + "."); });
        }
    }

    private static void normalise(Employee e) {
        e.setEmployeeCode(e.getEmployeeCode().trim().toUpperCase());
        e.setFirstName(e.getFirstName().trim());
        e.setLastName(e.getLastName().trim());
        e.setDepartment(e.getDepartment().trim());
        if (e.getEmail() != null) e.setEmail(e.getEmail().trim().toLowerCase());
        if (e.getPhone() != null) e.setPhone(e.getPhone().trim());
    }
}
