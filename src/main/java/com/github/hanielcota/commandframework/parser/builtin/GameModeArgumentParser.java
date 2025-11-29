package com.github.hanielcota.commandframework.parser.builtin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.github.hanielcota.commandframework.parser.ArgumentParser;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;

import java.util.Optional;

public class GameModeArgumentParser implements ArgumentParser<GameMode> {

    @Override
    public String name() {
        return "gamemode";
    }

    @Override
    public Class<GameMode> type() {
        return GameMode.class;
    }

    @Override
    public StringArgumentType brigadierType() {
        return StringArgumentType.word();
    }

    @Override
    public Optional<GameMode> parse(CommandContext<CommandSender> context, String name) {
        if (context == null || name == null) {
            return Optional.empty();
        }
        var input = StringArgumentType.getString(context, name);
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        return parseGameMode(input);
    }
    
    private Optional<GameMode> parseGameMode(String input) {
        try {
            var mode = GameMode.valueOf(input.toUpperCase());
            return Optional.of(mode);
        } catch (IllegalArgumentException e) {
            return parseGameModeByValue(input);
        }
    }
    
    private Optional<GameMode> parseGameModeByValue(String input) {
        try {
            var value = Integer.parseInt(input);
            var mode = GameMode.getByValue(value);
            return Optional.ofNullable(mode);
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}

