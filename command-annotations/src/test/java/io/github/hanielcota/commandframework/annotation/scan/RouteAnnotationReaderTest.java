package io.github.hanielcota.commandframework.annotation.scan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hanielcota.commandframework.annotation.Command;
import io.github.hanielcota.commandframework.annotation.Cooldown;
import io.github.hanielcota.commandframework.annotation.Default;
import io.github.hanielcota.commandframework.annotation.Permission;
import io.github.hanielcota.commandframework.annotation.Subcommand;
import io.github.hanielcota.commandframework.core.CommandRoute;
import io.github.hanielcota.commandframework.core.RouteConfigurationException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class RouteAnnotationReaderTest {

    private final RouteAnnotationReader reader = new RouteAnnotationReader();

    @Test
    void readsDefaultRoute() {
        RouteAnnotationModel model = reader.read(GoodCommand.class, GoodCommand.class.getAnnotation(Command.class), defaultMethod());
        assertEquals(List.of(), model.path());
        assertEquals("kit", model.root());
    }

    @Test
    void readsSubcommandPath() {
        RouteAnnotationModel model = reader.read(GoodCommand.class, GoodCommand.class.getAnnotation(Command.class), subcommandMethod());
        assertEquals(List.of("give"), model.path());
    }

    @Test
    void inheritsPermissionFromClass() {
        RouteAnnotationModel model = reader.read(PermissionClass.class, PermissionClass.class.getAnnotation(Command.class), defaultMethod(PermissionClass.class));
        assertEquals("kit.admin", model.permission());
    }

    @Test
    void methodPermissionOverridesClass() {
        RouteAnnotationModel model = reader.read(PermissionClass.class, PermissionClass.class.getAnnotation(Command.class), subcommandMethod(PermissionClass.class));
        assertEquals("kit.give", model.permission());
    }

    @Test
    void readsCooldown() {
        RouteAnnotationModel model = reader.read(CooldownCommand.class, CooldownCommand.class.getAnnotation(Command.class), subcommandMethod(CooldownCommand.class));
        assertTrue(model.cooldown().getSeconds() > 0);
    }

    @Test
    void rejectsBothAnnotations() {
        RouteConfigurationException exception = assertThrows(RouteConfigurationException.class, () ->
                reader.read(BadCommand.class, BadCommand.class.getAnnotation(Command.class), bothAnnotationsMethod()));
        assertTrue(exception.getMessage().contains("expected exactly one"));
    }

    @Test
    void rejectsBlankSubcommand() {
        RouteConfigurationException exception = assertThrows(RouteConfigurationException.class, () ->
                reader.read(BadCommand.class, BadCommand.class.getAnnotation(Command.class), blankSubcommandMethod()));
        assertTrue(exception.getMessage().contains("non-empty path"));
    }

    // Reflection helpers
    private java.lang.reflect.Method defaultMethod() {
        return defaultMethod(GoodCommand.class);
    }

    private java.lang.reflect.Method defaultMethod(Class<?> type) {
        for (java.lang.reflect.Method m : type.getDeclaredMethods()) {
            if (m.isAnnotationPresent(Default.class)) return m;
        }
        throw new AssertionError("No @Default method");
    }

    private java.lang.reflect.Method subcommandMethod() {
        return subcommandMethod(GoodCommand.class);
    }

    private java.lang.reflect.Method subcommandMethod(Class<?> type) {
        for (java.lang.reflect.Method m : type.getDeclaredMethods()) {
            if (m.isAnnotationPresent(Subcommand.class)) return m;
        }
        throw new AssertionError("No @Subcommand method");
    }

    private java.lang.reflect.Method bothAnnotationsMethod() {
        for (java.lang.reflect.Method m : BadCommand.class.getDeclaredMethods()) {
            if (m.getName().equals("both")) return m;
        }
        throw new AssertionError("No both method");
    }

    private java.lang.reflect.Method blankSubcommandMethod() {
        for (java.lang.reflect.Method m : BadCommand.class.getDeclaredMethods()) {
            if (m.getName().equals("blank")) return m;
        }
        throw new AssertionError("No blank method");
    }

    @Command("kit")
    private static final class GoodCommand {
        @Default
        void onDefault() {}

        @Subcommand("give")
        void onGive() {}
    }

    @Command("kit")
    @Permission("kit.admin")
    private static final class PermissionClass {
        @Default
        void onDefault() {}

        @Subcommand("give")
        @Permission("kit.give")
        void onGive() {}
    }

    @Command("kit")
    private static final class CooldownCommand {
        @Subcommand("give")
        @Cooldown(value = 5, unit = TimeUnit.SECONDS)
        void onGive() {}
    }

    @Command("bad")
    private static final class BadCommand {
        @Default
        @Subcommand("oops")
        void both() {}

        @Subcommand("   ")
        void blank() {}
    }
}
