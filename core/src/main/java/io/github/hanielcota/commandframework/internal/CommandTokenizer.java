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
        boolean trailingWhitespace = this.endsWithWhitespace(rawArguments);
        if (rawArguments.isBlank()) {
            return new TokenizedInput(List.of(), trailingWhitespace);
        }

        String[] parts = WHITESPACE.split(rawArguments.trim());
        List<String> tokens = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (!part.isEmpty()) {
                tokens.add(part);
            }
        }
        return new TokenizedInput(tokens, trailingWhitespace);
    }

    // Splitting uses \s+, so the trailing-whitespace flag must use the same definition —
    // otherwise a tab-terminated input like "foo\t" would be classified as "still typing foo"
    // instead of "ready for the next token", and tab-completion would suggest the wrong set.
    private boolean endsWithWhitespace(String input) {
        if (input.isEmpty()) {
            return false;
        }
        return Character.isWhitespace(input.charAt(input.length() - 1));
    }
}
