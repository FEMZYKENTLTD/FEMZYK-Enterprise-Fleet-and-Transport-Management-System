package com.femzyk.fleetmanagement.service;

import com.femzyk.fleetmanagement.TestSupport;
import com.femzyk.fleetmanagement.exception.BusinessRuleException;
import com.femzyk.fleetmanagement.exception.DuplicateRecordException;
import com.femzyk.fleetmanagement.exception.ValidationException;
import com.femzyk.fleetmanagement.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeAndDriverServiceTest {

    ServiceRegistry s;

    @BeforeEach void setUp() { s = TestSupport.freshRegistryLoggedInAsAdmin(); }

    @Test void createAssignsSequentialCodes() {
        Employee a = Fixtures.employee(s, "Ada", "Obi");
        Employee b = Fixtures.employee(s, "Bola", "Ige");
        assertEquals("EMP-0001", a.getEmployeeCode());
        assertEquals("EMP-0002", b.getEmployeeCode());
    }

    @Test void validationCollectsAllErrors() {
        Employee e = new Employee();
        e.setEmail("not-an-email"); e.setPhone("12"); e.setSalary(-5.0);
        ValidationException ex = assertThrows(ValidationException.class, () -> s.employees.create(e));
        assertTrue(ex.getErrors().size() >= 4, ex.getErrors().toString());
    }

    @Test void duplicateEmailRejected() {
        Employee a = Fixtures.employee(s, "Ada", "Obi");
        Employee b = new Employee();
        b.setFirstName("X"); b.setLastName("Y"); b.setPhone("08030001111"); b.setEmail(a.getEmail()); b.setDepartment("Ops"); b.setPosition("P"); b.setHireDate(LocalDate.now());
        assertThrows(DuplicateRecordException.class, () -> s.employees.create(b));
    }

    @Test void searchAndFilter() {
        Fixtures.employee(s, "Ada", "Obi");
        Employee b = Fixtures.employee(s, "Bola", "Ige");
        b.setDepartment("Finance"); s.employees.update(b);
        assertEquals(1, s.employees.search("bola", null, null).size());
        assertEquals(1, s.employees.search(null, "Finance", null).size());
        assertEquals(2, s.employees.search(null, null, EmploymentStatus.ACTIVE).size());
        assertEquals(0, s.employees.search(null, null, EmploymentStatus.TERMINATED).size());
    }

    @Test void softDeleteRestoreAndPermanentDelete() {
        Employee a = Fixtures.employee(s, "Ada", "Obi");
        s.employees.delete(a.getId());
        assertTrue(s.employees.findAll().isEmpty());
        assertEquals(1, s.employees.recycleBin().size());
        s.employees.restore(a.getId());
        assertEquals(1, s.employees.findAll().size());
        s.employees.delete(a.getId());
        s.employees.deletePermanently(a.getId());
        assertTrue(s.employees.recycleBin().isEmpty());
        assertTrue(s.employeeRepository.findById(a.getId()).isEmpty());
    }

    @Test void driverRequiresEmployeeAndUniqueLicence() {
        Employee e = Fixtures.employee(s, "Ada", "Obi");
        Driver d = Fixtures.driver(s, e, LicenceCategory.B, LocalDate.now().plusYears(1));
        assertEquals("DRV-0001", d.getDriverCode());
        assertEquals("Ada Obi", d.getFullName());
        assertTrue(s.employees.getById(e.getId()).isDriver());
        Driver dup = new Driver();
        dup.setEmployeeId(Fixtures.employee(s, "B", "C").getId()); dup.setLicenceNumber(d.getLicenceNumber()); dup.setLicenceExpiryDate(LocalDate.now().plusYears(1));
        assertThrows(DuplicateRecordException.class, () -> s.drivers.create(dup));
        Driver second = new Driver();
        second.setEmployeeId(e.getId()); second.setLicenceNumber("OTHER-123"); second.setLicenceExpiryDate(LocalDate.now().plusYears(1));
        assertThrows(DuplicateRecordException.class, () -> s.drivers.create(second), "one driver profile per employee");
    }

    @Test void terminatingEmployeeDeactivatesDriver() {
        Employee e = Fixtures.employee(s, "Ada", "Obi");
        Driver d = Fixtures.driver(s, e, LicenceCategory.B, LocalDate.now().plusYears(1));
        e.setStatus(EmploymentStatus.TERMINATED);
        s.employees.update(e);
        assertEquals(DriverStatus.INACTIVE, s.drivers.getById(d.getId()).getStatus());
    }

    @Test void licenceAlertsListExpiringAndExpired() {
        Fixtures.driver(s, Fixtures.employee(s, "A", "A"), LicenceCategory.B, LocalDate.now().plusDays(10));
        Fixtures.driver(s, Fixtures.employee(s, "B", "B"), LicenceCategory.B, LocalDate.now().plusYears(3));
        assertEquals(1, s.drivers.licenceAlerts().size());
        assertThrows(ValidationException.class, () -> Fixtures.driver(s, Fixtures.employee(s, "C", "C"), LicenceCategory.B, LocalDate.now().minusDays(1)),
                "a new driver cannot be created with an already-expired licence");
    }
}
