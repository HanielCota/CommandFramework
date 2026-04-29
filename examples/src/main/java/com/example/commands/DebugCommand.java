package com.example.commands;

import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Description;
import io.github.hanielcota.commandframework.annotation.Permission;
import io.github.hanielcota.commandframework.annotation.Subcommand;
import io.github.hanielcota.commandframework.core.CommandActor;
import io.github.hanielcota.commandframework.core.CommandDispatcher;
import io.github.hanielcota.commandframework.core.CommandRoot;
import io.github.hanielcota.commandframework.core.CommandRoute;
import java.util.StringJoiner;

@Command("debug")
@Description("Comandos de debug do framework")
public final class DebugCommand {

    private final CommandDispatcher dispatcher;

    public DebugCommand(CommandDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Subcommand("registry")
    @Permission("debug.registry")
    @Description("Mostra todos os comandos registrados")
    public void onRegistry(CommandActor actor) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n§6====== REGISTRY REPORT ======\n");

        for (CommandRoot root : dispatcher.roots()) {
            sb.append("\n§eCommand: §f").append(root.label()).append("\n");

            if (!root.aliases().isEmpty()) {
                sb.append("§7  Aliases: §f").append(String.join(", ", root.aliases())).append("\n");
            }

            dumpNode(root.node(), sb, "  ");
        }

        sb.append("\n§6==============================\n");
        actor.sendMessage(sb.toString());
    }

    private void dumpNode(io.github.hanielcota.commandframework.core.CommandNode node, StringBuilder sb, String indent) {
        node.route().ifPresent(route -> appendRouteInfo(route, sb, indent));

        node.defaultRoute().ifPresent(route -> {
            sb.append(indent).append("§e→ [DEFAULT] §f").append(route.canonicalPath()).append("\n");
        });
    }

    private void appendRouteInfo(CommandRoute route, StringBuilder sb, String indent) {
        sb.append(indent).append("§a→ §f").append(route.canonicalPath()).append("\n");

        if (route.hasPermission()) {
            sb.append(indent).append("§7  Permission: §f").append(route.permission()).append("\n");
        }
        if (route.hasCooldown()) {
            sb.append(indent).append("§7  Cooldown: §f").append(route.cooldown()).append("\n");
        }
        if (route.async()) {
            sb.append(indent).append("§c  [ASYNC]§f\n");
        }
        if (!route.description().isBlank()) {
            sb.append(indent).append("§7  Desc: §f").append(route.description()).append("\n");
        }
        if (!route.parameters().isEmpty()) {
            sb.append(indent).append("§7  Params: §f");
            StringJoiner joiner = new StringJoiner(", ");
            route.parameters().forEach(p -> joiner.add(p.name() + ":" + p.type().getSimpleName()));
            sb.append(joiner.toString()).append("\n");
        }
    }
}
