package com.femzyk.fleetmanagement.io;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/** RFC-4180-style CSV reading/writing (quotes, embedded commas, newlines, UTF-8) without external libraries. */
public final class CsvUtil {

    private CsvUtil() {}

    public static void writeRow(Writer w, Object... cells) throws IOException {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) w.write(',');
            w.write(escape(cells[i] == null ? "" : String.valueOf(cells[i])));
        }
        w.write("\r\n");
    }

    public static String escape(String s) {
        boolean needsQuotes = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r") || s.startsWith(" ") || s.endsWith(" ");
        if (!needsQuotes) return s;
        return '"' + s.replace("\"", "\"\"") + '"';
    }

    /** Parses the entire reader into rows. Handles quoted fields with embedded separators and newlines. */
    public static List<List<String>> read(Reader reader) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        int ch;
        int prev = -1;
        boolean first = true;
        while ((ch = reader.read()) != -1) {
            if (first && ch == 0xFEFF) { first = false; continue; } // BOM
            first = false;
            if (inQuotes) {
                if (ch == '"') {
                    reader.mark(1);
                    int next = reader.read();
                    if (next == '"') cell.append('"');
                    else { inQuotes = false; if (next != -1) reader.reset(); }
                } else cell.append((char) ch);
            } else {
                if (ch == '"') inQuotes = true;
                else if (ch == ',') { row.add(cell.toString()); cell.setLength(0); }
                else if (ch == '\n' || ch == '\r') {
                    if (ch == '\n' && prev == '\r') { prev = ch; continue; }
                    row.add(cell.toString()); cell.setLength(0);
                    rows.add(row); row = new ArrayList<>();
                } else cell.append((char) ch);
            }
            prev = ch;
        }
        if (cell.length() > 0 || !row.isEmpty()) { row.add(cell.toString()); rows.add(row); }
        return rows;
    }
}
