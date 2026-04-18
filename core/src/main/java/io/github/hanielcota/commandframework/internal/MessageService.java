package io.github.hanielcota.commandframework.internal;

import io.github.hanielcota.commandframework.CommandActor;
import io.github.hanielcota.commandframework.MessageKey;
import io.github.hanielcota.commandframework.MessageProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formats framework messages using {@link MessageProvider} templates and a MiniMessage parser.
 *
 * <p><b>Thread-safety:</b> once constructed, all state is effectively immutable and the service is
 * safe for concurrent use. Placeholder substitution creates per-call maps and does not share
 * state across calls.
 */
public final class MessageService {

    private static final long SECONDS_PER_MINUTE = 60L;
    // Restricted to identifier-like keys to limit the blast radius of future extensions.
    // A permissive match (e.g. any non-whitespace) would pre-qualify path-like or expression-like
    // tokens such as "{../etc/passwd}" or "{$env.SECRET}" as placeholders - fine today because
    // unknown keys pass through verbatim, but risky if dynamic lookup is ever layered on top.
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-zA-Z0-9_.-]{1,32})}");

    private final MessageProvider provider;
    private final MiniMessage miniMessage;

    public MessageService(MessageProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.miniMessage = MiniMessage.miniMessage();
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
        Map<String, String> safePlaceholders = placeholders == null ? Map.of() : placeholders;
        String rendered = this.applyPlaceholders(this.template(key), safePlaceholders);
        return this.miniMessage.deserialize(rendered, TagResolver.empty());
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
        Objects.requireNonNull(template, "template");
        // With no placeholders supplied every {key} match would fall through to the "raw == null"
        // branch below and be kept verbatim - producing an output identical to the input. Skip the
        // regex scan entirely in that case; this is the hot path for parameterless system messages.
        if (placeholders.isEmpty()) {
            return template;
        }
        // Single-pass scan: each {key} match in the template is replaced exactly once from the map,
        // so a user-controlled value like "{otherKey}" is never re-interpreted as a placeholder.
        // Unknown keys are left verbatim so missing placeholders are visible instead of silently empty.
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder(template.length());
        while (matcher.find()) {
            String key = matcher.group(1);
            String raw = placeholders.get(key);
            String replacement = raw == null
                    ? matcher.group()
                    : this.miniMessage.escapeTags(raw);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

}
