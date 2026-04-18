/**
 * Public runtime API of the command framework: the platform-agnostic builder entry points
 * ({@code CommandFrameworkBuilder}), command actor abstractions, argument resolvers, middleware
 * hooks, command result types, the virtual-thread async executor, the framework logger, and
 * the message provider that shapes user-facing feedback. Paper and Velocity modules compose
 * these primitives into their own entry points; consumers targeting neither platform can depend
 * on this package directly.
 */
package io.github.hanielcota.commandframework;
