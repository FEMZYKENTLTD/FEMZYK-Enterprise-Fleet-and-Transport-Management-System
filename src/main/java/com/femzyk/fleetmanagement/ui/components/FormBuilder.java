package com.femzyk.fleetmanagement.ui.components;

import com.femzyk.fleetmanagement.exception.ValidationException;
import com.femzyk.fleetmanagement.util.DateUtil;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Two-column form layout helper with typed accessors. Dates are entered as text (yyyy-MM-dd or dd/MM/yyyy);
 * parsing errors are collected into a ValidationException so the user sees all problems at once.
 */
public class FormBuilder {

    private final JPanel panel = new JPanel(new GridBagLayout());
    private final GridBagConstraints gc = new GridBagConstraints();
    private int row;
    private final List<String> parseErrors = new ArrayList<>();

    public FormBuilder() {
        gc.insets = new Insets(4, 6, 4, 6);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
    }

    public JPanel panel() { return panel; }

    public <C extends JComponent> C add(String label, C field) {
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0;
        JLabel l = new JLabel(label);
        panel.add(l, gc);
        gc.gridx = 1; gc.weightx = 1;
        panel.add(field, gc);
        row++;
        return field;
    }

    public void addFull(JComponent c) {
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2; gc.weightx = 1;
        panel.add(c, gc);
        gc.gridwidth = 1;
        row++;
    }

    public void section(String title) {
        JLabel l = new JLabel(title);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 13f));
        l.setForeground(new Color(29, 78, 216));
        l.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        addFull(l);
    }

    public JTextField text(String label, String value) { return text(label, value, 24); }
    public JTextField text(String label, String value, int columns) { JTextField f = new JTextField(value == null ? "" : value, columns); return add(label, f); }
    public JTextArea area(String label, String value) {
        JTextArea a = new JTextArea(value == null ? "" : value, 3, 24);
        a.setLineWrap(true); a.setWrapStyleWord(true);
        add(label, new JScrollPane(a));
        return a;
    }
    public JTextField number(String label, Number value) { return text(label, value == null ? "" : stripZero(value), 24); }
    public JTextField date(String label, LocalDate value) { JTextField f = text(label, value == null ? "" : DateUtil.format(value), 24); f.setToolTipText("yyyy-MM-dd or dd/MM/yyyy"); return f; }
    public JTextField dateTime(String label, LocalDateTime value) { JTextField f = text(label, value == null ? "" : DateUtil.format(value), 24); f.setToolTipText("yyyy-MM-dd HH:mm"); return f; }
    public <E> JComboBox<E> combo(String label, E[] values, E selected) { JComboBox<E> c = new JComboBox<>(values); if (selected != null) c.setSelectedItem(selected); return add(label, c); }
    public <E> JComboBox<E> combo(String label, List<E> values, E selected, boolean allowNone) {
        JComboBox<E> c = new JComboBox<>();
        if (allowNone) c.addItem(null);
        values.forEach(c::addItem);
        if (selected != null) c.setSelectedItem(selected);
        c.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, value == null ? "— none —" : value, index, isSelected, cellHasFocus);
            }
        });
        return add(label, c);
    }
    public JCheckBox check(String label, boolean value) { return add(label, new JCheckBox("", value)); }
    public JPasswordField password(String label) { return add(label, new JPasswordField(24)); }

    private static String stripZero(Number n) {
        if (n instanceof Double || n instanceof Float) { double d = n.doubleValue(); return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d); }
        return String.valueOf(n);
    }

    // ---- typed readers -------------------------------------------------------------------------------------

    public static String str(JTextField f) { String s = f.getText(); return s == null || s.isBlank() ? null : s.trim(); }
    public static String str(JTextArea f) { String s = f.getText(); return s == null || s.isBlank() ? null : s.trim(); }

    public Double dbl(JTextField f, String label) {
        String s = str(f);
        if (s == null) return null;
        try { return Double.parseDouble(s.replace(",", "").replace("\u20A6", "").trim()); } catch (NumberFormatException e) { parseErrors.add(label + " must be a number."); return null; }
    }
    public Long lng(JTextField f, String label) { Double d = dbl(f, label); return d == null ? null : d.longValue(); }
    public Integer integer(JTextField f, String label) { Double d = dbl(f, label); return d == null ? null : d.intValue(); }
    public double dblOrZero(JTextField f, String label) { Double d = dbl(f, label); return d == null ? 0 : d; }
    public long lngOrZero(JTextField f, String label) { Long l = lng(f, label); return l == null ? 0 : l; }
    public int intOrZero(JTextField f, String label) { Integer i = integer(f, label); return i == null ? 0 : i; }

    public LocalDate date(JTextField f, String label) {
        String s = str(f);
        if (s == null) return null;
        try { return DateUtil.parseUserDate(s); } catch (RuntimeException e) { parseErrors.add(label + " must be a valid date (yyyy-MM-dd)."); return null; }
    }
    public LocalDateTime dateTime(JTextField f, String label) {
        String s = str(f);
        if (s == null) return null;
        try { return DateUtil.parseUserDateTime(s); } catch (RuntimeException e) { parseErrors.add(label + " must be a valid date/time (yyyy-MM-dd HH:mm)."); return null; }
    }

    /** Throws if any typed reader failed; call after reading all fields. */
    public void throwIfParseErrors() {
        if (!parseErrors.isEmpty()) { List<String> copy = new ArrayList<>(parseErrors); parseErrors.clear(); throw new ValidationException(copy); }
    }
}
