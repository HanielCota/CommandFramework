package io.github.hanielcota.commandframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a command root.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {

    /**
     * Returns the primary command name.
     *
     * @return the primary command name
     */
    String name();

    /**
     * Returns the command aliases.
     *
     * @return the aliases
     */
    String[] aliases() default {};

    /**
     * Returns the command description.
     *
     * @return the command description
     */
    String description() default "";

    /**
     * Returns the base permission for the command class.
     *
     * @return the base permission
     */
    String permission() default "";
}
