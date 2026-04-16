# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-04-16

### Added

- Annotation-driven command framework with `@Command`, `@Execute`, `@Arg`, `@Sender`,
  `@Cooldown`, `@Permission`, `@Confirm`, `@Optional`, `@RequirePlayer`, `@Async`, `@Inject`,
  `@Description`.
- `core` module: command dispatcher pipeline (permission, player-only, cooldown, parsing,
  confirm, execution), automatic class scanning via ClassGraph, dependency container,
  custom middlewares, MiniMessage-backed message provider, Caffeine-backed cooldown,
  confirmation, and rate-limit managers.
- `paper` module: `PaperCommandFramework` builder with Paper Brigadier registration via
  `LifecycleEvents.COMMANDS` and built-in `Player` / `World` resolvers.
- `velocity` module: `VelocityCommandFramework` builder with `BrigadierCommand` registration
  and built-in `Player` resolver.
- Gradle Kotlin DSL multi-module build targeting Java 25 with Shadow 9.3.2 and relocated
  Caffeine/ClassGraph.
- Quality gates: Checkstyle 10.20.2, PMD 7.8.0, Error Prone 2.43.0 via
  `net.ltgt.errorprone` 5.1.0.
- GitHub Actions workflows for build (PR + main) and release (tag `v*` or
  `workflow_dispatch`), publishing to GitHub Packages.
- `jitpack.yml` for JitPack publishing with JDK 25 (via SDKMAN `25-tem`).
- `maven-publish` configuration across subprojects with full POM metadata (name,
  description, MIT license, developer, SCM).
- 86 tests: 59 in `core`, 13 in `paper` (Mockito 5.22.0), 14 in `velocity` (Mockito).

### Fixed

- Cooldown is no longer consumed when argument validation fails, so typos do not lock
  legitimate retries out of the command.
- Player resolver no longer rejects Bedrock/Floodgate usernames containing `.` or `*`;
  resolver now delegates name validity to the platform lookup with only an upper-bound
  length check.
- Velocity `manager.register(...)` failures now log at SEVERE instead of silently
  bubbling; registration continues for remaining commands.
- Velocity `Actor.sendMessage` no longer swallows all exceptions — only the no-op
  disconnection path is intentional, other exceptions surface as real bugs.

### Security

- Dependency CVE scanning configured via IntelliJ Mend integration; four CVEs in
  transitive Paper dependencies (`commons-lang3:3.12.0`, `plexus-utils:3.5.1`,
  `guava:31.0.1-jre`, `snakeyaml:1.33`) are not shipped — `paper-api` is declared
  `compileOnly`.

[Unreleased]: https://github.com/HanielCota/CommandFramework/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/HanielCota/CommandFramework/releases/tag/v0.1.0
