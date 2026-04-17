package io.github.hanielcota.commandframework;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Minimal logging abstraction used by the framework runtime.
 */
public interface FrameworkLogger {

    void debug(String message);

    void info(String message);

    void warn(String message);

    void warn(String message, Throwable cause);

    void error(String message, Throwable cause);

    static FrameworkLogger jul(Logger logger) {
        Objects.requireNonNull(logger, "logger");
        return new FrameworkLogger() {
            @Override
            public void debug(String message) {
                logger.fine(message);
            }

            @Override
            public void info(String message) {
                logger.info(message);
            }

            @Override
            public void warn(String message) {
                logger.warning(message);
            }

            @Override
            public void warn(String message, Throwable cause) {
                logger.log(Level.WARNING, message, cause);
            }

            @Override
            public void error(String message, Throwable cause) {
                logger.log(Level.SEVERE, message, cause);
            }
        };
    }
}
