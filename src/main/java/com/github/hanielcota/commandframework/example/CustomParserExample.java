package com.github.hanielcota.commandframework.example;

import com.github.hanielcota.commandframework.annotation.Command;
import com.github.hanielcota.commandframework.annotation.RequiredPermission;
import com.github.hanielcota.commandframework.annotation.SubCommand;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.github.hanielcota.commandframework.parser.ArgumentParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Exemplo de comando usando parser customizado.
 * 
 * NOTA: Para registrar parsers customizados, você deve fazer isso no onEnable do seu plugin:
 * 
 * var framework = CommandFramework.create(...);
 * framework.getParserRegistry().register(new WorldNameParser());
 */
@Command(
    name = "custom",
    description = "Comando com parser customizado"
)
public class CustomParserExample {

    @SubCommand("teleport")
    @RequiredPermission("framework.custom.teleport")
    public Component teleportToWorld(CommandSender sender, String worldName) {
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

        if (!(sender instanceof org.bukkit.entity.Player player)) {
            return Component.text("Este comando só pode ser usado por jogadores.", NamedTextColor.RED);
        }

        player.teleport(world.getSpawnLocation());
        var message = Component.text()
            .append(Component.text("Teleportado para o mundo: ", NamedTextColor.GREEN))
            .append(Component.text(worldName, NamedTextColor.YELLOW))
            .build();

        return message;
    }

    /**
     * Parser customizado para nomes de mundos.
     */
    private static class WorldNameParser implements ArgumentParser<String> {

        @Override
        public String name() {
            return "world";
        }

        @Override
        public Class<String> type() {
            return String.class;
        }

        @Override
        public StringArgumentType brigadierType() {
            return StringArgumentType.word();
        }

        @Override
        public Optional<String> parse(CommandContext<CommandSender> context, String name) {
            if (context == null) {
                return Optional.empty();
            }

            if (name == null) {
                return Optional.empty();
            }

            var input = StringArgumentType.getString(context, name);
            if (input == null || input.isBlank()) {
                return Optional.empty();
            }

            var world = Bukkit.getWorld(input);
            if (world == null) {
                return Optional.empty();
            }

            return Optional.of(input);
        }
    }

    /**
     * SuggestionProvider customizado para sugestões dinâmicas de mundos.
     */
    public static class WorldSuggestionProvider implements SuggestionProvider<CommandSender> {

        @Override
        public CompletableFuture<Suggestions> getSuggestions(
            com.mojang.brigadier.context.CommandContext<CommandSender> context,
            SuggestionsBuilder builder
        ) {
            var worlds = Bukkit.getWorlds();
            for (var world : worlds) {
                if (world.getName().toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                    builder.suggest(world.getName());
                }
            }

            return builder.buildFuture();
        }
    }
}

