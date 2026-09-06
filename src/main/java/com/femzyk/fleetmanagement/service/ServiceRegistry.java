package com.femzyk.fleetmanagement.service;

import com.femzyk.fleetmanagement.audit.AuditService;
import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.io.BackupService;
import com.femzyk.fleetmanagement.io.CsvImportExportService;
import com.femzyk.fleetmanagement.legacy.LegacyDataMigrator;
import com.femzyk.fleetmanagement.reporting.ReportService;
import com.femzyk.fleetmanagement.repository.*;

/**
 * Composition root: wires repositories and services over one DatabaseManager.
 * Manual dependency injection keeps the application free of frameworks and easy to test.
 */
public final class ServiceRegistry {

    private final DatabaseManager db;

    public final UserRepository userRepository;
    public final EmployeeRepository employeeRepository;
    public final DriverRepository driverRepository;
    public final VehicleRepository vehicleRepository;
    public final AssignmentRepository assignmentRepository;
    public final TripRepository tripRepository;
    public final MaintenanceRepository maintenanceRepository;
    public final FuelRepository fuelRepository;
    public final ExpenseRepository expenseRepository;
    public final AuditLogRepository auditLogRepository;
    public final CodeSequenceRepository codeSequenceRepository;
    public final SettingsRepository settingsRepository;

    public final AuditService audit;
    public final AuthService auth;
    public final EmployeeService employees;
    public final DriverService drivers;
    public final VehicleService vehicles;
    public final AssignmentService assignments;
    public final TripService trips;
    public final MaintenanceService maintenance;
    public final FuelService fuel;
    public final ExpenseService expenses;
    public final DashboardService dashboard;
    public final ReportService reports;
    public final BackupService backup;
    public final CsvImportExportService csv;
    public final DemoDataService demoData;
    public final LegacyDataMigrator legacyMigrator;

    public ServiceRegistry(DatabaseManager db) {
        this.db = db;
        userRepository = new UserRepository(db);
        employeeRepository = new EmployeeRepository(db);
        driverRepository = new DriverRepository(db);
        vehicleRepository = new VehicleRepository(db);
        assignmentRepository = new AssignmentRepository(db);
        tripRepository = new TripRepository(db);
        maintenanceRepository = new MaintenanceRepository(db);
        fuelRepository = new FuelRepository(db);
        expenseRepository = new ExpenseRepository(db);
        auditLogRepository = new AuditLogRepository(db);
        codeSequenceRepository = new CodeSequenceRepository(db);
        settingsRepository = new SettingsRepository(db);

        audit = new AuditService(auditLogRepository);
        auth = new AuthService(userRepository, audit);
        employees = new EmployeeService(db, employeeRepository, driverRepository, codeSequenceRepository, audit);
        drivers = new DriverService(db, driverRepository, employeeRepository, assignmentRepository, codeSequenceRepository, audit);
        vehicles = new VehicleService(db, vehicleRepository, assignmentRepository, tripRepository, codeSequenceRepository, audit);
        assignments = new AssignmentService(db, assignmentRepository, vehicleRepository, driverRepository, audit);
        trips = new TripService(db, tripRepository, vehicleRepository, driverRepository, assignmentRepository, codeSequenceRepository, audit);
        maintenance = new MaintenanceService(db, maintenanceRepository, vehicleRepository, expenseRepository, audit);
        fuel = new FuelService(db, fuelRepository, vehicleRepository, expenseRepository, audit);
        expenses = new ExpenseService(expenseRepository, vehicleRepository, audit);
        dashboard = new DashboardService(vehicleRepository, employeeRepository, driverRepository, tripRepository, assignmentRepository,
                maintenance, drivers, fuelRepository, maintenanceRepository, expenseRepository);
        reports = new ReportService(this);
        backup = new BackupService(db, audit);
        csv = new CsvImportExportService(this);
        demoData = new DemoDataService(this, db);
        legacyMigrator = new LegacyDataMigrator(this, db);
    }

    public DatabaseManager database() {
        return db;
    }

    /** First-run initialisation: schema is already applied by DatabaseManager; ensure admin exists. */
    public void bootstrap() {
        auth.ensureDefaultAdministrator();
    }
}
