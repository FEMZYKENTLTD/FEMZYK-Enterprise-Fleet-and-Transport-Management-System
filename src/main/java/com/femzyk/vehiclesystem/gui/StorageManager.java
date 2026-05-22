package com.femzyk.vehiclesystem.gui;

import com.femzyk.vehiclesystem.interfaces.Vehicle;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * StorageManager
 *
 * PURPOSE:
 * Handles saving and loading fleet data for one logged-in user.
 *
 * STORAGE LOCATION:
 * Data is stored under:
 *
 *     user-home/FemzykVehicleSystem/profiles/{username}/fleet-data.dat
 *
 * WHY ONE FILE PER USER:
 * Each user has separate active fleet and recycle bin data. This prevents
 * one user's vehicles from appearing in another user's profile.
 *
 * DESIGN NOTE:
 * StorageManager does not know anything about buttons or panels. It only
 * manages files. This keeps storage logic separate from GUI logic.
 */
public class StorageManager {

    private static final String APP_FOLDER = "FemzykVehicleSystem";
    private static final String PROFILES_FOLDER = "profiles";
    private static final String DATA_FILE = "fleet-data.dat";
    private static final String APP_VERSION = "4.0+ Storage Edition";

    private final String username;
    private final Path profileDirectory;
    private final Path dataFile;

    public StorageManager(String username) {
        this.username = username;

        this.profileDirectory = Paths.get(
            System.getProperty("user.home"),
            APP_FOLDER,
            PROFILES_FOLDER,
            sanitizeForFolder(username)
        );

        this.dataFile = profileDirectory.resolve(DATA_FILE);

        createProfileDirectory();
    }

    private void createProfileDirectory() {
        try {
            Files.createDirectories(profileDirectory);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Unable to create profile folder: " + profileDirectory, e
            );
        }
    }

    /**
     * Loads saved fleet data for the current user.
     *
     * If no file exists, an empty fleet is returned.
     * If the file is corrupted, it is backed up and an empty fleet is returned.
     *
     * @return saved fleet data or empty fleet data
     */
    public StoredFleetData load() {
        if (!Files.exists(dataFile)) {
            return new StoredFleetData(username, APP_VERSION, List.of(), List.of());
        }

        try (ObjectInputStream input =
                 new ObjectInputStream(Files.newInputStream(dataFile))) {

            Object object = input.readObject();

            if (object instanceof StoredFleetData data) {
                return data;
            }

        } catch (Exception e) {
            backupCorruptDataFile();
        }

        return new StoredFleetData(username, APP_VERSION, List.of(), List.of());
    }

    /**
     * Saves active and recycled vehicles for the current user.
     *
     * @param activeVehicles   vehicles currently visible in the fleet
     * @param recycledVehicles vehicles currently in recycle bin
     */
    public void save(List<Vehicle> activeVehicles,
                     List<Vehicle> recycledVehicles) throws IOException {

        Files.createDirectories(profileDirectory);

        StoredFleetData data = new StoredFleetData(
            username,
            APP_VERSION,
            activeVehicles,
            recycledVehicles
        );

        try (ObjectOutputStream output =
                 new ObjectOutputStream(Files.newOutputStream(dataFile))) {
            output.writeObject(data);
        }
    }

    /**
     * If the saved data file becomes unreadable, keep a backup instead of
     * silently deleting it.
     */
    private void backupCorruptDataFile() {
        try {
            if (Files.exists(dataFile)) {
                Path backup = profileDirectory.resolve(
                    "fleet-data-corrupt-" + System.currentTimeMillis() + ".dat"
                );
                Files.move(dataFile, backup);
            }
        } catch (IOException ignored) {
            // Start fresh even if the backup cannot be created.
        }
    }

    private String sanitizeForFolder(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "default";
        }

        return value.trim()
            .replaceAll("[^a-zA-Z0-9._-]", "_")
            .replaceAll("_+", "_");
    }

    public String getUsername() {
        return username;
    }

    public Path getDataFile() {
        return dataFile;
    }

    public String getStorageLocation() {
        return dataFile.toString();
    }
}