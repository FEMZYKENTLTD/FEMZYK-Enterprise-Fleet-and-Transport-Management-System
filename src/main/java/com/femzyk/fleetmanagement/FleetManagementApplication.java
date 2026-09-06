package com.femzyk.fleetmanagement;

import com.femzyk.fleetmanagement.config.AppConfig;
import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.model.User;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import com.femzyk.fleetmanagement.ui.ChangePasswordDialog;
import com.femzyk.fleetmanagement.ui.LoginDialog;
import com.femzyk.fleetmanagement.ui.MainFrame;
import com.femzyk.fleetmanagement.ui.Theme;
import com.femzyk.fleetmanagement.ui.UiUtils;
import com.femzyk.fleetmanagement.util.AppLogger;

import javax.swing.*;
import java.nio.file.Path;

/**
 * Application entry point.
 *
 * <pre>
 *   java -jar fleet-management.jar                 # normal start (data in ~/.femzyk-fleet)
 *   java -jar fleet-management.jar --data DIR      # alternative data directory
 *   java -jar fleet-management.jar --demo          # load demonstration data on first run
 *   java -jar fleet-management.jar --headless-check  # open DB, run integrity check, print summary and exit (CI / no display)
 * </pre>
 */
public final class FleetManagementApplication {

    private FleetManagementApplication() {}

    public static void main(String[] args) {
        boolean demo = false, headless = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--data" -> { if (i + 1 < args.length) AppConfig.setDataDirectory(Path.of(args[++i])); }
                case "--demo" -> demo = true;
                case "--headless-check" -> headless = true;
                case "--help", "-h" -> { System.out.println("Usage: java -jar fleet-management.jar [--data DIR] [--demo] [--headless-check]"); return; }
                default -> System.err.println("Unknown option: " + args[i]);
            }
        }
        AppLogger.initialise();
        if (headless) { headlessCheck(demo); return; }
        final boolean loadDemo = demo;
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            try {
                ServiceRegistry services = new ServiceRegistry(DatabaseManager.getInstance());
                services.bootstrap();
                if (loadDemo && !services.demoData.isLoaded()) {
                    // demo load requires a session; run it under a temporary admin session then log out
                    User admin = services.auth.login(com.femzyk.fleetmanagement.service.AuthService.DEFAULT_ADMIN_USERNAME,
                            com.femzyk.fleetmanagement.service.AuthService.DEFAULT_ADMIN_PASSWORD.toCharArray());
                    if (admin != null) { services.demoData.load(); services.auth.logout(); }
                }
                loginLoop(services);
            } catch (RuntimeException e) {
                AppLogger.error("Start-up failed", e);
                UiUtils.showError(null, e);
                System.exit(1);
            }
        });
    }

    /** Login → main window → (sign out) → login again, until the window is closed. */
    private static void loginLoop(ServiceRegistry initial) {
        // DatabaseManager reopens its connection lazily, so after a restore the same registry simply sees the restored file.
        final ServiceRegistry current = initial;
        current.bootstrap();
        User user = new LoginDialog(current).showAndGetUser();
        if (user == null) { current.database().close(); System.exit(0); return; }
        if (user.isMustChangePassword()) {
            while (!ChangePasswordDialog.show(null, current, true)) {
                if (!UiUtils.confirm(null, "Password change required", "You must change your password to continue. Try again?")) { current.auth.logout(); loginLoop(current); return; }
            }
        }
        MainFrame frame = new MainFrame(current, () -> loginLoop(current));
        frame.setVisible(true);
        if (current.legacyMigrator.hasPendingMigration() && com.femzyk.fleetmanagement.security.SessionContext.has(com.femzyk.fleetmanagement.model.Permission.IMPORT_DATA)) {
            SwingUtilities.invokeLater(() -> {
                if (UiUtils.confirm(frame, "Legacy data found", "Data files from the previous Femzyk Vehicle Management System were found in "
                        + AppConfig.getLegacyDataDirectory() + ".\nOpen Data & System to migrate them now?")) frame.show("system");
            });
        }
    }

    private static void headlessCheck(boolean demo) {
        DatabaseManager db = DatabaseManager.getInstance();
        ServiceRegistry services = new ServiceRegistry(db);
        services.bootstrap();
        System.out.println(AppConfig.APP_NAME + " v" + AppConfig.APP_VERSION);
        System.out.println("Database: " + db.getDatabasePath());
        System.out.println("Integrity check: " + (db.integrityCheck() ? "ok" : "FAILED"));
        if (demo && !services.demoData.isLoaded()) {
            services.auth.login(com.femzyk.fleetmanagement.service.AuthService.DEFAULT_ADMIN_USERNAME, com.femzyk.fleetmanagement.service.AuthService.DEFAULT_ADMIN_PASSWORD.toCharArray());
            var r = services.demoData.load();
            System.out.println("Demo data loaded: " + r);
            services.auth.logout();
        }
        System.out.println("Users: " + services.userRepository.count() + ", vehicles: " + services.vehicleRepository.count(null)
                + ", employees: " + services.employeeRepository.count(null) + ", audit rows: " + services.auditLogRepository.count());
        db.close();
    }
}
