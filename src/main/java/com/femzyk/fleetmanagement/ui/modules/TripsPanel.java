package com.femzyk.fleetmanagement.ui.modules;

import com.femzyk.fleetmanagement.model.*;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import com.femzyk.fleetmanagement.ui.ModulePanel;
import com.femzyk.fleetmanagement.ui.UiUtils;
import com.femzyk.fleetmanagement.ui.components.*;
import com.femzyk.fleetmanagement.util.DateUtil;

import javax.swing.*;
import java.time.LocalDateTime;
import java.util.List;

public class TripsPanel implements ModulePanel {

    private final ServiceRegistry s;
    private final CrudTablePanel<Trip> table;
    private final JComboBox<Object> status;

    public TripsPanel(ServiceRegistry services) {
        this.s = services;
        List<EntityTableModel.Column<Trip>> cols = List.of(
                EntityTableModel.Column.of("Code", Trip::getTripCode, 80),
                EntityTableModel.Column.of("Vehicle", Trip::getVehicleDisplay, 170),
                EntityTableModel.Column.of("Driver", Trip::getDriverDisplay, 150),
                EntityTableModel.Column.of("From", Trip::getOrigin, 120),
                EntityTableModel.Column.of("To", Trip::getDestination, 120),
                EntityTableModel.Column.of("Departure", t -> DateUtil.display(t.getDepartureTime()), 120),
                EntityTableModel.Column.of("Return", t -> DateUtil.display(t.getReturnTime()), 120),
                EntityTableModel.Column.of("Start km", Trip::getStartMileage, 70),
                EntityTableModel.Column.of("End km", Trip::getEndMileage, 70),
                EntityTableModel.Column.of("Distance", t -> t.getDistanceKm() == null ? "" : String.format("%,d km", t.getDistanceKm()), 80),
                EntityTableModel.Column.of("Status", Trip::getStatus, 90));
        table = new CrudTablePanel<>("Trips", "Plan, start and complete journeys with odometer validation", cols);
        table.setRenderer(10, new StatusCellRenderer());
        status = table.addFilter("Status:", new JComboBox<>());
        status.addItem("All");
        for (Trip.Status st : Trip.Status.values()) status.addItem(st);
        table.loader(() -> s.trips.search(table.searchText(), status.getSelectedItem() instanceof Trip.Status st ? st : null, null, null, null, null));
        if (SessionContext.has(Permission.MANAGE_TRIPS)) {
            table.addAction(UiUtils.primary("Plan trip", () -> edit(null)));
            table.addAction(UiUtils.neutral("Edit", () -> { Trip t = table.requireSelected("trip"); if (t != null) edit(t); }));
            table.addAction(UiUtils.success("Start", this::start));
            table.addAction(UiUtils.success("Complete", this::complete));
            table.addAction(UiUtils.danger("Cancel trip", this::cancel));
            table.addAction(UiUtils.neutral("Delete", this::delete));
            table.onDoubleClick(this::edit);
        }
    }

    @Override public String id() { return "trips"; }
    @Override public String title() { return "Trips"; }
    @Override public JComponent component() { return table; }
    @Override public void refresh() { table.refresh(); }

    private void edit(Trip existing) {
        Trip t = existing == null ? new Trip() : s.trips.getById(existing.getId());
        List<Vehicle> vehicles = s.vehicleRepository.findAll().stream().filter(v -> v.getStatus().isOperational()).toList();
        List<Driver> drivers = s.driverRepository.search(null, DriverStatus.ACTIVE);
        if (vehicles.isEmpty() || drivers.isEmpty()) { UiUtils.info(table, "Cannot plan trip", "You need at least one operational vehicle and one active driver."); return; }
        FormBuilder f = new FormBuilder();
        Vehicle curV = vehicles.stream().filter(v -> v.getId().equals(t.getVehicleId())).findFirst().orElse(null);
        Driver curD = drivers.stream().filter(d -> d.getId().equals(t.getDriverId())).findFirst().orElse(null);
        JComboBox<Vehicle> v = f.combo("Vehicle *", vehicles, curV, false);
        JComboBox<Driver> d = f.combo("Driver *", drivers, curD, false);
        // default the driver to the vehicle's assigned driver
        v.addActionListener(e -> { Vehicle sel = (Vehicle) v.getSelectedItem(); if (sel != null && sel.getAssignedDriverId() != null)
            drivers.stream().filter(x -> x.getId().equals(sel.getAssignedDriverId())).findFirst().ifPresent(d::setSelectedItem); });
        JTextField origin = f.text("Origin *", t.getOrigin());
        JTextField dest = f.text("Destination *", t.getDestination());
        JTextField purpose = f.text("Purpose", t.getPurpose());
        JTextField dep = f.dateTime("Departure *", t.getDepartureTime() == null ? LocalDateTime.now().withSecond(0).withNano(0) : t.getDepartureTime());
        JTextField startKm = f.number("Start odometer (km)", t.getStartMileage());
        f.addFull(UiUtils.muted("Leave start odometer blank to use the vehicle's current mileage when the trip starts."));
        JCheckBox startNow = existing == null ? f.check("Start immediately", false) : null;
        JTextArea notes = f.area("Notes", t.getNotes());
        boolean ok = new FormDialog(UiUtils.windowOf(table), existing == null ? "Plan trip" : "Edit " + t.getTripCode(), f, "Save", () -> {
            t.setVehicleId(((Vehicle) v.getSelectedItem()).getId()); t.setDriverId(((Driver) d.getSelectedItem()).getId());
            t.setOrigin(FormBuilder.str(origin)); t.setDestination(FormBuilder.str(dest)); t.setPurpose(FormBuilder.str(purpose));
            t.setDepartureTime(f.dateTime(dep, "Departure")); t.setStartMileage(f.lng(startKm, "Start odometer")); t.setNotes(FormBuilder.str(notes));
            f.throwIfParseErrors();
            if (existing == null) s.trips.create(t, startNow.isSelected()); else s.trips.update(t);
        }).showDialog();
        if (ok) refresh();
    }

    private void start() {
        Trip t = table.requireSelected("trip");
        if (t != null && UiUtils.run(table, () -> s.trips.start(t.getId()))) refresh();
    }

    private void complete() {
        Trip t = table.requireSelected("trip");
        if (t == null) return;
        FormBuilder f = new FormBuilder();
        f.add("Trip", new JLabel(t.getTripCode() + ": " + t.getOrigin() + " → " + t.getDestination()));
        f.add("Start odometer", new JLabel(t.getStartMileage() == null ? "-" : String.format("%,d km", t.getStartMileage())));
        JTextField endKm = f.number("End odometer (km) *", null);
        JTextField fuel = f.number("Fuel used (litres)", t.getFuelUsedLitres());
        JTextField ret = f.dateTime("Return time", LocalDateTime.now().withSecond(0).withNano(0));
        JTextArea notes = f.area("Notes", "");
        if (new FormDialog(UiUtils.windowOf(table), "Complete trip", f, "Complete", () -> {
            Long end = f.lng(endKm, "End odometer"); Double litres = f.dbl(fuel, "Fuel used"); LocalDateTime r = f.dateTime(ret, "Return time");
            f.throwIfParseErrors();
            if (end == null) throw new com.femzyk.fleetmanagement.exception.ValidationException("End odometer is required.");
            s.trips.complete(t.getId(), end, litres, r, FormBuilder.str(notes));
        }).showDialog()) refresh();
    }

    private void cancel() {
        Trip t = table.requireSelected("trip");
        if (t == null) return;
        String reason = UiUtils.prompt(table, "Cancel trip", "Reason for cancelling " + t.getTripCode() + ":");
        if (reason != null && UiUtils.run(table, () -> s.trips.cancel(t.getId(), reason))) refresh();
    }

    private void delete() {
        Trip t = table.requireSelected("trip");
        if (t != null && UiUtils.confirm(table, "Delete trip", "Delete " + t.getTripCode() + "? Completed trips cannot be deleted.")
                && UiUtils.run(table, () -> s.trips.delete(t.getId()))) refresh();
    }
}
