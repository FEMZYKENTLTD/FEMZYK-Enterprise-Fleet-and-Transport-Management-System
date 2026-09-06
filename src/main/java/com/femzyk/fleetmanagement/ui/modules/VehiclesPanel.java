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
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class VehiclesPanel implements ModulePanel {

    private final ServiceRegistry s;
    private final CrudTablePanel<Vehicle> table;
    private final JComboBox<Object> type;
    private final JComboBox<Object> status;

    public VehiclesPanel(ServiceRegistry services) {
        this.s = services;
        List<EntityTableModel.Column<Vehicle>> cols = List.of(
                EntityTableModel.Column.of("Code", Vehicle::getVehicleCode, 75),
                EntityTableModel.Column.of("Registration", Vehicle::getRegistrationNumber, 110),
                EntityTableModel.Column.of("Type", v -> v.getVehicleType().name(), 90),
                EntityTableModel.Column.of("Make / model", v -> v.getMake() + " " + v.getModel(), 160),
                EntityTableModel.Column.of("Year", Vehicle::getYear, 50),
                EntityTableModel.Column.of("Details", Vehicle::getKeyAttribute, 150),
                EntityTableModel.Column.of("Fuel", Vehicle::getFuelType, 70),
                EntityTableModel.Column.of("Mileage", v -> String.format("%,d km", v.getCurrentMileage()), 90),
                EntityTableModel.Column.of("Status", Vehicle::getStatus, 110),
                EntityTableModel.Column.of("Driver", Vehicle::getAssignedDriverName, 150));
        table = new CrudTablePanel<>("Vehicles", "Fleet register with life-cycle status, mileage and assignment", cols);
        table.setRenderer(2, new StatusCellRenderer(true));
        table.setRenderer(8, new StatusCellRenderer());
        type = table.addFilter("Type:", new JComboBox<>());
        type.addItem("All types");
        for (VehicleType t : VehicleType.values()) type.addItem(t);
        status = table.addFilter("Status:", new JComboBox<>());
        status.addItem("All statuses");
        for (VehicleStatus st : VehicleStatus.values()) status.addItem(st);
        table.loader(() -> s.vehicles.search(table.searchText(), type.getSelectedItem() instanceof VehicleType t ? t : null,
                status.getSelectedItem() instanceof VehicleStatus st ? st : null, null));
        if (SessionContext.has(Permission.MANAGE_VEHICLES)) {
            table.addAction(UiUtils.primary("Add vehicle", () -> edit(null)));
            table.addAction(UiUtils.neutral("Edit", () -> { Vehicle v = table.requireSelected("vehicle"); if (v != null) edit(v); }));
            table.addAction(UiUtils.neutral("Change status", this::changeStatus));
            table.addAction(UiUtils.danger("Delete", this::delete));
            table.addAction(UiUtils.neutral("Recycle bin", this::recycleBin));
            table.onDoubleClick(this::edit);
        }
        table.addAction(UiUtils.neutral("History", this::history));
    }

    @Override public String id() { return "vehicles"; }
    @Override public String title() { return "Vehicles"; }
    @Override public JComponent component() { return table; }
    @Override public void refresh() { table.refresh(); }

    private void edit(Vehicle existing) {
        VehicleType vt;
        if (existing == null) {
            Object pick = JOptionPane.showInputDialog(table, "Vehicle type:", "Add vehicle", JOptionPane.QUESTION_MESSAGE, null, VehicleType.values(), VehicleType.CAR);
            if (pick == null) return;
            vt = (VehicleType) pick;
        } else vt = existing.getVehicleType();
        Vehicle v = existing == null ? Vehicle.newOfType(vt) : s.vehicles.getById(existing.getId());
        FormBuilder f = new FormBuilder();
        f.section(vt.getLabel() + " details");
        JTextField reg = f.text("Registration number *", v.getRegistrationNumber());
        JTextField make = f.text("Make *", v.getMake());
        JTextField model = f.text("Model *", v.getModel());
        JTextField year = f.number("Year *", v.getYear() == 0 ? null : v.getYear());
        JTextField color = f.text("Colour", v.getColor());
        JTextField vin = f.text("VIN / chassis no.", v.getVin());
        JTextField engine = f.text("Engine number", v.getEngineNumber());
        JComboBox<FuelType> fuel = f.combo("Fuel type", FuelType.values(), v.getFuelType());
        JTextField capacity = f.number("Capacity (seats)", v.getCapacity());
        JTextField attrNum = null; JComboBox<String> attrText = null;
        switch (vt) {
            case CAR -> { attrNum = f.number("Number of doors", ((Car) v).getNumberOfDoors()); attrText = f.combo("Transmission", new String[]{"AUTOMATIC", "MANUAL"}, ((Car) v).getTransmission()); }
            case TRUCK -> { attrNum = f.number("Cargo capacity (tons)", ((Truck) v).getCargoCapacityTons()); attrText = f.combo("Transmission", new String[]{"MANUAL", "AUTOMATIC"}, ((Truck) v).getTransmission()); }
            case MOTORCYCLE -> { attrNum = f.number("Number of wheels", ((Motorcycle) v).getNumberOfWheels()); attrText = f.combo("Style", new String[]{"STANDARD", "SPORT", "CRUISER", "OFF_ROAD"}, ((Motorcycle) v).getStyle()); }
            default -> { }
        }
        f.section("Acquisition & usage");
        JTextField acqDate = f.date("Acquisition date", v.getAcquisitionDate());
        JTextField acqCost = f.number("Acquisition cost (\u20A6)", v.getAcquisitionCost());
        JTextField mileage = f.number("Current mileage (km)", v.getCurrentMileage());
        if (existing != null) f.add("Status", new JLabel(v.getStatus().getLabel() + "   (use 'Change status' to move it)"));
        JTextArea notes = f.area("Notes", v.getNotes());
        final JTextField fAttrNum = attrNum; final JComboBox<String> fAttrText = attrText;
        boolean ok = new FormDialog(UiUtils.windowOf(table), existing == null ? "Add " + vt.getLabel().toLowerCase() : "Edit " + v.getRegistrationNumber(), f, "Save", () -> {
            v.setRegistrationNumber(FormBuilder.str(reg)); v.setMake(FormBuilder.str(make)); v.setModel(FormBuilder.str(model));
            v.setYear(f.intOrZero(year, "Year")); v.setColor(FormBuilder.str(color)); v.setVin(FormBuilder.str(vin)); v.setEngineNumber(FormBuilder.str(engine));
            v.setFuelType((FuelType) fuel.getSelectedItem()); v.setCapacity(f.intOrZero(capacity, "Capacity"));
            if (fAttrNum != null) v.setAttributeNumber(f.dbl(fAttrNum, "Type attribute"));
            if (fAttrText != null) v.setAttributeText((String) fAttrText.getSelectedItem());
            v.setAcquisitionDate(f.date(acqDate, "Acquisition date")); v.setAcquisitionCost(f.dblOrZero(acqCost, "Acquisition cost"));
            v.setCurrentMileage(f.lngOrZero(mileage, "Mileage")); v.setNotes(FormBuilder.str(notes));
            f.throwIfParseErrors();
            if (existing == null) s.vehicles.create(v); else s.vehicles.update(v);
        }).showDialog();
        if (ok) refresh();
    }

    private void changeStatus() {
        Vehicle v = table.requireSelected("vehicle");
        if (v == null) return;
        List<VehicleStatus> allowed = new ArrayList<>(v.getStatus().allowedTransitions());
        allowed.remove(VehicleStatus.ASSIGNED); // only through assignments
        allowed.remove(VehicleStatus.IN_SERVICE); // only through trips
        if (allowed.isEmpty()) { UiUtils.info(table, "No transitions", "A " + v.getStatus().getLabel().toLowerCase() + " vehicle cannot change status here."); return; }
        FormBuilder f = new FormBuilder();
        f.add("Vehicle", new JLabel(v.getDisplayName()));
        f.add("Current status", new JLabel(v.getStatus().getLabel()));
        JComboBox<VehicleStatus> target = f.combo("New status *", allowed.toArray(new VehicleStatus[0]), allowed.get(0));
        JTextArea reason = f.area("Reason", "");
        f.addFull(UiUtils.muted("ASSIGNED is set by the Assignments module and IN SERVICE by starting a trip."));
        if (new FormDialog(UiUtils.windowOf(table), "Change vehicle status", f, "Apply", () ->
                s.vehicles.changeStatus(v.getId(), (VehicleStatus) target.getSelectedItem(), FormBuilder.str(reason))).showDialog()) refresh();
    }

    private void delete() {
        Vehicle v = table.requireSelected("vehicle");
        if (v == null) return;
        if (UiUtils.confirm(table, "Delete vehicle", "Move " + v.getDisplayName() + " to the recycle bin?") && UiUtils.run(table, () -> s.vehicles.delete(v.getId()))) refresh();
    }

    private void recycleBin() {
        List<EntityTableModel.Column<Vehicle>> cols = List.of(
                EntityTableModel.Column.of("Code", Vehicle::getVehicleCode), EntityTableModel.Column.of("Registration", Vehicle::getRegistrationNumber),
                EntityTableModel.Column.of("Vehicle", v -> v.getMake() + " " + v.getModel()), EntityTableModel.Column.of("Deleted", v -> DateUtil.display(v.getDeletedAt())));
        new RecycleBinDialog<>(UiUtils.windowOf(table), "Vehicle recycle bin", cols, s.vehicles::recycleBin, v -> s.vehicles.restore(v.getId()), v -> s.vehicles.deletePermanently(v.getId())).setVisible(true);
        refresh();
    }

    private void history() {
        Vehicle v = table.requireSelected("vehicle");
        if (v == null) return;
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Assignments", AssignmentsPanel.historyTable(s.assignments.historyForVehicle(v.getId())));
        List<EntityTableModel.Column<Trip>> tripCols = List.of(EntityTableModel.Column.of("Code", Trip::getTripCode), EntityTableModel.Column.of("Driver", Trip::getDriverDisplay),
                EntityTableModel.Column.of("Route", t -> t.getOrigin() + " → " + t.getDestination()), EntityTableModel.Column.of("Departure", t -> DateUtil.display(t.getDepartureTime())),
                EntityTableModel.Column.of("Distance", t -> t.getDistanceKm() == null ? "" : t.getDistanceKm() + " km"), EntityTableModel.Column.of("Status", Trip::getStatus));
        EntityTableModel<Trip> tm = new EntityTableModel<>(tripCols); tm.setRows(s.tripRepository.search(null, null, v.getId(), null, null, null));
        tabs.addTab("Trips", new JScrollPane(new JTable(tm)));
        List<EntityTableModel.Column<MaintenanceRecord>> mCols = List.of(EntityTableModel.Column.of("Date", m -> DateUtil.display(m.getServiceDate())), EntityTableModel.Column.of("Type", MaintenanceRecord::getType),
                EntityTableModel.Column.of("Description", MaintenanceRecord::getDescription), EntityTableModel.Column.of("Cost", m -> MoneyUtil.format(m.getCost())), EntityTableModel.Column.of("Status", MaintenanceRecord::getStatus));
        EntityTableModel<MaintenanceRecord> mm = new EntityTableModel<>(mCols); mm.setRows(s.maintenanceRepository.search(null, null, null, v.getId(), null, null));
        tabs.addTab("Maintenance", new JScrollPane(new JTable(mm)));
        List<EntityTableModel.Column<FuelRecord>> fCols = List.of(EntityTableModel.Column.of("Date", x -> DateUtil.display(x.getDate())), EntityTableModel.Column.of("Litres", FuelRecord::getQuantityLitres),
                EntityTableModel.Column.of("Total", x -> MoneyUtil.format(x.getTotalCost())), EntityTableModel.Column.of("Odometer", FuelRecord::getMileage), EntityTableModel.Column.of("Station", FuelRecord::getStation));
        EntityTableModel<FuelRecord> fm = new EntityTableModel<>(fCols); fm.setRows(s.fuelRepository.search(null, v.getId(), null, null));
        tabs.addTab("Fuel", new JScrollPane(new JTable(fm)));
        JPanel summary = new JPanel(new GridLayout(0, 1, 0, 4));
        summary.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        summary.add(new JLabel("<html><b>" + v.getDisplayName() + "</b> — " + v.getVehicleType() + ", " + v.getKeyAttribute() + "</html>"));
        summary.add(new JLabel("Total operating expenses: " + MoneyUtil.format(s.expenseRepository.totalByVehicle(java.time.LocalDate.of(2000, 1, 1), java.time.LocalDate.now().plusYears(1)).getOrDefault(v.getRegistrationNumber(), 0.0))));
        s.fuel.efficiencyForVehicle(v.getId()).ifPresentOrElse(e -> summary.add(new JLabel(String.format("Fuel efficiency: %.2f km/L (%.2f L/100km) over %,d km", e.kmPerLitre(), e.litresPer100Km(), e.distanceKm()))),
                () -> summary.add(new JLabel("Fuel efficiency: not enough odometer readings yet")));
        JDialog d = new JDialog(UiUtils.windowOf(table), "Vehicle history – " + v.getRegistrationNumber(), Dialog.ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout());
        root.add(summary, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        d.setContentPane(root);
        d.setSize(860, 520);
        d.setLocationRelativeTo(UiUtils.windowOf(table));
        d.setVisible(true);
    }
}
