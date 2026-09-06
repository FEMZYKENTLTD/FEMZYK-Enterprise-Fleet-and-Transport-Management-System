package com.femzyk.fleetmanagement.ui.modules;

import com.femzyk.fleetmanagement.model.*;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import com.femzyk.fleetmanagement.ui.ModulePanel;
import com.femzyk.fleetmanagement.ui.UiUtils;
import com.femzyk.fleetmanagement.ui.components.*;
import com.femzyk.fleetmanagement.util.DateUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AssignmentsPanel implements ModulePanel {

    private final ServiceRegistry s;
    private final CrudTablePanel<Assignment> table;
    private final JComboBox<Object> status;

    static List<EntityTableModel.Column<Assignment>> columns() {
        return List.of(
                EntityTableModel.Column.of("ID", Assignment::getId, 40),
                EntityTableModel.Column.of("Vehicle", Assignment::getVehicleDisplay, 200),
                EntityTableModel.Column.of("Driver", Assignment::getDriverDisplay, 180),
                EntityTableModel.Column.of("Assigned", a -> DateUtil.display(a.getAssignedAt()), 120),
                EntityTableModel.Column.of("Returned", a -> DateUtil.display(a.getReturnedAt()), 120),
                EntityTableModel.Column.of("Status", Assignment::getStatus, 90),
                EntityTableModel.Column.of("Purpose", Assignment::getPurpose, 200),
                EntityTableModel.Column.of("By", Assignment::getAssignedBy, 90));
    }

    public AssignmentsPanel(ServiceRegistry services) {
        this.s = services;
        table = new CrudTablePanel<>("Vehicle Assignments", "Which driver holds which vehicle - with conflict prevention and full history", columns());
        table.setRenderer(5, new StatusCellRenderer());
        status = table.addFilter("Status:", new JComboBox<>());
        status.addItem("All");
        for (Assignment.Status st : Assignment.Status.values()) status.addItem(st);
        status.setSelectedItem(Assignment.Status.ACTIVE);
        table.loader(() -> s.assignments.search(table.searchText(), status.getSelectedItem() instanceof Assignment.Status st ? st : null));
        if (SessionContext.has(Permission.MANAGE_ASSIGNMENTS)) {
            table.addAction(UiUtils.primary("Assign vehicle", this::assign));
            table.addAction(UiUtils.success("Return vehicle", this::returnVehicle));
            table.addAction(UiUtils.neutral("Reassign", this::reassign));
            table.addAction(UiUtils.danger("Cancel", this::cancel));
        }
    }

    @Override public String id() { return "assignments"; }
    @Override public String title() { return "Assignments"; }
    @Override public JComponent component() { return table; }
    @Override public void refresh() { table.refresh(); }

    private void assign() {
        List<Vehicle> vehicles = s.vehicleRepository.search(null, null, VehicleStatus.AVAILABLE, null);
        List<Driver> drivers = s.driverRepository.search(null, DriverStatus.ACTIVE).stream().filter(d -> d.getAssignedVehicle() == null && !d.isLicenceExpired()).toList();
        if (vehicles.isEmpty()) { UiUtils.info(table, "No available vehicles", "There are no AVAILABLE vehicles to assign."); return; }
        if (drivers.isEmpty()) { UiUtils.info(table, "No free drivers", "There are no active, unassigned drivers with a valid licence."); return; }
        FormBuilder f = new FormBuilder();
        JComboBox<Vehicle> v = f.combo("Vehicle *", vehicles, null, false);
        JComboBox<Driver> d = f.combo("Driver *", drivers, null, false);
        JTextField purpose = f.text("Purpose", "");
        JTextArea notes = f.area("Notes", "");
        f.addFull(UiUtils.muted("Rules: vehicle must be AVAILABLE, driver ACTIVE with a valid licence of a class that permits the vehicle type, and neither may already be assigned."));
        if (new FormDialog(UiUtils.windowOf(table), "Assign vehicle to driver", f, "Assign", () ->
                s.assignments.assign(((Vehicle) v.getSelectedItem()).getId(), ((Driver) d.getSelectedItem()).getId(), FormBuilder.str(purpose), FormBuilder.str(notes))).showDialog()) refresh();
    }

    private void returnVehicle() {
        Assignment a = table.requireSelected("assignment");
        if (a == null) return;
        String notes = UiUtils.prompt(table, "Return vehicle", "Return " + a.getVehicleDisplay() + " from " + a.getDriverDisplay() + "?\nOptional note:");
        if (notes == null) return;
        if (UiUtils.run(table, () -> s.assignments.returnVehicle(a.getId(), notes))) refresh();
    }

    private void reassign() {
        Assignment a = table.requireSelected("assignment");
        if (a == null) return;
        List<Driver> drivers = s.driverRepository.search(null, DriverStatus.ACTIVE).stream().filter(d -> d.getAssignedVehicle() == null && !d.isLicenceExpired()).toList();
        if (drivers.isEmpty()) { UiUtils.info(table, "No free drivers", "There are no active, unassigned drivers with a valid licence."); return; }
        FormBuilder f = new FormBuilder();
        f.add("Vehicle", new JLabel(a.getVehicleDisplay()));
        f.add("Current driver", new JLabel(a.getDriverDisplay()));
        JComboBox<Driver> d = f.combo("New driver *", drivers, null, false);
        JTextField purpose = f.text("Purpose", a.getPurpose());
        JTextArea notes = f.area("Notes", "");
        if (new FormDialog(UiUtils.windowOf(table), "Reassign vehicle", f, "Reassign", () ->
                s.assignments.reassign(a.getVehicleId(), ((Driver) d.getSelectedItem()).getId(), FormBuilder.str(purpose), FormBuilder.str(notes))).showDialog()) refresh();
    }

    private void cancel() {
        Assignment a = table.requireSelected("assignment");
        if (a == null) return;
        String reason = UiUtils.prompt(table, "Cancel assignment", "Reason for cancelling assignment #" + a.getId() + ":");
        if (reason == null) return;
        if (UiUtils.run(table, () -> s.assignments.cancel(a.getId(), reason))) refresh();
    }

    static JComponent historyTable(List<Assignment> rows) {
        EntityTableModel<Assignment> m = new EntityTableModel<>(columns());
        m.setRows(rows);
        JTable t = new JTable(m);
        t.setRowHeight(24);
        t.setAutoCreateRowSorter(true);
        t.getColumnModel().getColumn(5).setCellRenderer(new StatusCellRenderer());
        return new JScrollPane(t);
    }

    static void showHistory(Window owner, String title, List<Assignment> rows) {
        JDialog d = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        d.setContentPane(historyTable(rows));
        d.setSize(820, 400);
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
    }
}
