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
        substitute(module("com.github.HanielCota.CommandFramework:annotations")).using(project(":annotations"))
        substitute(module("com.github.HanielCota.CommandFramework:core")).using(project(":core"))
        substitute(module("com.github.HanielCota.CommandFramework:paper")).using(project(":paper"))
        substitute(module("com.github.HanielCota.CommandFramework:processor")).using(project(":processor"))
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
