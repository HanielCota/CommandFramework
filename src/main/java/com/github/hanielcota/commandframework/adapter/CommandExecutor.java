package com.github.hanielcota.commandframework.adapter;

import com.github.hanielcota.commandframework.annotation.Async;
import com.github.hanielcota.commandframework.annotation.RequiredPermission;
import com.github.hanielcota.commandframework.cooldown.CooldownService;
import com.github.hanielcota.commandframework.error.GlobalErrorHandler;
import com.github.hanielcota.commandframework.value.RemainingArguments;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class CommandExecutor {
    private final Plugin plugin;
    private final CooldownChecker cooldownChecker;
    private final MethodInvoker methodInvoker;
    private final PermissionChecker permissionChecker;
    private final GlobalErrorHandler errorHandler;

    public CommandExecutor(Plugin plugin, CooldownService cooldownService, GlobalErrorHandler errorHandler) {
        this.plugin = plugin;
        this.errorHandler = errorHandler;
        this.cooldownChecker = new CooldownChecker(cooldownService, errorHandler);
        this.methodInvoker = new MethodInvoker();
        this.permissionChecker = new PermissionChecker(errorHandler);
    }

    public boolean execute(CommandSender sender, String commandName, String[] args, Method method, Object instance, RequiredPermission classPermission, com.github.hanielcota.commandframework.annotation.Cooldown classCooldown) {
        if (sender == null) {
            return false;
        }
        if (!permissionChecker.checkClassPermission(sender, classPermission)) {
            return true;
        }
        if (!permissionChecker.checkMethodPermission(sender, method.getAnnotation(RequiredPermission.class))) {
            return true;
        }
        if (!cooldownChecker.checkAndApplyCooldown(sender, commandName, method.getName(), method.getAnnotation(com.github.hanielcota.commandframework.annotation.Cooldown.class), classCooldown)) {
            return true;
        }
        executeMethod(sender, method, instance, args);
        return true;
    }

    private void executeMethod(CommandSender sender, Method method, Object instance, String[] args) {
        var isAsync = method.isAnnotationPresent(Async.class);
        if (isAsync) {
            scheduleAsyncExecution(sender, method, instance, args);
            return;
        }
        executeSync(sender, method, instance, args);
    }

    private void scheduleAsyncExecution(CommandSender sender, Method method, Object instance, String[] args) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> executeSync(sender, method, instance, args));
    }

    private void executeSync(CommandSender sender, Method method, Object instance, String[] args) {
        try {
            methodInvoker.invoke(sender, method, instance, args);
        } catch (Exception e) {
            errorHandler.handleInternalError(sender, e).ifPresent(sender::sendMessage);
        }
    }
}

