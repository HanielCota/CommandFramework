plugins {
    `java-library`
    `maven-publish`
    id("io.freefair.lombok") version "8.10"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withJavadocJar()
    withSourcesJar()
}

group = "com.seuprojeto"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")

    api("com.mojang:brigadier:1.0.18")
    api("com.github.ben-manes.caffeine:caffeine:3.1.8")
    api("net.kyori:adventure-api:4.14.0")
    api("net.kyori:adventure-text-minimessage:4.14.0")
    api("net.kyori:adventure-platform-bukkit:4.3.2")
    api("io.github.classgraph:classgraph:4.8.179")

    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            
            pom {
                name.set("Command Framework")
                description.set("Command Framework completa para Paper/Purpur 1.21+ baseada em Brigadier")
                url.set("https://github.com/seuprojeto/CommandFramework")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("seuprojeto")
                        name.set("Seu Projeto")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/seuprojeto/CommandFramework.git")
                    developerConnection.set("scm:git:ssh://github.com:seuprojeto/CommandFramework.git")
                    url.set("https://github.com/seuprojeto/CommandFramework/tree/main")
                }
            }
        }
    }
}

 