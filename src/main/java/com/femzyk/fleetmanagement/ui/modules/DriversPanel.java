package com.femzyk.fleetmanagement.ui.modules;

import com.femzyk.fleetmanagement.model.*;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import com.femzyk.fleetmanagement.ui.ModulePanel;
import com.femzyk.fleetmanagement.ui.UiUtils;
import com.femzyk.fleetmanagement.ui.components.*;
import com.femzyk.fleetmanagement.util.DateUtil;

import javax.swing.*;
import java.util.List;

public class DriversPanel implements ModulePanel {

    private final ServiceRegistry s;
    private final CrudTablePanel<Driver> table;
    private final JComboBox<Object> status;

    public DriversPanel(ServiceRegistry services) {
        this.s = services;
        List<EntityTableModel.Column<Driver>> cols = List.of(
                EntityTableModel.Column.of("Code", Driver::getDriverCode, 80),
                EntityTableModel.Column.of("Name", Driver::getFullName, 170),
                EntityTableModel.Column.of("Phone", Driver::getPhone, 100),
                EntityTableModel.Column.of("Licence no.", Driver::getLicenceNumber, 130),
                EntityTableModel.Column.of("Class", d -> d.getLicenceCategory() + " – " + d.getLicenceCategory().getDescription(), 150),
                EntityTableModel.Column.of("Expiry", d -> DateUtil.display(d.getLicenceExpiryDate()), 100),
                EntityTableModel.Column.of("Licence", d -> d.isLicenceExpired() ? "EXPIRED" : d.isLicenceExpiringSoon() ? "EXPIRING SOON" : "VALID", 110),
                EntityTableModel.Column.of("Status", Driver::getStatus, 90),
                EntityTableModel.Column.of("Assigned vehicle", Driver::getAssignedVehicle, 160));
        table = new CrudTablePanel<>("Drivers", "Driver profiles linked to employees, licence classes and expiry tracking", cols);
        table.setRenderer(6, new StatusCellRenderer());
        table.setRenderer(7, new StatusCellRenderer());
        status = table.addFilter("Status:", new JComboBox<>());
        status.addItem("All");
        for (DriverStatus st : DriverStatus.values()) status.addItem(st);
        table.loader(() -> s.drivers.search(table.searchText(), status.getSelectedItem() instanceof DriverStatus st ? st : null));
        if (SessionContext.has(Permission.MANAGE_DRIVERS)) {
            table.addAction(UiUtils.primary("Add driver", () -> edit(null)));
            table.addAction(UiUtils.neutral("Edit", () -> { Driver d = table.requireSelected("driver"); if (d != null) edit(d); }));
            table.addAction(UiUtils.danger("Delete", this::delete));
            table.addAction(UiUtils.neutral("Recycle bin", this::recycleBin));
            table.onDoubleClick(this::edit);
        }
        table.addAction(UiUtils.neutral("Assignment history", this::history));
    }

    @Override public String id() { return "drivers"; }
    @Override public String title() { return "Drivers"; }
    @Override public JComponent component() { return table; }
    @Override public void refresh() { table.refresh(); }

    private void edit(Driver existing) {
        Driver d = existing == null ? new Driver() : s.drivers.getById(existing.getId());
        FormBuilder f = new FormBuilder();
        JComboBox<Employee> employee;
        if (existing == null) {
            List<Employee> candidates = s.drivers.employeesWithoutDriverProfile();
            if (candidates.isEmpty()) { UiUtils.info(table, "No eligible employees", "Every active employee already has a driver profile. Add the person as an employee first."); return; }
            employee = f.combo("Employee *", candidates, null, false);
        } else {
            employee = null;
            f.add("Employee", new JLabel(d.getFullName() + " (" + d.getPhone() + ")"));
        }
        JTextField licence = f.text("Licence number *", d.getLicenceNumber());
        JComboBox<LicenceCategory> cat = f.combo("Licence class *", LicenceCategory.values(), d.getLicenceCategory());
        cat.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean foc) {
                return super.getListCellRendererComponent(l, v instanceof LicenceCategory c ? c + " – " + c.getDescription() : v, i, sel, foc);
            }
        });
        JTextField issued = f.date("Issue date", d.getLicenceIssueDate());
        JTextField expiry = f.date("Expiry date *", d.getLicenceExpiryDate());
        JComboBox<DriverStatus> st = f.combo("Status", DriverStatus.values(), d.getStatus());
        JTextArea notes = f.area("Notes", d.getNotes());
        boolean ok = new FormDialog(UiUtils.windowOf(table), existing == null ? "Add driver" : "Edit " + d.getDriverCode(), f, "Save", () -> {
            if (employee != null && employee.getSelectedItem() instanceof Employee e) d.setEmployeeId(e.getId());
            d.setLicenceNumber(FormBuilder.str(licence)); d.setLicenceCategory((LicenceCategory) cat.getSelectedItem());
            d.setLicenceIssueDate(f.date(issued, "Issue date")); d.setLicenceExpiryDate(f.date(expiry, "Expiry date"));
            d.setStatus((DriverStatus) st.getSelectedItem()); d.setNotes(FormBuilder.str(notes));
            f.throwIfParseErrors();
            if (existing == null) s.drivers.create(d); else s.drivers.update(d);
        }).showDialog();
        if (ok) refresh();
    }

    private void delete() {
        Driver d = table.requireSelected("driver");
        if (d == null) return;
        if (UiUtils.confirm(table, "Delete driver", "Move " + d.getFullName() + "'s driver profile to the recycle bin? The employee record is kept.")
                && UiUtils.run(table, () -> s.drivers.delete(d.getId()))) refresh();
    }

    private void recycleBin() {
        List<EntityTableModel.Column<Driver>> cols = List.of(
                EntityTableModel.Column.of("Code", Driver::getDriverCode), EntityTableModel.Column.of("Name", Driver::getFullName),
                EntityTableModel.Column.of("Licence", Driver::getLicenceNumber), EntityTableModel.Column.of("Deleted", d -> DateUtil.display(d.getDeletedAt())));
        new RecycleBinDialog<>(UiUtils.windowOf(table), "Driver recycle bin", cols, s.drivers::recycleBin, d -> s.drivers.restore(d.getId()), null).setVisible(true);
        refresh();
    }

    private void history() {
        Driver d = table.requireSelected("driver");
        if (d == null) return;
        AssignmentsPanel.showHistory(UiUtils.windowOf(table), "Assignment history – " + d.getFullName(), s.assignments.historyForDriver(d.getId()));
    }
}
