package com.femzyk.vehiclesystem.gui;

import com.femzyk.vehiclesystem.interfaces.Vehicle;
import com.femzyk.vehiclesystem.model.Car;
import com.femzyk.vehiclesystem.model.Motorcycle;
import com.femzyk.vehiclesystem.model.Truck;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseWheelListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * FleetPanel
 *
 * PURPOSE:
 * Manages and displays the logged-in user's active rental fleet.
 *
 * RESPONSIBILITIES:
 * - Store active fleet vehicles.
 * - Store recycle bin vehicles.
 * - Group vehicles into Cars, Motorcycles, and Trucks sections.
 * - Add, edit, soft-delete, recover, and permanently delete vehicles.
 * - Notify StatsPanel whenever data changes.
 * - Auto-save changes through StorageManager.
 *
 * LAYOUT DESIGN:
 * The full fleet page scrolls vertically.
 *
 * Each vehicle type section has its own horizontal scroll bar:
 *
 *     CARS
 *     [Car 1] [Car 2] [Car 3] [Car 4]  horizontal scroll
 *
 *     MOTORCYCLES
 *     [Motorcycle 1] [Motorcycle 2]     horizontal scroll
 *
 *     TRUCKS
 *     [Truck 1] [Truck 2]               horizontal scroll
 *
 * TOUCHPAD / MOUSE WHEEL FIX:
 * Nested scroll panes can accidentally block two-finger vertical scrolling.
 * To solve that, each horizontal section forwards normal wheel scrolling to
 * the main vertical scroll pane. Shift + mouse wheel still scrolls sideways.
 *
 * IMPORTANT DELETE BEHAVIOR:
 * Delete from the active fleet does not permanently remove a vehicle.
 * It moves the vehicle to the recycle bin first.
 */
public class FleetPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /**
     * Active vehicles currently visible in the fleet registry.
     */
    private final List<Vehicle> fleet = new ArrayList<>();

    /**
     * Deleted vehicles waiting in recycle bin.
     */
    private final List<Vehicle> recycleBin = new ArrayList<>();

    /**
     * Sidebar statistics panel updated after every fleet change.
     */
    private final StatsPanel statsPanel;

    /**
     * Saves and loads fleet data for the currently logged-in user.
     */
    private final StorageManager storageManager;

    /**
     * Main scroll pane for vertical page scrolling.
     */
    private final JScrollPane mainVerticalScrollPane;

    /**
     * Main vertical content panel containing CARS, MOTORCYCLES, and TRUCKS sections.
     */
    private final ScrollableContentPanel contentPanel;

    /**
     * Constructs the fleet panel.
     *
     * @param statsPanel     stats panel to update
     * @param storageManager user-specific storage manager
     */
    public FleetPanel(StatsPanel statsPanel, StorageManager storageManager) {
        this.statsPanel = statsPanel;
        this.storageManager = storageManager;

        setLayout(new BorderLayout());
        setBackground(ThemeConstants.BG_PRIMARY);

        add(buildHeader(), BorderLayout.NORTH);

        /*
         * contentPanel holds all vehicle sections vertically.
         */
        contentPanel = new ScrollableContentPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(ThemeConstants.BG_PRIMARY);
        contentPanel.setBorder(new EmptyBorder(
            0,
            ThemeConstants.PADDING,
            ThemeConstants.PADDING,
            ThemeConstants.PADDING
        ));

        /*
         * Main scroll pane handles vertical scrolling.
         * Horizontal scrolling is disabled here because each vehicle section
         * has its own horizontal scroll pane.
         */
        mainVerticalScrollPane = new JScrollPane(contentPanel);
        mainVerticalScrollPane.setBackground(ThemeConstants.BG_PRIMARY);
        mainVerticalScrollPane.getViewport().setBackground(ThemeConstants.BG_PRIMARY);
        mainVerticalScrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainVerticalScrollPane.getVerticalScrollBar().setUnitIncrement(18);
        mainVerticalScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(mainVerticalScrollPane, BorderLayout.CENTER);

        showEmptyState();
    }

    /**
     * Builds the header above the fleet registry.
     */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());

        header.setBackground(ThemeConstants.BG_SECONDARY);
        header.setBorder(new EmptyBorder(
            ThemeConstants.PADDING,
            ThemeConstants.PADDING + 4,
            ThemeConstants.PADDING,
            ThemeConstants.PADDING
        ));

        JLabel title = new JLabel("FLEET REGISTRY");
        title.setFont(ThemeConstants.FONT_HEADING);
        title.setForeground(ThemeConstants.TEXT_PRIMARY);

        JLabel hint = new JLabel("Deleted vehicles move to Recycle Bin");
        hint.setFont(ThemeConstants.FONT_SMALL);
        hint.setForeground(ThemeConstants.TEXT_SECONDARY);

        header.add(title, BorderLayout.WEST);
        header.add(hint, BorderLayout.EAST);

        return header;
    }

    /**
     * Loads saved active fleet and recycle bin data from local user storage.
     */
    public void loadSavedData() {
        StoredFleetData data = storageManager.load();

        fleet.clear();
        fleet.addAll(data.getActiveVehicles());

        recycleBin.clear();
        recycleBin.addAll(data.getRecycledVehicles());

        refreshGrid();
        updateStats();
    }

    /**
     * Saves active fleet and recycle bin data.
     */
    public void saveNow() {
        try {
            storageManager.save(fleet, recycleBin);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                this,
                "Unable to save fleet data.\n\n" + e.getMessage(),
                "Save Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Displays an empty message when the user has no active vehicles.
     */
    private void showEmptyState() {
        contentPanel.removeAll();

        JLabel empty = new JLabel(
            "<html><center><br><br>No vehicles in fleet yet.<br><br>" +
            "Use the buttons above to add your first vehicle.</center></html>"
        );

        empty.setFont(ThemeConstants.FONT_BODY);
        empty.setForeground(ThemeConstants.TEXT_SECONDARY);
        empty.setAlignmentX(Component.LEFT_ALIGNMENT);
        empty.setBorder(new EmptyBorder(40, 40, 40, 40));

        contentPanel.add(empty);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Adds a vehicle to the active fleet.
     *
     * @param vehicle vehicle to add
     */
    public void addVehicle(Vehicle vehicle) {
        fleet.add(vehicle);
        refreshGrid();
        updateStats();
        saveNow();
    }

    /**
     * Soft delete.
     *
     * Moves a vehicle from active fleet into recycle bin.
     *
     * @param index active fleet index
     */
    public void removeVehicle(int index) {
        if (index >= 0 && index < fleet.size()) {
            Vehicle removed = fleet.remove(index);
            recycleBin.add(removed);

            refreshGrid();
            updateStats();
            saveNow();
        }
    }

    /**
     * Moves all active vehicles into recycle bin.
     *
     * Used by the Clear Fleet button.
     */
    public void moveAllToRecycleBin() {
        if (fleet.isEmpty()) {
            return;
        }

        recycleBin.addAll(fleet);
        fleet.clear();

        refreshGrid();
        updateStats();
        saveNow();
    }

    /**
     * Opens the correct edit dialog based on vehicle type.
     *
     * @param index active fleet index
     */
    private void editVehicle(int index) {
        if (index < 0 || index >= fleet.size()) {
            return;
        }

        Vehicle current = fleet.get(index);
        Vehicle updated = null;

        Window window = SwingUtilities.getWindowAncestor(this);
        Frame parent = window instanceof Frame ? (Frame) window : null;

        if (current instanceof Car car) {
            AddCarDialog dialog = new AddCarDialog(parent, car);
            dialog.setVisible(true);
            updated = dialog.getResult();

        } else if (current instanceof Motorcycle motorcycle) {
            AddMotorcycleDialog dialog = new AddMotorcycleDialog(parent, motorcycle);
            dialog.setVisible(true);
            updated = dialog.getResult();

        } else if (current instanceof Truck truck) {
            AddTruckDialog dialog = new AddTruckDialog(parent, truck);
            dialog.setVisible(true);
            updated = dialog.getResult();
        }

        if (updated != null) {
            fleet.set(index, updated);
            refreshGrid();
            updateStats();
            saveNow();
        }
    }

    /**
     * Confirms soft deletion before moving vehicle to recycle bin.
     *
     * @param index active fleet index
     */
    private void confirmMoveToRecycleBin(int index) {
        if (index < 0 || index >= fleet.size()) {
            return;
        }

        Vehicle vehicle = fleet.get(index);

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Move " + vehicle.getIdentityDisplay() + " to Recycle Bin?",
            "Move to Recycle Bin",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            removeVehicle(index);
        }
    }

    /**
     * Rebuilds all visible vehicle sections from the active fleet list.
     */
    private void refreshGrid() {
        contentPanel.removeAll();

        if (fleet.isEmpty()) {
            showEmptyState();
            return;
        }

        addVehicleSection("CARS", "Car");
        addVehicleSection("MOTORCYCLES", "Motorcycle");
        addVehicleSection("TRUCKS", "Truck");

        contentPanel.add(Box.createVerticalGlue());

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Adds one vehicle type section.
     *
     * Each section contains:
     * - A title, such as CARS (4)
     * - A horizontally scrollable card row
     *
     * This allows unlimited vehicles per type without hiding cards when the
     * window is smaller.
     */
    private void addVehicleSection(String sectionTitle, String vehicleType) {
        List<Integer> indexes = new ArrayList<>();

        for (int i = 0; i < fleet.size(); i++) {
            if (fleet.get(i).getVehicleType().equals(vehicleType)) {
                indexes.add(i);
            }
        }

        if (indexes.isEmpty()) {
            return;
        }

        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(ThemeConstants.BG_PRIMARY);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setBorder(new EmptyBorder(12, 0, 16, 0));

        JLabel title = new JLabel(sectionTitle + " (" + indexes.size() + ")");
        title.setFont(ThemeConstants.FONT_SUBHEADING);
        title.setForeground(getSectionColor(vehicleType));
        title.setBorder(new EmptyBorder(0, 0, 8, 0));

        /*
         * Cards are placed in a single horizontal row.
         */
        JPanel cardsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 12));
        cardsPanel.setBackground(ThemeConstants.BG_PRIMARY);

        int cardWidth = 390;
        int cardHeight = 245;
        int gap = 16;

        for (int index : indexes) {
            Vehicle vehicle = fleet.get(index);

            VehicleCardPanel card = new VehicleCardPanel(
                vehicle,
                () -> editVehicle(index),
                () -> confirmMoveToRecycleBin(index)
            );

            card.setComponentPopupMenu(buildContextMenu(index));

            cardsPanel.add(card);
        }

        /*
         * Force preferred width so every card exists side-by-side.
         * If the row is wider than the visible screen, horizontal scrollbar appears.
         */
        int preferredWidth = indexes.size() * (cardWidth + gap) + 40;
        int preferredHeight = cardHeight + 40;

        cardsPanel.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        cardsPanel.setMinimumSize(new Dimension(preferredWidth, preferredHeight));

        JScrollPane horizontalScrollPane = new JScrollPane(cardsPanel);
        horizontalScrollPane.setBorder(BorderFactory.createEmptyBorder());
        horizontalScrollPane.setBackground(ThemeConstants.BG_PRIMARY);
        horizontalScrollPane.getViewport().setBackground(ThemeConstants.BG_PRIMARY);

        /*
         * Always show horizontal scrollbar so users clearly know they can scroll sideways.
         */
        horizontalScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        horizontalScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        horizontalScrollPane.getHorizontalScrollBar().setUnitIncrement(18);

        /*
         * Fixed height prevents cards from being clipped vertically.
         */
        horizontalScrollPane.setPreferredSize(new Dimension(0, preferredHeight + 24));
        horizontalScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredHeight + 24));

        /*
         * Critical touchpad/mouse-wheel fix:
         * Normal wheel movement scrolls the full page vertically.
         * Shift + wheel scrolls the vehicle row horizontally.
         */
        installSectionScrollBehavior(horizontalScrollPane);

        section.add(title, BorderLayout.NORTH);
        section.add(horizontalScrollPane, BorderLayout.CENTER);

        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredHeight + 78));

        contentPanel.add(section);
    }

    /**
     * Fixes nested scroll pane behavior.
     *
     * Without this method, the horizontal scroll pane can capture two-finger
     * touchpad scrolling and prevent the main page from scrolling vertically.
     *
     * Behavior:
     * - Normal wheel/touchpad scroll moves the main page up/down.
     * - Shift + wheel moves the current vehicle row left/right.
     * - The visible horizontal scrollbar can always be dragged manually.
     *
     * @param horizontalScrollPane section-level horizontal scroll pane
     */
    private void installSectionScrollBehavior(JScrollPane horizontalScrollPane) {
        /*
         * Remove default wheel listeners from the section scroll pane.
         * This prevents it from swallowing vertical wheel events.
         */
        for (MouseWheelListener listener : horizontalScrollPane.getMouseWheelListeners()) {
            horizontalScrollPane.removeMouseWheelListener(listener);
        }

        horizontalScrollPane.addMouseWheelListener(event -> {
            JScrollBar verticalBar = mainVerticalScrollPane.getVerticalScrollBar();
            JScrollBar horizontalBar = horizontalScrollPane.getHorizontalScrollBar();

            /*
             * preciseWheelRotation works better with laptop touchpads.
             */
            int amount = (int) Math.round(event.getPreciseWheelRotation() * 40);

            if (amount == 0) {
                amount = event.getWheelRotation() * 18;
            }

            if (event.isShiftDown()) {
                /*
                 * Shift + wheel scrolls sideways.
                 */
                horizontalBar.setValue(horizontalBar.getValue() + amount);
            } else {
                /*
                 * Normal wheel/touchpad scroll controls the main vertical page.
                 */
                verticalBar.setValue(verticalBar.getValue() + amount);
            }

            event.consume();
        });
    }

    /**
     * Builds right-click menu for a vehicle card.
     */
    private JPopupMenu buildContextMenu(int index) {
        JPopupMenu contextMenu = new JPopupMenu();
        contextMenu.setBackground(ThemeConstants.BG_CARD);

        JMenuItem editItem = new JMenuItem("Edit Vehicle");
        editItem.setFont(ThemeConstants.FONT_BODY);
        editItem.setBackground(ThemeConstants.BG_CARD);
        editItem.setForeground(ThemeConstants.TEXT_PRIMARY);
        editItem.addActionListener(e -> editVehicle(index));

        JMenuItem removeItem = new JMenuItem("Move to Recycle Bin");
        removeItem.setFont(ThemeConstants.FONT_BODY);
        removeItem.setBackground(ThemeConstants.BG_CARD);
        removeItem.setForeground(ThemeConstants.TEXT_ERROR);
        removeItem.addActionListener(e -> confirmMoveToRecycleBin(index));

        contextMenu.add(editItem);
        contextMenu.add(removeItem);

        return contextMenu;
    }

    /**
     * Returns the section color based on vehicle type.
     */
    private Color getSectionColor(String vehicleType) {
        return switch (vehicleType) {
            case "Car" -> ThemeConstants.ACCENT_CAR;
            case "Motorcycle" -> ThemeConstants.ACCENT_MOTORCYCLE;
            case "Truck" -> ThemeConstants.ACCENT_TRUCK;
            default -> ThemeConstants.TEXT_ACCENT;
        };
    }

    /**
     * Opens the recycle bin window.
     */
    public void openRecycleBinDialog(Frame parent) {
        RecycleBinDialog dialog = new RecycleBinDialog(parent, this);
        dialog.setVisible(true);

        refreshGrid();
        updateStats();
        saveNow();
    }

    public List<Vehicle> getRecycleBin() {
        return new ArrayList<>(recycleBin);
    }

    public int getRecycleBinSize() {
        return recycleBin.size();
    }

    /**
     * Restores a vehicle from recycle bin back into active fleet.
     */
    public void recoverFromRecycleBin(int index) {
        if (index >= 0 && index < recycleBin.size()) {
            Vehicle recovered = recycleBin.remove(index);
            fleet.add(recovered);

            refreshGrid();
            updateStats();
            saveNow();
        }
    }

    /**
     * Permanently deletes a vehicle from recycle bin.
     */
    public void deleteFromRecycleBinPermanently(int index) {
        if (index >= 0 && index < recycleBin.size()) {
            recycleBin.remove(index);
            updateStats();
            saveNow();
        }
    }

    /**
     * Permanently clears all recycle bin items.
     */
    public void clearRecycleBin() {
        recycleBin.clear();
        updateStats();
        saveNow();
    }

    private void updateStats() {
        statsPanel.updateStats(fleet, recycleBin.size());
    }

    public List<Vehicle> getFleet() {
        return new ArrayList<>(fleet);
    }

    public int getFleetSize() {
        return fleet.size();
    }

    /**
     * Custom panel used inside the main vertical JScrollPane.
     *
     * It tracks viewport width so each section fills the available visible width.
     */
    private static class ScrollableContentPanel extends JPanel implements Scrollable {

        private static final long serialVersionUID = 1L;

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect,
                                              int orientation,
                                              int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect,
                                               int orientation,
                                               int direction) {
            return 90;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}