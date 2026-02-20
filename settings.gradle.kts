pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven {
            url = uri("https://maven.pkg.github.com/compose-miuix-ui/miuix")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(System.getenv("GITHUB_ACTOR"))
                    .get()
                password = providers.gradleProperty("gpr.key")
                    .orElse(System.getenv("GITHUB_TOKEN"))
                    .get()
            }
        }
    }
}

rootProject.name = "ThemeStore"
include(":app")
 