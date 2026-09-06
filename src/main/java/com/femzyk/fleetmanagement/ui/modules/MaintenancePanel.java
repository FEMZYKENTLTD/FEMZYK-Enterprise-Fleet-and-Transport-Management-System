package com.femzyk.fleetmanagement.ui.modules;

import com.femzyk.fleetmanagement.model.*;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.service.MaintenanceService;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import com.femzyk.fleetmanagement.ui.ModulePanel;
import com.femzyk.fleetmanagement.ui.UiUtils;
import com.femzyk.fleetmanagement.ui.components.*;
import com.femzyk.fleetmanagement.util.DateUtil;
import com.femzyk.fleetmanagement.util.MoneyUtil;

import javax.swing.*;
import java.time.LocalDate;
import java.util.List;

public class MaintenancePanel implements ModulePanel {

    private final ServiceRegistry s;
    private final CrudTablePanel<MaintenanceRecord> table;
    private final JComboBox<Object> status;
    private final JComboBox<Object> type;

    public MaintenancePanel(ServiceRegistry services) {
        this.s = services;
        List<EntityTableModel.Column<MaintenanceRecord>> cols = List.of(
                EntityTableModel.Column.of("Date", m -> DateUtil.display(m.getServiceDate()), 90),
                EntityTableModel.Column.of("Vehicle", MaintenanceRecord::getVehicleDisplay, 170),
                EntityTableModel.Column.of("Type", MaintenanceRecord::getType, 110),
                EntityTableModel.Column.of("Description", MaintenanceRecord::getDescription, 220),
                EntityTableModel.Column.of("Provider", MaintenanceRecord::getProvider, 130),
                EntityTableModel.Column.of("Cost", m -> MoneyUtil.format(m.getCost()), 100),
                EntityTableModel.Column.of("Mileage", m -> m.getMileageAtService() == null ? "" : String.format("%,d", m.getMileageAtService()), 80),
                EntityTableModel.Column.of("Next due", m -> DateUtil.display(m.getNextServiceDate()) + (m.getNextServiceMileage() == null ? "" : " / " + String.format("%,d km", m.getNextServiceMileage())), 150),
                EntityTableModel.Column.of("Due state", m -> m.getDueStatus(LocalDate.now(), m.getVehicleCurrentMileage()), 90),
                EntityTableModel.Column.of("Status", MaintenanceRecord::getStatus, 100));
        table = new CrudTablePanel<>("Maintenance", "Service history, repairs and upcoming due dates (completed jobs post an expense automatically)", cols);
        table.setRenderer(8, new StatusCellRenderer());
        table.setRenderer(9, new StatusCellRenderer());
        type = table.addFilter("Type:", new JComboBox<>());
        type.addItem("All types");
        for (MaintenanceRecord.Type t : MaintenanceRecord.Type.values()) type.addItem(t);
        status = table.addFilter("Status:", new JComboBox<>());
        status.addItem("All");
        for (MaintenanceRecord.Status st : MaintenanceRecord.Status.values()) status.addItem(st);
        table.loader(() -> s.maintenance.search(table.searchText(), type.getSelectedItem() instanceof MaintenanceRecord.Type t ? t : null,
                status.getSelectedItem() instanceof MaintenanceRecord.Status st ? st : null, null, null, null));
        if (SessionContext.has(Permission.MANAGE_MAINTENANCE)) {
            table.addAction(UiUtils.primary("Log maintenance", () -> edit(null)));
            table.addAction(UiUtils.neutral("Edit", () -> { MaintenanceRecord m = table.requireSelected("record"); if (m != null) edit(m); }));
            table.addAction(UiUtils.danger("Delete", this::delete));
            table.onDoubleClick(this::edit);
        }
        table.addAction(UiUtils.neutral("Due / overdue", this::due));
    }

    @Override public String id() { return "maintenance"; }
    @Override public String title() { return "Maintenance"; }
    @Override public JComponent component() { return table; }
    @Override public void refresh() { table.refresh(); }

    private void edit(MaintenanceRecord existing) {
        MaintenanceRecord m = existing == null ? new MaintenanceRecord() : s.maintenance.getById(existing.getId());
        List<Vehicle> vehicles = s.vehicleRepository.findAll();
        if (vehicles.isEmpty()) { UiUtils.info(table, "No vehicles", "Add a vehicle first."); return; }
        FormBuilder f = new FormBuilder();
        Vehicle cur = vehicles.stream().filter(v -> v.getId().equals(m.getVehicleId())).findFirst().orElse(null);
        JComboBox<Vehicle> v = f.combo("Vehicle *", vehicles, cur, false);
        JTextField date = f.date("Service date *", m.getServiceDate() == null ? LocalDate.now() : m.getServiceDate());
        JComboBox<MaintenanceRecord.Type> type = f.combo("Type *", MaintenanceRecord.Type.values(), m.getType());
        JTextField desc = f.text("Description *", m.getDescription());
        JTextField provider = f.text("Provider / workshop", m.getProvider());
        JTextField cost = f.number("Cost (\u20A6)", m.getCost());
        JTextField km = f.number("Odometer at service (km)", m.getMileageAtService());
        JComboBox<MaintenanceRecord.Status> st = f.combo("Status *", MaintenanceRecord.Status.values(), m.getStatus());
        f.section("Next service reminder (optional)");
        JTextField nextDate = f.date("Next service date", m.getNextServiceDate());
        JTextField nextKm = f.number("Next service mileage (km)", m.getNextServiceMileage());
        JTextArea notes = f.area("Notes", m.getNotes());
        f.addFull(UiUtils.muted("IN PROGRESS moves the vehicle to MAINTENANCE; COMPLETED/CANCELLED releases it. Costs create a linked expense."));
        boolean ok = new FormDialog(UiUtils.windowOf(table), existing == null ? "Log maintenance" : "Edit maintenance record", f, "Save", () -> {
            m.setVehicleId(((Vehicle) v.getSelectedItem()).getId()); m.setServiceDate(f.date(date, "Service date")); m.setType((MaintenanceRecord.Type) type.getSelectedItem());
            m.setDescription(FormBuilder.str(desc)); m.setProvider(FormBuilder.str(provider)); m.setCost(f.dblOrZero(cost, "Cost")); m.setMileageAtService(f.lng(km, "Odometer"));
            m.setStatus((MaintenanceRecord.Status) st.getSelectedItem()); m.setNextServiceDate(f.date(nextDate, "Next service date")); m.setNextServiceMileage(f.lng(nextKm, "Next service mileage"));
            m.setNotes(FormBuilder.str(notes));
            f.throwIfParseErrors();
            if (existing == null) s.maintenance.create(m); else s.maintenance.update(m);
        }).showDialog();
        if (ok) refresh();
    }

    private void delete() {
        MaintenanceRecord m = table.requireSelected("record");
        if (m != null && UiUtils.confirm(table, "Delete record", "Delete this maintenance record and its linked expense?") && UiUtils.run(table, () -> s.maintenance.delete(m.getId()))) refresh();
    }

    private void due() {
        List<MaintenanceService.DueItem> items = s.maintenance.dueAndOverdue();
        if (items.isEmpty()) { UiUtils.info(table, "Maintenance due", "No vehicles are due or overdue for service."); return; }
        StringBuilder b = new StringBuilder();
        for (MaintenanceService.DueItem d : items) {
            b.append(d.dueStatus()).append(" - ").append(d.record().getVehicleDisplay());
            if (d.record().getNextServiceDate() != null) b.append(" · due ").append(DateUtil.display(d.record().getNextServiceDate()));
            if (d.record().getNextServiceMileage() != null) b.append(" · due at ").append(String.format("%,d km", d.record().getNextServiceMileage()))
                    .append(" (now ").append(String.format("%,d km", d.record().getVehicleCurrentMileage() == null ? 0 : d.record().getVehicleCurrentMileage())).append(")");
            b.append('\n');
        }
        UiUtils.info(table, "Maintenance due / overdue", b.toString());
    }
}
