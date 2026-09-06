package com.femzyk.fleetmanagement.ui;

import com.femzyk.fleetmanagement.TestSupport;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import com.femzyk.fleetmanagement.ui.modules.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Constructs every module panel against a demo-data database and paints it offscreen.
 * This runs without a display (java.awt.headless) and catches layout/renderer exceptions;
 * the PNGs are written to target/screenshots for the documentation.
 */
class HeadlessRenderTest {

    static ServiceRegistry s;

    @BeforeAll
    static void setup() {
        System.setProperty("java.awt.headless", "true");
        Theme.install();
        s = TestSupport.freshRegistryLoggedInAsAdmin();
        s.demoData.load();
    }

    @Test
    void allModulePanelsRenderOffscreen() throws Exception {
        List<ModulePanel> panels = List.of(new DashboardPanel(s), new EmployeesPanel(s), new DriversPanel(s), new VehiclesPanel(s),
                new AssignmentsPanel(s), new TripsPanel(s), new MaintenancePanel(s), new FuelPanel(s), new ExpensesPanel(s),
                new ReportsPanel(s), new AuditLogPanel(s), new UsersPanel(s), new SystemPanel(s, () -> {}));
        Path dir = Path.of("target", "screenshots");
        Files.createDirectories(dir);
        for (ModulePanel p : panels) {
            SwingUtilities.invokeAndWait(() -> {
                p.refresh();
                JComponent c = p.component();
                c.setSize(1140, 760);
                layoutTree(c);
                BufferedImage img = new BufferedImage(1140, 760, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                c.print(g);
                g.dispose();
                try { ImageIO.write(img, "png", dir.resolve(p.id() + ".png").toFile()); } catch (Exception e) { throw new RuntimeException(e); }
            });
            assertTrue(Files.size(dir.resolve(p.id() + ".png")) > 1000, p.id());
        }
    }

    private static void layoutTree(Component c) {
        c.doLayout();
        if (c instanceof Container ct) for (Component ch : ct.getComponents()) layoutTree(ch);
        c.validate();
    }
}
