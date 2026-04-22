package io.github.hanielcota.commandframework.core.safety;

import java.util.List;
import java.util.Objects;

public record SanitizedInput(List<String> arguments, String invalidValue, String expectedValue, boolean valid) {

    public SanitizedInput(List<String> arguments, String invalidValue, String expectedValue, boolean valid) {
        Objects.requireNonNull(arguments, "arguments");
        this.arguments = List.copyOf(arguments);
        this.invalidValue = Objects.requireNonNull(invalidValue, "invalidValue");
        this.expectedValue = Objects.requireNonNull(expectedValue, "expectedValue");
        this.valid = valid;
    }

    public static SanitizedInput valid(List<String> arguments) {
        return new SanitizedInput(arguments, "", "", true);
    }

    public static SanitizedInput invalid(String invalidValue, String expectedValue) {
        return new SanitizedInput(List.of(), invalidValue, expectedValue, false);
    }

    public boolean isValid() {
        return valid;
    }
}
