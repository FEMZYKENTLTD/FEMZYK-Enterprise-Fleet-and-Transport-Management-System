package com.femzyk.vehiclesystem;

import com.femzyk.vehiclesystem.gui.LoginDialog;
import com.femzyk.vehiclesystem.gui.MainWindow;
import com.femzyk.vehiclesystem.gui.UserDatabase;

import javax.swing.*;

/**
 * VehicleRentalSystem - Application Entry Point
 *
 * PURPOSE:
 * Starts the entire Vehicle Management System application.
 *
 * STARTUP FLOW:
 * 1. The program starts on the Swing Event Dispatch Thread.
 * 2. The login/signup dialog is displayed.
 * 3. If login succeeds, MainWindow opens for that authenticated user.
 * 4. If the user logs out, the application returns to the login dialog.
 * 5. If the user exits from the login dialog, the application closes.
 *
 * SWING THREADING NOTE:
 * Swing applications should create and update GUI components on the
 * Event Dispatch Thread. SwingUtilities.invokeLater ensures this.
 */
public class VehicleRentalSystem {

    /**
     * Main application entry point.
     *
     * @param args command-line arguments; not used
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(VehicleRentalSystem::showLoginAndLaunch);
    }

    /**
     * Shows the login dialog and opens the main application if login succeeds.
     *
     * This method is also reused after logout so another user can log in
     * without restarting the program.
     */
    public static void showLoginAndLaunch() {
        UserDatabase userDatabase = new UserDatabase();

        LoginDialog loginDialog = new LoginDialog(null, userDatabase);
        loginDialog.setVisible(true);

        String username = loginDialog.getAuthenticatedUsername();

        if (username == null) {
            System.exit(0);
            return;
        }

        new MainWindow(username, VehicleRentalSystem::showLoginAndLaunch);
    }
}