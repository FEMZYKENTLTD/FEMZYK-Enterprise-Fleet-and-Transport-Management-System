package com.femzyk.fleetmanagement.service;

import com.femzyk.fleetmanagement.audit.AuditService;
import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.exception.BusinessRuleException;
import com.femzyk.fleetmanagement.exception.DuplicateRecordException;
import com.femzyk.fleetmanagement.exception.InvalidStateTransitionException;
import com.femzyk.fleetmanagement.exception.RecordNotFoundException;
import com.femzyk.fleetmanagement.model.*;
import com.femzyk.fleetmanagement.repository.AssignmentRepository;
import com.femzyk.fleetmanagement.repository.CodeSequenceRepository;
import com.femzyk.fleetmanagement.repository.TripRepository;
import com.femzyk.fleetmanagement.repository.VehicleRepository;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.validation.Validators;

import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Vehicle management: CRUD with validation carried over from the original Car/Motorcycle/Truck
 * constructors, uniqueness of registration/VIN, a status state machine and a recycle bin.
 */
public class VehicleService {

    public static final String CODE_PREFIX = "VEH";

    private final DatabaseManager db;
    private final VehicleRepository vehicles;
    private final AssignmentRepository assignments;
    private final TripRepository trips;
    private final CodeSequenceRepository codes;
    private final AuditService audit;

    public VehicleService(DatabaseManager db, VehicleRepository vehicles, AssignmentRepository assignments,
                          TripRepository trips, CodeSequenceRepository codes, AuditService audit) {
        this.db = db;
        this.vehicles = vehicles;
        this.assignments = assignments;
        this.trips = trips;
        this.codes = codes;
        this.audit = audit;
    }

    public List<Vehicle> findAll() {
        SessionContext.require(Permission.VIEW_RECORDS);
        return vehicles.findAll();
    }

    public List<Vehicle> search(String text, VehicleType type, VehicleStatus status, FuelType fuelType) {
        SessionContext.require(Permission.VIEW_RECORDS);
        return vehicles.search(text, type, status, fuelType);
    }

    public Optional<Vehicle> findById(long id) { return vehicles.findById(id); }

    public Vehicle getById(long id) {
        return vehicles.findById(id).orElseThrow(() -> new RecordNotFoundException("Vehicle", id));
    }

    public List<Vehicle> recycleBin() {
        SessionContext.require(Permission.MANAGE_VEHICLES);
        return vehicles.findDeleted();
    }

    public Map<VehicleStatus, Long> countByStatus() { return vehicles.countByStatus(); }
    public Map<VehicleType, Long> countByType() { return vehicles.countByType(); }

    public Vehicle create(Vehicle v) {
        SessionContext.require(Permission.MANAGE_VEHICLES);
        validate(v);
        normalise(v);
        vehicles.findByRegistration(v.getRegistrationNumber()).ifPresent(o -> {
            throw new DuplicateRecordException("Registration number " + v.getRegistrationNumber() + " is already used by " + o.getIdentityDisplay() + ".");
        });
        if (v.getStatus() == VehicleStatus.ASSIGNED) v.setStatus(VehicleStatus.AVAILABLE); // assignment is made through AssignmentService
        return db.inTransaction(c -> {
            if (Validators.isBlank(v.getVehicleCode())) v.setVehicleCode(codes.next(CODE_PREFIX));
            vehicles.save(v);
            audit.record("CREATE", "VEHICLE", v.getRegistrationNumber(), "Added " + v.getVehicleType() + " " + v.getIdentityDisplay());
            return v;
        });
    }

    public Vehicle update(Vehicle v) {
        SessionContext.require(Permission.MANAGE_VEHICLES);
        if (v.isNew()) throw new BusinessRuleException("Cannot update an unsaved vehicle.");
        validate(v);
        normalise(v);
        Vehicle existing = getById(v.getId());
        vehicles.findByRegistration(v.getRegistrationNumber()).filter(o -> !o.getId().equals(v.getId())).ifPresent(o -> {
            throw new DuplicateRecordException("Registration number " + v.getRegistrationNumber() + " is already used by " + o.getIdentityDisplay() + ".");
        });
        if (v.getCurrentMileage() < existing.getCurrentMileage()) {
            throw new BusinessRuleException("Current mileage (" + v.getCurrentMileage() + " km) cannot be lower than the recorded mileage ("
                    + existing.getCurrentMileage() + " km).");
        }
        if (existing.getStatus() != v.getStatus()) {
            checkTransition(existing, v.getStatus());
        }
        v.setVehicleCode(existing.getVehicleCode());
        vehicles.save(v);
        audit.record("UPDATE", "VEHICLE", v.getRegistrationNumber(), "Updated vehicle " + v.getIdentityDisplay()
                + (existing.getStatus() != v.getStatus() ? " (status " + existing.getStatus() + " -> " + v.getStatus() + ")" : ""));
        return v;
    }

    /** Explicit status change with state-machine enforcement. */
    public Vehicle changeStatus(long id, VehicleStatus target, String reason) {
        SessionContext.require(Permission.MANAGE_VEHICLES);
        Vehicle v = getById(id);
        if (v.getStatus() == target) return v;
        checkTransition(v, target);
        vehicles.updateStatus(id, target);
        audit.record("STATUS", "VEHICLE", v.getRegistrationNumber(), "Status " + v.getStatus() + " -> " + target
                + (Validators.isBlank(reason) ? "" : ": " + reason));
        v.setStatus(target);
        return v;
    }

    private void checkTransition(Vehicle v, VehicleStatus target) {
        if (!v.getStatus().canTransitionTo(target)) {
            throw new InvalidStateTransitionException("A vehicle cannot move from " + v.getStatus() + " to " + target + ".");
        }
        boolean hasActiveAssignment = assignments.findActiveByVehicle(v.getId()).isPresent();
        if (target == VehicleStatus.ASSIGNED && !hasActiveAssignment) {
            throw new BusinessRuleException("Use 'Assign vehicle' to put a vehicle into ASSIGNED status.");
        }
        if (target == VehicleStatus.AVAILABLE && hasActiveAssignment) {
            throw new BusinessRuleException("Vehicle has an active assignment; return it before marking AVAILABLE.");
        }
        if ((target == VehicleStatus.RETIRED || target == VehicleStatus.OUT_OF_SERVICE) && hasActiveAssignment) {
            throw new BusinessRuleException("Vehicle has an active assignment; return it before marking " + target + ".");
        }
        if (!trips.findActiveForVehicle(v.getId()).isEmpty() && target != VehicleStatus.IN_SERVICE) {
            throw new BusinessRuleException("Vehicle is on an active trip; complete or cancel the trip first.");
        }
    }

    public void delete(long id) {
        SessionContext.require(Permission.MANAGE_VEHICLES);
        Vehicle v = getById(id);
        if (assignments.findActiveByVehicle(id).isPresent()) throw new BusinessRuleException("Vehicle is currently assigned. Return it before deleting.");
        if (!trips.findActiveForVehicle(id).isEmpty()) throw new BusinessRuleException("Vehicle is on an active trip.");
        vehicles.softDelete(id);
        audit.record("DELETE", "VEHICLE", v.getRegistrationNumber(), "Moved vehicle to recycle bin");
    }

    public void restore(long id) {
        SessionContext.require(Permission.MANAGE_VEHICLES);
        Vehicle v = getById(id);
        vehicles.restore(id);
        audit.record("RESTORE", "VEHICLE", v.getRegistrationNumber(), "Restored vehicle from recycle bin");
    }

    /** Permanent removal; SQLite foreign keys (ON DELETE RESTRICT) refuse when history exists. */
    public void deletePermanently(long id) {
        SessionContext.require(Permission.MANAGE_VEHICLES);
        Vehicle v = getById(id);
        if (!v.isDeleted()) throw new BusinessRuleException("Only vehicles in the recycle bin can be permanently deleted.");
        try {
            vehicles.hardDelete(id);
        } catch (BusinessRuleException e) {
            throw new BusinessRuleException("Vehicle " + v.getRegistrationNumber() + " has trips, fuel, maintenance or assignment history and cannot be permanently deleted.");
        }
        audit.record("PURGE", "VEHICLE", v.getRegistrationNumber(), "Permanently deleted vehicle");
    }

    void validate(Vehicle v) {
        int maxYear = Year.now().getValue() + 1;
        Validators.Errors errors = Validators.errors()
                .required(v.getRegistrationNumber(), "Registration number")
                .check(Validators.isValidRegistration(v.getRegistrationNumber()), "Registration number must be 3-15 characters (letters, digits, spaces, hyphens).")
                .required(v.getMake(), "Make")
                .required(v.getModel(), "Model")
                .range(v.getYear(), 1950, maxYear, "Year")
                .required(v.getFuelType(), "Fuel type")
                .required(v.getStatus(), "Status")
                .nonNegative(v.getCurrentMileage(), "Current mileage")
                .nonNegative(v.getAcquisitionCost(), "Acquisition cost")
                .notFuture(v.getAcquisitionDate(), "Acquisition date")
                .check(v.getCapacity() >= 0 && v.getCapacity() <= 120, "Capacity must be between 0 and 120.")
                .maxLength(v.getNotes(), 2000, "Notes");
        if (v instanceof Car car) {
            errors.check(car.getNumberOfDoors() >= 2 && car.getNumberOfDoors() <= 6, "A car must have between 2 and 6 doors.");
            errors.check(isTransmission(car.getTransmission()), "Transmission must be MANUAL or AUTOMATIC.");
        } else if (v instanceof Motorcycle m) {
            errors.check(m.getNumberOfWheels() == 2 || m.getNumberOfWheels() == 3, "A motorcycle must have 2 or 3 wheels.");
            errors.check(m.getStyle() != null && List.of("SPORT", "CRUISER", "OFF_ROAD", "STANDARD").contains(m.getStyle().toUpperCase()),
                    "Motorcycle style must be SPORT, CRUISER, OFF_ROAD or STANDARD.");
        } else if (v instanceof Truck t) {
            errors.check(t.getCargoCapacityTons() > 0 && t.getCargoCapacityTons() <= 60, "Cargo capacity must be between 0 and 60 tons.");
            errors.check(isTransmission(t.getTransmission()), "Transmission must be MANUAL or AUTOMATIC.");
        }
        if (v.getVin() != null && !v.getVin().isBlank()) {
            errors.check(v.getVin().trim().length() >= 5 && v.getVin().trim().length() <= 20, "VIN/chassis number must be 5-20 characters.");
        }
        errors.throwIfAny();
    }

    private static boolean isTransmission(String t) {
        return t != null && (t.equalsIgnoreCase("MANUAL") || t.equalsIgnoreCase("AUTOMATIC"));
    }

    private static void normalise(Vehicle v) {
        v.setRegistrationNumber(Validators.normaliseRegistration(v.getRegistrationNumber()));
        v.setMake(v.getMake().trim());
        v.setModel(v.getModel().trim());
        if (v.getVin() != null) v.setVin(v.getVin().trim().toUpperCase());
        if (v.getAttributeText() != null) v.setAttributeText(v.getAttributeText().trim().toUpperCase());
    }
}
