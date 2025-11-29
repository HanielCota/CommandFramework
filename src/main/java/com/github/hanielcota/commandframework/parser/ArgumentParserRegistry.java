package com.github.hanielcota.commandframework.parser;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ArgumentParserRegistry {

    Cache<Class<?>, ArgumentParser<?>> cache;
    Map<Class<?>, ArgumentParser<?>> overrides = new ConcurrentHashMap<>();

    public static ArgumentParserRegistry create() {
        Cache<Class<?>, ArgumentParser<?>> cache = Caffeine.newBuilder()
            .maximumSize(128)
            .build();

        return new ArgumentParserRegistry(cache);
    }

    public <T> void register(ArgumentParser<T> parser) {
        if (parser == null) {
            return;
        }

        overrides.put(parser.type(), parser);
        cache.put(parser.type(), parser);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<ArgumentParser<T>> find(Class<T> type) {
        if (type == null) {
            return Optional.empty();
        }

        var overridden = Optional.ofNullable(overrides.get(type));
        if (overridden.isPresent()) {
            return overridden.map(p -> (ArgumentParser<T>) p);
        }

        var cached = Optional.ofNullable(cache.getIfPresent(type));
        if (cached.isPresent()) {
            return cached.map(p -> (ArgumentParser<T>) p);
        }

        return Optional.empty();
    }
}


