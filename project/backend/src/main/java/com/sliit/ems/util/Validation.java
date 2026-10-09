package com.sliit.ems.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Small, reusable server-side validation helpers. The React frontend may
 * also validate these same rules for a nicer UX, but every check here is
 * re-applied on the backend, because the frontend is never the real
 * security/data-integrity boundary.
 *
 * Every method either returns a value / boolean, or throws
 * IllegalArgumentException with a human-readable message - controllers
 * catch IllegalArgumentException and turn it into a 400 response, matching
 * the existing pattern already used in EmployeeController.
 */
public class Validation {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Digits, spaces, '+', '-', '(', ')' only, 7-20 characters total.
    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^[0-9+()\\-\\s]{7,20}$");

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone.trim()).matches() && phone.chars().anyMatch(Character::isDigit);
    }

    /** At least 8 characters, at least one letter and one digit. */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        return hasLetter && hasDigit;
    }

    public static void requireNonBlank(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldLabel + " is required.");
        }
    }

    public static void requireEmail(String email, String fieldLabel) {
        requireNonBlank(email, fieldLabel);
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException(fieldLabel + " must be a valid email address.");
        }
    }

    public static void requirePhone(String phone, String fieldLabel) {
        requireNonBlank(phone, fieldLabel);
        if (!isValidPhone(phone)) {
            throw new IllegalArgumentException(fieldLabel + " must contain only digits, spaces, '+' or '-' (7-20 characters).");
        }
    }

    public static void requireStrongPassword(String password) {
        if (!isStrongPassword(password)) {
            throw new IllegalArgumentException("Password must be at least 8 characters long and include at least one letter and one digit.");
        }
    }

    /** Rejects null, negative, or non-finite numbers (NaN/Infinity never happen from JSON parsing, kept as a guard anyway). */
    public static void requireNonNegative(Double value, String fieldLabel) {
        if (value == null) return; // optional numeric fields: null means "not provided", handled separately where required
        if (value.isNaN() || value.isInfinite() || value < 0) {
            throw new IllegalArgumentException(fieldLabel + " must be a non-negative number.");
        }
    }

    public static void requireNonNegative(double value, String fieldLabel) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldLabel + " must be a non-negative number.");
        }
    }

    /** Caps free-text fields at their VARCHAR column limit so an insert never fails with a raw truncation SQL error. */
    public static void requireMaxLength(String value, int maxLength, String fieldLabel) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(fieldLabel + " must be " + maxLength + " characters or fewer.");
        }
    }

    public static LocalDate parseDate(String value, String fieldLabel) {
        requireNonBlank(value, fieldLabel);
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldLabel + " must be a valid date (YYYY-MM-DD).");
        }
    }

    /** Validates a date string is a real calendar date, without requiring it to be in the past/future. */
    public static void requireValidDate(String value, String fieldLabel) {
        parseDate(value, fieldLabel);
    }

    public static void requireDateNotBefore(LocalDate value, LocalDate notBefore, String fieldLabel, String boundLabel) {
        if (value.isBefore(notBefore)) {
            throw new IllegalArgumentException(fieldLabel + " cannot be before " + boundLabel + ".");
        }
    }

    public static void requireNotInPast(LocalDate value, String fieldLabel) {
        if (value.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(fieldLabel + " cannot be in the past.");
        }
    }
}
