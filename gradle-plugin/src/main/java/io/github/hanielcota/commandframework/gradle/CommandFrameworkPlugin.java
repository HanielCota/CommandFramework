package io.github.hanielcota.commandframework.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

/**
 * Meta-plugin that applies the baseline configuration needed to use CommandFramework
 * with minimal boilerplate.
 *
 * <p>Apply with:
 * <pre>{@code
 * plugins {
 *     java
 *     id("io.github.hanielcota.commandframework") version "0.1.0"
 * }
 *
 * commandframework {
 *     platform.set("paper")    // or "velocity"
 *     version.set("0.1.0")     // CommandFramework version
 * }
 * }</pre>
 *
 * <p>The plugin then:
 * <ul>
 *   <li>Sets the Java toolchain to 25.</li>
 *   <li>Adds the chosen CommandFramework module as an {@code implementation} dependency.</li>
 *   <li>Adds the CommandFramework annotation processor.</li>
 * </ul>
 */
public final class CommandFrameworkPlugin implements Plugin<Project> {

    private static final int JAVA_TOOLCHAIN_VERSION = 25;

    @Override
    public void apply(Project project) {
        CommandFrameworkExtension extension = project.getExtensions()
                .create("commandframework", CommandFrameworkExtension.class);

        project.getPlugins().withType(JavaPlugin.class, ignored -> {
            this.configureToolchain(project);
            this.wireDependencies(project, extension);
        });
    }

    private void configureToolchain(Project project) {
        JavaPluginExtension java = project.getExtensions().findByType(JavaPluginExtension.class);
        if (java != null) {
            java.getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(JAVA_TOOLCHAIN_VERSION));
        }
    }

    private void wireDependencies(Project project, CommandFrameworkExtension extension) {
        Provider<String> module = extension.getPlatform()
                .orElse("paper")
                .map(this::normalizePlatform);
        Provider<String> version = extension.getVersion().orElse("0.1.0");
        Provider<String> coordinate = module.zip(version,
                (platform, resolvedVersion) -> "io.github.hanielcota.commandframework:"
                        + platform + ":" + resolvedVersion);
        Provider<String> processorCoordinate = version.map(resolvedVersion ->
                "io.github.hanielcota.commandframework:processor:" + resolvedVersion);

        project.getDependencies().addProvider(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, coordinate);
        project.getDependencies().addProvider(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME, processorCoordinate);
    }

    private String normalizePlatform(String platform) {
        String normalized = platform.toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "paper", "velocity", "core" -> normalized;
            default -> throw new IllegalArgumentException(
                    "commandframework.platform must be one of: paper, velocity, core - got: " + platform);
        };
    }
}
