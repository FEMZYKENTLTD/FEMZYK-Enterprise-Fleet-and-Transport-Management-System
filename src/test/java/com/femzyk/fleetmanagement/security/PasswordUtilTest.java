package com.femzyk.fleetmanagement.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test void hashVerifiesAndSaltsDiffer() {
        String s1 = PasswordUtil.generateSalt(), s2 = PasswordUtil.generateSalt();
        assertNotEquals(s1, s2);
        String h = PasswordUtil.hashPassword("Secret123".toCharArray(), s1);
        assertTrue(PasswordUtil.verifyPassword("Secret123".toCharArray(), s1, h));
        assertFalse(PasswordUtil.verifyPassword("Secret124".toCharArray(), s1, h));
        assertNotEquals(h, PasswordUtil.hashPassword("Secret123".toCharArray(), s2));
    }

    @Test void legacyHashFromV4IsStillVerifiable() {
        // Same PBKDF2WithHmacSHA256 / 65,536 iterations / 256-bit parameters as the v4.x PasswordUtil,
        // so a hash computed with the legacy parameters must verify here (this guards the .dat migration).
        String salt = java.util.Base64.getEncoder().encodeToString(new byte[16]);
        String legacyHash = legacyHash("Admin@2024".toCharArray(), salt);
        assertTrue(PasswordUtil.verifyPassword("Admin@2024".toCharArray(), salt, legacyHash));
    }

    private static String legacyHash(char[] pw, String salt) {
        try {
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(pw, java.util.Base64.getDecoder().decode(salt), 65_536, 256);
            byte[] hash = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    @Test void policy() {
        assertNotNull(PasswordUtil.checkPolicy("short1".toCharArray()));
        assertNotNull(PasswordUtil.checkPolicy("nodigitsatall".toCharArray()));
        assertNotNull(PasswordUtil.checkPolicy("12345678".toCharArray()));
        assertNull(PasswordUtil.checkPolicy("GoodPass1".toCharArray()));
    }
}
