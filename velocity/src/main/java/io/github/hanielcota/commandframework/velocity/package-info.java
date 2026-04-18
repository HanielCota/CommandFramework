/**
 * Velocity-platform entry point for the command framework. Exposes
 * {@link io.github.hanielcota.commandframework.velocity.VelocityCommandFramework}, the builder
 * that Velocity plugins use to register commands, plus the Velocity platform bridge that adapts
 * the proxy's command manager and scheduler to the framework runtime. Includes the player
 * visibility filter hook that lets plugins suppress suggestions for players the sender should
 * not see.
 */
package io.github.hanielcota.commandframework.velocity;
