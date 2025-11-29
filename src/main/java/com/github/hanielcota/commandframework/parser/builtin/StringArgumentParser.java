package com.github.hanielcota.commandframework.parser.builtin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.github.hanielcota.commandframework.parser.ArgumentParser;
import org.bukkit.command.CommandSender;

import java.util.Optional;

public class StringArgumentParser implements ArgumentParser<String> {

    @Override
    public String name() {
        return "string";
    }

    @Override
    public Class<String> type() {
        return String.class;
    }

    @Override
    public StringArgumentType brigadierType() {
        return StringArgumentType.string();
    }

    @Override
    public Optional<String> parse(CommandContext<CommandSender> context, String name) {
        if (context == null) {
            return Optional.empty();
        }

        if (name == null) {
            return Optional.empty();
        }

        return Optional.of(StringArgumentType.getString(context, name));
    }
}

