package com.femzyk.fleetmanagement.service;

import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.model.*;
import com.femzyk.fleetmanagement.security.SessionContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a clearly marked, realistic demonstration data set for evaluation and training.
 *
 * <ul>
 *   <li>Every demo record carries the marker {@value #MARKER} in its notes, and a settings flag records the load,
 *       so loading is idempotent (second call is a no-op) and the data is recognisable as sample data.</li>
 *   <li>All records are created through the normal services, so every business rule, code sequence, audit entry and
 *       status transition is exercised exactly as it would be by a user.</li>
 *   <li>Figures (costs, mileage) are plausible Lagos-area values chosen for demonstration; they are not real
 *       company data.</li>
 * </ul>
 */
public class DemoDataService {

    public static final String MARKER = "[DEMO DATA]";
    static final String SETTING_KEY = "demo.loaded_at";

    private final ServiceRegistry s;
    private final DatabaseManager db;

    public DemoDataService(ServiceRegistry services, DatabaseManager db) {
        this.s = services;
        this.db = db;
    }

    public boolean isLoaded() {
        return s.settingsRepository.get(SETTING_KEY).isPresent();
    }

    public record Summary(boolean loaded, int employees, int drivers, int vehicles, int assignments, int trips, int maintenance, int fuel, int expenses) {}

    /** Loads the data set if it has not been loaded already. Returns what was created (all zeros when skipped). */
    public Summary load() {
        SessionContext.require(Permission.LOAD_DEMO_DATA);
        if (isLoaded()) return new Summary(false, 0, 0, 0, 0, 0, 0, 0, 0);
        return db.inTransaction(c -> {
            Summary summary = createAll();
            s.settingsRepository.put(SETTING_KEY, LocalDateTime.now().toString());
            s.audit.record("DEMO_DATA", "SYSTEM", MARKER, "Demonstration data set loaded");
            return summary;
        });
    }

    private Summary createAll() {
        LocalDate today = LocalDate.now();
        List<Employee> emps = new ArrayList<>();
        emps.add(employee("Adebayo", "Okonkwo", "08031234567", "adebayo.okonkwo@femzyk.demo", "Operations", "Fleet Manager", 1985, 3, 12, today.minusYears(4), 350000));
        emps.add(employee("Chiamaka", "Eze", "08052345678", "chiamaka.eze@femzyk.demo", "Operations", "Operations Officer", 1992, 7, 4, today.minusYears(2).minusMonths(3), 220000));
        emps.add(employee("Ibrahim", "Musa", "08063456789", "ibrahim.musa@femzyk.demo", "Transport", "Senior Driver", 1980, 1, 20, today.minusYears(6), 145000));
        emps.add(employee("Funke", "Adeyemi", "08074567890", "funke.adeyemi@femzyk.demo", "Transport", "Driver", 1990, 11, 2, today.minusYears(3), 120000));
        emps.add(employee("Emeka", "Nwosu", "08085678901", "emeka.nwosu@femzyk.demo", "Transport", "Driver", 1988, 5, 15, today.minusYears(1).minusMonths(8), 120000));
        emps.add(employee("Tunde", "Bakare", "08096789012", "tunde.bakare@femzyk.demo", "Transport", "Driver / Dispatch Rider", 1995, 9, 9, today.minusMonths(14), 95000));
        emps.add(employee("Ngozi", "Okafor", "08107890123", "ngozi.okafor@femzyk.demo", "Finance", "Accounts Officer", 1991, 2, 28, today.minusYears(2), 240000));
        emps.add(employee("Yusuf", "Abdullahi", "08118901234", "yusuf.abdullahi@femzyk.demo", "Transport", "Truck Driver", 1983, 6, 30, today.minusYears(5), 160000));
        emps.add(employee("Blessing", "Johnson", "08129012345", "blessing.johnson@femzyk.demo", "Maintenance", "Workshop Supervisor", 1987, 12, 1, today.minusYears(3).minusMonths(6), 210000));
        emps.add(employee("Segun", "Olawale", "08130123456", "segun.olawale@femzyk.demo", "Transport", "Driver", 1993, 4, 18, today.minusMonths(20), 120000));

        List<Driver> drivers = new ArrayList<>();
        drivers.add(driver(emps.get(2), "LAG-2019-104477", LicenceCategory.D, today.minusYears(2), today.plusYears(3)));
        drivers.add(driver(emps.get(3), "LAG-2020-228190", LicenceCategory.B, today.minusYears(1), today.plusYears(4)));
        drivers.add(driver(emps.get(4), "OGN-2018-551233", LicenceCategory.C, today.minusYears(3), today.plusDays(21)));   // expiring soon
        drivers.add(driver(emps.get(5), "LAG-2022-771002", LicenceCategory.A, today.minusYears(1), today.plusYears(2)));
        drivers.add(driver(emps.get(7), "KAN-2017-330912", LicenceCategory.E, today.minusYears(4), today.plusYears(1)));
        drivers.add(driver(emps.get(9), "LAG-2021-660845", LicenceCategory.D, today.minusYears(2), today.plusMonths(8)));

        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(car("LND-482-KJA", "Toyota", "Corolla", 2019, "Silver", 4, "AUTOMATIC", 61200, 8500000, today.minusYears(3)));
        vehicles.add(car("KJA-915-AA", "Toyota", "Hilux", 2021, "White", 4, "MANUAL", 38400, 21000000, today.minusYears(2)));
        vehicles.add(car("EKY-207-BD", "Hyundai", "Elantra", 2018, "Black", 4, "AUTOMATIC", 88750, 6200000, today.minusYears(4)));
        vehicles.add(bus("LSR-330-XA", "Toyota", "Hiace", 2020, "White", 14, 72300, 18500000, today.minusYears(2).minusMonths(6)));
        vehicles.add(truck("APP-118-YC", "MAN", "TGS 26.440", 2017, "Blue", 20, "MANUAL", 154900, 45000000, today.minusYears(5)));
        vehicles.add(truck("FST-624-KD", "Mercedes-Benz", "Atego 1518", 2019, "White", 8, "MANUAL", 96100, 32000000, today.minusYears(3)));
        vehicles.add(bike("MUS-771-QA", "Bajaj", "Boxer 150", 2022, "Red", 2, "STANDARD", 12800, 650000, today.minusMonths(15)));
        vehicles.add(bike("IKJ-402-QB", "TVS", "HLX 125", 2021, "Black", 2, "STANDARD", 21400, 580000, today.minusYears(2)));
        vehicles.add(van("AGL-559-DE", "Ford", "Transit", 2016, "Grey", 3, 132000, 7800000, today.minusYears(6)));

        // Assignments (creates ASSIGNED statuses via the rules)
        int assignments = 0;
        s.assignments.assign(vehicles.get(1).getId(), drivers.get(0).getId(), "Field operations - Lekki axis", MARKER); assignments++;
        s.assignments.assign(vehicles.get(0).getId(), drivers.get(1).getId(), "Admin pool car", MARKER); assignments++;
        s.assignments.assign(vehicles.get(4).getId(), drivers.get(4).getId(), "Apapa port haulage", MARKER); assignments++;
        s.assignments.assign(vehicles.get(6).getId(), drivers.get(3).getId(), "Document dispatch", MARKER); assignments++;
        Assignment returned = s.assignments.assign(vehicles.get(3).getId(), drivers.get(5).getId(), "Staff shuttle", MARKER); assignments++;
        s.assignments.returnVehicle(returned.getId(), MARKER + " Shuttle contract ended");

        // Trips across the last three months
        int trips = 0;
        trips += tripCompleted(vehicles.get(1), drivers.get(0), "Ikeja HQ", "Lekki Phase 1", "Client visit", today.minusDays(75), 9, 38400, 38446, 5.2);
        trips += tripCompleted(vehicles.get(1), drivers.get(0), "Ikeja HQ", "Ibadan", "Regional office audit", today.minusDays(52), 7, 38446, 38702, 24.0);
        trips += tripCompleted(vehicles.get(1), drivers.get(0), "Ikeja HQ", "Victoria Island", "Board meeting", today.minusDays(20), 8, 38702, 38739, 4.1);
        trips += tripCompleted(vehicles.get(0), drivers.get(1), "Ikeja HQ", "Murtala Muhammed Airport", "Airport pickup", today.minusDays(40), 6, 61200, 61224, 2.3);
        trips += tripCompleted(vehicles.get(0), drivers.get(1), "Ikeja HQ", "Abeokuta", "Vendor inspection", today.minusDays(12), 8, 61224, 61398, 15.6);
        trips += tripCompleted(vehicles.get(4), drivers.get(4), "Apapa Port", "Agbara Industrial Estate", "Container delivery", today.minusDays(60), 6, 154900, 154968, 38.0);
        trips += tripCompleted(vehicles.get(4), drivers.get(4), "Apapa Port", "Ota", "Container delivery", today.minusDays(33), 7, 154968, 155030, 35.5);
        trips += tripCompleted(vehicles.get(4), drivers.get(4), "Apapa Port", "Ibadan", "Bulk cargo", today.minusDays(9), 9, 155030, 155290, 118.0);
        trips += tripCompleted(vehicles.get(6), drivers.get(3), "Ikeja HQ", "Yaba", "Document dispatch", today.minusDays(5), 10, 12800, 12822, 0.6);
        trips += tripCompleted(vehicles.get(6), drivers.get(3), "Ikeja HQ", "Ikoyi", "Document dispatch", today.minusDays(2), 11, 12822, 12851, 0.8);
        // one ACTIVE trip and one PLANNED trip
        s.trips.create(trip(vehicles.get(6), drivers.get(3), "Ikeja HQ", "Apapa", "Customs documents", LocalDateTime.now().minusHours(2), 12851L), true); trips++;
        s.trips.create(trip(vehicles.get(0), drivers.get(1), "Ikeja HQ", "Epe", "Site survey", LocalDateTime.now().plusDays(2).withHour(8).withMinute(0), null), false); trips++;

        // Maintenance
        int maint = 0;
        maint += maintenance(vehicles.get(0), today.minusDays(70), MaintenanceRecord.Type.ROUTINE_SERVICE, "Oil and filter change, brake inspection", "AutoCare Ikeja", 45000, 61150, today.minusDays(70).plusMonths(3), 66150L, MaintenanceRecord.Status.COMPLETED);
        maint += maintenance(vehicles.get(1), today.minusDays(45), MaintenanceRecord.Type.TYRE, "Replaced two rear tyres", "Tyre Express Ogba", 190000, 38500, null, 78500L, MaintenanceRecord.Status.COMPLETED);
        maint += maintenance(vehicles.get(4), today.minusDays(90), MaintenanceRecord.Type.ROUTINE_SERVICE, "Full service and gearbox oil", "MAN Service Centre Apapa", 320000, 154800, today.minusDays(90).plusMonths(2), 164800L, MaintenanceRecord.Status.COMPLETED); // overdue by date
        maint += maintenance(vehicles.get(5), today.minusDays(25), MaintenanceRecord.Type.REPAIR, "Clutch plate replacement", "Femzyk Workshop", 275000, 96050, null, null, MaintenanceRecord.Status.COMPLETED);
        maint += maintenance(vehicles.get(2), today.minusDays(3), MaintenanceRecord.Type.REPAIR, "Air-conditioning compressor failure", "AutoCare Ikeja", 185000, 88750, null, null, MaintenanceRecord.Status.IN_PROGRESS);
        maint += maintenance(vehicles.get(7), today.minusDays(15), MaintenanceRecord.Type.INSPECTION, "Roadworthiness inspection", "VIS Ojodu", 12500, 21350, today.minusDays(15).plusYears(1), null, MaintenanceRecord.Status.COMPLETED);
        maint += maintenance(vehicles.get(3), today.plusDays(6), MaintenanceRecord.Type.ROUTINE_SERVICE, "Scheduled 75,000 km service", "Toyota Lagos", 0, null, null, null, MaintenanceRecord.Status.SCHEDULED);

        // Fuel
        int fuel = 0;
        fuel += fuel(vehicles.get(1), drivers.get(0), today.minusDays(76), 60, 1050, 38400L, "NNPC Ikeja", "RCP-1001");
        fuel += fuel(vehicles.get(1), drivers.get(0), today.minusDays(53), 65, 1080, 38446L, "TotalEnergies Berger", "RCP-1017");
        fuel += fuel(vehicles.get(1), drivers.get(0), today.minusDays(21), 58, 1100, 38702L, "NNPC Ikeja", "RCP-1042");
        fuel += fuel(vehicles.get(0), drivers.get(1), today.minusDays(41), 45, 1050, 61200L, "Mobil Allen", "RCP-1009");
        fuel += fuel(vehicles.get(0), drivers.get(1), today.minusDays(13), 42, 1120, 61224L, "Mobil Allen", "RCP-1050");
        fuel += fuel(vehicles.get(4), drivers.get(4), today.minusDays(61), 300, 1250, 154900L, "Conoil Apapa", "RCP-1005");
        fuel += fuel(vehicles.get(4), drivers.get(4), today.minusDays(34), 280, 1280, 154968L, "Conoil Apapa", "RCP-1029");
        fuel += fuel(vehicles.get(4), drivers.get(4), today.minusDays(10), 320, 1300, 155030L, "Conoil Apapa", "RCP-1055");
        fuel += fuel(vehicles.get(6), drivers.get(3), today.minusDays(6), 8, 1100, 12800L, "Oando Ojota", "RCP-1053");
        fuel += fuel(vehicles.get(3), drivers.get(5), today.minusDays(30), 70, 1060, 72300L, "NNPC Ikeja", "RCP-1031");

        // Manual expenses
        int exp = 0;
        exp += expense(vehicles.get(1), today.minusDays(80), ExpenseCategory.INSURANCE, 210000, "Leadway Assurance", "Comprehensive insurance renewal", "INS-2024-0042");
        exp += expense(vehicles.get(4), today.minusDays(58), ExpenseCategory.TOLLS, 12000, "LCC", "Lekki toll passes - month", "TOL-0912");
        exp += expense(vehicles.get(0), today.minusDays(35), ExpenseCategory.REGISTRATION, 28500, "Lagos MVAA", "Vehicle licence renewal", "MVAA-77123");
        exp += expense(vehicles.get(4), today.minusDays(28), ExpenseCategory.FINES, 20000, "LASTMA", "Obstruction fine - contested", "LTM-5581");
        exp += expense(null, today.minusDays(18), ExpenseCategory.CLEANING, 35000, "SparkleWash", "Fleet wash - all vehicles", "SW-0331");
        exp += expense(vehicles.get(3), today.minusDays(7), ExpenseCategory.PARKING, 9000, "MMA2", "Airport parking", "PKG-118");

        // A retired and an out-of-service vehicle to exercise the state machine
        s.vehicles.changeStatus(vehicles.get(8).getId(), VehicleStatus.OUT_OF_SERVICE, MARKER + " Engine knock - awaiting decision");
        Vehicle old = car("LND-009-AB", "Peugeot", "406", 2008, "Green", 4, "MANUAL", 312000, 1200000, today.minusYears(14));
        s.vehicles.changeStatus(old.getId(), VehicleStatus.RETIRED, MARKER + " Disposed by auction");
        vehicles.add(old);

        return new Summary(true, emps.size(), drivers.size(), vehicles.size(), assignments, trips, maint, fuel, exp);
    }

    // ---- builders ---------------------------------------------------------------------------------------

    private Employee employee(String first, String last, String phone, String email, String dept, String position,
                              int by, int bm, int bd, LocalDate hired, double salary) {
        Employee e = new Employee();
        e.setFirstName(first); e.setLastName(last); e.setPhone(phone); e.setEmail(email);
        e.setAddress("Ikeja, Lagos"); e.setDepartment(dept); e.setPosition(position);
        e.setDateOfBirth(LocalDate.of(by, bm, bd)); e.setHireDate(hired); e.setSalary(salary);
        e.setStatus(EmploymentStatus.ACTIVE); e.setEmergencyContactName("Family of " + first); e.setEmergencyContactPhone("0700" + phone.substring(4));
        e.setNotes(MARKER);
        return s.employees.create(e);
    }

    private Driver driver(Employee e, String licence, LicenceCategory cat, LocalDate issued, LocalDate expiry) {
        Driver d = new Driver();
        d.setEmployeeId(e.getId()); d.setLicenceNumber(licence); d.setLicenceCategory(cat);
        d.setLicenceIssueDate(issued); d.setLicenceExpiryDate(expiry); d.setStatus(DriverStatus.ACTIVE); d.setNotes(MARKER);
        return s.drivers.create(d);
    }

    private Vehicle base(Vehicle v, String reg, String make, String model, int year, String color, long km, double cost, LocalDate acquired) {
        v.setRegistrationNumber(reg); v.setMake(make); v.setModel(model); v.setYear(year); v.setColor(color);
        v.setCurrentMileage(km); v.setAcquisitionCost(cost); v.setAcquisitionDate(acquired); v.setNotes(MARKER);
        v.setVin("DEMO" + reg.replace("-", ""));
        return s.vehicles.create(v);
    }

    private Vehicle car(String reg, String make, String model, int year, String color, int doors, String trans, long km, double cost, LocalDate acq) {
        Car c = new Car(); c.setNumberOfDoors(doors); c.setTransmission(trans); c.setCapacity(5); c.setFuelType(FuelType.PETROL);
        return base(c, reg, make, model, year, color, km, cost, acq);
    }

    private Vehicle truck(String reg, String make, String model, int year, String color, double tons, String trans, long km, double cost, LocalDate acq) {
        Truck t = new Truck(); t.setCargoCapacityTons(tons); t.setTransmission(trans); t.setCapacity(2); t.setFuelType(FuelType.DIESEL);
        return base(t, reg, make, model, year, color, km, cost, acq);
    }

    private Vehicle bike(String reg, String make, String model, int year, String color, int wheels, String style, long km, double cost, LocalDate acq) {
        Motorcycle m = new Motorcycle(); m.setNumberOfWheels(wheels); m.setStyle(style); m.setCapacity(2); m.setFuelType(FuelType.PETROL);
        return base(m, reg, make, model, year, color, km, cost, acq);
    }

    private Vehicle bus(String reg, String make, String model, int year, String color, int seats, long km, double cost, LocalDate acq) {
        GenericVehicle g = new GenericVehicle(VehicleType.BUS); g.setCapacity(seats); g.setFuelType(FuelType.DIESEL);
        return base(g, reg, make, model, year, color, km, cost, acq);
    }

    private Vehicle van(String reg, String make, String model, int year, String color, int seats, long km, double cost, LocalDate acq) {
        GenericVehicle g = new GenericVehicle(VehicleType.VAN); g.setCapacity(seats); g.setFuelType(FuelType.DIESEL);
        return base(g, reg, make, model, year, color, km, cost, acq);
    }

    private Trip trip(Vehicle v, Driver d, String from, String to, String purpose, LocalDateTime departure, Long startKm) {
        Trip t = new Trip();
        t.setVehicleId(v.getId()); t.setDriverId(d.getId()); t.setOrigin(from); t.setDestination(to); t.setPurpose(purpose);
        t.setDepartureTime(departure); t.setStartMileage(startKm); t.setNotes(MARKER);
        return t;
    }

    private int tripCompleted(Vehicle v, Driver d, String from, String to, String purpose, LocalDate day, int hour, long startKm, long endKm, double litres) {
        LocalDateTime dep = day.atTime(hour, 0);
        Trip t = s.trips.create(trip(v, d, from, to, purpose, dep, startKm), true);
        s.trips.complete(t.getId(), endKm, litres, dep.plusHours(Math.max(1, (endKm - startKm) / 40)), MARKER);
        return 1;
    }

    private int maintenance(Vehicle v, LocalDate date, MaintenanceRecord.Type type, String desc, String provider, double cost, Integer km,
                            LocalDate nextDate, Long nextKm, MaintenanceRecord.Status status) {
        MaintenanceRecord m = new MaintenanceRecord();
        m.setVehicleId(v.getId()); m.setServiceDate(date); m.setType(type); m.setDescription(desc); m.setProvider(provider); m.setCost(cost);
        m.setMileageAtService(km == null ? null : km.longValue()); m.setNextServiceDate(nextDate); m.setNextServiceMileage(nextKm); m.setStatus(status); m.setNotes(MARKER);
        s.maintenance.create(m);
        return 1;
    }

    private int fuel(Vehicle v, Driver d, LocalDate date, double litres, double price, Long km, String station, String receipt) {
        FuelRecord f = new FuelRecord();
        f.setVehicleId(v.getId()); f.setDriverId(d.getId()); f.setDate(date); f.setQuantityLitres(litres); f.setUnitPrice(price);
        f.setMileage(km); f.setStation(station); f.setReceiptReference(receipt); f.setNotes(MARKER);
        s.fuel.create(f);
        return 1;
    }

    private int expense(Vehicle v, LocalDate date, ExpenseCategory cat, double amount, String vendor, String desc, String ref) {
        Expense x = new Expense();
        x.setVehicleId(v == null ? null : v.getId()); x.setDate(date); x.setCategory(cat); x.setAmount(amount); x.setVendor(vendor);
        x.setDescription(desc); x.setReference(ref); x.setNotes(MARKER);
        s.expenses.create(x);
        return 1;
    }
}
