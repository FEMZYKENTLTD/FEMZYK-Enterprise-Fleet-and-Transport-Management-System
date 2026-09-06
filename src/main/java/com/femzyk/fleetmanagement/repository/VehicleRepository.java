package com.femzyk.fleetmanagement.repository;

import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.database.JdbcSupport;
import com.femzyk.fleetmanagement.model.FuelType;
import com.femzyk.fleetmanagement.model.Vehicle;
import com.femzyk.fleetmanagement.model.VehicleStatus;
import com.femzyk.fleetmanagement.model.VehicleType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class VehicleRepository extends JdbcSupport {

    private static final String BASE =
            "SELECT v.*, a.driver_id AS assigned_driver_id, e.first_name || ' ' || e.last_name AS assigned_driver_name "
            + "FROM vehicles v "
            + "LEFT JOIN vehicle_assignments a ON a.vehicle_id = v.id AND a.status = 'ACTIVE' "
            + "LEFT JOIN drivers d ON d.id = a.driver_id "
            + "LEFT JOIN employees e ON e.id = d.employee_id ";

    public VehicleRepository(DatabaseManager db) { super(db); }

    static Vehicle map(ResultSet rs) throws SQLException {
        Vehicle v = Vehicle.newOfType(VehicleType.valueOf(rs.getString("vehicle_type")));
        v.setId(rs.getLong("id"));
        v.setVehicleCode(rs.getString("vehicle_code"));
        v.setRegistrationNumber(rs.getString("registration_number"));
        v.setMake(rs.getString("make"));
        v.setModel(rs.getString("model"));
        v.setYear(rs.getInt("year"));
        v.setColor(rs.getString("color"));
        v.setVin(rs.getString("vin"));
        v.setEngineNumber(rs.getString("engine_number"));
        v.setAcquisitionDate(date(rs, "acquisition_date"));
        v.setAcquisitionCost(rs.getDouble("acquisition_cost"));
        v.setCurrentMileage(rs.getLong("current_mileage"));
        v.setFuelType(FuelType.valueOf(rs.getString("fuel_type")));
        v.setCapacity(rs.getInt("capacity"));
        v.setStatus(VehicleStatus.valueOf(rs.getString("status")));
        v.setAttributeNumber(doubleOrNull(rs, "attribute_number"));
        v.setAttributeText(rs.getString("attribute_text"));
        v.setNotes(rs.getString("notes"));
        v.setCreatedAt(dateTime(rs, "created_at"));
        v.setUpdatedAt(dateTime(rs, "updated_at"));
        v.setDeletedAt(dateTime(rs, "deleted_at"));
        v.setAssignedDriverId(longOrNull(rs, "assigned_driver_id"));
        v.setAssignedDriverName(rs.getString("assigned_driver_name"));
        return v;
    }

    public List<Vehicle> findAll() {
        return query(BASE + "WHERE v.deleted_at IS NULL ORDER BY v.registration_number", VehicleRepository::map);
    }

    public List<Vehicle> findDeleted() {
        return query(BASE + "WHERE v.deleted_at IS NOT NULL ORDER BY v.deleted_at DESC", VehicleRepository::map);
    }

    public Optional<Vehicle> findById(long id) {
        return queryOne(BASE + "WHERE v.id = ?", VehicleRepository::map, id);
    }

    public Optional<Vehicle> findByRegistration(String registration) {
        return queryOne(BASE + "WHERE v.registration_number = ? COLLATE NOCASE", VehicleRepository::map, registration);
    }

    public List<Vehicle> search(String text, VehicleType type, VehicleStatus status, FuelType fuelType) {
        StringBuilder sql = new StringBuilder(BASE + "WHERE v.deleted_at IS NULL ");
        List<Object> params = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            String like = "%" + text.trim().toLowerCase() + "%";
            sql.append("AND (lower(v.registration_number) LIKE ? OR lower(v.vehicle_code) LIKE ? OR lower(v.make) LIKE ? "
                    + "OR lower(v.model) LIKE ? OR lower(coalesce(v.vin,'')) LIKE ? OR lower(coalesce(v.color,'')) LIKE ?) ");
            for (int i = 0; i < 6; i++) params.add(like);
        }
        if (type != null) { sql.append("AND v.vehicle_type = ? "); params.add(type); }
        if (status != null) { sql.append("AND v.status = ? "); params.add(status); }
        if (fuelType != null) { sql.append("AND v.fuel_type = ? "); params.add(fuelType); }
        sql.append("ORDER BY v.registration_number");
        return query(sql.toString(), VehicleRepository::map, params.toArray());
    }

    public long count(VehicleStatus status) {
        if (status == null) return queryLong("SELECT COUNT(*) FROM vehicles WHERE deleted_at IS NULL");
        return queryLong("SELECT COUNT(*) FROM vehicles WHERE deleted_at IS NULL AND status = ?", status);
    }

    public Map<VehicleStatus, Long> countByStatus() {
        Map<VehicleStatus, Long> out = new LinkedHashMap<>();
        for (VehicleStatus s : VehicleStatus.values()) out.put(s, 0L);
        query("SELECT status, COUNT(*) FROM vehicles WHERE deleted_at IS NULL GROUP BY status",
                rs -> out.put(VehicleStatus.valueOf(rs.getString(1)), rs.getLong(2)));
        return out;
    }

    public Map<VehicleType, Long> countByType() {
        Map<VehicleType, Long> out = new LinkedHashMap<>();
        query("SELECT vehicle_type, COUNT(*) FROM vehicles WHERE deleted_at IS NULL GROUP BY vehicle_type ORDER BY vehicle_type",
                rs -> out.put(VehicleType.valueOf(rs.getString(1)), rs.getLong(2)));
        return out;
    }

    public Vehicle save(Vehicle v) {
        String now = now();
        if (v.isNew()) {
            long id = insert("INSERT INTO vehicles(vehicle_code, registration_number, vehicle_type, make, model, year, color, vin, engine_number, "
                    + "acquisition_date, acquisition_cost, current_mileage, fuel_type, capacity, status, attribute_number, attribute_text, notes, created_at, updated_at) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    v.getVehicleCode(), v.getRegistrationNumber(), v.getVehicleType(), v.getMake(), v.getModel(), v.getYear(), v.getColor(),
                    EmployeeRepository.blankToNull(v.getVin()), v.getEngineNumber(), v.getAcquisitionDate(), v.getAcquisitionCost(),
                    v.getCurrentMileage(), v.getFuelType(), v.getCapacity(), v.getStatus(), v.getAttributeNumber(), v.getAttributeText(),
                    v.getNotes(), now, now);
            v.setId(id);
        } else {
            update("UPDATE vehicles SET vehicle_code=?, registration_number=?, vehicle_type=?, make=?, model=?, year=?, color=?, vin=?, engine_number=?, "
                    + "acquisition_date=?, acquisition_cost=?, current_mileage=?, fuel_type=?, capacity=?, status=?, attribute_number=?, attribute_text=?, notes=?, updated_at=? "
                    + "WHERE id=?",
                    v.getVehicleCode(), v.getRegistrationNumber(), v.getVehicleType(), v.getMake(), v.getModel(), v.getYear(), v.getColor(),
                    EmployeeRepository.blankToNull(v.getVin()), v.getEngineNumber(), v.getAcquisitionDate(), v.getAcquisitionCost(),
                    v.getCurrentMileage(), v.getFuelType(), v.getCapacity(), v.getStatus(), v.getAttributeNumber(), v.getAttributeText(),
                    v.getNotes(), now, v.getId());
        }
        return v;
    }

    public void updateStatus(long id, VehicleStatus status) {
        update("UPDATE vehicles SET status = ?, updated_at = ? WHERE id = ?", status, now(), id);
    }

    public void updateMileageIfGreater(long id, long mileage) {
        update("UPDATE vehicles SET current_mileage = ?, updated_at = ? WHERE id = ? AND current_mileage < ?", mileage, now(), id, mileage);
    }

    public void softDelete(long id) {
        update("UPDATE vehicles SET deleted_at = ?, updated_at = ? WHERE id = ?", now(), now(), id);
    }

    public void restore(long id) {
        update("UPDATE vehicles SET deleted_at = NULL, updated_at = ? WHERE id = ?", now(), id);
    }

    public void hardDelete(long id) {
        update("DELETE FROM vehicles WHERE id = ?", id);
    }
}
