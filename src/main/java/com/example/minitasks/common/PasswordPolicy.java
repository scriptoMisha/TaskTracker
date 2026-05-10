package com.example.minitasks.common;

import com.example.minitasks.common.exceptions.ValidationException;


public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;

    private PasswordPolicy() {
    }

    public static void validate(String raw) {
        if (raw == null || raw.isEmpty()) {
            throw new ValidationException("PASSWORD_REQUIRED", "Password is required");
        }
        if (raw.length() < MIN_LENGTH) {
            throw new ValidationException("PASSWORD_TOO_SHORT",
                    "Password must be at least " + MIN_LENGTH + " characters");
        }
        if (raw.length() > MAX_LENGTH) {
            throw new ValidationException("PASSWORD_TOO_LONG",
                    "Password must be at most " + MAX_LENGTH + " characters");
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isLetter(c)) hasLetter = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        if (!hasLetter || !hasDigit) {
            throw new ValidationException("PASSWORD_WEAK",
                    "Password must contain at least one letter and one digit");
        }
    }
}
