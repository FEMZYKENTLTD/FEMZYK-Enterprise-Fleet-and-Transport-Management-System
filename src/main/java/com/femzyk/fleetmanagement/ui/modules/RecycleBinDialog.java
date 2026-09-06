package com.femzyk.fleetmanagement.ui.modules;

import com.femzyk.fleetmanagement.ui.Theme;
import com.femzyk.fleetmanagement.ui.UiUtils;
import com.femzyk.fleetmanagement.ui.components.EntityTableModel;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Generic recycle bin (kept from v4.x): restore or permanently delete soft-deleted records. */
public class RecycleBinDialog<T> extends JDialog {

    public RecycleBinDialog(Window owner, String title, List<EntityTableModel.Column<T>> columns, Supplier<List<T>> loader,
                            Consumer<T> restore, Consumer<T> purge) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        EntityTableModel<T> model = new EntityTableModel<>(columns);
        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Runnable reload = () -> model.setRows(loader.get());
        reload.run();
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(Theme.padding(14));
        root.add(UiUtils.muted("Deleted records are kept here until restored or permanently removed. Permanent removal is blocked while history exists."), BorderLayout.NORTH);
        root.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(UiUtils.success("Restore", () -> {
            int r = table.getSelectedRow();
            if (r < 0) { UiUtils.info(this, "No selection", "Select a record first."); return; }
            if (UiUtils.run(this, () -> restore.accept(model.getRow(table.convertRowIndexToModel(r))))) reload.run();
        }));
        if (purge != null) buttons.add(UiUtils.danger("Delete permanently", () -> {
            int r = table.getSelectedRow();
            if (r < 0) { UiUtils.info(this, "No selection", "Select a record first."); return; }
            if (UiUtils.confirm(this, "Permanent delete", "This cannot be undone. Continue?")
                    && UiUtils.run(this, () -> purge.accept(model.getRow(table.convertRowIndexToModel(r))))) reload.run();
        }));
        buttons.add(UiUtils.neutral("Close", this::dispose));
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        setSize(760, 420);
        setLocationRelativeTo(owner);
    }
}
