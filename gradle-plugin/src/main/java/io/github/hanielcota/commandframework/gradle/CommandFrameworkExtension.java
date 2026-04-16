package io.github.hanielcota.commandframework.gradle;

import org.gradle.api.provider.Property;

/**
 * DSL backing {@code commandframework { platform = ...; version = ... }}.
 */
public interface CommandFrameworkExtension {
    /**
     * Target platform: {@code paper}, {@code velocity}, or {@code core}.
     *
     * @return the platform property
     */
    Property<String> getPlatform();

    /**
     * CommandFramework version (e.g. {@code 0.1.0}).
     *
     * @return the version property
     */
    Property<String> getVersion();
}
