package com.femzyk.fleetmanagement.reporting;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Format-neutral report: a title, optional period, key/value summary lines, and one or more tabular sections.
 * Exporters (CSV, HTML, PDF) render this structure, so every report supports every format.
 */
public class Report {

    public static class Section {
        private final String title;
        private final List<String> columns;
        private final List<List<String>> rows = new ArrayList<>();

        public Section(String title, List<String> columns) {
            this.title = title;
            this.columns = List.copyOf(columns);
        }

        public Section addRow(Object... cells) {
            List<String> r = new ArrayList<>(cells.length);
            for (Object c : cells) r.add(c == null ? "" : String.valueOf(c));
            rows.add(r);
            return this;
        }

        public String getTitle() { return title; }
        public List<String> getColumns() { return columns; }
        public List<List<String>> getRows() { return rows; }
    }

    private final String title;
    private final String subtitle;
    private final LocalDateTime generatedAt = LocalDateTime.now();
    private final String generatedBy;
    private final Map<String, String> summary = new LinkedHashMap<>();
    private final List<Section> sections = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();

    public Report(String title, String subtitle, String generatedBy) {
        this.title = title;
        this.subtitle = subtitle;
        this.generatedBy = generatedBy;
    }

    public Report summary(String key, Object value) { summary.put(key, value == null ? "" : String.valueOf(value)); return this; }
    public Section section(String title, String... columns) { Section s = new Section(title, List.of(columns)); sections.add(s); return s; }
    public Report note(String note) { notes.add(note); return this; }

    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public String getGeneratedBy() { return generatedBy; }
    public Map<String, String> getSummary() { return summary; }
    public List<Section> getSections() { return sections; }
    public List<String> getNotes() { return notes; }

    public int totalRows() { return sections.stream().mapToInt(s -> s.getRows().size()).sum(); }
}
