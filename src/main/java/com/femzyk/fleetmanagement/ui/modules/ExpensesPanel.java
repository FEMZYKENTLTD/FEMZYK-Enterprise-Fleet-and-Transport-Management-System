package com.femzyk.fleetmanagement.ui.modules;

import com.femzyk.fleetmanagement.model.*;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import com.femzyk.fleetmanagement.ui.ModulePanel;
import com.femzyk.fleetmanagement.ui.UiUtils;
import com.femzyk.fleetmanagement.ui.components.*;
import com.femzyk.fleetmanagement.util.DateUtil;
import com.femzyk.fleetmanagement.util.MoneyUtil;

import javax.swing.*;
import java.time.LocalDate;
import java.util.List;

public class ExpensesPanel implements ModulePanel {

    private final ServiceRegistry s;
    private final CrudTablePanel<Expense> table;
    private final JComboBox<Object> category;

    public ExpensesPanel(ServiceRegistry services) {
        this.s = services;
        List<EntityTableModel.Column<Expense>> cols = List.of(
                EntityTableModel.Column.of("Date", x -> DateUtil.display(x.getDate()), 90),
                EntityTableModel.Column.of("Category", Expense::getCategory, 110),
                EntityTableModel.Column.of("Vehicle", Expense::getVehicleDisplay, 170),
                EntityTableModel.Column.of("Description", Expense::getDescription, 240),
                EntityTableModel.Column.of("Vendor", Expense::getVendor, 130),
                EntityTableModel.Column.of("Amount", x -> MoneyUtil.format(x.getAmount()), 110),
                EntityTableModel.Column.of("Reference", Expense::getReference, 100),
                EntityTableModel.Column.of("Source", x -> x.getSourceFuelRecordId() != null ? "Fuel" : x.getSourceMaintenanceRecordId() != null ? "Maintenance" : "Manual", 90));
        table = new CrudTablePanel<>("Expenses", "All operating costs - manual entries plus those posted automatically from fuel and maintenance", cols);
        category = table.addFilter("Category:", new JComboBox<>());
        category.addItem("All categories");
        for (ExpenseCategory c : ExpenseCategory.values()) category.addItem(c);
        table.loader(() -> s.expenses.search(table.searchText(), category.getSelectedItem() instanceof ExpenseCategory c ? c : null, null, null, null, null, null));
        if (SessionContext.has(Permission.MANAGE_EXPENSES)) {
            table.addAction(UiUtils.primary("Add expense", () -> edit(null)));
            table.addAction(UiUtils.neutral("Edit", () -> { Expense x = table.requireSelected("expense"); if (x != null) edit(x); }));
            table.addAction(UiUtils.danger("Delete", this::delete));
            table.onDoubleClick(this::edit);
        }
        table.addAction(UiUtils.neutral("Totals", this::totals));
    }

    @Override public String id() { return "expenses"; }
    @Override public String title() { return "Expenses"; }
    @Override public JComponent component() { return table; }
    @Override public void refresh() { table.refresh(); }

    private void edit(Expense existing) {
        Expense x = existing == null ? new Expense() : s.expenses.getById(existing.getId());
        if (x.isSystemGenerated()) { UiUtils.info(table, "System-generated expense", "This expense comes from a " + (x.getSourceFuelRecordId() != null ? "fuel" : "maintenance") + " record. Edit that record instead."); return; }
        List<Vehicle> vehicles = s.vehicleRepository.findAll();
        FormBuilder f = new FormBuilder();
        Vehicle cur = vehicles.stream().filter(v -> v.getId().equals(x.getVehicleId())).findFirst().orElse(null);
        JTextField date = f.date("Date *", x.getDate() == null ? LocalDate.now() : x.getDate());
        JComboBox<ExpenseCategory> cat = f.combo("Category *", ExpenseCategory.values(), x.getCategory());
        JComboBox<Vehicle> v = f.combo("Vehicle (optional)", vehicles, cur, true);
        JTextField amount = f.number("Amount (\u20A6) *", x.getAmount() == 0 ? null : x.getAmount());
        JTextField desc = f.text("Description *", x.getDescription());
        JTextField vendor = f.text("Vendor", x.getVendor());
        JTextField ref = f.text("Reference / receipt no.", x.getReference());
        JTextArea notes = f.area("Notes", x.getNotes());
        boolean ok = new FormDialog(UiUtils.windowOf(table), existing == null ? "Add expense" : "Edit expense", f, "Save", () -> {
            x.setDate(f.date(date, "Date")); x.setCategory((ExpenseCategory) cat.getSelectedItem()); x.setVehicleId(v.getSelectedItem() instanceof Vehicle ve ? ve.getId() : null);
            x.setAmount(f.dblOrZero(amount, "Amount")); x.setDescription(FormBuilder.str(desc)); x.setVendor(FormBuilder.str(vendor)); x.setReference(FormBuilder.str(ref)); x.setNotes(FormBuilder.str(notes));
            f.throwIfParseErrors();
            if (existing == null) s.expenses.create(x); else s.expenses.update(x);
        }).showDialog();
        if (ok) refresh();
    }

    private void delete() {
        Expense x = table.requireSelected("expense");
        if (x != null && UiUtils.confirm(table, "Delete expense", "Delete this expense of " + MoneyUtil.format(x.getAmount()) + "?") && UiUtils.run(table, () -> s.expenses.delete(x.getId()))) refresh();
    }

    private void totals() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate yearStart = today.withDayOfYear(1);
        StringBuilder b = new StringBuilder();
        b.append("This month: ").append(MoneyUtil.format(s.expenses.total(monthStart, today))).append('\n');
        b.append("Year to date: ").append(MoneyUtil.format(s.expenses.total(yearStart, today))).append("\n\nYear to date by category:\n");
        s.expenses.totalByCategory(yearStart, today).forEach((k, v) -> b.append("   ").append(k).append(": ").append(MoneyUtil.format(v)).append('\n'));
        b.append("\nYear to date by vehicle:\n");
        s.expenses.totalByVehicle(yearStart, today).forEach((k, v) -> b.append("   ").append(k).append(": ").append(MoneyUtil.format(v)).append('\n'));
        UiUtils.info(table, "Expense totals", b.toString());
    }
}
