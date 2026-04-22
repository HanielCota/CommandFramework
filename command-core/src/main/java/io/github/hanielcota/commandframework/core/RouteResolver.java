package io.github.hanielcota.commandframework.core;

import java.util.List;
import java.util.Optional;

/**
 * Read-only operations for resolving command routes.
 *
 * <p>This interface is intentionally narrow so that callers (such as the
 * {@link io.github.hanielcota.commandframework.core.suggestion.CommandSuggestionEngine})
 * can query routes without mutating state.</p>
 */
public interface RouteResolver {

    /**
     * Resolves a route given a label and arguments.
     *
     * @param label     the root command label; never {@code null}
     * @param arguments the argument tokens; never {@code null}
     * @return the resolution result; never {@code null}
     */
    RouteResolution resolve(String label, List<String> arguments);

    /** Returns all registered roots sorted by label. */
    List<CommandRoot> roots();

    /**
     * Returns root labels that start with the given prefix.
     *
     * @param prefix the prefix to filter by; never {@code null}
     */
    List<String> rootSuggestions(String prefix);

    /**
     * Finds a root by its label or alias.
     *
     * @param label the label to look up; never {@code null}
     */
    Optional<CommandRoot> root(String label);
}
