package com.femzyk.vehiclesystem.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;

/**
 * LoginDialog
 *
 * PURPOSE:
 * Provides the authentication screen shown before the main application opens.
 *
 * FEATURES:
 * - Login existing user
 * - Sign up new user
 * - Email validation during signup
 * - Show/hide password option
 * - Forgot password / password reset
 * - Exit application
 *
 * IMPORTANT GUI FIX:
 * The login screen originally placed too many buttons on one horizontal row.
 * On smaller dialog widths, the Login button could be pushed off-screen.
 * This version arranges login buttons into two rows so all buttons remain
 * visible and usable.
 */
public class LoginDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final UserDatabase userDatabase;

    /** If login succeeds, this stores the username that should enter MainWindow. */
    private String authenticatedUsername = null;

    // Login form components
    private JTextField loginUsernameField;
    private JPasswordField loginPasswordField;
    private JCheckBox loginShowPasswordCheckBox;
    private JLabel loginMessageLabel;
    private char loginPasswordEchoChar;

    // Signup form components
    private JTextField signupUsernameField;
    private JTextField signupEmailField;
    private JPasswordField signupPasswordField;
    private JPasswordField signupConfirmPasswordField;
    private JCheckBox signupShowPasswordCheckBox;
    private JLabel signupMessageLabel;
    private char signupPasswordEchoChar;
    private char signupConfirmPasswordEchoChar;

    /**
     * Creates the login/signup dialog.
     *
     * @param parent       parent frame; usually null at application startup
     * @param userDatabase user database used for login, signup, and reset
     */
    public LoginDialog(Frame parent, UserDatabase userDatabase) {
        super(parent, "Femzyk Vehicle System - Login", true);

        this.userDatabase = userDatabase;

        /*
         * Increased height slightly so the two-row button layout has enough room.
         */
        setSize(570, 520);
        setLocationRelativeTo(parent);
        setResizable(false);

        buildUI();
    }

    /**
     * Builds the root layout of the authentication dialog.
     */
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ThemeConstants.BG_PRIMARY);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildTabs(), BorderLayout.CENTER);

        setContentPane(root);
    }

    /**
     * Builds the branded header at the top of the login dialog.
     */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeConstants.BG_SECONDARY);
        header.setBorder(new EmptyBorder(20, 24, 18, 24));

        JLabel title = new JLabel("FEMZYK RENTAL FLEET MANAGER");
        title.setFont(ThemeConstants.FONT_HEADING);
        title.setForeground(ThemeConstants.TEXT_ACCENT);

        JLabel subtitle = new JLabel("Login or create an account to access your saved fleet");
        subtitle.setFont(ThemeConstants.FONT_BODY);
        subtitle.setForeground(ThemeConstants.TEXT_SECONDARY);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(6));
        textPanel.add(subtitle);

        header.add(textPanel, BorderLayout.WEST);

        return header;
    }

    /**
     * Builds the tabbed login/signup interface.
     */
    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(ThemeConstants.FONT_BUTTON);

        tabs.addTab("Login", buildLoginPanel(tabs));
        tabs.addTab("Sign Up", buildSignupPanel(tabs));

        return tabs;
    }

    /**
     * Builds the login panel.
     *
     * GUI FIX:
     * Login buttons are placed into two rows instead of one long row:
     *
     * Row 1: Login | Forgot Password
     * Row 2: Create Account | Exit
     *
     * This prevents the Login button from disappearing off-screen.
     */
    private JPanel buildLoginPanel(JTabbedPane tabs) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ThemeConstants.BG_CARD);
        panel.setBorder(new EmptyBorder(24, 36, 24, 36));

        GridBagConstraints gbc = createGbc();

        loginUsernameField = createTextField();
        loginPasswordField = createPasswordField();
        loginPasswordEchoChar = loginPasswordField.getEchoChar();

        addFormRow(panel, gbc, 0, "Username:", loginUsernameField);
        addFormRow(panel, gbc, 1, "Password:", loginPasswordField);

        loginShowPasswordCheckBox = createCheckBox("Show password");
        loginShowPasswordCheckBox.addActionListener(e -> toggleLoginPasswordVisibility());

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        panel.add(loginShowPasswordCheckBox, gbc);

        loginMessageLabel = createMessageLabel();

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(loginMessageLabel, gbc);

        /*
         * Button panel uses vertical layout so buttons cannot overflow.
         */
        JPanel buttonArea = new JPanel();
        buttonArea.setOpaque(false);
        buttonArea.setLayout(new BoxLayout(buttonArea, BoxLayout.Y_AXIS));

        JPanel rowOne = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        rowOne.setOpaque(false);

        JPanel rowTwo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        rowTwo.setOpaque(false);

        JButton loginBtn = createButton("Login", ThemeConstants.ACCENT_CAR, 130);
        JButton forgotBtn = createButton("Forgot Password", ThemeConstants.BTN_NEUTRAL, 170);
        JButton signupTabBtn = createButton("Create Account", ThemeConstants.ACCENT_MOTORCYCLE, 170);
        JButton exitBtn = createButton("Exit", ThemeConstants.BTN_NEUTRAL, 100);

        loginBtn.addActionListener(e -> handleLogin());
        forgotBtn.addActionListener(e -> openPasswordResetDialog());
        signupTabBtn.addActionListener(e -> tabs.setSelectedIndex(1));

        exitBtn.addActionListener(e -> {
            authenticatedUsername = null;
            dispose();
        });

        /*
         * Login is placed first and remains visible.
         */
        rowOne.add(loginBtn);
        rowOne.add(forgotBtn);

        rowTwo.add(signupTabBtn);
        rowTwo.add(exitBtn);

        buttonArea.add(rowOne);
        buttonArea.add(rowTwo);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(buttonArea, gbc);

        /*
         * Pressing Enter on the login page triggers Login.
         */
        getRootPane().setDefaultButton(loginBtn);

        return panel;
    }

    /**
     * Builds the signup panel.
     */
    private JPanel buildSignupPanel(JTabbedPane tabs) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ThemeConstants.BG_CARD);
        panel.setBorder(new EmptyBorder(24, 36, 24, 36));

        GridBagConstraints gbc = createGbc();

        signupUsernameField = createTextField();
        signupEmailField = createTextField();
        signupPasswordField = createPasswordField();
        signupConfirmPasswordField = createPasswordField();

        signupPasswordEchoChar = signupPasswordField.getEchoChar();
        signupConfirmPasswordEchoChar = signupConfirmPasswordField.getEchoChar();

        addFormRow(panel, gbc, 0, "Username:", signupUsernameField);
        addFormRow(panel, gbc, 1, "Email:", signupEmailField);
        addFormRow(panel, gbc, 2, "Password:", signupPasswordField);
        addFormRow(panel, gbc, 3, "Confirm Password:", signupConfirmPasswordField);

        signupShowPasswordCheckBox = createCheckBox("Show passwords");
        signupShowPasswordCheckBox.addActionListener(e -> toggleSignupPasswordVisibility());

        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        panel.add(signupShowPasswordCheckBox, gbc);

        signupMessageLabel = createMessageLabel();

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        panel.add(signupMessageLabel, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        buttons.setOpaque(false);

        JButton loginTabBtn = createButton("Back to Login", ThemeConstants.BTN_NEUTRAL, 140);
        JButton signupBtn = createButton("Sign Up", ThemeConstants.ACCENT_MOTORCYCLE, 130);

        loginTabBtn.addActionListener(e -> tabs.setSelectedIndex(0));
        signupBtn.addActionListener(e -> handleSignup(tabs));

        buttons.add(loginTabBtn);
        buttons.add(signupBtn);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        panel.add(buttons, gbc);

        return panel;
    }

    /**
     * Creates the default GridBagConstraints used by form rows.
     */
    private GridBagConstraints createGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 4, 8, 4);
        return gbc;
    }

    /**
     * Adds one label-field row to a GridBagLayout form.
     */
    private void addFormRow(JPanel panel, GridBagConstraints gbc,
                            int row, String labelText, JComponent field) {

        JLabel label = new JLabel(labelText);
        label.setFont(ThemeConstants.FONT_BODY);
        label.setForeground(ThemeConstants.TEXT_SECONDARY);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0.35;
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.65;
        panel.add(field, gbc);
    }

    /**
     * Creates a styled text field.
     */
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

    /**
     * Creates a styled password field.
     */
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

    /**
     * Creates a styled checkbox.
     */
    private JCheckBox createCheckBox(String text) {
        JCheckBox checkBox = new JCheckBox(text);

        checkBox.setFont(ThemeConstants.FONT_SMALL);
        checkBox.setForeground(ThemeConstants.TEXT_SECONDARY);
        checkBox.setBackground(ThemeConstants.BG_CARD);
        checkBox.setFocusPainted(false);

        return checkBox;
    }

    /**
     * Creates an error/status label.
     */
    private JLabel createMessageLabel() {
        JLabel label = new JLabel(" ");

        label.setFont(ThemeConstants.FONT_SMALL);
        label.setForeground(ThemeConstants.TEXT_ERROR);

        return label;
    }

    /**
     * Creates a styled button.
     */
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

    /**
     * Shows or hides the login password.
     */
    private void toggleLoginPasswordVisibility() {
        boolean show = loginShowPasswordCheckBox.isSelected();
        loginPasswordField.setEchoChar(show ? (char) 0 : loginPasswordEchoChar);
    }

    /**
     * Shows or hides both signup password fields.
     */
    private void toggleSignupPasswordVisibility() {
        boolean show = signupShowPasswordCheckBox.isSelected();

        signupPasswordField.setEchoChar(show ? (char) 0 : signupPasswordEchoChar);
        signupConfirmPasswordField.setEchoChar(show ? (char) 0 : signupConfirmPasswordEchoChar);
    }

    /**
     * Authenticates the user and closes the dialog if successful.
     */
    private void handleLogin() {
        loginMessageLabel.setText(" ");

        char[] password = loginPasswordField.getPassword();

        try {
            UserAccount account = userDatabase.authenticate(
                loginUsernameField.getText(),
                password
            );

            authenticatedUsername = account.getUsername();
            dispose();

        } catch (Exception e) {
            loginMessageLabel.setText("Error: " + e.getMessage());

        } finally {
            Arrays.fill(password, '\0');
            loginPasswordField.setText("");
        }
    }

    /**
     * Creates a new user account.
     */
    private void handleSignup(JTabbedPane tabs) {
        signupMessageLabel.setText(" ");

        char[] password = signupPasswordField.getPassword();
        char[] confirmPassword = signupConfirmPasswordField.getPassword();

        try {
            if (!Arrays.equals(password, confirmPassword)) {
                throw new IllegalArgumentException("Passwords do not match.");
            }

            userDatabase.createUser(
                signupUsernameField.getText(),
                signupEmailField.getText(),
                password
            );

            JOptionPane.showMessageDialog(
                this,
                "Account created successfully. Please log in.",
                "Signup Complete",
                JOptionPane.INFORMATION_MESSAGE
            );

            loginUsernameField.setText(signupUsernameField.getText().trim());

            signupUsernameField.setText("");
            signupEmailField.setText("");
            signupPasswordField.setText("");
            signupConfirmPasswordField.setText("");
            signupShowPasswordCheckBox.setSelected(false);
            toggleSignupPasswordVisibility();

            tabs.setSelectedIndex(0);

        } catch (Exception e) {
            signupMessageLabel.setText("Error: " + e.getMessage());

        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmPassword, '\0');
        }
    }

    /**
     * Opens the password reset dialog.
     */
    private void openPasswordResetDialog() {
        PasswordResetDialog dialog = new PasswordResetDialog(this, userDatabase);
        dialog.setVisible(true);
    }

    /**
     * Returns the authenticated username after successful login.
     *
     * @return username if login succeeded; otherwise null
     */
    public String getAuthenticatedUsername() {
        return authenticatedUsername;
    }
}