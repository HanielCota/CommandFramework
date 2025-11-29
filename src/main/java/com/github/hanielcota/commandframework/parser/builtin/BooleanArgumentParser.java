package com.github.hanielcota.commandframework.parser.builtin;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.github.hanielcota.commandframework.parser.ArgumentParser;
import org.bukkit.command.CommandSender;

import java.util.Optional;

public class BooleanArgumentParser implements ArgumentParser<Boolean> {

    @Override
    public String name() {
        return "bool";
    }

    @Override
    public Class<Boolean> type() {
        return Boolean.class;
    }

    @Override
    public BoolArgumentType brigadierType() {
        return BoolArgumentType.bool();
    }

    @Override
    public Optional<Boolean> parse(CommandContext<CommandSender> context, String name) {
        if (context == null) {
            return Optional.empty();
        }

        if (name == null) {
            return Optional.empty();
        }

        return Optional.of(BoolArgumentType.getBool(context, name));
    }
}

