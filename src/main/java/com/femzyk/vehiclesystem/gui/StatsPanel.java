package com.femzyk.vehiclesystem.gui;

import com.femzyk.vehiclesystem.interfaces.Vehicle;
import com.femzyk.vehiclesystem.model.Car;
import com.femzyk.vehiclesystem.model.Motorcycle;
import com.femzyk.vehiclesystem.model.Truck;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * StatsPanel
 *
 * PURPOSE:
 * Displays real-time fleet statistics for the logged-in user.
 *
 * DISPLAYED DATA:
 * - Current user
 * - Total active vehicles
 * - Cars count
 * - Motorcycles count
 * - Trucks count
 * - Recycle bin count
 * - Last added active vehicle
 *
 * DESIGN NOTE:
 * Recycle bin vehicles are not counted as active fleet vehicles.
 */
public class StatsPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private JLabel userLabel;
    private JLabel totalLabel;
    private JLabel carLabel;
    private JLabel motoLabel;
    private JLabel truckLabel;
    private JLabel recycleLabel;
    private JLabel newestLabel;

    private String currentUsername = "";

    public StatsPanel() {
        setBackground(ThemeConstants.BG_SECONDARY);
        setBorder(new EmptyBorder(
            ThemeConstants.PADDING,
            ThemeConstants.PADDING,
            ThemeConstants.PADDING,
            ThemeConstants.PADDING
        ));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(230, 0));

        buildUI();
    }

    private void buildUI() {
        JLabel header = new JLabel("FLEET STATS");
        header.setFont(ThemeConstants.FONT_SUBHEADING);
        header.setForeground(ThemeConstants.TEXT_ACCENT);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(header);
        add(Box.createVerticalStrut(16));
        addSeparator();
        add(Box.createVerticalStrut(14));

        userLabel = createPlainLabel("User: -");
        add(userLabel);
        add(Box.createVerticalStrut(12));

        totalLabel = createStatLabel("Total Vehicles", "0", ThemeConstants.TEXT_PRIMARY);
        add(totalLabel);
        add(Box.createVerticalStrut(12));

        JLabel typeHeader = createPlainLabel("By Type:");
        typeHeader.setFont(ThemeConstants.FONT_SMALL);
        typeHeader.setForeground(ThemeConstants.TEXT_SECONDARY);
        add(typeHeader);
        add(Box.createVerticalStrut(6));

        carLabel = createStatLabel("Cars", "0", ThemeConstants.ACCENT_CAR);
        motoLabel = createStatLabel("Motorcycles", "0", ThemeConstants.ACCENT_MOTORCYCLE);
        truckLabel = createStatLabel("Trucks", "0", ThemeConstants.ACCENT_TRUCK);

        add(carLabel);
        add(Box.createVerticalStrut(4));
        add(motoLabel);
        add(Box.createVerticalStrut(4));
        add(truckLabel);
        add(Box.createVerticalStrut(16));

        addSeparator();
        add(Box.createVerticalStrut(12));

        recycleLabel = createStatLabel("Recycle Bin", "0", ThemeConstants.BTN_DANGER);
        add(recycleLabel);
        add(Box.createVerticalStrut(16));

        addSeparator();
        add(Box.createVerticalStrut(12));

        JLabel recentHeader = createPlainLabel("Last Added:");
        recentHeader.setFont(ThemeConstants.FONT_SMALL);
        recentHeader.setForeground(ThemeConstants.TEXT_SECONDARY);

        add(recentHeader);
        add(Box.createVerticalStrut(4));

        newestLabel = createPlainLabel("<html><i>None yet</i></html>");
        add(newestLabel);
    }

    private void addSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(ThemeConstants.BORDER_SUBTLE);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        add(sep);
    }

    private JLabel createPlainLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(ThemeConstants.FONT_BODY);
        label.setForeground(ThemeConstants.TEXT_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel createStatLabel(String name, String value, Color color) {
        JLabel label = new JLabel(formatStat(name, value, color));
        label.setFont(ThemeConstants.FONT_BODY);
        label.setForeground(ThemeConstants.TEXT_PRIMARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private String formatStat(String name, String value, Color color) {
        return String.format(
            "<html>%s: <b><font color='#%02x%02x%02x'>%s</font></b></html>",
            name,
            color.getRed(),
            color.getGreen(),
            color.getBlue(),
            value
        );
    }

    public void setCurrentUser(String username) {
        this.currentUsername = username == null ? "" : username;
        userLabel.setText("User: " + this.currentUsername);
    }

    /**
     * Updates statistics from active fleet and recycle bin count.
     */
    public void updateStats(List<Vehicle> fleet, int recycleBinCount) {
        int cars = 0;
        int motos = 0;
        int trucks = 0;

        for (Vehicle vehicle : fleet) {
            if (vehicle instanceof Car) {
                cars++;
            } else if (vehicle instanceof Motorcycle) {
                motos++;
            } else if (vehicle instanceof Truck) {
                trucks++;
            }
        }

        totalLabel.setText(formatStat(
            "Total Vehicles",
            String.valueOf(fleet.size()),
            ThemeConstants.TEXT_PRIMARY
        ));

        carLabel.setText(formatStat(
            "Cars",
            String.valueOf(cars),
            ThemeConstants.ACCENT_CAR
        ));

        motoLabel.setText(formatStat(
            "Motorcycles",
            String.valueOf(motos),
            ThemeConstants.ACCENT_MOTORCYCLE
        ));

        truckLabel.setText(formatStat(
            "Trucks",
            String.valueOf(trucks),
            ThemeConstants.ACCENT_TRUCK
        ));

        recycleLabel.setText(formatStat(
            "Recycle Bin",
            String.valueOf(recycleBinCount),
            ThemeConstants.BTN_DANGER
        ));

        if (!fleet.isEmpty()) {
            Vehicle newest = fleet.get(fleet.size() - 1);
            newestLabel.setText("<html><i>" + newest.getIdentityDisplay() + "</i></html>");
        } else {
            newestLabel.setText("<html><i>None yet</i></html>");
        }

        revalidate();
        repaint();
    }

    /**
     * Backward-compatible method for older calls that only pass active fleet.
     */
    public void updateStats(List<Vehicle> fleet) {
        updateStats(fleet, 0);
    }
}