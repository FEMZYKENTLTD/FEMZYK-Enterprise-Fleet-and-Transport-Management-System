package com.femzyk.fleetmanagement;

import com.femzyk.fleetmanagement.reporting.ReportService;
import com.femzyk.fleetmanagement.service.DashboardService;
import com.femzyk.fleetmanagement.service.DemoDataService;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SmokeTest {

    @Test
    void bootstrapDemoDataAndReportsEndToEnd() throws Exception {
        ServiceRegistry r = TestSupport.freshRegistryLoggedInAsAdmin();
        DemoDataService.Summary s = r.demoData.load();
        assertTrue(s.loaded());
        assertEquals(10, s.employees());
        assertFalse(r.demoData.load().loaded(), "second load must be a no-op");

        DashboardService.Snapshot snap = r.dashboard.snapshot();
        assertEquals(10, snap.totalVehicles());
        assertTrue(snap.activeAssignments() >= 4);
        assertEquals(1, snap.activeTrips());
        assertTrue(snap.overdueMaintenance() >= 1);
        assertTrue(snap.expiringLicences() >= 1);
        assertTrue(snap.monthTotalExpenses() >= 0);

        Path dir = Files.createTempDirectory("fleet-reports");
        for (ReportService.Kind k : ReportService.Kind.values()) {
            var report = r.reports.build(k, null, null);
            for (ReportService.Format f : ReportService.Format.values()) {
                Path out = r.reports.export(report, f, dir.resolve(k + "." + f.name().toLowerCase()));
                assertTrue(Files.size(out) > 100, k + " " + f);
            }
        }
        assertTrue(r.auditLogRepository.count() > 50);
    }
}
