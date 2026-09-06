package com.femzyk.fleetmanagement.ui.components;

import com.femzyk.fleetmanagement.ui.Theme;
import com.femzyk.fleetmanagement.ui.UiUtils;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Reusable list screen: title, search box, filter controls, action toolbar, sortable table, record count.
 * Modules supply the column definitions, a loader (which applies the current search/filter) and actions.
 * Pattern adapted from the CourseManagementSystem JTable panels and the EMS filter bar.
 */
public class CrudTablePanel<T> extends JPanel {

    private final EntityTableModel<T> model;
    private final JTable table;
    private final JTextField search = new JTextField(22);
    private final JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    private final JLabel count = UiUtils.muted("");
    private Supplier<List<T>> loader = List::of;
    private Consumer<T> onDoubleClick;

    public CrudTablePanel(String title, String subtitle, List<EntityTableModel.Column<T>> columns) {
        super(new BorderLayout(0, 10));
        setBackground(Theme.BG);
        setBorder(Theme.padding(16));
        model = new EntityTableModel<>(columns);
        table = new JTable(model);
        table.setRowHeight(26);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).width() > 0) table.getColumnModel().getColumn(i).setPreferredWidth(columns.get(i).width());
        }
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && onDoubleClick != null) { T sel = selected(); if (sel != null) onDoubleClick.accept(sel); }
            }
        });

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(UiUtils.title(title));
        titles.add(UiUtils.muted(subtitle));
        header.add(titles, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        actions.setOpaque(false);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(new JLabel("Search:"));
        search.putClientProperty("JTextField.placeholderText", "Type to filter...");
        search.addKeyListener(new KeyAdapter() { @Override public void keyReleased(KeyEvent e) { refresh(); } });
        left.add(search);
        filters.setOpaque(false);
        left.add(filters);
        bar.add(left, BorderLayout.WEST);
        bar.add(count, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        top.add(bar, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);
    }

    public CrudTablePanel<T> loader(Supplier<List<T>> loader) { this.loader = loader; return this; }
    public CrudTablePanel<T> onDoubleClick(Consumer<T> handler) { this.onDoubleClick = handler; return this; }

    public JButton addAction(JButton button) { actions.add(button); return button; }

    public <C extends JComponent> C addFilter(String label, C component) {
        if (label != null) filters.add(new JLabel(label));
        filters.add(component);
        if (component instanceof JComboBox<?> combo) combo.addActionListener(e -> refresh());
        return component;
    }

    public String searchText() { String s = search.getText(); return s == null || s.isBlank() ? null : s.trim(); }

    public JTable table() { return table; }
    public EntityTableModel<T> model() { return model; }

    public void setRenderer(int column, javax.swing.table.TableCellRenderer renderer) { table.getColumnModel().getColumn(column).setCellRenderer(renderer); }

    public T selected() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return model.getRow(table.convertRowIndexToModel(row));
    }

    public T requireSelected(String what) {
        T sel = selected();
        if (sel == null) UiUtils.info(this, "No selection", "Please select a " + what + " in the table first.");
        return sel;
    }

    public void refresh() {
        T previous = selected();
        List<T> rows = UiUtils.call(this, loader::get);
        if (rows == null) rows = List.of();
        model.setRows(rows);
        count.setText(rows.size() + " record" + (rows.size() == 1 ? "" : "s"));
        if (previous != null) {
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).equals(previous)) { int v = table.convertRowIndexToView(i); table.setRowSelectionInterval(v, v); break; }
            }
        }
        @SuppressWarnings("unchecked") TableRowSorter<EntityTableModel<T>> sorter = (TableRowSorter<EntityTableModel<T>>) table.getRowSorter();
        if (sorter != null) sorter.allRowsChanged();
    }
}
