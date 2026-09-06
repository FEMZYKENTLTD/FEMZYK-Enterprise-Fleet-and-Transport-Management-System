package com.femzyk.fleetmanagement.legacy;

import com.femzyk.fleetmanagement.config.AppConfig;
import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.model.*;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import com.femzyk.fleetmanagement.service.VehicleService;
import com.femzyk.fleetmanagement.util.AppLogger;
import com.femzyk.vehiclesystem.gui.StoredFleetData;
import com.femzyk.vehiclesystem.gui.UserAccount;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * One-way, non-destructive migration of v4.x serialized data ({@code users.dat}, {@code fleet-data.dat})
 * into the SQLite database.
 *
 * <ul>
 *   <li>Legacy files are never modified or deleted; a marker setting records completed migrations so the
 *       operation is idempotent.</li>
 *   <li>Users keep their PBKDF2 salt/hash (same algorithm) so old passwords still work; they are imported
 *       with the {@link Role#FLEET_MANAGER} role and must change password at first login.</li>
 *   <li>Vehicles had no registration number in v4.x, so a clearly marked placeholder plate
 *       {@code MIG-<n>} is generated and the original renter details are kept in the notes.</li>
 *   <li>Deserialisation is restricted with an {@link ObjectInputFilter} whitelist.</li>
 * </ul>
 */
public class LegacyDataMigrator {

    private static final String SETTING_PREFIX = "legacy.migrated.";

    private final ServiceRegistry services;
    private final DatabaseManager db;

    public LegacyDataMigrator(ServiceRegistry services, DatabaseManager db) {
        this.services = services;
        this.db = db;
    }

    public record LegacySource(Path file, String kind, String owner, boolean alreadyMigrated) {}

    public record MigrationResult(int usersImported, int usersSkipped, int vehiclesImported, int vehiclesRecycled,
                                  List<String> messages) {}

    /** Finds legacy files in the default location without reading them. */
    public List<LegacySource> scan() {
        return scan(AppConfig.getLegacyDataDirectory());
    }

    public List<LegacySource> scan(Path legacyRoot) {
        List<LegacySource> out = new ArrayList<>();
        if (legacyRoot == null || !Files.isDirectory(legacyRoot)) return out;
        Path users = legacyRoot.resolve("users.dat");
        if (Files.isRegularFile(users)) out.add(new LegacySource(users, "users", "-", isMigrated(users)));
        Path profiles = legacyRoot.resolve("profiles");
        if (Files.isDirectory(profiles)) {
            try (Stream<Path> dirs = Files.list(profiles)) {
                dirs.filter(Files::isDirectory).sorted().forEach(d -> {
                    Path f = d.resolve("fleet-data.dat");
                    if (Files.isRegularFile(f)) out.add(new LegacySource(f, "fleet", d.getFileName().toString(), isMigrated(f)));
                });
            } catch (IOException e) {
                AppLogger.warn("Cannot list legacy profiles: " + e.getMessage());
            }
        }
        return out;
    }

    public boolean hasPendingMigration() {
        return scan().stream().anyMatch(s -> !s.alreadyMigrated());
    }

    public MigrationResult migrateAll() {
        return migrateAll(AppConfig.getLegacyDataDirectory());
    }

    public MigrationResult migrateAll(Path legacyRoot) {
        SessionContext.require(Permission.IMPORT_DATA);
        int users = 0, usersSkipped = 0, vehicles = 0, recycled = 0;
        List<String> messages = new ArrayList<>();
        for (LegacySource src : scan(legacyRoot)) {
            if (src.alreadyMigrated()) {
                messages.add("Skipped (already migrated): " + src.file());
                continue;
            }
            try {
                if ("users".equals(src.kind())) {
                    int[] r = migrateUsers(src.file());
                    users += r[0];
                    usersSkipped += r[1];
                    messages.add("users.dat: imported " + r[0] + ", skipped " + r[1] + " existing.");
                } else {
                    int[] r = migrateFleet(src.file(), src.owner());
                    vehicles += r[0];
                    recycled += r[1];
                    messages.add("Profile '" + src.owner() + "': imported " + r[0] + " vehicles (" + r[1] + " into recycle bin).");
                }
                markMigrated(src.file());
            } catch (Exception e) {
                AppLogger.error("Legacy migration failed for " + src.file(), e);
                messages.add("FAILED " + src.file() + ": " + e.getMessage());
            }
        }
        services.audit.record("MIGRATE", "SYSTEM", legacyRoot.toString(),
                "Legacy .dat migration: users=" + users + ", vehicles=" + vehicles + ", recycled=" + recycled);
        return new MigrationResult(users, usersSkipped, vehicles, recycled, messages);
    }

    // ---- users ----------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    int[] migrateUsers(Path file) throws IOException, ClassNotFoundException {
        Object o = readObject(file);
        if (!(o instanceof Map<?, ?> map)) throw new IOException("users.dat did not contain a user map");
        int imported = 0, skipped = 0;
        for (Object value : map.values()) {
            if (!(value instanceof UserAccount a) || a.getUsername() == null) continue;
            if (services.userRepository.findByUsername(a.getUsername()).isPresent()) { skipped++; continue; }
            User u = new User();
            u.setUsername(a.getUsername().toLowerCase());
            u.setEmail(a.getEmail() == null ? a.getUsername().toLowerCase() + "@migrated.local" : a.getEmail());
            u.setFullName(a.getUsername());
            u.setPasswordSalt(a.getPasswordSalt());
            u.setPasswordHash(a.getPasswordHash());
            u.setRole(Role.FLEET_MANAGER);
            u.setActive(true);
            u.setMustChangePassword(true);
            u.setLastLoginAt(a.getLastLoginAt());
            u.setLastPasswordResetAt(a.getLastPasswordResetAt());
            services.userRepository.save(u);
            imported++;
        }
        return new int[]{imported, skipped};
    }

    // ---- vehicles -------------------------------------------------------------------------------------

    int[] migrateFleet(Path file, String owner) throws IOException, ClassNotFoundException {
        Object o = readObject(file);
        if (!(o instanceof StoredFleetData data)) throw new IOException("fleet-data.dat did not contain StoredFleetData");
        int[] counts = new int[2];
        db.inTransaction(c -> {
            for (com.femzyk.vehiclesystem.interfaces.Vehicle lv : data.getActiveVehicles()) {
                importVehicle(lv, owner, false);
                counts[0]++;
            }
            for (com.femzyk.vehiclesystem.interfaces.Vehicle lv : data.getRecycledVehicles()) {
                importVehicle(lv, owner, true);
                counts[0]++;
                counts[1]++;
            }
        });
        return counts;
    }

    private void importVehicle(com.femzyk.vehiclesystem.interfaces.Vehicle lv, String owner, boolean recycled) {
        Vehicle v;
        if (lv instanceof com.femzyk.vehiclesystem.model.Truck t) {
            Truck truck = new Truck();
            truck.setCargoCapacityTons(t.getCargoCapacity());
            truck.setTransmission(t.getTransmissionType() == null ? "MANUAL" : t.getTransmissionType().toUpperCase());
            v = truck;
        } else if (lv instanceof com.femzyk.vehiclesystem.model.Motorcycle m) {
            Motorcycle bike = new Motorcycle();
            bike.setNumberOfWheels(m.getNumberOfWheels());
            bike.setStyle(m.getMotorcycleType() == null ? "STANDARD" : m.getMotorcycleType().toUpperCase().replace(' ', '_'));
            v = bike;
        } else if (lv instanceof com.femzyk.vehiclesystem.model.Car c) {
            Car car = new Car();
            car.setNumberOfDoors(c.getNumberOfDoors());
            v = car;
            v.setFuelType(parseFuel(c.getFuelType()));
        } else {
            v = new GenericVehicle(VehicleType.VAN);
        }
        v.setVehicleCode(services.codeSequenceRepository.next(VehicleService.CODE_PREFIX));
        v.setRegistrationNumber(services.codeSequenceRepository.next("MIG"));
        v.setMake(lv.getMake());
        v.setModel(lv.getModel());
        v.setYear(lv.getYear());
        v.setStatus(VehicleStatus.AVAILABLE);
        StringBuilder notes = new StringBuilder("Migrated from legacy profile '" + owner + "' (v4.x fleet-data.dat). ");
        notes.append("Placeholder registration - please update. ").append(lv.legacyDetail()).append('.');
        if (lv.getRenterName() != null && !lv.getRenterName().isBlank()) {
            notes.append(" Legacy renter: ").append(lv.getRenterName());
            if (lv.getRenterPhone() != null && !lv.getRenterPhone().isBlank()) notes.append(" (").append(lv.getRenterPhone()).append(')');
            notes.append('.');
        }
        v.setNotes(notes.toString());
        services.vehicleRepository.save(v);
        if (recycled) services.vehicleRepository.softDelete(v.getId());
    }

    private static FuelType parseFuel(String s) {
        if (s == null) return FuelType.PETROL;
        String u = s.trim().toUpperCase();
        for (FuelType f : FuelType.values()) if (f.name().equals(u)) return f;
        if (u.contains("GAS")) return FuelType.PETROL;
        return FuelType.PETROL;
    }

    // ---- helpers ------------------------------------------------------------------------------------

    private static Object readObject(Path file) throws IOException, ClassNotFoundException {
        try (InputStream in = Files.newInputStream(file); ObjectInputStream ois = new ObjectInputStream(in)) {
            ois.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                    "com.femzyk.vehiclesystem.**;java.util.*;java.time.*;java.lang.*;java.io.Serializable;!*"));
            return ois.readObject();
        }
    }

    private boolean isMigrated(Path file) {
        return services.settingsRepository.get(SETTING_PREFIX + file.toAbsolutePath()).isPresent();
    }

    private void markMigrated(Path file) {
        services.settingsRepository.put(SETTING_PREFIX + file.toAbsolutePath(), LocalDateTime.now().toString());
    }

    /** Mirrors the legacy StorageManager's on-disk layout for tests and documentation. */
    public static Map<String, String> legacyLayout() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("users", "~/FemzykVehicleSystem/users.dat (Map<String, UserAccount>)");
        m.put("fleet", "~/FemzykVehicleSystem/profiles/<username>/fleet-data.dat (StoredFleetData)");
        return m;
    }
}
