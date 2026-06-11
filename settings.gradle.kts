pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()

        // FOOLPROOF KOTLIN SYNTAX
        maven { setUrl("https://chaquo.com/maven") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // FOOLPROOF KOTLIN SYNTAX
        maven { setUrl("https://chaquo.com/maven") }
        maven { url = uri("https://jitpack.io") } // <--- ADĂUGAȚI ACEASTA
    }
}

rootProject.name = "HFPropagation"
include(":app")