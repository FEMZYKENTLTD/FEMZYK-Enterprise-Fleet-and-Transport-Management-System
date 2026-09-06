package com.femzyk.fleetmanagement.repository;

import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.database.JdbcSupport;
import com.femzyk.fleetmanagement.model.MaintenanceRecord;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MaintenanceRepository extends JdbcSupport {

    private static final String BASE =
            "SELECT m.*, v.registration_number || ' (' || v.make || ' ' || v.model || ')' AS vehicle_display, v.current_mileage AS vehicle_current_mileage "
            + "FROM maintenance_records m JOIN vehicles v ON v.id = m.vehicle_id ";

    public MaintenanceRepository(DatabaseManager db) { super(db); }

    static MaintenanceRecord map(ResultSet rs) throws SQLException {
        MaintenanceRecord m = new MaintenanceRecord();
        m.setId(rs.getLong("id"));
        m.setVehicleId(rs.getLong("vehicle_id"));
        m.setServiceDate(date(rs, "service_date"));
        m.setType(MaintenanceRecord.Type.valueOf(rs.getString("maintenance_type")));
        m.setDescription(rs.getString("description"));
        m.setProvider(rs.getString("provider"));
        m.setCost(rs.getDouble("cost"));
        m.setMileageAtService(longOrNull(rs, "mileage_at_service"));
        m.setNextServiceDate(date(rs, "next_service_date"));
        m.setNextServiceMileage(longOrNull(rs, "next_service_mileage"));
        m.setStatus(MaintenanceRecord.Status.valueOf(rs.getString("status")));
        m.setNotes(rs.getString("notes"));
        m.setCreatedAt(dateTime(rs, "created_at"));
        m.setUpdatedAt(dateTime(rs, "updated_at"));
        m.setVehicleDisplay(rs.getString("vehicle_display"));
        m.setVehicleCurrentMileage(longOrNull(rs, "vehicle_current_mileage"));
        return m;
    }

    public List<MaintenanceRecord> findAll() {
        return query(BASE + "ORDER BY m.service_date DESC, m.id DESC", MaintenanceRepository::map);
    }

    public Optional<MaintenanceRecord> findById(long id) {
        return queryOne(BASE + "WHERE m.id = ?", MaintenanceRepository::map, id);
    }

    public List<MaintenanceRecord> search(String text, MaintenanceRecord.Type type, MaintenanceRecord.Status status,
                                          Long vehicleId, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder(BASE + "WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            String like = "%" + text.trim().toLowerCase() + "%";
            sql.append("AND (lower(v.registration_number) LIKE ? OR lower(m.description) LIKE ? OR lower(coalesce(m.provider,'')) LIKE ?) ");
            for (int i = 0; i < 3; i++) params.add(like);
        }
        if (type != null) { sql.append("AND m.maintenance_type = ? "); params.add(type); }
        if (status != null) { sql.append("AND m.status = ? "); params.add(status); }
        if (vehicleId != null) { sql.append("AND m.vehicle_id = ? "); params.add(vehicleId); }
        if (from != null) { sql.append("AND m.service_date >= ? "); params.add(from); }
        if (to != null) { sql.append("AND m.service_date <= ? "); params.add(to); }
        sql.append("ORDER BY m.service_date DESC, m.id DESC");
        return query(sql.toString(), MaintenanceRepository::map, params.toArray());
    }

    /** Latest record per vehicle that has a next-service date or mileage (used for due/overdue calculation). */
    public List<MaintenanceRecord> findLatestWithNextService() {
        return query(BASE + "WHERE m.status <> 'CANCELLED' AND (m.next_service_date IS NOT NULL OR m.next_service_mileage IS NOT NULL) "
                + "AND v.deleted_at IS NULL AND v.status <> 'RETIRED' "
                + "AND m.id = (SELECT m2.id FROM maintenance_records m2 WHERE m2.vehicle_id = m.vehicle_id AND m2.status <> 'CANCELLED' "
                + "            ORDER BY m2.service_date DESC, m2.id DESC LIMIT 1) "
                + "ORDER BY m.next_service_date", MaintenanceRepository::map);
    }

    public double totalCost(LocalDate from, LocalDate to) {
        return queryDouble("SELECT COALESCE(SUM(cost),0) FROM maintenance_records WHERE status <> 'CANCELLED' AND service_date >= ? AND service_date <= ?", from, to);
    }

    public Map<String, Double> costByVehicle(LocalDate from, LocalDate to) {
        Map<String, Double> out = new LinkedHashMap<>();
        query("SELECT v.registration_number, COALESCE(SUM(m.cost),0) FROM maintenance_records m JOIN vehicles v ON v.id = m.vehicle_id "
                + "WHERE m.status <> 'CANCELLED' AND m.service_date >= ? AND m.service_date <= ? GROUP BY v.registration_number ORDER BY 2 DESC",
                rs -> out.put(rs.getString(1), rs.getDouble(2)), from, to);
        return out;
    }

    public MaintenanceRecord save(MaintenanceRecord m) {
        String now = now();
        if (m.isNew()) {
            long id = insert("INSERT INTO maintenance_records(vehicle_id, service_date, maintenance_type, description, provider, cost, mileage_at_service, "
                    + "next_service_date, next_service_mileage, status, notes, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    m.getVehicleId(), m.getServiceDate(), m.getType(), m.getDescription(), m.getProvider(), m.getCost(), m.getMileageAtService(),
                    m.getNextServiceDate(), m.getNextServiceMileage(), m.getStatus(), m.getNotes(), now, now);
            m.setId(id);
        } else {
            update("UPDATE maintenance_records SET vehicle_id=?, service_date=?, maintenance_type=?, description=?, provider=?, cost=?, mileage_at_service=?, "
                    + "next_service_date=?, next_service_mileage=?, status=?, notes=?, updated_at=? WHERE id=?",
                    m.getVehicleId(), m.getServiceDate(), m.getType(), m.getDescription(), m.getProvider(), m.getCost(), m.getMileageAtService(),
                    m.getNextServiceDate(), m.getNextServiceMileage(), m.getStatus(), m.getNotes(), now, m.getId());
        }
        return m;
    }

    public void delete(long id) {
        update("DELETE FROM maintenance_records WHERE id = ?", id);
    }
}
