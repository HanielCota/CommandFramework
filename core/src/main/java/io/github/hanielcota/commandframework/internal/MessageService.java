package io.github.hanielcota.commandframework.internal;

import io.github.hanielcota.commandframework.CommandActor;
import io.github.hanielcota.commandframework.MessageKey;
import io.github.hanielcota.commandframework.MessageProvider;
import net.kyori.adventure.text.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Formats framework messages using {@link MessageProvider} templates and an optional MiniMessage
 * parser, falling back to plain-text {@link Component} construction when MiniMessage is not
 * present on the runtime classpath.
 *
 * <p>The MiniMessage parser is resolved reflectively exactly once at construction, keeping the
 * framework usable on platforms that ship an older Adventure build without MiniMessage.
 *
 * <p><b>Thread-safety:</b> once constructed, all state is effectively immutable and the service is
 * safe for concurrent use. Placeholder substitution creates per-call maps and does not share
 * state across calls.
 */
public final class MessageService {

    private static final long SECONDS_PER_MINUTE = 60L;

    private final MessageProvider provider;
    private final MiniMessageParser miniMessageParser;

    public MessageService(MessageProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.miniMessageParser = this.loadMiniMessageParser();
    }

    public String template(MessageKey key) {
        Objects.requireNonNull(key, "key");
        String template = this.provider.message(key);
        if (template == null) {
            throw new IllegalStateException("MessageProvider returned null for " + key);
        }
        return template;
    }

    public void send(CommandActor actor, MessageKey key) {
        this.send(actor, key, Map.of());
    }

    public void send(CommandActor actor, MessageKey key, Map<String, String> placeholders) {
        Objects.requireNonNull(actor, "actor");
        if (!actor.isAvailable()) {
            return;
        }
        actor.sendMessage(this.render(key, placeholders));
    }

    public Component render(MessageKey key, Map<String, String> placeholders) {
        String rendered = this.applyPlaceholders(this.template(key), placeholders);
        return this.miniMessageParser.parse(rendered);
    }

    public Component renderLines(Component... lines) {
        Component result = Component.empty();
        for (int index = 0; index < lines.length; index++) {
            if (index > 0) {
                result = result.append(Component.newline());
            }
            result = result.append(lines[index]);
        }
        return result;
    }

    public String formatDuration(Duration duration) {
        long seconds = Math.max(1L, duration.toSeconds());
        long minutes = seconds / SECONDS_PER_MINUTE;
        if (minutes == 0L) {
            return seconds + "s";
        }
        long remainingSeconds = seconds % SECONDS_PER_MINUTE;
        if (remainingSeconds == 0L) {
            return minutes + "m";
        }
        return minutes + "m " + remainingSeconds + "s";
    }

    private String applyPlaceholders(String template, Map<String, String> placeholders) {
        String result = Objects.requireNonNull(template, "template");
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", Objects.toString(entry.getValue(), ""));
        }
        return result;
    }

    private MiniMessageParser loadMiniMessageParser() {
        try {
            Class<?> miniMessageClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            Class<?> tagResolverClass = Class.forName("net.kyori.adventure.text.minimessage.tag.resolver.TagResolver");
            Method miniMessageFactory = miniMessageClass.getMethod("miniMessage");
            Method emptyTagResolverFactory = tagResolverClass.getMethod("empty");
            Object miniMessage = miniMessageFactory.invoke(null);
            Object emptyTagResolver = emptyTagResolverFactory.invoke(null);
            Method deserialize = miniMessageClass.getMethod("deserialize", String.class, tagResolverClass);
            return input -> {
                try {
                    return (Component) deserialize.invoke(miniMessage, input, emptyTagResolver);
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Unable to parse MiniMessage input", exception);
                }
            };
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "MiniMessage runtime is required. Add net.kyori:adventure-text-minimessage to the runtime classpath.",
                    exception
            );
        }
    }

    @FunctionalInterface
    private interface MiniMessageParser {
        Component parse(String input);
    }
}
