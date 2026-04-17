<div align="center">

# CommandFramework

**Create Minecraft plugin commands by writing plain Java classes.**
**No `plugin.yml` commands. No Brigadier plumbing. Works on Paper and Velocity.**

[![Build](https://github.com/HanielCota/CommandFramework/actions/workflows/build.yml/badge.svg)](https://github.com/HanielCota/CommandFramework/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/HanielCota/CommandFramework?label=release&color=0b7)](https://github.com/HanielCota/CommandFramework/releases/latest)
[![JitPack](https://img.shields.io/jitpack/version/com.github.HanielCota/CommandFramework.svg)](https://jitpack.io/#HanielCota/CommandFramework)
[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/25/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

</div>

---

## 📖 What is this?

CommandFramework is a **library** you add to your Minecraft plugin (Paper or Velocity).
Instead of writing all the boilerplate that Paper/Velocity normally require — command
maps, `plugin.yml` declarations, argument parsing, permission checks, cooldowns,
tab-completion, help screens — you just write one Java class with a few annotations,
and everything else is wired automatically.

**Example of the whole workflow:**

```java
@Command(name = "heal")
@Permission("myplugin.heal")
public final class HealCommand {
    @Execute
    public void run(@Sender Player player) {
        player.setHealth(player.getMaxHealth());
    }
}
```

Drop that class in your plugin's `commands` package, and `/heal` works in game —
permission-gated, tab-completed, with a did-you-mean hint if someone mistypes a
subcommand, and a clickable confirm button when you add `@Confirm`. That's it.

---

## ✅ Who is this for?

- Developers building a Paper plugin (Minecraft server 1.21+) or a Velocity proxy plugin.
- Devs who know enough Java to write a class with a method, but don't want to memorise
  the full Bukkit/Brigadier API just to add a `/kit` command.
- Teams who want **one command codebase** that runs on both Paper and Velocity.

**You don't need to know:** Brigadier, `plugin.yml` tricks, reflection, or annotation-processor internals. The framework hides all of it.

---

## 📋 Before you start — prerequisites

| You need | Why | How to get it |
|---|---|---|
| **JDK 25** | Project and generated plugins compile for Java 25. | [Adoptium Temurin 25](https://adoptium.net/) or SDKMAN `sdk install java 25-tem` |
| **Gradle 9 (or Maven)** | Build tool that pulls the library from JitPack. | Use the `./gradlew` wrapper that Paper/Velocity starter projects include. |
| **A Paper or Velocity plugin skeleton** | Where you'll drop your command classes. | [Paper plugin template](https://docs.papermc.io/paper/dev/getting-started/paper-plugins) or Velocity's [IntelliJ template](https://docs.papermc.io/velocity/creating-your-first-plugin) |
| **An IDE (IntelliJ recommended)** | Annotation processing + autocomplete. | [IntelliJ Community](https://www.jetbrains.com/idea/download/) is free. |

> **New to plugin development?** Start with the official Paper
> [*Getting Started*](https://docs.papermc.io/paper/dev/getting-started/project-setup) guide first — build a hello-world plugin, see it load on a server, then come back here.

---

## 🗂️ Table of contents

1. [Install](#-install)
2. [Your first command — 10 minute tutorial](#-your-first-command--10-minute-tutorial)
3. [Learn by example](#-learn-by-example)
   - [Subcommands](#subcommands)
   - [Arguments & types](#arguments--types)
   - [Permissions](#permissions)
   - [Cooldowns](#cooldowns)
   - [Confirmations](#confirmations)
   - [Player-only and async](#player-only-and-async)
   - [Dependency injection](#dependency-injection)
   - [Custom argument types](#custom-argument-types)
   - [Middlewares (audit, metrics, tracing)](#middlewares)
   - [Rate limiting](#rate-limiting)
4. [Messages — fully configurable](#-messages--fully-configurable)
5. [Testing your commands](#-testing-your-commands)
6. [Annotation cheat sheet](#-annotation-cheat-sheet)
7. [Builder cheat sheet](#-builder-cheat-sheet)
8. [Troubleshooting](#-troubleshooting)
9. [FAQ](#-faq)
10. [Glossary](#-glossary)
11. [Architecture (for curious readers)](#-architecture-for-curious-readers)
12. [Compatibility](#-compatibility)
13. [Contributing & License](#-contributing)

---

## 📦 Install

You have **three ways** to add CommandFramework to your plugin. Pick one.

### Option 1 — JitPack (recommended, zero setup)

Add the JitPack repository and the module for your platform.

**`settings.gradle.kts`:**
```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://jitpack.io")
    }
}
```

**`build.gradle.kts` (Paper plugin):**
```kotlin
dependencies {
    implementation("com.github.HanielCota.CommandFramework:paper:0.2.0")
    annotationProcessor("com.github.HanielCota.CommandFramework:processor:0.2.0")
}
```

**`build.gradle.kts` (Velocity plugin):**
```kotlin
dependencies {
    implementation("com.github.HanielCota.CommandFramework:velocity:0.2.0")
    annotationProcessor("com.github.HanielCota.CommandFramework:processor:0.2.0")
}
```

> **What is the `annotationProcessor`?** It runs at compile time, reads your
> `@Command` / `@Execute` / `@Arg` annotations, validates them, and generates
> a small file the framework uses to find your commands at runtime. You don't
> interact with it — it just has to be on the classpath for `scanPackage(...)`
> to work.

### Option 2 — Gradle plugin (one-line setup)

If you use Gradle, the bundled plugin wires everything (platform dependency,
annotation processor, Java 25 toolchain) for you:

```kotlin
plugins {
    java
    id("io.github.hanielcota.commandframework") version "0.2.0"
}

commandframework {
    platform.set("paper")   // or "velocity"
    version.set("0.2.0")
}
```

### Option 3 — Maven

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
        <version>0.2.0</version>
    </dependency>
    <dependency>
        <groupId>com.github.HanielCota.CommandFramework</groupId>
        <artifactId>processor</artifactId>
        <version>0.2.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

---

## 🚀 Your first command — 10 minute tutorial

Assume you already have a working Paper plugin project (empty `onEnable`, `plugin.yml`
declared). We'll add a `/heal` command.

### Step 1 — Add the dependency

See the [Install](#-install) section above.

### Step 2 — Initialise the framework in `onEnable`

```java
package com.example.myplugin;

import io.github.hanielcota.commandframework.paper.PaperCommandFramework;
import org.bukkit.plugin.java.JavaPlugin;

public final class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        PaperCommandFramework.paper(this)
            .scanPackage("com.example.myplugin.commands")
            .build();
    }
}
```

**What this does:**
- `scanPackage(...)` tells the framework: "look in this Java package for any class
  annotated with `@Command` and register it automatically."
- `build()` validates everything and hooks into Paper's modern command registrar
  (Brigadier). No `plugin.yml` changes needed.

### Step 3 — Write the command

Create `src/main/java/com/example/myplugin/commands/HealCommand.java`:

```java
package com.example.myplugin.commands;

import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Execute;
import io.github.hanielcota.commandframework.annotation.Permission;
import io.github.hanielcota.commandframework.annotation.Sender;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Command(name = "heal", description = "Fully heal a player")
@Permission("myplugin.heal")
public final class HealCommand {

    @Execute
    public void healSelf(@Sender Player player) {
        player.setHealth(player.getMaxHealth());
        player.sendMessage(Component.text("You've been fully healed."));
    }
}
```

### Step 4 — Build and run

```bash
./gradlew build
```

Drop the produced jar into your server's `plugins/` folder and restart. In-game:

- `/heal` works for any player with the `myplugin.heal` permission.
- Without the permission: the framework sends the `NO_PERMISSION` message automatically (configurable — see [Messages](#-messages--fully-configurable)).
- Tab-completion, unknown-subcommand suggestions, and help for `/heal ?` are already wired.

**🎉 You wrote a command.** Now let's see everything else you can do.

---

## 📚 Learn by example

Each section below shows one feature in a minimal runnable snippet. All the
`@` imports come from `io.github.hanielcota.commandframework.annotation.*`.

### Subcommands

One class can hold a "root" executor (no `sub`) and any number of subcommands:

```java
@Command(name = "eco", description = "Economy commands")
public final class EconomyCommand {

    @Execute
    public void balance(@Sender Player player) {
        // /eco  → runs this
    }

    @Execute(sub = "pay")
    public void pay(@Sender Player player, Player target, double amount) {
        // /eco pay <player> <amount>
    }

    @Execute(sub = "reset")
    public void reset(@Sender Player player, Player target) {
        // /eco reset <player>
    }
}
```

> **Aliases:** `@Command(name = "eco", aliases = {"money", "bal"})` lets `/money` and `/bal` work too.

### Arguments & types

**Built-in types, converted automatically** from the player's input:
`String`, `int` / `Integer`, `long` / `Long`, `double` / `Double`,
`float` / `Float`, `boolean` / `Boolean`, `UUID`, and any `enum`.

- **Paper** also provides `Player` and `World`.
- **Velocity** also provides `Player`.

```java
@Execute(sub = "give")
public void give(@Sender Player sender, Player target, int amount) {
    // /eco give <target> <amount>
    // "target" completes online player names. Wrong name → "invalid argument" message.
    // "amount" must parse as int. "abc" → typed error.
}
```

**Optional arguments** — use a default when missing:

```java
@Execute(sub = "give")
public void give(@Sender Player player, Player target, double amount,
                 @Optional("false") boolean silent) {
    // `silent` defaults to false if the player doesn't type it.
}
```

**Greedy** — capture the whole remaining line as one `String`:

```java
@Execute(sub = "say")
public void say(@Sender Player player, @Arg(greedy = true) String message) {
    // /eco say Hello world, how are you?
    // → message = "Hello world, how are you?"
}
```

> Only one greedy arg per method, and it must be the **last** parameter. The
> annotation processor catches this at compile time.

### Permissions

Permissions attach to the class (apply to everyone) or to a specific method (override):

```java
@Command(name = "admin")
@Permission("myplugin.admin")              // required for every method below
public final class AdminCommand {

    @Execute
    public void panel(@Sender Player player) { /* needs myplugin.admin */ }

    @Execute(sub = "ban")
    @Permission("myplugin.admin.ban")      // stricter requirement for just /admin ban
    public void ban(@Sender Player player, Player target) { /* ... */ }
}
```

### Cooldowns

Per-sender, per-command. The cooldown is only applied **after** the arguments validate,
so mistyping does not lock you out.

```java
import java.util.concurrent.TimeUnit;

@Execute
@Cooldown(value = 30, unit = TimeUnit.SECONDS, bypassPermission = "myplugin.heal.bypass")
public void heal(@Sender Player player) { /* ... */ }
```

Players with `myplugin.heal.bypass` skip the cooldown entirely.

### Confirmations

For destructive actions, ask the player to confirm. The framework sends a **clickable
`[Confirm]` button** (Adventure `<click:run_command>`); if they don't click within the
window, the invocation is discarded.

```java
@Execute(sub = "wipe")
@Permission("myplugin.admin.wipe")
@Confirm(expireSeconds = 10, commandName = "confirmar")
public void wipe(@Sender Player player) {
    // Runs only if the player clicks [Confirm] (or types /confirmar) within 10s.
}
```

### Player-only and async

```java
@Command(name = "home")
@RequirePlayer                    // console gets a typed "player only" error
public final class HomeCommand {

    @Execute
    @Async                        // runs on a virtual thread — safe for DB/HTTP
    public void home(@Sender Player player) {
        Location h = database.loadHome(player.getUniqueId());
        player.teleport(h);       // sendMessage auto-hops back to the main thread
    }
}
```

### Dependency injection

Bind a service once in the builder; the framework wires it into every command.

```java
// In your onEnable:
var economy = new EconomyService(database);
var kits = new KitService();

PaperCommandFramework.paper(this)
    .bind(EconomyService.class, economy)
    .bind(KitService.class, kits)
    .scanPackage("com.example.myplugin.commands")
    .build();
```

```java
@Command(name = "bal")
public final class BalanceCommand {

    @Inject private EconomyService economy;        // ← injected automatically

    @Execute
    public void balance(@Sender Player player) {
        double balance = this.economy.getBalance(player.getUniqueId());
        player.sendMessage(Component.text("Balance: " + balance));
    }
}
```

`JavaPlugin` (Paper) and `ProxyServer` (Velocity) are pre-bound — you don't need
to `.bind(...)` them yourself.

### Custom argument types

If you want `/kit <kitname>` to accept your own `Kit` type (with autocomplete),
implement `ArgumentResolver<Kit>`:

```java
public final class KitResolver implements ArgumentResolver<Kit> {
    private final KitService kits;
    public KitResolver(KitService kits) { this.kits = kits; }

    @Override public Class<Kit> type() { return Kit.class; }

    @Override
    public Kit resolve(ArgumentResolutionContext ctx, String input)
            throws ArgumentResolveException {
        return this.kits.find(input).orElseThrow(() ->
                new ArgumentResolveException("kit", input, "Unknown kit"));
    }

    @Override
    public List<String> suggest(CommandActor actor, String currentInput) {
        return this.kits.names().stream()
                .filter(name -> name.startsWith(currentInput))
                .toList();
    }
}
```

Register it:

```java
PaperCommandFramework.paper(this)
    .resolver(new KitResolver(kits))
    .build();
```

Now any method parameter typed `Kit` works automatically:

```java
@Execute(sub = "give")
public void give(@Sender Player player, Kit kit) { /* ... */ }
```

### Middlewares

A middleware runs **around** every command — useful for auditing, metrics, or tracing.

```java
public final class AuditMiddleware implements CommandMiddleware {
    private final AuditLog log;
    public AuditMiddleware(AuditLog log) { this.log = log; }

    @Override
    public CommandResult handle(CommandContext ctx, Chain chain) {
        this.log.write(ctx.actor(), ctx.label(), ctx.rawArguments());
        CommandResult result = chain.proceed(ctx);
        this.log.result(ctx.actor(), ctx.label(), result);
        return result;
    }
}
```

```java
builder.middleware(new AuditMiddleware(auditLog));
```

Middlewares run in the order you register them.

### Rate limiting

A fixed-window limiter blocks command spam. Default: **30 commands per 10 seconds** per sender (console excluded). To change:

```java
builder.rateLimit(50, Duration.ofMinutes(1));
```

---

## 💬 Messages — fully configurable

Every error, prompt, and help line is a **MiniMessage** template you can customise.

### Override one message inline

```java
builder.message(MessageKey.NO_PERMISSION, "<red>You cannot use that command.");
```

### Load from a YAML config file (recommended)

Ship a `messages.yml` in your plugin's resources:

```yaml
# messages.yml
no-permission: "<red>Você não tem permissão para isso."
cooldown-active: "<yellow>Aguarde <bold>{remaining}</bold>."
confirm-prompt: "<yellow>Clique <click:run_command:'/{command}'><green>[Confirmar]</green></click> em {seconds}s."
```

Load it on startup:

```java
this.saveResource("messages.yml", false);
var yaml = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
var templates = yaml.getKeys(false).stream()
        .collect(Collectors.toMap(k -> k, yaml::getString));

PaperCommandFramework.paper(this)
    .messages(MessageProvider.fromStringMap(templates))
    .scanPackage("com.example.myplugin.commands")
    .build();
```

`fromStringMap` tolerates `no-permission`, `no_permission`, and `NO_PERMISSION`
indifferently, and any key you omit falls back to the built-in template.

**Complete reference** — all keys and their placeholders:

| Key | Placeholders | When it fires |
|---|---|---|
| `PLAYER_ONLY` | — | Non-player ran a `@RequirePlayer` command. |
| `NO_PERMISSION` | — | Sender missed `@Permission`. |
| `INVALID_ARGUMENT` | `{name}`, `{input}` | Resolver rejected the input. |
| `MISSING_ARGUMENT` | `{name}` | Required argument not supplied. |
| `TOO_MANY_ARGUMENTS` | `{input}` | Extra tokens beyond the signature. |
| `COOLDOWN_ACTIVE` | `{remaining}` | `@Cooldown` window still open. |
| `COMMAND_ERROR` | — | Handler threw an unhandled exception. |
| `CONFIRM_PROMPT` | `{command}`, `{seconds}` | Clickable confirm prompt. |
| `CONFIRM_NOTHING_PENDING` | — | User ran the confirm command with nothing queued. |
| `HELP_HEADER` | `{command}` | Top line of generated help. |
| `HELP_ENTRY` | `{usage}`, `{description}` | One line per executor in help. |
| `UNKNOWN_SUBCOMMAND` | `{typed}`, `{command}`, `{suggestion}` | Did-you-mean prompt for typos. |

A ready-to-copy `messages.yml` lives at
[`examples/paper-sample/src/main/resources/messages.yml`](./examples/paper-sample/src/main/resources/messages.yml).

---

## 🧪 Testing your commands

Add the testkit to your `testImplementation` so you can unit-test commands
without starting a server:

```kotlin
testImplementation("com.github.HanielCota.CommandFramework:core-testkit:0.2.0")
```

```java
@Test
void healCommandWorks() {
    var env = CommandTestKit.create();
    var result = env.framework(new HealCommand())
            .player("Alice").grant("myplugin.heal")
            .dispatch("heal", "");

    DispatchAssert.assertThat(result).succeeded();
}
```

See the testkit's [`CommandTestKit`](./core-testkit/src/main/java/io/github/hanielcota/commandframework/testkit/CommandTestKit.java)
source for the full fluent API (cooldown, permission, confirm assertions).

---

## 🏷️ Annotation cheat sheet

| Annotation | Put on | What it does |
|---|---|---|
| `@Command(name, aliases, description, permission)` | class | Declares a command. `name` is the label; `aliases` are alternatives. |
| `@Execute(sub)` | method | Declares an executor. No `sub` = root (`/command`). With `sub = "foo"` = subcommand (`/command foo`). |
| `@Sender` | parameter | Marks which parameter receives the player/console. Optional if the type is unambiguous. |
| `@Arg(value, greedy, maxLength)` | parameter | Overrides argument name/rules. Use `greedy=true` to capture the rest of the line. |
| `@Optional(value)` | parameter | Default value when the argument is missing. |
| `@Description(value)` | method | Text shown in the auto-generated help. |
| `@Permission(value)` | class or method | Gate a command (method overrides class). |
| `@Cooldown(value, unit, bypassPermission)` | method | Per-sender cooldown; optional bypass permission. |
| `@Confirm(expireSeconds, commandName)` | method | Requires a clickable/typed confirmation. |
| `@RequirePlayer` | class or method | Console gets a typed rejection. |
| `@Async` | method | Runs on a virtual thread; use for DB / HTTP. |
| `@Inject` | field | Injects a dependency bound via `builder.bind(...)`. |

---

## 🛠️ Builder cheat sheet

All builder methods return `this` (chain them). Call `.build()` last.

| Method | Purpose |
|---|---|
| `scanPackage(String)` | Load every `@Command` class in the package. Needs the annotation processor. |
| `command(Object)` | Register a single pre-instantiated command manually. |
| `commands(Object...)` | Same, for several. |
| `bind(Class<T>, T)` | Register a service for `@Inject` / resolver constructors. |
| `resolver(ArgumentResolver<?>)` | Register a custom argument type. |
| `middleware(CommandMiddleware)` | Register a middleware around every dispatch. |
| `message(MessageKey, String)` | Override one message template. |
| `messages(MessageProvider)` | Replace the whole message provider (use `MessageProvider.fromStringMap(...)`). |
| `rateLimit(int, Duration)` | Configure the per-sender limiter. |
| `debug(boolean)` | Log each dispatch phase (useful when something "doesn't fire"). |
| `build()` | Validate, register, return the live `CommandFramework<S>`. |

---

## 🔧 Troubleshooting

### "My command doesn't appear in game"

- Did you call `.build()`? Without it nothing is registered.
- Is the command class inside the package you passed to `scanPackage(...)` (or a subpackage)?
- Did you add the **annotation processor** as a dependency? Without it `scanPackage` finds nothing.
- Rebuild with `./gradlew clean build`. The generated descriptor is produced at compile time.
- Enable `.debug(true)` on the builder and watch the server console for dispatch traces.

### "Compile error: @Arg(greedy = true) must be the last parameter"

That's the annotation processor doing its job. Move the greedy parameter to
the end of the method signature, or use non-greedy (string) otherwise.

### "Unknown command" in game

- Check you registered the plugin correctly (the framework uses Paper's Brigadier
  lifecycle — legacy `plugin.yml` declarations are **not** needed, but the plugin
  itself must load).
- Try `.debug(true)` — if the dispatcher never logs your command, the literal
  was never registered.

### "My `@Inject` field is null"

- The type must be bound in the builder: `builder.bind(MyService.class, myService)`.
- Or the type must be `JavaPlugin`/`ProxyServer`, which are pre-bound.
- Field must be **non-final**, non-static. The framework sets it via reflection after instantiation.

### "Player resolver says 'Player not found' for a valid name"

The built-in resolver calls `server.getPlayerExact(name)` — the player must be
**online** and the name must match exactly. Case sensitivity follows the server's setting.

### `NoClassDefFoundError: net/kyori/adventure/text/minimessage/MiniMessage`

You built a non-shadow jar. The published `paper` / `velocity` jars relocate
Adventure's MiniMessage; if you produce your own fat-jar, use the Gradle
Shadow plugin or declare MiniMessage in your POM.

### "Cooldown doesn't apply"

- Only **successful** executions record cooldown. If parsing fails the cooldown is not set.
- Players with the `bypassPermission` skip cooldowns entirely.

---

## ❓ FAQ

**Do I still need a `plugin.yml`?**
Yes, but **without** a `commands:` section. You only declare `name`, `main`,
`version`, and `api-version`.

**Can one plugin register dozens of commands?**
Yes. `scanPackage("...")` walks every `@Command` class. There's no hard limit.

**Does it work on Spigot?**
No — Paper's Brigadier lifecycle API is required. Use Paper 1.20.6+.

**Does it work with Kotlin?**
Yes, as long as you keep `@Command`/`@Execute` on plain methods (not extension
functions). Kotlin `Unit` return types are treated as `void`.

**Does it block the server thread?**
Only if *your* command body does. Use `@Async` for DB/network calls.

**How do I write multi-level subcommands like `/admin player ban`?**
Use dotted `sub` values: `@Execute(sub = "player ban")`. The dispatcher
matches deepest-first.

**Where are commands declared for `plugin.yml` / `paper-plugin.yml`?**
They aren't. The framework registers them directly with Paper's Brigadier
registrar via `LifecycleEvents.COMMANDS`, which is the modern, recommended path.

---

## 📘 Glossary

| Term | Meaning |
|---|---|
| **Adapter / Bridge** | The thin module (`paper`, `velocity`) that translates between the server's API and the framework's dispatcher. |
| **Actor** | Platform-neutral sender — either a player or console. Use `CommandActor` to write platform-agnostic commands. |
| **Brigadier** | Mojang's command library shipped with Minecraft. Gives tab-completion in the client. |
| **Dispatcher** | The core component that takes a raw typed command and decides what method to call. |
| **Executor** | A method annotated with `@Execute`. The thing that actually runs. |
| **MiniMessage** | Adventure's text format (`<red>`, `<hover>`, `<click>`). Used in message templates. |
| **Resolver** | A converter from a raw string (what the player typed) to a typed object (`Player`, `Kit`, etc.). |
| **Middleware** | Code wrapping every dispatch — runs before and after the executor. |
| **Annotation processor** | Compile-time tool that reads annotations and generates a small metadata file the framework uses to find commands at runtime. Not a runtime dependency. |

---

## 🏗️ Architecture (for curious readers)

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
           |   .bind / .resolver /       |
           |   .middleware / .build()    |
           v                             v
+----------+-----------------------------+------------+
|              CommandFrameworkBuilder (core)         |
|                                                     |
|  InternalCommandBuilder ----> CommandFramework<S>   |
|     - loads generated descriptors                   |
|     - validates metadata / signatures               |
|     - injects fields                                |
|     - builds CommandDispatcher                      |
+-----------+-----------------------------------------+
            |
            v
+-----------+--------------------------+
| CommandDispatcher (pipeline)         |
|  1. permission    4. argument parse  |
|  2. player-only   5. confirmation    |
|  3. cooldown      6. execute         |
|                      (sync / virtual |
|                       thread)        |
+--------------------------------------+
             ^           ^
             |           |
     CooldownMgr   ConfirmationMgr    RateLimiter
     (Caffeine)    (Caffeine)         (Caffeine)
```

### Module layout

| Module | Purpose |
|---|---|
| `annotations/` | Annotation classes (`@Command`, `@Execute`, …). Lightweight — no runtime dependencies. |
| `core/` | Dispatcher, resolvers, managers, message service. Platform-agnostic. |
| `paper/` | Paper adapter using `LifecycleEvents.COMMANDS` Brigadier. |
| `velocity/` | Velocity adapter using `BrigadierCommand`. |
| `processor/` | Compile-time validator + descriptor generator. |
| `core-testkit/` | `CommandTestKit` + `TestSender` + `DispatchAssert`. |
| `gradle-plugin/` | Optional Gradle plugin that wires everything. |

The `paper` and `velocity` jars ship with Caffeine/ClassGraph relocated to
`io.github.hanielcota.commandframework.libs.*` to avoid clashes with other plugins.

---

## 🧾 Compatibility

| Tier | Version |
|---|---|
| Java | 25 (toolchain) |
| Gradle | 9.4.1 (via the wrapper) |
| Paper API | `26.1.2.build.7-alpha` (MC 1.21+) |
| Velocity API | `3.4.0-SNAPSHOT` |
| Adventure | 4.26.1 |
| MiniMessage | Shipped inside the shaded jar |
| Annotation processing | Required for `scanPackage(...)` |

Paper plugins must use the **modern** Paper API (Brigadier lifecycle events).
Legacy `JavaPlugin.getCommand(...)` is not used.

---

## 🤝 Contributing

- See [`CONTRIBUTING.md`](./CONTRIBUTING.md) for local setup and PR conventions.
- Changelog lives in [`CHANGELOG.md`](./CHANGELOG.md).
- Issues and PRs welcome at [HanielCota/CommandFramework](https://github.com/HanielCota/CommandFramework).

## 📄 License

MIT. See [`LICENSE`](./LICENSE).

---

<div align="center">

Built with care for Paper and Velocity networks.
Maintained by [@HanielCota](https://github.com/HanielCota).

</div>
