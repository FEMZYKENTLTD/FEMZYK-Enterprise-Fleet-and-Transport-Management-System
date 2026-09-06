package com.femzyk.fleetmanagement.reporting;

import com.femzyk.fleetmanagement.config.AppConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal dependency-free PDF writer (PDF 1.4, Helvetica, A4 portrait, multi-page, WinAnsi text).
 * Produces a plain tabular layout: title, summary lines, and each section as fixed-width columns.
 * Good enough for printing and submission; not a typesetting engine (long cells are truncated with an ellipsis).
 */
public class PdfReportExporter implements ReportExporter {

    private static final float PAGE_W = 595.28f, PAGE_H = 841.89f, MARGIN = 40f;
    private static final float TITLE_SIZE = 16f, H2_SIZE = 12f, BODY_SIZE = 8.5f, LINE = 12f;
    private static final float CHAR_W = 0.5f; // approx average Helvetica glyph width (em fraction)

    @Override public String fileExtension() { return "pdf"; }

    @Override
    public void export(Report r, Path target) throws IOException {
        Files.createDirectories(target.toAbsolutePath().getParent());
        Files.write(target, render(r));
    }

    // ---- layout -----------------------------------------------------------------------------------

    private static final class Page {
        final StringBuilder content = new StringBuilder();
        float y = PAGE_H - MARGIN;
    }

    public byte[] render(Report r) throws IOException {
        List<Page> pages = new ArrayList<>();
        Page p = newPage(pages);
        text(p, MARGIN, TITLE_SIZE, true, r.getTitle());
        p.y -= 4;
        String meta = AppConfig.APP_NAME + (r.getSubtitle() == null ? "" : "  |  " + r.getSubtitle())
                + "  |  Generated " + r.getGeneratedAt().withNano(0).toString().replace('T', ' ') + " by " + r.getGeneratedBy();
        text(p, MARGIN, BODY_SIZE, false, meta);
        p.y -= 6;

        if (!r.getSummary().isEmpty()) {
            p = heading(pages, p, "Summary");
            for (var e : r.getSummary().entrySet()) {
                p = ensure(pages, p, LINE);
                text(p, MARGIN, BODY_SIZE, true, fit(e.getKey(), 240));
                text(p, MARGIN + 250, BODY_SIZE, false, fit(e.getValue(), PAGE_W - 2 * MARGIN - 250), false);
                p.y -= LINE;
            }
            p.y -= 4;
        }

        for (Report.Section s : r.getSections()) {
            p = heading(pages, p, s.getTitle());
            int n = s.getColumns().size();
            float colW = (PAGE_W - 2 * MARGIN) / Math.max(1, n);
            p = ensure(pages, p, LINE * 2);
            p = tableHeader(pages, p, s, colW);
            if (s.getRows().isEmpty()) {
                p = ensure(pages, p, LINE);
                text(p, MARGIN, BODY_SIZE, false, "No records");
                p.y -= LINE;
            }
            for (List<String> row : s.getRows()) {
                if (p.y - LINE < MARGIN + 20) { p = newPage(pages); p = tableHeader(pages, p, s, colW); }
                for (int i = 0; i < n && i < row.size(); i++) {
                    text(p, MARGIN + i * colW + 2, BODY_SIZE, false, fit(row.get(i), colW - 4), false);
                }
                p.y -= LINE;
            }
            p.y -= 6;
        }
        for (String note : r.getNotes()) {
            p = ensure(pages, p, LINE);
            text(p, MARGIN, BODY_SIZE - 0.5f, false, fit(note, PAGE_W - 2 * MARGIN));
            p.y -= LINE;
        }
        // footer page numbers
        for (int i = 0; i < pages.size(); i++) {
            Page pg = pages.get(i);
            pg.content.append("BT /F1 8 Tf ").append(fmt(MARGIN)).append(' ').append(fmt(MARGIN - 15)).append(" Td (")
              .append(escape(AppConfig.APP_NAME + " v" + AppConfig.APP_VERSION + "   Page " + (i + 1) + " of " + pages.size()))
              .append(") Tj ET\n");
        }
        return assemble(pages);
    }

    private Page heading(List<Page> pages, Page p, String title) {
        p = ensure(pages, p, LINE * 3);
        p.y -= 4;
        text(p, MARGIN, H2_SIZE, true, title);
        p.y -= 4;
        return p;
    }

    private Page tableHeader(List<Page> pages, Page p, Report.Section s, float colW) {
        int n = s.getColumns().size();
        // header background bar
        p.content.append("0.114 0.306 0.847 rg ").append(fmt(MARGIN)).append(' ').append(fmt(p.y - 3)).append(' ')
                .append(fmt(PAGE_W - 2 * MARGIN)).append(' ').append(fmt(LINE)).append(" re f 0 0 0 rg\n");
        for (int i = 0; i < n; i++) {
            p.content.append("1 1 1 rg\n");
            text(p, MARGIN + i * colW + 2, BODY_SIZE, true, fit(s.getColumns().get(i), colW - 4), false);
            p.content.append("0 0 0 rg\n");
        }
        p.y -= LINE;
        return p;
    }

    private Page ensure(List<Page> pages, Page p, float needed) {
        return p.y - needed < MARGIN ? newPage(pages) : p;
    }

    private Page newPage(List<Page> pages) {
        Page p = new Page();
        pages.add(p);
        return p;
    }

    private void text(Page p, float x, float size, boolean bold, String s) { text(p, x, size, bold, s, true); }

    private void text(Page p, float x, float size, boolean bold, String s, boolean advance) {
        p.content.append("BT /").append(bold ? "F2" : "F1").append(' ').append(fmt(size)).append(" Tf ")
                .append(fmt(x)).append(' ').append(fmt(p.y - size)).append(" Td (").append(escape(s)).append(") Tj ET\n");
        if (advance) p.y -= size + 3;
    }

    private static String fit(String s, float width) {
        if (s == null) return "";
        int max = Math.max(1, (int) (width / (BODY_SIZE * CHAR_W)));
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 1)) + "\u2026";
    }

    private static String escape(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '(' || c == ')' || c == '\\') b.append('\\').append(c);
            else if (c == '\u2026') b.append("\\205");
            else if (c == '\u20A6') b.append("N");            // Naira sign not in WinAnsi
            else if (c < 32) b.append(' ');
            else if (c > 255) b.append('?');
            else if (c > 126) b.append(String.format("\\%03o", (int) c));
            else b.append(c);
        }
        return b.toString();
    }

    private static String fmt(float f) {
        String s = String.format(java.util.Locale.ROOT, "%.2f", f);
        return s.endsWith(".00") ? s.substring(0, s.length() - 3) : s;
    }

    // ---- PDF object assembly --------------------------------------------------------------------------

    private byte[] assemble(List<Page> pages) throws IOException {
        List<byte[]> objects = new ArrayList<>();
        // 1 catalog, 2 pages, 3 font regular, 4 font bold, then page+content pairs
        int pagesObj = 2;
        int firstPage = 5;
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) kids.append(firstPage + i * 2).append(" 0 R ");
        objects.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.ISO_8859_1));
        objects.add(("<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>").getBytes(StandardCharsets.ISO_8859_1));
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>".getBytes(StandardCharsets.ISO_8859_1));
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>".getBytes(StandardCharsets.ISO_8859_1));
        for (int i = 0; i < pages.size(); i++) {
            int contentObj = firstPage + i * 2 + 1;
            objects.add(("<< /Type /Page /Parent " + pagesObj + " 0 R /MediaBox [0 0 " + fmt(PAGE_W) + " " + fmt(PAGE_H) + "]"
                    + " /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + contentObj + " 0 R >>").getBytes(StandardCharsets.ISO_8859_1));
            byte[] stream = pages.get(i).content.toString().getBytes(StandardCharsets.ISO_8859_1);
            ByteArrayOutputStream c = new ByteArrayOutputStream();
            c.write(("<< /Length " + stream.length + " >>\nstream\n").getBytes(StandardCharsets.ISO_8859_1));
            c.write(stream);
            c.write("\nendstream".getBytes(StandardCharsets.ISO_8859_1));
            objects.add(c.toByteArray());
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n".getBytes(StandardCharsets.ISO_8859_1));
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.size());
            out.write(((i + 1) + " 0 obj\n").getBytes(StandardCharsets.ISO_8859_1));
            out.write(objects.get(i));
            out.write("\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        }
        int xref = out.size();
        StringBuilder x = new StringBuilder("xref\n0 ").append(objects.size() + 1).append("\n0000000000 65535 f \n");
        for (int off : offsets) x.append(String.format("%010d 00000 n \n", off));
        x.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        out.write(x.toString().getBytes(StandardCharsets.ISO_8859_1));
        return out.toByteArray();
    }
}
