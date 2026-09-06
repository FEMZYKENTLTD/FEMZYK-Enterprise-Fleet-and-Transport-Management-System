package com.femzyk.fleetmanagement.reporting;

import com.femzyk.fleetmanagement.config.AppConfig;
import com.femzyk.fleetmanagement.model.*;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import com.femzyk.fleetmanagement.util.DateUtil;
import com.femzyk.fleetmanagement.util.MoneyUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/** Builds all standard reports from live repository data and exports them via pluggable exporters. */
public class ReportService {

    public enum Kind {
        FLEET("Fleet Report"), EMPLOYEE("Employee Report"), DRIVER("Driver Report"), ASSIGNMENT("Assignment Report"),
        TRIP("Trip Report"), MAINTENANCE("Maintenance Report"), FUEL("Fuel Report"), EXPENSE("Expense Report"),
        MONTHLY_SUMMARY("Monthly Operations Summary"), AUDIT("Audit Trail Report");

        private final String title;
        Kind(String title) { this.title = title; }
        public String getTitle() { return title; }
        @Override public String toString() { return title; }
    }

    public enum Format { CSV, HTML, PDF }

    private final ServiceRegistry s;

    public ReportService(ServiceRegistry services) { this.s = services; }

    public Report build(Kind kind, LocalDate from, LocalDate to) {
        SessionContext.require(Permission.VIEW_REPORTS);
        LocalDate f = from == null ? LocalDate.of(2000, 1, 1) : from;
        LocalDate t = to == null ? LocalDate.now().plusYears(1) : to;
        String period = (from == null && to == null) ? "All time" : DateUtil.display(f) + " to " + DateUtil.display(t);
        return switch (kind) {
            case FLEET -> fleet(period);
            case EMPLOYEE -> employees(period);
            case DRIVER -> drivers(period);
            case ASSIGNMENT -> assignments(period);
            case TRIP -> trips(period, f, t);
            case MAINTENANCE -> maintenance(period, f, t);
            case FUEL -> fuel(period, f, t);
            case EXPENSE -> expenses(period, f, t);
            case MONTHLY_SUMMARY -> monthly(YearMonth.from(to == null ? LocalDate.now() : to));
            case AUDIT -> audit(period, f, t);
        };
    }

    public Path export(Report report, Format format, Path target) throws IOException {
        SessionContext.require(Permission.EXPORT_DATA);
        ReportExporter exporter = switch (format) {
            case CSV -> new CsvReportExporter();
            case HTML -> new HtmlReportExporter();
            case PDF -> new PdfReportExporter();
        };
        exporter.export(report, target);
        s.audit.record("EXPORT", "REPORT", report.getTitle(), format + " -> " + target.getFileName());
        return target;
    }

    public Path defaultTarget(Kind kind, Format format) {
        String name = kind.name().toLowerCase().replace('_', '-') + "-" + DateUtil.format(LocalDate.now()) + "." + format.name().toLowerCase();
        return AppConfig.getExportDirectory().resolve(name);
    }

    private Report newReport(Kind kind, String period) {
        return new Report(kind.getTitle(), period, SessionContext.currentUsername());
    }

    // ---- individual reports ---------------------------------------------------------------------------------------

    Report fleet(String period) {
        Report r = newReport(Kind.FLEET, period);
        List<Vehicle> list = s.vehicleRepository.findAll();
        Map<VehicleStatus, Long> byStatus = s.vehicleRepository.countByStatus();
        r.summary("Total vehicles", list.size());
        for (VehicleStatus st : VehicleStatus.values()) r.summary(st.getLabel(), byStatus.getOrDefault(st, 0L));
        r.summary("Total mileage (km)", String.format("%,d", list.stream().mapToLong(Vehicle::getCurrentMileage).sum()));
        r.summary("Fleet acquisition value", MoneyUtil.format(list.stream().mapToDouble(Vehicle::getAcquisitionCost).sum()));
        Report.Section sec = r.section("Vehicles", "Code", "Registration", "Type", "Make", "Model", "Year", "Fuel", "Status", "Mileage (km)", "Assigned driver", "Acquired");
        for (Vehicle v : list) sec.addRow(v.getVehicleCode(), v.getRegistrationNumber(), v.getVehicleType(), v.getMake(), v.getModel(), v.getYear(),
                v.getFuelType(), v.getStatus().getLabel(), String.format("%,d", v.getCurrentMileage()), nz(v.getAssignedDriverName()), DateUtil.display(v.getAcquisitionDate()));
        Report.Section types = r.section("Vehicles by type", "Type", "Count");
        s.vehicleRepository.countByType().forEach((k, v) -> types.addRow(k, v));
        return r;
    }

    Report employees(String period) {
        Report r = newReport(Kind.EMPLOYEE, period);
        List<Employee> list = s.employeeRepository.findAll(false);
        r.summary("Total employees", list.size());
        r.summary("Active", list.stream().filter(e -> e.getStatus() == EmploymentStatus.ACTIVE).count());
        r.summary("With driver profile", list.stream().filter(Employee::isDriver).count());
        double payroll = list.stream().filter(e -> e.getSalary() != null && e.getStatus().isWorking()).mapToDouble(Employee::getSalary).sum();
        r.summary("Monthly payroll (active)", MoneyUtil.format(payroll));
        Report.Section sec = r.section("Employees", "Code", "Name", "Department", "Position", "Phone", "Email", "Hire date", "Status", "Salary", "Driver");
        for (Employee e : list) sec.addRow(e.getEmployeeCode(), e.getFullName(), e.getDepartment(), e.getPosition(), e.getPhone(), e.getEmail(),
                DateUtil.display(e.getHireDate()), e.getStatus(), e.getSalary() == null ? "" : MoneyUtil.format(e.getSalary()), e.isDriver() ? "Yes" : "No");
        Report.Section dept = r.section("Headcount by department", "Department", "Employees");
        s.employeeRepository.countByDepartment().forEach((k, v) -> dept.addRow(k, v));
        return r;
    }

    Report drivers(String period) {
        Report r = newReport(Kind.DRIVER, period);
        List<Driver> list = s.driverRepository.findAll();
        r.summary("Total drivers", list.size());
        r.summary("Active", list.stream().filter(d -> d.getStatus() == DriverStatus.ACTIVE).count());
        r.summary("Licence expired", list.stream().filter(Driver::isLicenceExpired).count());
        r.summary("Licence expiring within " + AppConfig.LICENCE_EXPIRY_WARNING_DAYS + " days",
                list.stream().filter(d -> !d.isLicenceExpired() && d.isLicenceExpiringSoon()).count());
        Report.Section sec = r.section("Drivers", "Code", "Name", "Phone", "Licence no.", "Category", "Expiry", "Licence state", "Status", "Assigned vehicle");
        for (Driver d : list) sec.addRow(d.getDriverCode(), d.getFullName(), d.getPhone(), d.getLicenceNumber(), d.getLicenceCategory(),
                DateUtil.display(d.getLicenceExpiryDate()), d.isLicenceExpired() ? "EXPIRED" : d.isLicenceExpiringSoon() ? "EXPIRING SOON" : "Valid",
                d.getStatus(), nz(d.getAssignedVehicle()));
        return r;
    }

    Report assignments(String period) {
        Report r = newReport(Kind.ASSIGNMENT, period);
        List<Assignment> list = s.assignmentRepository.findAll();
        r.summary("Total assignments", list.size());
        r.summary("Active", list.stream().filter(a -> a.getStatus() == Assignment.Status.ACTIVE).count());
        r.summary("Returned", list.stream().filter(a -> a.getStatus() == Assignment.Status.RETURNED).count());
        Report.Section sec = r.section("Assignments", "ID", "Vehicle", "Driver", "Assigned at", "Returned at", "Status", "Purpose", "Assigned by");
        for (Assignment a : list) sec.addRow(a.getId(), a.getVehicleDisplay(), a.getDriverDisplay(), DateUtil.display(a.getAssignedAt()),
                DateUtil.display(a.getReturnedAt()), a.getStatus(), nz(a.getPurpose()), nz(a.getAssignedBy()));
        return r;
    }

    Report trips(String period, LocalDate f, LocalDate t) {
        Report r = newReport(Kind.TRIP, period);
        List<Trip> list = s.tripRepository.search(null, null, null, null, f, t);
        long completed = list.stream().filter(x -> x.getStatus() == Trip.Status.COMPLETED).count();
        long distance = list.stream().filter(x -> x.getDistanceKm() != null).mapToLong(Trip::getDistanceKm).sum();
        r.summary("Trips in period", list.size());
        r.summary("Completed", completed);
        r.summary("Active", list.stream().filter(x -> x.getStatus() == Trip.Status.ACTIVE).count());
        r.summary("Cancelled", list.stream().filter(x -> x.getStatus() == Trip.Status.CANCELLED).count());
        r.summary("Total distance (km)", String.format("%,d", distance));
        r.summary("Average distance per completed trip (km)", completed == 0 ? "n/a" : String.format("%,.1f", (double) distance / completed));
        Report.Section sec = r.section("Trips", "Code", "Vehicle", "Driver", "From", "To", "Departure", "Return", "Start km", "End km", "Distance", "Status");
        for (Trip x : list) sec.addRow(x.getTripCode(), x.getVehicleDisplay(), x.getDriverDisplay(), x.getOrigin(), x.getDestination(),
                DateUtil.display(x.getDepartureTime()), DateUtil.display(x.getReturnTime()), x.getStartMileage(), x.getEndMileage(), x.getDistanceKm(), x.getStatus());
        return r;
    }

    Report maintenance(String period, LocalDate f, LocalDate t) {
        Report r = newReport(Kind.MAINTENANCE, period);
        List<MaintenanceRecord> list = s.maintenanceRepository.search(null, null, null, null, f, t);
        r.summary("Records in period", list.size());
        r.summary("Total cost", MoneyUtil.format(s.maintenanceRepository.totalCost(f, t)));
        r.summary("Overdue services (now)", s.maintenance.countOverdue());
        Report.Section sec = r.section("Maintenance records", "Date", "Vehicle", "Type", "Description", "Provider", "Cost", "Mileage", "Next due date", "Next due km", "Status");
        for (MaintenanceRecord m : list) sec.addRow(DateUtil.display(m.getServiceDate()), m.getVehicleDisplay(), m.getType(), m.getDescription(), nz(m.getProvider()),
                MoneyUtil.format(m.getCost()), m.getMileageAtService(), DateUtil.display(m.getNextServiceDate()), m.getNextServiceMileage(), m.getStatus());
        Report.Section byV = r.section("Cost by vehicle", "Vehicle", "Cost");
        s.maintenanceRepository.costByVehicle(f, t).forEach((k, v) -> byV.addRow(k, MoneyUtil.format(v)));
        Report.Section due = r.section("Due / overdue", "Vehicle", "Last service", "Next due date", "Next due km", "Current km", "State");
        for (var d : s.maintenance.dueAndOverdue()) due.addRow(d.record().getVehicleDisplay(), DateUtil.display(d.record().getServiceDate()),
                DateUtil.display(d.record().getNextServiceDate()), d.record().getNextServiceMileage(), d.record().getVehicleCurrentMileage(), d.dueStatus());
        return r;
    }

    Report fuel(String period, LocalDate f, LocalDate t) {
        Report r = newReport(Kind.FUEL, period);
        List<FuelRecord> list = s.fuelRepository.search(null, null, f, t);
        double litres = s.fuelRepository.totalLitres(f, t), cost = s.fuelRepository.totalCost(f, t);
        r.summary("Transactions", list.size());
        r.summary("Total litres", String.format("%,.2f", litres));
        r.summary("Total cost", MoneyUtil.format(cost));
        r.summary("Average price per litre", litres == 0 ? "n/a" : MoneyUtil.format(cost / litres));
        Report.Section sec = r.section("Fuel transactions", "Date", "Vehicle", "Driver", "Litres", "Unit price", "Total", "Odometer", "Station", "Receipt");
        for (FuelRecord x : list) sec.addRow(DateUtil.display(x.getDate()), x.getVehicleDisplay(), nz(x.getDriverDisplay()), String.format("%.2f", x.getQuantityLitres()),
                MoneyUtil.format(x.getUnitPrice()), MoneyUtil.format(x.getTotalCost()), x.getMileage(), nz(x.getStation()), nz(x.getReceiptReference()));
        Report.Section byV = r.section("Cost by vehicle", "Vehicle", "Cost");
        s.fuelRepository.costByVehicle(f, t).forEach((k, v) -> byV.addRow(k, MoneyUtil.format(v)));
        Report.Section eff = r.section("Fuel efficiency (vehicles with 2+ odometer readings)", "Vehicle", "Intervals", "Distance (km)", "Litres", "km/L", "L/100km");
        for (Vehicle v : s.vehicleRepository.findAll()) {
            s.fuel.efficiencyForVehicle(v.getId()).ifPresent(e -> eff.addRow(v.getDisplayName(), e.intervals(), String.format("%,d", e.distanceKm()),
                    String.format("%.1f", e.litres()), String.format("%.2f", e.kmPerLitre()), String.format("%.2f", e.litresPer100Km())));
        }
        r.note("Efficiency figures are computed only from consecutive fuel records that both carry odometer readings.");
        return r;
    }

    Report expenses(String period, LocalDate f, LocalDate t) {
        Report r = newReport(Kind.EXPENSE, period);
        List<Expense> list = s.expenseRepository.search(null, null, null, f, t, null, null);
        r.summary("Expense entries", list.size());
        r.summary("Total", MoneyUtil.format(s.expenseRepository.total(f, t)));
        Report.Section cat = r.section("Total by category", "Category", "Amount");
        s.expenseRepository.totalByCategory(f, t).forEach((k, v) -> cat.addRow(k, MoneyUtil.format(v)));
        Report.Section byV = r.section("Total by vehicle", "Vehicle", "Amount");
        s.expenseRepository.totalByVehicle(f, t).forEach((k, v) -> byV.addRow(k, MoneyUtil.format(v)));
        Report.Section sec = r.section("Expense entries", "Date", "Category", "Vehicle", "Description", "Vendor", "Amount", "Reference", "Source");
        for (Expense x : list) sec.addRow(DateUtil.display(x.getDate()), x.getCategory(), nz(x.getVehicleDisplay()), x.getDescription(), nz(x.getVendor()),
                MoneyUtil.format(x.getAmount()), nz(x.getReference()), x.getSourceFuelRecordId() != null ? "Fuel record" : x.getSourceMaintenanceRecordId() != null ? "Maintenance record" : "Manual");
        return r;
    }

    Report monthly(YearMonth month) {
        LocalDate f = month.atDay(1), t = month.atEndOfMonth();
        Report r = newReport(Kind.MONTHLY_SUMMARY, month.getMonth() + " " + month.getYear());
        List<Trip> trips = s.tripRepository.search(null, null, null, null, f, t);
        r.summary("Vehicles in fleet", s.vehicleRepository.count(null));
        r.summary("Active employees", s.employeeRepository.count(EmploymentStatus.ACTIVE));
        r.summary("Active drivers", s.driverRepository.count(DriverStatus.ACTIVE));
        r.summary("Trips (all statuses)", trips.size());
        r.summary("Trips completed", trips.stream().filter(x -> x.getStatus() == Trip.Status.COMPLETED).count());
        r.summary("Distance travelled (km)", String.format("%,d", s.tripRepository.totalDistanceKm(f, t)));
        r.summary("Fuel purchased (L)", String.format("%,.2f", s.fuelRepository.totalLitres(f, t)));
        r.summary("Fuel cost", MoneyUtil.format(s.fuelRepository.totalCost(f, t)));
        r.summary("Maintenance cost", MoneyUtil.format(s.maintenanceRepository.totalCost(f, t)));
        r.summary("Total expenses", MoneyUtil.format(s.expenseRepository.total(f, t)));
        r.summary("Assignments currently active", s.assignmentRepository.countActive());
        r.summary("Maintenance overdue (now)", s.maintenance.countOverdue());
        Report.Section cat = r.section("Expenses by category", "Category", "Amount");
        s.expenseRepository.totalByCategory(f, t).forEach((k, v) -> cat.addRow(k, MoneyUtil.format(v)));
        Report.Section byV = r.section("Operating cost by vehicle", "Vehicle", "Amount");
        s.expenseRepository.totalByVehicle(f, t).forEach((k, v) -> byV.addRow(k, MoneyUtil.format(v)));
        Report.Section st = r.section("Fleet status at generation time", "Status", "Vehicles");
        s.vehicleRepository.countByStatus().forEach((k, v) -> st.addRow(k.getLabel(), v));
        Report.Section trend = r.section("Six-month expense trend", "Month", "Total expenses", "Fuel");
        Map<String, Double> exp = s.expenseRepository.totalByMonth(month.minusMonths(5).atDay(1), t);
        Map<String, Double> fuel = s.fuelRepository.costByMonth(month.minusMonths(5).atDay(1), t);
        for (YearMonth m = month.minusMonths(5); !m.isAfter(month); m = m.plusMonths(1))
            trend.addRow(m, MoneyUtil.format(exp.getOrDefault(m.toString(), 0.0)), MoneyUtil.format(fuel.getOrDefault(m.toString(), 0.0)));
        return r;
    }

    Report audit(String period, LocalDate f, LocalDate t) {
        SessionContext.require(Permission.VIEW_AUDIT_LOG);
        Report r = newReport(Kind.AUDIT, period);
        List<AuditLog> list = s.auditLogRepository.search(null, null, null, f, t, 5000);
        r.summary("Entries", list.size());
        Report.Section sec = r.section("Audit entries", "Timestamp", "User", "Action", "Module", "Reference", "Description");
        for (AuditLog a : list) sec.addRow(DateUtil.format(a.getTimestamp()), a.getUsername(), a.getAction(), a.getModule(), nz(a.getReference()), a.getDescription());
        return r;
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
