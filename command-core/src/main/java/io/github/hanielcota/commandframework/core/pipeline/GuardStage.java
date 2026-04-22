package io.github.hanielcota.commandframework.core.pipeline;

import io.github.hanielcota.commandframework.core.CommandContext;
import io.github.hanielcota.commandframework.core.CommandLogger;
import io.github.hanielcota.commandframework.core.CommandMessenger;
import io.github.hanielcota.commandframework.core.CommandResult;
import io.github.hanielcota.commandframework.core.SenderRequirement;
import io.github.hanielcota.commandframework.core.cooldown.CooldownClaim;
import io.github.hanielcota.commandframework.core.cooldown.RouteCooldownStore;
import java.util.Objects;

/**
 * Guards the dispatch: permission, sender requirement, cooldown.
 */
public final class GuardStage implements CommandDispatchStage {

    private final RouteCooldownStore cooldownStore;
    private final CommandMessenger messenger;
    private final CommandLogger logger;

    public GuardStage(RouteCooldownStore cooldownStore, CommandMessenger messenger, CommandLogger logger) {
        this.cooldownStore = Objects.requireNonNull(cooldownStore, "cooldownStore");
        this.messenger = Objects.requireNonNull(messenger, "messenger");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public CommandResult process(CommandContext context, DispatchContinuation continuation) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(continuation, "continuation");
        CommandResult permission = checkPermission(context);
        if (!permission.isSuccess()) {
            logger.debug("Guard denied permission: actor=%s, route=%s".formatted(context.actor().uniqueId(), context.route().canonicalPath()));
            return permission;
        }
        CommandResult sender = checkSender(context);
        if (!sender.isSuccess()) {
            logger.debug("Guard denied sender: actor=%s, route=%s, expected=%s".formatted(context.actor().uniqueId(), context.route().canonicalPath(), context.route().senderRequirement()));
            return sender;
        }
        CommandResult cooldown = checkCooldown(context);
        if (!cooldown.isSuccess()) {
            logger.debug("Guard denied cooldown: actor=%s, route=%s".formatted(context.actor().uniqueId(), context.route().canonicalPath()));
            return cooldown;
        }
        return continuation.proceed(context);
    }

    private CommandResult checkPermission(CommandContext context) {
        if (!context.route().hasPermission() || context.actor().hasPermission(context.route().permission())) {
            return CommandResult.success();
        }
        return messenger.noPermission(context);
    }

    private CommandResult checkSender(CommandContext context) {
        SenderRequirement expected = context.route().senderRequirement();
        if (expected.allows(context.actor())) {
            return CommandResult.success();
        }
        return messenger.invalidSender(context);
    }

    /**
     * Checks the cooldown and claims it eagerly.
     *
     * <p>The cooldown is claimed before the executor runs, which means a failed
     * execution still consumes the cooldown. This is intentional to prevent
     * cooldown bypass through repeated failed attempts, and aligns with the
     * common pattern in game command frameworks.</p>
     */
    private CommandResult checkCooldown(CommandContext context) {
        CooldownClaim claim = cooldownStore.claim(context.actor(), context.route());
        if (claim.isAllowed()) {
            return CommandResult.success();
        }
        return messenger.cooldown(context, claim.remaining());
    }
}
