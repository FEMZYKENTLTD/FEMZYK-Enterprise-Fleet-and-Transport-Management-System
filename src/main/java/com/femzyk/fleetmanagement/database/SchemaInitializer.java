package com.femzyk.fleetmanagement.database;

import com.femzyk.fleetmanagement.exception.DataAccessException;
import com.femzyk.fleetmanagement.util.AppLogger;
import com.femzyk.fleetmanagement.util.DateUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Creates the schema on first run and records the schema version. Idempotent: every statement in
 * {@code schema.sql} uses IF NOT EXISTS, so re-running on an existing database is harmless.
 */
public final class SchemaInitializer {

    public static final int CURRENT_VERSION = 1;
    private static final Logger LOG = AppLogger.get(SchemaInitializer.class);

    private final DatabaseManager db;

    public SchemaInitializer(DatabaseManager db) {
        this.db = db;
    }

    public void initialise() {
        Connection c = db.getConnection();
        try {
            for (String sql : loadStatements()) {
                try (Statement st = c.createStatement()) {
                    st.execute(sql);
                }
            }
            recordVersion(c);
        } catch (SQLException | IOException e) {
            throw new DataAccessException("Failed to initialise database schema", e);
        }
    }

    private void recordVersion(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM schema_version WHERE version = ?")) {
            ps.setInt(1, CURRENT_VERSION);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO schema_version(version, applied_at, description) VALUES (?, ?, ?)")) {
            ps.setInt(1, CURRENT_VERSION);
            ps.setString(2, DateUtil.format(LocalDateTime.now()));
            ps.setString(3, "Initial fleet management schema");
            ps.executeUpdate();
        }
        LOG.info("Schema version " + CURRENT_VERSION + " recorded");
    }

    static List<String> loadStatements() throws IOException {
        try (InputStream in = SchemaInitializer.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IOException("schema.sql not found on classpath");
            }
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            List<String> statements = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (String line : text.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                    continue;
                }
                int comment = line.indexOf("--");
                if (comment >= 0) {
                    line = line.substring(0, comment);
                }
                current.append(line).append('\n');
                if (line.trim().endsWith(";")) {
                    statements.add(current.toString().trim());
                    current.setLength(0);
                }
            }
            if (!current.toString().isBlank()) {
                statements.add(current.toString().trim());
            }
            return statements;
        }
    }
}
