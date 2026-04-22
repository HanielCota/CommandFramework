package io.github.hanielcota.commandframework.core.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

final class InputSanitizerTest {

    @Test
    void acceptsValidArguments() {
        InputSanitizer sanitizer = new InputSanitizer(5, 32);
        SanitizedInput result = sanitizer.sanitize(new String[]{"hello", "world"});
        assertTrue(result.isValid());
        assertEquals(List.of("hello", "world"), result.arguments());
    }

    @Test
    void rejectsTooManyArguments() {
        InputSanitizer sanitizer = new InputSanitizer(2, 32);
        SanitizedInput result = sanitizer.sanitize(new String[]{"a", "b", "c"});
        assertFalse(result.isValid());
    }

    @Test
    void rejectsArgumentTooLong() {
        InputSanitizer sanitizer = new InputSanitizer(5, 3);
        SanitizedInput result = sanitizer.sanitize(new String[]{"hello"});
        assertFalse(result.isValid());
    }

    @Test
    void rejectsNullElementInArray() {
        InputSanitizer sanitizer = new InputSanitizer(5, 32);
        SanitizedInput result = sanitizer.sanitize(Arrays.asList("hello", null, "world"));
        assertFalse(result.isValid());
    }

    @Test
    void stripsControlCharacters() {
        InputSanitizer sanitizer = new InputSanitizer(5, 32);
        SanitizedInput result = sanitizer.sanitize(new String[]{"hel\u0000lo"});
        assertTrue(result.isValid());
        assertEquals("hello", result.arguments().get(0));
    }

    @Test
    void acceptsEmptyArguments() {
        InputSanitizer sanitizer = new InputSanitizer(5, 32);
        SanitizedInput result = sanitizer.sanitize(new String[]{});
        assertTrue(result.isValid());
        assertTrue(result.arguments().isEmpty());
    }
}
