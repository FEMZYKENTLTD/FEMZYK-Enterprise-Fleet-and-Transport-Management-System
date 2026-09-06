package com.femzyk.fleetmanagement.io;

import com.femzyk.fleetmanagement.TestSupport;
import com.femzyk.fleetmanagement.config.AppConfig;
import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BackupAndCsvTest {

    Path dir;

    @AfterEach void cleanup() { AppConfig.setDataDirectory(null); }

    private ServiceRegistry fileBackedRegistry() throws Exception {
        dir = Files.createTempDirectory("fleet-test");
        AppConfig.setDataDirectory(dir);
        ServiceRegistry r = new ServiceRegistry(DatabaseManager.forFile(dir.resolve("fleet.db")));
        r.bootstrap();
        TestSupport.loginAsAdmin(r);
        return r;
    }

    @Test void backupIsVerifiedAndRestoreBringsDataBack() throws Exception {
        ServiceRegistry r = fileBackedRegistry();
        r.demoData.load();
        Path backup = r.backup.createBackup(dir.resolve("backups"));
        assertTrue(Files.size(backup) > 10_000);
        BackupService.ValidationResult v = r.backup.validate(backup);
        assertTrue(v.valid(), v.message());
        assertEquals(10, v.vehicleCount());
        // damage the live data, then restore
        r.vehicles.delete(r.vehicleRepository.findAll().get(0).getId());
        assertEquals(9, r.vehicleRepository.count(null));
        r.backup.restore(backup);
        ServiceRegistry after = new ServiceRegistry(DatabaseManager.forFile(dir.resolve("fleet.db")));
        assertEquals(10, after.vehicleRepository.count(null));
        assertTrue(Files.list(dir).anyMatch(p -> p.getFileName().toString().contains("pre-restore")), "safety copy kept");
        after.database().close();
    }

    @Test void invalidFileIsRejectedForRestore() throws Exception {
        ServiceRegistry r = fileBackedRegistry();
        Path junk = dir.resolve("junk.db");
        Files.writeString(junk, "not a database");
        assertFalse(r.backup.validate(junk).valid());
        assertThrows(RuntimeException.class, () -> r.backup.restore(junk));
        r.database().close();
    }

    @Test void csvParserHandlesQuotesCommasAndNewlines() throws Exception {
        String csv = "a,b,c\r\n1,\"hello, world\",\"line1\nline2\"\r\n2,\"say \"\"hi\"\"\",\r\n";
        List<List<String>> rows = CsvUtil.read(new StringReader(csv));
        assertEquals(3, rows.size());
        assertEquals("hello, world", rows.get(1).get(1));
        assertEquals("line1\nline2", rows.get(1).get(2));
        assertEquals("say \"hi\"", rows.get(2).get(1));
        assertEquals("", rows.get(2).get(2));
        assertEquals("\"a,b\"", CsvUtil.escape("a,b"));
    }

    @Test void exportThenImportRoundTripWithRowLevelErrors() throws Exception {
        ServiceRegistry r = TestSupport.freshRegistryLoggedInAsAdmin();
        Path tmp = Files.createTempDirectory("csv");
        Path template = r.csv.writeTemplate(CsvImportExportService.Entity.EMPLOYEES, tmp.resolve("t.csv"));
        String header = Files.readString(template).trim();
        String body = header + "\r\n"
                + "Ada,Obi,08031112222,ada@x.com,Lagos,Ops,Officer,1990-01-01,2022-03-01,150000,ACTIVE,,,\r\n"
                + "Bad,Row,12,not-an-email,,Ops,Officer,,2022-03-01,abc,ACTIVE,,,\r\n"
                + ",,,,,,,,,,,,,\r\n"
                + "Bola,Ige,08033334444,bola@x.com,Lagos,Finance,Accountant,1988-05-05,01/02/2021,200000,ON_LEAVE,,,\r\n";
        Path in = tmp.resolve("in.csv");
        Files.writeString(in, body, StandardCharsets.UTF_8);
        CsvImportExportService.ImportResult res = r.csv.importFile(CsvImportExportService.Entity.EMPLOYEES, in);
        assertEquals(2, res.imported());
        assertEquals(1, res.skipped());
        assertEquals(1, res.errors().size());
        assertTrue(res.errors().get(0).startsWith("Line 3"));
        Path out = r.csv.export(CsvImportExportService.Entity.EMPLOYEES, tmp.resolve("out.csv"));
        List<List<String>> rows = CsvUtil.read(Files.newBufferedReader(out));
        assertEquals(3, rows.size());
        assertEquals("employee_code", rows.get(0).get(0));
        assertTrue(rows.stream().anyMatch(row -> row.contains("Bola")));
    }
}
