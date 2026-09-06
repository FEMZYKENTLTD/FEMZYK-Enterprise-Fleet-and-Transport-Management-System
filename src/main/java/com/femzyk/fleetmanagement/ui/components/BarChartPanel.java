package com.femzyk.fleetmanagement.ui.components;

import com.femzyk.fleetmanagement.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** Simple vertical bar chart drawn with Java2D (no external charting library). */
public class BarChartPanel extends JPanel {

    private Map<String, Double> data = new LinkedHashMap<>();
    private final String title;
    private Function<Double, String> valueFormat = v -> String.format("%,.0f", v);
    private Color barColor = Theme.PRIMARY;

    public BarChartPanel(String title) {
        this.title = title;
        setBackground(Theme.CARD);
        setBorder(Theme.cardBorder());
        setPreferredSize(new Dimension(380, 240));
    }

    public void setData(Map<String, Double> data) { this.data = data == null ? new LinkedHashMap<>() : data; repaint(); }
    public void setValueFormat(Function<Double, String> f) { this.valueFormat = f; }
    public void setBarColor(Color c) { this.barColor = c; }

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
        if (data.isEmpty()) { g.setColor(Theme.TEXT_MUTED); g.setFont(Theme.FONT_BODY); g.drawString("No data yet", w / 2 - 30, h / 2); g.dispose(); return; }
        int top = 34, bottom = h - 34, left = 8, right = w - 8;
        double max = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        if (max <= 0) max = 1;
        int n = data.size();
        double slot = (double) (right - left) / n;
        int barW = (int) Math.max(6, Math.min(48, slot * 0.6));
        // gridlines
        g.setColor(Theme.BORDER);
        for (int i = 0; i <= 4; i++) { int y = bottom - (bottom - top) * i / 4; g.drawLine(left, y, right, y); }
        int i = 0;
        g.setFont(Theme.FONT_SMALL);
        for (Map.Entry<String, Double> e : data.entrySet()) {
            double v = Math.max(0, e.getValue());
            int bh = (int) Math.round((bottom - top) * v / max);
            int x = (int) (left + slot * i + (slot - barW) / 2);
            g.setColor(barColor);
            g.fill(new Rectangle2D.Double(x, bottom - bh, barW, bh));
            g.setColor(Theme.TEXT_MUTED);
            String label = shorten(e.getKey(), (int) (slot / 6));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(label, x + barW / 2 - fm.stringWidth(label) / 2, bottom + 14);
            String val = valueFormat.apply(e.getValue());
            g.setColor(Theme.TEXT);
            if (fm.stringWidth(val) <= slot) g.drawString(val, x + barW / 2 - fm.stringWidth(val) / 2, bottom - bh - 4);
            i++;
        }
        g.dispose();
    }

    private static String shorten(String s, int max) { return s.length() <= Math.max(3, max) ? s : s.substring(0, Math.max(2, max - 1)) + "…"; }
}
