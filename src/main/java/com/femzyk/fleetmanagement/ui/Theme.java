package com.femzyk.fleetmanagement.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Visual constants. Evolved from the v4.x ThemeConstants (dark navy palette) into a lighter, print-friendly
 * enterprise palette; the vehicle-type accent colours are kept.
 */
public final class Theme {

    private Theme() {}

    public static final Color PRIMARY = new Color(29, 78, 216);
    public static final Color PRIMARY_DARK = new Color(30, 58, 138);
    public static final Color PRIMARY_LIGHT = new Color(219, 234, 254);
    public static final Color SIDEBAR_BG = new Color(17, 24, 39);
    public static final Color SIDEBAR_HOVER = new Color(31, 41, 55);
    public static final Color SIDEBAR_ACTIVE = new Color(29, 78, 216);
    public static final Color SIDEBAR_TEXT = new Color(209, 213, 219);
    public static final Color BG = new Color(243, 244, 246);
    public static final Color CARD = Color.WHITE;
    public static final Color BORDER = new Color(229, 231, 235);
    public static final Color TEXT = new Color(17, 24, 39);
    public static final Color TEXT_MUTED = new Color(107, 114, 128);
    public static final Color SUCCESS = new Color(22, 163, 74);
    public static final Color WARNING = new Color(217, 119, 6);
    public static final Color DANGER = new Color(220, 38, 38);
    public static final Color INFO = new Color(8, 145, 178);

    public static final Color ACCENT_CAR = new Color(65, 145, 255);
    public static final Color ACCENT_MOTORCYCLE = new Color(255, 150, 40);
    public static final Color ACCENT_TRUCK = new Color(255, 70, 70);
    public static final Color ACCENT_BUS = new Color(139, 92, 246);
    public static final Color ACCENT_VAN = new Color(16, 185, 129);

    public static final Color[] CHART_SERIES = {PRIMARY, new Color(16, 185, 129), new Color(245, 158, 11), new Color(239, 68, 68),
            new Color(139, 92, 246), new Color(6, 182, 212), new Color(236, 72, 153), new Color(107, 114, 128), new Color(132, 204, 22), new Color(249, 115, 22)};

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_SUBHEADING = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_STAT = new Font("Segoe UI", Font.BOLD, 26);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), new EmptyBorder(12, 14, 12, 14));
    }

    public static Border padding(int all) { return new EmptyBorder(all, all, all, all); }

    public static Color statusColor(String status) {
        if (status == null) return TEXT_MUTED;
        switch (status.toUpperCase()) {
            case "AVAILABLE": case "ACTIVE": case "COMPLETED": case "RETURNED": case "VALID": return SUCCESS;
            case "ASSIGNED": case "IN_SERVICE": case "IN SERVICE": case "PLANNED": case "IN_PROGRESS": case "SCHEDULED": return INFO;
            case "MAINTENANCE": case "ON_LEAVE": case "DUE SOON": case "EXPIRING SOON": return WARNING;
            case "OUT_OF_SERVICE": case "OUT OF SERVICE": case "RETIRED": case "SUSPENDED": case "TERMINATED": case "INACTIVE":
            case "CANCELLED": case "OVERDUE": case "EXPIRED": return DANGER;
            default: return TEXT_MUTED;
        }
    }

    public static Color vehicleTypeColor(String type) {
        if (type == null) return TEXT_MUTED;
        switch (type.toUpperCase()) {
            case "CAR": return ACCENT_CAR;
            case "MOTORCYCLE": return ACCENT_MOTORCYCLE;
            case "TRUCK": return ACCENT_TRUCK;
            case "BUS": return ACCENT_BUS;
            case "VAN": return ACCENT_VAN;
            default: return TEXT_MUTED;
        }
    }

    /** Applies global UIManager defaults; called once at start-up. */
    public static void install() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) { UIManager.setLookAndFeel(info.getClassName()); break; }
            }
        } catch (Exception ignored) { /* fall back to default L&F */ }
        UIManager.put("control", BG);
        UIManager.put("nimbusBase", PRIMARY_DARK);
        UIManager.put("nimbusBlueGrey", new Color(203, 213, 225));
        UIManager.put("nimbusSelectionBackground", PRIMARY);
        UIManager.put("nimbusFocus", PRIMARY);
        UIManager.put("Table.alternateRowColor", new Color(249, 250, 251));
        UIManager.put("Table.showGrid", Boolean.FALSE);
        UIManager.put("defaultFont", FONT_BODY);
        UIManager.put("TextField.font", FONT_BODY);
        UIManager.put("Table.font", FONT_BODY);
        UIManager.put("TableHeader.font", FONT_SUBHEADING);
        UIManager.put("Button.font", FONT_BODY);
        UIManager.put("Label.font", FONT_BODY);
        UIManager.put("ComboBox.font", FONT_BODY);
        UIManager.put("TabbedPane.font", FONT_BODY);
        UIManager.put("OptionPane.messageFont", FONT_BODY);
        UIManager.put("OptionPane.buttonFont", FONT_BODY);
    }
}
