package com.femzyk.fleetmanagement.ui.modules;

import com.femzyk.fleetmanagement.model.Role;
import com.femzyk.fleetmanagement.model.User;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import com.femzyk.fleetmanagement.ui.ModulePanel;
import com.femzyk.fleetmanagement.ui.UiUtils;
import com.femzyk.fleetmanagement.ui.components.*;
import com.femzyk.fleetmanagement.util.DateUtil;

import javax.swing.*;
import java.util.List;

public class UsersPanel implements ModulePanel {

    private final ServiceRegistry s;
    private final CrudTablePanel<User> table;

    public UsersPanel(ServiceRegistry services) {
        this.s = services;
        List<EntityTableModel.Column<User>> cols = List.of(
                EntityTableModel.Column.of("Username", User::getUsername, 110),
                EntityTableModel.Column.of("Full name", User::getFullName, 160),
                EntityTableModel.Column.of("Email", User::getEmail, 190),
                EntityTableModel.Column.of("Role", u -> u.getRole().getDisplayName(), 150),
                EntityTableModel.Column.of("Active", u -> u.isActive() ? "Yes" : "No", 60),
                EntityTableModel.Column.of("Locked", u -> u.isLocked() ? "Yes" : "", 60),
                EntityTableModel.Column.of("Must change pw", u -> u.isMustChangePassword() ? "Yes" : "", 100),
                EntityTableModel.Column.of("Last login", u -> DateUtil.display(u.getLastLoginAt()), 130));
        table = new CrudTablePanel<>("Users & Roles", "System accounts. Roles: Administrator, Fleet Manager, Operations Officer, Viewer", cols);
        table.loader(() -> {
            String q = table.searchText();
            return s.auth.listUsers().stream().filter(u -> q == null || (u.getUsername() + " " + u.getFullName() + " " + u.getEmail() + " " + u.getRole()).toLowerCase().contains(q.toLowerCase())).toList();
        });
        table.addAction(UiUtils.primary("Add user", this::create));
        table.addAction(UiUtils.neutral("Edit", this::edit));
        table.addAction(UiUtils.neutral("Reset password", this::resetPassword));
        table.addAction(UiUtils.danger("Delete", this::delete));
        table.onDoubleClick(u -> edit());
    }

    @Override public String id() { return "users"; }
    @Override public String title() { return "Users"; }
    @Override public JComponent component() { return table; }
    @Override public void refresh() { table.refresh(); }

    private void create() {
        FormBuilder f = new FormBuilder();
        JTextField username = f.text("Username *", "");
        JTextField fullName = f.text("Full name *", "");
        JTextField email = f.text("Email *", "");
        JComboBox<Role> role = f.combo("Role *", Role.values(), Role.VIEWER);
        JPasswordField p1 = f.password("Initial password *");
        JPasswordField p2 = f.password("Confirm password *");
        f.addFull(UiUtils.muted("The user will be asked to change this password at first sign-in."));
        if (new FormDialog(UiUtils.windowOf(table), "Add user", f, "Create", () -> {
            if (!String.valueOf(p1.getPassword()).equals(String.valueOf(p2.getPassword()))) throw new IllegalArgumentException("Passwords do not match.");
            s.auth.createUser(FormBuilder.str(username), FormBuilder.str(email), FormBuilder.str(fullName), (Role) role.getSelectedItem(), p1.getPassword());
        }).showDialog()) refresh();
    }

    private void edit() {
        User u = table.requireSelected("user");
        if (u == null) return;
        FormBuilder f = new FormBuilder();
        f.add("Username", new JLabel(u.getUsername()));
        JTextField fullName = f.text("Full name *", u.getFullName());
        JTextField email = f.text("Email *", u.getEmail());
        JComboBox<Role> role = f.combo("Role *", Role.values(), u.getRole());
        JCheckBox active = f.check("Active", u.isActive());
        if (new FormDialog(UiUtils.windowOf(table), "Edit user", f, "Save", () ->
                s.auth.updateUser(u.getId(), FormBuilder.str(email), FormBuilder.str(fullName), (Role) role.getSelectedItem(), active.isSelected())).showDialog()) refresh();
    }

    private void resetPassword() {
        User u = table.requireSelected("user");
        if (u == null) return;
        FormBuilder f = new FormBuilder();
        f.add("User", new JLabel(u.getUsername()));
        JPasswordField p1 = f.password("New password *");
        JPasswordField p2 = f.password("Confirm *");
        if (new FormDialog(UiUtils.windowOf(table), "Reset password", f, "Reset", () -> {
            if (!String.valueOf(p1.getPassword()).equals(String.valueOf(p2.getPassword()))) throw new IllegalArgumentException("Passwords do not match.");
            s.auth.adminResetPassword(u.getId(), p1.getPassword());
        }).showDialog()) refresh();
    }

    private void delete() {
        User u = table.requireSelected("user");
        if (u != null && UiUtils.confirm(table, "Delete user", "Delete account '" + u.getUsername() + "'?") && UiUtils.run(table, () -> s.auth.deleteUser(u.getId()))) refresh();
    }
}
