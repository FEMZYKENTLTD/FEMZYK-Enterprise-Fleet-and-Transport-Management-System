package com.femzyk.fleetmanagement.service;

import com.femzyk.fleetmanagement.model.*;
import com.femzyk.fleetmanagement.repository.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Aggregates live database figures for the dashboard. Nothing here is hard-coded. */
public class DashboardService {

    private final VehicleRepository vehicles;
    private final EmployeeRepository employees;
    private final DriverRepository drivers;
    private final TripRepository trips;
    private final AssignmentRepository assignments;
    private final MaintenanceService maintenanceService;
    private final DriverService driverService;
    private final FuelRepository fuel;
    private final MaintenanceRepository maintenance;
    private final ExpenseRepository expenses;

    public DashboardService(VehicleRepository vehicles, EmployeeRepository employees, DriverRepository drivers, TripRepository trips,
                            AssignmentRepository assignments, MaintenanceService maintenanceService, DriverService driverService,
                            FuelRepository fuel, MaintenanceRepository maintenance, ExpenseRepository expenses) {
        this.vehicles = vehicles;
        this.employees = employees;
        this.drivers = drivers;
        this.trips = trips;
        this.assignments = assignments;
        this.maintenanceService = maintenanceService;
        this.driverService = driverService;
        this.fuel = fuel;
        this.maintenance = maintenance;
        this.expenses = expenses;
    }

    public record Snapshot(long totalVehicles, long availableVehicles, long assignedVehicles, long inServiceVehicles,
                           long maintenanceVehicles, long outOfServiceVehicles, long retiredVehicles,
                           long totalEmployees, long activeEmployees, long totalDrivers, long activeDrivers,
                           long activeTrips, long plannedTrips, long completedTripsThisMonth, long activeAssignments,
                           long overdueMaintenance, long dueSoonMaintenance, long expiringLicences, long expiredLicences,
                           double monthFuelCost, double monthMaintenanceCost, double monthTotalExpenses,
                           double yearTotalExpenses, long monthDistanceKm,
                           Map<VehicleStatus, Long> vehiclesByStatus, Map<VehicleType, Long> vehiclesByType,
                           Map<String, Double> expensesByMonth, Map<String, Double> fuelByMonth,
                           Map<ExpenseCategory, Double> expensesByCategory, Map<String, Long> employeesByDepartment,
                           List<MaintenanceService.DueItem> maintenanceAlerts, List<Driver> licenceAlerts) {}

    public Snapshot snapshot() {
        LocalDate today = LocalDate.now();
        YearMonth month = YearMonth.from(today);
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        LocalDate yearStart = today.withDayOfYear(1);
        LocalDate sixMonthsAgo = month.minusMonths(5).atDay(1);

        Map<VehicleStatus, Long> byStatus = vehicles.countByStatus();
        List<MaintenanceService.DueItem> due = maintenanceService.dueAndOverdue();
        List<Driver> licence = driverService.licenceAlerts();

        Map<String, Double> expByMonth = fillMonths(expenses.totalByMonth(sixMonthsAgo, monthEnd), sixMonthsAgo, month);
        Map<String, Double> fuelByMonth = fillMonths(fuel.costByMonth(sixMonthsAgo, monthEnd), sixMonthsAgo, month);

        return new Snapshot(
                vehicles.count(null),
                byStatus.getOrDefault(VehicleStatus.AVAILABLE, 0L),
                byStatus.getOrDefault(VehicleStatus.ASSIGNED, 0L),
                byStatus.getOrDefault(VehicleStatus.IN_SERVICE, 0L),
                byStatus.getOrDefault(VehicleStatus.MAINTENANCE, 0L),
                byStatus.getOrDefault(VehicleStatus.OUT_OF_SERVICE, 0L),
                byStatus.getOrDefault(VehicleStatus.RETIRED, 0L),
                employees.count(null), employees.count(EmploymentStatus.ACTIVE),
                drivers.count(null), drivers.count(DriverStatus.ACTIVE),
                trips.count(Trip.Status.ACTIVE), trips.count(Trip.Status.PLANNED),
                trips.search(null, Trip.Status.COMPLETED, null, null, monthStart, monthEnd).size(),
                assignments.countActive(),
                due.stream().filter(d -> "OVERDUE".equals(d.dueStatus())).count(),
                due.stream().filter(d -> "DUE SOON".equals(d.dueStatus())).count(),
                licence.stream().filter(d -> !d.isLicenceExpired()).count(),
                licence.stream().filter(Driver::isLicenceExpired).count(),
                fuel.totalCost(monthStart, monthEnd),
                maintenance.totalCost(monthStart, monthEnd),
                expenses.total(monthStart, monthEnd),
                expenses.total(yearStart, monthEnd),
                trips.totalDistanceKm(monthStart, monthEnd),
                byStatus, vehicles.countByType(), expByMonth, fuelByMonth,
                expenses.totalByCategory(yearStart, monthEnd), employees.countByDepartment(),
                due, licence);
    }

    private static Map<String, Double> fillMonths(Map<String, Double> sparse, LocalDate from, YearMonth to) {
        Map<String, Double> out = new LinkedHashMap<>();
        YearMonth m = YearMonth.from(from);
        while (!m.isAfter(to)) {
            String key = m.toString();
            out.put(key, sparse.getOrDefault(key, 0.0));
            m = m.plusMonths(1);
        }
        return out;
    }
}
