package io.github.hanielcota.commandframework.generated;

import java.util.List;
import java.util.Objects;

/**
 * Generated compile-time descriptor for a single {@code @Command} class.
 */
public interface CommandDescriptor {

    String packageName();

    String commandClassName();

    Class<?> commandType();

    Command command();

    record Command(
            String name,
            List<String> aliases,
            String description,
            List<Field> injectedFields,
            List<Executor> executors
    ) {
        public Command {
            Objects.requireNonNull(name, "name");
            aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
            Objects.requireNonNull(description, "description");
            injectedFields = List.copyOf(Objects.requireNonNull(injectedFields, "injectedFields"));
            executors = List.copyOf(Objects.requireNonNull(executors, "executors"));
        }
    }

    record Field(String declaringClassName, String fieldName, String fieldTypeName) {
        public Field {
            Objects.requireNonNull(declaringClassName, "declaringClassName");
            Objects.requireNonNull(fieldName, "fieldName");
            Objects.requireNonNull(fieldTypeName, "fieldTypeName");
        }
    }

    record Executor(
            String declaringClassName,
            String methodName,
            String subcommand,
            String description,
            String permission,
            boolean requirePlayer,
            boolean async,
            Cooldown cooldown,
            Confirm confirm,
            List<Parameter> parameters
    ) {
        public Executor {
            Objects.requireNonNull(declaringClassName, "declaringClassName");
            Objects.requireNonNull(methodName, "methodName");
            Objects.requireNonNull(subcommand, "subcommand");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(permission, "permission");
            parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
        }
    }

    record Parameter(
            String name,
            String rawTypeName,
            String resolvedTypeName,
            boolean sender,
            boolean javaOptional,
            boolean optional,
            String optionalValue,
            boolean greedy,
            int maxLength
    ) {
        public Parameter {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(rawTypeName, "rawTypeName");
            Objects.requireNonNull(resolvedTypeName, "resolvedTypeName");
            Objects.requireNonNull(optionalValue, "optionalValue");
        }
    }

    record Cooldown(long value, String unitName, String bypassPermission) {
        public Cooldown {
            Objects.requireNonNull(unitName, "unitName");
            Objects.requireNonNull(bypassPermission, "bypassPermission");
        }
    }

    record Confirm(long expireSeconds, String commandName) {
        public Confirm {
            Objects.requireNonNull(commandName, "commandName");
        }
    }
}
