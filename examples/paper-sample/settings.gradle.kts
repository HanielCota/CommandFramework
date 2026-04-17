pluginManagement {
    includeBuild("../../gradle-plugin")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

includeBuild("../..") {
    dependencySubstitution {
        substitute(module("io.github.hanielcota.commandframework:annotations")).using(project(":annotations"))
        substitute(module("io.github.hanielcota.commandframework:core")).using(project(":core"))
        substitute(module("io.github.hanielcota.commandframework:paper")).using(project(":paper"))
        substitute(module("io.github.hanielcota.commandframework:processor")).using(project(":processor"))
    }
}

rootProject.name = "paper-sample"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}
