package com.femzyk.fleetmanagement.legacy;

import com.femzyk.fleetmanagement.TestSupport;
import com.femzyk.fleetmanagement.model.*;
import com.femzyk.fleetmanagement.security.PasswordUtil;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Uses real v4.x serialized files (src/test/resources/legacy-v4) produced by the ORIGINAL vehiclesystem classes
 * (users.dat with one account, fleet-data.dat with 2 active vehicles + 1 recycled).
 */
class LegacyDataMigratorTest {

    static final Path LEGACY = Path.of("src", "test", "resources", "legacy-v4");

    @Test void scanFindsBothFiles() {
        ServiceRegistry s = TestSupport.freshRegistryLoggedInAsAdmin();
        List<LegacyDataMigrator.LegacySource> found = s.legacyMigrator.scan(LEGACY);
        assertEquals(2, found.size());
        assertTrue(found.stream().noneMatch(LegacyDataMigrator.LegacySource::alreadyMigrated));
    }

    @Test void migratesUsersAndVehiclesNonDestructivelyAndIdempotently() throws Exception {
        ServiceRegistry s = TestSupport.freshRegistryLoggedInAsAdmin();
        long usersBefore = Files.size(LEGACY.resolve("users.dat"));
        LegacyDataMigrator.MigrationResult r = s.legacyMigrator.migrateAll(LEGACY);
        assertEquals(1, r.usersImported());
        assertEquals(3, r.vehiclesImported());
        assertEquals(1, r.vehiclesRecycled());

        User femi = s.userRepository.findByUsername("femi").orElseThrow();
        assertEquals(Role.FLEET_MANAGER, femi.getRole());
        assertTrue(femi.isMustChangePassword());
        assertTrue(PasswordUtil.verifyPassword("Legacy2024".toCharArray(), femi.getPasswordSalt(), femi.getPasswordHash()), "old password still works");

        List<Vehicle> active = s.vehicleRepository.findAll();
        assertEquals(2, active.size());
        assertEquals(1, s.vehicleRepository.findDeleted().size());
        Vehicle car = active.stream().filter(v -> v.getVehicleType() == VehicleType.CAR).findFirst().orElseThrow();
        assertEquals("Toyota", car.getMake());
        assertEquals(4, ((Car) car).getNumberOfDoors());
        assertTrue(car.getRegistrationNumber().startsWith("MIG-"));
        assertTrue(car.getNotes().contains("Mr Ade"));
        Vehicle truck = active.stream().filter(v -> v.getVehicleType() == VehicleType.TRUCK).findFirst().orElseThrow();
        assertEquals(3.5, ((Truck) truck).getCargoCapacityTons());

        assertEquals(usersBefore, Files.size(LEGACY.resolve("users.dat")), "legacy file untouched");
        LegacyDataMigrator.MigrationResult again = s.legacyMigrator.migrateAll(LEGACY);
        assertEquals(0, again.usersImported());
        assertEquals(0, again.vehiclesImported());
        assertEquals(2, s.vehicleRepository.findAll().size());
    }
}
