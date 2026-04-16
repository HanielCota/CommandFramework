# CommandFramework

Annotation-based command framework for Paper and Velocity with a shared core, zero YAML registration, automatic parsing,
permissions, cooldowns, confirmation flows, help, and tab-complete.

- Author: Haniel Cota
- GitHub: https://github.com/HanielCota
- Package: `io.github.hanielcota.commandframework`
- License: MIT

## Highlights

- Multi-module Gradle Kotlin DSL project: `core`, `paper`, `velocity`
- Java 25 toolchain
- Zero `plugin.yml` / zero `commands:` registration
- Shared pipeline with permission, player-only, cooldown, parsing, confirm, and execution stages
- Automatic command scanning with field injection via `@Inject`
- Custom argument resolvers and custom middlewares
- Thread-safe cooldowns, confirmations, rate-limits, and immutable command metadata

## Installation

JitPack publishes the multi-module artifacts under `com.github.HanielCota.CommandFramework:<module>:<tag>`. This follows
the official JitPack multi-module convention for repository modules.

Add the repository in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://jitpack.io")
    }
}
```

Add one or more modules:

```kotlin
dependencies {
    implementation("com.github.HanielCota.CommandFramework:core:<tag>")
    implementation("com.github.HanielCota.CommandFramework:paper:<tag>")
    implementation("com.github.HanielCota.CommandFramework:velocity:<tag>")
}
```

If you want the whole repository aggregate published by JitPack, use:

```kotlin
dependencies {
    implementation("com.github.HanielCota:CommandFramework:<tag>")
}
```

### Alternative: GitHub Packages

Releases are also published to GitHub Packages. Authentication is required even for public packages.

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
    implementation("io.github.hanielcota.commandframework:core:<version>")
    implementation("io.github.hanielcota.commandframework:paper:<version>")
    implementation("io.github.hanielcota.commandframework:velocity:<version>")
}
```

Configure credentials via `~/.gradle/gradle.properties` (`gpr.user` / `gpr.key`) or `GITHUB_ACTOR` / `GITHUB_TOKEN`
environment variables. The token needs `read:packages` scope.

## Quick Start

### Paper

```java
import io.github.hanielcota.commandframework.MessageKey;
import io.github.hanielcota.commandframework.paper.PaperCommandFramework;
import org.bukkit.plugin.java.JavaPlugin;

public final class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        PaperCommandFramework.paper(this)
            .bind(EconomyService.class, new EconomyService())
            .message(MessageKey.NO_PERMISSION, "<red>Sem permissao!")
            .build();
    }
}
```

### Velocity

```java
import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.hanielcota.commandframework.velocity.VelocityCommandFramework;

@Plugin(id = "myplugin", name = "MyPlugin", version = "1.0.0")
public final class MyPlugin {
    @Inject
    public MyPlugin(ProxyServer server) {
        VelocityCommandFramework.velocity(server, this).build();
    }
}
```

### Example Command

```java
import io.github.hanielcota.commandframework.annotation.Arg;
import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Confirm;
import io.github.hanielcota.commandframework.annotation.Cooldown;
import io.github.hanielcota.commandframework.annotation.Description;
import io.github.hanielcota.commandframework.annotation.Execute;
import io.github.hanielcota.commandframework.annotation.Inject;
import io.github.hanielcota.commandframework.annotation.Optional;
import io.github.hanielcota.commandframework.annotation.Permission;
import io.github.hanielcota.commandframework.annotation.Sender;
import java.util.concurrent.TimeUnit;
import org.bukkit.entity.Player;

@Command(name = "eco", aliases = {"economy"}, description = "Economy commands")
@Permission("myplugin.eco")
public final class EconomyCommand {

    @Inject private EconomyService economy;

    @Execute
    @Description("Check your balance")
    public void balance(@Sender Player sender) {
        sender.sendMessage(net.kyori.adventure.text.Component.text(
            "Balance: $" + economy.getBalance(sender.getUniqueId())
        ));
    }

    @Execute(sub = "pay")
    @Description("Pay another player")
    @Cooldown(value = 30, unit = TimeUnit.SECONDS, bypassPermission = "myplugin.eco.cooldown.bypass")
    public void pay(@Sender Player sender, Player target, double amount) {
        economy.transfer(sender.getUniqueId(), target.getUniqueId(), amount);
    }

    @Execute(sub = "reset")
    @Permission("myplugin.eco.admin")
    @Confirm(expireSeconds = 10, commandName = "confirmar")
    public void reset(@Sender Player sender, Player target) {
        economy.reset(target.getUniqueId());
    }

    @Execute(sub = "note")
    @Permission("myplugin.eco.admin")
    public void note(@Sender Player sender, Player target, @Arg(greedy = true) String message) {
        economy.addNote(target.getUniqueId(), message);
    }

    @Execute(sub = "give")
    @Permission("myplugin.eco.admin")
    public void give(@Sender Player sender, Player target, double amount, @Optional("false") boolean silent) {
        economy.give(target.getUniqueId(), amount);
    }
}
```

## Annotation Reference

| Annotation                                         | Target        | Purpose                                                                                                  |
|----------------------------------------------------|---------------|----------------------------------------------------------------------------------------------------------|
| `@Command(name, aliases, description, permission)` | class         | Declares a top-level command root                                                                        |
| `@Execute(sub)`                                    | method        | Declares a root executor when empty, otherwise a subcommand executor                                     |
| `@Sender`                                          | parameter     | Injects `CommandActor` or a supported platform sender/player type                                        |
| `@Arg(value, greedy, maxLength)`                   | parameter     | Overrides inferred argument metadata                                                                     |
| `@Permission`                                      | class, method | Declares base permission on class or override permission on method                                       |
| `@Cooldown`                                        | method        | Applies a command cooldown with optional bypass permission                                               |
| `@Optional`                                        | parameter     | Declares a default value when an argument is omitted                                                     |
| `@Async`                                           | method        | Runs method invocation on a virtual thread after parsing; async sender injection must use `CommandActor` |
| `@RequirePlayer`                                   | class, method | Restricts execution to players; class-level usage applies to all executors in that class                 |
| `@Confirm`                                         | method        | Delays execution until `/confirm` or a custom confirmation command is used                               |
| `@Description`                                     | method        | Supplies the help text for an executor                                                                   |
| `@Inject`                                          | field         | Injects a singleton binding registered on the builder                                                    |

## Custom Resolver

```java
import io.github.hanielcota.commandframework.ArgumentResolutionContext;
import io.github.hanielcota.commandframework.ArgumentResolver;

public record RegionId(String value) {}

public final class RegionResolver implements ArgumentResolver<RegionId> {
    @Override
    public Class<RegionId> type() {
        return RegionId.class;
    }

    @Override
    public RegionId resolve(ArgumentResolutionContext context, String input) {
        return new RegionId(input.toLowerCase());
    }

    @Override
    public java.util.List<String> suggest(io.github.hanielcota.commandframework.CommandActor actor, String currentInput) {
        return java.util.List.of("spawn", "market", "arena").stream()
            .filter(value -> value.startsWith(currentInput))
            .toList();
    }
}
```

Register it:

```java
PaperCommandFramework.paper(this)
    .resolver(new RegionResolver())
    .build();
```

## Custom Middleware

Custom middlewares run before the framework pipeline.

```java
import io.github.hanielcota.commandframework.CommandMiddleware;
import io.github.hanielcota.commandframework.CommandResult;

CommandMiddleware audit = (context, chain) -> {
    System.out.println("Running " + context.commandPath() + " for " + context.actor().name());
    CommandResult result = chain.proceed(context);
    System.out.println("Finished " + context.commandPath() + " with " + result.getClass().getSimpleName());
    return result;
};

PaperCommandFramework.paper(this)
    .middleware(audit)
    .build();
```

## Notes

- Argument names are inferred from Java parameter names when compiled with `-parameters`.
- Platform resolvers run during parsing on the caller thread; only `@Async` method invocation moves to a virtual thread.
- Async executors must inject `@Sender CommandActor`; raw platform sender types are rejected at startup to avoid
  off-thread API access.
- `@RequirePlayer` on a class applies to every executor declared in that class; method-level usage marks only that
  executor.
- Global rate-limit defaults to 30 commands per 10 seconds per player and is configurable with
  `.rateLimit(int, Duration)`.
- Confirmation commands are auto-registered from `@Confirm(commandName = "...")`.
- Built-in message templates are in English and can be replaced per key with `.message(...)` or entirely with
  `.messages(...)`.
