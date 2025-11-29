package com.github.hanielcota.commandframework.parser.builtin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.github.hanielcota.commandframework.parser.ArgumentParser;
import org.bukkit.command.CommandSender;

import java.util.Optional;
import java.util.UUID;

public class UUIDArgumentParser implements ArgumentParser<UUID> {

    @Override
    public String name() {
        return "uuid";
    }

    @Override
    public Class<UUID> type() {
        return UUID.class;
    }

    @Override
    public StringArgumentType brigadierType() {
        return StringArgumentType.word();
    }

    @Override
    public Optional<UUID> parse(CommandContext<CommandSender> context, String name) {
        if (context == null || name == null) {
            return Optional.empty();
        }
        var input = StringArgumentType.getString(context, name);
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        try {
            var uuid = UUID.fromString(input);
            return Optional.of(uuid);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

