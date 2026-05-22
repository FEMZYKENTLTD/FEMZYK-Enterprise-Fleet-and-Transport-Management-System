package com.femzyk.vehiclesystem.gui;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * UserDatabase
 *
 * PURPOSE:
 * Manages all registered application users.
 *
 * STORAGE:
 * Users are saved locally in the user's home folder:
 *
 *     user-home/FemzykVehicleSystem/users.dat
 *
 * On Windows, this will be similar to:
 *
 *     C:/Users/YourWindowsName/FemzykVehicleSystem/users.dat
 *
 * IMPORTANT JAVA NOTE:
 * Do not write Windows paths with single backslashes inside Java comments,
 * such as C:\Users, because Java may interpret backslash-u as a Unicode escape.
 *
 * RESPONSIBILITIES:
 * - Load saved users
 * - Save registered users
 * - Create new users
 * - Validate usernames
 * - Validate email addresses
 * - Prevent duplicate usernames
 * - Prevent duplicate emails
 * - Authenticate login attempts
 * - Reset forgotten passwords
 *
 * SECURITY:
 * Passwords are never stored as plain text. PasswordUtil hashes passwords
 * using PBKDF2WithHmacSHA256.
 *
 * DESIGN NOTE:
 * This class manages account data only. It does not display GUI components.
 * LoginDialog and PasswordResetDialog handle the user interface.
 */
public class UserDatabase {

    private static final String APP_FOLDER = "FemzykVehicleSystem";
    private static final String USERS_FILE = "users.dat";

    /*
     * Email validation pattern.
     *
     * This is intentionally practical rather than overly complex.
     * It accepts normal emails such as:
     * - femi@gmail.com
     * - student@uopeople.edu
     * - admin@company.org
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private final Path appDirectory;
    private final Path usersFile;

    /*
     * The key is normalized lowercase username.
     * This makes usernames case-insensitive.
     */
    private Map<String, UserAccount> users;

    public UserDatabase() {
        this.appDirectory = Paths.get(System.getProperty("user.home"), APP_FOLDER);
        this.usersFile = appDirectory.resolve(USERS_FILE);

        createAppDirectory();
        loadUsers();
    }

    /**
     * Ensures the application data folder exists.
     */
    private void createAppDirectory() {
        try {
            Files.createDirectories(appDirectory);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Unable to create application data folder: " + appDirectory, e
            );
        }
    }

    /**
     * Loads the users.dat file if it exists.
     *
     * If it does not exist, an empty user database is created.
     * If it is corrupt, the corrupt file is backed up and a fresh database starts.
     */
    @SuppressWarnings("unchecked")
    private void loadUsers() {
        if (!Files.exists(usersFile)) {
            users = new LinkedHashMap<>();
            return;
        }

        try (ObjectInputStream input =
                 new ObjectInputStream(Files.newInputStream(usersFile))) {

            Object object = input.readObject();

            if (object instanceof Map<?, ?> loadedMap) {
                users = (Map<String, UserAccount>) loadedMap;
            } else {
                users = new LinkedHashMap<>();
            }

        } catch (Exception e) {
            backupCorruptUsersFile();
            users = new LinkedHashMap<>();
        }
    }

    /**
     * Saves all users to users.dat.
     */
    private void saveUsers() {
        try {
            Files.createDirectories(appDirectory);

            try (ObjectOutputStream output =
                     new ObjectOutputStream(Files.newOutputStream(usersFile))) {
                output.writeObject(users);
            }

        } catch (IOException e) {
            throw new IllegalStateException("Unable to save users database.", e);
        }
    }

    /**
     * Creates a backup of a corrupt users.dat file.
     */
    private void backupCorruptUsersFile() {
        try {
            if (Files.exists(usersFile)) {
                Path backup = appDirectory.resolve(
                    "users-corrupt-" + System.currentTimeMillis() + ".dat"
                );
                Files.move(usersFile, backup);
            }
        } catch (IOException ignored) {
            // If backup fails, the application still starts with a new user database.
        }
    }

    /**
     * Creates a new user account.
     *
     * @param username selected username
     * @param email    recovery email address
     * @param password selected password
     */
    public void createUser(String username, String email, char[] password) {
        validateUsername(username);
        validateEmail(email);
        validatePassword(password);

        String usernameKey = normalizeUsername(username);
        String emailKey = normalizeEmail(email);

        if (users.containsKey(usernameKey)) {
            throw new IllegalArgumentException(
                "Username already exists. Please choose another username."
            );
        }

        if (emailExists(emailKey)) {
            throw new IllegalArgumentException(
                "Email address is already registered to another account."
            );
        }

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);

        UserAccount account = new UserAccount(
            username.trim(),
            emailKey,
            salt,
            hash
        );

        users.put(usernameKey, account);
        saveUsers();
    }

    /**
     * Authenticates a login attempt.
     *
     * @param username entered username
     * @param password entered password
     * @return authenticated user account
     */
    public UserAccount authenticate(String username, char[] password) {
        validateUsername(username);
        validatePassword(password);

        String usernameKey = normalizeUsername(username);

        UserAccount account = users.get(usernameKey);

        if (account == null) {
            throw new IllegalArgumentException("Username does not exist.");
        }

        boolean validPassword = PasswordUtil.verifyPassword(
            password,
            account.getPasswordSalt(),
            account.getPasswordHash()
        );

        if (!validPassword) {
            throw new IllegalArgumentException("Incorrect password.");
        }

        account.markLoginSuccessful();
        saveUsers();

        return account;
    }

    /**
     * Resets a user's password after verifying username and registered email.
     *
     * This is the local desktop version of account recovery.
     * The app does not send an email. Instead, it checks that the user knows
     * the registered email address.
     *
     * @param username    username
     * @param email       registered email
     * @param newPassword new password
     */
    public void resetPassword(String username, String email, char[] newPassword) {
        validateUsername(username);
        validateEmail(email);
        validatePassword(newPassword);

        String usernameKey = normalizeUsername(username);
        String emailKey = normalizeEmail(email);

        UserAccount account = users.get(usernameKey);

        if (account == null) {
            throw new IllegalArgumentException("Username does not exist.");
        }

        if (account.getEmail() == null ||
            !account.getEmail().equalsIgnoreCase(emailKey)) {

            throw new IllegalArgumentException(
                "The email address does not match this account."
            );
        }

        String newSalt = PasswordUtil.generateSalt();
        String newHash = PasswordUtil.hashPassword(newPassword, newSalt);

        account.resetPassword(newSalt, newHash);
        saveUsers();
    }

    public boolean usernameExists(String username) {
        if (username == null) {
            return false;
        }

        return users.containsKey(normalizeUsername(username));
    }

    public boolean emailExists(String email) {
        if (email == null) {
            return false;
        }

        String normalizedEmail = normalizeEmail(email);

        for (UserAccount account : users.values()) {
            if (account.getEmail() != null &&
                account.getEmail().equalsIgnoreCase(normalizedEmail)) {
                return true;
            }
        }

        return false;
    }

    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }

        if (!username.trim().matches("[a-zA-Z0-9._-]{3,30}")) {
            throw new IllegalArgumentException(
                "Username must be 3-30 characters and may contain letters, " +
                "numbers, dot, underscore, or hyphen."
            );
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address cannot be empty.");
        }

        String normalizedEmail = normalizeEmail(email);

        if (normalizedEmail.contains(" ")) {
            throw new IllegalArgumentException("Email address cannot contain spaces.");
        }

        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException(
                "Invalid email address. Example: user@example.com"
            );
        }
    }

    private void validatePassword(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        if (password.length < 4) {
            throw new IllegalArgumentException(
                "Password must be at least 4 characters long."
            );
        }
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    public Path getUsersFile() {
        return usersFile;
    }
}