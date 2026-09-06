package com.femzyk.fleetmanagement.ui.components;

import com.femzyk.fleetmanagement.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.util.LinkedHashMap;
import java.util.Map;

/** Donut chart with legend drawn with Java2D. */
public class PieChartPanel extends JPanel {

    private Map<String, Double> data = new LinkedHashMap<>();
    private final String title;
    private java.util.function.Function<String, Color> colorFor;

    public PieChartPanel(String title) { this(title, null); }

    public PieChartPanel(String title, java.util.function.Function<String, Color> colorFor) {
        this.title = title;
        this.colorFor = colorFor;
        setBackground(Theme.CARD);
        setBorder(Theme.cardBorder());
        setPreferredSize(new Dimension(380, 240));
    }

    public void setData(Map<String, Double> data) { this.data = data == null ? new LinkedHashMap<>() : data; repaint(); }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Insets in = getInsets();
        int w = getWidth() - in.left - in.right, h = getHeight() - in.top - in.bottom;
        g.translate(in.left, in.top);
        g.setColor(Theme.TEXT); g.setFont(Theme.FONT_SUBHEADING);
        g.drawString(title, 0, 14);
        double total = data.values().stream().mapToDouble(Double::doubleValue).sum();
        if (data.isEmpty() || total <= 0) { g.setColor(Theme.TEXT_MUTED); g.setFont(Theme.FONT_BODY); g.drawString("No data yet", w / 2 - 30, h / 2); g.dispose(); return; }
        int size = Math.min(w / 2, h - 30) - 10;
        int cx = 10, cy = 30;
        double start = 90;
        int i = 0;
        for (Map.Entry<String, Double> e : data.entrySet()) {
            double extent = -360.0 * e.getValue() / total;
            g.setColor(color(e.getKey(), i));
            g.fill(new Arc2D.Double(cx, cy, size, size, start, extent, Arc2D.PIE));
            start += extent;
            i++;
        }
        g.setColor(Theme.CARD);
        g.fillOval(cx + size / 4, cy + size / 4, size / 2, size / 2);
        g.setColor(Theme.TEXT); g.setFont(Theme.FONT_SUBHEADING);
        String tot = String.format("%,.0f", total);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(tot, cx + size / 2 - fm.stringWidth(tot) / 2, cy + size / 2 + 5);
        // legend
        int lx = cx + size + 20, ly = 34;
        g.setFont(Theme.FONT_SMALL);
        i = 0;
        for (Map.Entry<String, Double> e : data.entrySet()) {
            if (ly > h - 6) break;
            g.setColor(color(e.getKey(), i));
            g.fillRoundRect(lx, ly - 9, 10, 10, 3, 3);
            g.setColor(Theme.TEXT);
            String pct = String.format("%.0f%%", 100 * e.getValue() / total);
            g.drawString(e.getKey().replace('_', ' ') + "  " + String.format("%,.0f", e.getValue()) + "  (" + pct + ")", lx + 16, ly);
            ly += 16;
            i++;
        }
        g.dispose();
    }

    private Color color(String key, int i) {
        if (colorFor != null) { Color c = colorFor.apply(key); if (c != null) return c; }
        return Theme.CHART_SERIES[i % Theme.CHART_SERIES.length];
    }
}
