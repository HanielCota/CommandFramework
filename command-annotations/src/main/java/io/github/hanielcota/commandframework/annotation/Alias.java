package io.github.hanielcota.commandframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an additional alias for a specific subcommand method.
 *
 * <p>This is additive to the root aliases defined in {@link Command}.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Alias {

    String[] value();
}
