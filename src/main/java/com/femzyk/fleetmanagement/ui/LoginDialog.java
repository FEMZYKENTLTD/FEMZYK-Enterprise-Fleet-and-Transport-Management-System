package com.femzyk.fleetmanagement.ui;

import com.femzyk.fleetmanagement.config.AppConfig;
import com.femzyk.fleetmanagement.model.User;
import com.femzyk.fleetmanagement.service.ServiceRegistry;
import com.femzyk.fleetmanagement.ui.components.FormBuilder;
import com.femzyk.fleetmanagement.ui.components.FormDialog;

import javax.swing.*;
import java.awt.*;

/** Sign-in window. Evolved from the v4.x LoginDialog; password reset now needs username + registered email. */
public class LoginDialog extends JDialog {

    private final ServiceRegistry services;
    private final JTextField username = new JTextField(20);
    private final JPasswordField password = new JPasswordField(20);
    private final JLabel status = new JLabel(" ");
    private User result;

    public LoginDialog(ServiceRegistry services) {
        super((Frame) null, "Sign in - " + AppConfig.APP_SHORT_NAME, true);
        this.services = services;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        JPanel banner = new JPanel(new GridLayout(3, 1));
        banner.setBackground(Theme.SIDEBAR_BG);
        banner.setBorder(Theme.padding(24));
        JLabel brand = new JLabel("FEMZYK");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 30)); brand.setForeground(Color.WHITE);
        JLabel name = new JLabel("Enterprise Fleet & Transport Management System");
        name.setFont(Theme.FONT_SUBHEADING); name.setForeground(Theme.SIDEBAR_TEXT);
        JLabel ver = new JLabel("Version " + AppConfig.APP_VERSION + "  ·  LASU CSC 392 SIWES project");
        ver.setFont(Theme.FONT_SMALL); ver.setForeground(Theme.TEXT_MUTED);
        banner.add(brand); banner.add(name); banner.add(ver);
        root.add(banner, BorderLayout.NORTH);

        FormBuilder f = new FormBuilder();
        f.add("Username", username);
        f.add("Password", password);
        JPanel form = f.panel();
        form.setBorder(Theme.padding(20));
        root.add(form, BorderLayout.CENTER);

        status.setForeground(Theme.DANGER);
        status.setFont(Theme.FONT_SMALL);
        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createEmptyBorder(0, 20, 16, 20));
        south.add(status, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton forgot = UiUtils.neutral("Forgot password...", this::forgotPassword);
        JButton login = UiUtils.primary("Sign in", this::attemptLogin);
        buttons.add(forgot); buttons.add(login);
        south.add(buttons, BorderLayout.SOUTH);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(login);
        pack();
        setSize(Math.max(getWidth(), 480), getHeight());
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void attemptLogin() {
        try {
            result = services.auth.login(username.getText(), password.getPassword());
            dispose();
        } catch (RuntimeException e) {
            status.setText(e.getMessage());
            password.setText("");
            password.requestFocusInWindow();
        }
    }

    private void forgotPassword() {
        FormBuilder f = new FormBuilder();
        JTextField u = f.text("Username", username.getText());
        JTextField e = f.text("Registered email", "");
        JPasswordField p1 = f.password("New password");
        JPasswordField p2 = f.password("Confirm password");
        f.addFull(UiUtils.muted("Passwords need 8+ characters with letters and digits."));
        new FormDialog(this, "Reset password", f, "Reset", () -> {
            if (!String.valueOf(p1.getPassword()).equals(String.valueOf(p2.getPassword()))) throw new IllegalArgumentException("Passwords do not match.");
            services.auth.resetPasswordWithEmail(u.getText(), e.getText(), p1.getPassword());
            UiUtils.info(this, "Password reset", "Password updated. You can sign in now.");
        }).showDialog();
    }

    /** Shows the dialog and returns the signed-in user, or null if the window was closed. */
    public User showAndGetUser() {
        setVisible(true);
        return result;
    }
}
