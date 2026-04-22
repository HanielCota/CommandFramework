package io.github.hanielcota.commandframework.core.dispatch;

import java.util.Objects;

public record ParameterParseFailure(String invalidValue, String expectedValue) {

    public ParameterParseFailure {
        Objects.requireNonNull(invalidValue, "invalidValue");
        Objects.requireNonNull(expectedValue, "expectedValue");
    }
}
