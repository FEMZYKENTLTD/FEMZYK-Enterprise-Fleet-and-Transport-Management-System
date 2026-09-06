package com.femzyk.fleetmanagement.service;

import com.femzyk.fleetmanagement.audit.AuditService;
import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.exception.RecordNotFoundException;
import com.femzyk.fleetmanagement.model.*;
import com.femzyk.fleetmanagement.repository.ExpenseRepository;
import com.femzyk.fleetmanagement.repository.FuelRepository;
import com.femzyk.fleetmanagement.repository.VehicleRepository;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.util.MoneyUtil;
import com.femzyk.fleetmanagement.validation.Validators;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fuel transactions. Each fuel record automatically maintains a linked FUEL expense.
 * Efficiency analysis is only reported when consecutive odometer readings exist (no invented figures).
 */
public class FuelService {

    private final DatabaseManager db;
    private final FuelRepository fuel;
    private final VehicleRepository vehicles;
    private final ExpenseRepository expenses;
    private final AuditService audit;

    public FuelService(DatabaseManager db, FuelRepository fuel, VehicleRepository vehicles, ExpenseRepository expenses, AuditService audit) {
        this.db = db;
        this.fuel = fuel;
        this.vehicles = vehicles;
        this.expenses = expenses;
        this.audit = audit;
    }

    public List<FuelRecord> findAll() {
        SessionContext.require(Permission.VIEW_RECORDS);
        return fuel.findAll();
    }

    public List<FuelRecord> search(String text, Long vehicleId, LocalDate from, LocalDate to) {
        SessionContext.require(Permission.VIEW_RECORDS);
        return fuel.search(text, vehicleId, from, to);
    }

    public Optional<FuelRecord> findById(long id) { return fuel.findById(id); }

    public FuelRecord getById(long id) {
        return fuel.findById(id).orElseThrow(() -> new RecordNotFoundException("Fuel record", id));
    }

    public FuelRecord create(FuelRecord f) {
        SessionContext.require(Permission.MANAGE_FUEL);
        f.computeTotal();
        validate(f);
        Vehicle v = vehicles.findById(f.getVehicleId()).orElseThrow(() -> new RecordNotFoundException("Vehicle", f.getVehicleId()));
        if (f.getFuelType() == null) f.setFuelType(v.getFuelType());
        return db.inTransaction(c -> {
            fuel.save(f);
            syncExpense(f, v);
            if (f.getMileage() != null) vehicles.updateMileageIfGreater(v.getId(), f.getMileage());
            audit.record("CREATE", "FUEL", v.getRegistrationNumber(), String.format("%.2f L @ %s = %s", f.getQuantityLitres(),
                    MoneyUtil.format(f.getUnitPrice()), MoneyUtil.format(f.getTotalCost())));
            return fuel.findById(f.getId()).orElse(f);
        });
    }

    public FuelRecord update(FuelRecord f) {
        SessionContext.require(Permission.MANAGE_FUEL);
        getById(f.getId());
        f.computeTotal();
        validate(f);
        Vehicle v = vehicles.findById(f.getVehicleId()).orElseThrow(() -> new RecordNotFoundException("Vehicle", f.getVehicleId()));
        return db.inTransaction(c -> {
            fuel.save(f);
            syncExpense(f, v);
            if (f.getMileage() != null) vehicles.updateMileageIfGreater(v.getId(), f.getMileage());
            audit.record("UPDATE", "FUEL", v.getRegistrationNumber(), "Fuel record updated: " + MoneyUtil.format(f.getTotalCost()));
            return fuel.findById(f.getId()).orElse(f);
        });
    }

    public void delete(long id) {
        SessionContext.require(Permission.MANAGE_FUEL);
        FuelRecord f = getById(id);
        db.inTransaction(c -> {
            expenses.findByFuelRecord(id).ifPresent(x -> expenses.delete(x.getId()));
            fuel.delete(id);
            audit.record("DELETE", "FUEL", f.getVehicleDisplay(), "Deleted fuel record of " + MoneyUtil.format(f.getTotalCost()));
        });
    }

    private void syncExpense(FuelRecord f, Vehicle v) {
        Expense x = expenses.findByFuelRecord(f.getId()).orElseGet(Expense::new);
        x.setVehicleId(v.getId());
        x.setDate(f.getDate());
        x.setCategory(ExpenseCategory.FUEL);
        x.setAmount(f.getTotalCost());
        x.setVendor(f.getStation());
        x.setDescription(String.format("Fuel %.2f L", f.getQuantityLitres()));
        x.setReference(f.getReceiptReference());
        x.setSourceFuelRecordId(f.getId());
        expenses.save(x);
    }

    // ---- analytics ---------------------------------------------------------------------------------

    public double totalCost(LocalDate from, LocalDate to) { return fuel.totalCost(from, to); }
    public double totalLitres(LocalDate from, LocalDate to) { return fuel.totalLitres(from, to); }
    public Map<String, Double> costByVehicle(LocalDate from, LocalDate to) { return fuel.costByVehicle(from, to); }
    public Map<String, Double> costByMonth(LocalDate from, LocalDate to) { return fuel.costByMonth(from, to); }

    public double averageUnitPrice(LocalDate from, LocalDate to) {
        double litres = totalLitres(from, to);
        return litres <= 0 ? 0 : totalCost(from, to) / litres;
    }

    /**
     * Fuel-efficiency estimate for a vehicle based on consecutive fill-ups that both carry odometer readings.
     * Returns empty when fewer than two usable readings exist.
     */
    public Optional<EfficiencySummary> efficiencyForVehicle(long vehicleId) {
        List<FuelRecord> records = fuel.findForVehicleChronological(vehicleId).stream().filter(r -> r.getMileage() != null).toList();
        if (records.size() < 2) return Optional.empty();
        long distance = 0;
        double litres = 0;
        int intervals = 0;
        List<String> warnings = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            long d = records.get(i).getMileage() - records.get(i - 1).getMileage();
            if (d <= 0) {
                warnings.add("Non-increasing odometer between " + records.get(i - 1).getDate() + " and " + records.get(i).getDate());
                continue;
            }
            distance += d;
            litres += records.get(i).getQuantityLitres();
            intervals++;
        }
        if (intervals == 0 || litres <= 0) return Optional.empty();
        return Optional.of(new EfficiencySummary(vehicleId, intervals, distance, litres, distance / litres, (litres / distance) * 100, warnings));
    }

    public record EfficiencySummary(long vehicleId, int intervals, long distanceKm, double litres,
                                    double kmPerLitre, double litresPer100Km, List<String> warnings) {}

    void validate(FuelRecord f) {
        Validators.errors()
                .required(f.getVehicleId(), "Vehicle")
                .required(f.getDate(), "Date")
                .positive(f.getQuantityLitres(), "Quantity (litres)")
                .nonNegative(f.getUnitPrice(), "Unit price")
                .nonNegative(f.getMileage(), "Mileage")
                .notFuture(f.getDate(), "Date")
                .check(f.getQuantityLitres() <= 2000, "Quantity above 2,000 litres is not plausible for one transaction.")
                .maxLength(f.getNotes(), 2000, "Notes")
                .throwIfAny();
    }
}
