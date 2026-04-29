package io.github.hanielcota.commandframework.core.suggestion;

import io.github.hanielcota.commandframework.core.*;
import io.github.hanielcota.commandframework.core.route.CommandLiteralNormalizer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CommandSuggestionEngine(RouteResolver resolver) {

    public CommandSuggestionEngine {
        Objects.requireNonNull(resolver, "resolver");
    }

    private static final CommandLiteralNormalizer NORMALIZER = new CommandLiteralNormalizer();

    public List<String> suggest(CommandActor actor, String label, String[] arguments) {
        Objects.requireNonNull(arguments, "arguments");
        for (String argument : arguments) {
            Objects.requireNonNull(argument, "argument");
        }
        return suggest(actor, label, List.of(arguments));
    }

    public List<String> suggest(CommandActor actor, String label, List<String> arguments) {
        CommandActor checkedActor = Objects.requireNonNull(actor, "actor");
        String checkedLabel = Objects.requireNonNull(label, "label");
        List<String> checkedArguments = List.copyOf(arguments);
        if (checkedLabel.isBlank()) {
            return resolver.rootSuggestions("");
        }
        return resolver.root(checkedLabel)
                .map(root -> suggestForRoot(checkedActor, root, checkedArguments))
                .orElseGet(() -> resolver.rootSuggestions(checkedLabel));
    }

    private List<String> suggestForRoot(CommandActor actor, CommandRoot root, List<String> values) {
        if (values.isEmpty()) {
            return root.node().childLiteralsStartingWith("");
        }
        List<String> childMatches = suggestChild(root, values);
        if (!childMatches.isEmpty()) {
            return childMatches;
        }
        RouteResolution resolution = resolver.resolve(root.label(), values);
        return resolution
                .match()
                .map(match -> suggestParameter(actor, match.route(), match.arguments()))
                .orElseGet(List::of);
    }

    private List<String> suggestChild(CommandRoot root, List<String> arguments) {
        if (arguments.isEmpty()) {
            return List.of();
        }
        String current = arguments.getLast();
        Optional<CommandNode> parent = parentNode(root, arguments);
        return parent.map(node -> node.childLiteralsStartingWith(NORMALIZER.normalize(current)))
                .orElseGet(List::of);
    }

    private Optional<CommandNode> parentNode(CommandRoot root, List<String> arguments) {
        CommandNode current = root.node();
        for (int index = 0; index < arguments.size() - 1; index++) {
            Optional<CommandNode> child = current.child(NORMALIZER.normalize(arguments.get(index)));
            if (child.isEmpty()) {
                return Optional.empty();
            }
            current = child.get();
        }
        return Optional.of(current);
    }

    private List<String> suggestParameter(CommandActor actor, CommandRoute route, List<String> arguments) {
        int index = Math.max(0, arguments.size() - 1);
        return parameterAt(route, index)
                .map(parameter -> suggestParameter(actor, route, parameter, arguments))
                .orElseGet(List::of);
    }

    private Optional<CommandParameter<?>> parameterAt(CommandRoute route, int inputIndex) {
        int seen = 0;
        for (CommandParameter<?> parameter : route.parameters()) {
            if (!parameter.consumesInput()) {
                continue;
            }
            if (seen == inputIndex) {
                return Optional.of(parameter);
            }
            seen++;
        }
        return Optional.empty();
    }

    private List<String> suggestParameter(CommandActor actor, CommandRoute route, CommandParameter<?> parameter, List<String> arguments) {
        String current = arguments.isEmpty() ? "" : arguments.getLast();
        ParameterSuggestionContext context = new ParameterSuggestionContext(actor, route, parameter, current, arguments);
        return parameter.suggest(context);
    }
}
