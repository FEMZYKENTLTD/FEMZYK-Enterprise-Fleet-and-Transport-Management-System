package com.femzyk.fleetmanagement.repository;

import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.database.JdbcSupport;
import com.femzyk.fleetmanagement.model.Expense;
import com.femzyk.fleetmanagement.model.ExpenseCategory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExpenseRepository extends JdbcSupport {

    private static final String BASE =
            "SELECT x.*, CASE WHEN v.id IS NULL THEN NULL ELSE v.registration_number || ' (' || v.make || ' ' || v.model || ')' END AS vehicle_display "
            + "FROM expenses x LEFT JOIN vehicles v ON v.id = x.vehicle_id ";

    public ExpenseRepository(DatabaseManager db) { super(db); }

    static Expense map(ResultSet rs) throws SQLException {
        Expense x = new Expense();
        x.setId(rs.getLong("id"));
        x.setVehicleId(longOrNull(rs, "vehicle_id"));
        x.setDate(date(rs, "expense_date"));
        x.setCategory(ExpenseCategory.valueOf(rs.getString("category")));
        x.setAmount(rs.getDouble("amount"));
        x.setVendor(rs.getString("vendor"));
        x.setDescription(rs.getString("description"));
        x.setReference(rs.getString("reference"));
        x.setNotes(rs.getString("notes"));
        x.setSourceFuelRecordId(longOrNull(rs, "source_fuel_record_id"));
        x.setSourceMaintenanceRecordId(longOrNull(rs, "source_maintenance_record_id"));
        x.setCreatedAt(dateTime(rs, "created_at"));
        x.setUpdatedAt(dateTime(rs, "updated_at"));
        x.setVehicleDisplay(rs.getString("vehicle_display"));
        return x;
    }

    public List<Expense> findAll() {
        return query(BASE + "ORDER BY x.expense_date DESC, x.id DESC", ExpenseRepository::map);
    }

    public Optional<Expense> findById(long id) {
        return queryOne(BASE + "WHERE x.id = ?", ExpenseRepository::map, id);
    }

    public Optional<Expense> findByFuelRecord(long fuelRecordId) {
        return queryOne(BASE + "WHERE x.source_fuel_record_id = ?", ExpenseRepository::map, fuelRecordId);
    }

    public Optional<Expense> findByMaintenanceRecord(long maintenanceId) {
        return queryOne(BASE + "WHERE x.source_maintenance_record_id = ?", ExpenseRepository::map, maintenanceId);
    }

    public List<Expense> search(String text, ExpenseCategory category, Long vehicleId, LocalDate from, LocalDate to,
                                Double minAmount, Double maxAmount) {
        StringBuilder sql = new StringBuilder(BASE + "WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            String like = "%" + text.trim().toLowerCase() + "%";
            sql.append("AND (lower(x.description) LIKE ? OR lower(coalesce(x.vendor,'')) LIKE ? OR lower(coalesce(x.reference,'')) LIKE ? OR lower(coalesce(v.registration_number,'')) LIKE ?) ");
            for (int i = 0; i < 4; i++) params.add(like);
        }
        if (category != null) { sql.append("AND x.category = ? "); params.add(category); }
        if (vehicleId != null) { sql.append("AND x.vehicle_id = ? "); params.add(vehicleId); }
        if (from != null) { sql.append("AND x.expense_date >= ? "); params.add(from); }
        if (to != null) { sql.append("AND x.expense_date <= ? "); params.add(to); }
        if (minAmount != null) { sql.append("AND x.amount >= ? "); params.add(minAmount); }
        if (maxAmount != null) { sql.append("AND x.amount <= ? "); params.add(maxAmount); }
        sql.append("ORDER BY x.expense_date DESC, x.id DESC");
        return query(sql.toString(), ExpenseRepository::map, params.toArray());
    }

    public double total(LocalDate from, LocalDate to) {
        return queryDouble("SELECT COALESCE(SUM(amount),0) FROM expenses WHERE expense_date >= ? AND expense_date <= ?", from, to);
    }

    public Map<ExpenseCategory, Double> totalByCategory(LocalDate from, LocalDate to) {
        Map<ExpenseCategory, Double> out = new LinkedHashMap<>();
        query("SELECT category, COALESCE(SUM(amount),0) FROM expenses WHERE expense_date >= ? AND expense_date <= ? GROUP BY category ORDER BY 2 DESC",
                rs -> out.put(ExpenseCategory.valueOf(rs.getString(1)), rs.getDouble(2)), from, to);
        return out;
    }

    public Map<String, Double> totalByVehicle(LocalDate from, LocalDate to) {
        Map<String, Double> out = new LinkedHashMap<>();
        query("SELECT COALESCE(v.registration_number, '(general)'), COALESCE(SUM(x.amount),0) FROM expenses x LEFT JOIN vehicles v ON v.id = x.vehicle_id "
                + "WHERE x.expense_date >= ? AND x.expense_date <= ? GROUP BY 1 ORDER BY 2 DESC",
                rs -> out.put(rs.getString(1), rs.getDouble(2)), from, to);
        return out;
    }

    public Map<String, Double> totalByMonth(LocalDate from, LocalDate to) {
        Map<String, Double> out = new LinkedHashMap<>();
        query("SELECT substr(expense_date,1,7) AS month, COALESCE(SUM(amount),0) FROM expenses WHERE expense_date >= ? AND expense_date <= ? "
                + "GROUP BY month ORDER BY month", rs -> out.put(rs.getString(1), rs.getDouble(2)), from, to);
        return out;
    }

    public Map<String, Map<ExpenseCategory, Double>> totalByMonthAndCategory(LocalDate from, LocalDate to) {
        Map<String, Map<ExpenseCategory, Double>> out = new LinkedHashMap<>();
        query("SELECT substr(expense_date,1,7) AS month, category, COALESCE(SUM(amount),0) FROM expenses WHERE expense_date >= ? AND expense_date <= ? "
                + "GROUP BY month, category ORDER BY month, category",
                rs -> out.computeIfAbsent(rs.getString(1), k -> new LinkedHashMap<>()).put(ExpenseCategory.valueOf(rs.getString(2)), rs.getDouble(3)),
                from, to);
        return out;
    }

    public Expense save(Expense x) {
        String now = now();
        if (x.isNew()) {
            long id = insert("INSERT INTO expenses(vehicle_id, expense_date, category, amount, vendor, description, reference, notes, "
                    + "source_fuel_record_id, source_maintenance_record_id, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    x.getVehicleId(), x.getDate(), x.getCategory(), x.getAmount(), x.getVendor(), x.getDescription(), x.getReference(), x.getNotes(),
                    x.getSourceFuelRecordId(), x.getSourceMaintenanceRecordId(), now, now);
            x.setId(id);
        } else {
            update("UPDATE expenses SET vehicle_id=?, expense_date=?, category=?, amount=?, vendor=?, description=?, reference=?, notes=?, "
                    + "source_fuel_record_id=?, source_maintenance_record_id=?, updated_at=? WHERE id=?",
                    x.getVehicleId(), x.getDate(), x.getCategory(), x.getAmount(), x.getVendor(), x.getDescription(), x.getReference(), x.getNotes(),
                    x.getSourceFuelRecordId(), x.getSourceMaintenanceRecordId(), now, x.getId());
        }
        return x;
    }

    public void delete(long id) {
        update("DELETE FROM expenses WHERE id = ?", id);
    }
}
