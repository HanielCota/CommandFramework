package com.github.hanielcota.commandframework.parser;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ArgumentParserRegistry {

    Map<Class<?>, ArgumentParser<?>> parsers = new ConcurrentHashMap<>();

    private ArgumentParserRegistry() {
    }

    public static ArgumentParserRegistry create() {
        return new ArgumentParserRegistry();
    }

    public <T> void register(ArgumentParser<T> parser) {
        if (parser == null) {
            return;
        }
        parsers.put(parser.type(), parser);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<ArgumentParser<T>> find(Class<T> type) {
        if (type == null) {
            return Optional.empty();
        }
        var parser = parsers.get(type);
        if (parser == null) {
            return Optional.empty();
        }
        return Optional.of((ArgumentParser<T>) parser);
    }

    public boolean hasParser(Class<?> type) {
        if (type == null) {
            return false;
        }
        return parsers.containsKey(type);
    }

    public void unregister(Class<?> type) {
        if (type == null) {
            return;
        }
        parsers.remove(type);
    }
}
