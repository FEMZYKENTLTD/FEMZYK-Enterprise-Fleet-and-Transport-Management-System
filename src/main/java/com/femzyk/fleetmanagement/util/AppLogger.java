package com.femzyk.fleetmanagement.util;

import com.femzyk.fleetmanagement.config.AppConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Technical logging (java.util.logging). Stack traces go to the log file, never to the user.
 */
public final class AppLogger {

    private static final Logger ROOT = Logger.getLogger("com.femzyk.fleetmanagement");
    private static boolean fileHandlerInstalled;

    private AppLogger() {}

    public static Logger get(Class<?> type) {
        return Logger.getLogger(type.getName());
    }

    public static void info(String message) { ROOT.info(message); }
    public static void warn(String message) { ROOT.warning(message); }
    public static void error(String message, Throwable t) { ROOT.log(Level.SEVERE, message, t); }

    /** Installs a rotating file handler under the data directory. Safe to call more than once. */
    public static synchronized void initialise() {
        if (fileHandlerInstalled) {
            return;
        }
        try {
            Path dir = AppConfig.getLogDirectory();
            Files.createDirectories(dir);
            FileHandler handler = new FileHandler(dir.resolve("fleet-%g.log").toString(), 1_000_000, 5, true);
            handler.setFormatter(new SimpleFormatter());
            handler.setLevel(Level.INFO);
            ROOT.addHandler(handler);
            ROOT.setLevel(Level.INFO);
            fileHandlerInstalled = true;
        } catch (IOException e) {
            ROOT.log(Level.WARNING, "Could not create log file; logging to console only.", e);
        }
    }
}
