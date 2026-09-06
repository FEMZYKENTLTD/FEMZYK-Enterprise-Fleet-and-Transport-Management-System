package com.femzyk.fleetmanagement.ui.components;

import com.femzyk.fleetmanagement.ui.Theme;
import com.femzyk.fleetmanagement.ui.UiUtils;

import javax.swing.*;
import java.awt.*;

/** Modal OK/Cancel dialog around a FormBuilder. The save callback may throw domain exceptions; the dialog stays open. */
public class FormDialog extends JDialog {

    private boolean saved;

    public interface Save { void run() throws Exception; }

    public FormDialog(Window owner, String title, FormBuilder form, String okText, Save onSave) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBorder(Theme.padding(14));
        JScrollPane scroll = new JScrollPane(form.panel());
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        content.add(scroll, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = UiUtils.primary(okText, () -> {
            try { onSave.run(); saved = true; dispose(); }
            catch (Exception e) { UiUtils.showError(this, e); }
        });
        JButton cancel = UiUtils.neutral("Cancel", this::dispose);
        buttons.add(cancel); buttons.add(ok);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        getRootPane().setDefaultButton(ok);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        Dimension d = getSize();
        setSize(Math.max(d.width, 520), Math.min(Math.max(d.height, 200), 720));
        setLocationRelativeTo(owner);
    }

    /** Shows the dialog and returns true if the save callback completed. */
    public boolean showDialog() { setVisible(true); return saved; }
}
