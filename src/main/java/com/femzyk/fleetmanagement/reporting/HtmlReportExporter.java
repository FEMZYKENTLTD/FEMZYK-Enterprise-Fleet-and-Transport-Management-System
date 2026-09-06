package com.femzyk.fleetmanagement.reporting;

import com.femzyk.fleetmanagement.config.AppConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Self-contained printable HTML (open in any browser and "Print to PDF" if a nicer layout is wanted). */
public class HtmlReportExporter implements ReportExporter {

    @Override public String fileExtension() { return "html"; }

    @Override
    public void export(Report r, Path target) throws IOException {
        Files.createDirectories(target.toAbsolutePath().getParent());
        Files.writeString(target, render(r), StandardCharsets.UTF_8);
    }

    public String render(Report r) {
        StringBuilder b = new StringBuilder();
        b.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>").append(esc(r.getTitle())).append("</title><style>")
         .append("body{font-family:Segoe UI,Arial,sans-serif;margin:32px;color:#1f2937}h1{color:#1d4ed8;margin-bottom:0}")
         .append(".sub{color:#6b7280;margin-top:4px}table{border-collapse:collapse;width:100%;margin:12px 0 24px}")
         .append("th{background:#1d4ed8;color:#fff;text-align:left;padding:6px 8px;font-size:13px}")
         .append("td{border-bottom:1px solid #e5e7eb;padding:5px 8px;font-size:13px}tr:nth-child(even) td{background:#f9fafb}")
         .append(".summary td:first-child{font-weight:600;width:280px}.note{color:#6b7280;font-size:12px}")
         .append("footer{margin-top:32px;font-size:11px;color:#9ca3af;border-top:1px solid #e5e7eb;padding-top:8px}")
         .append("@media print{body{margin:12mm}}</style></head><body>");
        b.append("<h1>").append(esc(r.getTitle())).append("</h1>");
        b.append("<div class='sub'>").append(esc(AppConfig.APP_NAME));
        if (r.getSubtitle() != null) b.append(" &middot; ").append(esc(r.getSubtitle()));
        b.append(" &middot; Generated ").append(esc(r.getGeneratedAt().withNano(0).toString().replace('T', ' ')))
         .append(" by ").append(esc(r.getGeneratedBy())).append("</div>");
        if (!r.getSummary().isEmpty()) {
            b.append("<h2>Summary</h2><table class='summary'>");
            r.getSummary().forEach((k, v) -> b.append("<tr><td>").append(esc(k)).append("</td><td>").append(esc(v)).append("</td></tr>"));
            b.append("</table>");
        }
        for (Report.Section s : r.getSections()) {
            b.append("<h2>").append(esc(s.getTitle())).append("</h2><table><thead><tr>");
            for (String c : s.getColumns()) b.append("<th>").append(esc(c)).append("</th>");
            b.append("</tr></thead><tbody>");
            if (s.getRows().isEmpty()) b.append("<tr><td colspan='").append(s.getColumns().size()).append("'><i>No records</i></td></tr>");
            for (var row : s.getRows()) {
                b.append("<tr>");
                for (String c : row) b.append("<td>").append(esc(c)).append("</td>");
                b.append("</tr>");
            }
            b.append("</tbody></table>");
        }
        for (String n : r.getNotes()) b.append("<p class='note'>").append(esc(n)).append("</p>");
        b.append("<footer>").append(esc(AppConfig.APP_NAME)).append(" v").append(esc(AppConfig.APP_VERSION)).append("</footer></body></html>");
        return b.toString();
    }

    static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
