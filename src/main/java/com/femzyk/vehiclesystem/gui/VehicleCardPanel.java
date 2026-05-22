package com.femzyk.vehiclesystem.gui;

import com.femzyk.vehiclesystem.interfaces.Vehicle;
import com.femzyk.vehiclesystem.model.Car;
import com.femzyk.vehiclesystem.model.Motorcycle;
import com.femzyk.vehiclesystem.model.Truck;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * VehicleCardPanel
 *
 * PURPOSE:
 * Displays one vehicle as a styled card inside the fleet registry.
 *
 * FEATURES:
 * - Vehicle type badge
 * - Vehicle identity
 * - Vehicle-specific details
 * - Optional renter display
 * - Edit button
 * - Delete button
 *
 * DESIGN NOTE:
 * The card receives Runnable callbacks for edit/delete. This keeps the card
 * focused on presentation while FleetPanel controls the actual data changes.
 */
public class VehicleCardPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Vehicle vehicle;
    private final Color accentColor;
    private final Runnable editAction;
    private final Runnable deleteAction;

    private boolean hovered = false;

    public VehicleCardPanel(Vehicle vehicle) {
        this(vehicle, null, null);
    }

    public VehicleCardPanel(Vehicle vehicle,
                            Runnable editAction,
                            Runnable deleteAction) {

        this.vehicle = vehicle;
        this.editAction = editAction;
        this.deleteAction = deleteAction;

        this.accentColor = switch (vehicle.getVehicleType()) {
            case "Car" -> ThemeConstants.ACCENT_CAR;
            case "Motorcycle" -> ThemeConstants.ACCENT_MOTORCYCLE;
            case "Truck" -> ThemeConstants.ACCENT_TRUCK;
            default -> ThemeConstants.TEXT_ACCENT;
        };

        setOpaque(false);
        setPreferredSize(new Dimension(390, 245));
        setBorder(new EmptyBorder(14, 16, 14, 16));
        setLayout(new BorderLayout(0, 8));

        buildCard();
        addHoverEffect();
    }

    private void buildCard() {
        add(buildHeader(), BorderLayout.NORTH);
        add(buildDetailLabel(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        header.setOpaque(false);

        JLabel badge = new JLabel(" " + vehicle.getVehicleType().toUpperCase() + " ");
        badge.setFont(ThemeConstants.FONT_SMALL);
        badge.setForeground(Color.WHITE);
        badge.setBackground(accentColor);
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        JLabel identity = new JLabel(vehicle.getIdentityDisplay());
        identity.setFont(ThemeConstants.FONT_SUBHEADING);
        identity.setForeground(ThemeConstants.TEXT_PRIMARY);

        header.add(badge);
        header.add(identity);

        return header;
    }

    private JLabel buildDetailLabel() {
        String htmlDetail;

        if (vehicle instanceof Car car) {
            htmlDetail = car.toHtmlDetail();
        } else if (vehicle instanceof Motorcycle motorcycle) {
            htmlDetail = motorcycle.toHtmlDetail();
        } else if (vehicle instanceof Truck truck) {
            htmlDetail = truck.toHtmlDetail();
        } else {
            htmlDetail = "<html>" + vehicle.toString() + "</html>";
        }

        JLabel detail = new JLabel(htmlDetail);
        detail.setFont(ThemeConstants.FONT_BODY);
        detail.setForeground(ThemeConstants.TEXT_SECONDARY);
        detail.setVerticalAlignment(SwingConstants.TOP);

        return detail;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);

        JLabel keyAttr = new JLabel(vehicle.getKeyAttribute());
        keyAttr.setFont(ThemeConstants.FONT_SMALL);
        keyAttr.setForeground(accentColor);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actionPanel.setOpaque(false);

        JButton editBtn = createSmallButton("Edit", ThemeConstants.BTN_NEUTRAL);
        JButton deleteBtn = createSmallButton("Delete", ThemeConstants.BTN_DANGER);

        editBtn.addActionListener(e -> {
            if (editAction != null) {
                editAction.run();
            }
        });

        deleteBtn.addActionListener(e -> {
            if (deleteAction != null) {
                deleteAction.run();
            }
        });

        actionPanel.add(editBtn);
        actionPanel.add(deleteBtn);

        footer.add(keyAttr, BorderLayout.WEST);
        footer.add(actionPanel, BorderLayout.EAST);

        return footer;
    }

    private JButton createSmallButton(String text, Color bg) {
        JButton btn = new JButton(text);

        btn.setFont(ThemeConstants.FONT_SMALL);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(66, 24));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        return btn;
    }

    private void addHoverEffect() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        int w = getWidth();
        int h = getHeight();
        int arc = ThemeConstants.CORNER_RADIUS;

        Color bg = hovered ? ThemeConstants.BG_HOVER : ThemeConstants.BG_CARD;

        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Double(0, 0, w, h, arc, arc));

        g2.setColor(accentColor);
        g2.fillRoundRect(0, 0, 5, h, arc, arc);
        g2.fillRect(2, 0, 5, h);

        g2.setColor(hovered ? accentColor : ThemeConstants.BORDER_CARD);
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, arc, arc));

        g2.dispose();

        super.paintComponent(g);
    }

    public Vehicle getVehicle() {
        return vehicle;
    }
}