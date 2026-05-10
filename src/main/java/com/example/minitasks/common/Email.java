package com.example.minitasks.common;

import com.example.minitasks.common.exceptions.ValidationException;

import java.util.Objects;
import java.util.regex.Pattern;


public final class Email {

    private static final int MAX_LENGTH = 254;
    private static final Pattern PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$"
    );

    private final String value;

    private Email(String value) {
        this.value = value;
    }

    public static Email of(String raw) {
        if (raw == null) {
            throw new ValidationException("EMAIL_REQUIRED", "Email is required");
        }
        String normalized = raw.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new ValidationException("EMAIL_REQUIRED", "Email is required");
        }
        if (normalized.length() > MAX_LENGTH) {
            throw new ValidationException("EMAIL_TOO_LONG", "Email is too long");
        }
        if (!PATTERN.matcher(normalized).matches()) {
            throw new ValidationException("EMAIL_INVALID", "Email format is invalid");
        }
        return new Email(normalized);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email e)) return false;
        return Objects.equals(value, e.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
