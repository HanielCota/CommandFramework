package io.github.hanielcota.commandframework.internal;

import java.util.regex.Pattern;

/**
 * Collapses control characters and caps length for values that originate from user-controlled
 * sources (player names, labels routed through the framework). Prevents log injection via
 * command-block CustomName or Bedrock-style usernames that include unusual bytes, and bounds
 * line length so a single log entry cannot dominate a log aggregator view.
 */
final class LogSafety {

    private static final int MAX_LOG_STRING_LENGTH = 64;
    private static final Pattern UNSAFE_CHARS = Pattern.compile("[\\r\\n\\t\\p{Cntrl}]");

    private LogSafety() {
    }

    static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        String collapsed = UNSAFE_CHARS.matcher(value).replaceAll("?");
        return collapsed.length() > MAX_LOG_STRING_LENGTH
                ? collapsed.substring(0, MAX_LOG_STRING_LENGTH) + "…"
                : collapsed;
    }
}
