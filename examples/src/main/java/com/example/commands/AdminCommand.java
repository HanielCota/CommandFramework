package com.example.commands;

import io.github.hanielcota.commandframework.annotation.Async;
import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Description;
import io.github.hanielcota.commandframework.annotation.Permission;
import io.github.hanielcota.commandframework.annotation.Subcommand;
import io.github.hanielcota.commandframework.core.CommandActor;
import io.github.hanielcota.commandframework.core.CommandContext;
import io.github.hanielcota.commandframework.core.CommandInterceptor;
import io.github.hanielcota.commandframework.core.CommandResult;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Command("admin")
@Description("Comandos administrativos")
public final class AdminCommand {

    @Subcommand("broadcast")
    @Permission("admin.broadcast")
    @Description("Envia uma mensagem para todos os jogadores")
    public void onBroadcast(CommandActor actor, String message) {
        String formatted = "§c[BROADCAST] §f" + message;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(formatted);
        }
        actor.sendMessage("§aMensagem enviada para §f" + Bukkit.getOnlinePlayers().size() + " §ajogadores.");
    }

    @Subcommand("players")
    @Async
    @Permission("admin.players")
    @Description("Lista jogadores online (execução assíncrona)")
    public void onListPlayers(CommandActor actor) {
        long onlineCount = Bukkit.getOnlinePlayers().size();
        StringBuilder sb = new StringBuilder();
        sb.append("§6Jogadores online (§f").append(onlineCount).append("§6):\n");

        for (Player player : Bukkit.getOnlinePlayers()) {
            sb.append("§f- §e").append(player.getName()).append("\n");
        }

        actor.sendMessage(sb.toString());
    }

    @Subcommand("reload")
    @Permission("admin.reload")
    @Async
    @Description("Reload do servidor (executa em virtual thread)")
    public void onReload(CommandActor actor) {
        actor.sendMessage("§6Iniciando reload...");
        Bukkit.reload();
        actor.sendMessage("§aReload concluído!");
    }

    public static final class AdminInterceptor implements CommandInterceptor {

        @Override
        public CommandResult before(CommandContext context) {
            String permission = context.route().permission();
            if (permission.contains("admin") && !permission.isBlank()) {
                context.actor().sendMessage("§7[DEBUG] Acessando comando admin: " + context.route().canonicalPath());
            }
            return CommandResult.success();
        }

        @Override
        public CommandResult after(CommandContext context, CommandResult result) {
            if (result.isSuccess()) {
                context.actor().sendMessage("§7[DEBUG] Comando executado com sucesso");
            }
            return result;
        }
    }
}
