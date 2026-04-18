package io.github.hanielcota.commandframework.paper;

import io.github.hanielcota.commandframework.ArgumentResolver;
import io.github.hanielcota.commandframework.CommandActor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.TooManyStaticImports")
class PaperPlatformBridgeTest {

    @Mock
    private JavaPlugin plugin;

    @Mock
    private Server server;

    @Mock
    private Logger logger;

    private PaperPlatformBridge bridge;

    @BeforeEach
    void setUp() {
        lenient().when(this.plugin.getServer()).thenReturn(this.server);
        lenient().when(this.plugin.getLogger()).thenReturn(this.logger);
        this.bridge = new PaperPlatformBridge(this.plugin);
    }

    @Test
    @DisplayName("classLoader returns the plugin class loader")
    void classLoaderReturnsPluginClassLoader() {
        assertSame(this.plugin.getClass().getClassLoader(), this.bridge.classLoader());
    }

    @Test
    @DisplayName("defaultScanPackage returns the plugin package name")
    void defaultScanPackageReturnsPluginPackage() {
        assertEquals(this.plugin.getClass().getPackageName(), this.bridge.defaultScanPackage());
    }

    @Test
    @DisplayName("logger returns the plugin logger")
    void loggerReturnsPluginLogger() {
        assertNotNull(this.bridge.logger());
    }

    @Test
    @DisplayName("supportsSenderType accepts CommandActor, CommandSender and Player")
    void supportsSenderTypeAcceptsSupportedTypes() {
        assertTrue(this.bridge.supportsSenderType(CommandActor.class));
        assertTrue(this.bridge.supportsSenderType(CommandSender.class));
        assertTrue(this.bridge.supportsSenderType(Player.class));
    }

    @Test
    @DisplayName("supportsSenderType rejects unrelated types")
    void supportsSenderTypeRejectsOtherTypes() {
        assertFalse(this.bridge.supportsSenderType(String.class));
        assertFalse(this.bridge.supportsSenderType(Object.class));
    }

    @Test
    @DisplayName("isPlayerSenderType is true only for Player")
    void isPlayerSenderTypeMatchesPlayerOnly() {
        assertTrue(this.bridge.isPlayerSenderType(Player.class));
        assertFalse(this.bridge.isPlayerSenderType(CommandSender.class));
        assertFalse(this.bridge.isPlayerSenderType(CommandActor.class));
    }

    @Test
    @DisplayName("createActor wraps a non-player sender into a non-player actor")
    void createActorWrapsNonPlayerSender() {
        CommandSender sender = org.mockito.Mockito.mock(CommandSender.class);
        when(sender.getName()).thenReturn("CONSOLE");
        when(sender.hasPermission("x")).thenReturn(true);

        CommandActor actor = this.bridge.createActor(sender);

        assertNotNull(actor);
        assertEquals("CONSOLE", actor.name());
        assertFalse(actor.isPlayer());
        assertTrue(actor.hasPermission("x"));
        assertTrue(actor.isAvailable());
        assertSame(sender, actor.platformSender());
        assertNotNull(actor.uniqueId());
    }

    @Test
    @DisplayName("createActor keeps non-player identities distinct even when names match")
    void createActorKeepsDistinctIdsForNonPlayersWithSameName() {
        CommandSender first = org.mockito.Mockito.mock(CommandSender.class);
        CommandSender second = org.mockito.Mockito.mock(CommandSender.class);
        when(first.getName()).thenReturn("CONSOLE");
        when(second.getName()).thenReturn("CONSOLE");

        CommandActor firstActor = this.bridge.createActor(first);
        CommandActor secondActor = this.bridge.createActor(second);

        assertNotEquals(firstActor.uniqueId(), secondActor.uniqueId());
    }

    @Test
    @DisplayName("createActor wraps a Player sender into a player actor")
    void createActorWrapsPlayerSender() {
        Player player = org.mockito.Mockito.mock(Player.class);
        java.util.UUID uuid = java.util.UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.isOnline()).thenReturn(true);

        CommandActor actor = this.bridge.createActor(player);

        assertTrue(actor.isPlayer());
        assertEquals(uuid, actor.uniqueId());
        assertTrue(actor.isAvailable());
    }

    @Test
    @DisplayName("createActor reports offline player as unavailable")
    void createActorReportsOfflinePlayerUnavailable() {
        Player player = org.mockito.Mockito.mock(Player.class);
        when(player.isOnline()).thenReturn(false);

        CommandActor actor = this.bridge.createActor(player);

        assertFalse(actor.isAvailable());
    }

    @Test
    @DisplayName("platformResolvers exposes player and world resolvers")
    void platformResolversExposesPlayerAndWorld() {
        List<ArgumentResolver<?>> resolvers = this.bridge.platformResolvers();

        assertEquals(2, resolvers.size());
        assertTrue(resolvers.stream().anyMatch(r -> r.type() == org.bukkit.entity.Player.class));
        assertTrue(resolvers.stream().anyMatch(r -> r.type() == org.bukkit.World.class));
    }
}
