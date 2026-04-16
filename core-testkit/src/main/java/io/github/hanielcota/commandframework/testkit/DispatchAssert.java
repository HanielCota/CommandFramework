package io.github.hanielcota.commandframework.testkit;

import io.github.hanielcota.commandframework.CommandResult;
import java.time.Duration;

/**
 * Fluent assertion over a {@link CommandResult} returned by
 * {@link CommandTestKit#dispatch(String, String)}.
 *
 * <p>Every method either returns {@code this} (chain further checks) or throws
 * {@link AssertionError} with an informative message.
 */
public final class DispatchAssert {

    private final TestSender sender;
    private final String label;
    private final CommandResult result;

    DispatchAssert(TestSender sender, String label, CommandResult result) {
        this.sender = sender;
        this.label = label;
        this.result = result;
    }

    /**
     * Returns the raw result for callers that need non-assertion inspection.
     *
     * @return the command result
     */
    public CommandResult result() {
        return this.result;
    }

    /**
     * Returns the sender that produced the result — useful for checking messages.
     *
     * @return the sender
     */
    public TestSender sender() {
        return this.sender;
    }

    public DispatchAssert assertSuccess() {
        if (!(this.result instanceof CommandResult.Success)) {
            throw this.fail("Success");
        }
        return this;
    }

    public DispatchAssert assertHandled() {
        if (!(this.result instanceof CommandResult.Handled)) {
            throw this.fail("Handled");
        }
        return this;
    }

    public DispatchAssert assertFailure() {
        if (!(this.result instanceof CommandResult.Failure)) {
            throw this.fail("Failure");
        }
        return this;
    }

    public DispatchAssert assertInvalidArgs(String argumentName) {
        if (!(this.result instanceof CommandResult.InvalidArgs invalid)) {
            throw this.fail("InvalidArgs");
        }
        if (!invalid.argumentName().equals(argumentName)) {
            throw new AssertionError("Expected InvalidArgs for argument '" + argumentName
                    + "' but got '" + invalid.argumentName() + "'");
        }
        return this;
    }

    public DispatchAssert assertNoPermission() {
        if (!(this.result instanceof CommandResult.NoPermission)) {
            throw this.fail("NoPermission");
        }
        return this;
    }

    public DispatchAssert assertNoPermission(String permission) {
        if (!(this.result instanceof CommandResult.NoPermission denied)) {
            throw this.fail("NoPermission");
        }
        if (!denied.permission().equals(permission)) {
            throw new AssertionError("Expected missing permission '" + permission
                    + "' but got '" + denied.permission() + "'");
        }
        return this;
    }

    public DispatchAssert assertPlayerOnly() {
        if (!(this.result instanceof CommandResult.PlayerOnly)) {
            throw this.fail("PlayerOnly");
        }
        return this;
    }

    public DispatchAssert assertCooldownActive() {
        if (!(this.result instanceof CommandResult.CooldownActive)) {
            throw this.fail("CooldownActive");
        }
        return this;
    }

    public DispatchAssert assertCooldownRemainingAtLeast(Duration minimum) {
        if (!(this.result instanceof CommandResult.CooldownActive active)) {
            throw this.fail("CooldownActive");
        }
        if (active.remaining().compareTo(minimum) < 0) {
            throw new AssertionError("Expected cooldown remaining >= " + minimum
                    + " but got " + active.remaining());
        }
        return this;
    }

    public DispatchAssert assertPendingConfirmation(String confirmCommand) {
        if (!(this.result instanceof CommandResult.PendingConfirmation pending)) {
            throw this.fail("PendingConfirmation");
        }
        if (!pending.commandName().equals(confirmCommand)) {
            throw new AssertionError("Expected pending confirmation command '" + confirmCommand
                    + "' but got '" + pending.commandName() + "'");
        }
        return this;
    }

    public DispatchAssert assertHelpShown() {
        if (!(this.result instanceof CommandResult.HelpShown)) {
            throw this.fail("HelpShown");
        }
        return this;
    }

    public DispatchAssert assertRateLimited() {
        if (!(this.result instanceof CommandResult.RateLimited)) {
            throw this.fail("RateLimited");
        }
        return this;
    }

    /**
     * Asserts the last plain-text message the sender received contains {@code fragment}.
     *
     * @param fragment substring that must appear in the last message
     * @return this
     */
    public DispatchAssert assertLastMessageContains(String fragment) {
        String last = this.sender.lastMessage();
        if (!last.contains(fragment)) {
            throw new AssertionError("Expected last message to contain '" + fragment
                    + "' but was '" + last + "'");
        }
        return this;
    }

    private AssertionError fail(String expected) {
        return new AssertionError("Expected " + expected + " for /"
                + this.label + " but got "
                + this.result.getClass().getSimpleName() + ": " + this.result);
    }
}
