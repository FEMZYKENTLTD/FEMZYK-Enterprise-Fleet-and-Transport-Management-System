package com.femzyk.fleetmanagement.service;

import com.femzyk.fleetmanagement.audit.AuditService;
import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.exception.BusinessRuleException;
import com.femzyk.fleetmanagement.exception.InvalidStateTransitionException;
import com.femzyk.fleetmanagement.exception.RecordNotFoundException;
import com.femzyk.fleetmanagement.model.*;
import com.femzyk.fleetmanagement.repository.*;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.validation.Validators;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class TripService {

    public static final String CODE_PREFIX = "TRP";

    private final DatabaseManager db;
    private final TripRepository trips;
    private final VehicleRepository vehicles;
    private final DriverRepository drivers;
    private final AssignmentRepository assignments;
    private final CodeSequenceRepository codes;
    private final AuditService audit;

    public TripService(DatabaseManager db, TripRepository trips, VehicleRepository vehicles, DriverRepository drivers,
                       AssignmentRepository assignments, CodeSequenceRepository codes, AuditService audit) {
        this.db = db;
        this.trips = trips;
        this.vehicles = vehicles;
        this.drivers = drivers;
        this.assignments = assignments;
        this.codes = codes;
        this.audit = audit;
    }

    public List<Trip> findAll() {
        SessionContext.require(Permission.VIEW_RECORDS);
        return trips.findAll();
    }

    public List<Trip> search(String text, Trip.Status status, Long vehicleId, Long driverId, LocalDate from, LocalDate to) {
        SessionContext.require(Permission.VIEW_RECORDS);
        return trips.search(text, status, vehicleId, driverId, from, to);
    }

    public Optional<Trip> findById(long id) { return trips.findById(id); }

    public Trip getById(long id) {
        return trips.findById(id).orElseThrow(() -> new RecordNotFoundException("Trip", id));
    }

    public long countActive() { return trips.count(Trip.Status.ACTIVE); }

    /** Creates a PLANNED trip (or ACTIVE when {@code startNow}). */
    public Trip create(Trip t, boolean startNow) {
        SessionContext.require(Permission.MANAGE_TRIPS);
        validate(t, false);
        Vehicle vehicle = vehicles.findById(t.getVehicleId()).orElseThrow(() -> new RecordNotFoundException("Vehicle", t.getVehicleId()));
        Driver driver = drivers.findById(t.getDriverId()).orElseThrow(() -> new RecordNotFoundException("Driver", t.getDriverId()));
        if (vehicle.isDeleted() || vehicle.getStatus() == VehicleStatus.RETIRED || vehicle.getStatus() == VehicleStatus.OUT_OF_SERVICE
                || vehicle.getStatus() == VehicleStatus.MAINTENANCE) {
            throw new BusinessRuleException("Vehicle " + vehicle.getRegistrationNumber() + " is " + vehicle.getStatus() + " and cannot be used for a trip.");
        }
        if (!driver.getStatus().canBeAssigned()) throw new BusinessRuleException("Driver " + driver.getFullName() + " is " + driver.getStatus() + ".");
        if (driver.isLicenceExpired()) throw new BusinessRuleException("Driver " + driver.getFullName() + "'s licence has expired.");
        if (!driver.getLicenceCategory().permits(vehicle.getVehicleType())) {
            throw new BusinessRuleException("Licence class " + driver.getLicenceCategory() + " does not permit driving a " + vehicle.getVehicleType() + ".");
        }
        if (t.getStartMileage() == null) t.setStartMileage(vehicle.getCurrentMileage());
        if (t.getStartMileage() < vehicle.getCurrentMileage()) {
            throw new BusinessRuleException("Starting mileage (" + t.getStartMileage() + ") cannot be lower than the vehicle's current odometer (" + vehicle.getCurrentMileage() + " km).");
        }
        return db.inTransaction(c -> {
            if (Validators.isBlank(t.getTripCode())) t.setTripCode(codes.next(CODE_PREFIX));
            t.setStatus(Trip.Status.PLANNED);
            trips.save(t);
            audit.record("CREATE", "TRIP", t.getTripCode(), "Planned trip to " + t.getDestination() + " with " + vehicle.getRegistrationNumber() + " / " + driver.getFullName());
            if (startNow) start(t.getId());
            return trips.findById(t.getId()).orElse(t);
        });
    }

    public Trip update(Trip t) {
        SessionContext.require(Permission.MANAGE_TRIPS);
        Trip existing = getById(t.getId());
        if (existing.getStatus() == Trip.Status.COMPLETED || existing.getStatus() == Trip.Status.CANCELLED) {
            throw new BusinessRuleException("A " + existing.getStatus() + " trip cannot be edited.");
        }
        validate(t, existing.getStatus() == Trip.Status.ACTIVE);
        t.setTripCode(existing.getTripCode());
        t.setStatus(existing.getStatus());
        trips.save(t);
        audit.record("UPDATE", "TRIP", t.getTripCode(), "Updated trip to " + t.getDestination());
        return trips.findById(t.getId()).orElse(t);
    }

    public Trip start(long id) {
        SessionContext.require(Permission.MANAGE_TRIPS);
        Trip t = getById(id);
        if (t.getStatus() != Trip.Status.PLANNED) throw new InvalidStateTransitionException("Only PLANNED trips can be started (trip is " + t.getStatus() + ").");
        Vehicle v = vehicles.findById(t.getVehicleId()).orElseThrow();
        if (!trips.findActiveForVehicle(v.getId()).isEmpty()) throw new BusinessRuleException("Vehicle " + v.getRegistrationNumber() + " is already on an active trip.");
        if (!trips.findActiveForDriver(t.getDriverId()).isEmpty()) throw new BusinessRuleException("Driver is already on an active trip.");
        if (!v.getStatus().isOperational()) throw new BusinessRuleException("Vehicle is " + v.getStatus() + ".");
        return db.inTransaction(c -> {
            t.setStatus(Trip.Status.ACTIVE);
            if (t.getDepartureTime() == null) t.setDepartureTime(LocalDateTime.now());
            trips.save(t);
            vehicles.updateStatus(v.getId(), VehicleStatus.IN_SERVICE);
            audit.record("START", "TRIP", t.getTripCode(), "Trip started");
            return t;
        });
    }

    /** Completes a trip: validates odometer consistency, updates vehicle mileage, restores vehicle status. */
    public Trip complete(long id, long endMileage, Double fuelUsedLitres, LocalDateTime returnTime, String notes) {
        SessionContext.require(Permission.MANAGE_TRIPS);
        Trip t = getById(id);
        if (t.getStatus() != Trip.Status.ACTIVE && t.getStatus() != Trip.Status.PLANNED) {
            throw new InvalidStateTransitionException("Trip is already " + t.getStatus() + ".");
        }
        LocalDateTime ret = returnTime == null ? LocalDateTime.now() : returnTime;
        Validators.errors()
                .check(t.getStartMileage() != null, "Starting mileage is missing.")
                .check(t.getStartMileage() == null || endMileage >= t.getStartMileage(), "Ending mileage (" + endMileage + ") must be greater than or equal to starting mileage (" + t.getStartMileage() + ").")
                .check(fuelUsedLitres == null || fuelUsedLitres >= 0, "Fuel used cannot be negative.")
                .check(t.getDepartureTime() == null || !ret.isBefore(t.getDepartureTime()), "Return time cannot be before departure time.")
                .throwIfAny();
        if (t.getStartMileage() != null && endMileage - t.getStartMileage() > 5000) {
            throw new BusinessRuleException("A single trip of more than 5,000 km looks wrong. Please check the odometer readings.");
        }
        return db.inTransaction(c -> {
            t.setEndMileage(endMileage);
            t.setFuelUsedLitres(fuelUsedLitres);
            t.setReturnTime(ret);
            if (!Validators.isBlank(notes)) t.setNotes(notes.trim());
            t.setStatus(Trip.Status.COMPLETED);
            trips.save(t);
            vehicles.updateMileageIfGreater(t.getVehicleId(), endMileage);
            Vehicle v = vehicles.findById(t.getVehicleId()).orElseThrow();
            if (v.getStatus() == VehicleStatus.IN_SERVICE) {
                boolean assigned = assignments.findActiveByVehicle(v.getId()).isPresent();
                vehicles.updateStatus(v.getId(), assigned ? VehicleStatus.ASSIGNED : VehicleStatus.AVAILABLE);
            }
            audit.record("COMPLETE", "TRIP", t.getTripCode(), "Trip completed, distance " + t.getDistanceKm() + " km");
            return trips.findById(t.getId()).orElse(t);
        });
    }

    public Trip cancel(long id, String reason) {
        SessionContext.require(Permission.MANAGE_TRIPS);
        Trip t = getById(id);
        if (t.getStatus() == Trip.Status.COMPLETED || t.getStatus() == Trip.Status.CANCELLED) {
            throw new InvalidStateTransitionException("Trip is already " + t.getStatus() + ".");
        }
        return db.inTransaction(c -> {
            boolean wasActive = t.getStatus() == Trip.Status.ACTIVE;
            t.setStatus(Trip.Status.CANCELLED);
            t.setNotes(((t.getNotes() == null ? "" : t.getNotes() + "\n") + "Cancelled: " + (reason == null ? "" : reason)).trim());
            trips.save(t);
            if (wasActive) {
                Vehicle v = vehicles.findById(t.getVehicleId()).orElseThrow();
                if (v.getStatus() == VehicleStatus.IN_SERVICE) {
                    boolean assigned = assignments.findActiveByVehicle(v.getId()).isPresent();
                    vehicles.updateStatus(v.getId(), assigned ? VehicleStatus.ASSIGNED : VehicleStatus.AVAILABLE);
                }
            }
            audit.record("CANCEL", "TRIP", t.getTripCode(), "Trip cancelled: " + reason);
            return t;
        });
    }

    public void delete(long id) {
        SessionContext.require(Permission.MANAGE_TRIPS);
        Trip t = getById(id);
        if (t.getStatus() != Trip.Status.PLANNED && t.getStatus() != Trip.Status.CANCELLED) {
            throw new BusinessRuleException("Only PLANNED or CANCELLED trips can be deleted; completed trips are part of the vehicle history.");
        }
        trips.delete(id);
        audit.record("DELETE", "TRIP", t.getTripCode(), "Trip deleted");
    }

    void validate(Trip t, boolean active) {
        Validators.Errors errors = Validators.errors()
                .required(t.getVehicleId(), "Vehicle")
                .required(t.getDriverId(), "Driver")
                .required(t.getDestination(), "Destination")
                .required(t.getDepartureTime(), "Departure date/time")
                .nonNegative(t.getStartMileage(), "Starting mileage")
                .nonNegative(t.getEndMileage(), "Ending mileage")
                .nonNegative(t.getFuelUsedLitres(), "Fuel used")
                .maxLength(t.getNotes(), 2000, "Notes");
        if (t.getStartMileage() != null && t.getEndMileage() != null) {
            errors.check(t.getEndMileage() >= t.getStartMileage(), "Ending mileage must be greater than or equal to starting mileage.");
        }
        if (t.getDepartureTime() != null && t.getReturnTime() != null) {
            errors.check(!t.getReturnTime().isBefore(t.getDepartureTime()), "Return time cannot be before departure time.");
        }
        errors.throwIfAny();
    }
}
