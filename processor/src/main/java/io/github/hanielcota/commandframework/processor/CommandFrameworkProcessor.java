package io.github.hanielcota.commandframework.processor;

import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Confirm;
import io.github.hanielcota.commandframework.annotation.Cooldown;
import io.github.hanielcota.commandframework.annotation.Execute;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/**
 * Compile-time validator for CommandFramework annotations. Catches errors that would
 * otherwise only surface at runtime in {@code build()}.
 *
 * <p>Current checks:
 * <ul>
 *   <li>Duplicate {@code @Execute(sub = ...)} values within the same {@code @Command}
 *       class.</li>
 *   <li>{@code @Cooldown(value)} must be positive.</li>
 *   <li>{@code @Confirm(expireSeconds)} must be positive.</li>
 *   <li>{@code @Execute} on a method whose enclosing class lacks {@code @Command}.</li>
 * </ul>
 *
 * <p>Activated automatically via {@code META-INF/services/javax.annotation.processing.Processor}
 * when the processor jar is on the annotation processor path.
 */
@SupportedAnnotationTypes({
        "io.github.hanielcota.commandframework.annotation.Command",
        "io.github.hanielcota.commandframework.annotation.Execute",
        "io.github.hanielcota.commandframework.annotation.Cooldown",
        "io.github.hanielcota.commandframework.annotation.Confirm"
})
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public final class CommandFrameworkProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        this.checkExecuteInsideCommand(roundEnv);
        this.checkDuplicateSubcommands(roundEnv);
        this.checkCooldownValues(roundEnv);
        this.checkConfirmValues(roundEnv);
        return false; // do not claim the annotations — let other processors see them
    }

    private void checkExecuteInsideCommand(RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Execute.class)) {
            Element enclosing = element.getEnclosingElement();
            if (enclosing == null || enclosing.getAnnotation(Command.class) == null) {
                this.error(element, "@Execute method must live inside a class annotated with @Command.");
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
            String sub = method.getAnnotation(Execute.class).sub();
            Map<String, ExecutableElement> seen = perClass.computeIfAbsent(owner, ignored -> new HashMap<>());
            ExecutableElement previous = seen.put(sub, method);
            if (previous != null) {
                String label = sub.isEmpty() ? "<root>" : sub;
                this.error(method, "Duplicate @Execute(sub = \"" + label + "\") in "
                        + owner.getQualifiedName() + ". Already declared by "
                        + previous.getSimpleName() + "().");
            }
        }
        // ensure every @Command has at least one @Execute
        Set<TypeElement> emptyCommands = new LinkedHashSet<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(Command.class)) {
            if (!(element instanceof TypeElement type)) {
                continue;
            }
            if (!perClass.containsKey(type)) {
                emptyCommands.add(type);
            }
        }
        for (TypeElement empty : emptyCommands) {
            this.error(empty, "@Command class " + empty.getQualifiedName()
                    + " has no @Execute methods. Add at least one executor.");
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
            if (confirm.commandName().isBlank()) {
                this.error(element, "@Confirm(commandName) must not be blank.");
            }
        }
    }

    private void error(Element element, String message) {
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
