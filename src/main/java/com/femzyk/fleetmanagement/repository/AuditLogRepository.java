package com.femzyk.fleetmanagement.repository;

import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.database.JdbcSupport;
import com.femzyk.fleetmanagement.model.AuditLog;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Append-only. There is intentionally no update or delete method. */
public class AuditLogRepository extends JdbcSupport {

    public AuditLogRepository(DatabaseManager db) { super(db); }

    static AuditLog map(ResultSet rs) throws SQLException {
        AuditLog a = new AuditLog();
        a.setId(rs.getLong("id"));
        a.setTimestamp(dateTime(rs, "timestamp"));
        a.setUsername(rs.getString("username"));
        a.setAction(rs.getString("action"));
        a.setModule(rs.getString("module"));
        a.setReference(rs.getString("reference"));
        a.setDescription(rs.getString("description"));
        return a;
    }

    public void append(AuditLog a) {
        insert("INSERT INTO audit_logs(timestamp, username, action, module, reference, description) VALUES (?,?,?,?,?,?)",
                a.getTimestamp(), a.getUsername(), a.getAction(), a.getModule(), a.getReference(), a.getDescription());
    }

    public List<AuditLog> findRecent(int limit) {
        return query("SELECT * FROM audit_logs ORDER BY id DESC LIMIT ?", AuditLogRepository::map, limit);
    }

    public List<AuditLog> search(String text, String module, String action, LocalDate from, LocalDate to, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM audit_logs WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            String like = "%" + text.trim().toLowerCase() + "%";
            sql.append("AND (lower(username) LIKE ? OR lower(coalesce(reference,'')) LIKE ? OR lower(coalesce(description,'')) LIKE ?) ");
            for (int i = 0; i < 3; i++) params.add(like);
        }
        if (module != null && !module.isBlank()) { sql.append("AND module = ? "); params.add(module); }
        if (action != null && !action.isBlank()) { sql.append("AND action = ? "); params.add(action); }
        if (from != null) { sql.append("AND timestamp >= ? "); params.add(from.atStartOfDay()); }
        if (to != null) { sql.append("AND timestamp < ? "); params.add(to.plusDays(1).atStartOfDay()); }
        sql.append("ORDER BY id DESC LIMIT ?");
        params.add(limit);
        return query(sql.toString(), AuditLogRepository::map, params.toArray());
    }

    public long count() {
        return queryLong("SELECT COUNT(*) FROM audit_logs");
    }

    public List<String> distinctModules() {
        return query("SELECT DISTINCT module FROM audit_logs ORDER BY module", rs -> rs.getString(1));
    }

    public List<String> distinctActions() {
        return query("SELECT DISTINCT action FROM audit_logs ORDER BY action", rs -> rs.getString(1));
    }
}
