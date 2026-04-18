package io.github.hanielcota.commandframework;

import io.github.hanielcota.commandframework.internal.DefaultMessageProvider;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Provides framework message templates.
 *
 * <p><b>Trust boundary — IMPORTANT:</b> templates returned by this provider are fed directly to
 * the MiniMessage deserializer. MiniMessage tags such as {@code <click:run_command:…>},
 * {@code <hover:…>} and {@code <insert:…>} are interpreted as interactive components. Only
 * placeholder <i>values</i> injected by the framework are escaped at render time — templates
 * themselves are NOT. Load templates only from sources you trust (code, plugin-bundled
 * resources, admin-reviewed config). Never source them from untrusted input (user chat, web
 * forms, databases with user-writable rows) without sanitising the strings first via
 * {@code MiniMessage.miniMessage().escapeTags(raw)}.
 */
@FunctionalInterface
public interface MessageProvider {

    /**
     * Returns the template for the given message key.
     *
     * @param key the message key
     * @return the message template
     */
    String message(MessageKey key);

    /**
     * Returns the built-in MiniMessage templates. Safe to call at any time — stateless.
     */
    static MessageProvider defaults() {
        return DefaultMessageProvider.INSTANCE;
    }

    /**
     * Returns a provider backed by the given map, falling back to {@link #defaults()} for
     * any key the map does not cover. The map is copied defensively.
     *
     * @param templates templates keyed by {@link MessageKey}
     */
    static MessageProvider fromMap(Map<MessageKey, String> templates) {
        Objects.requireNonNull(templates, "templates");
        Map<MessageKey, String> copy = Map.copyOf(templates);
        MessageProvider fallback = defaults();
        return key -> copy.getOrDefault(key, fallback.message(key));
    }

    /**
     * Returns a provider backed by a name-keyed map, tolerant of common config conventions:
     * keys are normalised by upper-casing and replacing {@code -} with {@code _}, so
     * {@code "no-permission"}, {@code "NO_PERMISSION"} and {@code "no_permission"} all map
     * to {@link MessageKey#NO_PERMISSION}. Unknown keys are silently ignored so configs may
     * carry comments or forward-compatible extras. Missing keys fall back to {@link #defaults()}.
     *
     * @param templates templates keyed by string
     */
    static MessageProvider fromStringMap(Map<String, String> templates) {
        Objects.requireNonNull(templates, "templates");
        EnumMap<MessageKey, String> resolved = new EnumMap<>(MessageKey.class);
        for (Map.Entry<String, String> entry : templates.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String normalized = entry.getKey().toUpperCase(Locale.ROOT).replace('-', '_');
            try {
                resolved.put(MessageKey.valueOf(normalized), entry.getValue());
            } catch (IllegalArgumentException ignored) {
                // Skip unknown keys — configs often carry comments or forward-compatible entries.
            }
        }
        return fromMap(resolved);
    }
}
