package com.femzyk.fleetmanagement.repository;

import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.database.JdbcSupport;
import com.femzyk.fleetmanagement.model.Assignment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AssignmentRepository extends JdbcSupport {

    private static final String BASE =
            "SELECT a.*, v.registration_number || ' (' || v.make || ' ' || v.model || ')' AS vehicle_display, "
            + "d.driver_code || ' - ' || e.first_name || ' ' || e.last_name AS driver_display "
            + "FROM vehicle_assignments a JOIN vehicles v ON v.id = a.vehicle_id "
            + "JOIN drivers d ON d.id = a.driver_id JOIN employees e ON e.id = d.employee_id ";

    public AssignmentRepository(DatabaseManager db) { super(db); }

    static Assignment map(ResultSet rs) throws SQLException {
        Assignment a = new Assignment();
        a.setId(rs.getLong("id"));
        a.setVehicleId(rs.getLong("vehicle_id"));
        a.setDriverId(rs.getLong("driver_id"));
        a.setAssignedAt(dateTime(rs, "assigned_at"));
        a.setReturnedAt(dateTime(rs, "returned_at"));
        a.setStatus(Assignment.Status.valueOf(rs.getString("status")));
        a.setPurpose(rs.getString("purpose"));
        a.setNotes(rs.getString("notes"));
        a.setAssignedBy(rs.getString("assigned_by"));
        a.setCreatedAt(dateTime(rs, "created_at"));
        a.setUpdatedAt(dateTime(rs, "updated_at"));
        a.setVehicleDisplay(rs.getString("vehicle_display"));
        a.setDriverDisplay(rs.getString("driver_display"));
        return a;
    }

    public List<Assignment> findAll() {
        return query(BASE + "ORDER BY a.assigned_at DESC", AssignmentRepository::map);
    }

    public List<Assignment> search(String text, Assignment.Status status) {
        StringBuilder sql = new StringBuilder(BASE + "WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            String like = "%" + text.trim().toLowerCase() + "%";
            sql.append("AND (lower(v.registration_number) LIKE ? OR lower(e.first_name || ' ' || e.last_name) LIKE ? OR lower(coalesce(a.purpose,'')) LIKE ?) ");
            for (int i = 0; i < 3; i++) params.add(like);
        }
        if (status != null) { sql.append("AND a.status = ? "); params.add(status); }
        sql.append("ORDER BY a.assigned_at DESC");
        return query(sql.toString(), AssignmentRepository::map, params.toArray());
    }

    public Optional<Assignment> findById(long id) {
        return queryOne(BASE + "WHERE a.id = ?", AssignmentRepository::map, id);
    }

    public Optional<Assignment> findActiveByVehicle(long vehicleId) {
        return queryOne(BASE + "WHERE a.vehicle_id = ? AND a.status = 'ACTIVE'", AssignmentRepository::map, vehicleId);
    }

    public Optional<Assignment> findActiveByDriver(long driverId) {
        return queryOne(BASE + "WHERE a.driver_id = ? AND a.status = 'ACTIVE'", AssignmentRepository::map, driverId);
    }

    public List<Assignment> historyForVehicle(long vehicleId) {
        return query(BASE + "WHERE a.vehicle_id = ? ORDER BY a.assigned_at DESC", AssignmentRepository::map, vehicleId);
    }

    public List<Assignment> historyForDriver(long driverId) {
        return query(BASE + "WHERE a.driver_id = ? ORDER BY a.assigned_at DESC", AssignmentRepository::map, driverId);
    }

    public long countActive() {
        return queryLong("SELECT COUNT(*) FROM vehicle_assignments WHERE status = 'ACTIVE'");
    }

    public Assignment save(Assignment a) {
        String now = now();
        if (a.isNew()) {
            long id = insert("INSERT INTO vehicle_assignments(vehicle_id, driver_id, assigned_at, returned_at, status, purpose, notes, assigned_by, created_at, updated_at) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?)",
                    a.getVehicleId(), a.getDriverId(), a.getAssignedAt(), a.getReturnedAt(), a.getStatus(), a.getPurpose(), a.getNotes(), a.getAssignedBy(), now, now);
            a.setId(id);
        } else {
            update("UPDATE vehicle_assignments SET vehicle_id=?, driver_id=?, assigned_at=?, returned_at=?, status=?, purpose=?, notes=?, assigned_by=?, updated_at=? WHERE id=?",
                    a.getVehicleId(), a.getDriverId(), a.getAssignedAt(), a.getReturnedAt(), a.getStatus(), a.getPurpose(), a.getNotes(), a.getAssignedBy(), now, a.getId());
        }
        return a;
    }
}
