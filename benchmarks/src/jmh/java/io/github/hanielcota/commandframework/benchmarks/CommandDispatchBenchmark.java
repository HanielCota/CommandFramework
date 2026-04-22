package io.github.hanielcota.commandframework.benchmarks;

import io.github.hanielcota.commandframework.core.ActorKind;
import io.github.hanielcota.commandframework.core.CommandActor;
import io.github.hanielcota.commandframework.core.CommandDispatcher;
import io.github.hanielcota.commandframework.core.CommandParameter;
import io.github.hanielcota.commandframework.core.CommandResult;
import io.github.hanielcota.commandframework.core.CommandRoute;
import io.github.hanielcota.commandframework.core.ParameterResolverRegistry;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Benchmarks command dispatch throughput.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class CommandDispatchBenchmark {

    private CommandDispatcher dispatcher;
    private CommandActor actor;

    @Setup
    public void setup() {
        dispatcher = CommandDispatcher.builder()
                .throttle(new io.github.hanielcota.commandframework.core.rate.DispatchThrottle(Integer.MAX_VALUE, Duration.ofDays(1)))
                .build();
        actor = new BenchmarkActor();
        ParameterResolverRegistry resolvers = ParameterResolverRegistry.withDefaults();
        dispatcher.register(CommandRoute.builder("kit", (ctx, params) -> CommandResult.success()).build());
        dispatcher.register(
                CommandRoute.builder("kit", (ctx, params) -> CommandResult.success())
                        .path(List.of("give"))
                        .parameters(List.of(new CommandParameter<>("target", String.class, resolvers.find(String.class).orElseThrow(), true)))
                        .build()
        );
    }

    @Benchmark
    public CommandResult dispatchRoot() {
        return dispatcher.dispatch(actor, "kit", List.of());
    }

    @Benchmark
    public CommandResult dispatchSubcommand() {
        return dispatcher.dispatch(actor, "kit", List.of("give", "Steve"));
    }

    private static final class BenchmarkActor implements CommandActor {

        @Override
        public String uniqueId() {
            return "benchmark";
        }

        @Override
        public String name() {
            return "Benchmark";
        }

        @Override
        public ActorKind kind() {
            return ActorKind.CONSOLE;
        }

        @Override
        public boolean hasPermission(String permission) {
            return true;
        }

        @Override
        public void sendMessage(String message) {}
    }
}
