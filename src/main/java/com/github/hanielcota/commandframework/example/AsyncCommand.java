package com.github.hanielcota.commandframework.example;

import com.github.hanielcota.commandframework.annotation.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Exemplo de comando assíncrono usando @Async e CompletionStage.
 */
@Command(
    name = "async",
    description = "Comando assíncrono de exemplo"
)
public class AsyncCommand {

    @DefaultCommand
    @Async
    public CompletionStage<Component> asyncHandler(CommandSender sender) {
        if (sender == null) {
            return CompletableFuture.completedFuture(Component.empty());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(2000);
                return (Component) Component.text("Operação assíncrona concluída!", NamedTextColor.GREEN);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return (Component) Component.text("Operação interrompida.", NamedTextColor.RED);
            }
        });
    }

    @SubCommand("database")
    @Async
    @RequiredPermission("framework.async.database")
    public CompletionStage<Component> databaseOperation(CommandSender sender, String query) {
        if (sender == null) {
            return CompletableFuture.completedFuture(Component.empty());
        }

        if (query == null || query.isBlank()) {
            return CompletableFuture.completedFuture(
                Component.text("Query não pode ser vazia.", NamedTextColor.RED)
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
                var message = Component.text()
                    .append(Component.text("Query executada: ", NamedTextColor.GREEN))
                    .append(Component.text(query, NamedTextColor.YELLOW))
                    .build();

                return message;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Component.text("Operação interrompida.", NamedTextColor.RED);
            }
        });
    }
}

