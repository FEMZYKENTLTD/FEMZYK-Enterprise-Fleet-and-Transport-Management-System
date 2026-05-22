package com.femzyk.vehiclesystem.gui;

import com.femzyk.vehiclesystem.interfaces.Vehicle;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * StoredFleetData
 *
 * PURPOSE:
 * Serializable data container for one user's saved fleet state.
 *
 * STORAGE ROLE:
 * This object is written to each user's fleet-data.dat file.
 *
 * It contains:
 * - active fleet vehicles
 * - recycle bin vehicles
 * - username
 * - last saved timestamp
 * - application version
 *
 * DESIGN NOTE:
 * This class is intentionally simple. It does not contain GUI logic.
 * It only stores data.
 */
public class StoredFleetData implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String username;
    private final String appVersion;
    private final List<Vehicle> activeVehicles;
    private final List<Vehicle> recycledVehicles;
    private final LocalDateTime lastSavedAt;

    public StoredFleetData(String username,
                           String appVersion,
                           List<Vehicle> activeVehicles,
                           List<Vehicle> recycledVehicles) {

        this.username = username;
        this.appVersion = appVersion;
        this.activeVehicles = new ArrayList<>(activeVehicles);
        this.recycledVehicles = new ArrayList<>(recycledVehicles);
        this.lastSavedAt = LocalDateTime.now();
    }

    public String getUsername() {
        return username;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public List<Vehicle> getActiveVehicles() {
        return new ArrayList<>(activeVehicles);
    }

    public List<Vehicle> getRecycledVehicles() {
        return new ArrayList<>(recycledVehicles);
    }

    public LocalDateTime getLastSavedAt() {
        return lastSavedAt;
    }
}