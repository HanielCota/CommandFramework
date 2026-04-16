package io.github.hanielcota.commandframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an argument as optional.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Optional {

    /**
     * Sentinel used when no default was declared.
     */
    String UNSET = "\u0000";

    /**
     * Returns the default string representation.
     *
     * @return the default value, or {@link #UNSET}
     */
    String value() default UNSET;
}
