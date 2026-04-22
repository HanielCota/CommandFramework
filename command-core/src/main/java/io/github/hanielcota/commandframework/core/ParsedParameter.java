package io.github.hanielcota.commandframework.core;

import java.util.Objects;

public record ParsedParameter<T>(CommandParameter<T> parameter, T value) {

    public ParsedParameter {
        Objects.requireNonNull(parameter, "parameter");
        Objects.requireNonNull(value, "value");
    }
}
