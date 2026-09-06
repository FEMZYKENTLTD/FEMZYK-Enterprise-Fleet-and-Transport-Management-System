package com.femzyk.fleetmanagement.ui.components;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Generic read-only table model driven by a list of column definitions. */
public class EntityTableModel<T> extends AbstractTableModel {

    public record Column<T>(String header, Function<T, Object> value, Class<?> type, int width) {
        public static <T> Column<T> of(String header, Function<T, Object> value) { return new Column<>(header, value, Object.class, 0); }
        public static <T> Column<T> of(String header, Function<T, Object> value, int width) { return new Column<>(header, value, Object.class, width); }
        public static <T> Column<T> num(String header, Function<T, Object> value, int width) { return new Column<>(header, value, Number.class, width); }
    }

    private final List<Column<T>> columns;
    private List<T> rows = new ArrayList<>();

    public EntityTableModel(List<Column<T>> columns) { this.columns = columns; }

    public void setRows(List<T> rows) { this.rows = new ArrayList<>(rows); fireTableDataChanged(); }
    public List<T> getRows() { return rows; }
    public T getRow(int modelIndex) { return rows.get(modelIndex); }
    public List<Column<T>> getColumns() { return columns; }

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return columns.size(); }
    @Override public String getColumnName(int c) { return columns.get(c).header(); }
    @Override public Class<?> getColumnClass(int c) { return columns.get(c).type(); }
    @Override public boolean isCellEditable(int r, int c) { return false; }

    @Override
    public Object getValueAt(int r, int c) {
        try {
            Object v = columns.get(c).value().apply(rows.get(r));
            return v == null ? "" : v;
        } catch (RuntimeException e) {
            return "";
        }
    }
}
