package io.github.hanielcota.commandframework.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class VelocityCommandFrameworkTest {

    private final Object plugin = new PluginStub();
    @Mock
    private ProxyServer server;

    @Test
    @DisplayName("velocity() returns a non-null builder for valid arguments")
    void velocityReturnsBuilderForValidArguments() {
        VelocityCommandFramework framework = VelocityCommandFramework.velocity(this.server, this.plugin);

        assertNotNull(framework);
    }

    @Test
    @DisplayName("velocity() returns distinct instances for separate calls")
    void velocityReturnsDistinctInstances() {
        VelocityCommandFramework first = VelocityCommandFramework.velocity(this.server, this.plugin);
        VelocityCommandFramework second = VelocityCommandFramework.velocity(this.server, this.plugin);

        assertNotSame(first, second);
    }

    @Test
    @DisplayName("velocity() throws NullPointerException for a null server")
    void velocityThrowsNpeForNullServer() {
        assertThrows(NullPointerException.class, () -> VelocityCommandFramework.velocity(null, this.plugin));
    }

    @Test
    @DisplayName("velocity() throws NullPointerException for a null plugin")
    void velocityThrowsNpeForNullPlugin() {
        assertThrows(NullPointerException.class, () -> VelocityCommandFramework.velocity(this.server, null));
    }

    private static final class PluginStub {
    }
}
