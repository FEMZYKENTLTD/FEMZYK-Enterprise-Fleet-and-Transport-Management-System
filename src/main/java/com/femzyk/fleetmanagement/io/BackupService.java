package com.femzyk.fleetmanagement.io;

import com.femzyk.fleetmanagement.audit.AuditService;
import com.femzyk.fleetmanagement.config.AppConfig;
import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.exception.BusinessRuleException;
import com.femzyk.fleetmanagement.exception.DataAccessException;
import com.femzyk.fleetmanagement.model.Permission;
import com.femzyk.fleetmanagement.security.SessionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Local backup and restore.
 *
 * <p>What it does: checkpoints the WAL, copies the SQLite file to the backups folder with a timestamp,
 * then validates the copy by opening it and running {@code PRAGMA integrity_check} plus a table count.
 * Restore validates the chosen file the same way, keeps a safety copy of the current database, replaces
 * it and reopens the connection. It is a verified file-level backup — not incremental or off-site.</p>
 */
public class BackupService {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final DatabaseManager db;
    private final AuditService audit;

    public BackupService(DatabaseManager db, AuditService audit) {
        this.db = db;
        this.audit = audit;
    }

    public record BackupInfo(Path file, long sizeBytes, LocalDateTime createdAt) {}

    public Path createBackup() {
        SessionContext.require(Permission.MANAGE_BACKUPS);
        return createBackup(AppConfig.getBackupDirectory());
    }

    public Path createBackup(Path directory) {
        if (db.getDatabasePath() == null) throw new BusinessRuleException("In-memory databases cannot be backed up.");
        try {
            Files.createDirectories(directory);
            Path target = directory.resolve("fleet-backup-" + STAMP.format(LocalDateTime.now()) + ".db");
            synchronized (db.getLock()) {
                db.checkpoint();
                try (Statement st = db.getConnection().createStatement()) {
                    st.executeUpdate("VACUUM INTO '" + target.toAbsolutePath().toString().replace("'", "''") + "'");
                }
            }
            ValidationResult v = validate(target);
            if (!v.valid()) {
                Files.deleteIfExists(target);
                throw new DataAccessException("Backup verification failed: " + v.message(), null);
            }
            audit.record("BACKUP", "SYSTEM", target.getFileName().toString(), "Database backup created and verified (" + v.message() + ")");
            return target;
        } catch (IOException | SQLException e) {
            throw new DataAccessException("Backup failed: " + e.getMessage(), e);
        }
    }

    public record ValidationResult(boolean valid, String message, int tableCount, long vehicleCount, long employeeCount) {}

    /** Opens the file read-only and checks integrity + expected tables. */
    public ValidationResult validate(Path file) {
        if (file == null || !Files.isRegularFile(file)) return new ValidationResult(false, "File does not exist", 0, 0, 0);
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath()); Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("PRAGMA integrity_check")) {
                if (!rs.next() || !"ok".equalsIgnoreCase(rs.getString(1))) return new ValidationResult(false, "Integrity check failed", 0, 0, 0);
            }
            int tables;
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name IN "
                    + "('users','employees','drivers','vehicles','vehicle_assignments','trips','maintenance_records','fuel_records','expenses','audit_logs')")) {
                rs.next();
                tables = rs.getInt(1);
            }
            if (tables < 10) return new ValidationResult(false, "Not a fleet database (found " + tables + " of 10 expected tables)", tables, 0, 0);
            long vehicles, employees;
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM vehicles")) { rs.next(); vehicles = rs.getLong(1); }
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM employees")) { rs.next(); employees = rs.getLong(1); }
            return new ValidationResult(true, tables + " tables, " + vehicles + " vehicles, " + employees + " employees", tables, vehicles, employees);
        } catch (SQLException e) {
            return new ValidationResult(false, "Cannot open as SQLite: " + e.getMessage(), 0, 0, 0);
        }
    }

    /** Replaces the live database with the backup. The caller must rebuild services afterwards. */
    public void restore(Path backupFile) {
        SessionContext.require(Permission.MANAGE_BACKUPS);
        ValidationResult v = validate(backupFile);
        if (!v.valid()) throw new BusinessRuleException("Backup file is not valid: " + v.message());
        Path live = db.getDatabasePath();
        if (live == null) throw new BusinessRuleException("In-memory databases cannot be restored.");
        try {
            audit.record("RESTORE", "SYSTEM", backupFile.getFileName().toString(), "Restore requested (" + v.message() + ")");
            synchronized (db.getLock()) {
                db.checkpoint();
                db.close();
                Path safety = live.resolveSibling(live.getFileName() + ".pre-restore-" + STAMP.format(LocalDateTime.now()));
                Files.copy(live, safety, StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(live.resolveSibling(live.getFileName() + "-wal"));
                Files.deleteIfExists(live.resolveSibling(live.getFileName() + "-shm"));
                Files.copy(backupFile, live, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new DataAccessException("Restore failed: " + e.getMessage(), e);
        }
    }

    public List<BackupInfo> listBackups() {
        Path dir = AppConfig.getBackupDirectory();
        if (!Files.isDirectory(dir)) return List.of();
        List<BackupInfo> out = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".db")).sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    out.add(new BackupInfo(p, Files.size(p), LocalDateTime.ofInstant(Files.getLastModifiedTime(p).toInstant(), java.time.ZoneId.systemDefault())));
                } catch (IOException ignored) { /* skip unreadable */ }
            });
        } catch (IOException e) {
            throw new DataAccessException("Cannot list backups", e);
        }
        return out;
    }
}
