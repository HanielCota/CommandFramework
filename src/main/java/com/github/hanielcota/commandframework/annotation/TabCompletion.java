package com.github.hanielcota.commandframework.annotation;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import org.bukkit.command.CommandSender;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TabCompletion {

    /**
     * Classe que implementa SuggestionProvider para este argumento.
     */
    Class<? extends SuggestionProvider<CommandSender>> provider() default NoProvider.class;

    interface NoProvider extends SuggestionProvider<CommandSender> {
    }
}


