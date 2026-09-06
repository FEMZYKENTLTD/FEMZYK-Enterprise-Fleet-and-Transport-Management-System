package com.femzyk.fleetmanagement.ui.components;

import com.femzyk.fleetmanagement.ui.Theme;

import javax.swing.*;
import java.awt.*;

/** Dashboard KPI tile. */
public class StatCard extends JPanel {

    private final JLabel value = new JLabel("–");
    private final JLabel caption;

    public StatCard(String title, Color accent) {
        super(new BorderLayout(0, 4));
        setBackground(Theme.CARD);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER), BorderFactory.createEmptyBorder(12, 14, 12, 14))));
        JLabel t = new JLabel(title.toUpperCase());
        t.setFont(Theme.FONT_SMALL); t.setForeground(Theme.TEXT_MUTED);
        value.setFont(Theme.FONT_STAT); value.setForeground(Theme.TEXT);
        caption = new JLabel(" ");
        caption.setFont(Theme.FONT_SMALL); caption.setForeground(Theme.TEXT_MUTED);
        add(t, BorderLayout.NORTH);
        add(value, BorderLayout.CENTER);
        add(caption, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(200, 96));
    }

    public void set(Object v, String captionText) {
        value.setText(String.valueOf(v));
        caption.setText(captionText == null ? " " : captionText);
    }

    public void setValueColor(Color c) { value.setForeground(c); }
}
