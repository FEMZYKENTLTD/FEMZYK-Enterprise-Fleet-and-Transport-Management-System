package com.femzyk.fleetmanagement.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Central application configuration.
 *
 * <p>All paths derive from a single data directory so the application can be redirected for
 * testing (system property {@code fleet.data.dir}) or run from a portable folder.</p>
 */
public final class AppConfig {

    public static final String APP_NAME = "FEMZYK Enterprise Fleet & Transport Management System";
    public static final String APP_SHORT_NAME = "FEMZYK Fleet";
    public static final String APP_VERSION = "2.0.0";
    public static final String ORGANISATION = "FEMZYK Enterprise";

    /** Folder name in the user's home directory. */
    public static final String DATA_FOLDER_NAME = "FemzykFleetManagement";
    public static final String DATABASE_FILE_NAME = "fleet.db";
    public static final String LEGACY_FOLDER_NAME = "FemzykVehicleSystem";

    public static final int MAX_FAILED_LOGINS = 5;
    public static final int LOCKOUT_MINUTES = 15;
    public static final int LICENCE_EXPIRY_WARNING_DAYS = 30;
    public static final int MAINTENANCE_DUE_WARNING_DAYS = 14;
    public static final int MAINTENANCE_DUE_WARNING_KM = 500;

    private static volatile Path dataDirectoryOverride;

    private AppConfig() {}

    public static Path getDataDirectory() {
        Path override = dataDirectoryOverride;
        if (override != null) {
            return override;
        }
        String prop = System.getProperty("fleet.data.dir");
        if (prop != null && !prop.isBlank()) {
            return Paths.get(prop);
        }
        return Paths.get(System.getProperty("user.home"), DATA_FOLDER_NAME);
    }

    /** Used by tests and by the portable launcher to relocate all data. */
    public static void setDataDirectory(Path directory) {
        dataDirectoryOverride = directory;
    }

    public static Path getDatabasePath() {
        return getDataDirectory().resolve(DATABASE_FILE_NAME);
    }

    public static Path getBackupDirectory() {
        return getDataDirectory().resolve("backups");
    }

    public static Path getExportDirectory() {
        return getDataDirectory().resolve("exports");
    }

    public static Path getLogDirectory() {
        return getDataDirectory().resolve("logs");
    }

    public static Path getLegacyDataDirectory() {
        return Paths.get(System.getProperty("user.home"), LEGACY_FOLDER_NAME);
    }
}
