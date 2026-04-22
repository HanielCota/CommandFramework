package io.github.hanielcota.commandframework.core;

import java.util.Objects;

public record ArgumentInput(String rawValue, String parameterName) {

    public ArgumentInput {
        Objects.requireNonNull(rawValue, "rawValue");
        Objects.requireNonNull(parameterName, "parameterName");
    }
}
