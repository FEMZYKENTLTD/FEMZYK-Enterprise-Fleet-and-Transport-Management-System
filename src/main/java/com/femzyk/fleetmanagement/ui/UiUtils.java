package com.femzyk.fleetmanagement.ui;

import com.femzyk.fleetmanagement.exception.*;
import com.femzyk.fleetmanagement.util.AppLogger;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Callable;

/** Small Swing helpers: consistent error handling, buttons, labels. */
public final class UiUtils {

    private UiUtils() {}

    /** Runs an action and shows domain exceptions as friendly dialogs. Returns true when it succeeded. */
    public static boolean run(Component parent, Runnable action) {
        try {
            action.run();
            return true;
        } catch (RuntimeException e) {
            showError(parent, e);
            return false;
        }
    }

    public static <T> T call(Component parent, Callable<T> action) {
        try {
            return action.call();
        } catch (Exception e) {
            showError(parent, e);
            return null;
        }
    }

    public static void showError(Component parent, Throwable e) {
        String title;
        String message;
        int type = JOptionPane.WARNING_MESSAGE;
        if (e instanceof ValidationException v) {
            title = "Please check your input";
            message = "<html><b>The record could not be saved:</b><ul>" + String.join("", v.getErrors().stream().map(x -> "<li>" + escape(x) + "</li>").toList()) + "</ul></html>";
        } else if (e instanceof DuplicateRecordException) { title = "Duplicate record"; message = e.getMessage(); }
        else if (e instanceof InvalidStateTransitionException) { title = "Status change not allowed"; message = e.getMessage(); }
        else if (e instanceof BusinessRuleException) { title = "Business rule"; message = e.getMessage(); }
        else if (e instanceof RecordNotFoundException) { title = "Not found"; message = e.getMessage(); }
        else if (e instanceof AuthorizationException) { title = "Permission denied"; message = e.getMessage(); }
        else if (e instanceof AuthenticationException) { title = "Sign-in failed"; message = e.getMessage(); }
        else if (e instanceof DataAccessException) {
            title = "Database error"; message = e.getMessage(); type = JOptionPane.ERROR_MESSAGE;
            AppLogger.error("Data access error", e);
        } else {
            title = "Unexpected error"; message = e.getClass().getSimpleName() + ": " + e.getMessage(); type = JOptionPane.ERROR_MESSAGE;
            AppLogger.error("Unexpected UI error", e);
        }
        JOptionPane.showMessageDialog(parent, wrap(message), title, type);
    }

    private static Object wrap(String message) {
        if (message != null && message.startsWith("<html>")) return message;
        JTextArea area = new JTextArea(message == null ? "" : message);
        area.setEditable(false); area.setLineWrap(true); area.setWrapStyleWord(true); area.setOpaque(false); area.setFont(Theme.FONT_BODY);
        area.setColumns(45); area.setRows(Math.min(8, Math.max(1, (message == null ? 0 : message.length()) / 60 + 1)));
        return area;
    }

    public static void info(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, wrap(message), title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String title, String message) {
        return JOptionPane.showConfirmDialog(parent, wrap(message), title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public static String prompt(Component parent, String title, String message) {
        return JOptionPane.showInputDialog(parent, message, title, JOptionPane.QUESTION_MESSAGE);
    }

    public static JButton button(String text, Color bg, Runnable action) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFont(Theme.FONT_BODY);
        if (bg != null) { b.setBackground(bg); b.setForeground(Color.WHITE); b.setOpaque(true); b.setBorderPainted(false); b.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14)); }
        if (action != null) b.addActionListener(e -> action.run());
        return b;
    }

    public static JButton primary(String text, Runnable action) { return button(text, Theme.PRIMARY, action); }
    public static JButton danger(String text, Runnable action) { return button(text, Theme.DANGER, action); }
    public static JButton success(String text, Runnable action) { return button(text, Theme.SUCCESS, action); }
    public static JButton neutral(String text, Runnable action) { return button(text, null, action); }

    public static JLabel heading(String text) { JLabel l = new JLabel(text); l.setFont(Theme.FONT_HEADING); l.setForeground(Theme.TEXT); return l; }
    public static JLabel title(String text) { JLabel l = new JLabel(text); l.setFont(Theme.FONT_TITLE); l.setForeground(Theme.TEXT); return l; }
    public static JLabel muted(String text) { JLabel l = new JLabel(text); l.setFont(Theme.FONT_SMALL); l.setForeground(Theme.TEXT_MUTED); return l; }

    public static JPanel card() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.CARD);
        p.setBorder(Theme.cardBorder());
        return p;
    }

    public static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static Window windowOf(Component c) { return c == null ? null : SwingUtilities.getWindowAncestor(c); }
}
