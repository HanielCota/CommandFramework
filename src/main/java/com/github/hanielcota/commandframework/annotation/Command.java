package com.github.hanielcota.commandframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {

    String name();

    String description() default "";

    String[] aliases() default {};

    /**
     * Quando true, permite override seguro de comandos vanilla.
     * O framework nunca faz override se isso não for explicitamente solicitado.
     */
    boolean overrideVanilla() default false;
}


