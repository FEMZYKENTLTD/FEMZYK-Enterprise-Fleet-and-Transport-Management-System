package com.femzyk.fleetmanagement.ui.modules;

import com.femzyk.fleetmanagement.config.AppConfig;
import com.femzyk.fleetmanagement.io.BackupService;
import com.femzyk.fleetmanagement.io.CsvImportExportService;
import com.femzyk.fleetmanagement.legacy.LegacyDataMigrator;
import com.femzyk.fleetmanagement.model.Permission;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.service.DemoDataService;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import com.femzyk.fleetmanagement.ui.ModulePanel;
import com.femzyk.fleetmanagement.ui.Theme;
import com.femzyk.fleetmanagement.ui.UiUtils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Data & system tools: backup/restore, CSV import/export, demo data, legacy migration, environment info. */
public class SystemPanel extends JPanel implements ModulePanel {

    private final ServiceRegistry s;
    private final Runnable onDatabaseReplaced;
    private final JTextArea info = new JTextArea();

    public SystemPanel(ServiceRegistry services, Runnable onDatabaseReplaced) {
        super(new BorderLayout(0, 12));
        this.s = services;
        this.onDatabaseReplaced = onDatabaseReplaced;
        setBackground(Theme.BG);
        setBorder(Theme.padding(16));
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(UiUtils.title("Data & System"));
        titles.add(UiUtils.muted("Backups, CSV import/export, demonstration data and migration from the old .dat storage"));
        add(titles, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 12));
        grid.setOpaque(false);
        grid.add(section("Backup & restore", "Backups are verified copies of the SQLite database (integrity check + table check) stored in the backups folder.",
                UiUtils.primary("Create backup now", this::backup), UiUtils.neutral("Restore from backup...", this::restore), UiUtils.neutral("Open backups folder info", this::listBackups)));
        grid.add(section("CSV export", "Export any module to a UTF-8 CSV file for Excel / Google Sheets.",
                UiUtils.primary("Export...", this::exportCsv)));
        grid.add(section("CSV import", "Import employees, vehicles or drivers. Every row goes through the same validation as the forms; rejected rows are listed.",
                UiUtils.primary("Import...", this::importCsv), UiUtils.neutral("Save import template...", this::template)));
        grid.add(section("Demonstration data", "Loads a clearly marked sample data set (10 employees, 6 drivers, 10 vehicles, trips, fuel, maintenance, expenses). Idempotent.",
                UiUtils.primary("Load demo data", this::demo)));
        grid.add(section("Legacy migration", "Imports users and vehicles from the previous Femzyk Vehicle Management System .dat files (" + AppConfig.getLegacyDataDirectory() + "). Original files are not modified.",
                UiUtils.primary("Scan & migrate legacy data", this::migrate)));
        JPanel infoCard = UiUtils.card();
        infoCard.add(UiUtils.heading("Environment"), BorderLayout.NORTH);
        info.setEditable(false); info.setFont(Theme.FONT_MONO); info.setOpaque(false);
        infoCard.add(info, BorderLayout.CENTER);
        grid.add(infoCard);
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BG);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel section(String title, String text, JButton... buttons) {
        JPanel card = UiUtils.card();
        card.add(UiUtils.heading(title), BorderLayout.NORTH);
        JTextArea t = new JTextArea(text);
        t.setLineWrap(true); t.setWrapStyleWord(true); t.setEditable(false); t.setOpaque(false); t.setFont(Theme.FONT_BODY); t.setForeground(Theme.TEXT_MUTED);
        t.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        card.add(t, BorderLayout.CENTER);
        JPanel b = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        b.setOpaque(false);
        for (JButton x : buttons) b.add(x);
        card.add(b, BorderLayout.SOUTH);
        return card;
    }

    @Override public String id() { return "system"; }
    @Override public String title() { return "Data & System"; }
    @Override public JComponent component() { return this; }

    @Override
    public void refresh() {
        StringBuilder b = new StringBuilder();
        b.append("Application : ").append(AppConfig.APP_NAME).append(" v").append(AppConfig.APP_VERSION).append('\n');
        b.append("Database    : ").append(s.database().getDatabasePath()).append('\n');
        b.append("Backups     : ").append(AppConfig.getBackupDirectory()).append('\n');
        b.append("Exports     : ").append(AppConfig.getExportDirectory()).append('\n');
        b.append("Logs        : ").append(AppConfig.getLogDirectory()).append('\n');
        b.append("Java        : ").append(System.getProperty("java.version")).append(" (").append(System.getProperty("java.vendor")).append(")\n");
        b.append("Demo data   : ").append(s.demoData.isLoaded() ? "loaded" : "not loaded").append('\n');
        b.append("Audit rows  : ").append(s.auditLogRepository.count()).append('\n');
        b.append("Signed in   : ").append(SessionContext.currentUsername()).append('\n');
        info.setText(b.toString());
    }

    private void backup() {
        Path p = UiUtils.call(this, s.backup::createBackup);
        if (p != null) UiUtils.info(this, "Backup created", "Verified backup written to:\n" + p.toAbsolutePath());
    }

    private void restore() {
        JFileChooser fc = new JFileChooser(AppConfig.getBackupDirectory().toFile());
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SQLite database (*.db)", "db"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path file = fc.getSelectedFile().toPath();
        BackupService.ValidationResult v = s.backup.validate(file);
        if (!v.valid()) { UiUtils.info(this, "Invalid backup", v.message()); return; }
        if (!UiUtils.confirm(this, "Restore database", "Replace the current database with this backup?\n" + v.message() + "\n\nA safety copy of the current database is kept. You will be signed out.")) return;
        if (UiUtils.run(this, () -> s.backup.restore(file))) onDatabaseReplaced.run();
    }

    private void listBackups() {
        List<BackupService.BackupInfo> list = s.backup.listBackups();
        StringBuilder b = new StringBuilder("Folder: " + AppConfig.getBackupDirectory() + "\n\n");
        if (list.isEmpty()) b.append("No backups yet.");
        for (BackupService.BackupInfo i : list) b.append(i.file().getFileName()).append("  ").append(i.sizeBytes() / 1024).append(" KB  ").append(i.createdAt().withNano(0)).append('\n');
        UiUtils.info(this, "Backups", b.toString());
    }

    private void exportCsv() {
        Object pick = JOptionPane.showInputDialog(this, "Export which data?", "CSV export", JOptionPane.QUESTION_MESSAGE, null, CsvImportExportService.Entity.values(), CsvImportExportService.Entity.VEHICLES);
        if (pick == null) return;
        CsvImportExportService.Entity e = (CsvImportExportService.Entity) pick;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(AppConfig.getExportDirectory().resolve(e.name().toLowerCase() + "-" + java.time.LocalDate.now() + ".csv").toFile());
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path target = fc.getSelectedFile().toPath();
        if (UiUtils.run(this, () -> { try { s.csv.export(e, target); } catch (java.io.IOException ex) { throw new com.femzyk.fleetmanagement.exception.DataAccessException("Could not write file: " + ex.getMessage(), ex); } }))
            UiUtils.info(this, "Exported", "Saved to:\n" + target.toAbsolutePath());
    }

    private void template() {
        CsvImportExportService.Entity[] options = {CsvImportExportService.Entity.EMPLOYEES, CsvImportExportService.Entity.VEHICLES, CsvImportExportService.Entity.DRIVERS};
        Object pick = JOptionPane.showInputDialog(this, "Template for:", "Import template", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (pick == null) return;
        CsvImportExportService.Entity e = (CsvImportExportService.Entity) pick;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(AppConfig.getExportDirectory().resolve(e.name().toLowerCase() + "-import-template.csv").toFile());
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path target = fc.getSelectedFile().toPath();
        if (UiUtils.run(this, () -> { try { s.csv.writeTemplate(e, target); } catch (java.io.IOException ex) { throw new com.femzyk.fleetmanagement.exception.DataAccessException(ex.getMessage(), ex); } }))
            UiUtils.info(this, "Template saved", target.toAbsolutePath().toString());
    }

    private void importCsv() {
        if (!SessionContext.has(Permission.IMPORT_DATA)) { UiUtils.info(this, "Permission", "Your role cannot import data."); return; }
        CsvImportExportService.Entity[] options = {CsvImportExportService.Entity.EMPLOYEES, CsvImportExportService.Entity.VEHICLES, CsvImportExportService.Entity.DRIVERS};
        Object pick = JOptionPane.showInputDialog(this, "Import which data?", "CSV import", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (pick == null) return;
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV files", "csv"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path file = fc.getSelectedFile().toPath();
        CsvImportExportService.ImportResult r = UiUtils.call(this, () -> s.csv.importFile((CsvImportExportService.Entity) pick, file));
        if (r == null) return;
        StringBuilder b = new StringBuilder("Imported: " + r.imported() + "\nSkipped blank rows: " + r.skipped() + "\nRejected: " + r.errors().size() + "\n");
        if (r.hasErrors()) { b.append("\n"); r.errors().stream().limit(30).forEach(x -> b.append(x).append('\n')); if (r.errors().size() > 30) b.append("...\n"); }
        UiUtils.info(this, "Import finished", b.toString());
        onDataChanged();
    }

    private void demo() {
        if (s.demoData.isLoaded()) { UiUtils.info(this, "Demo data", "The demonstration data set is already loaded."); return; }
        if (!UiUtils.confirm(this, "Load demo data", "Load the demonstration data set? Records are marked " + DemoDataService.MARKER + " in their notes.")) return;
        DemoDataService.Summary r = UiUtils.call(this, s.demoData::load);
        if (r != null && r.loaded()) {
            UiUtils.info(this, "Demo data loaded", String.format("Employees %d, drivers %d, vehicles %d, assignments %d, trips %d, maintenance %d, fuel %d, expenses %d.",
                    r.employees(), r.drivers(), r.vehicles(), r.assignments(), r.trips(), r.maintenance(), r.fuel(), r.expenses()));
            onDataChanged();
        }
    }

    private void migrate() {
        List<LegacyDataMigrator.LegacySource> sources = s.legacyMigrator.scan();
        if (sources.isEmpty()) { UiUtils.info(this, "Legacy migration", "No legacy files found under " + AppConfig.getLegacyDataDirectory() + ".\nExpected users.dat and profiles/<user>/fleet-data.dat."); return; }
        StringBuilder b = new StringBuilder("Found:\n");
        for (var src : sources) b.append("  ").append(src.kind()).append(" - ").append(src.file()).append(src.alreadyMigrated() ? "  (already migrated)" : "").append('\n');
        b.append("\nImport pending files now? Original files are left untouched.");
        if (!UiUtils.confirm(this, "Legacy migration", b.toString())) return;
        LegacyDataMigrator.MigrationResult r = UiUtils.call(this, s.legacyMigrator::migrateAll);
        if (r == null) return;
        UiUtils.info(this, "Migration finished", "Users imported: " + r.usersImported() + " (skipped " + r.usersSkipped() + ")\nVehicles imported: " + r.vehiclesImported()
                + " (" + r.vehiclesRecycled() + " into recycle bin)\n\n" + String.join("\n", r.messages()));
        onDataChanged();
    }

    private void onDataChanged() { refresh(); firePropertyChange("dataChanged", false, true); }

    static boolean exists(Path p) { return p != null && Files.exists(p); }
}
