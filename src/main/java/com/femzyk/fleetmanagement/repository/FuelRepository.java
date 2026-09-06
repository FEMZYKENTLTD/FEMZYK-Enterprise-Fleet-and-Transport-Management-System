package com.femzyk.fleetmanagement.repository;

import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.database.JdbcSupport;
import com.femzyk.fleetmanagement.model.FuelRecord;
import com.femzyk.fleetmanagement.model.FuelType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FuelRepository extends JdbcSupport {

    private static final String BASE =
            "SELECT f.*, v.registration_number || ' (' || v.make || ' ' || v.model || ')' AS vehicle_display, "
            + "CASE WHEN d.id IS NULL THEN NULL ELSE d.driver_code || ' - ' || e.first_name || ' ' || e.last_name END AS driver_display "
            + "FROM fuel_records f JOIN vehicles v ON v.id = f.vehicle_id "
            + "LEFT JOIN drivers d ON d.id = f.driver_id LEFT JOIN employees e ON e.id = d.employee_id ";

    public FuelRepository(DatabaseManager db) { super(db); }

    static FuelRecord map(ResultSet rs) throws SQLException {
        FuelRecord f = new FuelRecord();
        f.setId(rs.getLong("id"));
        f.setVehicleId(rs.getLong("vehicle_id"));
        f.setDriverId(longOrNull(rs, "driver_id"));
        f.setDate(date(rs, "fuel_date"));
        f.setQuantityLitres(rs.getDouble("quantity_litres"));
        f.setUnitPrice(rs.getDouble("unit_price"));
        f.setTotalCost(rs.getDouble("total_cost"));
        f.setMileage(longOrNull(rs, "mileage"));
        f.setStation(rs.getString("station"));
        f.setReceiptReference(rs.getString("receipt_reference"));
        f.setFuelType(enumOrNull(rs, "fuel_type", FuelType.class));
        f.setNotes(rs.getString("notes"));
        f.setCreatedAt(dateTime(rs, "created_at"));
        f.setUpdatedAt(dateTime(rs, "updated_at"));
        f.setVehicleDisplay(rs.getString("vehicle_display"));
        f.setDriverDisplay(rs.getString("driver_display"));
        return f;
    }

    public List<FuelRecord> findAll() {
        return query(BASE + "ORDER BY f.fuel_date DESC, f.id DESC", FuelRepository::map);
    }

    public Optional<FuelRecord> findById(long id) {
        return queryOne(BASE + "WHERE f.id = ?", FuelRepository::map, id);
    }

    public List<FuelRecord> search(String text, Long vehicleId, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder(BASE + "WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            String like = "%" + text.trim().toLowerCase() + "%";
            sql.append("AND (lower(v.registration_number) LIKE ? OR lower(coalesce(f.station,'')) LIKE ? OR lower(coalesce(f.receipt_reference,'')) LIKE ?) ");
            for (int i = 0; i < 3; i++) params.add(like);
        }
        if (vehicleId != null) { sql.append("AND f.vehicle_id = ? "); params.add(vehicleId); }
        if (from != null) { sql.append("AND f.fuel_date >= ? "); params.add(from); }
        if (to != null) { sql.append("AND f.fuel_date <= ? "); params.add(to); }
        sql.append("ORDER BY f.fuel_date DESC, f.id DESC");
        return query(sql.toString(), FuelRepository::map, params.toArray());
    }

    /** Records for one vehicle ordered by date then mileage, used for efficiency analysis. */
    public List<FuelRecord> findForVehicleChronological(long vehicleId) {
        return query(BASE + "WHERE f.vehicle_id = ? ORDER BY f.fuel_date, coalesce(f.mileage, 0), f.id", FuelRepository::map, vehicleId);
    }

    public double totalCost(LocalDate from, LocalDate to) {
        return queryDouble("SELECT COALESCE(SUM(total_cost),0) FROM fuel_records WHERE fuel_date >= ? AND fuel_date <= ?", from, to);
    }

    public double totalLitres(LocalDate from, LocalDate to) {
        return queryDouble("SELECT COALESCE(SUM(quantity_litres),0) FROM fuel_records WHERE fuel_date >= ? AND fuel_date <= ?", from, to);
    }

    public Map<String, Double> costByVehicle(LocalDate from, LocalDate to) {
        Map<String, Double> out = new LinkedHashMap<>();
        query("SELECT v.registration_number, COALESCE(SUM(f.total_cost),0) FROM fuel_records f JOIN vehicles v ON v.id = f.vehicle_id "
                + "WHERE f.fuel_date >= ? AND f.fuel_date <= ? GROUP BY v.registration_number ORDER BY 2 DESC",
                rs -> out.put(rs.getString(1), rs.getDouble(2)), from, to);
        return out;
    }

    public Map<String, Double> costByMonth(LocalDate from, LocalDate to) {
        Map<String, Double> out = new LinkedHashMap<>();
        query("SELECT substr(fuel_date,1,7) AS month, COALESCE(SUM(total_cost),0) FROM fuel_records WHERE fuel_date >= ? AND fuel_date <= ? "
                + "GROUP BY month ORDER BY month", rs -> out.put(rs.getString(1), rs.getDouble(2)), from, to);
        return out;
    }

    public FuelRecord save(FuelRecord f) {
        String now = now();
        if (f.isNew()) {
            long id = insert("INSERT INTO fuel_records(vehicle_id, driver_id, fuel_date, quantity_litres, unit_price, total_cost, mileage, station, "
                    + "receipt_reference, fuel_type, notes, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    f.getVehicleId(), f.getDriverId(), f.getDate(), f.getQuantityLitres(), f.getUnitPrice(), f.getTotalCost(), f.getMileage(),
                    f.getStation(), f.getReceiptReference(), f.getFuelType(), f.getNotes(), now, now);
            f.setId(id);
        } else {
            update("UPDATE fuel_records SET vehicle_id=?, driver_id=?, fuel_date=?, quantity_litres=?, unit_price=?, total_cost=?, mileage=?, station=?, "
                    + "receipt_reference=?, fuel_type=?, notes=?, updated_at=? WHERE id=?",
                    f.getVehicleId(), f.getDriverId(), f.getDate(), f.getQuantityLitres(), f.getUnitPrice(), f.getTotalCost(), f.getMileage(),
                    f.getStation(), f.getReceiptReference(), f.getFuelType(), f.getNotes(), now, f.getId());
        }
        return f;
    }

    public void delete(long id) {
        update("DELETE FROM fuel_records WHERE id = ?", id);
    }
}
