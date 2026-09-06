package com.femzyk.fleetmanagement.database;

import com.femzyk.fleetmanagement.exception.DataAccessException;
import com.femzyk.fleetmanagement.exception.DuplicateRecordException;
import com.femzyk.fleetmanagement.exception.BusinessRuleException;
import com.femzyk.fleetmanagement.util.DateUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Small JDBC helper used by all repositories: prepared statements only, parameter binding,
 * row mapping, and translation of SQLite constraint errors into meaningful application exceptions.
 */
public abstract class JdbcSupport {

    protected final DatabaseManager db;

    protected JdbcSupport(DatabaseManager db) {
        this.db = db;
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    protected <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        synchronized (db.getLock()) {
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    List<T> out = new ArrayList<>();
                    while (rs.next()) {
                        out.add(mapper.map(rs));
                    }
                    return out;
                }
            } catch (SQLException e) {
                throw translate(e, "Query failed");
            }
        }
    }

    protected <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = query(sql, mapper, params);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    protected long queryLong(String sql, Object... params) {
        return queryOne(sql, rs -> rs.getLong(1), params).orElse(0L);
    }

    protected double queryDouble(String sql, Object... params) {
        return queryOne(sql, rs -> rs.getDouble(1), params).orElse(0.0);
    }

    protected int update(String sql, Object... params) {
        synchronized (db.getLock()) {
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                bind(ps, params);
                return ps.executeUpdate();
            } catch (SQLException e) {
                throw translate(e, "Update failed");
            }
        }
    }

    protected long insert(String sql, Object... params) {
        synchronized (db.getLock()) {
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, params);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getLong(1);
                    }
                }
                throw new DataAccessException("Insert did not return a generated key", null);
            } catch (SQLException e) {
                throw translate(e, "Insert failed");
            }
        }
    }

    protected void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            int idx = i + 1;
            if (p == null) {
                ps.setObject(idx, null);
            } else if (p instanceof LocalDate) {
                ps.setString(idx, DateUtil.format((LocalDate) p));
            } else if (p instanceof LocalDateTime) {
                ps.setString(idx, DateUtil.format((LocalDateTime) p));
            } else if (p instanceof Enum) {
                ps.setString(idx, ((Enum<?>) p).name());
            } else if (p instanceof Boolean) {
                ps.setInt(idx, ((Boolean) p) ? 1 : 0);
            } else {
                ps.setObject(idx, p);
            }
        }
    }

    protected Connection connection() {
        return db.getConnection();
    }

    // ---- column readers -----------------------------------------------------------------------

    protected static LocalDate date(ResultSet rs, String column) throws SQLException {
        return DateUtil.parseDate(rs.getString(column));
    }

    protected static LocalDateTime dateTime(ResultSet rs, String column) throws SQLException {
        return DateUtil.parseDateTime(rs.getString(column));
    }

    protected static Long longOrNull(ResultSet rs, String column) throws SQLException {
        long v = rs.getLong(column);
        return rs.wasNull() ? null : v;
    }

    protected static Double doubleOrNull(ResultSet rs, String column) throws SQLException {
        double v = rs.getDouble(column);
        return rs.wasNull() ? null : v;
    }

    protected static <E extends Enum<E>> E enumOrNull(ResultSet rs, String column, Class<E> type) throws SQLException {
        String v = rs.getString(column);
        return v == null ? null : Enum.valueOf(type, v);
    }

    protected static boolean bool(ResultSet rs, String column) throws SQLException {
        return rs.getInt(column) == 1;
    }

    protected static String now() {
        return DateUtil.format(LocalDateTime.now());
    }

    // ---- error translation --------------------------------------------------------------------

    protected DataAccessException translate(SQLException e, String context) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (msg.contains("UNIQUE constraint failed")) {
            throw new DuplicateRecordException(describeUnique(msg));
        }
        if (msg.contains("FOREIGN KEY constraint failed")) {
            throw new BusinessRuleException("This record is referenced by other records and cannot be removed or re-linked.");
        }
        if (msg.contains("CHECK constraint failed")) {
            throw new BusinessRuleException("A database rule rejected the data: " + msg.replace("[SQLITE_CONSTRAINT_CHECK]", "").trim());
        }
        return new DataAccessException(context + ": " + msg, e);
    }

    private static String describeUnique(String msg) {
        // e.g. "[SQLITE_CONSTRAINT_UNIQUE] A UNIQUE constraint failed (UNIQUE constraint failed: vehicles.registration_number)"
        int idx = msg.lastIndexOf("UNIQUE constraint failed:");
        String col = idx >= 0 ? msg.substring(idx + "UNIQUE constraint failed:".length()).replace(")", "").trim() : "";
        if (col.contains("vehicle_assignments.vehicle_id")) return "This vehicle already has an active assignment.";
        if (col.contains("vehicle_assignments.driver_id")) return "This driver already has an active assignment.";
        if (col.contains("registration_number")) return "A vehicle with this registration number already exists.";
        if (col.contains("licence_number")) return "A driver with this licence number already exists.";
        if (col.contains("employee_code")) return "An employee with this employee code already exists.";
        if (col.contains("drivers.employee_id")) return "This employee already has a driver profile.";
        if (col.contains("users.username")) return "This username is already taken.";
        if (col.contains("users.email") || col.contains("employees.email")) return "This e-mail address is already registered.";
        if (col.contains("vin")) return "A vehicle with this VIN/chassis number already exists.";
        return "A record with the same unique value already exists (" + col + ").";
    }
}
