plugins {
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("commandframework") {
            id = "io.github.hanielcota.commandframework"
            implementationClass = "io.github.hanielcota.commandframework.gradle.CommandFrameworkPlugin"
            displayName = "CommandFramework Gradle plugin"
            description = "Wires Paper/Velocity repositories, Java 25 toolchain, and the CommandFramework dependency."
        }
    }
}
