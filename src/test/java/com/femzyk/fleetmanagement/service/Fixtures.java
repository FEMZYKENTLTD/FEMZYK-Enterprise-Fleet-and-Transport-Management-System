package com.femzyk.fleetmanagement.service;

import com.femzyk.fleetmanagement.model.*;

import java.time.LocalDate;

/** Minimal valid entities for service tests. */
public final class Fixtures {
    private Fixtures() {}
    private static int seq = 0;

    public static Employee employee(ServiceRegistry s, String first, String last) {
        Employee e = new Employee();
        e.setFirstName(first); e.setLastName(last); e.setPhone("0803000" + (1000 + seq++)); e.setEmail((first + "." + last + seq + "@test.local").toLowerCase());
        e.setDepartment("Transport"); e.setPosition("Driver"); e.setHireDate(LocalDate.now().minusYears(1)); e.setDateOfBirth(LocalDate.of(1990, 1, 1)); e.setSalary(100000.0);
        return s.employees.create(e);
    }

    public static Driver driver(ServiceRegistry s, Employee e, LicenceCategory cat, LocalDate expiry) {
        Driver d = new Driver();
        d.setEmployeeId(e.getId()); d.setLicenceNumber("LIC-" + (10000 + seq++)); d.setLicenceCategory(cat);
        d.setLicenceIssueDate(LocalDate.now().minusYears(2)); d.setLicenceExpiryDate(expiry);
        return s.drivers.create(d);
    }

    public static Driver driver(ServiceRegistry s) { return driver(s, employee(s, "Test", "Driver"), LicenceCategory.D, LocalDate.now().plusYears(2)); }

    public static Vehicle car(ServiceRegistry s, String reg, long km) {
        Car c = new Car();
        c.setRegistrationNumber(reg); c.setMake("Toyota"); c.setModel("Corolla"); c.setYear(2020); c.setCurrentMileage(km); c.setCapacity(5);
        return s.vehicles.create(c);
    }

    public static Vehicle truck(ServiceRegistry s, String reg) {
        Truck t = new Truck();
        t.setRegistrationNumber(reg); t.setMake("MAN"); t.setModel("TGS"); t.setYear(2018); t.setCargoCapacityTons(20); t.setFuelType(FuelType.DIESEL); t.setCapacity(2);
        return s.vehicles.create(t);
    }
}
