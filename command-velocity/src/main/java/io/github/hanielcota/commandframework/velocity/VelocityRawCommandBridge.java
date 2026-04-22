package io.github.hanielcota.commandframework.velocity;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import io.github.hanielcota.commandframework.core.CommandDispatcher;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

final class VelocityRawCommandBridge implements RawCommand {

    private final CommandDispatcher dispatcher;
    private final Cache<CommandSource, VelocityCommandActor> actorCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(5))
            .build();

    VelocityRawCommandBridge(CommandDispatcher dispatcher) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
    }

    @Override
    public void execute(Invocation invocation) {
        dispatcher.dispatch(
                actorFor(invocation.source()),
                invocation.alias(),
                tokenize(invocation.arguments()));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return dispatcher.suggest(
                actorFor(invocation.source()),
                invocation.alias(),
                tokenize(invocation.arguments()));
    }

    static List<String> tokenize(String arguments) {
        String normalized = Objects.requireNonNull(arguments, "arguments").trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        return List.of(normalized.split("\\s+"));
    }

    private VelocityCommandActor actorFor(CommandSource source) {
        return actorCache.get(source, VelocityCommandActor::new);
    }
}
