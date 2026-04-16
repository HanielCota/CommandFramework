package io.github.hanielcota.commandframework.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.plugins.JavaPluginExtension;
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
 *   <li>Adds the PaperMC Maven repository and JitPack.</li>
 *   <li>Sets the Java toolchain to 25.</li>
 *   <li>Adds the chosen CommandFramework module as an {@code implementation} dependency.</li>
 *   <li>Adds the CommandFramework annotation processor.</li>
 * </ul>
 */
public final class CommandFrameworkPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        CommandFrameworkExtension extension = project.getExtensions()
                .create("commandframework", CommandFrameworkExtension.class);

        this.registerRepositories(project);
        this.configureToolchain(project);

        project.afterEvaluate(evaluated -> this.wireDependencies(evaluated, extension));
    }

    private void registerRepositories(Project project) {
        project.getRepositories().mavenCentral();
        project.getRepositories().maven((MavenArtifactRepository repo) -> {
            repo.setName("PaperMC");
            repo.setUrl("https://repo.papermc.io/repository/maven-public/");
        });
        project.getRepositories().maven((MavenArtifactRepository repo) -> {
            repo.setName("JitPack");
            repo.setUrl("https://jitpack.io");
        });
    }

    private void configureToolchain(Project project) {
        JavaPluginExtension java = project.getExtensions().findByType(JavaPluginExtension.class);
        if (java != null) {
            java.getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(25));
        }
    }

    private void wireDependencies(Project project, CommandFrameworkExtension extension) {
        String platform = extension.getPlatform().getOrElse("paper");
        String version = extension.getVersion().getOrElse("0.1.0");
        String module = switch (platform.toLowerCase(java.util.Locale.ROOT)) {
            case "paper", "velocity", "core" -> platform.toLowerCase(java.util.Locale.ROOT);
            default -> throw new IllegalArgumentException(
                    "commandframework.platform must be one of: paper, velocity, core — got: " + platform);
        };
        String coordinate = "com.github.HanielCota.CommandFramework:" + module + ":" + version;
        String processorCoordinate = "com.github.HanielCota.CommandFramework:processor:" + version;

        project.getDependencies().add("implementation", coordinate);
        project.getDependencies().add("annotationProcessor", processorCoordinate);
    }
}
