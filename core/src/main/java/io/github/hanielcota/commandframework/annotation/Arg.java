package io.github.hanielcota.commandframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides argument metadata inferred from the Java parameter.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Arg {

    /**
     * Returns the public argument name.
     *
     * @return the argument name override
     */
    String value() default "";

    /**
     * Returns whether the argument consumes the remaining input.
     *
     * @return {@code true} when the argument is greedy
     */
    boolean greedy() default false;

    /**
     * Returns the maximum accepted character length for the argument.
     *
     * @return the maximum accepted length
     */
    int maxLength() default 256;
}
