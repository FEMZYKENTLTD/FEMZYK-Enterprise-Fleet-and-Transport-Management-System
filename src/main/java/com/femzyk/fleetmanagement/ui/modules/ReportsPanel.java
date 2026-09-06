package com.femzyk.fleetmanagement.ui.modules;

import com.femzyk.fleetmanagement.model.Permission;
import com.femzyk.fleetmanagement.reporting.HtmlReportExporter;
import com.femzyk.fleetmanagement.reporting.Report;
import com.femzyk.fleetmanagement.reporting.ReportService;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import com.femzyk.fleetmanagement.ui.ModulePanel;
import com.femzyk.fleetmanagement.ui.Theme;
import com.femzyk.fleetmanagement.ui.UiUtils;
import com.femzyk.fleetmanagement.util.DateUtil;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.time.LocalDate;

/** Report builder: pick a report and period, preview it, export as CSV / HTML / PDF. */
public class ReportsPanel extends JPanel implements ModulePanel {

    private final ServiceRegistry s;
    private final JComboBox<ReportService.Kind> kind = new JComboBox<>(ReportService.Kind.values());
    private final JTextField from = new JTextField(10);
    private final JTextField to = new JTextField(10);
    private final JEditorPane preview = new JEditorPane();
    private Report current;

    public ReportsPanel(ServiceRegistry services) {
        super(new BorderLayout(0, 10));
        this.s = services;
        setBackground(Theme.BG);
        setBorder(Theme.padding(16));
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(UiUtils.title("Reports"));
        titles.add(UiUtils.muted("Generated from live data; export to CSV for spreadsheets, HTML for printing, or PDF for submission"));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        controls.add(new JLabel("Report:")); controls.add(kind);
        controls.add(new JLabel("From:")); controls.add(from);
        controls.add(new JLabel("To:")); controls.add(to);
        from.setToolTipText("yyyy-MM-dd (blank = all time)");
        to.setToolTipText("yyyy-MM-dd (blank = today)");
        controls.add(UiUtils.neutral("This month", () -> { from.setText(DateUtil.format(LocalDate.now().withDayOfMonth(1))); to.setText(DateUtil.format(LocalDate.now())); generate(); }));
        controls.add(UiUtils.neutral("This year", () -> { from.setText(DateUtil.format(LocalDate.now().withDayOfYear(1))); to.setText(DateUtil.format(LocalDate.now())); generate(); }));
        controls.add(UiUtils.neutral("All time", () -> { from.setText(""); to.setText(""); generate(); }));
        controls.add(UiUtils.primary("Generate", this::generate));
        if (SessionContext.has(Permission.EXPORT_DATA)) {
            controls.add(UiUtils.success("Export CSV", () -> export(ReportService.Format.CSV)));
            controls.add(UiUtils.success("Export HTML", () -> export(ReportService.Format.HTML)));
            controls.add(UiUtils.success("Export PDF", () -> export(ReportService.Format.PDF)));
        }
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(titles, BorderLayout.NORTH);
        top.add(controls, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        preview.setContentType("text/html");
        preview.setEditable(false);
        JScrollPane scroll = new JScrollPane(preview);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        add(scroll, BorderLayout.CENTER);
        kind.addActionListener(e -> generate());
    }

    @Override public String id() { return "reports"; }
    @Override public String title() { return "Reports"; }
    @Override public JComponent component() { return this; }
    @Override public void refresh() { if (current != null) generate(); else generate(); }

    private LocalDate parse(JTextField f) {
        String t = f.getText();
        return t == null || t.isBlank() ? null : DateUtil.parseUserDate(t.trim());
    }

    private void generate() {
        current = UiUtils.call(this, () -> s.reports.build((ReportService.Kind) kind.getSelectedItem(), parse(from), parse(to)));
        if (current == null) return;
        preview.setText(new HtmlReportExporter().render(current));
        preview.setCaretPosition(0);
    }

    private void export(ReportService.Format format) {
        if (current == null) generate();
        if (current == null) return;
        JFileChooser fc = new JFileChooser();
        Path def = s.reports.defaultTarget((ReportService.Kind) kind.getSelectedItem(), format);
        fc.setSelectedFile(def.toFile());
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path target = fc.getSelectedFile().toPath();
        if (!target.toString().toLowerCase().endsWith("." + format.name().toLowerCase())) target = target.resolveSibling(target.getFileName() + "." + format.name().toLowerCase());
        final Path t = target;
        if (UiUtils.run(this, () -> { try { s.reports.export(current, format, t); } catch (java.io.IOException e) { throw new com.femzyk.fleetmanagement.exception.DataAccessException("Could not write file: " + e.getMessage(), e); } })) {
            UiUtils.info(this, "Report exported", "Saved to:\n" + t.toAbsolutePath());
        }
    }
}
