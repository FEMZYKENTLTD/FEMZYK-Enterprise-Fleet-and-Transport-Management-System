package com.femzyk.vehiclesystem.gui;

import java.awt.*;

/**
 * ThemeConstants - Centralized GUI Theme Registry
 *
 * PURPOSE:
 * All colors, fonts, dimensions, and spacing values used across the GUI
 * are defined here as constants. Centralizing theme values means changing
 * the look of the entire application requires editing only this one file.
 *
 * DESIGN PATTERN: This is an application of the "Single Source of Truth"
 * principle for UI styling - the GUI equivalent of interface constants in
 * the vehicle model layer.
 *
 * COLOR SCHEME: Professional dark theme with vehicle-type accent colors.
 * - Dark backgrounds reduce eye strain during extended use
 * - Color-coded vehicle types (Blue=Car, Orange=Motorcycle, Red=Truck)
 *   allow instant visual fleet composition assessment
 *
 * @author  Femzyk Enterprise Systems
 * @version 4.0 - Premium GUI Edition
 * @since   2024
 */
public final class ThemeConstants {

    // Prevent instantiation - this is a constants-only class
    private ThemeConstants() {}

    // =========================================================================
    // BACKGROUND COLORS
    // =========================================================================

    /** Main window background - deep navy dark */
    public static final Color BG_PRIMARY    = new Color(18, 22, 36);

    /** Panel background - slightly lighter than primary */
    public static final Color BG_SECONDARY  = new Color(26, 32, 50);

    /** Card and dialog backgrounds */
    public static final Color BG_CARD       = new Color(34, 42, 64);

    /** Input field backgrounds */
    public static final Color BG_INPUT      = new Color(44, 54, 80);

    /** Hover highlight color */
    public static final Color BG_HOVER      = new Color(54, 66, 98);

    // =========================================================================
    // TEXT COLORS
    // =========================================================================

    /** Primary text - near white for maximum contrast */
    public static final Color TEXT_PRIMARY   = new Color(230, 235, 245);

    /** Secondary text - muted for labels and descriptions */
    public static final Color TEXT_SECONDARY = new Color(140, 155, 185);

    /** Accent text - bright cyan for emphasis */
    public static final Color TEXT_ACCENT    = new Color(100, 210, 255);

    /** Success text - green for confirmations */
    public static final Color TEXT_SUCCESS   = new Color(80, 220, 140);

    /** Error text - red for error messages */
    public static final Color TEXT_ERROR     = new Color(255, 90, 90);

    // =========================================================================
    // VEHICLE TYPE ACCENT COLORS
    // =========================================================================

    /** Car accent - professional blue */
    public static final Color ACCENT_CAR        = new Color(65, 145, 255);

    /** Car accent dark - darker shade for gradients */
    public static final Color ACCENT_CAR_DARK   = new Color(30, 80, 180);

    /** Motorcycle accent - energetic orange */
    public static final Color ACCENT_MOTORCYCLE      = new Color(255, 150, 40);

    /** Motorcycle accent dark */
    public static final Color ACCENT_MOTORCYCLE_DARK = new Color(180, 90, 10);

    /** Truck accent - industrial red */
    public static final Color ACCENT_TRUCK      = new Color(255, 70, 70);

    /** Truck accent dark */
    public static final Color ACCENT_TRUCK_DARK = new Color(180, 30, 30);

    // =========================================================================
    // BUTTON COLORS
    // =========================================================================

    /** Primary action button background */
    public static final Color BTN_PRIMARY   = new Color(65, 145, 255);

    /** Primary button hover */
    public static final Color BTN_PRIMARY_HOVER = new Color(90, 170, 255);

    /** Danger button (delete) */
    public static final Color BTN_DANGER    = new Color(200, 50, 50);

    /** Success button (confirm) */
    public static final Color BTN_SUCCESS   = new Color(50, 180, 100);

    /** Neutral button */
    public static final Color BTN_NEUTRAL   = new Color(70, 80, 110);

    // =========================================================================
    // BORDER COLORS
    // =========================================================================

    /** Subtle border for panels */
    public static final Color BORDER_SUBTLE = new Color(50, 62, 90);

    /** Prominent border for cards and input controls */
    public static final Color BORDER_CARD   = new Color(65, 80, 120);

    // =========================================================================
    // FONTS
    // =========================================================================

    /** Main heading font */
    public static final Font FONT_HEADING    = new Font("Segoe UI", Font.BOLD, 22);

    /** Sub-heading font */
    public static final Font FONT_SUBHEADING = new Font("Segoe UI", Font.BOLD, 15);

    /** Body text font */
    public static final Font FONT_BODY       = new Font("Segoe UI", Font.PLAIN, 13);

    /** Small label font */
    public static final Font FONT_SMALL      = new Font("Segoe UI", Font.PLAIN, 11);

    /** Button font */
    public static final Font FONT_BUTTON     = new Font("Segoe UI", Font.BOLD, 13);

    /** Monospace font for stats */
    public static final Font FONT_MONO       = new Font("Consolas", Font.PLAIN, 12);

    // =========================================================================
    // DIMENSIONS
    // =========================================================================

    /** Standard button height */
    public static final int BTN_HEIGHT    = 38;

    /** Standard corner arc for rounded elements */
    public static final int CORNER_RADIUS = 12;

    /** Standard padding inside panels */
    public static final int PADDING       = 16;

    /** Vehicle card preferred width */
    public static final int CARD_WIDTH    = 320;

    /** Vehicle card preferred height */
    public static final int CARD_HEIGHT   = 180;
}