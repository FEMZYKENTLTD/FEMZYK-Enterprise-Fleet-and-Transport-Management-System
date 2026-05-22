package com.femzyk.vehiclesystem.gui;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * PasswordUtil
 *
 * PURPOSE:
 * Provides password hashing and password verification services.
 *
 * WHY THIS CLASS EXISTS:
 * Password-related logic should not be mixed into GUI code or user database
 * code. Separating it into this utility class improves readability and keeps
 * security logic centralized.
 *
 * SECURITY DESIGN:
 * Passwords are hashed using PBKDF2WithHmacSHA256 with a random salt.
 * This is much safer than storing plain text passwords.
 *
 * IMPORTANT:
 * This is suitable for an academic desktop application. A real enterprise
 * application would also include password reset, account locking, audit logs,
 * and possibly stronger authentication controls.
 */
public final class PasswordUtil {

    /** Number of PBKDF2 iterations. Higher values make brute-force attacks harder. */
    private static final int ITERATIONS = 65_536;

    /** Length of generated password hash in bits. */
    private static final int KEY_LENGTH = 256;

    /** Length of generated random salt in bytes. */
    private static final int SALT_BYTES = 16;

    private PasswordUtil() {
        // Utility class; prevent instantiation.
    }

    /**
     * Generates a random salt for password hashing.
     *
     * @return Base64 encoded salt string
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashes a password using PBKDF2WithHmacSHA256.
     *
     * @param password user-entered password as char array
     * @param salt     Base64 encoded salt string
     * @return Base64 encoded password hash
     */
    public static String hashPassword(char[] password, String salt) {
        try {
            byte[] saltBytes = Base64.getDecoder().decode(salt);

            KeySpec spec = new PBEKeySpec(password, saltBytes, ITERATIONS, KEY_LENGTH);

            SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            byte[] hash = factory.generateSecret(spec).getEncoded();

            return Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash password.", e);
        }
    }

    /**
     * Verifies whether an entered password matches the saved password hash.
     *
     * @param enteredPassword password entered during login
     * @param savedSalt       saved salt from user account
     * @param savedHash       saved hash from user account
     * @return true if password is correct; otherwise false
     */
    public static boolean verifyPassword(char[] enteredPassword,
                                         String savedSalt,
                                         String savedHash) {

        String enteredHash = hashPassword(enteredPassword, savedSalt);

        /*
         * MessageDigest.isEqual performs a constant-time comparison.
         * This is better than using plain String.equals for security-sensitive
         * comparisons.
         */
        return MessageDigest.isEqual(
            enteredHash.getBytes(StandardCharsets.UTF_8),
            savedHash.getBytes(StandardCharsets.UTF_8)
        );
    }
}