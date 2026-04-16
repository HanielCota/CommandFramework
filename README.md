<div align="center">

# CommandFramework

**Annotation-driven command framework for Paper and Velocity**
**Zero YAML. Zero `plugin.yml` command declarations. Java 25.**

[![Build](https://github.com/HanielCota/CommandFramework/actions/workflows/build.yml/badge.svg)](https://github.com/HanielCota/CommandFramework/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/HanielCota/CommandFramework?label=release&color=0b7)](https://github.com/HanielCota/CommandFramework/releases/latest)
[![JitPack](https://img.shields.io/jitpack/version/com.github.HanielCota/CommandFramework.svg)](https://jitpack.io/#HanielCota/CommandFramework)
[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/25/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

</div>

---

## Why CommandFramework

- **Declarative.** Add `@Command` + `@Execute` to a class, call `build()`, done. No `plugin.yml`, no `CommandMap` juggling.
- **Shared core, thin adapters.** Same commands run on Paper and Velocity. Swap the entry point.
- **Brigadier-native.** Hooks into Paper's `LifecycleEvents.COMMANDS` registrar and Velocity's `BrigadierCommand`, so suggestions and parse errors are real client-side Brigadier.
- **Safety built in.** Per-sender cooldowns, confirmation flows, fixed-window rate limits, async-on-virtual-threads — all thread-safe via Caffeine and `ConcurrentHashMap`.
- **Fluent, typed.** `PaperCommandFramework.paper(this).bind(...).resolver(...).middleware(...).build()`.
- **Open API.** Custom resolvers, middlewares, message providers, and platform bridges are all first-class.

---

## Table of contents

1. [Install](#install)
2. [Quick start](#quick-start)
3. [Core concepts](#core-concepts)
4. [Features](#features)
   - [Subcommands](#subcommands)
   - [Arguments and resolvers](#arguments-and-resolvers)
   - [Permissions](#permissions)
   - [Cooldowns](#cooldowns)
   - [Confirmations](#confirmations)
   - [Player-only and async](#player-only-and-async)
   - [Dependency injection](#dependency-injection)
   - [Help and tab completion](#help-and-tab-completion)
   - [Middlewares](#middlewares)
   - [Rate limiting](#rate-limiting)
   - [Messages and MiniMessage](#messages-and-minimessage)
5. [Annotation reference](#annotation-reference)
6. [Builder API reference](#builder-api-reference)
7. [Architecture](#architecture)
8. [Testing](#testing)
9. [Compatibility](#compatibility)
10. [License](#license)

---

## Install

### JitPack (recommended)

Add the repository and the modules you need:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://jitpack.io")
    }
}
```

```kotlin
// build.gradle.kts
dependencies {
    // Pick the module matching your platform. core is included transitively.
    implementation("com.github.HanielCota.CommandFramework:paper:0.1.0")
    implementation("com.github.HanielCota.CommandFramework:velocity:0.1.0")
}
```

Use the latest tag: [![JitPack](https://img.shields.io/jitpack/version/com.github.HanielCota/CommandFramework.svg)](https://jitpack.io/#HanielCota/CommandFramework)

### GitHub Packages (alternative)

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/HanielCota/CommandFramework")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("io.github.hanielcota.commandframework:paper:0.1.0")
}
```

GitHub Packages requires a token with `read:packages` scope even for public packages.

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.HanielCota.CommandFramework</groupId>
        <artifactId>paper</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

---

## Quick start

### Paper plugin

```java
import io.github.hanielcota.commandframework.paper.PaperCommandFramework;
import org.bukkit.plugin.java.JavaPlugin;

public final class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        PaperCommandFramework.paper(this)
            .scanPackage("com.example.mycommands")
            .build();
    }
}
```

### Velocity plugin

```java
import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.hanielcota.commandframework.velocity.VelocityCommandFramework;

@Plugin(id = "myplugin", name = "MyPlugin", version = "1.0.0")
public final class MyProxyPlugin {
    @Inject
    public MyProxyPlugin(ProxyServer server) {
        VelocityCommandFramework.velocity(server, this)
            .scanPackage("com.example.proxycommands")
            .build();
    }
}
```

No `plugin.yml` `commands:` entries. No manual registration. The framework scans the package for annotated classes, builds them, and registers each one through the native Brigadier APIs.

### A complete command

```java
import io.github.hanielcota.commandframework.annotation.Arg;
import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Cooldown;
import io.github.hanielcota.commandframework.annotation.Description;
import io.github.hanielcota.commandframework.annotation.Execute;
import io.github.hanielcota.commandframework.annotation.Permission;
import io.github.hanielcota.commandframework.annotation.Sender;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Command(name = "heal", aliases = {"curar"}, description = "Heal a player")
@Permission("myplugin.heal")
public final class HealCommand {

    @Execute
    @Description("Heal yourself")
    @Cooldown(value = 30, unit = TimeUnit.SECONDS, bypassPermission = "myplugin.heal.bypass")
    public void healSelf(@Sender Player sender) {
        sender.setHealth(sender.getMaxHealth());
        sender.sendMessage(Component.text("Healed."));
    }

    @Execute(sub = "other")
    @Description("Heal another player")
    @Permission("myplugin.heal.other")
    public void healOther(@Sender Player sender, Player target) {
        target.setHealth(target.getMaxHealth());
        sender.sendMessage(Component.text("Healed " + target.getName()));
    }
}
```

Usage in game:
- `/heal` — heals the sender (30s cooldown, bypassable by permission).
- `/heal other <name>` — heals someone else (requires `myplugin.heal.other`).
- `/curar` — alias, same behavior.
- `<Tab>` on `<name>` — Brigadier-native completion powered by the built-in `Player` resolver.

---

## Core concepts

### Commands

A **command class** is any class annotated with `@Command`. Its `name` is the top-level label; `aliases` add alternative labels; `description` shows in `/help`; `permission` applies to every executor in the class.

### Executors

A **method** annotated with `@Execute` is an executor. `@Execute` (no `sub`) is the root executor — invoked by `/name`. `@Execute(sub = "foo")` is a subcommand — invoked by `/name foo`.

One class can mix a root and any number of subcommands.

### Senders

The first parameter annotated with `@Sender` (or the first parameter whose type the platform recognises as a sender type) receives the caller. Supported types depend on the platform:

| Platform | Sender types |
|---|---|
| Paper | `CommandActor`, `CommandSender`, `Player` |
| Velocity | `CommandActor`, `CommandSource`, `Player` |

`CommandActor` is the platform-neutral abstraction — use it in core/shared command classes.

### Results

Every `dispatch` returns a `CommandResult` from a sealed interface. You rarely inspect it directly (the framework sends the right message automatically), but tests and middlewares do:

```java
sealed interface CommandResult permits
    Success, Handled, Failure, InvalidArgs, NoPermission, PlayerOnly,
    CooldownActive, PendingConfirmation, HelpShown, RateLimited { }
```

---

## Features

### Subcommands

```java
@Command(name = "eco")
public final class EconomyCommand {
    @Execute
    public void balance(@Sender Player sender) { /* ... */ }

    @Execute(sub = "pay")
    public void pay(@Sender Player sender, Player target, double amount) { /* ... */ }

    @Execute(sub = "reset")
    public void reset(@Sender Player sender, Player target) { /* ... */ }
}
```

### Arguments and resolvers

Built-in resolvers: `String`, `Boolean` / `boolean`, `Integer` / `int`, `Long` / `long`, `Double` / `double`, `Float` / `float`, `UUID`, any `Enum`.

Paper adds: `Player`, `World`.
Velocity adds: `Player`.

Parameter name is inferred via `-parameters` (Gradle toolchain already enables this). Override with `@Arg("name")` when you want different display text.

#### Greedy

Consume the rest of the input as a single string:

```java
@Execute(sub = "note")
public void note(@Sender Player sender, Player target, @Arg(greedy = true) String message) { /* ... */ }
```

#### Optional

Provide a default when the argument is missing:

```java
@Execute(sub = "give")
public void give(@Sender Player sender, Player target, double amount,
                 @Optional("false") boolean silent) { /* ... */ }
```

#### Custom resolvers

Implement `ArgumentResolver<T>` and register it:

```java
public final class KitResolver implements ArgumentResolver<Kit> {
    private final KitService kits;
    public KitResolver(KitService kits) { this.kits = kits; }

    @Override public Class<Kit> type() { return Kit.class; }

    @Override
    public Kit resolve(ArgumentResolutionContext ctx, String input) throws ArgumentResolveException {
        return this.kits.find(input).orElseThrow(() ->
            new ArgumentResolveException("kit", input, "Unknown kit"));
    }

    @Override
    public List<String> suggest(CommandActor actor, String currentInput) {
        return this.kits.names().stream()
            .filter(n -> n.startsWith(currentInput))
            .toList();
    }
}
```

```java
PaperCommandFramework.paper(this)
    .resolver(new KitResolver(kitService))
    .build();
```

### Permissions

Class-level `@Permission` applies to every executor. Method-level overrides:

```java
@Command(name = "admin")
@Permission("myplugin.admin")
public final class AdminCommand {
    @Execute public void panel(@Sender Player sender) { /* needs myplugin.admin */ }

    @Execute(sub = "ban")
    @Permission("myplugin.admin.ban")
    public void ban(@Sender Player sender, Player target) { /* needs myplugin.admin.ban */ }
}
```

### Cooldowns

Per-sender, per-command-path. Entries expire through a Caffeine `Expiry`, so idle state consumes no memory.

```java
@Execute
@Cooldown(value = 5, unit = TimeUnit.SECONDS, bypassPermission = "myplugin.heal.bypass")
public void heal(@Sender Player sender) { /* ... */ }
```

Cooldowns are only committed **after** argument parsing succeeds, so a typo does not lock out the legitimate retry.

### Confirmations

Prompt and gate a destructive action behind a second command:

```java
@Execute(sub = "wipe")
@Permission("myplugin.admin.wipe")
@Confirm(expireSeconds = 10, commandName = "confirmar")
public void wipe(@Sender Player sender) { /* ... */ }
```

Sender sees: `Run /confirmar within 10s to confirm.`

Typing `/confirmar` within the window executes the original invocation with the original arguments.

### Player-only and async

```java
@Command(name = "home")
@RequirePlayer
public final class HomeCommand {
    @Execute
    @Async
    public void home(@Sender Player player) {
        // Runs on a virtual thread. Safe for DB reads, HTTP, etc.
        // sendMessage automatically hops back for Paper.
    }
}
```

`@RequirePlayer` can also be placed on individual methods.

### Dependency injection

Bind services in the builder, receive them as fields in commands and resolvers:

```java
PaperCommandFramework.paper(this)
    .bind(EconomyService.class, new EconomyService(database))
    .bind(KitService.class, new KitService())
    .build();
```

```java
@Command(name = "bal")
public final class BalanceCommand {
    @Inject private EconomyService economy;

    @Execute public void balance(@Sender Player sender) {
        sender.sendMessage(Component.text(this.economy.getBalance(sender.getUniqueId())));
    }
}
```

`JavaPlugin` (Paper) and `ProxyServer` (Velocity) are auto-bound by the entry point. Resolution tries exact type first, then falls back to a single assignable match; ambiguous matches throw `IllegalStateException` at build time.

### Help and tab completion

`/command` with no matching subcommand renders the help template — subcommands visible only if the sender has their permission. Customise with:

```java
builder.message(MessageKey.HELP_HEADER, "<gold>=== /{command} ===");
builder.message(MessageKey.HELP_ENTRY, "<yellow>/{usage}</yellow> <gray>-</gray> {description}");
```

Tab completion is automatic, powered by Brigadier and the active resolver for each parameter. Custom resolvers contribute their `suggest(...)` output.

### Middlewares

Wrap every dispatch with cross-cutting logic — auditing, tracing, metrics:

```java
public final class AuditMiddleware implements CommandMiddleware {
    private final Audit audit;
    public AuditMiddleware(Audit audit) { this.audit = audit; }

    @Override
    public CommandResult handle(CommandContext ctx, Chain chain) {
        this.audit.logStart(ctx.actor(), ctx.label(), ctx.rawArguments());
        CommandResult result = chain.proceed(ctx);
        this.audit.logEnd(ctx.actor(), ctx.label(), result);
        return result;
    }
}
```

```java
builder.middleware(new AuditMiddleware(audit));
```

Middlewares run in registration order, wrapping the dispatcher.

### Rate limiting

A fixed-window rate limiter protects against command spam. Default: 30 commands per 10 seconds per sender. Override:

```java
builder.rateLimit(50, Duration.ofMinutes(1));
```

Console is never rate-limited.

### Messages and MiniMessage

Customise any of the built-in keys:

```java
builder
    .message(MessageKey.NO_PERMISSION, "<red>Acesso negado.")
    .message(MessageKey.COOLDOWN_ACTIVE, "<yellow>Aguarde <bold>{remaining}</bold>.");
```

Or swap the provider entirely:

```java
builder.messages(key -> switch (key) {
    case NO_PERMISSION -> "<red>No perm.";
    // ...
    default -> "";
});
```

All templates are MiniMessage — colours, formatting, clickable/hoverable components all supported.

---

## Annotation reference

| Annotation | Target | Purpose |
|---|---|---|
| `@Command(name, aliases, description, permission)` | class | Declares a top-level command root. |
| `@Execute(sub)` | method | Root (`sub=""`) or subcommand executor. |
| `@Sender` | parameter | Marks the sender parameter explicitly. |
| `@Arg(value, greedy, maxLength)` | parameter | Overrides inferred argument metadata. |
| `@Optional(value)` | parameter | Default value when argument omitted. |
| `@Description(value)` | method | Description shown in help output. |
| `@Permission(value)` | class, method | Permission requirement; method overrides class. |
| `@Cooldown(value, unit, bypassPermission)` | method | Per-sender cooldown with optional bypass. |
| `@Confirm(expireSeconds, commandName)` | method | Two-step confirmation flow. |
| `@RequirePlayer` | class, method | Console is rejected with `PLAYER_ONLY` message. |
| `@Async` | method | Runs the executor on a virtual thread. |
| `@Inject` | field | Injects a binding (exact type or assignable). |

---

## Builder API reference

All mutators are fluent (`return this`). `build()` finalises registration.

| Method | Purpose |
|---|---|
| `scanPackage(String)` | Add a package to scan for `@Command` classes. |
| `command(Object)` | Register a pre-instantiated command. |
| `commands(Object...)` | Register several commands. |
| `bind(Class<T>, T)` | Bind a dependency for `@Inject` / resolvers. |
| `resolver(ArgumentResolver<?>)` | Register a custom argument resolver. |
| `middleware(CommandMiddleware)` | Register a middleware. |
| `message(MessageKey, String)` | Override a single message template. |
| `messages(MessageProvider)` | Replace the message provider entirely. |
| `rateLimit(int, Duration)` | Configure the per-sender rate limit. |
| `build()` | Build, validate, register. Returns `CommandFramework<S>`. |

---

## Architecture

```
+-------------------+         +-----------------------+
|  Paper plugin     |         |  Velocity plugin      |
|  onEnable(...)    |         |  @Plugin ctor(...)    |
+----------+--------+         +----------+------------+
           |                             |
           v                             v
+----------+----------+         +--------+-----------+
| PaperCommandFw      |         | VelocityCommandFw  |
| (extends Builder)   |         | (extends Builder)  |
+----------+----------+         +--------+-----------+
           |                             |
           |  .bind / .resolver /        |
           |  .middleware / .build()     |
           v                             v
+----------+-----------------------------+------------+
|              CommandFrameworkBuilder (core)         |
|                                                     |
|  InternalCommandBuilder ----> CommandFramework<S>   |
|     - scans classes                                 |
|     - validates signatures                          |
|     - injects fields                                |
|     - builds CommandDispatcher                      |
+-----------+-----------------------------------------+
            |
            v
+-----------+--------------------------+
| CommandDispatcher (pipeline)         |
|  1. permission    4. argument parse  |
|  2. player-only   5. confirmation    |
|  3. cooldown      6. execute (sync / |
|                      virtual thread) |
+--------------------------------------+
             ^           ^
             |           |
     CooldownMgr   ConfirmationMgr    RateLimiter
     (Caffeine)    (Caffeine)         (Caffeine)
```

### Module layout

```
core/       Platform-agnostic: dispatcher, resolvers, managers, message service.
paper/      PaperPlatformBridge + Brigadier via LifecycleEvents.COMMANDS.
velocity/   VelocityPlatformBridge + BrigadierCommand via CommandManager.
```

Both `paper` and `velocity` ship as shadow jars with Caffeine and ClassGraph relocated to `io.github.hanielcota.commandframework.libs.*` to avoid classpath clashes.

---

## Testing

The core is tested in isolation with a `TestBridge` + `TestPlayer` fixture. Paper and Velocity adapters are tested with Mockito 5 — no MockBukkit required.

```bash
./gradlew test
```

As of 0.1.0: **86 tests** (59 core, 13 paper, 14 velocity). Coverage includes every annotation, error path, cooldown/confirmation timing, rate limiting, and dispatcher semantics.

See [`CONTRIBUTING.md`](./CONTRIBUTING.md) for local setup, PR conventions, and release flow.

---

## Compatibility

| Tier | Version |
|---|---|
| Java | 25 (toolchain) |
| Gradle | 9.4.1 (via wrapper) |
| Paper API | `26.1.2.build.+` (MC 1.21+) |
| Velocity API | `3.4.0-SNAPSHOT` |
| Adventure | 4.26.1 |
| MiniMessage | Required at runtime (shipped non-transitively by the shadowed jar) |

Paper plugins must be built against the modern Paper API (Brigadier lifecycle events) — legacy `JavaPlugin.getCommand(...)` is not used.

---

## License

MIT. See [`LICENSE`](./LICENSE) (or the MIT text linked in every POM).

## Contributing

See [`CONTRIBUTING.md`](./CONTRIBUTING.md) and [`CHANGELOG.md`](./CHANGELOG.md).

Issues and PRs welcome at [HanielCota/CommandFramework](https://github.com/HanielCota/CommandFramework).

---

<div align="center">

Built with care for Paper and Velocity networks.
Maintained by [@HanielCota](https://github.com/HanielCota).

</div>
