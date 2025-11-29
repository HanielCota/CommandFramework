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
     * Sugestões estáticas para tab completion.
     * Pode ser usado diretamente: @TabCompletion("1", "2", "3")
     * ou com nome: @TabCompletion(value = {"1", "2", "3"})
     */
    String[] value() default {};

    /**
     * Classe que implementa SuggestionProvider para este argumento.
     * Usado quando sugestões dinâmicas são necessárias.
     * Exemplo: @TabCompletion(provider = PlayerSuggestionProvider.class)
     */
    Class<? extends SuggestionProvider<CommandSender>> provider() default NoProvider.class;

    interface NoProvider extends SuggestionProvider<CommandSender> {
    }
}


