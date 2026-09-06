package com.femzyk.fleetmanagement.ui.modules;

import com.femzyk.fleetmanagement.model.Driver;
import com.femzyk.fleetmanagement.service.DashboardService;
import com.femzyk.fleetmanagement.service.MaintenanceService;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import com.femzyk.fleetmanagement.ui.ModulePanel;
import com.femzyk.fleetmanagement.ui.Theme;
import com.femzyk.fleetmanagement.ui.UiUtils;
import com.femzyk.fleetmanagement.ui.components.BarChartPanel;
import com.femzyk.fleetmanagement.ui.components.PieChartPanel;
import com.femzyk.fleetmanagement.ui.components.StatCard;
import com.femzyk.fleetmanagement.util.DateUtil;
import com.femzyk.fleetmanagement.util.MoneyUtil;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Live KPI dashboard. Every number comes from DashboardService (SQL aggregates), refreshed on each visit. */
public class DashboardPanel extends JPanel implements ModulePanel {

    private final ServiceRegistry s;
    private final StatCard vehicles = new StatCard("Fleet size", Theme.PRIMARY);
    private final StatCard available = new StatCard("Available", Theme.SUCCESS);
    private final StatCard onRoad = new StatCard("Assigned / in service", Theme.INFO);
    private final StatCard inMaint = new StatCard("In maintenance", Theme.WARNING);
    private final StatCard staff = new StatCard("Employees", Theme.ACCENT_BUS);
    private final StatCard drivers = new StatCard("Active drivers", Theme.ACCENT_VAN);
    private final StatCard trips = new StatCard("Trips (active / planned)", Theme.ACCENT_CAR);
    private final StatCard monthCost = new StatCard("Expenses this month", Theme.ACCENT_TRUCK);
    private final PieChartPanel statusChart = new PieChartPanel("Vehicles by status", Theme::statusColor);
    private final PieChartPanel typeChart = new PieChartPanel("Vehicles by type", Theme::vehicleTypeColor);
    private final BarChartPanel expenseChart = new BarChartPanel("Expenses - last 6 months");
    private final BarChartPanel fuelChart = new BarChartPanel("Fuel spend - last 6 months");
    private final PieChartPanel categoryChart = new PieChartPanel("Expenses by category (year to date)");
    private final BarChartPanel deptChart = new BarChartPanel("Headcount by department");
    private final JPanel alerts = new JPanel();
    private final JLabel updated = UiUtils.muted("");

    public DashboardPanel(ServiceRegistry services) {
        super(new BorderLayout(0, 12));
        this.s = services;
        setBackground(Theme.BG);
        setBorder(Theme.padding(16));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(UiUtils.title("Dashboard"));
        titles.add(updated);
        header.add(titles, BorderLayout.WEST);
        header.add(UiUtils.neutral("Refresh", this::refresh), BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel() {
            @Override public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                Container vp = getParent();
                return vp instanceof JViewport ? new Dimension(vp.getWidth(), d.height) : d;
            }
        };
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JPanel cards = new JPanel(new GridLayout(2, 4, 12, 12));
        cards.setOpaque(false);
        for (StatCard c : new StatCard[]{vehicles, available, onRoad, inMaint, staff, drivers, trips, monthCost}) cards.add(c);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));
        body.add(cards);
        body.add(Box.createVerticalStrut(12));

        JPanel charts = new JPanel(new GridLayout(2, 3, 12, 12));
        charts.setOpaque(false);
        expenseChart.setValueFormat(v -> v >= 1000 ? String.format("%,.0fk", v / 1000) : String.format("%,.0f", v));
        fuelChart.setValueFormat(expenseChart == null ? null : v -> v >= 1000 ? String.format("%,.0fk", v / 1000) : String.format("%,.0f", v));
        fuelChart.setBarColor(Theme.ACCENT_MOTORCYCLE);
        deptChart.setBarColor(Theme.ACCENT_BUS);
        charts.add(statusChart); charts.add(typeChart); charts.add(expenseChart);
        charts.add(fuelChart); charts.add(categoryChart); charts.add(deptChart);
        charts.setMaximumSize(new Dimension(Integer.MAX_VALUE, 560));
        body.add(charts);
        body.add(Box.createVerticalStrut(12));

        alerts.setLayout(new BoxLayout(alerts, BoxLayout.Y_AXIS));
        alerts.setBackground(Theme.CARD);
        alerts.setBorder(Theme.cardBorder());
        alerts.setAlignmentX(LEFT_ALIGNMENT);
        body.add(alerts);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BG);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
    }

    @Override public String id() { return "dashboard"; }
    @Override public String title() { return "Dashboard"; }
    @Override public JComponent component() { return this; }

    @Override
    public void refresh() {
        DashboardService.Snapshot d = UiUtils.call(this, s.dashboard::snapshot);
        if (d == null) return;
        vehicles.set(d.totalVehicles(), d.retiredVehicles() + " retired · " + d.outOfServiceVehicles() + " out of service");
        available.set(d.availableVehicles(), "ready for assignment");
        onRoad.set(d.assignedVehicles() + d.inServiceVehicles(), d.assignedVehicles() + " assigned · " + d.inServiceVehicles() + " on a trip");
        inMaint.set(d.maintenanceVehicles(), d.overdueMaintenance() + " overdue · " + d.dueSoonMaintenance() + " due soon");
        inMaint.setValueColor(d.overdueMaintenance() > 0 ? Theme.DANGER : Theme.TEXT);
        staff.set(d.totalEmployees(), d.activeEmployees() + " active");
        drivers.set(d.activeDrivers(), d.expiredLicences() + " expired · " + d.expiringLicences() + " expiring licences");
        drivers.setValueColor(d.expiredLicences() > 0 ? Theme.DANGER : Theme.TEXT);
        trips.set(d.activeTrips() + " / " + d.plannedTrips(), d.completedTripsThisMonth() + " completed this month · " + String.format("%,d km", d.monthDistanceKm()));
        monthCost.set(MoneyUtil.format(d.monthTotalExpenses()), "fuel " + MoneyUtil.format(d.monthFuelCost()) + " · maint. " + MoneyUtil.format(d.monthMaintenanceCost()));

        Map<String, Double> st = new LinkedHashMap<>();
        d.vehiclesByStatus().forEach((k, v) -> { if (v > 0) st.put(k.name(), v.doubleValue()); });
        statusChart.setData(st);
        Map<String, Double> ty = new LinkedHashMap<>();
        d.vehiclesByType().forEach((k, v) -> { if (v > 0) ty.put(k.name(), v.doubleValue()); });
        typeChart.setData(ty);
        expenseChart.setData(d.expensesByMonth());
        fuelChart.setData(d.fuelByMonth());
        Map<String, Double> cat = new LinkedHashMap<>();
        d.expensesByCategory().forEach((k, v) -> { if (v > 0) cat.put(k.name(), v); });
        categoryChart.setData(cat);
        Map<String, Double> dept = new LinkedHashMap<>();
        d.employeesByDepartment().forEach((k, v) -> dept.put(k == null ? "Unassigned" : k, v.doubleValue()));
        deptChart.setData(dept);

        alerts.removeAll();
        JLabel h = UiUtils.heading("Attention required");
        h.setAlignmentX(LEFT_ALIGNMENT);
        alerts.add(h);
        int n = 0;
        for (MaintenanceService.DueItem item : d.maintenanceAlerts()) {
            String due = item.record().getNextServiceDate() != null ? "due " + DateUtil.display(item.record().getNextServiceDate()) : "";
            if (item.record().getNextServiceMileage() != null) due += (due.isEmpty() ? "" : ", ") + "due at " + String.format("%,d km", item.record().getNextServiceMileage())
                    + " (now " + String.format("%,d km", item.record().getVehicleCurrentMileage() == null ? 0 : item.record().getVehicleCurrentMileage()) + ")";
            alerts.add(alertLine(item.dueStatus(), "Maintenance " + item.dueStatus().toLowerCase() + ": " + item.record().getVehicleDisplay() + " — " + due));
            n++;
        }
        for (Driver dr : d.licenceAlerts()) {
            String state = dr.isLicenceExpired() ? "EXPIRED" : "EXPIRING SOON";
            alerts.add(alertLine(state, "Licence " + state.toLowerCase() + ": " + dr.getFullName() + " (" + dr.getLicenceNumber() + ") — " + DateUtil.display(dr.getLicenceExpiryDate())));
            n++;
        }
        if (n == 0) { JLabel ok = new JLabel("No overdue maintenance or licence problems."); ok.setForeground(Theme.SUCCESS); ok.setAlignmentX(LEFT_ALIGNMENT); alerts.add(ok); }
        updated.setText("Live figures from the database · refreshed " + DateUtil.display(java.time.LocalDateTime.now()) + " · today " + DateUtil.display(LocalDate.now()));
        alerts.revalidate();
        revalidate();
        repaint();
    }

    private JComponent alertLine(String status, String text) {
        JLabel l = new JLabel("●  " + text);
        l.setForeground(Theme.statusColor(status));
        l.setFont(Theme.FONT_BODY);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        return l;
    }
}
