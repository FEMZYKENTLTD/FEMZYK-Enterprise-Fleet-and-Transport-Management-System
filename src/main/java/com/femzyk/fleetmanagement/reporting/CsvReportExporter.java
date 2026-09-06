package com.femzyk.fleetmanagement.reporting;

import com.femzyk.fleetmanagement.io.CsvUtil;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Writes the report as UTF-8 CSV: metadata rows, summary rows, then each section with its own header. */
public class CsvReportExporter implements ReportExporter {

    @Override public String fileExtension() { return "csv"; }

    @Override
    public void export(Report r, Path target) throws IOException {
        Files.createDirectories(target.toAbsolutePath().getParent());
        try (Writer w = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            CsvUtil.writeRow(w, r.getTitle());
            if (r.getSubtitle() != null) CsvUtil.writeRow(w, r.getSubtitle());
            CsvUtil.writeRow(w, "Generated", r.getGeneratedAt().withNano(0).toString(), "By", r.getGeneratedBy());
            if (!r.getSummary().isEmpty()) {
                CsvUtil.writeRow(w);
                CsvUtil.writeRow(w, "Summary");
                for (Map.Entry<String, String> e : r.getSummary().entrySet()) CsvUtil.writeRow(w, e.getKey(), e.getValue());
            }
            for (Report.Section s : r.getSections()) {
                CsvUtil.writeRow(w);
                CsvUtil.writeRow(w, s.getTitle());
                CsvUtil.writeRow(w, s.getColumns().toArray());
                for (var row : s.getRows()) CsvUtil.writeRow(w, row.toArray());
            }
            if (!r.getNotes().isEmpty()) {
                CsvUtil.writeRow(w);
                for (String n : r.getNotes()) CsvUtil.writeRow(w, "Note", n);
            }
        }
    }
}
