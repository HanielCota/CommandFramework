package io.github.hanielcota.commandframework.paper;

import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PaperCommandFrameworkTest {

    @Mock
    private JavaPlugin plugin;

    @Mock
    private Server server;

    @Mock
    private Logger logger;

    @Test
    @DisplayName("paper() returns a non-null builder for a valid plugin")
    void paperReturnsBuilderForValidPlugin() {
        lenient().when(this.plugin.getServer()).thenReturn(this.server);
        lenient().when(this.plugin.getLogger()).thenReturn(this.logger);

        PaperCommandFramework framework = PaperCommandFramework.paper(this.plugin);

        assertNotNull(framework);
    }

    @Test
    @DisplayName("paper() returns distinct instances for separate calls")
    void paperReturnsDistinctInstances() {
        lenient().when(this.plugin.getServer()).thenReturn(this.server);
        lenient().when(this.plugin.getLogger()).thenReturn(this.logger);

        PaperCommandFramework first = PaperCommandFramework.paper(this.plugin);
        PaperCommandFramework second = PaperCommandFramework.paper(this.plugin);

        assertNotSame(first, second);
    }

    @Test
    @DisplayName("paper() throws NullPointerException for a null plugin")
    void paperThrowsNpeForNullPlugin() {
        assertThrows(NullPointerException.class, () -> PaperCommandFramework.paper(null));
    }
}
