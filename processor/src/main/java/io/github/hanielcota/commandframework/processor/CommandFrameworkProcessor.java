package io.github.hanielcota.commandframework.processor;

import io.github.hanielcota.commandframework.annotation.Arg;
import io.github.hanielcota.commandframework.annotation.Async;
import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Confirm;
import io.github.hanielcota.commandframework.annotation.Cooldown;
import io.github.hanielcota.commandframework.annotation.Description;
import io.github.hanielcota.commandframework.annotation.Execute;
import io.github.hanielcota.commandframework.annotation.Inject;
import io.github.hanielcota.commandframework.annotation.Permission;
import io.github.hanielcota.commandframework.annotation.RequirePlayer;
import io.github.hanielcota.commandframework.annotation.Sender;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/**
 * Compile-time validator and metadata generator for CommandFramework annotations.
 */
@SupportedAnnotationTypes({
        "io.github.hanielcota.commandframework.annotation.Command",
        "io.github.hanielcota.commandframework.annotation.Execute",
        "io.github.hanielcota.commandframework.annotation.Cooldown",
        "io.github.hanielcota.commandframework.annotation.Confirm",
        "io.github.hanielcota.commandframework.annotation.Inject"
})
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public final class CommandFrameworkProcessor extends AbstractProcessor {

    private static final Pattern VALID_LABEL = Pattern.compile("[a-zA-Z0-9_-]+");
    private static final String GENERATED_PACKAGE = "io.github.hanielcota.commandframework.generated";
    private static final String DESCRIPTOR_INTERFACE = GENERATED_PACKAGE + ".CommandDescriptor";
    /** Mirrors {@link io.github.hanielcota.commandframework.annotation.Arg#maxLength()}. */
    private static final int DEFAULT_ARG_MAX_LENGTH = 256;

    private final Set<String> generatedDescriptors = new LinkedHashSet<>();
    private final Set<String> processedCommands = new LinkedHashSet<>();
    private Types types;
    private Elements elements;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.types = processingEnv.getTypeUtils();
        this.elements = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        this.checkExecuteInsideCommand(roundEnv);
        this.checkCommandLabels(roundEnv);
        this.checkDuplicateSubcommands(roundEnv);
        this.checkExecutorSignatures(roundEnv);
        this.checkCooldownValues(roundEnv);
        this.checkConfirmValues(roundEnv);
        this.checkInjectFields(roundEnv);

        for (Element element : roundEnv.getElementsAnnotatedWith(Command.class)) {
            if (element instanceof TypeElement type) {
                this.generateDescriptor(type);
            }
        }

        if (roundEnv.processingOver()) {
            this.writeServiceFile();
        }
        return false;
    }

    private void checkExecuteInsideCommand(RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Execute.class)) {
            Element enclosing = element.getEnclosingElement();
            if (enclosing == null || enclosing.getAnnotation(Command.class) == null) {
                this.error(element, "@Execute method must live inside a class annotated with @Command.");
            }
        }
    }

    private void checkCommandLabels(RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Command.class)) {
            if (!(element instanceof TypeElement type)) {
                continue;
            }
            Command command = type.getAnnotation(Command.class);
            this.validateLabel(type, command.name().trim(), "@Command(name)");
            for (String alias : command.aliases()) {
                this.validateLabel(type, alias.trim(), "@Command(alias)");
            }
        }
    }

    private void checkDuplicateSubcommands(RoundEnvironment roundEnv) {
        Map<TypeElement, Map<String, ExecutableElement>> perClass = new HashMap<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(Execute.class)) {
            if (!(element instanceof ExecutableElement method)) {
                continue;
            }
            Element enclosing = method.getEnclosingElement();
            if (!(enclosing instanceof TypeElement owner)) {
                continue;
            }
            String rawSub = method.getAnnotation(Execute.class).sub();
            String sub = this.normalize(rawSub);
            if (sub.contains(" ")) {
                this.error(method, "@Execute(sub = \"" + rawSub + "\") must be a single token "
                        + "(no internal whitespace). Got " + sub.split(" ").length + " tokens. "
                        + "Fix options: (a) use a single-word name like "
                        + "\"" + sub.replace(' ', '_') + "\" or \"" + sub.replace(" ", "") + "\"; "
                        + "(b) split into two methods with their own @Execute(sub = \"...\"). "
                        + "Nested multi-word subcommands are not supported — each @Execute maps to one token.");
            }
            Map<String, ExecutableElement> seen = perClass.computeIfAbsent(owner, ignored -> new HashMap<>());
            ExecutableElement previous = seen.put(sub, method);
            if (previous != null) {
                String label = sub.isEmpty() ? "<root>" : sub;
                this.error(method, "Duplicate @Execute(sub = \"" + label + "\") in "
                        + owner.getQualifiedName() + ". Already declared by "
                        + previous.getSimpleName() + "().");
            }
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(Command.class)) {
            if (element instanceof TypeElement type && !perClass.containsKey(type)) {
                this.error(type, "@Command class " + type.getQualifiedName()
                        + " has no @Execute methods. Add at least one executor.");
            }
        }
    }

    private void checkExecutorSignatures(RoundEnvironment roundEnv) {
        TypeElement commandResultType = this.elements.getTypeElement("io.github.hanielcota.commandframework.CommandResult");
        String commandActorName = "io.github.hanielcota.commandframework.CommandActor";

        for (Element element : roundEnv.getElementsAnnotatedWith(Execute.class)) {
            if (!(element instanceof ExecutableElement method)) {
                continue;
            }
            TypeMirror returnType = method.getReturnType();
            if (returnType.getKind() != TypeKind.VOID
                    && (commandResultType == null
                    || !this.types.isAssignable(this.types.erasure(returnType), this.types.erasure(commandResultType.asType())))) {
                this.error(method, "@Execute methods must return void or CommandResult.");
            }

            List<? extends VariableElement> parameters = method.getParameters();
            for (int index = 0; index < parameters.size(); index++) {
                this.validateExecuteParameter(method, parameters, index, commandActorName);
            }
        }
    }

    private void validateExecuteParameter(
            ExecutableElement method,
            List<? extends VariableElement> parameters,
            int index,
            String commandActorName
    ) {
        VariableElement parameter = parameters.get(index);
        this.validateArg(parameter, index, parameters.size());
        this.validateOptional(parameter);
        this.validateAsyncSender(method, parameter, commandActorName);
    }

    private void validateArg(VariableElement parameter, int index, int parameterCount) {
        Arg arg = parameter.getAnnotation(Arg.class);
        if (arg == null) {
            return;
        }
        if (arg.maxLength() <= 0) {
            this.error(parameter, "@Arg(maxLength) must be > 0.");
        }
        if (!arg.greedy()) {
            return;
        }
        if (index != parameterCount - 1) {
            this.error(parameter, "@Arg(greedy = true) must be the last parameter.");
        }
        if (!"java.lang.String".equals(this.resolvedTypeName(parameter))) {
            this.error(parameter, "@Arg(greedy = true) only supports String parameters.");
        }
    }

    private void validateOptional(VariableElement parameter) {
        io.github.hanielcota.commandframework.annotation.Optional optional =
                parameter.getAnnotation(io.github.hanielcota.commandframework.annotation.Optional.class);
        if (optional != null && parameter.asType().getKind().isPrimitive()
                && io.github.hanielcota.commandframework.annotation.Optional.UNSET.equals(optional.value())) {
            this.error(parameter, "@Optional on primitive parameters requires a default value.");
        }
    }

    private void validateAsyncSender(ExecutableElement method, VariableElement parameter, String commandActorName) {
        if (method.getAnnotation(Async.class) != null
                && parameter.getAnnotation(Sender.class) != null
                && !commandActorName.equals(this.erasedTypeName(parameter.asType()))) {
            this.error(parameter, "@Async methods must declare @Sender CommandActor "
                    + "(got @Sender " + this.erasedTypeName(parameter.asType()) + "). "
                    + "Platform sender types (Player, CommandSender, CommandSource, ProxiedPlayer) "
                    + "are not thread-safe off the main thread. "
                    + "Fix: change the parameter to @Sender CommandActor and, inside the method, "
                    + "re-resolve the platform sender on-demand — e.g. "
                    + "Bukkit.getPlayer(java.util.UUID.fromString(actor.id())) on Paper, or "
                    + "proxyServer.getPlayer(java.util.UUID.fromString(actor.id())) on Velocity. "
                    + "Remember the lookup may return null if the player disconnected.");
        }
    }

    private void checkCooldownValues(RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Cooldown.class)) {
            Cooldown cooldown = element.getAnnotation(Cooldown.class);
            if (cooldown.value() <= 0) {
                this.error(element, "@Cooldown value must be > 0 (got " + cooldown.value() + ").");
            }
        }
    }

    private void checkConfirmValues(RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Confirm.class)) {
            Confirm confirm = element.getAnnotation(Confirm.class);
            if (confirm.expireSeconds() <= 0) {
                this.error(element, "@Confirm(expireSeconds) must be > 0 (got "
                        + confirm.expireSeconds() + ").");
            }
            String commandName = confirm.commandName().trim();
            if (commandName.isEmpty()) {
                this.error(element, "@Confirm(commandName) must not be blank.");
            } else if (!VALID_LABEL.matcher(commandName).matches()) {
                this.error(element, "@Confirm(commandName) must use only letters, numbers, hyphens, or underscores.");
            }
        }
    }

    private void checkInjectFields(RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Inject.class)) {
            if (element.getKind() != ElementKind.FIELD) {
                this.error(element, "@Inject is only supported on fields.");
                continue;
            }
            if (element.getModifiers().contains(Modifier.FINAL)) {
                this.error(element, "@Inject fields must not be final.");
            }
        }
    }

    private void generateDescriptor(TypeElement type) {
        String descriptorName = this.descriptorName(type);
        String qualifiedDescriptorName = GENERATED_PACKAGE + "." + descriptorName;
        if (!this.processedCommands.add(qualifiedDescriptorName)) {
            return;
        }

        String source = this.descriptorSource(type, descriptorName);
        try {
            JavaFileObject file = this.processingEnv.getFiler().createSourceFile(qualifiedDescriptorName, type);
            try (Writer writer = file.openWriter()) {
                writer.write(source);
            }
            this.generatedDescriptors.add(qualifiedDescriptorName);
        } catch (IOException exception) {
            this.error(type, "Failed to generate command descriptor: " + exception.getMessage());
        }
    }

    private String descriptorSource(TypeElement type, String descriptorName) {
        Command command = type.getAnnotation(Command.class);
        String packageName = this.elements.getPackageOf(type).getQualifiedName().toString();
        String commandClassName = this.binaryName(type);

        return """
                package %s;

                import java.util.List;

                public final class %s implements CommandDescriptor {
                    private static final CommandDescriptor.Command COMMAND = %s;

                    @Override
                    public String packageName() {
                        return %s;
                    }

                    @Override
                    public String commandClassName() {
                        return %s;
                    }

                    @Override
                    public Class<?> commandType() {
                        try {
                            return Class.forName(%s, false, this.getClass().getClassLoader());
                        } catch (ClassNotFoundException exception) {
                            throw new IllegalStateException("Unable to load command class " + %s, exception);
                        }
                    }

                    @Override
                    public CommandDescriptor.Command command() {
                        return COMMAND;
                    }
                }
                """.formatted(
                GENERATED_PACKAGE,
                descriptorName,
                this.commandLiteral(type, command),
                this.stringLiteral(packageName),
                this.stringLiteral(commandClassName),
                this.stringLiteral(commandClassName),
                this.stringLiteral(commandClassName)
        );
    }

    private String commandLiteral(TypeElement type, Command command) {
        List<VariableElement> injectedFields = this.collectInjectFields(type);
        List<ExecutableElement> executors = ElementFilter.methodsIn(type.getEnclosedElements()).stream()
                .filter(method -> method.getAnnotation(Execute.class) != null)
                .toList();

        return "new CommandDescriptor.Command("
                + this.stringLiteral(command.name().trim()) + ", "
                + this.stringListLiteral(List.of(command.aliases()).stream().map(String::trim).toList()) + ", "
                + this.stringLiteral(command.description()) + ", "
                + this.fieldListLiteral(injectedFields) + ", "
                + this.executorListLiteral(type, command, executors)
                + ")";
    }

    private List<VariableElement> collectInjectFields(TypeElement type) {
        LinkedHashSet<VariableElement> fields = new LinkedHashSet<>();
        TypeElement current = type;
        while (current != null && !"java.lang.Object".contentEquals(current.getQualifiedName())) {
            for (VariableElement field : ElementFilter.fieldsIn(current.getEnclosedElements())) {
                if (field.getAnnotation(Inject.class) != null) {
                    fields.add(field);
                }
            }
            TypeMirror superType = current.getSuperclass();
            if (superType.getKind() == TypeKind.NONE) {
                break;
            }
            Element superElement = this.types.asElement(superType);
            current = superElement instanceof TypeElement superTypeElement ? superTypeElement : null;
        }
        return List.copyOf(fields);
    }

    private String fieldListLiteral(List<VariableElement> fields) {
        if (fields.isEmpty()) {
            return "List.of()";
        }
            return "List.of(" + fields.stream()
                .map(field -> "new CommandDescriptor.Field("
                        + this.stringLiteral(this.binaryName((TypeElement) field.getEnclosingElement())) + ", "
                        + this.stringLiteral(field.getSimpleName().toString()) + ", "
                        + this.stringLiteral(this.erasedTypeName(field.asType()))
                        + ")")
                .collect(Collectors.joining(", ")) + ")";
    }

    private String executorListLiteral(TypeElement owner, Command command, List<ExecutableElement> executors) {
        if (executors.isEmpty()) {
            return "List.of()";
        }
        String classPermission = this.classPermission(owner, command);
        boolean classRequirePlayer = owner.getAnnotation(RequirePlayer.class) != null;
        return "List.of(" + executors.stream()
                .map(method -> this.executorLiteral(owner, method, command.description(), classPermission, classRequirePlayer))
                .collect(Collectors.joining(", ")) + ")";
    }

    private String executorLiteral(
            TypeElement owner,
            ExecutableElement method,
            String defaultDescription,
            String classPermission,
            boolean classRequirePlayer
    ) {
        Execute execute = method.getAnnotation(Execute.class);
        Description description = method.getAnnotation(Description.class);
        Permission permission = method.getAnnotation(Permission.class);
        Cooldown cooldown = method.getAnnotation(Cooldown.class);
        Confirm confirm = method.getAnnotation(Confirm.class);

        String descriptionValue = description != null ? description.value() : defaultDescription;
        String permissionValue = permission != null ? permission.value() : classPermission;
        boolean requirePlayer = classRequirePlayer || method.getAnnotation(RequirePlayer.class) != null;

        return "new CommandDescriptor.Executor("
                + this.stringLiteral(this.binaryName(owner)) + ", "
                + this.stringLiteral(method.getSimpleName().toString()) + ", "
                + this.stringLiteral(this.normalize(execute.sub())) + ", "
                + this.stringLiteral(descriptionValue) + ", "
                + this.stringLiteral(permissionValue) + ", "
                + requirePlayer + ", "
                + (method.getAnnotation(Async.class) != null) + ", "
                + this.cooldownLiteral(cooldown) + ", "
                + this.confirmLiteral(confirm) + ", "
                + this.parameterListLiteral(method.getParameters())
                + ")";
    }

    private String parameterListLiteral(List<? extends VariableElement> parameters) {
        if (parameters.isEmpty()) {
            return "List.of()";
        }
        return "List.of(" + parameters.stream()
                .map(this::parameterLiteral)
                .collect(Collectors.joining(", ")) + ")";
    }

    private String parameterLiteral(VariableElement parameter) {
        Arg arg = parameter.getAnnotation(Arg.class);
        io.github.hanielcota.commandframework.annotation.Optional optional =
                parameter.getAnnotation(io.github.hanielcota.commandframework.annotation.Optional.class);
        boolean javaOptional = "java.util.Optional".equals(this.erasedTypeName(parameter.asType()));
        String resolvedTypeName = this.resolvedTypeName(parameter);
        String name = arg != null && !arg.value().isBlank()
                ? arg.value()
                : parameter.getSimpleName().toString();
        boolean greedy = arg != null && arg.greedy();
        int maxLength = arg != null ? arg.maxLength() : DEFAULT_ARG_MAX_LENGTH;
        String optionalValue = optional != null
                ? optional.value()
                : io.github.hanielcota.commandframework.annotation.Optional.UNSET;

        return "new CommandDescriptor.Parameter("
                + this.stringLiteral(name) + ", "
                + this.stringLiteral(this.erasedTypeName(parameter.asType())) + ", "
                + this.stringLiteral(resolvedTypeName) + ", "
                + (parameter.getAnnotation(Sender.class) != null) + ", "
                + javaOptional + ", "
                + (optional != null || javaOptional) + ", "
                + this.stringLiteral(optionalValue) + ", "
                + greedy + ", "
                + maxLength
                + ")";
    }

    private String cooldownLiteral(Cooldown cooldown) {
        if (cooldown == null) {
            return "null";
        }
        return "new CommandDescriptor.Cooldown("
                + cooldown.value() + "L, "
                + this.stringLiteral(cooldown.unit().name()) + ", "
                + this.stringLiteral(cooldown.bypassPermission())
                + ")";
    }

    private String confirmLiteral(Confirm confirm) {
        if (confirm == null) {
            return "null";
        }
        return "new CommandDescriptor.Confirm("
                + confirm.expireSeconds() + "L, "
                + this.stringLiteral(confirm.commandName().trim())
                + ")";
    }

    private String classPermission(TypeElement type, Command command) {
        Permission permission = type.getAnnotation(Permission.class);
        return permission != null ? permission.value() : command.permission();
    }

    private String resolvedTypeName(VariableElement parameter) {
        if (!"java.util.Optional".equals(this.erasedTypeName(parameter.asType()))) {
            return this.erasedTypeName(parameter.asType());
        }
        if (parameter.asType() instanceof DeclaredType declaredType && !declaredType.getTypeArguments().isEmpty()) {
            return this.erasedTypeName(declaredType.getTypeArguments().getFirst());
        }
        return "java.util.Optional";
    }

    private String erasedTypeName(TypeMirror typeMirror) {
        if (typeMirror.getKind().isPrimitive()) {
            return typeMirror.toString();
        }
        TypeMirror erased = this.types.erasure(typeMirror);
        Element element = this.types.asElement(erased);
        if (element instanceof TypeElement type) {
            return this.binaryName(type);
        }
        return erased.toString();
    }

    private String descriptorName(TypeElement type) {
        return "Descriptor_" + type.getQualifiedName().toString().replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private String binaryName(TypeElement type) {
        return this.elements.getBinaryName(type).toString();
    }

    private void writeServiceFile() {
        if (this.generatedDescriptors.isEmpty()) {
            return;
        }
        try {
            FileObject file = this.processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT,
                    "",
                    "META-INF/services/" + DESCRIPTOR_INTERFACE
            );
            try (Writer writer = file.openWriter()) {
                for (String descriptor : this.generatedDescriptors) {
                    writer.write(descriptor);
                    writer.write(System.lineSeparator());
                }
            }
        } catch (IOException exception) {
            this.processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Failed to write command descriptor service file: " + exception.getMessage()
            );
        }
    }

    private void validateLabel(Element element, String label, String usage) {
        if (label.isEmpty()) {
            this.error(element, usage + " must not be blank.");
            return;
        }
        if (!VALID_LABEL.matcher(label).matches()) {
            this.error(element, usage + " must use only letters, numbers, hyphens, or underscores.");
        }
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String stringListLiteral(List<String> values) {
        if (values.isEmpty()) {
            return "List.of()";
        }
        return "List.of(" + values.stream().map(this::stringLiteral).collect(Collectors.joining(", ")) + ")";
    }

    private String stringLiteral(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                + "\"";
    }

    private void error(Element element, String message) {
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
