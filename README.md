# CommandFramework

[![Build](https://github.com/HanielCota/CommandFramework/actions/workflows/build.yml/badge.svg)](https://github.com/HanielCota/CommandFramework/actions/workflows/build.yml)
[![CodeQL](https://github.com/HanielCota/CommandFramework/actions/workflows/codeql.yml/badge.svg)](https://github.com/HanielCota/CommandFramework/actions/workflows/codeql.yml)
[![Javadoc](https://github.com/HanielCota/CommandFramework/actions/workflows/javadoc.yml/badge.svg)](https://github.com/HanielCota/CommandFramework/actions/workflows/javadoc.yml)
[![JitPack](https://jitpack.io/v/HanielCota/CommandFramework.svg)](https://jitpack.io/#HanielCota/CommandFramework)

A lightweight, type-safe command framework for Minecraft server plugins, supporting both **Paper** (Bukkit) and **Velocity** proxy platforms.

## Features

- **Platform Abstraction**: Core module is platform-agnostic — adapters for Paper and Velocity provided
- **Annotation-Based Commands**: Declare commands with `@Command`, `@Subcommand`, `@Permission`, `@Cooldown`, `@Async`
- **Type-Safe Parameters**: Automatic argument parsing with `ParameterResolver` registry (String, int, long, double, boolean, enums, greedy strings)
- **Pipeline Architecture**: Dispatch pipeline with guard stage (permission, sender, cooldown) and execution stage (parse + invoke + interceptors)
- **Async Support**: Mark routes with `@Async` for off-thread execution
- **Rate Limiting & Throttling**: Built-in token-bucket rate limiting and input sanitization
- **Tab Completion**: Automatic suggestion engine based on route tree
- **Production Ready**: Thread-safe actor caches, debounced messages, safe logging, configurable overlays

## Modules

| Module | Description |
|--------|-------------|
| `command-core` | Core framework — dispatcher, routing, parsing, pipeline, rate limiting, cooldowns |
| `command-annotations` | Annotation scanning (`@Command`, `@Subcommand`, etc.) and method binding |
| `command-paper` | Paper/Bukkit adapter — Brigadier lifecycle + legacy Bukkit command map |
| `command-velocity` | Velocity proxy adapter — SimpleCommand + RawCommand bridges |
| `examples` | Sample plugins for Paper and Velocity |
| `benchmarks` | JMH benchmarks |

## Installation (JitPack)

Add the JitPack repository to your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
}
```

Add the dependencies you need:

```kotlin
dependencies {
    // Paper plugin
    implementation("com.github.HanielCota.CommandFramework:command-paper:v0.3.1")
    
    // Velocity plugin
    implementation("com.github.HanielCota.CommandFramework:command-velocity:v0.3.1")
    
    // Or individual modules
    implementation("com.github.HanielCota.CommandFramework:command-core:v0.3.1")
    implementation("com.github.HanielCota.CommandFramework:command-annotations:v0.3.1")
}
```

For snapshots from `main`, replace `v0.3.1` with `main-SNAPSHOT`.

## Requirements

- **Java**: 21+
- **Paper API**: 1.21.11+
- **Velocity API**: 3.5.0-SNAPSHOT+

## Quick Start

### Paper Plugin

```java
public final class MyPlugin extends JavaPlugin {
    private PaperCommandFramework commands;

    @Override
    public void onEnable() {
        commands = PaperCommandFramework.builder(this).build();
        commands.registerAnnotated(new MyCommands());
    }

    @Override
    public void onDisable() {
        if (commands != null) commands.shutdown();
    }
}

@Command("kit")
public class MyCommands {
    @Default
    @OnlyPlayer
    public void onDefault(CommandActor actor) {
        actor.sendMessage("Use /kit give <player>");
    }

    @Subcommand("give")
    @Permission("kit.give")
    @Cooldown(value = 3, unit = TimeUnit.SECONDS)
    public void onGive(CommandActor actor, String target) {
        actor.sendMessage("Kit sent to " + target);
    }
}
```

### Velocity Plugin

```java
@Plugin(id = "my-plugin", name = "My Plugin", version = "1.0.0")
public class MyVelocityPlugin {
    private final VelocityCommandFramework<MyVelocityPlugin> commands;

    @Inject
    public MyVelocityPlugin(ProxyServer server, Logger logger) {
        this.commands = VelocityCommandFramework.builder(server, this).build();
        commands.registerAnnotated(new MyCommands());
    }
}
```

## Thread Safety

- **Paper**: Most Bukkit API calls must run on the main thread. When using `@Async`, the framework runs the pipeline on the configured executor. The Paper adapter automatically schedules `sendMessage` back to the main thread. Other API calls (Player, World, Inventory) must be scheduled manually by the command executor.
- **Velocity**: The proxy API is generally thread-safe. `@Async` is safe for most operations.

## Building

```bash
./gradlew build
```

## Documentation

- [LLM usage guide](llms.txt)
- [Changelog](CHANGELOG.md)
- [Javadoc](https://hanielcota.github.io/CommandFramework/)

## License

MIT License — see [LICENSE](LICENSE) for details.
