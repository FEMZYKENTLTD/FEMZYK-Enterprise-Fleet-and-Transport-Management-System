package com.femzyk.fleetmanagement.database;

import com.femzyk.fleetmanagement.config.AppConfig;
import com.femzyk.fleetmanagement.exception.DataAccessException;
import com.femzyk.fleetmanagement.util.AppLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns the SQLite connection for the application.
 *
 * <p>SQLite is an embedded single-writer database; a desktop application is well served by one
 * long-lived connection guarded by a lock. Foreign keys are switched on for every connection because
 * SQLite disables them by default. The pattern (home-directory database, create-if-missing) is adapted
 * from the Employee Management System's {@code DatabaseManager} and generalised.</p>
 */
public final class DatabaseManager implements AutoCloseable {

    private static final Logger LOG = AppLogger.get(DatabaseManager.class);
    private static volatile DatabaseManager instance;

    private final Path databasePath;
    private final String jdbcUrl;
    private Connection connection;
    private final Object lock = new Object();

    private DatabaseManager(Path databasePath) {
        this.databasePath = databasePath;
        this.jdbcUrl = databasePath == null ? "jdbc:sqlite::memory:" : "jdbc:sqlite:" + databasePath.toAbsolutePath();
        open();
    }

    /** Shared instance backed by {@link AppConfig#getDatabasePath()}. */
    public static DatabaseManager getInstance() {
        DatabaseManager local = instance;
        if (local == null) {
            synchronized (DatabaseManager.class) {
                local = instance;
                if (local == null) {
                    instance = local = new DatabaseManager(AppConfig.getDatabasePath());
                }
            }
        }
        return local;
    }

    /** Independent manager for a specific file (tests, backup verification). */
    public static DatabaseManager forFile(Path databasePath) {
        return new DatabaseManager(databasePath);
    }

    /** In-memory database, used heavily by unit tests. */
    public static DatabaseManager inMemory() {
        return new DatabaseManager(null);
    }

    /** Drops the shared instance so the next getInstance() reopens (used after restore). */
    public static synchronized void resetShared() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }

    private void open() {
        try {
            if (databasePath != null) {
                Files.createDirectories(databasePath.toAbsolutePath().getParent());
            }
            connection = DriverManager.getConnection(jdbcUrl);
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
                st.execute("PRAGMA journal_mode = WAL");
                st.execute("PRAGMA busy_timeout = 5000");
            }
            new SchemaInitializer(this).initialise();
            LOG.info(() -> "Database opened: " + jdbcUrl);
        } catch (SQLException | IOException e) {
            throw new DataAccessException("Unable to open the fleet database at " + jdbcUrl, e);
        }
    }

    public Connection getConnection() {
        synchronized (lock) {
            try {
                if (connection == null || connection.isClosed()) {
                    open();
                }
            } catch (SQLException e) {
                throw new DataAccessException("Database connection check failed", e);
            }
            return connection;
        }
    }

    public Object getLock() {
        return lock;
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    /**
     * Runs work inside a transaction. Nested calls join the outer transaction.
     */
    public <T> T inTransaction(TransactionalWork<T> work) {
        synchronized (lock) {
            Connection c = getConnection();
            boolean outermost;
            try {
                outermost = c.getAutoCommit();
                if (outermost) {
                    c.setAutoCommit(false);
                }
            } catch (SQLException e) {
                throw new DataAccessException("Could not begin transaction", e);
            }
            try {
                T result = work.execute(c);
                if (outermost) {
                    c.commit();
                }
                return result;
            } catch (RuntimeException | SQLException e) {
                if (outermost) {
                    try {
                        c.rollback();
                    } catch (SQLException rollbackError) {
                        LOG.log(Level.SEVERE, "Rollback failed", rollbackError);
                    }
                }
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new DataAccessException("Transaction failed", e);
            } finally {
                if (outermost) {
                    try {
                        c.setAutoCommit(true);
                    } catch (SQLException e) {
                        LOG.log(Level.WARNING, "Could not restore auto-commit", e);
                    }
                }
            }
        }
    }

    public void inTransaction(TransactionalRunnable work) {
        inTransaction(c -> {
            work.execute(c);
            return null;
        });
    }

    /** Forces a WAL checkpoint so the main file contains all data (used before backup). */
    public void checkpoint() {
        synchronized (lock) {
            try (Statement st = getConnection().createStatement()) {
                st.execute("PRAGMA wal_checkpoint(TRUNCATE)");
            } catch (SQLException e) {
                throw new DataAccessException("Checkpoint failed", e);
            }
        }
    }

    /** Runs PRAGMA integrity_check and returns true when SQLite reports "ok". */
    public boolean integrityCheck() {
        synchronized (lock) {
            try (Statement st = getConnection().createStatement();
                 var rs = st.executeQuery("PRAGMA integrity_check")) {
                return rs.next() && "ok".equalsIgnoreCase(rs.getString(1));
            } catch (SQLException e) {
                throw new DataAccessException("Integrity check failed", e);
            }
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOG.log(Level.WARNING, "Error closing database", e);
            } finally {
                connection = null;
            }
        }
    }

    @FunctionalInterface
    public interface TransactionalWork<T> {
        T execute(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface TransactionalRunnable {
        void execute(Connection connection) throws SQLException;
    }
}
