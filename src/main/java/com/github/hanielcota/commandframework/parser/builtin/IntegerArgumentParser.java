package com.github.hanielcota.commandframework.parser.builtin;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.github.hanielcota.commandframework.parser.ArgumentParser;
import org.bukkit.command.CommandSender;

import java.util.Optional;

public class IntegerArgumentParser implements ArgumentParser<Integer> {

    @Override
    public String name() {
        return "int";
    }

    @Override
    public Class<Integer> type() {
        return Integer.class;
    }

    @Override
    public IntegerArgumentType brigadierType() {
        return IntegerArgumentType.integer();
    }

    @Override
    public Optional<Integer> parse(CommandContext<CommandSender> context, String name) {
        if (context == null) {
            return Optional.empty();
        }

        if (name == null) {
            return Optional.empty();
        }

        return Optional.of(IntegerArgumentType.getInteger(context, name));
    }
}


