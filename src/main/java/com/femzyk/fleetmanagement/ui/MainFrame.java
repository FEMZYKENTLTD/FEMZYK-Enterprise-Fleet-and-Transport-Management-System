package com.femzyk.fleetmanagement.ui;

import com.femzyk.fleetmanagement.config.AppConfig;
import com.femzyk.fleetmanagement.model.Permission;
import com.femzyk.fleetmanagement.model.User;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import com.femzyk.fleetmanagement.ui.modules.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/** Main window: dark sidebar navigation on the left, module content on the right, status bar below. */
public class MainFrame extends JFrame {

    private final ServiceRegistry services;
    private final Runnable onLogout;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, ModulePanel> modules = new LinkedHashMap<>();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final JLabel status = new JLabel();
    private String currentId;

    public MainFrame(ServiceRegistry services, Runnable onLogout) {
        super(AppConfig.APP_NAME);
        this.services = services;
        this.onLogout = onLogout;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { exit(); } });
        User user = SessionContext.currentUser().orElseThrow();

        register(new DashboardPanel(services), Permission.VIEW_DASHBOARD);
        register(new EmployeesPanel(services), Permission.VIEW_RECORDS);
        register(new DriversPanel(services), Permission.VIEW_RECORDS);
        register(new VehiclesPanel(services), Permission.VIEW_RECORDS);
        register(new AssignmentsPanel(services), Permission.VIEW_RECORDS);
        register(new TripsPanel(services), Permission.VIEW_RECORDS);
        register(new MaintenancePanel(services), Permission.VIEW_RECORDS);
        register(new FuelPanel(services), Permission.VIEW_RECORDS);
        register(new ExpensesPanel(services), Permission.VIEW_RECORDS);
        register(new ReportsPanel(services), Permission.VIEW_REPORTS);
        register(new AuditLogPanel(services), Permission.VIEW_AUDIT_LOG);
        register(new UsersPanel(services), Permission.MANAGE_USERS);
        SystemPanel system = new SystemPanel(services, this::databaseReplaced);
        system.addPropertyChangeListener("dataChanged", e -> refreshAll());
        register(system, Permission.EXPORT_DATA);

        JPanel sidebar = buildSidebar(user);
        JPanel root = new JPanel(new BorderLayout());
        root.add(sidebar, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER), BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        status.setFont(Theme.FONT_SMALL);
        status.setForeground(Theme.TEXT_MUTED);
        root.add(status, BorderLayout.SOUTH);
        setContentPane(root);
        setJMenuBar(buildMenu());
        setSize(1360, 840);
        setMinimumSize(new Dimension(1024, 680));
        setLocationRelativeTo(null);
        setStatus("Signed in as " + user.getFullName() + " (" + user.getRole().getDisplayName() + ")  ·  database: " + services.database().getDatabasePath());
        show("dashboard");
    }

    private void register(ModulePanel m, Permission required) {
        if (!SessionContext.has(required)) return;
        modules.put(m.id(), m);
        content.add(m.component(), m.id());
    }

    private JPanel buildSidebar(User user) {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(Theme.SIDEBAR_BG);
        side.setPreferredSize(new Dimension(220, 0));
        JLabel brand = new JLabel("FEMZYK");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 24)); brand.setForeground(Color.WHITE);
        brand.setBorder(BorderFactory.createEmptyBorder(18, 18, 0, 18)); brand.setAlignmentX(LEFT_ALIGNMENT);
        JLabel sub = new JLabel("<html>Enterprise Fleet &amp; Transport<br>Management System</html>");
        sub.setFont(Theme.FONT_SMALL); sub.setForeground(Theme.TEXT_MUTED);
        sub.setBorder(BorderFactory.createEmptyBorder(2, 18, 16, 18)); sub.setAlignmentX(LEFT_ALIGNMENT);
        side.add(brand); side.add(sub);
        String[] groups = {"OVERVIEW", "dashboard", "PEOPLE", "employees", "drivers", "users", "FLEET", "vehicles", "assignments", "trips", "OPERATIONS", "maintenance", "fuel", "expenses", "INSIGHT", "reports", "audit", "system"};
        for (String g : groups) {
            if (g.equals(g.toUpperCase())) {
                boolean any = false;
                for (String id : modules.keySet()) if (groupOf(id).equals(g)) any = true;
                if (!any) continue;
                JLabel l = new JLabel(g);
                l.setFont(new Font("Segoe UI", Font.BOLD, 10)); l.setForeground(Theme.TEXT_MUTED);
                l.setBorder(BorderFactory.createEmptyBorder(10, 18, 4, 18)); l.setAlignmentX(LEFT_ALIGNMENT);
                side.add(l);
                continue;
            }
            ModulePanel m = modules.get(g);
            if (m == null) continue;
            JButton b = new JButton(m.title());
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setFont(Theme.FONT_BODY); b.setForeground(Theme.SIDEBAR_TEXT); b.setBackground(Theme.SIDEBAR_BG);
            b.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18)); b.setFocusPainted(false); b.setContentAreaFilled(false); b.setOpaque(true);
            b.setAlignmentX(LEFT_ALIGNMENT); b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            b.addActionListener(e -> show(m.id()));
            navButtons.put(m.id(), b);
            side.add(b);
        }
        side.add(Box.createVerticalGlue());
        JLabel who = new JLabel("<html>" + UiUtils.escape(user.getFullName()) + "<br><span style='color:#9ca3af'>" + user.getRole().getDisplayName() + "</span></html>");
        who.setForeground(Color.WHITE); who.setFont(Theme.FONT_SMALL); who.setBorder(BorderFactory.createEmptyBorder(8, 18, 4, 18)); who.setAlignmentX(LEFT_ALIGNMENT);
        side.add(who);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        bottom.setOpaque(false); bottom.setAlignmentX(LEFT_ALIGNMENT); bottom.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        bottom.add(UiUtils.button("Password", Theme.SIDEBAR_HOVER, () -> ChangePasswordDialog.show(this, services, false)));
        bottom.add(UiUtils.button("Sign out", Theme.SIDEBAR_HOVER, this::logout));
        side.add(bottom);
        return side;
    }

    private static String groupOf(String id) {
        switch (id) {
            case "dashboard": return "OVERVIEW";
            case "employees": case "drivers": case "users": return "PEOPLE";
            case "vehicles": case "assignments": case "trips": return "FLEET";
            case "maintenance": case "fuel": case "expenses": return "OPERATIONS";
            default: return "INSIGHT";
        }
    }

    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem refresh = new JMenuItem("Refresh current view"); refresh.setAccelerator(KeyStroke.getKeyStroke("F5")); refresh.addActionListener(e -> refreshCurrent());
        file.add(refresh);
        file.addSeparator();
        JMenuItem logout = new JMenuItem("Sign out"); logout.addActionListener(e -> logout());
        JMenuItem exit = new JMenuItem("Exit"); exit.addActionListener(e -> exit());
        file.add(logout); file.add(exit);
        JMenu go = new JMenu("Go");
        for (ModulePanel m : modules.values()) { JMenuItem i = new JMenuItem(m.title()); i.addActionListener(e -> show(m.id())); go.add(i); }
        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About"); about.addActionListener(e -> about());
        JMenuItem guide = new JMenuItem("Quick guide"); guide.addActionListener(e -> quickGuide());
        help.add(guide); help.add(about);
        bar.add(file); bar.add(go); bar.add(help);
        return bar;
    }

    public void show(String id) {
        ModulePanel m = modules.get(id);
        if (m == null) return;
        currentId = id;
        navButtons.forEach((k, b) -> { b.setBackground(k.equals(id) ? Theme.SIDEBAR_ACTIVE : Theme.SIDEBAR_BG); b.setForeground(k.equals(id) ? Color.WHITE : Theme.SIDEBAR_TEXT); });
        cards.show(content, id);
        m.refresh();
    }

    public void refreshCurrent() { if (currentId != null) modules.get(currentId).refresh(); }
    public void refreshAll() { refreshCurrent(); }

    public void setStatus(String text) { status.setText(text); }

    private void logout() {
        services.auth.logout();
        dispose();
        onLogout.run();
    }

    private void databaseReplaced() {
        SessionContext.logout();
        dispose();
        onLogout.run();
    }

    private void exit() {
        if (UiUtils.confirm(this, "Exit", "Close " + AppConfig.APP_SHORT_NAME + "?")) {
            services.auth.logout();
            services.database().close();
            dispose();
            System.exit(0);
        }
    }

    private void about() {
        UiUtils.info(this, "About", AppConfig.APP_NAME + "\nVersion " + AppConfig.APP_VERSION + "\n\n"
                + "Lagos State University - CSC 392 SIWES project\nFEMZYK ENT LTD\n\n"
                + "Evolved from the Femzyk Vehicle Management System (v4.x), Employee Management System, Course Management System, Contact Book Pro and Stock Analysis projects.\n"
                + "Java 17 · Swing · SQLite · Maven · JUnit 5");
    }

    private void quickGuide() {
        UiUtils.info(this, "Quick guide",
                "1. Employees: add staff first. 2. Drivers: create a driver profile for an employee with licence details.\n"
                + "3. Vehicles: register vehicles (car, truck, motorcycle, van, bus).\n"
                + "4. Assignments: give an AVAILABLE vehicle to an ACTIVE driver (licence class is checked).\n"
                + "5. Trips: plan → start (vehicle IN SERVICE) → complete with end odometer (mileage updates).\n"
                + "6. Maintenance / Fuel: log work and purchases; expenses are posted automatically.\n"
                + "7. Reports: generate and export CSV / HTML / PDF. 8. Data & System: backups, CSV import, demo data.\n\n"
                + "Roles: Administrator (everything), Fleet Manager (all records), Operations Officer (trips, maintenance, fuel, expenses), Viewer (read-only).");
    }
}
