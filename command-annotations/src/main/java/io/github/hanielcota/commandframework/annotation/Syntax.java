package io.github.hanielcota.commandframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Overrides the auto-generated usage syntax for a command route.
 *
 * <p>If absent, the framework derives usage from the parameter list.</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Syntax {

    String value();
}
