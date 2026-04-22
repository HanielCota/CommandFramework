package io.github.hanielcota.commandframework.core.help;

import io.github.hanielcota.commandframework.core.CommandActor;
import io.github.hanielcota.commandframework.core.CommandContext;
import io.github.hanielcota.commandframework.core.CommandExecutor;
import io.github.hanielcota.commandframework.core.CommandNode;
import io.github.hanielcota.commandframework.core.CommandResult;
import io.github.hanielcota.commandframework.core.CommandRoot;
import io.github.hanielcota.commandframework.core.CommandRoute;
import io.github.hanielcota.commandframework.core.ParsedParameter;
import io.github.hanielcota.commandframework.core.RouteResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Auto-generated help command that lists available routes for an actor.
 *
 * <p>Filters routes by permission and produces a simple text output.
 * Integrate by registering a route with {@link #create(String, RouteResolver)}.</p>
 */
public final class AutoHelpCommand implements CommandExecutor {

    private final RouteResolver resolver;

    private AutoHelpCommand(RouteResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    /**
     * Creates a help route for the given root label.
     *
     * @param label    the command label (e.g. "help")
     * @param resolver the route resolver to query
     * @return a ready-to-register command route
     */
    public static CommandRoute create(String label, RouteResolver resolver) {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(resolver, "resolver");
        return CommandRoute.builder(label, new AutoHelpCommand(resolver)).build();
    }

    @Override
    public CommandResult execute(CommandContext context, List<ParsedParameter<?>> parameters) {
        CommandActor actor = context.actor();
        for (CommandRoot root : resolver.roots()) {
            for (CommandRoute route : collectRoutes(root.node())) {
                if (!route.senderRequirement().allows(actor)) {
                    continue;
                }
                if (route.hasPermission() && !actor.hasPermission(route.permission())) {
                    continue;
                }
                actor.sendMessage("/" + route.canonicalPath() + " - " + description(route));
            }
        }
        return CommandResult.success();
    }

    private List<CommandRoute> collectRoutes(CommandNode node) {
        List<CommandRoute> routes = new ArrayList<>();
        node.route().ifPresent(routes::add);
        node.defaultRoute().ifPresent(routes::add);
        for (String literal : node.childLiteralsStartingWith("")) {
            node.child(literal).ifPresent(child -> routes.addAll(collectRoutes(child)));
        }
        return routes.stream().distinct().toList();
    }

    private String description(CommandRoute route) {
        return route.description().isBlank() ? "No description" : route.description();
    }
}
