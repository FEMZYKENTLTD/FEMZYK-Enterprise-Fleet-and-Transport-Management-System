package com.femzyk.fleetmanagement.io;

import com.femzyk.fleetmanagement.exception.ValidationException;
import com.femzyk.fleetmanagement.model.*;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import com.femzyk.fleetmanagement.util.DateUtil;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

/**
 * CSV export of every module and CSV import of master data (employees, vehicles, drivers).
 * Import is validated row by row through the normal services, so all business rules apply;
 * bad rows are reported with their line number and do not abort the good rows.
 */
public class CsvImportExportService {

    public enum Entity { EMPLOYEES, DRIVERS, VEHICLES, ASSIGNMENTS, TRIPS, MAINTENANCE, FUEL, EXPENSES, AUDIT_LOG }

    public static final List<String> EMPLOYEE_COLUMNS = List.of("first_name", "last_name", "phone", "email", "address", "department",
            "position", "date_of_birth", "hire_date", "salary", "status", "emergency_contact_name", "emergency_contact_phone", "notes");
    public static final List<String> VEHICLE_COLUMNS = List.of("registration_number", "type", "make", "model", "year", "color", "vin",
            "engine_number", "acquisition_date", "acquisition_cost", "current_mileage", "fuel_type", "capacity", "status", "attribute_number",
            "attribute_text", "notes");
    public static final List<String> DRIVER_COLUMNS = List.of("employee_code", "licence_number", "licence_category", "licence_issue_date",
            "licence_expiry_date", "status", "notes");

    private final ServiceRegistry s;

    public CsvImportExportService(ServiceRegistry services) { this.s = services; }

    // ---- export -----------------------------------------------------------------------------------------

    public Path export(Entity entity, Path target) throws IOException {
        SessionContext.require(Permission.EXPORT_DATA);
        Files.createDirectories(target.toAbsolutePath().getParent());
        int rows;
        try (Writer w = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            rows = switch (entity) {
                case EMPLOYEES -> exportEmployees(w);
                case DRIVERS -> exportDrivers(w);
                case VEHICLES -> exportVehicles(w);
                case ASSIGNMENTS -> exportAssignments(w);
                case TRIPS -> exportTrips(w);
                case MAINTENANCE -> exportMaintenance(w);
                case FUEL -> exportFuel(w);
                case EXPENSES -> exportExpenses(w);
                case AUDIT_LOG -> exportAudit(w);
            };
        }
        s.audit.record("EXPORT", entity.name(), target.getFileName().toString(), rows + " rows exported to CSV");
        return target;
    }

    private int exportEmployees(Writer w) throws IOException {
        List<String> cols = new ArrayList<>(List.of("employee_code"));
        cols.addAll(EMPLOYEE_COLUMNS);
        CsvUtil.writeRow(w, cols.toArray());
        List<Employee> list = s.employeeRepository.findAll(false);
        for (Employee e : list) CsvUtil.writeRow(w, e.getEmployeeCode(), e.getFirstName(), e.getLastName(), e.getPhone(), e.getEmail(), e.getAddress(),
                e.getDepartment(), e.getPosition(), e.getDateOfBirth(), e.getHireDate(), e.getSalary(), e.getStatus(), e.getEmergencyContactName(),
                e.getEmergencyContactPhone(), e.getNotes());
        return list.size();
    }

    private int exportVehicles(Writer w) throws IOException {
        List<String> cols = new ArrayList<>(List.of("vehicle_code"));
        cols.addAll(VEHICLE_COLUMNS);
        CsvUtil.writeRow(w, cols.toArray());
        List<Vehicle> list = s.vehicleRepository.findAll();
        for (Vehicle v : list) CsvUtil.writeRow(w, v.getVehicleCode(), v.getRegistrationNumber(), v.getVehicleType().name(), v.getMake(), v.getModel(), v.getYear(),
                v.getColor(), v.getVin(), v.getEngineNumber(), v.getAcquisitionDate(), v.getAcquisitionCost(), v.getCurrentMileage(), v.getFuelType(),
                v.getCapacity(), v.getStatus(), v.getAttributeNumber(), v.getAttributeText(), v.getNotes());
        return list.size();
    }

    private int exportDrivers(Writer w) throws IOException {
        List<String> cols = new ArrayList<>(List.of("driver_code", "full_name"));
        cols.addAll(DRIVER_COLUMNS);
        CsvUtil.writeRow(w, cols.toArray());
        List<Driver> list = s.driverRepository.findAll();
        Map<Long, String> codes = new HashMap<>();
        for (Employee e : s.employeeRepository.findAll(true)) codes.put(e.getId(), e.getEmployeeCode());
        for (Driver d : list) CsvUtil.writeRow(w, d.getDriverCode(), d.getFullName(), codes.get(d.getEmployeeId()), d.getLicenceNumber(), d.getLicenceCategory(),
                d.getLicenceIssueDate(), d.getLicenceExpiryDate(), d.getStatus(), d.getNotes());
        return list.size();
    }

    private int exportAssignments(Writer w) throws IOException {
        CsvUtil.writeRow(w, "id", "vehicle", "driver", "assigned_at", "returned_at", "status", "purpose", "assigned_by", "notes");
        List<Assignment> list = s.assignmentRepository.findAll();
        for (Assignment a : list) CsvUtil.writeRow(w, a.getId(), a.getVehicleDisplay(), a.getDriverDisplay(), a.getAssignedAt(), a.getReturnedAt(), a.getStatus(),
                a.getPurpose(), a.getAssignedBy(), a.getNotes());
        return list.size();
    }

    private int exportTrips(Writer w) throws IOException {
        CsvUtil.writeRow(w, "trip_code", "vehicle", "driver", "origin", "destination", "purpose", "departure_time", "return_time", "start_mileage",
                "end_mileage", "distance_km", "fuel_used_litres", "status", "notes");
        List<Trip> list = s.tripRepository.findAll();
        for (Trip t : list) CsvUtil.writeRow(w, t.getTripCode(), t.getVehicleDisplay(), t.getDriverDisplay(), t.getOrigin(), t.getDestination(), t.getPurpose(),
                t.getDepartureTime(), t.getReturnTime(), t.getStartMileage(), t.getEndMileage(), t.getDistanceKm(), t.getFuelUsedLitres(), t.getStatus(), t.getNotes());
        return list.size();
    }

    private int exportMaintenance(Writer w) throws IOException {
        CsvUtil.writeRow(w, "id", "vehicle", "service_date", "type", "description", "provider", "cost", "mileage_at_service", "next_service_date",
                "next_service_mileage", "status", "notes");
        List<MaintenanceRecord> list = s.maintenanceRepository.findAll();
        for (MaintenanceRecord m : list) CsvUtil.writeRow(w, m.getId(), m.getVehicleDisplay(), m.getServiceDate(), m.getType(), m.getDescription(), m.getProvider(),
                m.getCost(), m.getMileageAtService(), m.getNextServiceDate(), m.getNextServiceMileage(), m.getStatus(), m.getNotes());
        return list.size();
    }

    private int exportFuel(Writer w) throws IOException {
        CsvUtil.writeRow(w, "id", "vehicle", "driver", "date", "quantity_litres", "unit_price", "total_cost", "mileage", "station", "receipt_reference", "fuel_type", "notes");
        List<FuelRecord> list = s.fuelRepository.findAll();
        for (FuelRecord f : list) CsvUtil.writeRow(w, f.getId(), f.getVehicleDisplay(), f.getDriverDisplay(), f.getDate(), f.getQuantityLitres(), f.getUnitPrice(),
                f.getTotalCost(), f.getMileage(), f.getStation(), f.getReceiptReference(), f.getFuelType(), f.getNotes());
        return list.size();
    }

    private int exportExpenses(Writer w) throws IOException {
        CsvUtil.writeRow(w, "id", "date", "category", "vehicle", "amount", "vendor", "description", "reference", "source", "notes");
        List<Expense> list = s.expenseRepository.findAll();
        for (Expense x : list) CsvUtil.writeRow(w, x.getId(), x.getDate(), x.getCategory(), x.getVehicleDisplay(), x.getAmount(), x.getVendor(), x.getDescription(),
                x.getReference(), x.getSourceFuelRecordId() != null ? "FUEL" : x.getSourceMaintenanceRecordId() != null ? "MAINTENANCE" : "MANUAL", x.getNotes());
        return list.size();
    }

    private int exportAudit(Writer w) throws IOException {
        SessionContext.require(Permission.VIEW_AUDIT_LOG);
        CsvUtil.writeRow(w, "timestamp", "username", "action", "module", "reference", "description");
        List<AuditLog> list = s.auditLogRepository.search(null, null, null, null, null, 100_000);
        for (AuditLog a : list) CsvUtil.writeRow(w, a.getTimestamp(), a.getUsername(), a.getAction(), a.getModule(), a.getReference(), a.getDescription());
        return list.size();
    }

    /** Writes a header-only template so users know the expected columns. */
    public Path writeTemplate(Entity entity, Path target) throws IOException {
        List<String> cols = switch (entity) {
            case EMPLOYEES -> EMPLOYEE_COLUMNS;
            case VEHICLES -> VEHICLE_COLUMNS;
            case DRIVERS -> DRIVER_COLUMNS;
            default -> throw new ValidationException("Import templates exist for employees, vehicles and drivers only.");
        };
        Files.createDirectories(target.toAbsolutePath().getParent());
        try (Writer w = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) { CsvUtil.writeRow(w, cols.toArray()); }
        return target;
    }

    // ---- import -----------------------------------------------------------------------------------------

    public record ImportResult(int imported, int skipped, List<String> errors) {
        public boolean hasErrors() { return !errors.isEmpty(); }
    }

    public ImportResult importFile(Entity entity, Path file) throws IOException {
        SessionContext.require(Permission.IMPORT_DATA);
        List<List<String>> rows;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { rows = CsvUtil.read(r); }
        if (rows.isEmpty()) throw new ValidationException("The file is empty.");
        Map<String, Integer> header = new HashMap<>();
        List<String> h = rows.get(0);
        for (int i = 0; i < h.size(); i++) header.put(h.get(i).trim().toLowerCase().replace(' ', '_'), i);

        int imported = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row.stream().allMatch(String::isBlank)) { skipped++; continue; }
            Function<String, String> col = name -> { Integer idx = header.get(name); return idx == null || idx >= row.size() ? null : blankToNull(row.get(idx)); };
            try {
                switch (entity) {
                    case EMPLOYEES -> importEmployee(col);
                    case VEHICLES -> importVehicle(col);
                    case DRIVERS -> importDriver(col);
                    default -> throw new ValidationException("Import is supported for employees, vehicles and drivers only.");
                }
                imported++;
            } catch (RuntimeException e) {
                errors.add("Line " + (i + 1) + ": " + e.getMessage());
            }
        }
        s.audit.record("IMPORT", entity.name(), file.getFileName().toString(), imported + " rows imported, " + errors.size() + " rejected");
        return new ImportResult(imported, skipped, errors);
    }

    private void importEmployee(Function<String, String> c) {
        Employee e = new Employee();
        e.setFirstName(c.apply("first_name"));
        e.setLastName(c.apply("last_name"));
        e.setPhone(c.apply("phone"));
        e.setEmail(c.apply("email"));
        e.setAddress(c.apply("address"));
        e.setDepartment(c.apply("department"));
        e.setPosition(c.apply("position"));
        e.setDateOfBirth(date(c.apply("date_of_birth")));
        e.setHireDate(date(c.apply("hire_date")));
        e.setSalary(dbl(c.apply("salary")));
        e.setStatus(enm(EmploymentStatus.class, c.apply("status"), EmploymentStatus.ACTIVE));
        e.setEmergencyContactName(c.apply("emergency_contact_name"));
        e.setEmergencyContactPhone(c.apply("emergency_contact_phone"));
        e.setNotes(c.apply("notes"));
        s.employees.create(e);
    }

    private void importVehicle(Function<String, String> c) {
        VehicleType type = enm(VehicleType.class, c.apply("type"), VehicleType.CAR);
        Vehicle v = Vehicle.newOfType(type);
        v.setRegistrationNumber(c.apply("registration_number"));
        v.setMake(c.apply("make"));
        v.setModel(c.apply("model"));
        Integer year = integer(c.apply("year"));
        v.setYear(year == null ? 0 : year);
        v.setColor(c.apply("color"));
        v.setVin(c.apply("vin"));
        v.setEngineNumber(c.apply("engine_number"));
        v.setAcquisitionDate(date(c.apply("acquisition_date")));
        Double cost = dbl(c.apply("acquisition_cost"));
        v.setAcquisitionCost(cost == null ? 0 : cost);
        Long km = lng(c.apply("current_mileage"));
        v.setCurrentMileage(km == null ? 0 : km);
        v.setFuelType(enm(FuelType.class, c.apply("fuel_type"), FuelType.PETROL));
        Integer cap = integer(c.apply("capacity"));
        v.setCapacity(cap == null ? 0 : cap);
        v.setStatus(enm(VehicleStatus.class, c.apply("status"), VehicleStatus.AVAILABLE));
        v.setAttributeNumber(dbl(c.apply("attribute_number")));
        v.setAttributeText(c.apply("attribute_text"));
        v.setNotes(c.apply("notes"));
        s.vehicles.create(v);
    }

    private void importDriver(Function<String, String> c) {
        String code = c.apply("employee_code");
        if (code == null) throw new ValidationException("employee_code is required.");
        Employee e = s.employeeRepository.findByCode(code).orElseThrow(() -> new ValidationException("No employee with code " + code));
        Driver d = new Driver();
        d.setEmployeeId(e.getId());
        d.setLicenceNumber(c.apply("licence_number"));
        d.setLicenceCategory(enm(LicenceCategory.class, c.apply("licence_category"), LicenceCategory.B));
        d.setLicenceIssueDate(date(c.apply("licence_issue_date")));
        d.setLicenceExpiryDate(date(c.apply("licence_expiry_date")));
        d.setStatus(enm(DriverStatus.class, c.apply("status"), DriverStatus.ACTIVE));
        d.setNotes(c.apply("notes"));
        s.drivers.create(d);
    }

    // ---- parsing helpers ----------------------------------------------------------------------------------

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private static LocalDate date(String s) {
        if (s == null) return null;
        try { return DateUtil.parseUserDate(s); } catch (RuntimeException e) { throw new ValidationException("Invalid date '" + s + "' (use yyyy-MM-dd)"); }
    }

    private static Double dbl(String s) {
        if (s == null) return null;
        try { return Double.parseDouble(s.replace(",", "").replace("\u20A6", "").trim()); } catch (NumberFormatException e) { throw new ValidationException("Invalid number '" + s + "'"); }
    }

    private static Long lng(String s) { Double d = dbl(s); return d == null ? null : d.longValue(); }
    private static Integer integer(String s) { Double d = dbl(s); return d == null ? null : d.intValue(); }

    private static <E extends Enum<E>> E enm(Class<E> type, String s, E dflt) {
        if (s == null) return dflt;
        String u = s.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        try { return Enum.valueOf(type, u); } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid " + type.getSimpleName() + " '" + s + "'. Allowed: " + Arrays.toString(type.getEnumConstants()));
        }
    }
}
