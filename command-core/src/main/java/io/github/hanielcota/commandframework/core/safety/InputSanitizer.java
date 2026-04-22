package io.github.hanielcota.commandframework.core.safety;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record InputSanitizer(int maxArguments, int maxArgumentLength) {

    public InputSanitizer {
        if (maxArguments < 1) {
            throw new IllegalArgumentException("Invalid max arguments: expected at least one");
        }
        if (maxArgumentLength < 1) {
            throw new IllegalArgumentException("Invalid max argument length: expected at least one");
        }
    }

    public SanitizedInput sanitize(String[] arguments) {
        Objects.requireNonNull(arguments, "arguments");
        for (String argument : arguments) {
            if (argument == null) {
                return SanitizedInput.invalid("null", "non-null argument");
            }
        }
        if (arguments.length > maxArguments) {
            return SanitizedInput.invalid(String.valueOf(arguments.length), "at most %d arguments".formatted(maxArguments));
        }
        return sanitizeValues(List.of(arguments));
    }

    public SanitizedInput sanitize(List<String> arguments) {
        Objects.requireNonNull(arguments, "arguments");
        if (arguments.size() > maxArguments) {
            return SanitizedInput.invalid(String.valueOf(arguments.size()), "at most %d arguments".formatted(maxArguments));
        }
        return sanitizeValues(arguments);
    }

    private SanitizedInput sanitizeValues(List<String> arguments) {
        List<String> sanitized = new ArrayList<>(arguments.size());
        for (String argument : arguments) {
            if (argument == null) {
                return SanitizedInput.invalid("null", "non-null argument");
            }
            SanitizedInput checked = sanitizeOne(argument, sanitized);
            if (!checked.isValid()) {
                return checked;
            }
        }
        return SanitizedInput.valid(sanitized);
    }

    private SanitizedInput sanitizeOne(String argument, List<String> sanitized) {
        if (argument.length() > maxArgumentLength) {
            return SanitizedInput.invalid(argument, "argument with %d chars or less".formatted(maxArgumentLength));
        }
        sanitized.add(stripControls(argument));
        return SanitizedInput.valid(sanitized);
    }

    private String stripControls(String argument) {
        StringBuilder builder = new StringBuilder(argument.length());
        for (int index = 0; index < argument.length(); index++) {
            appendIfSafe(builder, argument.charAt(index));
        }
        return builder.toString();
    }

    private void appendIfSafe(StringBuilder builder, char value) {
        if (!Character.isISOControl(value)) {
            builder.append(value);
        }
    }
}
