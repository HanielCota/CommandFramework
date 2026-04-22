package io.github.hanielcota.commandframework.velocity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

final class VelocityRawCommandBridgeTest {

    @Test
    void tokenizesBlankRawArgumentsAsEmptyList() {
        assertEquals(List.of(), VelocityRawCommandBridge.tokenize("   "));
    }

    @Test
    void trimsAndCollapsesWhitespaceWhenTokenizingRawArguments() {
        assertEquals(List.of("give", "Steve", "daily"), VelocityRawCommandBridge.tokenize("  give   Steve daily  "));
    }
}
