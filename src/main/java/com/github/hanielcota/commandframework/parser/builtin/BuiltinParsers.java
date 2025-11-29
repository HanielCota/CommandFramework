package com.github.hanielcota.commandframework.parser.builtin;

import com.github.hanielcota.commandframework.parser.ArgumentParserRegistry;

public final class BuiltinParsers {

    private BuiltinParsers() {
    }

    public static void registerAll(ArgumentParserRegistry registry) {
        if (registry == null) {
            return;
        }

        registry.register(new IntegerArgumentParser());
        registry.register(new StringArgumentParser());
        registry.register(new BooleanArgumentParser());
        registry.register(new PlayerArgumentParser());
        registry.register(new GameModeArgumentParser());
        registry.register(new UUIDArgumentParser());
    }
}

