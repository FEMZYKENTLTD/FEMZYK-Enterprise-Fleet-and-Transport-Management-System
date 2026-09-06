package com.femzyk.fleetmanagement.repository;

import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.database.JdbcSupport;
import com.femzyk.fleetmanagement.model.Trip;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TripRepository extends JdbcSupport {

    private static final String BASE =
            "SELECT t.*, v.registration_number || ' (' || v.make || ' ' || v.model || ')' AS vehicle_display, "
            + "d.driver_code || ' - ' || e.first_name || ' ' || e.last_name AS driver_display "
            + "FROM trips t JOIN vehicles v ON v.id = t.vehicle_id JOIN drivers d ON d.id = t.driver_id JOIN employees e ON e.id = d.employee_id ";

    public TripRepository(DatabaseManager db) { super(db); }

    static Trip map(ResultSet rs) throws SQLException {
        Trip t = new Trip();
        t.setId(rs.getLong("id"));
        t.setTripCode(rs.getString("trip_code"));
        t.setVehicleId(rs.getLong("vehicle_id"));
        t.setDriverId(rs.getLong("driver_id"));
        t.setOrigin(rs.getString("origin"));
        t.setDestination(rs.getString("destination"));
        t.setPurpose(rs.getString("purpose"));
        t.setDepartureTime(dateTime(rs, "departure_time"));
        t.setReturnTime(dateTime(rs, "return_time"));
        t.setStartMileage(longOrNull(rs, "start_mileage"));
        t.setEndMileage(longOrNull(rs, "end_mileage"));
        t.setFuelUsedLitres(doubleOrNull(rs, "fuel_used_litres"));
        t.setStatus(Trip.Status.valueOf(rs.getString("status")));
        t.setNotes(rs.getString("notes"));
        t.setCreatedAt(dateTime(rs, "created_at"));
        t.setUpdatedAt(dateTime(rs, "updated_at"));
        t.setVehicleDisplay(rs.getString("vehicle_display"));
        t.setDriverDisplay(rs.getString("driver_display"));
        return t;
    }

    public List<Trip> findAll() {
        return query(BASE + "ORDER BY t.departure_time DESC", TripRepository::map);
    }

    public Optional<Trip> findById(long id) {
        return queryOne(BASE + "WHERE t.id = ?", TripRepository::map, id);
    }

    public List<Trip> search(String text, Trip.Status status, Long vehicleId, Long driverId, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder(BASE + "WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            String like = "%" + text.trim().toLowerCase() + "%";
            sql.append("AND (lower(t.trip_code) LIKE ? OR lower(t.destination) LIKE ? OR lower(coalesce(t.origin,'')) LIKE ? "
                    + "OR lower(coalesce(t.purpose,'')) LIKE ? OR lower(v.registration_number) LIKE ? OR lower(e.first_name || ' ' || e.last_name) LIKE ?) ");
            for (int i = 0; i < 6; i++) params.add(like);
        }
        if (status != null) { sql.append("AND t.status = ? "); params.add(status); }
        if (vehicleId != null) { sql.append("AND t.vehicle_id = ? "); params.add(vehicleId); }
        if (driverId != null) { sql.append("AND t.driver_id = ? "); params.add(driverId); }
        if (from != null) { sql.append("AND t.departure_time >= ? "); params.add(from.atStartOfDay()); }
        if (to != null) { sql.append("AND t.departure_time < ? "); params.add(to.plusDays(1).atStartOfDay()); }
        sql.append("ORDER BY t.departure_time DESC");
        return query(sql.toString(), TripRepository::map, params.toArray());
    }

    public List<Trip> findActiveForVehicle(long vehicleId) {
        return query(BASE + "WHERE t.vehicle_id = ? AND t.status = 'ACTIVE'", TripRepository::map, vehicleId);
    }

    public List<Trip> findActiveForDriver(long driverId) {
        return query(BASE + "WHERE t.driver_id = ? AND t.status = 'ACTIVE'", TripRepository::map, driverId);
    }

    public long count(Trip.Status status) {
        if (status == null) return queryLong("SELECT COUNT(*) FROM trips");
        return queryLong("SELECT COUNT(*) FROM trips WHERE status = ?", status);
    }

    public long totalDistanceKm(LocalDate from, LocalDate to) {
        return queryLong("SELECT COALESCE(SUM(end_mileage - start_mileage),0) FROM trips WHERE status = 'COMPLETED' "
                + "AND end_mileage IS NOT NULL AND start_mileage IS NOT NULL AND departure_time >= ? AND departure_time < ?",
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    public Trip save(Trip t) {
        String now = now();
        if (t.isNew()) {
            long id = insert("INSERT INTO trips(trip_code, vehicle_id, driver_id, origin, destination, purpose, departure_time, return_time, "
                    + "start_mileage, end_mileage, fuel_used_litres, status, notes, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    t.getTripCode(), t.getVehicleId(), t.getDriverId(), t.getOrigin(), t.getDestination(), t.getPurpose(), t.getDepartureTime(),
                    t.getReturnTime(), t.getStartMileage(), t.getEndMileage(), t.getFuelUsedLitres(), t.getStatus(), t.getNotes(), now, now);
            t.setId(id);
        } else {
            update("UPDATE trips SET trip_code=?, vehicle_id=?, driver_id=?, origin=?, destination=?, purpose=?, departure_time=?, return_time=?, "
                    + "start_mileage=?, end_mileage=?, fuel_used_litres=?, status=?, notes=?, updated_at=? WHERE id=?",
                    t.getTripCode(), t.getVehicleId(), t.getDriverId(), t.getOrigin(), t.getDestination(), t.getPurpose(), t.getDepartureTime(),
                    t.getReturnTime(), t.getStartMileage(), t.getEndMileage(), t.getFuelUsedLitres(), t.getStatus(), t.getNotes(), now, t.getId());
        }
        return t;
    }

    public void delete(long id) {
        update("DELETE FROM trips WHERE id = ?", id);
    }
}
