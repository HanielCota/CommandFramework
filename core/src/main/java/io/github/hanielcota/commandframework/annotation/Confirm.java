package io.github.hanielcota.commandframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires an explicit confirmation before running a command executor.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Confirm {

    /**
     * Returns the confirmation expiration in seconds.
     *
     * @return the expiration window in seconds
     */
    long expireSeconds();

    /**
     * Returns the command label used to confirm the pending action.
     *
     * @return the confirmation command label
     */
    String commandName() default "confirm";
}
