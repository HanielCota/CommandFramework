package io.github.hanielcota.commandframework;

import io.github.hanielcota.commandframework.annotation.Async;
import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Confirm;
import io.github.hanielcota.commandframework.annotation.Cooldown;
import io.github.hanielcota.commandframework.annotation.Description;
import io.github.hanielcota.commandframework.annotation.Execute;
import io.github.hanielcota.commandframework.annotation.Inject;
import io.github.hanielcota.commandframework.annotation.Optional;
import io.github.hanielcota.commandframework.annotation.Permission;
import io.github.hanielcota.commandframework.annotation.RequirePlayer;
import io.github.hanielcota.commandframework.annotation.Sender;
import io.github.hanielcota.commandframework.internal.MessageService;
import io.github.hanielcota.commandframework.scanfixtures.ManualFixtureCommand;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"DoNotCallSuggester", "EffectivelyPrivate", "UnusedMethod", "UnusedVariable"})
class CommandFrameworkCoreTest {

    private TestEnvironment environment;

    @BeforeEach
    void setUp() {
        this.environment = new TestEnvironment();
    }

    @Test
    void rootCommandExecutes() {
        RootCommand command = new RootCommand();
        CommandFramework<TestSender> framework = this.framework(command);

        CommandResult result = framework.dispatch(this.environment.player("Alice"), "root", "");

        assertInstanceOf(CommandResult.Success.class, result);
        assertEquals(1, command.calls.get());
    }

    @Test
    void subcommandExecutesAndInfersParameterName() {
        SubCommand command = new SubCommand();
        CommandFramework<TestSender> framework = this.framework(command);

        CommandResult result = framework.dispatch(this.environment.player("Alice"), "profile", "set Haniel");

        assertInstanceOf(CommandResult.Success.class, result);
        assertEquals("Haniel", command.lastUsername);
    }

    @Test
    void unexpectedExtraArgumentsAreRejected() {
        RootCommand command = new RootCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");

        CommandResult result = framework.dispatch(player, "root", "extra");

        assertInstanceOf(CommandResult.Handled.class, result);
        assertEquals(0, command.calls.get());
        assertTrue(player.lastMessage().contains("Too many arguments"));
    }

    @Test
    void classPermissionAppliesAndMethodPermissionOverrides() {
        PermissionCommand command = new PermissionCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");
        player.grant("perm.admin");

        CommandResult rootResult = framework.dispatch(player, "perm", "");
        CommandResult adminResult = framework.dispatch(player, "perm", "admin");

        assertInstanceOf(CommandResult.NoPermission.class, rootResult);
        assertInstanceOf(CommandResult.Success.class, adminResult);
        assertEquals(0, command.rootCalls.get());
        assertEquals(1, command.adminCalls.get());
    }

    @Test
    void cooldownBlocksReexecutionAndBypassWorks() {
        CooldownCommand command = new CooldownCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");

        CommandResult first = framework.dispatch(player, "cool", "");
        CommandResult second = framework.dispatch(player, "cool", "");

        assertInstanceOf(CommandResult.Success.class, first);
        assertInstanceOf(CommandResult.CooldownActive.class, second);
        assertEquals(1, command.calls.get());
        assertTrue(player.lastMessage().contains("wait"));

        TestPlayer bypass = this.environment.player("Bob");
        bypass.grant("cool.bypass");
        framework.dispatch(bypass, "cool", "");
        framework.dispatch(bypass, "cool", "");
        assertEquals(3, command.calls.get());
    }

    @Test
    void cooldownIsNotConsumedWhenArgumentValidationFails() {
        CooldownArgCommand command = new CooldownArgCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");

        CommandResult missing = framework.dispatch(player, "coolarg", "");
        assertInstanceOf(CommandResult.Handled.class, missing);
        assertTrue(player.lastMessage().contains("Missing argument"));

        CommandResult valid = framework.dispatch(player, "coolarg", "42");
        assertInstanceOf(CommandResult.Success.class, valid);
        assertEquals(1, command.calls.get());

        CommandResult blocked = framework.dispatch(player, "coolarg", "42");
        assertInstanceOf(CommandResult.CooldownActive.class, blocked);
    }

    @Test
    void confirmationCreatesPendingAndExecutes() {
        ConfirmCommand command = new ConfirmCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");

        CommandResult pending = framework.dispatch(player, "danger", "reset target");
        CommandResult confirmed = framework.dispatch(player, "confirm", "");

        assertInstanceOf(CommandResult.PendingConfirmation.class, pending);
        assertInstanceOf(CommandResult.Success.class, confirmed);
        assertEquals(1, command.calls.get());
        assertEquals("target", command.lastTarget);
    }

    @Test
    void confirmationRechecksPermissionBeforeExecution() {
        ProtectedConfirmCommand command = new ProtectedConfirmCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");
        player.grant("secure.use");

        CommandResult pending = framework.dispatch(player, "secure", "");
        player.revoke("secure.use");
        CommandResult confirmed = framework.dispatch(player, "confirm", "");

        assertInstanceOf(CommandResult.PendingConfirmation.class, pending);
        assertInstanceOf(CommandResult.NoPermission.class, confirmed);
        assertEquals(0, command.calls.get());
        assertTrue(player.lastMessage().contains("permission"));
    }

    @Test
    void helpListsVisibleSubcommands() {
        HelpCommand command = new HelpCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");
        player.grant("help.user");

        CommandResult result = framework.dispatch(player, "helpme", "");

        assertInstanceOf(CommandResult.HelpShown.class, result);
        assertTrue(player.lastMessage().contains("helpme user"));
        assertFalse(player.lastMessage().contains("helpme admin"));
    }

    @Test
    void fromStringMapOverridesKnownKeysAndFallsBackForMissing() {
        MessageProvider provider = MessageProvider.fromStringMap(java.util.Map.of(
                "no-permission", "<red>custom nope",
                "unknown-key-ignored", "whatever"));

        assertEquals("<red>custom nope", provider.message(MessageKey.NO_PERMISSION));
        assertEquals(MessageProvider.defaults().message(MessageKey.PLAYER_ONLY),
                provider.message(MessageKey.PLAYER_ONLY));
    }

    @Test
    void unknownSubcommandSuggestsClosestMatch() {
        HelpCommand command = new HelpCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");
        player.grant("help.user");

        CommandResult result = framework.dispatch(player, "helpme", "usr");

        assertInstanceOf(CommandResult.HelpShown.class, result);
        assertTrue(
                player.messages.stream().anyMatch(message -> message.contains("user") && message.contains("usr")),
                "Expected a did-you-mean message suggesting 'user' for typo 'usr', got: " + player.messages);
    }

    @Test
    void unknownSubcommandSuggestionSkippedForVeryShortTypo() {
        // Two-char typed tokens are within edit distance 2 of almost any short name,
        // so did-you-mean would produce noise. Skip entirely below length 3.
        // "zx" is chosen so the typo token does not appear as a substring inside the help listing.
        HelpCommand command = new HelpCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");
        player.grant("help.user");

        CommandResult result = framework.dispatch(player, "helpme", "zx");

        assertInstanceOf(CommandResult.HelpShown.class, result);
        assertFalse(
                player.messages.stream().anyMatch(message -> message.contains("zx")),
                "Expected no did-you-mean suggestion for 2-char typo, got: " + player.messages);
    }

    @Test
    void unknownSubcommandSuggestionHidesInaccessibleCommands() {
        HelpCommand command = new HelpCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");
        player.grant("help.user");

        CommandResult result = framework.dispatch(player, "helpme", "admn");

        assertInstanceOf(CommandResult.HelpShown.class, result);
        assertFalse(
                player.messages.stream().anyMatch(message -> message.contains("admin") && message.contains("admn")),
                "Expected typo suggestions to ignore inaccessible subcommands, got: " + player.messages
        );
    }

    @Test
    void optionalDefaultAndVoidReturnWork() {
        OptionalCommand command = new OptionalCommand();
        CommandFramework<TestSender> framework = this.framework(command);

        CommandResult result = framework.dispatch(this.environment.player("Alice"), "optional", "");

        assertInstanceOf(CommandResult.Success.class, result);
        assertTrue(command.lastSilent);
    }

    @Test
    void asyncInvokesOnVirtualThread() throws InterruptedException {
        AsyncCommand command = new AsyncCommand();
        CommandFramework<TestSender> framework = this.framework(command);

        CommandResult result = framework.dispatch(this.environment.player("Alice"), "async", "");

        assertInstanceOf(CommandResult.Success.class, result);
        assertTrue(command.latch.await(3, TimeUnit.SECONDS));
        assertTrue(command.virtualThreadSeen);
    }

    @Test
    void asyncAllowsCommandActorSender() throws InterruptedException {
        AsyncActorCommand command = new AsyncActorCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");

        CommandResult result = framework.dispatch(player, "asyncactor", "");

        assertInstanceOf(CommandResult.Success.class, result);
        assertTrue(command.latch.await(3, TimeUnit.SECONDS));
        assertTrue(command.virtualThreadSeen);
        assertTrue(player.lastMessage().contains("async hello"));
    }

    @Test
    void tabCompleteReturnsSuggestionsFromResolver() {
        SuggestionCommand command = new SuggestionCommand();
        CommandFramework<TestSender> framework = this.framework(builder -> builder.resolver(new SuggestionValueResolver()), command);

        List<String> suggestions = framework.suggest(this.environment.player("Alice"), "suggest", "pick a");

        assertEquals(List.of("alpha"), suggestions);
    }

    @Test
    void rootArgumentSuggestionsAfterTrailingSpaceUseResolver() {
        RootSuggestionCommand command = new RootSuggestionCommand();
        CommandFramework<TestSender> framework =
                this.framework(builder -> builder.resolver(new SuggestionValueResolver()), command);

        List<String> suggestions = framework.suggest(this.environment.player("Alice"), "rootsuggest", " ");

        assertEquals(List.of("alpha", "beta"), suggestions);
    }

    @Test
    void rootArgumentSuggestionsAfterTrailingTabUseResolver() {
        // Tokenizer splits on \s+, so a tab-terminated input must be treated as "ready for next
        // token" just like a space-terminated one. Previously endsWith(" ") disagreed with the
        // split and would have returned suggestions for the PREVIOUS token.
        RootSuggestionCommand command = new RootSuggestionCommand();
        CommandFramework<TestSender> framework =
                this.framework(builder -> builder.resolver(new SuggestionValueResolver()), command);

        List<String> suggestions = framework.suggest(this.environment.player("Alice"), "rootsuggest", "\t");

        assertEquals(List.of("alpha", "beta"), suggestions);
    }

    @Test
    void tabCompleteStopsAfterLastArgument() {
        RootSuggestionCommand command = new RootSuggestionCommand();
        CommandFramework<TestSender> framework =
                this.framework(builder -> builder.resolver(new SuggestionValueResolver()), command);

        List<String> suggestions = framework.suggest(this.environment.player("Alice"), "rootsuggest", "alpha ");

        assertEquals(List.of(), suggestions);
    }

    @Test
    void invalidArgumentSendsMessageAndDoesNotExecute() {
        NumberCommand command = new NumberCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");

        CommandResult result = framework.dispatch(player, "number", "NaN");

        assertInstanceOf(CommandResult.InvalidArgs.class, result);
        assertEquals(0, command.calls.get());
        assertTrue(player.lastMessage().contains("Invalid value"));
    }

    @Test
    void missingArgumentSendsMessageAndDoesNotExecute() {
        NumberCommand command = new NumberCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");

        CommandResult result = framework.dispatch(player, "number", "");

        assertInstanceOf(CommandResult.Handled.class, result);
        assertEquals(0, command.calls.get());
        assertTrue(player.lastMessage().contains("Missing argument"));
    }

    @Test
    void requirePlayerBlocksConsole() {
        PlayerOnlyCommand command = new PlayerOnlyCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestConsole console = this.environment.console("Console");

        CommandResult result = framework.dispatch(console, "player", "");

        assertInstanceOf(CommandResult.PlayerOnly.class, result);
        assertEquals(0, command.calls.get());
        assertTrue(console.lastMessage().contains("Only players"));
    }

    @Test
    void methodLevelRequirePlayerOnlyAffectsThatExecutor() {
        MethodPlayerOnlyCommand command = new MethodPlayerOnlyCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestConsole console = this.environment.console("Console");

        CommandResult rootResult = framework.dispatch(console, "methodplayer", "");
        CommandResult playerOnlyResult = framework.dispatch(console, "methodplayer", "player");

        assertInstanceOf(CommandResult.Success.class, rootResult);
        assertInstanceOf(CommandResult.PlayerOnly.class, playerOnlyResult);
        assertEquals(1, command.rootCalls.get());
        assertEquals(0, command.playerCalls.get());
    }

    @Test
    void playerSenderParameterImplicitlyRequiresPlayer() {
        ConfirmCommand command = new ConfirmCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestConsole console = this.environment.console("Console");

        CommandResult result = framework.dispatch(console, "danger", "reset target");
        List<String> suggestions = framework.suggest(console, "danger", "");

        assertInstanceOf(CommandResult.PlayerOnly.class, result);
        assertEquals(List.of(), suggestions);
        assertEquals(0, command.calls.get());
        assertTrue(console.lastMessage().contains("Only players"));
    }

    @Test
    void noPermissionSendsMessageAndDoesNotExecute() {
        PermissionCommand command = new PermissionCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");

        CommandResult result = framework.dispatch(player, "perm", "");

        assertInstanceOf(CommandResult.NoPermission.class, result);
        assertEquals(0, command.rootCalls.get());
        assertTrue(player.lastMessage().contains("permission"));
    }

    @Test
    void confirmationExpires() throws InterruptedException {
        ExpiringConfirmCommand command = new ExpiringConfirmCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");

        framework.dispatch(player, "expire", "");
        Thread.sleep(1100L);
        framework.dispatch(player, "confirm", "");

        assertEquals(0, command.calls.get());
        assertTrue(player.lastMessage().contains("nothing pending"));
    }

    @Test
    void resolverMissingFailsFast() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> this.framework(new MissingResolverCommand()));
        assertTrue(exception.getMessage().contains("No resolver for type"));
    }

    @Test
    void cooldownDoubleClickAllowsOnlyOneExecution() throws Exception {
        CooldownCommand command = new CooldownCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> framework.dispatch(player, "cool", ""));
            var second = executor.submit(() -> framework.dispatch(player, "cool", ""));

            List<Class<?>> resultTypes = List.of(first.get().getClass(), second.get().getClass());
            assertTrue(resultTypes.contains(CommandResult.Success.class));
            assertTrue(resultTypes.contains(CommandResult.CooldownActive.class));
            assertEquals(1, command.calls.get());
        }
    }

    @Test
    void doubleConfirmExecutesOnlyOnce() throws Exception {
        ConfirmCommand command = new ConfirmCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");
        framework.dispatch(player, "danger", "reset once");

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> framework.dispatch(player, "confirm", ""));
            var second = executor.submit(() -> framework.dispatch(player, "confirm", ""));
            first.get();
            second.get();
        }

        assertEquals(1, command.calls.get());
    }

    @Test
    void confirmationOverrideKeepsOnlyNewestPending() {
        ConfirmCommand command = new ConfirmCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");

        framework.dispatch(player, "danger", "reset first");
        framework.dispatch(player, "danger", "reset second");
        framework.dispatch(player, "confirm", "");

        assertEquals(1, command.calls.get());
        assertEquals("second", command.lastTarget);
    }

    @Test
    void cooldownWithConfirmationDeferredUntilConfirm() {
        ConfirmCooldownCommand command = new ConfirmCooldownCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");

        CommandResult first = framework.dispatch(player, "armed", "");
        CommandResult second = framework.dispatch(player, "armed", "");
        CommandResult confirmed = framework.dispatch(player, "confirm", "");
        CommandResult afterConfirm = framework.dispatch(player, "armed", "");

        assertInstanceOf(CommandResult.PendingConfirmation.class, first);
        // Cooldown is not charged while the prompt is pending — a second dispatch just re-queues.
        assertInstanceOf(CommandResult.PendingConfirmation.class, second);
        assertInstanceOf(CommandResult.Success.class, confirmed);
        // Cooldown starts at the moment of successful confirmation, so the next dispatch is blocked.
        assertInstanceOf(CommandResult.CooldownActive.class, afterConfirm);
        assertEquals(1, command.calls.get());
    }

    @Test
    void cooldownNotConsumedWhenConfirmAbandoned() throws InterruptedException {
        BriefConfirmCooldownCommand command = new BriefConfirmCooldownCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");

        assertInstanceOf(CommandResult.PendingConfirmation.class, framework.dispatch(player, "armedbrief", ""));

        // Let the confirm window expire. Cooldown must not have been eaten by the abandoned prompt.
        Thread.sleep(1100L);

        assertInstanceOf(CommandResult.PendingConfirmation.class, framework.dispatch(player, "armedbrief", ""));
        assertEquals(0, command.calls.get());
    }

    @Test
    void tabCompleteWithoutPermissionReturnsEmptyList() {
        HelpCommand command = new HelpCommand();
        CommandFramework<TestSender> framework = this.framework(command);

        List<String> suggestions = framework.suggest(this.environment.player("Alice"), "helpme", "a");

        assertEquals(List.of(), suggestions);
    }

    @Test
    void exceptionInCommandSendsGenericMessage() {
        ThrowingCommand command = new ThrowingCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");

        CommandResult result = framework.dispatch(player, "boom", "");

        assertInstanceOf(CommandResult.Failure.class, result);
        assertTrue(player.lastMessage().contains("unexpected error"));
        assertFalse(player.lastMessage().contains("boom"));
    }

    @Test
    void resolverRuntimeFailureIsConvertedToGenericCommandError() {
        ExplodingResolverCommand command = new ExplodingResolverCommand();
        CommandFramework<TestSender> framework =
                this.framework(builder -> builder.resolver(new ExplodingValueResolver()), command);
        TestPlayer player = this.environment.player("Alice");

        CommandResult result = framework.dispatch(player, "explode", "kaboom");

        assertInstanceOf(CommandResult.Failure.class, result);
        assertEquals(0, command.calls.get());
        assertTrue(player.lastMessage().contains("unexpected error"));
        assertFalse(player.lastMessage().contains("kaboom"));
    }

    @Test
    void invalidPlayerNameIsRejected() {
        PlayerTargetCommand command = new PlayerTargetCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");

        CommandResult result = framework.dispatch(player, "target", "../etc");

        assertInstanceOf(CommandResult.InvalidArgs.class, result);
        assertEquals(0, command.calls.get());
    }

    @Test
    void playerCannotConfirmAnotherPlayersPendingAction() {
        ConfirmCommand command = new ConfirmCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer alice = this.environment.player("Alice");
        TestPlayer bob = this.environment.player("Bob");

        framework.dispatch(alice, "danger", "reset onlyAlice");
        framework.dispatch(bob, "confirm", "");

        assertEquals(0, command.calls.get());
        assertTrue(bob.lastMessage().contains("nothing pending"));
    }

    @Test
    void rateLimitSilencesOverage() {
        RootCommand command = new RootCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");

        CommandResult last = CommandResult.success();
        for (int index = 0; index < 31; index++) {
            last = framework.dispatch(player, "root", "");
        }

        assertInstanceOf(CommandResult.RateLimited.class, last);
        assertEquals(30, command.calls.get());
        assertEquals(0, player.messages.size());
    }

    @Test
    void missingInjectionBindingFailsFast() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> this.framework(new MissingBindingCommand()));
        assertTrue(exception.getMessage().contains("No binding for"));
    }

    @Test
    void ambiguousInjectionBindingFailsFast() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> this.framework(
                        builder -> {
                            builder.bind(AmbiguousImplA.class, new AmbiguousImplA());
                            builder.bind(AmbiguousImplB.class, new AmbiguousImplB());
                        },
                        new AmbiguousBindingCommand()
                )
        );
        assertTrue(exception.getMessage().contains("Multiple bindings match"));
    }

    @Test
    void manualCommandsStillUseDefaultPackageScan() {
        TestBuilder builder = new TestBuilder(new ScanBridge(this.environment));
        builder.bind(TestEnvironment.class, this.environment);
        builder.command(new ManualFixtureCommand());

        CommandFramework<TestSender> framework = builder.build();

        assertTrue(framework.commandLabels().contains("manualfixture"));
        assertTrue(framework.commandLabels().contains("scannedfixture"));
    }

    @Test
    void messagePlaceholdersEscapeMiniMessageTags() {
        MessageService messages = new MessageService(MessageProvider.defaults());
        String maliciousInput = "<click:run_command:'/op @s'>boom</click>";

        Component rendered = messages.render(MessageKey.INVALID_ARGUMENT, Map.of(
                "name", "amount",
                "input", maliciousInput
        ));

        assertFalse(this.hasClickEvent(rendered));
        assertTrue(this.plainText(rendered).contains(maliciousInput));
    }

    @Test
    void placeholderValueContainingAnotherPlaceholderKeyIsNotRecursivelyReplaced() {
        MessageService messages = new MessageService(MessageProvider.fromStringMap(Map.of(
                "no-permission", "A={a} B={b}")));

        Component rendered = messages.render(MessageKey.NO_PERMISSION, Map.of(
                "a", "{b}",
                "b", "VALUE"
        ));

        // Single-pass replacement: the value "{b}" substituted for {a} is NOT re-scanned for {b}.
        // Under the old cascading loop this output depended on Map.of iteration order.
        assertEquals("A={b} B=VALUE", this.plainText(rendered));
    }

    @Test
    void messageRenderWithNullPlaceholdersDoesNotThrow() {
        MessageService messages = new MessageService(MessageProvider.defaults());

        Component rendered = messages.render(MessageKey.NO_PERMISSION, null);

        assertNotNull(rendered);
    }

    @Test
    void tabCompletionAndDispatchAgreeOnPermissionForConsole() {
        // Regression guard: allowed() is now derived from preflight(), so a console trying to
        // run a @RequirePlayer subcommand must see an empty suggestion list AND a PlayerOnly
        // result. If the two predicates ever drift, one of these two assertions fails.
        MethodPlayerOnlyCommand command = new MethodPlayerOnlyCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestConsole console = this.environment.console("Console");

        List<String> suggestions = framework.suggest(console, "methodplayer", "pla");
        CommandResult result = framework.dispatch(console, "methodplayer", "player");

        assertFalse(suggestions.contains("player"),
                "Console must not see a @RequirePlayer subcommand in tab completion");
        assertInstanceOf(CommandResult.PlayerOnly.class, result);
    }

    @Test
    void placeholderPatternRejectsPathLikeKeys() {
        // Hardening: keys outside [a-zA-Z0-9_.-]{1,32} are no longer matched as placeholders,
        // so a template containing path-like or expression-like braces passes through verbatim.
        MessageService messages = new MessageService(MessageProvider.fromStringMap(Map.of(
                "no-permission", "root={../etc/passwd} env={$SECRET} ok={name}")));

        Component rendered = messages.render(MessageKey.NO_PERMISSION, Map.of(
                "../etc/passwd", "PWNED",
                "$SECRET", "PWNED",
                "name", "Alice"
        ));

        assertEquals("root={../etc/passwd} env={$SECRET} ok=Alice", this.plainText(rendered));
    }

    private CommandFramework<TestSender> framework(Object... commands) {
        return this.framework(builder -> {
        }, commands);
    }

    private CommandFramework<TestSender> framework(java.util.function.Consumer<TestBuilder> customizer, Object... commands) {
        TestBuilder builder = new TestBuilder(new TestBridge(this.environment));
        builder.bind(TestEnvironment.class, this.environment);
        builder.commands(commands);
        customizer.accept(builder);
        return builder.build();
    }

    private boolean hasClickEvent(Component component) {
        if (component.style().clickEvent() != null) {
            return true;
        }
        for (Component child : component.children()) {
            if (this.hasClickEvent(child)) {
                return true;
            }
        }
        return false;
    }

    private String plainText(Component component) {
        StringBuilder builder = new StringBuilder();
        this.appendPlain(component, builder);
        return builder.toString();
    }

    private void appendPlain(Component component, StringBuilder builder) {
        if (component instanceof net.kyori.adventure.text.TextComponent text) {
            builder.append(text.content());
        }
        component.children().forEach(child -> this.appendPlain(child, builder));
    }

    @Test
    void greedyArgumentConsumesAllRemainingTokens() {
        GreedyCommand command = new GreedyCommand();
        CommandFramework<TestSender> framework = this.framework(command);

        CommandResult result = framework.dispatch(this.environment.player("Alice"), "greedy", "hello world foo");

        assertInstanceOf(CommandResult.Success.class, result);
        assertEquals("hello world foo", command.lastMessage);
    }

    @Test
    void unknownTokenDoesNotFallThroughToRootWithoutArguments() {
        RootAndSubcommandCommand command = new RootAndSubcommandCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");

        CommandResult result = framework.dispatch(player, "mixed", "staus");

        assertInstanceOf(CommandResult.HelpShown.class, result);
        assertEquals(0, command.rootCalls.get());
        assertEquals(0, command.statusCalls.get());
        assertTrue(
                player.messages.stream().anyMatch(message -> message.contains("status") && message.contains("staus")),
                "Expected an unknown-subcommand suggestion for 'status', got: " + player.messages
        );
    }

    @Test
    void javaOptionalParameterEmptyWhenOmitted() {
        JavaOptionalCommand command = new JavaOptionalCommand();
        CommandFramework<TestSender> framework = this.framework(command);

        CommandResult result = framework.dispatch(this.environment.player("Alice"), "opttype", "");

        assertInstanceOf(CommandResult.Success.class, result);
        assertTrue(command.lastValue.isEmpty());
    }

    @Test
    void javaOptionalParameterPresentWhenProvided() {
        JavaOptionalCommand command = new JavaOptionalCommand();
        CommandFramework<TestSender> framework = this.framework(command);

        CommandResult result = framework.dispatch(this.environment.player("Alice"), "opttype", "hello");

        assertInstanceOf(CommandResult.Success.class, result);
        assertEquals("hello", command.lastValue.orElse(null));
    }

    @Test
    void javaOptionalParameterWithDefaultDoesNotDoubleWrap() {
        // Regression: previously defaultValue() re-wrapped an already-Optional resolved value,
        // yielding Optional<Optional<String>> which broke reflective invoke at runtime.
        JavaOptionalWithDefaultCommand command = new JavaOptionalWithDefaultCommand();
        CommandFramework<TestSender> framework = this.framework(command);

        CommandResult result = framework.dispatch(this.environment.player("Alice"), "optdefault", "");

        assertInstanceOf(CommandResult.Success.class, result);
        assertEquals("fallback", command.lastValue.orElse(null));
    }

    @Test
    void javaOptionalParameterWithDefaultUsesProvidedTokenWhenPresent() {
        JavaOptionalWithDefaultCommand command = new JavaOptionalWithDefaultCommand();
        CommandFramework<TestSender> framework = this.framework(command);

        CommandResult result = framework.dispatch(this.environment.player("Alice"), "optdefault", "provided");

        assertInstanceOf(CommandResult.Success.class, result);
        assertEquals("provided", command.lastValue.orElse(null));
    }

    @Test
    void maxLengthRejectsLongInput() {
        MaxLengthCommand command = new MaxLengthCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");

        CommandResult result = framework.dispatch(player, "maxlen", "toolong");

        assertInstanceOf(CommandResult.InvalidArgs.class, result);
        assertEquals(0, command.calls.get());
    }

    @Test
    void maxLengthAcceptsShortInput() {
        MaxLengthCommand command = new MaxLengthCommand();
        CommandFramework<TestSender> framework = this.framework(command);

        CommandResult result = framework.dispatch(this.environment.player("Alice"), "maxlen", "ok");

        assertInstanceOf(CommandResult.Success.class, result);
        assertEquals(1, command.calls.get());
    }

    @Test
    void enumResolverParsesAndSuggests() {
        EnumCommand command = new EnumCommand();
        CommandFramework<TestSender> framework = this.framework(command);

        CommandResult result = framework.dispatch(this.environment.player("Alice"), "color", "GREEN");
        assertInstanceOf(CommandResult.Success.class, result);
        assertEquals(Color.GREEN, command.lastColor);

        List<String> suggestions = framework.suggest(this.environment.player("Alice"), "color", "r");
        assertEquals(List.of("red"), suggestions);
    }

    @Test
    void injectionSucceedsWhenBindingPresent() {
        InjectedCommand command = new InjectedCommand();
        CommandFramework<TestSender> framework = this.framework(command);

        CommandResult result = framework.dispatch(this.environment.player("Alice"), "injected", "");

        assertInstanceOf(CommandResult.Success.class, result);
        assertEquals(1, command.calls.get());
    }

    @Test
    void asyncErrorEmitsFailureToActor() throws InterruptedException {
        AsyncThrowingCommand command = new AsyncThrowingCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");

        CommandResult result = framework.dispatch(player, "asyncboom", "");

        assertInstanceOf(CommandResult.Success.class, result);
        assertTrue(command.latch.await(3, TimeUnit.SECONDS));
        Thread.sleep(200L);
        assertTrue(player.lastMessage().contains("unexpected error"));
    }

    @Test
    void aliasExecutesCommand() {
        AliasedCommand command = new AliasedCommand();
        CommandFramework<TestSender> framework = this.framework(command);

        CommandResult byAlias = framework.dispatch(this.environment.player("Alice"), "al", "");
        CommandResult byAlias2 = framework.dispatch(this.environment.player("Alice"), "ali", "");

        assertInstanceOf(CommandResult.Success.class, byAlias);
        assertInstanceOf(CommandResult.Success.class, byAlias2);
        assertEquals(2, command.calls.get());
    }

    @Test
    void customConfirmationCommandNameWorks() {
        CustomConfirmCommand command = new CustomConfirmCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestPlayer player = this.environment.player("Alice");

        CommandResult pending = framework.dispatch(player, "customconfirm", "");
        assertInstanceOf(CommandResult.PendingConfirmation.class, pending);
        assertEquals("yesplease", ((CommandResult.PendingConfirmation) pending).commandName());

        CommandResult wrongConfirm = framework.dispatch(player, "confirm", "");
        assertInstanceOf(CommandResult.Handled.class, wrongConfirm);
        assertEquals(0, command.calls.get());

        CommandResult confirmed = framework.dispatch(player, "yesplease", "");
        assertInstanceOf(CommandResult.Success.class, confirmed);
        assertEquals(1, command.calls.get());
    }

    @Test
    void subcommandTabCompleteReturnsArgumentSuggestions() {
        SubCompleteCommand command = new SubCompleteCommand();
        CommandFramework<TestSender> framework = this.framework(builder -> builder.resolver(new SuggestionValueResolver()), command);

        List<String> suggestions = framework.suggest(this.environment.player("Alice"), "subcomplete", "pick a");
        assertEquals(List.of("alpha"), suggestions);
    }

    @Test
    void subcommandTabCompleteReturnsSubcommandNames() {
        SubCompleteCommand command = new SubCompleteCommand();
        CommandFramework<TestSender> framework = this.framework(builder -> builder.resolver(new SuggestionValueResolver()), command);

        List<String> suggestions = framework.suggest(this.environment.player("Alice"), "subcomplete", "o");
        assertEquals(List.of("other"), suggestions);
    }

    @Test
    void consoleExecutesNonPlayerOnlyCommand() {
        ConsoleFriendlyCommand command = new ConsoleFriendlyCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestConsole console = this.environment.console("Console");

        CommandResult result = framework.dispatch(console, "consolefriendly", "");

        assertInstanceOf(CommandResult.Success.class, result);
        assertEquals(1, command.calls.get());
    }

    @Test
    void multipleCommandsRegisteredAndExecuteIndependently() {
        MultiCommand1 cmd1 = new MultiCommand1();
        MultiCommand2 cmd2 = new MultiCommand2();
        CommandFramework<TestSender> framework = this.framework(cmd1, cmd2);

        framework.dispatch(this.environment.player("Alice"), "multi1", "");
        framework.dispatch(this.environment.player("Alice"), "multi2", "");

        assertEquals(1, cmd1.calls.get());
        assertEquals(1, cmd2.calls.get());
    }

    @Test
    void rateLimitDoesNotAffectConsole() {
        RootCommand command = new RootCommand();
        CommandFramework<TestSender> framework = this.framework(command);
        TestConsole console = this.environment.console("Console");

        for (int index = 0; index < 50; index++) {
            framework.dispatch(console, "root", "");
        }

        assertEquals(50, command.calls.get());
    }

    @Test
    void middlewareCanBlockExecution() {
        MiddlewareCommand command = new MiddlewareCommand();
        CommandMiddleware blocker = (context, chain) -> CommandResult.failure(MessageKey.NO_PERMISSION);
        CommandFramework<TestSender> framework = this.framework(
                builder -> builder.middleware(blocker), command);
        TestPlayer player = this.environment.player("Alice");

        CommandResult result = framework.dispatch(player, "mw", "");

        assertInstanceOf(CommandResult.Failure.class, result);
        assertEquals(0, command.calls.get());
    }

    @Test
    void middlewareCanPassThrough() {
        MiddlewareCommand command = new MiddlewareCommand();
        CommandMiddleware passthrough = (context, chain) -> chain.proceed(context);
        CommandFramework<TestSender> framework = this.framework(
                builder -> builder.middleware(passthrough), command);

        CommandResult result = framework.dispatch(this.environment.player("Alice"), "mw", "");

        assertInstanceOf(CommandResult.Success.class, result);
        assertEquals(1, command.calls.get());
    }

    @Test
    void confirmCommandNameCollidingWithRealCommandFailsFast() {
        assertThrows(IllegalStateException.class, () -> this.framework(new ConfirmCollisionCommand()));
    }

    @Test
    void rateLimitRejectsNonPositiveWindow() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> this.framework(builder -> builder.rateLimit(1, Duration.ZERO), new RootCommand())
        );
        assertTrue(exception.getMessage().contains("window"));
    }

    @Test
    void aliasWithSpacesIsTrimmedAndExecutes() {
        SpacedAliasCommand command = new SpacedAliasCommand();
        CommandFramework<TestSender> framework = this.framework(command);

        CommandResult byTrimmedAlias = framework.dispatch(this.environment.player("Alice"), "spaced", "");

        assertInstanceOf(CommandResult.Success.class, byTrimmedAlias);
        assertEquals(1, command.calls.get());
    }

    @Test
    void suggestCatchesResolverException() {
        CrashSuggestCommand command = new CrashSuggestCommand();
        CommandFramework<TestSender> framework = this.framework(
                builder -> builder.resolver(new CrashingSuggestionResolver()), command);

        List<String> suggestions = framework.suggest(this.environment.player("Alice"), "crashsuggest", "a");

        assertEquals(List.of(), suggestions);
    }

    private enum Color {RED, GREEN, BLUE}

    private interface AmbiguousService {
    }

    private static final class TestBuilder extends CommandFrameworkBuilder<TestSender, TestBuilder> {
        private TestBuilder(TestBridge bridge) {
            super(bridge);
        }

        @Override
        protected TestBuilder self() {
            return this;
        }
    }

    private static class TestBridge implements PlatformBridge<TestSender> {
        private final TestEnvironment environment;

        private TestBridge(TestEnvironment environment) {
            this.environment = environment;
        }

        @Override
        public ClassLoader classLoader() {
            return this.getClass().getClassLoader();
        }

        @Override
        public String defaultScanPackage() {
            return this.getClass().getPackageName() + ".scanempty";
        }

        @Override
        public FrameworkLogger logger() {
            return FrameworkLogger.jul(java.util.logging.Logger.getLogger("CommandFrameworkTest"));
        }

        @Override
        public CommandActor createActor(TestSender sender) {
            return new TestActor(sender);
        }

        @Override
        public boolean supportsSenderType(Class<?> type) {
            return type == CommandActor.class || type == TestSender.class || type == TestPlayer.class;
        }

        @Override
        public boolean isPlayerSenderType(Class<?> type) {
            return type == TestPlayer.class;
        }

        @Override
        public List<ArgumentResolver<?>> platformResolvers() {
            return List.of(new TestPlayerResolver(this.environment));
        }

        @Override
        public void register(CommandFramework<TestSender> framework) {
        }
    }

    private static final class ScanBridge extends TestBridge {
        private ScanBridge(TestEnvironment environment) {
            super(environment);
        }

        @Override
        public String defaultScanPackage() {
            return "io.github.hanielcota.commandframework.scanfixtures";
        }
    }

    private record SuggestionValue(String value) {
    }

    private record ExplodingValue(String value) {
    }

    private static final class SuggestionValueResolver implements ArgumentResolver<SuggestionValue> {
        @Override
        public Class<SuggestionValue> type() {
            return SuggestionValue.class;
        }

        @Override
        public SuggestionValue resolve(ArgumentResolutionContext context, String input) {
            return new SuggestionValue(input);
        }

        @Override
        public List<String> suggest(CommandActor actor, String currentInput) {
            return List.of("alpha", "beta").stream().filter(value -> value.startsWith(currentInput)).toList();
        }
    }

    private static final class ExplodingValueResolver implements ArgumentResolver<ExplodingValue> {
        @Override
        public Class<ExplodingValue> type() {
            return ExplodingValue.class;
        }

        @Override
        public ExplodingValue resolve(ArgumentResolutionContext context, String input) {
            throw new IllegalStateException("kaboom");
        }
    }

    private static final class TestPlayerResolver implements ArgumentResolver<TestPlayer> {
        private final TestEnvironment environment;

        private TestPlayerResolver(TestEnvironment environment) {
            this.environment = environment;
        }

        @Override
        public Class<TestPlayer> type() {
            return TestPlayer.class;
        }

        @Override
        public TestPlayer resolve(ArgumentResolutionContext context, String input) throws ArgumentResolveException {
            if (!input.matches("[a-zA-Z0-9_]{1,16}")) {
                throw new ArgumentResolveException("player", input, "Invalid player name");
            }
            TestPlayer player = this.environment.players.get(input.toLowerCase(Locale.ROOT));
            if (player == null || !player.online) {
                throw new ArgumentResolveException("player", input, "Player not found");
            }
            return player;
        }

        @Override
        public List<String> suggest(CommandActor actor, String currentInput) {
            String lowered = currentInput.toLowerCase(Locale.ROOT);
            return this.environment.players.values().stream()
                    .filter(player -> player.online)
                    .map(player -> player.name)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(lowered))
                    .sorted()
                    .toList();
        }
    }

    private static final class TestEnvironment {
        private final Map<String, TestPlayer> players = new ConcurrentHashMap<>();

        private TestPlayer player(String name) {
            return this.players.computeIfAbsent(name.toLowerCase(Locale.ROOT), ignored -> new TestPlayer(name));
        }

        private TestConsole console(String name) {
            return new TestConsole(name);
        }
    }

    private static class TestSender {
        final String name;
        final UUID uniqueId = UUID.randomUUID();
        final Set<String> permissions = ConcurrentHashMap.newKeySet();
        final List<String> messages = java.util.Collections.synchronizedList(new ArrayList<>());
        final boolean player;
        volatile boolean online = true;

        private TestSender(String name, boolean player) {
            this.name = name;
            this.player = player;
        }

        void grant(String permission) {
            this.permissions.add(permission);
        }

        void revoke(String permission) {
            this.permissions.remove(permission);
        }

        String lastMessage() {
            return this.messages.isEmpty() ? "" : this.messages.get(this.messages.size() - 1);
        }
    }

    private static final class TestPlayer extends TestSender {
        private TestPlayer(String name) {
            super(name, true);
        }
    }

    private static final class TestConsole extends TestSender {
        private TestConsole(String name) {
            super(name, false);
        }
    }

    private static final class TestActor implements CommandActor {
        private final TestSender sender;

        private TestActor(TestSender sender) {
            this.sender = sender;
        }

        @Override
        public String name() {
            return this.sender.name;
        }

        @Override
        public UUID uniqueId() {
            return this.sender.uniqueId;
        }

        @Override
        public boolean isPlayer() {
            return this.sender.player;
        }

        @Override
        public boolean hasPermission(String permission) {
            return this.sender.permissions.contains(permission);
        }

        @Override
        public void sendMessage(Component message) {
            if (this.isAvailable()) {
                this.sender.messages.add(message.toString());
            }
        }

        @Override
        public boolean isAvailable() {
            return !this.sender.player || this.sender.online;
        }

        @Override
        public Object platformSender() {
            return this.sender;
        }
    }

    @Command(name = "root")
    private static final class RootCommand {
        private final AtomicInteger calls = new AtomicInteger();

        @Execute
        public void execute() {
            this.calls.incrementAndGet();
        }
    }

    // ── New test fixtures ──────────────────────────────────────────────

    @Command(name = "profile")
    private static final class SubCommand {
        private String lastUsername;

        @Execute(sub = "set")
        public void set(String username) {
            this.lastUsername = username;
        }
    }

    @Command(name = "perm")
    @Permission("perm.use")
    private static final class PermissionCommand {
        private final AtomicInteger rootCalls = new AtomicInteger();
        private final AtomicInteger adminCalls = new AtomicInteger();

        @Execute
        public void root() {
            this.rootCalls.incrementAndGet();
        }

        @Execute(sub = "admin")
        @Permission("perm.admin")
        public void admin() {
            this.adminCalls.incrementAndGet();
        }
    }

    @Command(name = "cool")
    private static final class CooldownCommand {
        private final AtomicInteger calls = new AtomicInteger();

        @Execute
        @Cooldown(value = 3, unit = TimeUnit.SECONDS, bypassPermission = "cool.bypass")
        public void execute(@Sender TestPlayer player) {
            this.calls.incrementAndGet();
        }
    }

    @Command(name = "coolarg")
    private static final class CooldownArgCommand {
        private final AtomicInteger calls = new AtomicInteger();

        @Execute
        @Cooldown(value = 3, unit = TimeUnit.SECONDS)
        public void execute(int amount) {
            this.calls.incrementAndGet();
        }
    }

    @Command(name = "danger")
    private static final class ConfirmCommand {
        private final AtomicInteger calls = new AtomicInteger();
        private String lastTarget;

        @Execute(sub = "reset")
        @Confirm(expireSeconds = 5)
        public void reset(@Sender TestPlayer player, String target) {
            this.calls.incrementAndGet();
            this.lastTarget = target;
        }
    }

    @Command(name = "secure")
    private static final class ProtectedConfirmCommand {
        private final AtomicInteger calls = new AtomicInteger();

        @Execute
        @Permission("secure.use")
        @Confirm(expireSeconds = 5)
        public void execute(@Sender TestPlayer player) {
            this.calls.incrementAndGet();
        }
    }

    @Command(name = "helpme")
    private static final class HelpCommand {
        @Execute(sub = "user")
        @Permission("help.user")
        @Description("User command")
        public void user() {
        }

        @Execute(sub = "admin")
        @Permission("help.admin")
        @Description("Admin command")
        public void admin() {
        }
    }

    @Command(name = "optional")
    private static final class OptionalCommand {
        private boolean lastSilent;

        @Execute
        public void execute(@Optional("true") boolean silent) {
            this.lastSilent = silent;
        }
    }

    @Command(name = "async")
    private static final class AsyncCommand {
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile boolean virtualThreadSeen;

        @Execute
        @Async
        public void execute() {
            this.virtualThreadSeen = Thread.currentThread().isVirtual();
            this.latch.countDown();
        }
    }

    @Command(name = "asyncactor")
    private static final class AsyncActorCommand {
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile boolean virtualThreadSeen;

        @Execute
        @Async
        public void execute(@Sender CommandActor actor) {
            this.virtualThreadSeen = Thread.currentThread().isVirtual();
            actor.sendMessage(Component.text("async hello"));
            this.latch.countDown();
        }
    }

    @Command(name = "suggest")
    private static final class SuggestionCommand {
        @Execute(sub = "pick")
        public void pick(SuggestionValue value) {
        }
    }

    @Command(name = "rootsuggest")
    private static final class RootSuggestionCommand {
        @Execute
        public void execute(SuggestionValue value) {
        }
    }

    @Command(name = "number")
    private static final class NumberCommand {
        private final AtomicInteger calls = new AtomicInteger();

        @Execute
        public void execute(int amount) {
            this.calls.incrementAndGet();
        }
    }

    @Command(name = "player")
    @RequirePlayer
    private static final class PlayerOnlyCommand {
        private final AtomicInteger calls = new AtomicInteger();

        @Execute
        public void execute() {
            this.calls.incrementAndGet();
        }
    }

    @Command(name = "methodplayer")
    private static final class MethodPlayerOnlyCommand {
        private final AtomicInteger rootCalls = new AtomicInteger();
        private final AtomicInteger playerCalls = new AtomicInteger();

        @Execute
        public void root() {
            this.rootCalls.incrementAndGet();
        }

        @Execute(sub = "player")
        @RequirePlayer
        public void player() {
            this.playerCalls.incrementAndGet();
        }
    }

    @Command(name = "expire")
    private static final class ExpiringConfirmCommand {
        private final AtomicInteger calls = new AtomicInteger();

        @Execute
        @Confirm(expireSeconds = 1)
        public void execute(@Sender TestPlayer player) {
            this.calls.incrementAndGet();
        }
    }

    @Command(name = "armed")
    private static final class ConfirmCooldownCommand {
        private final AtomicInteger calls = new AtomicInteger();

        @Execute
        @Cooldown(value = 3, unit = TimeUnit.SECONDS, bypassPermission = "armed.bypass")
        @Confirm(expireSeconds = 5)
        public void execute(@Sender TestPlayer player) {
            this.calls.incrementAndGet();
        }
    }

    @Command(name = "boom")
    private static final class ThrowingCommand {
        @Execute
        public void execute() {
            throw new IllegalStateException("boom");
        }
    }

    @Command(name = "target")
    private static final class PlayerTargetCommand {
        private final AtomicInteger calls = new AtomicInteger();

        @Execute
        public void execute(TestPlayer target) {
            this.calls.incrementAndGet();
        }
    }

    @Command(name = "explode")
    private static final class ExplodingResolverCommand {
        private final AtomicInteger calls = new AtomicInteger();

        @Execute
        public void execute(ExplodingValue value) {
            this.calls.incrementAndGet();
        }
    }

    @Command(name = "missing")
    private static final class MissingResolverCommand {
        @Execute
        public void execute(MissingType type) {
        }
    }

    // ── New tests ──────────────────────────────────────────────────────

    @Command(name = "inject")
    private static final class MissingBindingCommand {
        @Inject
        private MissingType service;

        @Execute
        public void execute() {
        }
    }

    private static final class AmbiguousImplA implements AmbiguousService {
    }

    private static final class AmbiguousImplB implements AmbiguousService {
    }

    @Command(name = "ambiguous")
    private static final class AmbiguousBindingCommand {
        @Inject
        private AmbiguousService service;

        @Execute
        public void execute() {
        }
    }

    @Command(name = "greedy")
    private static final class GreedyCommand {
        private String lastMessage;

        @Execute
        public void execute(@io.github.hanielcota.commandframework.annotation.Arg(greedy = true) String message) {
            this.lastMessage = message;
        }
    }

    @Command(name = "mixed")
    private static final class RootAndSubcommandCommand {
        private final AtomicInteger rootCalls = new AtomicInteger();
        private final AtomicInteger statusCalls = new AtomicInteger();

        @Execute
        public void root() {
            this.rootCalls.incrementAndGet();
        }

        @Execute(sub = "status")
        public void status() {
            this.statusCalls.incrementAndGet();
        }
    }

    @Command(name = "opttype")
    private static final class JavaOptionalCommand {
        private java.util.Optional<String> lastValue = java.util.Optional.empty();

        @Execute
        public void execute(java.util.Optional<String> value) {
            this.lastValue = value;
        }
    }

    @Command(name = "optdefault")
    private static final class JavaOptionalWithDefaultCommand {
        private java.util.Optional<String> lastValue = java.util.Optional.empty();

        @Execute
        public void execute(@Optional("fallback") java.util.Optional<String> value) {
            this.lastValue = value;
        }
    }

    @Command(name = "armedbrief")
    private static final class BriefConfirmCooldownCommand {
        private final AtomicInteger calls = new AtomicInteger();

        @Execute
        @Cooldown(value = 3, unit = TimeUnit.SECONDS)
        @Confirm(expireSeconds = 1)
        public void execute(@Sender TestPlayer player) {
            this.calls.incrementAndGet();
        }
    }

    @Command(name = "maxlen")
    private static final class MaxLengthCommand {
        private final AtomicInteger calls = new AtomicInteger();

        @Execute
        public void execute(@io.github.hanielcota.commandframework.annotation.Arg(maxLength = 5) String value) {
            this.calls.incrementAndGet();
        }
    }

    @Command(name = "color")
    private static final class EnumCommand {
        private Color lastColor;

        @Execute
        public void execute(Color color) {
            this.lastColor = color;
        }
    }

    @Command(name = "injected")
    private static final class InjectedCommand {
        private final AtomicInteger calls = new AtomicInteger();
        @Inject
        private TestEnvironment env;

        @Execute
        public void execute() {
            if (this.env == null) {
                throw new IllegalStateException("injection failed");
            }
            this.calls.incrementAndGet();
        }
    }

    @Command(name = "asyncboom")
    private static final class AsyncThrowingCommand {
        private final CountDownLatch latch = new CountDownLatch(1);

        @Execute
        @Async
        public void execute(@Sender CommandActor actor) {
            this.latch.countDown();
            throw new IllegalStateException("async boom");
        }
    }

    @Command(name = "aliased", aliases = {"al", "ali"})
    private static final class AliasedCommand {
        private final AtomicInteger calls = new AtomicInteger();

        @Execute
        public void execute() {
            this.calls.incrementAndGet();
        }
    }

    @Command(name = "customconfirm")
    private static final class CustomConfirmCommand {
        private final AtomicInteger calls = new AtomicInteger();

        @Execute
        @Confirm(expireSeconds = 5, commandName = "yesplease")
        public void execute(@Sender TestPlayer player) {
            this.calls.incrementAndGet();
        }
    }

    @Command(name = "multi1")
    private static final class MultiCommand1 {
        private final AtomicInteger calls = new AtomicInteger();

        @Execute
        public void execute() {
            this.calls.incrementAndGet();
        }
    }

    @Command(name = "multi2")
    private static final class MultiCommand2 {
        private final AtomicInteger calls = new AtomicInteger();

        @Execute
        public void execute() {
            this.calls.incrementAndGet();
        }
    }

    @Command(name = "subcomplete")
    private static final class SubCompleteCommand {
        @Execute(sub = "pick")
        public void pick(SuggestionValue value) {
        }

        @Execute(sub = "other")
        @Description("Other")
        public void other() {
        }
    }

    @Command(name = "consolefriendly")
    private static final class ConsoleFriendlyCommand {
        private final AtomicInteger calls = new AtomicInteger();

        @Execute
        public void execute() {
            this.calls.incrementAndGet();
        }
    }

    @Command(name = "mw")
    private static final class MiddlewareCommand {
        private final AtomicInteger calls = new AtomicInteger();

        @Execute
        public void execute() {
            this.calls.incrementAndGet();
        }
    }

    @Command(name = "collide")
    private static final class ConfirmCollisionCommand {
        @Execute
        @Confirm(expireSeconds = 5, commandName = "collide")
        public void execute(@Sender TestPlayer player) {
        }
    }

    @Command(name = "spacealias", aliases = {" spaced "})
    private static final class SpacedAliasCommand {
        private final AtomicInteger calls = new AtomicInteger();

        @Execute
        public void execute() {
            this.calls.incrementAndGet();
        }
    }

    // ── Tests for audit fixes ──────────────────────────────────────────

    @Command(name = "crashsuggest")
    private static final class CrashSuggestCommand {
        @Execute
        public void execute(SuggestionValue value) {
        }
    }

    private static final class CrashingSuggestionResolver implements ArgumentResolver<SuggestionValue> {
        @Override
        public Class<SuggestionValue> type() {
            return SuggestionValue.class;
        }

        @Override
        public SuggestionValue resolve(ArgumentResolutionContext context, String input) {
            return new SuggestionValue(input);
        }

        @Override
        public List<String> suggest(CommandActor actor, String currentInput) {
            throw new IllegalStateException("suggest crashed");
        }
    }

    private static final class MissingType {
    }
}
