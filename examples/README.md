# Examples

Two standalone Gradle projects showing realistic CommandFramework usage.

| Project | What it shows |
|---|---|
| [`paper-sample`](./paper-sample) | Paper plugin with a heal/economy-style command family, cooldowns, confirmations, permissions, and dependency injection. |
| [`velocity-sample`](./velocity-sample) | Velocity plugin demonstrating cross-server `find`/`kick` commands. |

## Running locally

Each example is a self-contained Gradle project. From the project root:

```bash
cd examples/paper-sample
./gradlew build
```

The resulting `build/libs/paper-sample-1.0.0.jar` can be dropped into a Paper server's
`plugins/` folder. Velocity works the same way.

## Dependency source

These examples pull CommandFramework from **JitPack** using the `0.1.0` tag - no local
installation required. To point at a newer release, edit the `commandframework` version
in each `build.gradle.kts`.
