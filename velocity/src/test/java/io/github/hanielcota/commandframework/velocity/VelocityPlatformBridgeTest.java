package io.github.hanielcota.commandframework.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.hanielcota.commandframework.ArgumentResolver;
import io.github.hanielcota.commandframework.CommandActor;
import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Execute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.function.BiPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.TooManyStaticImports", "EffectivelyPrivate", "UnusedMethod"})
class VelocityPlatformBridgeTest {

    private final Object plugin = new PluginStub();
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
    @DisplayName("createActor keeps non-player identities distinct even when source classes match")
    void createActorKeepsDistinctIdsForNonPlayers() {
        CommandSource first = org.mockito.Mockito.mock(CommandSource.class);
        CommandSource second = org.mockito.Mockito.mock(CommandSource.class);

        CommandActor firstActor = this.bridge.createActor(first);
        CommandActor secondActor = this.bridge.createActor(second);

        assertNotEquals(firstActor.uniqueId(), secondActor.uniqueId());
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

    @Test
    @DisplayName("player resolver honors the custom visibility filter during suggest")
    void playerResolverAppliesVisibilityFilter() {
        Player visible = org.mockito.Mockito.mock(Player.class);
        when(visible.isActive()).thenReturn(true);
        when(visible.getUsername()).thenReturn("alice");

        Player hidden = org.mockito.Mockito.mock(Player.class);
        when(hidden.isActive()).thenReturn(true);
        when(hidden.getUsername()).thenReturn("alice-shadow");

        when(this.server.getAllPlayers()).thenReturn(List.of(visible, hidden));

        BiPredicate<CommandActor, Player> hideShadow = (actor, target) -> !target.getUsername().endsWith("-shadow");
        VelocityPlatformBridge filteredBridge = new VelocityPlatformBridge(this.server, this.plugin, hideShadow);

        @SuppressWarnings("unchecked")
        ArgumentResolver<Player> resolver = (ArgumentResolver<Player>) filteredBridge.platformResolvers().get(0);
        CommandActor caller = org.mockito.Mockito.mock(CommandActor.class);

        List<String> suggestions = resolver.suggest(caller, "ali");

        assertEquals(List.of("alice"), suggestions);
    }

    @Test
    @DisplayName("default visibility filter suggests every online player")
    void defaultVisibilityFilterIncludesAllPlayers() {
        Player alice = org.mockito.Mockito.mock(Player.class);
        when(alice.isActive()).thenReturn(true);
        when(alice.getUsername()).thenReturn("alice");

        Player bob = org.mockito.Mockito.mock(Player.class);
        when(bob.isActive()).thenReturn(true);
        when(bob.getUsername()).thenReturn("alicia");

        when(this.server.getAllPlayers()).thenReturn(List.of(alice, bob));

        @SuppressWarnings("unchecked")
        ArgumentResolver<Player> resolver = (ArgumentResolver<Player>) this.bridge.platformResolvers().get(0);
        CommandActor caller = org.mockito.Mockito.mock(CommandActor.class);

        List<String> suggestions = resolver.suggest(caller, "ali");

        assertEquals(List.of("alice", "alicia"), suggestions);
    }

    @Test
    @DisplayName("register fails fast when a command label is already taken")
    void registerFailsFastOnCommandCollision() {
        com.velocitypowered.api.command.CommandManager manager =
                org.mockito.Mockito.mock(com.velocitypowered.api.command.CommandManager.class);
        when(this.server.getCommandManager()).thenReturn(manager);
        when(manager.hasCommand("taken")).thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> VelocityCommandFramework.velocity(this.server, this.plugin)
                        .command(new TakenCommand())
                        .build()
        );

        assertTrue(exception.getMessage().contains("taken"));
    }

    private static final class PluginStub {
    }

    @Command(name = "taken")
    private static final class TakenCommand {
        @Execute
        public void execute() {
        }
    }
}
