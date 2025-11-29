package com.github.hanielcota.commandframework.parser.builtin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.github.hanielcota.commandframework.parser.ArgumentParser;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class PlayerArgumentParser implements ArgumentParser<Player> {

    @Override
    public String name() {
        return "player";
    }

    @Override
    public Class<Player> type() {
        return Player.class;
    }

    @Override
    public StringArgumentType brigadierType() {
        return StringArgumentType.word();
    }

    @Override
    public Optional<Player> parse(CommandContext<CommandSender> context, String name) {
        if (context == null || name == null) {
            return Optional.empty();
        }
        var input = StringArgumentType.getString(context, name);
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        var player = Bukkit.getPlayer(input);
        return Optional.ofNullable(player);
    }
}

