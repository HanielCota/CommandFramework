package io.github.hanielcota.commandframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Executes the annotated command method on a virtual thread.
 *
 * <p>Argument resolution still runs on the platform thread before the method is invoked. Framework-managed sender
 * messaging remains availability-aware, but async executors must inject {@code @Sender CommandActor} instead of raw
 * platform sender types. Consumer code executed inside an async command is responsible for using thread-safe shared
 * state.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Async {
}
