package com.femzyk.fleetmanagement.service;

import com.femzyk.fleetmanagement.TestSupport;
import com.femzyk.fleetmanagement.exception.BusinessRuleException;
import com.femzyk.fleetmanagement.exception.ValidationException;
import com.femzyk.fleetmanagement.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TripServiceTest {

    ServiceRegistry s;
    Vehicle car;
    Driver driver;

    @BeforeEach void setUp() {
        s = TestSupport.freshRegistryLoggedInAsAdmin();
        car = Fixtures.car(s, "CAR-001-AA", 10000);
        driver = Fixtures.driver(s);
    }

    private Trip plan(Long startKm) {
        Trip t = new Trip();
        t.setVehicleId(car.getId()); t.setDriverId(driver.getId()); t.setOrigin("Ikeja"); t.setDestination("Lekki");
        t.setDepartureTime(LocalDateTime.now().minusHours(1)); t.setStartMileage(startKm);
        return s.trips.create(t, false);
    }

    @Test void lifecyclePlannedActiveCompletedUpdatesOdometer() {
        Trip t = plan(null);
        assertEquals("TRP-0001", t.getTripCode());
        assertEquals(Trip.Status.PLANNED, t.getStatus());
        assertEquals(10000L, t.getStartMileage(), "defaults to vehicle mileage");
        s.trips.start(t.getId());
        assertEquals(VehicleStatus.IN_SERVICE, s.vehicles.getById(car.getId()).getStatus());
        assertThrows(ValidationException.class, () -> s.trips.complete(t.getId(), 9990, null, null, null), "end < start");
        assertThrows(BusinessRuleException.class, () -> s.trips.complete(t.getId(), 20000, null, null, null), "implausible distance");
        s.trips.complete(t.getId(), 10045, 4.5, LocalDateTime.now(), "ok");
        Trip done = s.trips.getById(t.getId());
        assertEquals(Trip.Status.COMPLETED, done.getStatus());
        assertEquals(45L, done.getDistanceKm());
        Vehicle v = s.vehicles.getById(car.getId());
        assertEquals(10045, v.getCurrentMileage());
        assertEquals(VehicleStatus.AVAILABLE, v.getStatus());
        assertThrows(BusinessRuleException.class, () -> s.trips.delete(t.getId()), "completed trips are immutable");
        assertThrows(BusinessRuleException.class, () -> s.trips.update(done));
    }

    @Test void assignedVehicleReturnsToAssignedAfterTrip() {
        s.assignments.assign(car.getId(), driver.getId(), null, null);
        Trip t = plan(null);
        s.trips.start(t.getId());
        s.trips.complete(t.getId(), 10010, null, null, null);
        assertEquals(VehicleStatus.ASSIGNED, s.vehicles.getById(car.getId()).getStatus());
    }

    @Test void vehicleCannotBeOnTwoActiveTrips() {
        Trip a = plan(null);
        s.trips.start(a.getId());
        Trip b = plan(null);
        assertThrows(BusinessRuleException.class, () -> s.trips.start(b.getId()));
    }

    @Test void cancelReleasesVehicleAndPlannedCanBeDeleted() {
        Trip t = plan(null);
        s.trips.start(t.getId());
        s.trips.cancel(t.getId(), "weather");
        assertEquals(Trip.Status.CANCELLED, s.trips.getById(t.getId()).getStatus());
        assertEquals(VehicleStatus.AVAILABLE, s.vehicles.getById(car.getId()).getStatus());
        Trip p = plan(null);
        s.trips.delete(p.getId());
        assertTrue(s.trips.findById(p.getId()).isEmpty());
    }

    @Test void startMileageBelowOdometerRejected() {
        assertThrows(BusinessRuleException.class, () -> plan(9000L));
        assertThrows(ValidationException.class, () -> { Trip t = new Trip(); t.setVehicleId(car.getId()); t.setDriverId(driver.getId()); s.trips.create(t, false); });
    }

    @Test void vehicleInMaintenanceCannotStartTrip() {
        s.vehicles.changeStatus(car.getId(), VehicleStatus.MAINTENANCE, null);
        assertThrows(BusinessRuleException.class, () -> plan(null));
    }
}
