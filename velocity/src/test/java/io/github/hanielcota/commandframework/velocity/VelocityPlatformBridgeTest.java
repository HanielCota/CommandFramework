package io.github.hanielcota.commandframework.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.hanielcota.commandframework.ArgumentResolver;
import io.github.hanielcota.commandframework.CommandActor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VelocityPlatformBridgeTest {

    private final Object plugin = new TestPlugin();
    @Mock
    private ProxyServer server;
    private VelocityPlatformBridge bridge;

    @BeforeEach
    void setUp() {
        this.bridge = new VelocityPlatformBridge(this.server, this.plugin);
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
    @DisplayName("logger is named after the plugin class")
    void loggerIsNamedAfterPluginClass() {
        assertNotNull(this.bridge.logger());
        assertEquals(this.plugin.getClass().getName(), this.bridge.logger().getName());
    }

    @Test
    @DisplayName("supportsSenderType accepts CommandActor, CommandSource and Player")
    void supportsSenderTypeAcceptsSupportedTypes() {
        assertTrue(this.bridge.supportsSenderType(CommandActor.class));
        assertTrue(this.bridge.supportsSenderType(CommandSource.class));
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
        assertFalse(this.bridge.isPlayerSenderType(CommandSource.class));
        assertFalse(this.bridge.isPlayerSenderType(CommandActor.class));
    }

    @Test
    @DisplayName("createActor wraps a non-player source into a non-player actor")
    void createActorWrapsNonPlayerSource() {
        CommandSource source = org.mockito.Mockito.mock(CommandSource.class);
        lenient().when(source.hasPermission("x")).thenReturn(true);

        CommandActor actor = this.bridge.createActor(source);

        assertNotNull(actor);
        assertFalse(actor.isPlayer());
        assertTrue(actor.hasPermission("x"));
        assertTrue(actor.isAvailable());
        assertSame(source, actor.platformSender());
        assertNotNull(actor.uniqueId());
        assertNotNull(actor.name());
    }

    @Test
    @DisplayName("createActor wraps a Player source into a player actor")
    void createActorWrapsPlayerSource() {
        Player player = org.mockito.Mockito.mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getUsername()).thenReturn("alice");
        when(player.isActive()).thenReturn(true);

        CommandActor actor = this.bridge.createActor(player);

        assertTrue(actor.isPlayer());
        assertEquals(uuid, actor.uniqueId());
        assertEquals("alice", actor.name());
        assertTrue(actor.isAvailable());
    }

    @Test
    @DisplayName("createActor reports inactive player as unavailable")
    void createActorReportsInactivePlayerUnavailable() {
        Player player = org.mockito.Mockito.mock(Player.class);
        when(player.isActive()).thenReturn(false);

        CommandActor actor = this.bridge.createActor(player);

        assertFalse(actor.isAvailable());
    }

    @Test
    @DisplayName("platformResolvers exposes the player resolver only")
    void platformResolversExposesPlayerResolver() {
        List<ArgumentResolver<?>> resolvers = this.bridge.platformResolvers();

        assertEquals(1, resolvers.size());
        assertSame(Player.class, resolvers.get(0).type());
    }

    private static final class TestPlugin {
    }
}
