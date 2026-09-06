package com.femzyk.fleetmanagement.security;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * PBKDF2WithHmacSHA256 password hashing. Retained from the original Vehicle Management System
 * (same algorithm, salt size and iteration count) so existing hashes remain verifiable.
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {}

    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hashPassword(char[] password, String salt) {
        PBEKeySpec spec = new PBEKeySpec(password, Base64.getDecoder().decode(salt), ITERATIONS, KEY_LENGTH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return Base64.getEncoder().encodeToString(factory.generateSecret(spec).getEncoded());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Password hashing unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }

    /** Constant-time comparison. */
    public static boolean verifyPassword(char[] entered, String salt, String expectedHash) {
        if (entered == null || salt == null || expectedHash == null) return false;
        byte[] a = hashPassword(entered, salt).getBytes();
        byte[] b = expectedHash.getBytes();
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
        Arrays.fill(a, (byte) 0);
        return diff == 0;
    }

    /** Minimum policy: 8+ characters with at least one letter and one digit. */
    public static String checkPolicy(char[] password) {
        if (password == null || password.length < 8) return "Password must be at least 8 characters long.";
        boolean letter = false, digit = false;
        for (char c : password) {
            if (Character.isLetter(c)) letter = true;
            if (Character.isDigit(c)) digit = true;
        }
        if (!letter || !digit) return "Password must contain at least one letter and one digit.";
        return null;
    }
}
