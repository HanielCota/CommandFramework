package io.github.hanielcota.commandframework.core;

import java.util.List;
import java.util.Objects;

/**
 * Result of pre-dispatch validation.
 */
public record ValidationResult(boolean valid, List<String> arguments, String invalidValue, String expectedValue, boolean rateLimited) {

    public ValidationResult {
        Objects.requireNonNull(arguments, "arguments");
        arguments = List.copyOf(arguments);
        Objects.requireNonNull(invalidValue, "invalidValue");
        Objects.requireNonNull(expectedValue, "expectedValue");
        if (valid && rateLimited) {
            throw new IllegalArgumentException("Invalid validation result: cannot be both valid and rate-limited");
        }
    }

    public static ValidationResult valid(List<String> arguments) {
        return new ValidationResult(true, arguments, "", "", false);
    }

    public static ValidationResult invalid(String invalidValue, String expectedValue) {
        return new ValidationResult(false, List.of(), invalidValue, expectedValue, false);
    }

    public static ValidationResult throttled() {
        return new ValidationResult(false, List.of(), "", "", true);
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isRateLimited() {
        return rateLimited;
    }
}
