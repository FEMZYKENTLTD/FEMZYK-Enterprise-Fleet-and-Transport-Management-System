package com.femzyk.fleetmanagement.service;

import com.femzyk.fleetmanagement.TestSupport;
import com.femzyk.fleetmanagement.exception.BusinessRuleException;
import com.femzyk.fleetmanagement.exception.ValidationException;
import com.femzyk.fleetmanagement.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MaintenanceFuelExpenseServiceTest {

    ServiceRegistry s;
    Vehicle car;

    @BeforeEach void setUp() {
        s = TestSupport.freshRegistryLoggedInAsAdmin();
        car = Fixtures.car(s, "CAR-001-AA", 50000);
    }

    private MaintenanceRecord maint(MaintenanceRecord.Status st, double cost, LocalDate nextDate, Long nextKm) {
        MaintenanceRecord m = new MaintenanceRecord();
        m.setVehicleId(car.getId()); m.setServiceDate(LocalDate.now().minusDays(30)); m.setType(MaintenanceRecord.Type.ROUTINE_SERVICE);
        m.setDescription("Service"); m.setCost(cost); m.setMileageAtService(50000L); m.setStatus(st); m.setNextServiceDate(nextDate); m.setNextServiceMileage(nextKm);
        return s.maintenance.create(m);
    }

    private FuelRecord fuel(LocalDate date, double litres, double price, Long km) {
        FuelRecord f = new FuelRecord();
        f.setVehicleId(car.getId()); f.setDate(date); f.setQuantityLitres(litres); f.setUnitPrice(price); f.setMileage(km);
        return s.fuel.create(f);
    }

    @Test void completedMaintenancePostsLinkedExpenseAndSyncsOnUpdateAndDelete() {
        MaintenanceRecord m = maint(MaintenanceRecord.Status.COMPLETED, 45000, null, null);
        Optional<Expense> x = s.expenseRepository.findByMaintenanceRecord(m.getId());
        assertTrue(x.isPresent());
        assertEquals(45000, x.get().getAmount());
        assertEquals(ExpenseCategory.MAINTENANCE, x.get().getCategory());
        assertThrows(BusinessRuleException.class, () -> s.expenses.update(x.get()), "system expense cannot be edited directly");
        assertThrows(BusinessRuleException.class, () -> s.expenses.delete(x.get().getId()));
        m.setCost(50000); s.maintenance.update(m);
        assertEquals(50000, s.expenseRepository.findByMaintenanceRecord(m.getId()).orElseThrow().getAmount());
        s.maintenance.delete(m.getId());
        assertTrue(s.expenseRepository.findByMaintenanceRecord(m.getId()).isEmpty());
    }

    @Test void inProgressMaintenanceMovesVehicleAndCompletionReleasesIt() {
        MaintenanceRecord m = maint(MaintenanceRecord.Status.IN_PROGRESS, 0, null, null);
        assertEquals(VehicleStatus.MAINTENANCE, s.vehicles.getById(car.getId()).getStatus());
        m.setStatus(MaintenanceRecord.Status.COMPLETED); s.maintenance.update(m);
        assertEquals(VehicleStatus.AVAILABLE, s.vehicles.getById(car.getId()).getStatus());
    }

    @Test void dueAndOverdueDetection() {
        maint(MaintenanceRecord.Status.COMPLETED, 100, LocalDate.now().minusDays(3), null);
        assertEquals(1, s.maintenance.countOverdue());
        Vehicle other = Fixtures.car(s, "CAR-002-AA", 80000);
        MaintenanceRecord m = new MaintenanceRecord();
        m.setVehicleId(other.getId()); m.setServiceDate(LocalDate.now().minusMonths(2)); m.setType(MaintenanceRecord.Type.REPAIR); m.setDescription("x");
        m.setMileageAtService(70000L); m.setNextServiceMileage(75000L); m.setStatus(MaintenanceRecord.Status.COMPLETED);
        s.maintenance.create(m);
        assertEquals(2, s.maintenance.countOverdue(), "mileage-based overdue");
        assertThrows(ValidationException.class, () -> maint(MaintenanceRecord.Status.COMPLETED, -1, null, null));
    }

    @Test void fuelComputesTotalPostsExpenseAndUpdatesMileage() {
        FuelRecord f = fuel(LocalDate.now().minusDays(2), 40, 1000, 50100L);
        assertEquals(40000, f.getTotalCost());
        Expense x = s.expenseRepository.findByFuelRecord(f.getId()).orElseThrow();
        assertEquals(ExpenseCategory.FUEL, x.getCategory());
        assertEquals(50100, s.vehicles.getById(car.getId()).getCurrentMileage());
        assertThrows(ValidationException.class, () -> fuel(LocalDate.now().plusDays(1), 10, 100, null), "future date");
        assertThrows(ValidationException.class, () -> fuel(LocalDate.now(), 0, 100, null), "zero litres");
        s.fuel.delete(f.getId());
        assertTrue(s.expenseRepository.findByFuelRecord(f.getId()).isEmpty());
    }

    @Test void fuelEfficiencyNeedsTwoOdometerReadings() {
        fuel(LocalDate.now().minusDays(10), 40, 1000, 50000L);
        assertTrue(s.fuel.efficiencyForVehicle(car.getId()).isEmpty());
        fuel(LocalDate.now().minusDays(5), 40, 1000, 50400L);
        FuelService.EfficiencySummary e = s.fuel.efficiencyForVehicle(car.getId()).orElseThrow();
        assertEquals(400, e.distanceKm());
        assertEquals(10.0, e.kmPerLitre(), 0.001);
        assertEquals(10.0, e.litresPer100Km(), 0.001);
    }

    @Test void expenseTotalsByCategoryAndMonth() {
        Expense x = new Expense();
        x.setDate(LocalDate.now()); x.setCategory(ExpenseCategory.TOLLS); x.setAmount(1500); x.setDescription("toll"); x.setVehicleId(car.getId());
        s.expenses.create(x);
        fuel(LocalDate.now(), 10, 1000, null);
        LocalDate from = LocalDate.now().withDayOfMonth(1), to = LocalDate.now();
        assertEquals(11500, s.expenses.total(from, to), 0.001);
        assertEquals(1500, s.expenses.totalByCategory(from, to).get(ExpenseCategory.TOLLS), 0.001);
        assertEquals(11500, s.expenses.totalByMonth(from, to).values().stream().mapToDouble(Double::doubleValue).sum(), 0.001);
        assertThrows(ValidationException.class, () -> { Expense bad = new Expense(); bad.setDate(LocalDate.now()); bad.setAmount(0); bad.setDescription("x"); s.expenses.create(bad); });
    }
}
