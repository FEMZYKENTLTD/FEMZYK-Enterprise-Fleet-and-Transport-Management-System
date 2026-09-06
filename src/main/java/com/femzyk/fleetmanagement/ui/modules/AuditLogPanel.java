package com.femzyk.fleetmanagement.ui.modules;

import com.femzyk.fleetmanagement.model.AuditLog;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import com.femzyk.fleetmanagement.ui.ModulePanel;
import com.femzyk.fleetmanagement.ui.components.CrudTablePanel;
import com.femzyk.fleetmanagement.ui.components.EntityTableModel;
import com.femzyk.fleetmanagement.util.DateUtil;

import javax.swing.*;
import java.util.List;

public class AuditLogPanel implements ModulePanel {

    private final ServiceRegistry s;
    private final CrudTablePanel<AuditLog> table;
    private final JComboBox<String> module;
    private final JComboBox<String> action;

    public AuditLogPanel(ServiceRegistry services) {
        this.s = services;
        List<EntityTableModel.Column<AuditLog>> cols = List.of(
                EntityTableModel.Column.of("Timestamp", a -> DateUtil.format(a.getTimestamp()), 140),
                EntityTableModel.Column.of("User", AuditLog::getUsername, 90),
                EntityTableModel.Column.of("Action", AuditLog::getAction, 100),
                EntityTableModel.Column.of("Module", AuditLog::getModule, 100),
                EntityTableModel.Column.of("Reference", AuditLog::getReference, 130),
                EntityTableModel.Column.of("Description", AuditLog::getDescription, 420));
        table = new CrudTablePanel<>("Audit Log", "Who did what and when - every create, update, delete, login and export (latest 2,000 shown)", cols);
        module = table.addFilter("Module:", new JComboBox<>());
        action = table.addFilter("Action:", new JComboBox<>());
        table.loader(() -> s.auditLogRepository.search(table.searchText(), module.getSelectedIndex() <= 0 ? null : (String) module.getSelectedItem(),
                action.getSelectedIndex() <= 0 ? null : (String) action.getSelectedItem(), null, null, 2000));
    }

    @Override public String id() { return "audit"; }
    @Override public String title() { return "Audit Log"; }
    @Override public JComponent component() { return table; }

    @Override
    public void refresh() {
        reload(module, "All modules", s.auditLogRepository.distinctModules());
        reload(action, "All actions", s.auditLogRepository.distinctActions());
        table.refresh();
    }

    private static void reload(JComboBox<String> box, String all, List<String> values) {
        Object sel = box.getSelectedItem();
        var listeners = box.getActionListeners();
        for (var l : listeners) box.removeActionListener(l);
        box.removeAllItems();
        box.addItem(all);
        values.forEach(box::addItem);
        if (sel != null) box.setSelectedItem(sel);
        for (var l : listeners) box.addActionListener(l);
    }
}
