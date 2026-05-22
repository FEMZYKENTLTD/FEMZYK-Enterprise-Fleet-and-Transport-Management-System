package com.femzyk.vehiclesystem.gui;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * UserAccount
 *
 * PURPOSE:
 * Represents one registered application user.
 *
 * STORED INFORMATION:
 * - username
 * - email address
 * - password salt
 * - password hash
 * - creation date
 * - last login date
 * - last password reset date
 *
 * SECURITY DESIGN:
 * The plain password is never stored. Only a salt and hash are stored.
 * Password hashing and verification are handled by PasswordUtil.
 *
 * ACCOUNT RECOVERY:
 * The email address is stored so the user can reset their password by
 * providing the correct username and registered email address.
 *
 * OOP PRINCIPLE:
 * Encapsulation is used here. Fields are private and only controlled methods
 * can update password and login metadata.
 */
public class UserAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Username chosen during signup. */
    private final String username;

    /** Email address used for account recovery. */
    private final String email;

    /** Random salt used to hash the password. */
    private String passwordSalt;

    /** Hashed password value. The original password is never saved. */
    private String passwordHash;

    /** Date/time when this account was created. */
    private final LocalDateTime createdAt;

    /** Date/time when this account last logged in successfully. */
    private LocalDateTime lastLoginAt;

    /** Date/time when this account last reset its password. */
    private LocalDateTime lastPasswordResetAt;

    /**
     * Constructs a new user account.
     *
     * @param username     username
     * @param email        recovery email address
     * @param passwordSalt generated password salt
     * @param passwordHash hashed password value
     */
    public UserAccount(String username,
                       String email,
                       String passwordSalt,
                       String passwordHash) {

        this.username = username;
        this.email = email;
        this.passwordSalt = passwordSalt;
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now();
        this.lastLoginAt = null;
        this.lastPasswordResetAt = null;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public LocalDateTime getLastPasswordResetAt() {
        return lastPasswordResetAt;
    }

    /**
     * Updates the last login timestamp after successful authentication.
     */
    public void markLoginSuccessful() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * Replaces the old password hash and salt with new values.
     *
     * This method is used by the password recovery/reset feature.
     *
     * @param newSalt new generated salt
     * @param newHash new hashed password
     */
    public void resetPassword(String newSalt, String newHash) {
        this.passwordSalt = newSalt;
        this.passwordHash = newHash;
        this.lastPasswordResetAt = LocalDateTime.now();
    }
}