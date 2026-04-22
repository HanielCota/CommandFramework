package io.github.hanielcota.commandframework.core.route;

import java.util.Locale;
import java.util.Objects;

public final class CommandLiteralNormalizer {

    public String normalize(String literal) {
        Objects.requireNonNull(literal, "literal");
        return literal.trim().toLowerCase(Locale.ROOT);
    }
}
