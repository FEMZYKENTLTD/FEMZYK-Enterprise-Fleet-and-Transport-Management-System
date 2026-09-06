package com.femzyk.fleetmanagement.service;

import com.femzyk.fleetmanagement.audit.AuditService;
import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.exception.BusinessRuleException;
import com.femzyk.fleetmanagement.exception.RecordNotFoundException;
import com.femzyk.fleetmanagement.model.*;
import com.femzyk.fleetmanagement.repository.ExpenseRepository;
import com.femzyk.fleetmanagement.repository.MaintenanceRepository;
import com.femzyk.fleetmanagement.repository.VehicleRepository;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.validation.Validators;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Maintenance tracking. Completed maintenance with a cost automatically creates a linked MAINTENANCE
 * expense so operational cost reports are complete without double entry.
 */
public class MaintenanceService {

    private final DatabaseManager db;
    private final MaintenanceRepository maintenance;
    private final VehicleRepository vehicles;
    private final ExpenseRepository expenses;
    private final AuditService audit;

    public MaintenanceService(DatabaseManager db, MaintenanceRepository maintenance, VehicleRepository vehicles,
                              ExpenseRepository expenses, AuditService audit) {
        this.db = db;
        this.maintenance = maintenance;
        this.vehicles = vehicles;
        this.expenses = expenses;
        this.audit = audit;
    }

    public List<MaintenanceRecord> findAll() {
        SessionContext.require(Permission.VIEW_RECORDS);
        return maintenance.findAll();
    }

    public List<MaintenanceRecord> search(String text, MaintenanceRecord.Type type, MaintenanceRecord.Status status,
                                          Long vehicleId, LocalDate from, LocalDate to) {
        SessionContext.require(Permission.VIEW_RECORDS);
        return maintenance.search(text, type, status, vehicleId, from, to);
    }

    public Optional<MaintenanceRecord> findById(long id) { return maintenance.findById(id); }

    public MaintenanceRecord getById(long id) {
        return maintenance.findById(id).orElseThrow(() -> new RecordNotFoundException("Maintenance record", id));
    }

    public MaintenanceRecord create(MaintenanceRecord m) {
        SessionContext.require(Permission.MANAGE_MAINTENANCE);
        validate(m);
        Vehicle v = vehicles.findById(m.getVehicleId()).orElseThrow(() -> new RecordNotFoundException("Vehicle", m.getVehicleId()));
        return db.inTransaction(c -> {
            maintenance.save(m);
            syncExpense(m, v);
            if (m.getMileageAtService() != null) vehicles.updateMileageIfGreater(v.getId(), m.getMileageAtService());
            if (m.getStatus() == MaintenanceRecord.Status.IN_PROGRESS && v.getStatus().canTransitionTo(VehicleStatus.MAINTENANCE)
                    && v.getStatus() != VehicleStatus.IN_SERVICE) {
                vehicles.updateStatus(v.getId(), VehicleStatus.MAINTENANCE);
            }
            audit.record("CREATE", "MAINTENANCE", v.getRegistrationNumber(), m.getType() + ": " + m.getDescription() + " (" + m.getStatus() + ")");
            return maintenance.findById(m.getId()).orElse(m);
        });
    }

    public MaintenanceRecord update(MaintenanceRecord m) {
        SessionContext.require(Permission.MANAGE_MAINTENANCE);
        validate(m);
        MaintenanceRecord existing = getById(m.getId());
        Vehicle v = vehicles.findById(m.getVehicleId()).orElseThrow(() -> new RecordNotFoundException("Vehicle", m.getVehicleId()));
        return db.inTransaction(c -> {
            maintenance.save(m);
            syncExpense(m, v);
            if (m.getMileageAtService() != null) vehicles.updateMileageIfGreater(v.getId(), m.getMileageAtService());
            // Vehicle status follows the maintenance job
            if (existing.getStatus() != m.getStatus()) {
                Vehicle fresh = vehicles.findById(v.getId()).orElseThrow();
                if (m.getStatus() == MaintenanceRecord.Status.IN_PROGRESS && fresh.getStatus() != VehicleStatus.MAINTENANCE
                        && fresh.getStatus().canTransitionTo(VehicleStatus.MAINTENANCE) && fresh.getStatus() != VehicleStatus.IN_SERVICE) {
                    vehicles.updateStatus(v.getId(), VehicleStatus.MAINTENANCE);
                } else if ((m.getStatus() == MaintenanceRecord.Status.COMPLETED || m.getStatus() == MaintenanceRecord.Status.CANCELLED)
                        && fresh.getStatus() == VehicleStatus.MAINTENANCE) {
                    boolean assigned = fresh.getAssignedDriverId() != null;
                    vehicles.updateStatus(v.getId(), assigned ? VehicleStatus.ASSIGNED : VehicleStatus.AVAILABLE);
                }
            }
            audit.record("UPDATE", "MAINTENANCE", v.getRegistrationNumber(), m.getType() + ": " + m.getDescription() + " (" + m.getStatus() + ")");
            return maintenance.findById(m.getId()).orElse(m);
        });
    }

    public void delete(long id) {
        SessionContext.require(Permission.MANAGE_MAINTENANCE);
        MaintenanceRecord m = getById(id);
        db.inTransaction(c -> {
            expenses.findByMaintenanceRecord(id).ifPresent(x -> expenses.delete(x.getId()));
            maintenance.delete(id);
            audit.record("DELETE", "MAINTENANCE", m.getVehicleDisplay(), "Deleted maintenance record: " + m.getDescription());
        });
    }

    /** Keeps the linked expense in step with the maintenance record (create / update / remove). */
    private void syncExpense(MaintenanceRecord m, Vehicle v) {
        Optional<Expense> linked = expenses.findByMaintenanceRecord(m.getId());
        boolean shouldExist = m.getCost() > 0 && m.getStatus() != MaintenanceRecord.Status.CANCELLED;
        if (shouldExist) {
            Expense x = linked.orElseGet(Expense::new);
            x.setVehicleId(v.getId());
            x.setDate(m.getServiceDate());
            x.setCategory(m.getType() == MaintenanceRecord.Type.REPAIR ? ExpenseCategory.REPAIRS : ExpenseCategory.MAINTENANCE);
            x.setAmount(m.getCost());
            x.setVendor(m.getProvider());
            x.setDescription(m.getType().name().replace('_', ' ') + " - " + m.getDescription());
            x.setSourceMaintenanceRecordId(m.getId());
            expenses.save(x);
        } else {
            linked.ifPresent(x -> expenses.delete(x.getId()));
        }
    }

    // ---- due / overdue -------------------------------------------------------------------------

    public record DueItem(MaintenanceRecord record, String dueStatus) {}

    public List<DueItem> dueAndOverdue() {
        LocalDate today = LocalDate.now();
        return maintenance.findLatestWithNextService().stream()
                .map(m -> new DueItem(m, m.getDueStatus(today, m.getVehicleCurrentMileage())))
                .filter(d -> !d.dueStatus().isEmpty())
                .toList();
    }

    public long countOverdue() {
        return dueAndOverdue().stream().filter(d -> "OVERDUE".equals(d.dueStatus())).count();
    }

    public double totalCost(LocalDate from, LocalDate to) { return maintenance.totalCost(from, to); }
    public Map<String, Double> costByVehicle(LocalDate from, LocalDate to) { return maintenance.costByVehicle(from, to); }

    void validate(MaintenanceRecord m) {
        Validators.Errors errors = Validators.errors()
                .required(m.getVehicleId(), "Vehicle")
                .required(m.getServiceDate(), "Service date")
                .required(m.getType(), "Maintenance type")
                .required(m.getDescription(), "Description")
                .required(m.getStatus(), "Status")
                .nonNegative(m.getCost(), "Cost")
                .nonNegative(m.getMileageAtService(), "Mileage at service")
                .nonNegative(m.getNextServiceMileage(), "Next service mileage")
                .maxLength(m.getNotes(), 2000, "Notes");
        if (m.getServiceDate() != null && m.getServiceDate().isAfter(LocalDate.now().plusYears(1))) {
            errors.add("Service date cannot be more than a year in the future.");
        }
        if (m.getServiceDate() != null && m.getNextServiceDate() != null) {
            errors.check(m.getNextServiceDate().isAfter(m.getServiceDate()), "Next service date must be after the service date.");
        }
        if (m.getMileageAtService() != null && m.getNextServiceMileage() != null) {
            errors.check(m.getNextServiceMileage() > m.getMileageAtService(), "Next service mileage must be greater than mileage at service.");
        }
        if (m.getStatus() == MaintenanceRecord.Status.COMPLETED && m.getServiceDate() != null) {
            errors.notFuture(m.getServiceDate(), "Service date of a completed job");
        }
        errors.throwIfAny();
    }
}
