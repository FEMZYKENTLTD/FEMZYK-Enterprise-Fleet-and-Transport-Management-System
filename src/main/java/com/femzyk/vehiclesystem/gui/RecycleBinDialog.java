package com.femzyk.vehiclesystem.gui;

import com.femzyk.vehiclesystem.interfaces.Vehicle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * RecycleBinDialog
 *
 * PURPOSE:
 * Displays vehicles that have been deleted from the active fleet but not
 * permanently removed.
 *
 * SUPPORTED ACTIONS:
 * - Recover selected vehicle(s)
 * - Delete selected vehicle(s) permanently
 * - Clear entire recycle bin
 * - Close dialog
 *
 * DATA FLOW:
 * This dialog delegates recycle bin operations to FleetPanel. FleetPanel is
 * the owner of the active fleet and recycle bin lists, so all modifications
 * happen there.
 */
public class RecycleBinDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final FleetPanel fleetPanel;

    private DefaultListModel<Vehicle> listModel;
    private JList<Vehicle> recycleList;
    private JLabel statusLabel;

    private JButton recoverBtn;
    private JButton deleteBtn;
    private JButton clearBtn;

    public RecycleBinDialog(Frame parent, FleetPanel fleetPanel) {
        super(parent, "Recycle Bin", true);

        this.fleetPanel = fleetPanel;

        setSize(650, 460);
        setLocationRelativeTo(parent);
        setResizable(false);

        buildUI();
        refreshList();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ThemeConstants.BG_CARD);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildListPanel(), BorderLayout.CENTER);
        root.add(buildButtonPanel(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeConstants.BTN_DANGER);
        header.setBorder(new EmptyBorder(14, 18, 14, 18));

        JLabel title = new JLabel("Recycle Bin");
        title.setFont(ThemeConstants.FONT_SUBHEADING);
        title.setForeground(Color.WHITE);

        JLabel hint = new JLabel("Recover or permanently delete removed vehicles");
        hint.setFont(ThemeConstants.FONT_SMALL);
        hint.setForeground(Color.WHITE);

        header.add(title, BorderLayout.WEST);
        header.add(hint, BorderLayout.EAST);

        return header;
    }

    private JPanel buildListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ThemeConstants.BG_CARD);
        panel.setBorder(new EmptyBorder(16, 18, 12, 18));

        listModel = new DefaultListModel<>();

        recycleList = new JList<>(listModel);
        recycleList.setBackground(ThemeConstants.BG_PRIMARY);
        recycleList.setForeground(ThemeConstants.TEXT_PRIMARY);
        recycleList.setFont(ThemeConstants.FONT_BODY);
        recycleList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        recycleList.setFixedCellHeight(60);

        recycleList.setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {

                JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

                if (value instanceof Vehicle vehicle) {
                    label.setText(
                        "<html><b>" + vehicle.getVehicleType() + ":</b> " +
                        vehicle.getIdentityDisplay() +
                        "<br><span style='color:#8c9bb9;'>Renter: " +
                        vehicle.getRenterDisplay() +
                        "</span></html>"
                    );
                }

                label.setFont(ThemeConstants.FONT_BODY);
                label.setOpaque(true);
                label.setBorder(new EmptyBorder(6, 10, 6, 10));

                if (isSelected) {
                    label.setBackground(ThemeConstants.BG_HOVER);
                    label.setForeground(ThemeConstants.TEXT_PRIMARY);
                } else {
                    label.setBackground(ThemeConstants.BG_PRIMARY);
                    label.setForeground(ThemeConstants.TEXT_PRIMARY);
                }

                return label;
            }
        });

        JScrollPane scrollPane = new JScrollPane(recycleList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(ThemeConstants.BG_PRIMARY);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(ThemeConstants.FONT_SMALL);
        statusLabel.setForeground(ThemeConstants.TEXT_SECONDARY);
        statusLabel.setBorder(new EmptyBorder(8, 0, 0, 0));

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildButtonPanel() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        buttons.setBackground(ThemeConstants.BG_CARD);
        buttons.setBorder(new EmptyBorder(0, 14, 10, 14));

        recoverBtn = createButton("Recover", ThemeConstants.BTN_SUCCESS, 110);
        deleteBtn = createButton("Delete Permanently", ThemeConstants.BTN_DANGER, 170);
        clearBtn = createButton("Clear Bin", ThemeConstants.BTN_DANGER, 110);
        JButton closeBtn = createButton("Close", ThemeConstants.BTN_NEUTRAL, 100);

        recoverBtn.addActionListener(e -> recoverSelected());
        deleteBtn.addActionListener(e -> deleteSelectedPermanently());
        clearBtn.addActionListener(e -> clearBin());
        closeBtn.addActionListener(e -> dispose());

        buttons.add(recoverBtn);
        buttons.add(deleteBtn);
        buttons.add(clearBtn);
        buttons.add(closeBtn);

        return buttons;
    }

    private JButton createButton(String text, Color color, int width) {
        JButton btn = new JButton(text);
        btn.setFont(ThemeConstants.FONT_BUTTON);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(width, ThemeConstants.BTN_HEIGHT));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void refreshList() {
        listModel.clear();

        for (Vehicle vehicle : fleetPanel.getRecycleBin()) {
            listModel.addElement(vehicle);
        }

        int count = listModel.size();
        statusLabel.setText(count + " item(s) in recycle bin.");

        boolean hasItems = count > 0;
        recoverBtn.setEnabled(hasItems);
        deleteBtn.setEnabled(hasItems);
        clearBtn.setEnabled(hasItems);
    }

    private void recoverSelected() {
        int[] selected = recycleList.getSelectedIndices();

        if (selected.length == 0) {
            JOptionPane.showMessageDialog(
                this,
                "Please select at least one vehicle to recover.",
                "Recover Vehicle",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        for (int i = selected.length - 1; i >= 0; i--) {
            fleetPanel.recoverFromRecycleBin(selected[i]);
        }

        refreshList();
    }

    private void deleteSelectedPermanently() {
        int[] selected = recycleList.getSelectedIndices();

        if (selected.length == 0) {
            JOptionPane.showMessageDialog(
                this,
                "Please select at least one vehicle to delete permanently.",
                "Delete Permanently",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Permanently delete " + selected.length +
            " selected vehicle(s)?\nThis cannot be undone.",
            "Permanent Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        for (int i = selected.length - 1; i >= 0; i--) {
            fleetPanel.deleteFromRecycleBinPermanently(selected[i]);
        }

        refreshList();
    }

    private void clearBin() {
        if (fleetPanel.getRecycleBinSize() == 0) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Clear all items from the recycle bin?\nThis cannot be undone.",
            "Clear Recycle Bin",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            fleetPanel.clearRecycleBin();
            refreshList();
        }
    }
}