/**
 * Test harness for the command framework. Provides
 * {@link io.github.hanielcota.commandframework.testkit.CommandTestKit} for spinning up an
 * in-process framework without a live Paper or Velocity server,
 * {@link io.github.hanielcota.commandframework.testkit.TestSender} as a controllable command
 * actor, and {@link io.github.hanielcota.commandframework.testkit.DispatchAssert} for fluent
 * assertions on dispatch outcomes. Intended for unit-testing command classes in isolation.
 */
package io.github.hanielcota.commandframework.testkit;
