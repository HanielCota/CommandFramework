package io.github.hanielcota.commandframework.core.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class SafeLogTextTest {

    @Test
    void cleansControlCharacters() {
        SafeLogText safe = new SafeLogText();
        assertEquals("hello?world", safe.clean("hello\nworld"));
    }

    @Test
    void preservesNormalText() {
        SafeLogText safe = new SafeLogText();
        assertEquals("hello world", safe.clean("hello world"));
    }

    @Test
    void handlesEmptyString() {
        SafeLogText safe = new SafeLogText();
        assertEquals("", safe.clean(""));
    }

    @Test
    void replacesMultipleControls() {
        SafeLogText safe = new SafeLogText();
        assertEquals("???", safe.clean("\u0000\u0001\u0002"));
    }
}
