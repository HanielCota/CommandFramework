# Contributing

Thanks for considering a contribution. This document covers the minimum you need to run
the build locally and how PRs are handled.

## Prerequisites

- JDK 25 (Temurin recommended) — Gradle toolchain will auto-provision if missing, but
  having it installed is faster.
- Git.

No IDE is required, but IntelliJ IDEA 2025.3+ supports Java 25 previews cleanly.

## Build and test

```bash
./gradlew build
```

This runs, in order, for every subproject (`core`, `paper`, `velocity`):

1. `compileJava` — with Error Prone 2.43.0 via `net.ltgt.errorprone` 5.1.0.
2. `checkstyleMain` — using `config/checkstyle/checkstyle.xml` (Checkstyle 10.20.2).
3. `pmdMain` — using `config/pmd/ruleset.xml` (PMD 7.8.0).
4. `test` — JUnit 5 + Mockito 5.22.0 (Paper/Velocity use `-javaagent` for Mockito
   attach on Java 21+).
5. `shadowJar` — produces relocated fat jars for `paper` and `velocity`.

Target test count as of 0.1.0: **86 tests** (`core` 59, `paper` 13, `velocity` 14).

### Running a single test

```bash
./gradlew :core:test --tests '*cooldownBlocks*'
```

### Publishing locally

```bash
./gradlew publishToMavenLocal
```

Artifacts land under `~/.m2/repository/io/github/hanielcota/commandframework/`.

## Code conventions

- Java 25 language level. Records for immutable data, sealed interfaces for closed
  hierarchies, pattern matching in `switch`/`instanceof` wherever it makes code clearer.
- `core` is platform-agnostic; `paper` and `velocity` are thin adapters around the core
  `PlatformBridge` contract.
- Public API surface lives in `io.github.hanielcota.commandframework.*` (non-`internal`).
  Anything under `.internal.` is subject to change without notice.
- Class-level Javadoc is required for every non-test public API class and every
  `internal/` collaborator.
- Avoid adding dependencies. Core is deliberately small (Adventure, Caffeine, ClassGraph).

### Style checks

Checkstyle and PMD must pass. If a rule genuinely obstructs a valid fix, discuss in the
PR before suppressing — do not introduce new `@SuppressWarnings` without justification.

Error Prone runs on every `compileJava`. An Error Prone error fails the build. To
suppress a specific false positive, use `@SuppressWarnings("BugPatternName")` with a
one-line comment explaining why.

## Tests

- New behavior → new test. New public API → at least one test per public method.
- Tests live under `{module}/src/test/java/...` mirroring the main package structure.
- Prefer pure Mockito over MockBukkit for Paper adapter tests unless a real lifecycle
  is unavoidable; Velocity has no mocking framework — use Mockito.
- Avoid `Thread.sleep` in tests. If a timing test is unavoidable, document the margin
  and the tradeoff.

## Pull requests

1. Fork and branch from `main`.
2. Keep commits focused. Prefer a handful of meaningful commits over dozens of fixups.
3. PR description should describe **what changed and why**. Link any issue it resolves.
4. CI (build.yml) must be green before review.
5. At least one approval required. Rebase, don't merge, if the base moves during review.

## Releases

Releases are cut from annotated tags named `vMAJOR.MINOR.PATCH`:

```bash
git tag -a v0.1.1 -m "CommandFramework 0.1.1"
git push origin v0.1.1
```

The `release.yml` workflow builds with `-Pversion=0.1.1`, publishes to GitHub Packages,
and creates a GitHub Release with auto-generated notes plus the three module jars.
Update `CHANGELOG.md` before tagging.

## Reporting issues

Open a GitHub issue with:
- Exact Paper or Velocity version.
- A minimal reproducer.
- Stack trace or unexpected behavior description.
