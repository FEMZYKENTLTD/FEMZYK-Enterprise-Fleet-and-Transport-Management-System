package com.femzyk.fleetmanagement.service;

import com.femzyk.fleetmanagement.TestSupport;
import com.femzyk.fleetmanagement.exception.BusinessRuleException;
import com.femzyk.fleetmanagement.exception.DuplicateRecordException;
import com.femzyk.fleetmanagement.exception.InvalidStateTransitionException;
import com.femzyk.fleetmanagement.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class VehicleAndAssignmentServiceTest {

    ServiceRegistry s;

    @BeforeEach void setUp() { s = TestSupport.freshRegistryLoggedInAsAdmin(); }

    @Test void polymorphicVehiclesRoundTrip() {
        Vehicle car = Fixtures.car(s, "ABC-123-XY", 1000);
        Vehicle truck = Fixtures.truck(s, "TRK-001-AA");
        assertEquals("VEH-0001", car.getVehicleCode());
        assertInstanceOf(Car.class, s.vehicles.getById(car.getId()));
        assertInstanceOf(Truck.class, s.vehicles.getById(truck.getId()));
        assertEquals(20.0, ((Truck) s.vehicles.getById(truck.getId())).getCargoCapacityTons());
        assertEquals(VehicleType.TRUCK, s.vehicles.getById(truck.getId()).getVehicleType());
    }

    @Test void registrationIsUniqueAndNormalised() {
        Fixtures.car(s, "abc-123-xy", 0);
        assertEquals("ABC-123-XY", s.vehicles.findAll().get(0).getRegistrationNumber());
        assertThrows(DuplicateRecordException.class, () -> Fixtures.car(s, "ABC-123-XY", 0));
    }

    @Test void statusStateMachine() {
        Vehicle v = Fixtures.car(s, "ABC-123-XY", 0);
        assertThrows(BusinessRuleException.class, () -> s.vehicles.changeStatus(v.getId(), VehicleStatus.ASSIGNED, null), "ASSIGNED only via assignment");
        s.vehicles.changeStatus(v.getId(), VehicleStatus.MAINTENANCE, "service");
        s.vehicles.changeStatus(v.getId(), VehicleStatus.RETIRED, "end of life");
        assertThrows(InvalidStateTransitionException.class, () -> s.vehicles.changeStatus(v.getId(), VehicleStatus.AVAILABLE, null));
        assertTrue(s.auditLogRepository.search(null, "VEHICLE", "STATUS", null, null, 10).size() >= 2);
    }

    @Test void mileageCannotDecrease() {
        Vehicle v = Fixtures.car(s, "ABC-123-XY", 5000);
        v.setCurrentMileage(4000);
        assertThrows(BusinessRuleException.class, () -> s.vehicles.update(v));
    }

    @Test void assignmentRulesAndHistory() {
        Vehicle car = Fixtures.car(s, "CAR-001-AA", 0);
        Vehicle truck = Fixtures.truck(s, "TRK-001-AA");
        Driver classB = Fixtures.driver(s, Fixtures.employee(s, "B", "Driver"), LicenceCategory.B, LocalDate.now().plusYears(1));
        Driver classE = Fixtures.driver(s, Fixtures.employee(s, "E", "Driver"), LicenceCategory.E, LocalDate.now().plusYears(1));

        assertThrows(BusinessRuleException.class, () -> s.assignments.assign(truck.getId(), classB.getId(), null, null), "class B cannot drive a truck");
        Assignment a = s.assignments.assign(car.getId(), classB.getId(), "pool", null);
        assertEquals(VehicleStatus.ASSIGNED, s.vehicles.getById(car.getId()).getStatus());
        assertEquals(classB.getId(), s.vehicles.getById(car.getId()).getAssignedDriverId());
        assertThrows(BusinessRuleException.class, () -> s.assignments.assign(car.getId(), classE.getId(), null, null), "vehicle already assigned");
        assertThrows(BusinessRuleException.class, () -> s.assignments.assign(truck.getId(), classB.getId(), null, null), "driver already assigned");
        assertThrows(BusinessRuleException.class, () -> s.vehicles.delete(car.getId()), "cannot delete assigned vehicle");

        s.assignments.returnVehicle(a.getId(), "done");
        assertEquals(VehicleStatus.AVAILABLE, s.vehicles.getById(car.getId()).getStatus());
        assertEquals(Assignment.Status.RETURNED, s.assignmentRepository.findById(a.getId()).orElseThrow().getStatus());
        s.assignments.assign(car.getId(), classE.getId(), null, null);
        assertEquals(2, s.assignments.historyForVehicle(car.getId()).size());
        assertEquals(1, s.assignments.countActive());
    }

    @Test void suspendedDriverOrExpiredLicenceCannotBeAssigned() {
        Vehicle car = Fixtures.car(s, "CAR-001-AA", 0);
        Driver d = Fixtures.driver(s, Fixtures.employee(s, "B", "Driver"), LicenceCategory.B, LocalDate.now().plusYears(1));
        d.setStatus(DriverStatus.SUSPENDED); s.drivers.update(d);
        assertThrows(BusinessRuleException.class, () -> s.assignments.assign(car.getId(), d.getId(), null, null));
        d.setStatus(DriverStatus.ACTIVE); d.setLicenceExpiryDate(LocalDate.now().plusDays(3)); s.drivers.update(d);
        assertDoesNotThrow(() -> s.assignments.assign(car.getId(), d.getId(), null, null));
    }

    @Test void countsByStatusAndType() {
        Fixtures.car(s, "CAR-001-AA", 0); Fixtures.car(s, "CAR-002-AA", 0); Fixtures.truck(s, "TRK-001-AA");
        assertEquals(3L, s.vehicles.countByStatus().get(VehicleStatus.AVAILABLE));
        assertEquals(2L, s.vehicles.countByType().get(VehicleType.CAR));
    }
}
