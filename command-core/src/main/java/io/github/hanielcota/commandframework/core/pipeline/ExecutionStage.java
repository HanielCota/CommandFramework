package io.github.hanielcota.commandframework.core.pipeline;

import io.github.hanielcota.commandframework.core.CommandContext;
import io.github.hanielcota.commandframework.core.CommandInterceptor;
import io.github.hanielcota.commandframework.core.CommandLogger;
import io.github.hanielcota.commandframework.core.CommandMessenger;
import io.github.hanielcota.commandframework.core.CommandResult;
import io.github.hanielcota.commandframework.core.CommandStatus;
import io.github.hanielcota.commandframework.core.ParsedParameter;
import io.github.hanielcota.commandframework.core.dispatch.CommandParameterParser;
import io.github.hanielcota.commandframework.core.dispatch.ParameterParseOutcome;
import io.github.hanielcota.commandframework.core.safety.SafeLogText;
import io.github.hanielcota.commandframework.core.usage.UsageFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parses parameters and invokes the executor, including interceptors.
 */
public final class ExecutionStage implements CommandDispatchStage {

    private final CommandParameterParser parser;
    private final UsageFormatter usageFormatter;
    private final CommandMessenger messenger;
    private final List<CommandInterceptor> interceptors;
    private final CommandLogger logger;
    private final SafeLogText safeLogText;

    public ExecutionStage(
            CommandParameterParser parser,
            UsageFormatter usageFormatter,
            CommandMessenger messenger,
            List<CommandInterceptor> interceptors,
            CommandLogger logger,
            SafeLogText safeLogText) {
        this.parser = Objects.requireNonNull(parser, "parser");
        this.usageFormatter = Objects.requireNonNull(usageFormatter, "usageFormatter");
        this.messenger = Objects.requireNonNull(messenger, "messenger");
        this.interceptors = List.copyOf(interceptors);
        this.logger = Objects.requireNonNull(logger, "logger");
        this.safeLogText = Objects.requireNonNull(safeLogText, "safeLogText");
    }

    @Override
    public CommandResult process(CommandContext context, DispatchContinuation continuation) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(continuation, "continuation");
        return switch (parser.parse(context)) {
            case ParameterParseOutcome.Success success -> {
                CommandContext withParams = context.withParsedParameters(success.parameters());
                yield execute(withParams, success.parameters());
            }
            case ParameterParseOutcome.Failure failure -> {
                messenger.notifyParseFailure(context, failure.failureValue().invalidValue(), failure.failureValue().expectedValue());
                yield messenger.invalidUsage(context, usageFormatter.format(context.route()));
            }
        };
    }

    private CommandResult execute(CommandContext context, List<ParsedParameter<?>> parameters) {
        List<CommandInterceptor> all = allInterceptors(context);
        List<CommandInterceptor> succeeded = new ArrayList<>();
        CommandResult before = before(context, all, succeeded);
        if (!before.isSuccess()) {
            after(context, before, succeeded);
            return before;
        }
        CommandResult executed = invoke(context, parameters);
        return after(context, executed, all);
    }

    private List<CommandInterceptor> allInterceptors(CommandContext context) {
        List<CommandInterceptor> all = new ArrayList<>(interceptors.size() + context.route().interceptors().size());
        all.addAll(interceptors);
        all.addAll(context.route().interceptors());
        return all;
    }

    /**
     * Runs before-callbacks left-to-right. Only interceptors that returned
     * success from {@code before} are added to {@code succeeded}. When a
     * before-callback fails, it is NOT added to the succeeded list, so
     * {@code after} is called only on interceptors that passed before.
     */
    private CommandResult before(CommandContext context, List<CommandInterceptor> all, List<CommandInterceptor> succeeded) {
        for (CommandInterceptor interceptor : all) {
            CommandResult result = interceptor.before(context);
            Objects.requireNonNull(result, "interceptor.before returned null");
            if (!result.isSuccess()) {
                return result;
            }
            succeeded.add(interceptor);
        }
        return CommandResult.success();
    }

    private CommandResult after(CommandContext context, CommandResult result, List<CommandInterceptor> all) {
        CommandResult current = result;
        for (int index = all.size() - 1; index >= 0; index--) {
            current = all.get(index).after(context, current);
            Objects.requireNonNull(current, "interceptor.after returned null");
        }
        return current;
    }

    private CommandResult invoke(CommandContext context, List<ParsedParameter<?>> parameters) {
        try {
            CommandResult result = context.route().executor().execute(context, parameters);
            return Objects.requireNonNull(result, "executor returned null");
        } catch (RuntimeException exception) {
            String route = safeLogText.clean(context.route().canonicalPath());
            logger.warn("Command execution failed for route: " + route, exception);
            return CommandResult.failure(CommandStatus.ERROR);
        }
    }
}
