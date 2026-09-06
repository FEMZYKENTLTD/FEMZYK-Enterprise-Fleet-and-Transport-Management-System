package com.femzyk.fleetmanagement.service;

import com.femzyk.fleetmanagement.TestSupport;
import com.femzyk.fleetmanagement.reporting.PdfReportExporter;
import com.femzyk.fleetmanagement.reporting.Report;
import com.femzyk.fleetmanagement.reporting.ReportService;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DashboardAndReportTest {

    @Test void emptyDatabaseGivesZeroSnapshotNotErrors() {
        ServiceRegistry s = TestSupport.freshRegistryLoggedInAsAdmin();
        DashboardService.Snapshot d = s.dashboard.snapshot();
        assertEquals(0, d.totalVehicles());
        assertEquals(0.0, d.monthTotalExpenses());
        assertEquals(6, d.expensesByMonth().size(), "six month buckets always present");
        assertTrue(d.maintenanceAlerts().isEmpty());
    }

    @Test void snapshotReflectsDemoData() {
        ServiceRegistry s = TestSupport.freshRegistryLoggedInAsAdmin();
        s.demoData.load();
        DashboardService.Snapshot d = s.dashboard.snapshot();
        assertEquals(10, d.totalVehicles());
        assertEquals(s.vehicleRepository.count(null), d.totalVehicles());
        assertEquals(1, d.retiredVehicles());
        assertEquals(1, d.outOfServiceVehicles());
        assertEquals(10, d.totalEmployees());
        assertEquals(6, d.activeDrivers());
        assertEquals(4, d.activeAssignments());
        assertEquals(1, d.activeTrips());
        assertEquals(1, d.plannedTrips());
        assertTrue(d.expensesByCategory().values().stream().mapToDouble(Double::doubleValue).sum() > 0);
    }

    @Test void reportsContainRealRowsAndPdfIsWellFormed() throws Exception {
        ServiceRegistry s = TestSupport.freshRegistryLoggedInAsAdmin();
        s.demoData.load();
        Report fleet = s.reports.build(ReportService.Kind.FLEET, null, null);
        assertEquals(10, fleet.getSections().get(0).getRows().size());
        assertEquals("10", fleet.getSummary().get("Total vehicles"));
        Report trips = s.reports.build(ReportService.Kind.TRIP, java.time.LocalDate.now().minusDays(30), java.time.LocalDate.now());
        assertTrue(trips.getSections().get(0).getRows().size() >= 5);
        byte[] pdf = new PdfReportExporter().render(fleet);
        String head = new String(pdf, 0, 8, StandardCharsets.ISO_8859_1);
        assertTrue(head.startsWith("%PDF-1.4"));
        String tail = new String(pdf, pdf.length - 6, 6, StandardCharsets.ISO_8859_1);
        assertTrue(tail.contains("%%EOF"));
        assertTrue(new String(pdf, StandardCharsets.ISO_8859_1).contains("/Type /Page "));
    }
}
