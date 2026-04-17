package io.github.hanielcota.commandframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Declares a cooldown for a command executor.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cooldown {

    /**
     * Returns the cooldown amount.
     *
     * @return the cooldown amount
     */
    long value();

    /**
     * Returns the cooldown unit.
     *
     * @return the cooldown unit
     */
    TimeUnit unit();

    /**
     * Returns the permission that bypasses the cooldown.
     *
     * @return the bypass permission
     */
    String bypassPermission() default "";
}
