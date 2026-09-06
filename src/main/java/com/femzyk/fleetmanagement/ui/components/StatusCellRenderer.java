package com.femzyk.fleetmanagement.ui.components;

import com.femzyk.fleetmanagement.ui.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/** Colours status-like values (AVAILABLE, OVERDUE, ...) using the theme palette. */
public class StatusCellRenderer extends DefaultTableCellRenderer {

    private final boolean vehicleType;

    public StatusCellRenderer() { this(false); }
    public StatusCellRenderer(boolean vehicleType) { this.vehicleType = vehicleType; }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String text = value == null ? "" : value.toString();
        l.setText(text.replace('_', ' '));
        l.setFont(Theme.FONT_SUBHEADING);
        if (!isSelected) l.setForeground(vehicleType ? Theme.vehicleTypeColor(text) : Theme.statusColor(text));
        return l;
    }
}
