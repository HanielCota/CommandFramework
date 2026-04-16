package io.github.hanielcota.commandframework;

import io.github.hanielcota.commandframework.internal.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Shared command runtime used by platform integrations.
 *
 * @param <S> the native sender type
 */
public final class CommandFramework<S> {

    private final PlatformBridge<S> bridge;
    private final List<RegisteredCommand> registeredCommands;
    private final CommandDispatcher dispatcher;
    private final MessageService messages;
    private final RateLimiter rateLimiter;

    /**
     * Creates a new runtime.
     *
     * @param bridge             the platform bridge
     * @param registeredCommands the grouped command registrations
     * @param dispatcher         the dispatcher
     * @param messages           the message service
     * @param rateLimiter        the global rate limiter
     */
    public CommandFramework(
            PlatformBridge<S> bridge,
            List<RegisteredCommand> registeredCommands,
            CommandDispatcher dispatcher,
            MessageService messages,
            RateLimiter rateLimiter
    ) {
        this.bridge = Objects.requireNonNull(bridge, "bridge");
        this.registeredCommands = List.copyOf(Objects.requireNonNull(registeredCommands, "registeredCommands"));
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
    }

    /**
     * Dispatches a command label with the supplied raw arguments.
     *
     * @param sender       the native sender
     * @param label        the command label
     * @param rawArguments the raw argument string
     * @return the command result
     */
    public CommandResult dispatch(S sender, String label, String rawArguments) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(rawArguments, "rawArguments");

        CommandActor actor = Objects.requireNonNull(
                this.bridge.createActor(sender), "PlatformBridge.createActor() must not return null");
        if (this.rateLimiter.shouldSilence(actor)) {
            return new CommandResult.RateLimited();
        }
        return this.dispatcher.dispatch(actor, label, rawArguments);
    }

    /**
     * Returns tab-completion suggestions for the supplied command input.
     *
     * @param sender       the native sender
     * @param label        the command label
     * @param rawArguments the raw argument string
     * @return the suggestion list
     */
    public List<String> suggest(S sender, String label, String rawArguments) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(rawArguments, "rawArguments");

        CommandActor actor = Objects.requireNonNull(
                this.bridge.createActor(sender), "PlatformBridge.createActor() must not return null");
        return this.dispatcher.suggest(actor, label, rawArguments);
    }

    /**
     * Returns the registered top-level command names and aliases.
     *
     * @return the registered command labels
     */
    public Set<String> commandLabels() {
        return this.dispatcher.commandLabels();
    }

    /**
     * Returns the grouped top-level command registrations.
     *
     * @return the grouped command registrations
     */
    public List<RegisteredCommand> registeredCommands() {
        return this.registeredCommands;
    }

    /**
     * Returns the auto-registered confirmation command names.
     *
     * @return the confirmation command names
     */
    public Set<String> confirmationCommandLabels() {
        return this.dispatcher.confirmationCommandLabels();
    }

    /**
     * Returns the message service used by the runtime.
     *
     * @return the message service
     */
    public MessageService messageService() {
        return this.messages;
    }

    /**
     * Returns the internal confirmation manager for advanced integrations.
     *
     * @return the confirmation manager
     */
    public ConfirmationManager confirmationManager() {
        return this.dispatcher.confirmationManager();
    }

    /**
     * Returns the internal cooldown manager for advanced integrations.
     *
     * @return the cooldown manager
     */
    public CooldownManager cooldownManager() {
        return this.dispatcher.cooldownManager();
    }
}
