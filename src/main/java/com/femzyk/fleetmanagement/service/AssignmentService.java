package com.femzyk.fleetmanagement.service;

import com.femzyk.fleetmanagement.audit.AuditService;
import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.exception.BusinessRuleException;
import com.femzyk.fleetmanagement.exception.RecordNotFoundException;
import com.femzyk.fleetmanagement.model.*;
import com.femzyk.fleetmanagement.repository.AssignmentRepository;
import com.femzyk.fleetmanagement.repository.DriverRepository;
import com.femzyk.fleetmanagement.repository.VehicleRepository;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.validation.Validators;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Vehicle ↔ driver assignment with conflict prevention. Business rules are enforced here and backed by
 * partial unique indexes in the database so no code path can create two active assignments.
 */
public class AssignmentService {

    private final DatabaseManager db;
    private final AssignmentRepository assignments;
    private final VehicleRepository vehicles;
    private final DriverRepository drivers;
    private final AuditService audit;

    public AssignmentService(DatabaseManager db, AssignmentRepository assignments, VehicleRepository vehicles,
                             DriverRepository drivers, AuditService audit) {
        this.db = db;
        this.assignments = assignments;
        this.vehicles = vehicles;
        this.drivers = drivers;
        this.audit = audit;
    }

    public List<Assignment> findAll() {
        SessionContext.require(Permission.VIEW_RECORDS);
        return assignments.findAll();
    }

    public List<Assignment> search(String text, Assignment.Status status) {
        SessionContext.require(Permission.VIEW_RECORDS);
        return assignments.search(text, status);
    }

    public List<Assignment> historyForVehicle(long vehicleId) { return assignments.historyForVehicle(vehicleId); }
    public List<Assignment> historyForDriver(long driverId) { return assignments.historyForDriver(driverId); }
    public long countActive() { return assignments.countActive(); }

    public Assignment assign(long vehicleId, long driverId, String purpose, String notes) {
        SessionContext.require(Permission.MANAGE_ASSIGNMENTS);
        Vehicle vehicle = vehicles.findById(vehicleId).orElseThrow(() -> new RecordNotFoundException("Vehicle", vehicleId));
        Driver driver = drivers.findById(driverId).orElseThrow(() -> new RecordNotFoundException("Driver", driverId));

        if (vehicle.isDeleted()) throw new BusinessRuleException("Vehicle is in the recycle bin.");
        if (driver.isDeleted()) throw new BusinessRuleException("Driver is in the recycle bin.");
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new BusinessRuleException("Vehicle " + vehicle.getRegistrationNumber() + " is " + vehicle.getStatus() + " and cannot be assigned. Only AVAILABLE vehicles can be assigned.");
        }
        if (!driver.getStatus().canBeAssigned()) {
            throw new BusinessRuleException("Driver " + driver.getFullName() + " is " + driver.getStatus() + " and cannot be assigned a vehicle.");
        }
        if (driver.isLicenceExpired()) {
            throw new BusinessRuleException("Driver " + driver.getFullName() + "'s licence expired on " + driver.getLicenceExpiryDate() + ".");
        }
        if (!driver.getLicenceCategory().permits(vehicle.getVehicleType())) {
            throw new BusinessRuleException("Licence class " + driver.getLicenceCategory() + " (" + driver.getLicenceCategory().getDescription()
                    + ") does not permit driving a " + vehicle.getVehicleType() + ".");
        }
        assignments.findActiveByVehicle(vehicleId).ifPresent(a -> {
            throw new BusinessRuleException("Vehicle is already assigned to " + a.getDriverDisplay() + ".");
        });
        assignments.findActiveByDriver(driverId).ifPresent(a -> {
            throw new BusinessRuleException("Driver already has vehicle " + a.getVehicleDisplay() + " assigned.");
        });

        return db.inTransaction(c -> {
            Assignment a = new Assignment();
            a.setVehicleId(vehicleId);
            a.setDriverId(driverId);
            a.setAssignedAt(LocalDateTime.now());
            a.setStatus(Assignment.Status.ACTIVE);
            a.setPurpose(Validators.isBlank(purpose) ? null : purpose.trim());
            a.setNotes(Validators.isBlank(notes) ? null : notes.trim());
            a.setAssignedBy(SessionContext.currentUsername());
            assignments.save(a);
            vehicles.updateStatus(vehicleId, VehicleStatus.ASSIGNED);
            audit.record("ASSIGN", "ASSIGNMENT", vehicle.getRegistrationNumber(),
                    "Assigned " + vehicle.getRegistrationNumber() + " to " + driver.getFullName() + (a.getPurpose() == null ? "" : " for " + a.getPurpose()));
            return assignments.findById(a.getId()).orElse(a);
        });
    }

    /** Ends an active assignment; the vehicle becomes AVAILABLE again. */
    public Assignment returnVehicle(long assignmentId, String notes) {
        SessionContext.require(Permission.MANAGE_ASSIGNMENTS);
        Assignment a = assignments.findById(assignmentId).orElseThrow(() -> new RecordNotFoundException("Assignment", assignmentId));
        if (a.getStatus() != Assignment.Status.ACTIVE) throw new BusinessRuleException("This assignment is already " + a.getStatus() + ".");
        return db.inTransaction(c -> {
            a.setStatus(Assignment.Status.RETURNED);
            a.setReturnedAt(LocalDateTime.now());
            if (!Validators.isBlank(notes)) a.setNotes((a.getNotes() == null ? "" : a.getNotes() + "\n") + notes.trim());
            assignments.save(a);
            Vehicle v = vehicles.findById(a.getVehicleId()).orElseThrow();
            if (v.getStatus() == VehicleStatus.ASSIGNED || v.getStatus() == VehicleStatus.IN_SERVICE) {
                vehicles.updateStatus(v.getId(), VehicleStatus.AVAILABLE);
            }
            audit.record("RETURN", "ASSIGNMENT", v.getRegistrationNumber(), "Vehicle returned by " + a.getDriverDisplay());
            return a;
        });
    }

    /** Re-assigns a vehicle to another driver in one transaction (return + assign). */
    public Assignment reassign(long vehicleId, long newDriverId, String purpose, String notes) {
        SessionContext.require(Permission.MANAGE_ASSIGNMENTS);
        return db.inTransaction(c -> {
            assignments.findActiveByVehicle(vehicleId).ifPresent(a -> returnVehicle(a.getId(), "Reassigned"));
            return assign(vehicleId, newDriverId, purpose, notes);
        });
    }

    public Assignment cancel(long assignmentId, String reason) {
        SessionContext.require(Permission.MANAGE_ASSIGNMENTS);
        Assignment a = assignments.findById(assignmentId).orElseThrow(() -> new RecordNotFoundException("Assignment", assignmentId));
        if (a.getStatus() != Assignment.Status.ACTIVE) throw new BusinessRuleException("Only active assignments can be cancelled.");
        return db.inTransaction(c -> {
            a.setStatus(Assignment.Status.CANCELLED);
            a.setReturnedAt(LocalDateTime.now());
            a.setNotes((a.getNotes() == null ? "" : a.getNotes() + "\n") + "Cancelled: " + (reason == null ? "" : reason));
            assignments.save(a);
            vehicles.findById(a.getVehicleId()).filter(v -> v.getStatus() == VehicleStatus.ASSIGNED)
                    .ifPresent(v -> vehicles.updateStatus(v.getId(), VehicleStatus.AVAILABLE));
            audit.record("CANCEL", "ASSIGNMENT", a.getVehicleDisplay(), "Assignment cancelled: " + reason);
            return a;
        });
    }
}
