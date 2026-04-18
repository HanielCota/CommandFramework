package io.github.hanielcota.commandframework.internal;

import io.github.hanielcota.commandframework.ArgumentResolver;
import io.github.hanielcota.commandframework.CommandActor;
import io.github.hanielcota.commandframework.FrameworkLogger;
import io.github.hanielcota.commandframework.MessageKey;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Produces tab-completion suggestions and emits did-you-mean hints for a dispatched command.
 *
 * <p>Collaborators are supplied by function reference (permission gate, resolver lookup) so the
 * engine carries no back-reference to {@link CommandDispatcher} and is unit-testable in isolation.
 *
 * <p><b>Thread-safety:</b> safe for concurrent use. All mutable state is request-scoped.
 */
final class CommandSuggestionEngine {

    private final Map<String, CommandDefinition> commandsByLabel;
    private final CommandTokenizer tokenizer;
    private final MessageService messages;
    private final FrameworkLogger logger;
    private final BiPredicate<CommandActor, ExecutorDefinition> permissionGate;
    private final Function<Class<?>, ArgumentResolver<Object>> resolverLookup;

    CommandSuggestionEngine(
            Map<String, CommandDefinition> commandsByLabel,
            CommandTokenizer tokenizer,
            MessageService messages,
            FrameworkLogger logger,
            BiPredicate<CommandActor, ExecutorDefinition> permissionGate,
            Function<Class<?>, ArgumentResolver<Object>> resolverLookup
    ) {
        this.commandsByLabel = Objects.requireNonNull(commandsByLabel, "commandsByLabel");
        this.tokenizer = Objects.requireNonNull(tokenizer, "tokenizer");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.permissionGate = Objects.requireNonNull(permissionGate, "permissionGate");
        this.resolverLookup = Objects.requireNonNull(resolverLookup, "resolverLookup");
    }

    List<String> suggest(CommandActor actor, String label, String rawArguments) {
        try {
            CommandDefinition command = this.commandsByLabel.get(label.toLowerCase(Locale.ROOT));
            if (command == null) {
                return List.of();
            }
            TokenizedInput tokenizedInput = this.tokenizer.tokenize(rawArguments);
            if (tokenizedInput.tokens().isEmpty()) {
                return this.emptyTokenSuggestions(actor, command, tokenizedInput.trailingSpace());
            }

            String firstToken = tokenizedInput.tokens().getFirst().toLowerCase(Locale.ROOT);
            ExecutorDefinition subcommand = command.executorsBySubcommand().get(firstToken);
            if (subcommand == null) {
                return this.rootExecutorSuggestions(actor, command, tokenizedInput, firstToken);
            }

            if (tokenizedInput.tokens().size() == 1 && !tokenizedInput.trailingSpace()) {
                return this.subcommandSuggestions(actor, command, firstToken);
            }
            if (!this.permissionGate.test(actor, subcommand)) {
                return List.of();
            }
            return this.argumentSuggestions(actor, subcommand, tokenizedInput.tokens(), tokenizedInput.trailingSpace(), 1);
        } catch (RuntimeException exception) {
            this.logger.warn("Exception during tab completion for " + LogSafety.sanitize(label), exception);
            return List.of();
        }
    }

    private List<String> emptyTokenSuggestions(CommandActor actor, CommandDefinition command, boolean trailingSpace) {
        List<String> subcommand = this.subcommandSuggestions(actor, command, "");
        ExecutorDefinition rootExecutor = command.rootExecutor();
        if (!trailingSpace || rootExecutor == null || !this.permissionGate.test(actor, rootExecutor)) {
            return subcommand;
        }
        List<String> rootArgs = this.argumentSuggestions(actor, rootExecutor, List.of(), true, 0);
        // Avoid the LinkedHashSet/List.copyOf allocation on the per-keystroke tab-completion path
        // when only one side has suggestions. The merge set is only needed to dedupe subcommand
        // names against first-argument suggestions - pointless when either list is empty.
        if (rootArgs.isEmpty()) {
            return subcommand;
        }
        if (subcommand.isEmpty()) {
            return rootArgs;
        }
        LinkedHashSet<String> merged = new LinkedHashSet<>(subcommand);
        merged.addAll(rootArgs);
        return List.copyOf(merged);
    }

    private List<String> rootExecutorSuggestions(
            CommandActor actor,
            CommandDefinition command,
            TokenizedInput tokenizedInput,
            String firstToken
    ) {
        ExecutorDefinition rootExecutor = command.rootExecutor();
        if (!tokenizedInput.trailingSpace()) {
            List<String> subSuggestions = this.subcommandSuggestions(actor, command, firstToken);
            if (!subSuggestions.isEmpty() || rootExecutor == null) {
                return subSuggestions;
            }
        }
        if (rootExecutor == null || !this.permissionGate.test(actor, rootExecutor)) {
            return List.of();
        }
        return this.argumentSuggestions(actor, rootExecutor, tokenizedInput.tokens(), tokenizedInput.trailingSpace(), 0);
    }

    private List<String> subcommandSuggestions(CommandActor actor, CommandDefinition command, String prefix) {
        String lowered = prefix.toLowerCase(Locale.ROOT);
        Map<String, ExecutorDefinition> executors = command.executorsBySubcommand();
        List<String> suggestions = new ArrayList<>(executors.size());
        for (Map.Entry<String, ExecutorDefinition> entry : executors.entrySet()) {
            if (!entry.getKey().startsWith(lowered)) {
                continue;
            }
            if (!this.permissionGate.test(actor, entry.getValue())) {
                continue;
            }
            suggestions.add(entry.getKey());
        }
        return List.copyOf(suggestions);
    }

    private List<String> argumentSuggestions(
            CommandActor actor,
            ExecutorDefinition executor,
            List<String> tokens,
            boolean trailingSpace,
            int tokenOffset
    ) {
        List<ParameterDefinition> arguments = executor.suggestableParameters();
        if (arguments.isEmpty()) {
            return List.of();
        }

        int consumed = trailingSpace ? tokens.size() - tokenOffset : tokens.size() - tokenOffset - 1;
        if (consumed < 0) {
            consumed = 0;
        }
        if (consumed >= arguments.size()) {
            return List.of();
        }
        int parameterIndex = consumed;
        ParameterDefinition parameter = arguments.get(parameterIndex);
        if (parameter.greedy()) {
            return List.of();
        }

        String currentInput = trailingSpace ? "" : tokens.getLast();
        List<String> suggestions = this.resolverLookup.apply(parameter.resolvedType()).suggest(actor, currentInput);
        return suggestions != null ? suggestions : List.of();
    }

    void emitDidYouMean(CommandActor actor, CommandDefinition command, TokenizedInput tokenizedInput) {
        if (tokenizedInput.tokens().isEmpty()) {
            return;
        }
        String typed = tokenizedInput.tokens().getFirst().toLowerCase(Locale.ROOT);
        // Skip did-you-mean for very short tokens - any 2-char typo is within edit distance 2 of
        // most short subcommand names, which produces irrelevant suggestions.
        if (typed.length() < 3) {
            return;
        }
        String closest = null;
        int bestDistance = Integer.MAX_VALUE;
        int threshold = Math.min(3, Math.max(1, typed.length() / 3));
        for (Map.Entry<String, ExecutorDefinition> entry : command.executorsBySubcommand().entrySet()) {
            if (!this.permissionGate.test(actor, entry.getValue())) {
                continue;
            }
            String candidate = entry.getKey();
            int distance = levenshtein(typed, candidate);
            if (distance < bestDistance && distance <= threshold) {
                bestDistance = distance;
                closest = candidate;
            }
        }
        if (closest != null) {
            this.messages.send(actor, MessageKey.UNKNOWN_SUBCOMMAND, Map.of(
                    "typed", typed,
                    "command", command.name(),
                    "suggestion", closest));
        }
    }

    private static int levenshtein(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }
}
