package com.femzyk.vehiclesystem.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;

/**
 * PasswordResetDialog
 *
 * PURPOSE:
 * Allows a user to reset their password if they know:
 * - their username
 * - their registered email address
 *
 * WHY EMAIL IS USED:
 * This is a local desktop application, so it does not send real recovery
 * emails. Instead, the registered email acts as the recovery identity.
 *
 * SECURITY:
 * The old password is never displayed or recovered.
 * The user creates a new password, which is hashed and saved.
 */
public class PasswordResetDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final UserDatabase userDatabase;

    private JTextField usernameField;
    private JTextField emailField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;
    private JCheckBox showPasswordCheckBox;
    private JLabel messageLabel;

    private char newPasswordEchoChar;
    private char confirmPasswordEchoChar;

    public PasswordResetDialog(Window parent, UserDatabase userDatabase) {
        super(parent, "Reset Password", ModalityType.APPLICATION_MODAL);

        this.userDatabase = userDatabase;

        setSize(500, 390);
        setLocationRelativeTo(parent);
        setResizable(false);

        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ThemeConstants.BG_CARD);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildForm(), BorderLayout.CENTER);
        root.add(buildButtons(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeConstants.BTN_NEUTRAL);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel title = new JLabel("Account Recovery");
        title.setFont(ThemeConstants.FONT_SUBHEADING);
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Reset password using username and registered email");
        subtitle.setFont(ThemeConstants.FONT_SMALL);
        subtitle.setForeground(Color.WHITE);

        header.add(title, BorderLayout.WEST);
        header.add(subtitle, BorderLayout.EAST);

        return header;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(ThemeConstants.BG_CARD);
        form.setBorder(new EmptyBorder(22, 30, 10, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(7, 4, 7, 4);

        usernameField = createTextField();
        emailField = createTextField();
        newPasswordField = createPasswordField();
        confirmPasswordField = createPasswordField();

        newPasswordEchoChar = newPasswordField.getEchoChar();
        confirmPasswordEchoChar = confirmPasswordField.getEchoChar();

        addFormRow(form, gbc, 0, "Username:", usernameField);
        addFormRow(form, gbc, 1, "Registered Email:", emailField);
        addFormRow(form, gbc, 2, "New Password:", newPasswordField);
        addFormRow(form, gbc, 3, "Confirm Password:", confirmPasswordField);

        showPasswordCheckBox = createCheckBox("Show passwords");
        showPasswordCheckBox.addActionListener(e -> togglePasswordVisibility());

        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        form.add(showPasswordCheckBox, gbc);

        messageLabel = new JLabel(" ");
        messageLabel.setFont(ThemeConstants.FONT_SMALL);
        messageLabel.setForeground(ThemeConstants.TEXT_ERROR);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        form.add(messageLabel, gbc);

        return form;
    }

    private JPanel buildButtons() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        buttons.setBackground(ThemeConstants.BG_CARD);
        buttons.setBorder(new EmptyBorder(0, 14, 10, 14));

        JButton cancelBtn = createButton("Cancel", ThemeConstants.BTN_NEUTRAL, 110);
        JButton resetBtn = createButton("Reset Password", ThemeConstants.ACCENT_CAR, 155);

        cancelBtn.addActionListener(e -> dispose());
        resetBtn.addActionListener(e -> handleResetPassword());

        buttons.add(cancelBtn);
        buttons.add(resetBtn);

        return buttons;
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc,
                            int row, String labelText, JComponent field) {

        JLabel label = new JLabel(labelText);
        label.setFont(ThemeConstants.FONT_BODY);
        label.setForeground(ThemeConstants.TEXT_SECONDARY);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0.38;
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.62;
        panel.add(field, gbc);
    }

    private JTextField createTextField() {
        JTextField field = new JTextField();

        field.setFont(ThemeConstants.FONT_BODY);
        field.setBackground(ThemeConstants.BG_INPUT);
        field.setForeground(ThemeConstants.TEXT_PRIMARY);
        field.setCaretColor(ThemeConstants.TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeConstants.BORDER_CARD),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        return field;
    }

    private JPasswordField createPasswordField() {
        JPasswordField field = new JPasswordField();

        field.setFont(ThemeConstants.FONT_BODY);
        field.setBackground(ThemeConstants.BG_INPUT);
        field.setForeground(ThemeConstants.TEXT_PRIMARY);
        field.setCaretColor(ThemeConstants.TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeConstants.BORDER_CARD),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        return field;
    }

    private JCheckBox createCheckBox(String text) {
        JCheckBox checkBox = new JCheckBox(text);

        checkBox.setFont(ThemeConstants.FONT_SMALL);
        checkBox.setForeground(ThemeConstants.TEXT_SECONDARY);
        checkBox.setBackground(ThemeConstants.BG_CARD);
        checkBox.setFocusPainted(false);

        return checkBox;
    }

    private JButton createButton(String text, Color color, int width) {
        JButton btn = new JButton(text);

        btn.setFont(ThemeConstants.FONT_BUTTON);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(width, ThemeConstants.BTN_HEIGHT));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        return btn;
    }

    private void togglePasswordVisibility() {
        boolean show = showPasswordCheckBox.isSelected();

        newPasswordField.setEchoChar(show ? (char) 0 : newPasswordEchoChar);
        confirmPasswordField.setEchoChar(show ? (char) 0 : confirmPasswordEchoChar);
    }

    private void handleResetPassword() {
        messageLabel.setText(" ");

        char[] newPassword = newPasswordField.getPassword();
        char[] confirmPassword = confirmPasswordField.getPassword();

        try {
            if (!Arrays.equals(newPassword, confirmPassword)) {
                throw new IllegalArgumentException("Passwords do not match.");
            }

            userDatabase.resetPassword(
                usernameField.getText(),
                emailField.getText(),
                newPassword
            );

            JOptionPane.showMessageDialog(
                this,
                "Password reset successfully. You can now login with your new password.",
                "Password Reset Complete",
                JOptionPane.INFORMATION_MESSAGE
            );

            dispose();

        } catch (Exception e) {
            messageLabel.setText("Error: " + e.getMessage());

        } finally {
            Arrays.fill(newPassword, '\0');
            Arrays.fill(confirmPassword, '\0');

            newPasswordField.setText("");
            confirmPasswordField.setText("");
        }
    }
}