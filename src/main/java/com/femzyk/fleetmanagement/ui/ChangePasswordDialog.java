package com.femzyk.fleetmanagement.ui;

import com.femzyk.fleetmanagement.service.ServiceRegistry;
import com.femzyk.fleetmanagement.ui.components.FormBuilder;
import com.femzyk.fleetmanagement.ui.components.FormDialog;

import javax.swing.*;
import java.awt.*;

public final class ChangePasswordDialog {

    private ChangePasswordDialog() {}

    public static boolean show(Window owner, ServiceRegistry services, boolean forced) {
        FormBuilder f = new FormBuilder();
        if (forced) f.addFull(UiUtils.muted("You must choose a new password before continuing."));
        JPasswordField cur = f.password("Current password");
        JPasswordField p1 = f.password("New password");
        JPasswordField p2 = f.password("Confirm new password");
        f.addFull(UiUtils.muted("Passwords need 8+ characters with letters and digits."));
        return new FormDialog(owner, "Change password", f, "Change", () -> {
            if (!String.valueOf(p1.getPassword()).equals(String.valueOf(p2.getPassword()))) throw new IllegalArgumentException("New passwords do not match.");
            services.auth.changeOwnPassword(cur.getPassword(), p1.getPassword());
        }).showDialog();
    }
}
