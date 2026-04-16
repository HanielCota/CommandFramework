package io.github.hanielcota.commandframework.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits a raw argument string into whitespace-separated tokens and tracks whether the caller
 * finished with a trailing space.
 *
 * <p>The trailing-space flag is the signal used by tab-completion to decide whether the cursor is
 * at the start of a new token (suggest next positional) or still inside the previous one.
 *
 * <p><b>Thread-safety:</b> stateless and safe for concurrent use.
 */
public final class CommandTokenizer {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    @SuppressWarnings("StringSplitter") // input is trimmed and empties are filtered below
    TokenizedInput tokenize(String rawArguments) {
        if (rawArguments.isBlank()) {
            return new TokenizedInput(List.of(), rawArguments.endsWith(" "));
        }

        String[] parts = WHITESPACE.split(rawArguments.trim());
        List<String> tokens = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (!part.isEmpty()) {
                tokens.add(part);
            }
        }
        return new TokenizedInput(tokens, rawArguments.endsWith(" "));
    }
}
