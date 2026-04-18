package io.github.hanielcota.commandframework.processor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandFrameworkProcessorTest {

    @Test
    void invalidReturnTypeFailsCompilation() throws IOException {
        CompilationResult result = this.compile("InvalidReturnCommand.java", """
                package example;

                import io.github.hanielcota.commandframework.annotation.Command;
                import io.github.hanielcota.commandframework.annotation.Execute;

                @Command(name = "invalid")
                public final class InvalidReturnCommand {
                    @Execute
                    public String execute() {
                        return "invalid";
                    }
                }
                """);

        assertFalse(result.success());
        assertTrue(result.messages().stream().anyMatch(message -> message.contains("void or CommandResult")));
    }

    @Test
    void asyncSenderTypeFailsCompilation() throws IOException {
        CompilationResult result = this.compile("InvalidAsyncCommand.java", """
                package example;

                import io.github.hanielcota.commandframework.annotation.Async;
                import io.github.hanielcota.commandframework.annotation.Command;
                import io.github.hanielcota.commandframework.annotation.Execute;
                import io.github.hanielcota.commandframework.annotation.Sender;

                @Command(name = "invalidasync")
                public final class InvalidAsyncCommand {
                    @Execute
                    @Async
                    public void execute(@Sender String sender) {
                    }
                }
                """);

        assertFalse(result.success());
        assertTrue(result.messages().stream().anyMatch(message ->
                message.contains("@Async methods must declare @Sender CommandActor")));
        assertTrue(result.messages().stream().anyMatch(message ->
                message.contains("Bukkit.getPlayer")),
                "diagnostic should point at the Bukkit re-resolution fix");
    }

    @Test
    void multiTokenSubNameFailsCompilation() throws IOException {
        CompilationResult result = this.compile("MultiTokenSubCommand.java", """
                package example;

                import io.github.hanielcota.commandframework.annotation.Command;
                import io.github.hanielcota.commandframework.annotation.Execute;

                @Command(name = "player")
                public final class MultiTokenSubCommand {
                    @Execute(sub = "player ban")
                    public void execute() {
                    }
                }
                """);

        assertFalse(result.success());
        assertTrue(result.messages().stream().anyMatch(message ->
                message.contains("must be a single token")));
        assertTrue(result.messages().stream().anyMatch(message ->
                message.contains("player ban")),
                "diagnostic should echo the offending sub value");
        assertTrue(result.messages().stream().anyMatch(message ->
                message.contains("split into two methods")),
                "diagnostic should suggest splitting into separate @Execute methods");
    }

    @Test
    void tabInSubFailsCompilation() throws IOException {
        CompilationResult result = this.compile("TabSubCommand.java", """
                package example;

                import io.github.hanielcota.commandframework.annotation.Command;
                import io.github.hanielcota.commandframework.annotation.Execute;

                @Command(name = "tab")
                public final class TabSubCommand {
                    @Execute(sub = "foo\\tbar")
                    public void execute() {
                    }
                }
                """);

        assertFalse(result.success(),
                "tab inside sub name should be rejected — the tokenizer splits on \\s+, so it would break routing at runtime");
        assertTrue(result.messages().stream().anyMatch(message ->
                message.contains("single token") || message.contains("letters, numbers")),
                () -> "expected a whitespace/label diagnostic, got: " + result.messages());
    }

    @Test
    void dotInSubFailsCompilation() throws IOException {
        CompilationResult result = this.compile("DotSubCommand.java", """
                package example;

                import io.github.hanielcota.commandframework.annotation.Command;
                import io.github.hanielcota.commandframework.annotation.Execute;

                @Command(name = "dot")
                public final class DotSubCommand {
                    @Execute(sub = "foo.bar")
                    public void execute() {
                    }
                }
                """);

        assertFalse(result.success(),
                "characters outside [a-zA-Z0-9_-] in sub should be rejected — @Command(name) and @Confirm(commandName) already enforce the same rule");
        assertTrue(result.messages().stream().anyMatch(message ->
                message.contains("letters, numbers")),
                () -> "expected a label-character diagnostic, got: " + result.messages());
    }

    @Test
    void emptySubIsRoot() throws IOException {
        CompilationResult result = this.compile("RootSubCommand.java", """
                package example;

                import io.github.hanielcota.commandframework.annotation.Command;
                import io.github.hanielcota.commandframework.annotation.Execute;

                @Command(name = "root")
                public final class RootSubCommand {
                    @Execute
                    public void execute() {
                    }
                }
                """);

        assertTrue(result.success(),
                () -> "empty sub should be accepted as root executor, got: " + result.messages());
    }

    @Test
    void invalidCommandMetadataFailsCompilation() throws IOException {
        CompilationResult result = this.compile("InvalidMetadataCommand.java", """
                package example;

                import io.github.hanielcota.commandframework.annotation.Arg;
                import io.github.hanielcota.commandframework.annotation.Command;
                import io.github.hanielcota.commandframework.annotation.Confirm;
                import io.github.hanielcota.commandframework.annotation.Cooldown;
                import io.github.hanielcota.commandframework.annotation.Execute;
                import java.util.concurrent.TimeUnit;

                @Command(name = "bad name")
                public final class InvalidMetadataCommand {
                    @Execute
                    @Cooldown(value = 0, unit = TimeUnit.SECONDS)
                    @Confirm(expireSeconds = 0, commandName = "   ")
                    public void execute(@Arg(maxLength = 0) String value) {
                    }
                }
                """);

        assertFalse(result.success());
        assertTrue(result.messages().stream().anyMatch(message -> message.contains("@Command(name)")));
        assertTrue(result.messages().stream().anyMatch(message -> message.contains("@Cooldown value must be > 0")));
        assertTrue(result.messages().stream().anyMatch(message -> message.contains("@Confirm(expireSeconds)")));
        assertTrue(result.messages().stream().anyMatch(message -> message.contains("@Arg(maxLength)")));
    }

    @Test
    void greedyWithOptionalAnnotationFailsCompilation() throws IOException {
        CompilationResult result = this.compile("GreedyOptionalCommand.java", """
                package example;

                import io.github.hanielcota.commandframework.annotation.Arg;
                import io.github.hanielcota.commandframework.annotation.Command;
                import io.github.hanielcota.commandframework.annotation.Execute;
                import io.github.hanielcota.commandframework.annotation.Optional;

                @Command(name = "greedyopt")
                public final class GreedyOptionalCommand {
                    @Execute
                    public void execute(@Optional("x") @Arg(greedy = true) String rest) {
                    }
                }
                """);

        assertFalse(result.success(),
                "greedy + @Optional should be rejected — the combination is ambiguous");
        assertTrue(result.messages().stream().anyMatch(message ->
                        message.contains("greedy") && message.contains("@Optional")),
                () -> "expected greedy+@Optional diagnostic, got: " + result.messages());
    }

    @Test
    void greedyWithJavaOptionalTypeFailsCompilation() throws IOException {
        CompilationResult result = this.compile("GreedyJavaOptionalCommand.java", """
                package example;

                import io.github.hanielcota.commandframework.annotation.Arg;
                import io.github.hanielcota.commandframework.annotation.Command;
                import io.github.hanielcota.commandframework.annotation.Execute;
                import java.util.Optional;

                @Command(name = "greedyjopt")
                public final class GreedyJavaOptionalCommand {
                    @Execute
                    public void execute(@Arg(greedy = true) Optional<String> rest) {
                    }
                }
                """);

        assertFalse(result.success(),
                "greedy + Optional<String> should be rejected — the combination is ambiguous");
        assertTrue(result.messages().stream().anyMatch(message ->
                        message.contains("greedy") && message.contains("java.util.Optional")),
                () -> "expected greedy+Optional diagnostic, got: " + result.messages());
    }

    @Test
    void descriptionWithQuotesAndNewlinesDoesNotBreakGeneratedSource() throws IOException {
        // Hardening: ensures stringLiteral() escaping keeps generated code valid even when
        // developer-supplied annotation strings contain Java-source metacharacters. A regression
        // here would enable source injection at annotation-processing time.
        CompilationResult result = this.compile("QuotedDescriptionCommand.java", """
                package example;

                import io.github.hanielcota.commandframework.annotation.Command;
                import io.github.hanielcota.commandframework.annotation.Description;
                import io.github.hanielcota.commandframework.annotation.Execute;

                @Command(name = "quoted", description = "has \\"quotes\\" and\\nnewline")
                public final class QuotedDescriptionCommand {
                    @Execute
                    @Description("sub \\"desc\\" \\\\ backslash")
                    public void execute() {
                    }
                }
                """);

        assertTrue(result.success(),
                () -> "generator must escape metacharacters; got: " + result.messages());
    }

    @Test
    void validCommandGeneratesDescriptorService() throws IOException {
        CompilationResult result = this.compile("ValidCommand.java", """
                package example;

                import io.github.hanielcota.commandframework.CommandResult;
                import io.github.hanielcota.commandframework.annotation.Command;
                import io.github.hanielcota.commandframework.annotation.Execute;

                @Command(name = "valid", aliases = {"ok"})
                public final class ValidCommand {
                    @Execute
                    public CommandResult execute() {
                        return CommandResult.success();
                    }
                }
                """);

        assertTrue(result.success(), () -> String.join(System.lineSeparator(), result.messages()));
        Path serviceFile = result.outputDirectory()
                .resolve("META-INF")
                .resolve("services")
                .resolve("io.github.hanielcota.commandframework.generated.CommandDescriptor");
        assertTrue(Files.exists(serviceFile));
        String serviceContents = Files.readString(serviceFile);
        assertTrue(serviceContents.contains("Descriptor_example_ValidCommand"));
    }

    private CompilationResult compile(String fileName, String source) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "A JDK compiler is required to run processor tests");

        Path tempDir = Files.createTempDirectory("cf-processor-test");
        Path sourceFile = tempDir.resolve("example").resolve(fileName);
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, source);

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, null)) {
            fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(tempDir));
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(List.of(sourceFile.toFile()));
            List<String> options = List.of(
                    "-classpath", System.getProperty("java.class.path"),
                    "-proc:only"
            );
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, units);
            task.setProcessors(List.of(new CommandFrameworkProcessor()));
            boolean success = Boolean.TRUE.equals(task.call());
            List<String> messages = diagnostics.getDiagnostics().stream()
                    .map(diagnostic -> diagnostic.getMessage(Locale.ROOT))
                    .toList();
            return new CompilationResult(success, messages, tempDir);
        }
    }

    private record CompilationResult(boolean success, List<String> messages, Path outputDirectory) {
    }
}
