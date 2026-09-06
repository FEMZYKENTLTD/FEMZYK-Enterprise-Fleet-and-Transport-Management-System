package com.femzyk.fleetmanagement.ui;

import javax.swing.*;

/** A navigable screen inside the main window. */
public interface ModulePanel {
    String id();
    String title();
    JComponent component();
    /** Reload data from the database (called when the module is shown and after related changes). */
    void refresh();
}
