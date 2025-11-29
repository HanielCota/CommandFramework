package com.github.hanielcota.commandframework.example;

import com.github.hanielcota.commandframework.annotation.Command;
import com.github.hanielcota.commandframework.annotation.RequiredPermission;
import com.github.hanielcota.commandframework.annotation.SubCommand;
import com.github.hanielcota.commandframework.annotation.TabCompletion;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

/**
 * Exemplo de comando com sugestões dinâmicas usando SuggestionProvider.
 */
@Command(
    name = "suggest",
    description = "Comando com sugestões dinâmicas"
)
public class DynamicSuggestionsExample {

    @SubCommand("player")
    @RequiredPermission("framework.suggest.player")
    public Component playerCommand(CommandSender sender, @TabCompletion(provider = PlayerSuggestionProvider.class) Player target) {
        if (sender == null) {
            return Component.empty();
        }

        if (target == null) {
            return Component.text("Jogador não encontrado.", NamedTextColor.RED);
        }

        var message = Component.text()
            .append(Component.text("Jogador selecionado: ", NamedTextColor.GREEN))
            .append(Component.text(target.getName(), NamedTextColor.YELLOW))
            .build();

        return message;
    }

    @SubCommand("world")
    @RequiredPermission("framework.suggest.world")
    public Component worldCommand(CommandSender sender, @TabCompletion(provider = WorldSuggestionProvider.class) String worldName) {
        if (sender == null) {
            return Component.empty();
        }

        if (worldName == null || worldName.isBlank()) {
            return Component.text("Nome do mundo não pode ser vazio.", NamedTextColor.RED);
        }

        var world = Bukkit.getWorld(worldName);
        if (world == null) {
            return Component.text("Mundo não encontrado: " + worldName, NamedTextColor.RED);
        }

        var message = Component.text()
            .append(Component.text("Mundo selecionado: ", NamedTextColor.GREEN))
            .append(Component.text(worldName, NamedTextColor.YELLOW))
            .build();

        return message;
    }

    /**
     * SuggestionProvider para jogadores online.
     */
    public static class PlayerSuggestionProvider implements SuggestionProvider<CommandSender> {

        @Override
        public CompletableFuture<Suggestions> getSuggestions(
            com.mojang.brigadier.context.CommandContext<CommandSender> context,
            SuggestionsBuilder builder
        ) {
            var players = Bukkit.getOnlinePlayers();
            for (var player : players) {
                var name = player.getName();
                if (name.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                    builder.suggest(name);
                }
            }

            return builder.buildFuture();
        }
    }

    /**
     * SuggestionProvider para nomes de mundos.
     */
    public static class WorldSuggestionProvider implements SuggestionProvider<CommandSender> {

        @Override
        public CompletableFuture<Suggestions> getSuggestions(
            com.mojang.brigadier.context.CommandContext<CommandSender> context,
            SuggestionsBuilder builder
        ) {
            var worlds = Bukkit.getWorlds();
            for (var world : worlds) {
                var name = world.getName();
                if (name.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                    builder.suggest(name);
                }
            }

            return builder.buildFuture();
        }
    }
}

