package io.github.hanielcota.commandframework.core.safety;

import java.util.Objects;

public final class SafeLogText {

    public String clean(String value) {
        Objects.requireNonNull(value, "value");
        StringBuilder builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            appendSafe(builder, value.charAt(index));
        }
        return builder.toString();
    }

    private void appendSafe(StringBuilder builder, char value) {
        if (Character.isISOControl(value)) {
            builder.append('?');
            return;
        }
        builder.append(value);
    }
}
