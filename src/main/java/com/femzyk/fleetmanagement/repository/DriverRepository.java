package com.femzyk.fleetmanagement.repository;

import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.database.JdbcSupport;
import com.femzyk.fleetmanagement.model.Driver;
import com.femzyk.fleetmanagement.model.DriverStatus;
import com.femzyk.fleetmanagement.model.LicenceCategory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DriverRepository extends JdbcSupport {

    private static final String BASE =
            "SELECT d.*, e.first_name || ' ' || e.last_name AS full_name, e.phone, e.address, "
            + "e.emergency_contact_name, e.emergency_contact_phone, "
            + "(SELECT v.registration_number FROM vehicle_assignments a JOIN vehicles v ON v.id = a.vehicle_id "
            + " WHERE a.driver_id = d.id AND a.status = 'ACTIVE' LIMIT 1) AS assigned_vehicle "
            + "FROM drivers d JOIN employees e ON e.id = d.employee_id ";

    public DriverRepository(DatabaseManager db) { super(db); }

    static Driver map(ResultSet rs) throws SQLException {
        Driver d = new Driver();
        d.setId(rs.getLong("id"));
        d.setDriverCode(rs.getString("driver_code"));
        d.setEmployeeId(rs.getLong("employee_id"));
        d.setLicenceNumber(rs.getString("licence_number"));
        d.setLicenceCategory(LicenceCategory.valueOf(rs.getString("licence_category")));
        d.setLicenceIssueDate(date(rs, "licence_issue_date"));
        d.setLicenceExpiryDate(date(rs, "licence_expiry_date"));
        d.setStatus(DriverStatus.valueOf(rs.getString("status")));
        d.setNotes(rs.getString("notes"));
        d.setCreatedAt(dateTime(rs, "created_at"));
        d.setUpdatedAt(dateTime(rs, "updated_at"));
        d.setDeletedAt(dateTime(rs, "deleted_at"));
        d.setFullName(rs.getString("full_name"));
        d.setPhone(rs.getString("phone"));
        d.setAddress(rs.getString("address"));
        d.setEmergencyContactName(rs.getString("emergency_contact_name"));
        d.setEmergencyContactPhone(rs.getString("emergency_contact_phone"));
        d.setAssignedVehicle(rs.getString("assigned_vehicle"));
        return d;
    }

    public List<Driver> findAll() {
        return query(BASE + "WHERE d.deleted_at IS NULL ORDER BY e.last_name, e.first_name", DriverRepository::map);
    }

    public List<Driver> findDeleted() {
        return query(BASE + "WHERE d.deleted_at IS NOT NULL ORDER BY d.deleted_at DESC", DriverRepository::map);
    }

    public Optional<Driver> findById(long id) {
        return queryOne(BASE + "WHERE d.id = ?", DriverRepository::map, id);
    }

    public Optional<Driver> findByLicence(String licence) {
        return queryOne(BASE + "WHERE d.licence_number = ? COLLATE NOCASE", DriverRepository::map, licence);
    }

    public Optional<Driver> findByEmployeeId(long employeeId) {
        return queryOne(BASE + "WHERE d.employee_id = ?", DriverRepository::map, employeeId);
    }

    public List<Driver> search(String text, DriverStatus status) {
        StringBuilder sql = new StringBuilder(BASE + "WHERE d.deleted_at IS NULL ");
        List<Object> params = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            String like = "%" + text.trim().toLowerCase() + "%";
            sql.append("AND (lower(d.driver_code) LIKE ? OR lower(e.first_name || ' ' || e.last_name) LIKE ? OR lower(d.licence_number) LIKE ? OR lower(coalesce(e.phone,'')) LIKE ?) ");
            for (int i = 0; i < 4; i++) params.add(like);
        }
        if (status != null) {
            sql.append("AND d.status = ? ");
            params.add(status);
        }
        sql.append("ORDER BY e.last_name, e.first_name");
        return query(sql.toString(), DriverRepository::map, params.toArray());
    }

    public List<Driver> findLicenceExpiringBefore(LocalDate date) {
        return query(BASE + "WHERE d.deleted_at IS NULL AND d.status <> 'INACTIVE' AND d.licence_expiry_date <= ? ORDER BY d.licence_expiry_date",
                DriverRepository::map, date);
    }

    public long count(DriverStatus status) {
        if (status == null) return queryLong("SELECT COUNT(*) FROM drivers WHERE deleted_at IS NULL");
        return queryLong("SELECT COUNT(*) FROM drivers WHERE deleted_at IS NULL AND status = ?", status);
    }

    public Driver save(Driver d) {
        String now = now();
        if (d.isNew()) {
            long id = insert("INSERT INTO drivers(driver_code, employee_id, licence_number, licence_category, licence_issue_date, "
                    + "licence_expiry_date, status, notes, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
                    d.getDriverCode(), d.getEmployeeId(), d.getLicenceNumber(), d.getLicenceCategory(), d.getLicenceIssueDate(),
                    d.getLicenceExpiryDate(), d.getStatus(), d.getNotes(), now, now);
            d.setId(id);
        } else {
            update("UPDATE drivers SET driver_code=?, employee_id=?, licence_number=?, licence_category=?, licence_issue_date=?, "
                    + "licence_expiry_date=?, status=?, notes=?, updated_at=? WHERE id=?",
                    d.getDriverCode(), d.getEmployeeId(), d.getLicenceNumber(), d.getLicenceCategory(), d.getLicenceIssueDate(),
                    d.getLicenceExpiryDate(), d.getStatus(), d.getNotes(), now, d.getId());
        }
        return d;
    }

    public void softDelete(long id) {
        update("UPDATE drivers SET deleted_at = ?, status = 'INACTIVE', updated_at = ? WHERE id = ?", now(), now(), id);
    }

    public void restore(long id) {
        update("UPDATE drivers SET deleted_at = NULL, updated_at = ? WHERE id = ?", now(), id);
    }
}
