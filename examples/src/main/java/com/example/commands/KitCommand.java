package com.example.commands;

import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Cooldown;
import io.github.hanielcota.commandframework.annotation.Default;
import io.github.hanielcota.commandframework.annotation.OnlyPlayer;
import io.github.hanielcota.commandframework.annotation.Permission;
import io.github.hanielcota.commandframework.annotation.Subcommand;
import io.github.hanielcota.commandframework.core.CommandActor;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;

@Command("kit")
public final class KitCommand {

    @Default
    @OnlyPlayer
    public void onDefault(CommandActor actor) {
        Objects.requireNonNull(actor, "actor");
        actor.sendMessage("§eUse §f/kit give <jogador>§e.");
    }

    @Subcommand("give")
    @Permission("kit.give")
    @Cooldown(value = 3, unit = TimeUnit.SECONDS)
    public void onGive(CommandActor actor, String target) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(target, "target");
        if (target.isBlank()) {
            actor.sendMessage("§cInforme o nome do jogador.");
            return;
        }
        actor.sendMessage("§aKit enviado para §f" + target + "§a.");
    }
}
