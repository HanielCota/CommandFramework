package io.github.hanielcota.commandframework;

/**
 * Provides framework message templates.
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
}
