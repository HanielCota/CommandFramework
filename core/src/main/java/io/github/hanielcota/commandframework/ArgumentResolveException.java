package io.github.hanielcota.commandframework;

import java.util.Objects;

/**
 * Signals that an argument could not be resolved.
 */
public class ArgumentResolveException extends Exception {

    private final String argumentName;
    private final String input;

    /**
     * Creates a new exception instance.
     *
     * @param argumentName the argument name
     * @param input        the raw input that failed to resolve
     * @param message      the failure message
     */
    public ArgumentResolveException(String argumentName, String input, String message) {
        super(message);
        this.argumentName = Objects.requireNonNull(argumentName, "argumentName");
        this.input = Objects.requireNonNull(input, "input");
    }

    /**
     * Creates a new exception instance.
     *
     * @param argumentName the argument name
     * @param input        the raw input that failed to resolve
     * @param message      the failure message
     * @param cause        the underlying cause
     */
    public ArgumentResolveException(String argumentName, String input, String message, Throwable cause) {
        super(message, cause);
        this.argumentName = Objects.requireNonNull(argumentName, "argumentName");
        this.input = Objects.requireNonNull(input, "input");
    }

    /**
     * Returns the argument name.
     *
     * @return the argument name
     */
    public String argumentName() {
        return this.argumentName;
    }

    /**
     * Returns the raw input value.
     *
     * @return the raw input value
     */
    public String input() {
        return this.input;
    }
}
