package io.github.hanielcota.commandframework.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable context available during command execution.
 *
 * <p>Holds the actor, resolved route, raw label, arguments and any
 * parameters that were successfully parsed by the
 * {@link io.github.hanielcota.commandframework.core.dispatch.CommandParameterParser}.
 * </p>
 */
public record CommandContext(
        CommandActor actor,
        CommandRoute route,
        String label,
        List<String> arguments,
        Map<String, ParsedParameter<?>> parsedParameters
) {

    /**
     * Creates a context without parsed parameters (used before parsing).
     */
    public CommandContext(CommandActor actor, CommandRoute route, String label, List<String> arguments) {
        this(actor, route, label, arguments, Map.of());
    }

    public CommandContext(
            CommandActor actor,
            CommandRoute route,
            String label,
            List<String> arguments,
            Map<String, ParsedParameter<?>> parsedParameters
    ) {
        this.actor = Objects.requireNonNull(actor, "actor");
        this.route = Objects.requireNonNull(route, "route");
        this.label = Objects.requireNonNull(label, "label");
        this.arguments = List.copyOf(arguments);
        this.parsedParameters = Map.copyOf(parsedParameters);
    }

    /**
     * Returns a new context with the given parsed parameters attached.
     *
     * @param parsed the successfully parsed parameters
     */
    public CommandContext withParsedParameters(List<ParsedParameter<?>> parsed) {
        Objects.requireNonNull(parsed, "parsed");
        Map<String, ParsedParameter<?>> values = new LinkedHashMap<>();
        parsed.forEach(value -> values.put(value.parameter().name(), value));
        return new CommandContext(actor, route, label, arguments, values);
    }

    /**
     * Looks up a parsed parameter by its declared name.
     *
     * @param name the parameter name
     * @return the parsed parameter if present
     */
    public Optional<ParsedParameter<?>> parsedParameter(String name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(parsedParameters.get(name));
    }
}
