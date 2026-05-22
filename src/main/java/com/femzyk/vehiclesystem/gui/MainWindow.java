package com.femzyk.vehiclesystem.gui;

import com.femzyk.vehiclesystem.model.Car;
import com.femzyk.vehiclesystem.model.Motorcycle;
import com.femzyk.vehiclesystem.model.Truck;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * MainWindow
 *
 * PURPOSE:
 * Main application window shown after a user logs in.
 *
 * RESPONSIBILITIES:
 * - Create the main layout.
 * - Create toolbar buttons.
 * - Open add vehicle dialogs.
 * - Open recycle bin.
 * - Handle clear fleet.
 * - Handle logout.
 * - Save data on close/logout.
 *
 * USER-SPECIFIC STORAGE:
 * MainWindow receives the authenticated username. It creates a StorageManager
 * for that user and passes it to FleetPanel.
 */
public class MainWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private final String username;
    private final Runnable logoutCallback;
    private final StorageManager storageManager;

    private FleetPanel fleetPanel;
    private StatsPanel statsPanel;

    public MainWindow(String username, Runnable logoutCallback) {
        super("Femzyk Car Rental Agency - Vehicle Management System v4.0");

        this.username = username;
        this.logoutCallback = logoutCallback;
        this.storageManager = new StorageManager(username);

        applyLookAndFeel();

        setTitle("Femzyk Car Rental Agency - Vehicle Management System v4.0 | User: " + username);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1320, 760);
        setMinimumSize(new Dimension(1100, 640));
        setLocationRelativeTo(null);

        buildUI();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        setVisible(true);
    }

    private void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fall back silently.
        }

        getContentPane().setBackground(ThemeConstants.BG_PRIMARY);
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        statsPanel = new StatsPanel();
        statsPanel.setCurrentUser(username);

        fleetPanel = new FleetPanel(statsPanel, storageManager);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(ThemeConstants.BG_PRIMARY);
        content.add(fleetPanel, BorderLayout.CENTER);
        content.add(statsPanel, BorderLayout.EAST);

        add(buildToolbar(), BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        fleetPanel.loadSavedData();
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(30, 0));
        toolbar.setBackground(ThemeConstants.BG_SECONDARY);
        toolbar.setBorder(new EmptyBorder(12, 22, 12, 24));

        JLabel brand = new JLabel("FEMZYK RENTAL FLEET MANAGER");
        brand.setFont(ThemeConstants.FONT_HEADING);
        brand.setForeground(ThemeConstants.TEXT_ACCENT);
        brand.setBorder(new EmptyBorder(0, 0, 0, 24));

        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brandPanel.setOpaque(false);
        brandPanel.add(brand);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);

        JButton addCarBtn = createToolbarButton("Add Car", ThemeConstants.ACCENT_CAR, 112);
        JButton addMotoBtn = createToolbarButton("Add Motorcycle", ThemeConstants.ACCENT_MOTORCYCLE, 155);
        JButton addTruckBtn = createToolbarButton("Add Truck", ThemeConstants.ACCENT_TRUCK, 112);
        JButton recycleBtn = createToolbarButton("Recycle Bin", ThemeConstants.BTN_NEUTRAL, 130);
        JButton logoutBtn = createToolbarButton("Logout", ThemeConstants.BTN_NEUTRAL, 105);
        JButton clearBtn = createToolbarButton("Clear Fleet", ThemeConstants.BTN_DANGER, 125);

        addCarBtn.addActionListener(e -> handleAddCar());
        addMotoBtn.addActionListener(e -> handleAddMotorcycle());
        addTruckBtn.addActionListener(e -> handleAddTruck());
        recycleBtn.addActionListener(e -> fleetPanel.openRecycleBinDialog(this));
        logoutBtn.addActionListener(e -> logout());
        clearBtn.addActionListener(e -> handleClearFleet());

        btnRow.add(addCarBtn);
        btnRow.add(addMotoBtn);
        btnRow.add(addTruckBtn);
        btnRow.add(recycleBtn);
        btnRow.add(logoutBtn);
        btnRow.add(Box.createHorizontalStrut(10));
        btnRow.add(clearBtn);

        toolbar.add(brandPanel, BorderLayout.CENTER);
        toolbar.add(btnRow, BorderLayout.EAST);

        JSeparator sep = new JSeparator();
        sep.setForeground(ThemeConstants.BORDER_SUBTLE);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ThemeConstants.BG_SECONDARY);
        wrapper.add(toolbar, BorderLayout.CENTER);
        wrapper.add(sep, BorderLayout.SOUTH);

        return wrapper;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(ThemeConstants.BG_SECONDARY);
        bar.setBorder(new EmptyBorder(6, 16, 6, 16));

        JLabel status = new JLabel(
            "User: " + username +
            "  |  Auto-save enabled" +
            "  |  Recycle Bin enabled" +
            "  |  Data: " + storageManager.getStorageLocation()
        );

        status.setFont(ThemeConstants.FONT_SMALL);
        status.setForeground(ThemeConstants.TEXT_SECONDARY);

        bar.add(status, BorderLayout.WEST);

        return bar;
    }

    private void handleAddCar() {
        AddCarDialog dialog = new AddCarDialog(this);
        dialog.setVisible(true);

        Car car = dialog.getResult();

        if (car != null) {
            fleetPanel.addVehicle(car);
            showSuccessToast("Car added and saved: " + car.getIdentityDisplay());
        }
    }

    private void handleAddMotorcycle() {
        AddMotorcycleDialog dialog = new AddMotorcycleDialog(this);
        dialog.setVisible(true);

        Motorcycle motorcycle = dialog.getResult();

        if (motorcycle != null) {
            fleetPanel.addVehicle(motorcycle);
            showSuccessToast("Motorcycle added and saved: " + motorcycle.getIdentityDisplay());
        }
    }

    private void handleAddTruck() {
        AddTruckDialog dialog = new AddTruckDialog(this);
        dialog.setVisible(true);

        Truck truck = dialog.getResult();

        if (truck != null) {
            fleetPanel.addVehicle(truck);
            showSuccessToast("Truck added and saved: " + truck.getIdentityDisplay());
        }
    }

    private void handleClearFleet() {
        if (fleetPanel.getFleetSize() == 0) {
            JOptionPane.showMessageDialog(
                this,
                "The fleet is already empty.",
                "Clear Fleet",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Move ALL " + fleetPanel.getFleetSize() +
            " active vehicles to the Recycle Bin?\n\n" +
            "You can recover them later from the Recycle Bin.",
            "Clear Fleet",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            int count = fleetPanel.getFleetSize();
            fleetPanel.moveAllToRecycleBin();
            showSuccessToast(count + " vehicle(s) moved to Recycle Bin.");
        }
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Save and log out?",
            "Logout",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        fleetPanel.saveNow();
        dispose();

        if (logoutCallback != null) {
            SwingUtilities.invokeLater(logoutCallback);
        }
    }

    private void exitApplication() {
        if (fleetPanel != null) {
            fleetPanel.saveNow();
        }

        dispose();
        System.exit(0);
    }

    private void showSuccessToast(String message) {
        String originalTitle = getTitle();

        setTitle("[SAVED] " + message);

        Timer timer = new Timer(3000, e -> setTitle(originalTitle));
        timer.setRepeats(false);
        timer.start();
    }

    private JButton createToolbarButton(String text, Color color, int width) {
        JButton btn = new JButton(text);

        btn.setFont(ThemeConstants.FONT_BUTTON);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(width, ThemeConstants.BTN_HEIGHT));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            private final Color original = color;

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(color.brighter());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(original);
            }
        });

        return btn;
    }
}