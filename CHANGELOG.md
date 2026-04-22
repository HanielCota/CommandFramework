# Changelog

All notable changes to CommandFramework are documented in this file.

The project follows Git tag releases such as `v0.3.1`. JitPack consumers should
use those tags as dependency versions.

## [0.3.1] - 2026-04-22

### Added

- Added `llms.txt`, a compact usage and repository guide for LLMs, AI agents,
  and documentation tools.
- Added README badges for Build, CodeQL, Javadoc, and JitPack.
- Added regression tests for route alias/root conflicts, platform root
  registration, and Velocity raw argument tokenization.
- Added a Dependabot auto-merge workflow for safe patch and minor dependency
  updates.

### Changed

- Updated the root Gradle version to `0.3.1` so release artifacts match the Git
  tag.
- Updated README installation snippets to use `v0.3.1`.
- Updated README requirements to Paper API `1.21.11+` and Velocity API
  `3.5.0-SNAPSHOT+`.
- Consolidated Dependabot dependency updates and grouped future Gradle/GitHub
  Actions updates.
- Updated GitHub Actions and CodeQL workflow versions.
- Made CodeQL compile Java with `--rerun-tasks --no-build-cache` so analysis
  still sees source compilation when Gradle caches are warm.

### Fixed

- Rejected root command labels that collide with an alias already registered for
  another root.
- Prevented platform adapters from re-registering the same root command when
  aliases change after later route registrations.
- Trimmed Velocity raw command input before tokenization to avoid empty leading
  arguments.

## [0.3.0] - 2026-04-22

### Changed

- Reworked the framework around the current dispatch pipeline architecture.
- Added production-focused safety features, command registration polish, and
  GitHub repository automation.

[0.3.1]: https://github.com/HanielCota/CommandFramework/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/HanielCota/CommandFramework/releases/tag/v0.3.0
