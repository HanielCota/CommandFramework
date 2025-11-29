package com.github.hanielcota.commandframework.framework;

import com.github.hanielcota.commandframework.messaging.MessageProvider;
import com.github.hanielcota.commandframework.parser.ArgumentParserRegistry;
import com.github.hanielcota.commandframework.registry.CommandScanner;
import com.github.hanielcota.commandframework.execution.CommandExecutor;
import com.github.hanielcota.commandframework.processor.CommandProcessor;
import com.github.benmanes.caffeine.cache.Cache;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class FrameworkConfiguration {
    private final MessageProvider messageProvider;
    private final ArgumentParserRegistry parserRegistry;
    private final CommandScanner scanner;
    private final CommandExecutor executor;
    private final CommandProcessor processor;
    private final Cache<Class<?>, Object> handlerCache;

    public FrameworkConfiguration(
            MessageProvider messageProvider,
            ArgumentParserRegistry parserRegistry,
            CommandScanner scanner,
            CommandExecutor executor,
            CommandProcessor processor,
            Cache<Class<?>, Object> handlerCache) {
        this.messageProvider = messageProvider;
        this.parserRegistry = parserRegistry;
        this.scanner = scanner;
        this.executor = executor;
        this.processor = processor;
        this.handlerCache = handlerCache;
    }

    public MessageProvider getMessageProvider() {
        return messageProvider;
    }

    public ArgumentParserRegistry getParserRegistry() {
        return parserRegistry;
    }

    public CommandScanner getScanner() {
        return scanner;
    }

    public CommandExecutor getExecutor() {
        return executor;
    }

    public CommandProcessor getProcessor() {
        return processor;
    }

    public Cache<Class<?>, Object> getHandlerCache() {
        return handlerCache;
    }
}

