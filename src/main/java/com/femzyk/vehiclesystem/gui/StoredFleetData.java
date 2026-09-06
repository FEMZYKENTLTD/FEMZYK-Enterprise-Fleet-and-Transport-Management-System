package com.femzyk.vehiclesystem.gui;

import com.femzyk.vehiclesystem.interfaces.Vehicle;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Legacy shadow class for deserialising v4.x fleet-data.dat files. Field names/SUID must not change. */
public class StoredFleetData implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String appVersion;
    private List<Vehicle> activeVehicles;
    private List<Vehicle> recycledVehicles;
    private LocalDateTime lastSavedAt;

    public String getUsername() { return username; }
    public String getAppVersion() { return appVersion; }
    public List<Vehicle> getActiveVehicles() { return activeVehicles == null ? List.of() : new ArrayList<>(activeVehicles); }
    public List<Vehicle> getRecycledVehicles() { return recycledVehicles == null ? List.of() : new ArrayList<>(recycledVehicles); }
    public LocalDateTime getLastSavedAt() { return lastSavedAt; }
}
