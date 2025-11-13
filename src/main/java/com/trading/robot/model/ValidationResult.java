package com.trading.robot.model;

/**
 * Represents a validation outcome along with contextual message.
 */
public class ValidationResult {

    private final boolean valid;
    private final String message;

    private ValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public static ValidationResult ok(String message) {
        return new ValidationResult(true, message);
    }

    public static ValidationResult error(String message) {
        return new ValidationResult(false, message);
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }
}

