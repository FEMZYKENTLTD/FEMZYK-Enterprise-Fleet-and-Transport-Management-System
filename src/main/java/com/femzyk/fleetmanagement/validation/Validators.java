package com.femzyk.fleetmanagement.validation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.femzyk.fleetmanagement.exception.ValidationException;

/**
 * Field-level validation rules shared by all services (and reused by the UI for early feedback).
 * Phone/email rules are adapted from the Contact Book project's validator and the original system's
 * e-mail pattern.
 */
public final class Validators {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_DIGITS = Pattern.compile("^\\d{7,15}$");
    private static final Pattern REGISTRATION = Pattern.compile("^[A-Z0-9][A-Z0-9 -]{2,14}$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]{3,32}$");

    private Validators() {}

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String cleaned = phone.replaceAll("[\\s()+-]", "");
        return PHONE_DIGITS.matcher(cleaned).matches();
    }

    public static boolean isValidRegistration(String reg) {
        return reg != null && REGISTRATION.matcher(reg.trim().toUpperCase()).matches();
    }

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME.matcher(username.trim()).matches();
    }

    public static String normaliseRegistration(String reg) {
        return reg == null ? null : reg.trim().toUpperCase().replaceAll("\\s+", " ");
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Fluent collector so a service can report every problem at once. */
    public static final class Errors {
        private final List<String> messages = new ArrayList<>();

        public Errors required(String value, String label) {
            if (isBlank(value)) messages.add(label + " is required.");
            return this;
        }

        public Errors required(Object value, String label) {
            if (value == null) messages.add(label + " is required.");
            return this;
        }

        public Errors email(String value, String label, boolean required) {
            if (isBlank(value)) { if (required) messages.add(label + " is required."); return this; }
            if (!isValidEmail(value)) messages.add(label + " is not a valid e-mail address.");
            return this;
        }

        public Errors phone(String value, String label, boolean required) {
            if (isBlank(value)) { if (required) messages.add(label + " is required."); return this; }
            if (!isValidPhone(value)) messages.add(label + " is not a valid phone number (7-15 digits).");
            return this;
        }

        public Errors nonNegative(Double value, String label) {
            if (value != null && value < 0) messages.add(label + " cannot be negative.");
            return this;
        }

        public Errors nonNegative(Long value, String label) {
            if (value != null && value < 0) messages.add(label + " cannot be negative.");
            return this;
        }

        public Errors positive(Double value, String label) {
            if (value == null || value <= 0) messages.add(label + " must be greater than zero.");
            return this;
        }

        public Errors range(int value, int min, int max, String label) {
            if (value < min || value > max) messages.add(label + " must be between " + min + " and " + max + ".");
            return this;
        }

        public Errors notFuture(LocalDate date, String label) {
            if (date != null && date.isAfter(LocalDate.now())) messages.add(label + " cannot be in the future.");
            return this;
        }

        public Errors maxLength(String value, int max, String label) {
            if (value != null && value.length() > max) messages.add(label + " must be at most " + max + " characters.");
            return this;
        }

        public Errors check(boolean condition, String message) {
            if (!condition) messages.add(message);
            return this;
        }

        public Errors add(String message) {
            messages.add(message);
            return this;
        }

        public boolean isEmpty() { return messages.isEmpty(); }
        public List<String> messages() { return messages; }

        public void throwIfAny() {
            if (!messages.isEmpty()) throw new ValidationException(new ArrayList<>(messages));
        }
    }

    public static Errors errors() {
        return new Errors();
    }
}
