package io.github.hanielcota.commandframework.paper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.hanielcota.commandframework.core.CommandDispatcher;
import io.github.hanielcota.commandframework.core.CommandRoot;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

final class PaperCommandBridge extends Command {

    private final CommandDispatcher dispatcher;
    private final @Nullable Plugin plugin;
    private final Cache<CommandSender, PaperCommandActor> actorCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(5))
            .build();

    PaperCommandBridge(CommandRoot root, CommandDispatcher dispatcher, @Nullable Plugin plugin) {
        super(Objects.requireNonNull(root, "root").label());
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.plugin = plugin;
        setAliases(new ArrayList<>(root.aliases()));
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        dispatcher.dispatch(actorFor(sender), label, args);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return dispatcher.suggest(actorFor(sender), alias, args);
    }

    private PaperCommandActor actorFor(CommandSender sender) {
        return actorCache.get(sender, s -> new PaperCommandActor(s, plugin));
    }
}
