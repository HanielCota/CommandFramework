/**
 * Paper-platform entry point for the command framework. Exposes
 * {@link io.github.hanielcota.commandframework.paper.PaperCommandFramework}, the builder that
 * Paper plugins instantiate from their {@code onEnable}, and
 * {@link io.github.hanielcota.commandframework.paper.PaperPlatformBridge}, which wires Bukkit and
 * Paper services (scheduler, server, command map, audiences) into the platform-agnostic runtime.
 */
package io.github.hanielcota.commandframework.paper;
