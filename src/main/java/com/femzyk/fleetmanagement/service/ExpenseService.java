package com.femzyk.fleetmanagement.service;

import com.femzyk.fleetmanagement.audit.AuditService;
import com.femzyk.fleetmanagement.exception.BusinessRuleException;
import com.femzyk.fleetmanagement.exception.RecordNotFoundException;
import com.femzyk.fleetmanagement.model.Expense;
import com.femzyk.fleetmanagement.model.ExpenseCategory;
import com.femzyk.fleetmanagement.model.Permission;
import com.femzyk.fleetmanagement.repository.ExpenseRepository;
import com.femzyk.fleetmanagement.repository.VehicleRepository;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.util.MoneyUtil;
import com.femzyk.fleetmanagement.validation.Validators;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExpenseService {

    private final ExpenseRepository expenses;
    private final VehicleRepository vehicles;
    private final AuditService audit;

    public ExpenseService(ExpenseRepository expenses, VehicleRepository vehicles, AuditService audit) {
        this.expenses = expenses;
        this.vehicles = vehicles;
        this.audit = audit;
    }

    public List<Expense> findAll() {
        SessionContext.require(Permission.VIEW_RECORDS);
        return expenses.findAll();
    }

    public List<Expense> search(String text, ExpenseCategory category, Long vehicleId, LocalDate from, LocalDate to, Double min, Double max) {
        SessionContext.require(Permission.VIEW_RECORDS);
        return expenses.search(text, category, vehicleId, from, to, min, max);
    }

    public Optional<Expense> findById(long id) { return expenses.findById(id); }

    public Expense getById(long id) {
        return expenses.findById(id).orElseThrow(() -> new RecordNotFoundException("Expense", id));
    }

    public Expense create(Expense x) {
        SessionContext.require(Permission.MANAGE_EXPENSES);
        validate(x);
        if (x.getVehicleId() != null) vehicles.findById(x.getVehicleId()).orElseThrow(() -> new RecordNotFoundException("Vehicle", x.getVehicleId()));
        x.setAmount(MoneyUtil.round(x.getAmount()));
        expenses.save(x);
        audit.record("CREATE", "EXPENSE", x.getReference() == null ? String.valueOf(x.getId()) : x.getReference(),
                x.getCategory() + " " + MoneyUtil.format(x.getAmount()) + " - " + x.getDescription());
        return expenses.findById(x.getId()).orElse(x);
    }

    public Expense update(Expense x) {
        SessionContext.require(Permission.MANAGE_EXPENSES);
        Expense existing = getById(x.getId());
        if (existing.isSystemGenerated()) {
            throw new BusinessRuleException("This expense was generated from a fuel or maintenance record. Edit the source record instead.");
        }
        validate(x);
        x.setAmount(MoneyUtil.round(x.getAmount()));
        expenses.save(x);
        audit.record("UPDATE", "EXPENSE", String.valueOf(x.getId()), x.getCategory() + " " + MoneyUtil.format(x.getAmount()) + " - " + x.getDescription());
        return expenses.findById(x.getId()).orElse(x);
    }

    public void delete(long id) {
        SessionContext.require(Permission.MANAGE_EXPENSES);
        Expense x = getById(id);
        if (x.isSystemGenerated()) {
            throw new BusinessRuleException("This expense was generated from a fuel or maintenance record. Delete the source record instead.");
        }
        expenses.delete(id);
        audit.record("DELETE", "EXPENSE", String.valueOf(id), "Deleted " + x.getCategory() + " expense of " + MoneyUtil.format(x.getAmount()));
    }

    public double total(LocalDate from, LocalDate to) { return expenses.total(from, to); }
    public Map<ExpenseCategory, Double> totalByCategory(LocalDate from, LocalDate to) { return expenses.totalByCategory(from, to); }
    public Map<String, Double> totalByVehicle(LocalDate from, LocalDate to) { return expenses.totalByVehicle(from, to); }
    public Map<String, Double> totalByMonth(LocalDate from, LocalDate to) { return expenses.totalByMonth(from, to); }
    public Map<String, Map<ExpenseCategory, Double>> totalByMonthAndCategory(LocalDate from, LocalDate to) { return expenses.totalByMonthAndCategory(from, to); }

    void validate(Expense x) {
        Validators.errors()
                .required(x.getDate(), "Date")
                .required(x.getCategory(), "Category")
                .required(x.getDescription(), "Description")
                .check(x.getAmount() > 0, "Amount must be greater than zero.")
                .check(x.getAmount() <= 100_000_000, "Amount is implausibly large.")
                .notFuture(x.getDate(), "Date")
                .maxLength(x.getNotes(), 2000, "Notes")
                .throwIfAny();
    }
}
