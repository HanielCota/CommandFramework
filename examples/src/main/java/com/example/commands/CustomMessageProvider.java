package com.example.commands;

import io.github.hanielcota.commandframework.core.CommandActor;
import io.github.hanielcota.commandframework.core.CommandContext;
import io.github.hanielcota.commandframework.core.CommandMessageProvider;
import io.github.hanielcota.commandframework.core.SenderRequirement;
import java.time.Duration;
import java.util.Objects;

public final class CustomMessageProvider implements CommandMessageProvider {

    @Override
    public String unknownCommand(String label) {
        Objects.requireNonNull(label, "label");
        return "§cComando desconhecido: §f%s".formatted(label);
    }

    @Override
    public String noPermission(CommandContext context, String permission) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(permission, "permission");
        return "§cVocê não tem permissão para usar este comando.";
    }

    @Override
    public String invalidSender(CommandContext context, SenderRequirement expected) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(expected, "expected");
        return "§cEste comando só pode ser usado por §f%s§c.".formatted(expected.name().toLowerCase());
    }

    @Override
    public String cooldown(CommandContext context, Duration remaining) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(remaining, "remaining");
        return "§cAguarde §f%s §csegundos para usar este comando novamente.".formatted(remaining.getSeconds());
    }

    @Override
    public String invalidUsage(CommandContext context, String usage) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(usage, "usage");
        return "§cUso correto: §f%s".formatted(usage);
    }

    @Override
    public String parseFailure(CommandContext context, String invalidValue, String expectedValue) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(invalidValue, "invalidValue");
        Objects.requireNonNull(expectedValue, "expectedValue");
        return "§cValor inválido: §f%s §c(esperado: §f%s§c)".formatted(invalidValue, expectedValue);
    }

    @Override
    public String invalidInput(CommandActor actor, String invalidValue, String expectedValue) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(invalidValue, "invalidValue");
        Objects.requireNonNull(expectedValue, "expectedValue");
        return "§cInput inválido: §f%s".formatted(invalidValue);
    }

    @Override
    public String rateLimited(CommandActor actor) {
        Objects.requireNonNull(actor, "actor");
        return "§cVocê está enviando comandos rápido demais.";
    }

    @Override
    public String internalError(CommandContext context) {
        Objects.requireNonNull(context, "context");
        return "§cOcorreu um erro interno ao processar o comando.";
    }
}
