package io.github.hanielcota.commandframework.core;

import java.util.List;

/**
 * Functional interface for custom tab-completion logic.
 */
@FunctionalInterface
public interface SuggestionProvider {

    /**
     * Returns suggestions for the current input.
     *
     * @param context the suggestion context; never {@code null}
     * @return a list of suggestions; never {@code null}
     */
    List<String> suggest(SuggestionContext context);
}
