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

public class FuelPanel implements ModulePanel {

    private final ServiceRegistry s;
    private final CrudTablePanel<FuelRecord> table;

    public FuelPanel(ServiceRegistry services) {
        this.s = services;
        List<EntityTableModel.Column<FuelRecord>> cols = List.of(
                EntityTableModel.Column.of("Date", x -> DateUtil.display(x.getDate()), 90),
                EntityTableModel.Column.of("Vehicle", FuelRecord::getVehicleDisplay, 170),
                EntityTableModel.Column.of("Driver", FuelRecord::getDriverDisplay, 140),
                EntityTableModel.Column.of("Fuel", FuelRecord::getFuelType, 70),
                EntityTableModel.Column.of("Litres", x -> String.format("%.2f", x.getQuantityLitres()), 70),
                EntityTableModel.Column.of("Unit price", x -> MoneyUtil.format(x.getUnitPrice()), 90),
                EntityTableModel.Column.of("Total", x -> MoneyUtil.format(x.getTotalCost()), 100),
                EntityTableModel.Column.of("Odometer", x -> x.getMileage() == null ? "" : String.format("%,d km", x.getMileage()), 90),
                EntityTableModel.Column.of("Station", FuelRecord::getStation, 130),
                EntityTableModel.Column.of("Receipt", FuelRecord::getReceiptReference, 90));
        table = new CrudTablePanel<>("Fuel", "Fuel purchases, unit prices and consumption analysis (each entry posts a FUEL expense)", cols);
        table.loader(() -> s.fuel.search(table.searchText(), null, null, null));
        if (SessionContext.has(Permission.MANAGE_FUEL)) {
            table.addAction(UiUtils.primary("Record fuel", () -> edit(null)));
            table.addAction(UiUtils.neutral("Edit", () -> { FuelRecord x = table.requireSelected("fuel record"); if (x != null) edit(x); }));
            table.addAction(UiUtils.danger("Delete", this::delete));
            table.onDoubleClick(this::edit);
        }
        table.addAction(UiUtils.neutral("Efficiency", this::efficiency));
    }

    @Override public String id() { return "fuel"; }
    @Override public String title() { return "Fuel"; }
    @Override public JComponent component() { return table; }
    @Override public void refresh() { table.refresh(); }

    private void edit(FuelRecord existing) {
        FuelRecord x = existing == null ? new FuelRecord() : s.fuel.getById(existing.getId());
        List<Vehicle> vehicles = s.vehicleRepository.findAll();
        if (vehicles.isEmpty()) { UiUtils.info(table, "No vehicles", "Add a vehicle first."); return; }
        List<Driver> drivers = s.driverRepository.findAll();
        FormBuilder f = new FormBuilder();
        Vehicle curV = vehicles.stream().filter(v -> v.getId().equals(x.getVehicleId())).findFirst().orElse(null);
        Driver curD = drivers.stream().filter(d -> d.getId().equals(x.getDriverId())).findFirst().orElse(null);
        JComboBox<Vehicle> v = f.combo("Vehicle *", vehicles, curV, false);
        JComboBox<Driver> d = f.combo("Driver", drivers, curD, true);
        JTextField date = f.date("Date *", x.getDate() == null ? LocalDate.now() : x.getDate());
        JTextField litres = f.number("Quantity (litres) *", x.getQuantityLitres() == 0 ? null : x.getQuantityLitres());
        JTextField price = f.number("Unit price (\u20A6/L) *", x.getUnitPrice() == 0 ? null : x.getUnitPrice());
        JTextField km = f.number("Odometer (km)", x.getMileage());
        JTextField station = f.text("Station", x.getStation());
        JTextField receipt = f.text("Receipt reference", x.getReceiptReference());
        JComboBox<FuelType> ft = f.combo("Fuel type", FuelType.values(), x.getFuelType() == null && curV != null ? curV.getFuelType() : x.getFuelType());
        JTextArea notes = f.area("Notes", x.getNotes());
        v.addActionListener(e -> { Vehicle sel = (Vehicle) v.getSelectedItem(); if (sel != null) { ft.setSelectedItem(sel.getFuelType());
            if (sel.getAssignedDriverId() != null) drivers.stream().filter(dr -> dr.getId().equals(sel.getAssignedDriverId())).findFirst().ifPresent(d::setSelectedItem); } });
        boolean ok = new FormDialog(UiUtils.windowOf(table), existing == null ? "Record fuel purchase" : "Edit fuel record", f, "Save", () -> {
            x.setVehicleId(((Vehicle) v.getSelectedItem()).getId()); x.setDriverId(d.getSelectedItem() instanceof Driver dr ? dr.getId() : null);
            x.setDate(f.date(date, "Date")); x.setQuantityLitres(f.dblOrZero(litres, "Quantity")); x.setUnitPrice(f.dblOrZero(price, "Unit price"));
            x.setMileage(f.lng(km, "Odometer")); x.setStation(FormBuilder.str(station)); x.setReceiptReference(FormBuilder.str(receipt));
            x.setFuelType((FuelType) ft.getSelectedItem()); x.setNotes(FormBuilder.str(notes));
            f.throwIfParseErrors();
            if (existing == null) s.fuel.create(x); else s.fuel.update(x);
        }).showDialog();
        if (ok) refresh();
    }

    private void delete() {
        FuelRecord x = table.requireSelected("fuel record");
        if (x != null && UiUtils.confirm(table, "Delete fuel record", "Delete this fuel record and its linked expense?") && UiUtils.run(table, () -> s.fuel.delete(x.getId()))) refresh();
    }

    private void efficiency() {
        StringBuilder b = new StringBuilder();
        for (Vehicle v : s.vehicleRepository.findAll()) {
            s.fuel.efficiencyForVehicle(v.getId()).ifPresent(e -> b.append(String.format("%s: %.2f km/L  (%.2f L/100 km) over %,d km, %d interval(s)%n",
                    v.getDisplayName(), e.kmPerLitre(), e.litresPer100Km(), e.distanceKm(), e.intervals())));
        }
        if (b.length() == 0) b.append("Not enough data: record at least two fuel purchases with odometer readings for a vehicle.");
        UiUtils.info(table, "Fuel efficiency", b.toString());
    }
}
