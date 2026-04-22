package io.github.hanielcota.commandframework.core.argument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.hanielcota.commandframework.core.ActorKind;
import io.github.hanielcota.commandframework.core.ArgumentInput;
import io.github.hanielcota.commandframework.core.CommandActor;
import io.github.hanielcota.commandframework.core.CommandRoute;
import io.github.hanielcota.commandframework.core.EnumArgumentResolver;
import io.github.hanielcota.commandframework.core.ParseResult;
import io.github.hanielcota.commandframework.core.SuggestionContext;
import io.github.hanielcota.commandframework.core.TestActor;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ArgumentResolverTest {

    @Test
    void booleanResolverParsesTrue() {
        BooleanArgumentResolver resolver = new BooleanArgumentResolver(Boolean.class);
        ParseResult<Boolean> result = resolver.parse(new ArgumentInput("true", "value"));
        assertTrue(result.isSuccess());
        assertEquals(Boolean.TRUE, result.value());
    }

    @Test
    void booleanResolverParsesFalse() {
        BooleanArgumentResolver resolver = new BooleanArgumentResolver(Boolean.class);
        ParseResult<Boolean> result = resolver.parse(new ArgumentInput("false", "value"));
        assertTrue(result.isSuccess());
        assertEquals(Boolean.FALSE, result.value());
    }

    @Test
    void booleanResolverRejectsInvalid() {
        BooleanArgumentResolver resolver = new BooleanArgumentResolver(Boolean.class);
        ParseResult<Boolean> result = resolver.parse(new ArgumentInput("maybe", "value"));
        assertFalse(result.isSuccess());
    }

    @Test
    void booleanResolverSuggestsValues() {
        BooleanArgumentResolver resolver = new BooleanArgumentResolver(Boolean.class);
        CommandActor actor = new TestActor(ActorKind.PLAYER);
        CommandRoute route = CommandRoute.builder("cmd", (ctx, params) -> null).build();
        assertEquals(List.of("true", "false"), resolver.suggest(new SuggestionContext(actor, route, "", List.of())));
    }

    @Test
    void stringResolverParsesAnyValue() {
        StringArgumentResolver resolver = new StringArgumentResolver();
        ParseResult<String> result = resolver.parse(new ArgumentInput("anything", "value"));
        assertTrue(result.isSuccess());
        assertEquals("anything", result.value());
    }

    @Test
    void enumResolverParsesValidConstant() {
        EnumArgumentResolver<SampleUnit> resolver = new EnumArgumentResolver<>(SampleUnit.class);
        ParseResult<SampleUnit> result = resolver.parse(new ArgumentInput("seconds", "value"));
        assertTrue(result.isSuccess());
        assertEquals(SampleUnit.SECONDS, result.value());
    }

    @Test
    void enumResolverRejectsInvalidConstant() {
        EnumArgumentResolver<SampleUnit> resolver = new EnumArgumentResolver<>(SampleUnit.class);
        ParseResult<SampleUnit> result = resolver.parse(new ArgumentInput("years", "value"));
        assertFalse(result.isSuccess());
    }

    @Test
    void enumResolverRequiresEnumType() {
        assertThrows(IllegalArgumentException.class, () -> new EnumArgumentResolver<>(String.class));
    }

    private enum SampleUnit {
        SECONDS, MINUTES, HOURS
    }
}
