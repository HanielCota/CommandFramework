package io.github.hanielcota.commandframework.core;

/**
 * Thrown when a command route is configured incorrectly.
 *
 * <p>This covers builder validation, annotation scanning errors, duplicate
 * routes, invalid literals, and alias conflicts. It is always a programmer
 * error and should surface as early as possible (build-time or startup).</p>
 */
public class RouteConfigurationException extends CommandException {

    public RouteConfigurationException(String message) {
        super(message);
    }

    public RouteConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
