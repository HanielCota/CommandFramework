package io.github.hanielcota.commandframework.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable definition of a command route.
 *
 * <p>Created via {@link Builder}. A route has a root literal, optional path
 * segments, parameters, permission, sender restriction, cooldown and the
 * {@link CommandExecutor} that implements its logic.</p>
 */
public final class CommandRoute {

    private final String root;
    private final Set<String> aliases;
    private final List<String> path;
    private final String permission;
    private final SenderRequirement senderRequirement;
    private final Duration cooldown;
    private final List<CommandParameter<?>> parameters;
    private final CommandExecutor executor;
    private final String description;
    private final String syntax;
    private final List<CommandInterceptor> interceptors;
    private final boolean async;

    private CommandRoute(Builder builder) {
        this.root = builder.root;
        this.aliases = Collections.unmodifiableSet(new LinkedHashSet<>(builder.aliases));
        this.path = List.copyOf(builder.path);
        this.permission = builder.permission;
        this.senderRequirement = builder.senderRequirement;
        this.cooldown = builder.cooldown;
        this.parameters = List.copyOf(builder.parameters);
        this.executor = builder.executor;
        this.description = builder.description;
        this.syntax = builder.syntax;
        this.interceptors = List.copyOf(builder.interceptors);
        this.async = builder.async;
    }

    public static Builder builder(String root, CommandExecutor executor) {
        return new Builder(root, executor);
    }

    public String root() {
        return root;
    }

    public Set<String> aliases() {
        return aliases;
    }

    public List<String> path() {
        return path;
    }

    public String permission() {
        return permission;
    }

    public SenderRequirement senderRequirement() {
        return senderRequirement;
    }

    public Duration cooldown() {
        return cooldown;
    }

    public List<CommandParameter<?>> parameters() {
        return parameters;
    }

    public CommandExecutor executor() {
        return executor;
    }

    /** Returns the human-readable description, or empty string if none. */
    public String description() {
        return description;
    }

    /** Returns the explicit usage syntax, or empty string if auto-derived. */
    public String syntax() {
        return syntax;
    }

    /** Returns interceptors scoped to this route. */
    public List<CommandInterceptor> interceptors() {
        return interceptors;
    }

    /** Whether this route should be executed asynchronously.
     *
     * <p><strong>Warning for Paper/Bukkit:</strong> When {@code true}, the entire
     * dispatch pipeline runs off the main thread. Command executors that call
     * Bukkit/Paper APIs ({@code Player}, {@code World}, {@code Inventory}, etc.)
     * must schedule work back to the main thread explicitly. See the
     * {@code @Async} annotation documentation for details.</p>
     */
    public boolean async() {
        return async;
    }

    public String canonicalPath() {
        if (path.isEmpty()) {
            return root;
        }
        return root + " " + String.join(" ", path);
    }

    public boolean hasPermission() {
        return !permission.isBlank();
    }

    public boolean hasCooldown() {
        return !cooldown.isZero() && !cooldown.isNegative();
    }

    public static final class Builder {

        private final String root;
        private final CommandExecutor executor;
        private final Set<String> aliases = new LinkedHashSet<>();
        private final List<String> path = new ArrayList<>();
        private final List<CommandParameter<?>> parameters = new ArrayList<>();
        private String permission = "";
        private SenderRequirement senderRequirement = SenderRequirement.ANY;
        private Duration cooldown = Duration.ZERO;
        private String description = "";
        private String syntax = "";
        private final List<CommandInterceptor> interceptors = new ArrayList<>();
        private boolean async = false;

        private Builder(String root, CommandExecutor executor) {
            this.root = Objects.requireNonNull(root, "root");
            this.executor = Objects.requireNonNull(executor, "executor");
        }

        public Builder aliases(Set<String> aliases) {
            Objects.requireNonNull(aliases, "aliases");
            this.aliases.clear();
            this.aliases.addAll(aliases);
            return this;
        }

        public Builder path(List<String> path) {
            Objects.requireNonNull(path, "path");
            this.path.clear();
            this.path.addAll(path);
            return this;
        }

        public Builder permission(String permission) {
            this.permission = Objects.requireNonNull(permission, "permission");
            return this;
        }

        public Builder senderRequirement(SenderRequirement senderRequirement) {
            this.senderRequirement = Objects.requireNonNull(senderRequirement, "senderRequirement");
            return this;
        }

        public Builder cooldown(Duration cooldown) {
            Objects.requireNonNull(cooldown, "cooldown");
            if (cooldown.isNegative()) {
                throw new IllegalArgumentException("Invalid cooldown: expected zero or positive duration");
            }
            this.cooldown = cooldown;
            return this;
        }

        public Builder parameters(List<CommandParameter<?>> parameters) {
            Objects.requireNonNull(parameters, "parameters");
            this.parameters.clear();
            this.parameters.addAll(parameters);
            return this;
        }

        public Builder description(String description) {
            this.description = Objects.requireNonNull(description, "description");
            return this;
        }

        public Builder syntax(String syntax) {
            this.syntax = Objects.requireNonNull(syntax, "syntax");
            return this;
        }

        public Builder interceptor(CommandInterceptor interceptor) {
            this.interceptors.add(Objects.requireNonNull(interceptor, "interceptor"));
            return this;
        }

        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        public CommandRoute build() {
            CommandRouteValidator.validate(root, aliases, path, executor);
            CommandRouteValidator.validateGreedyPosition(parameters);
            return new CommandRoute(this);
        }
    }
}
