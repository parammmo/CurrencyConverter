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
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    // War FAIL_ON_PROJECT_REPOS. Der Kotlin/Wasm-Plugin hängt fürs Herunterladen
    // von Node.js/Binaryen eigene Repositories ans Root-Projekt — mit FAIL
    // bricht der :web-Build deshalb ab. PREFER_PROJECT lässt das zu; :app
    // und :web deklarieren selbst keine Repositories und nutzen weiter die
    // hier unten.
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}

// Ohne Leerzeichen: Der Wasm-Build leitet daraus einen npm-Paketnamen ab, und
// npm erlaubt keine Leerzeichen. Für :app ist der Name nur der Projekttitel.
rootProject.name = "CurrencyConverter"
include(":app")
// Web-Variante (PWA), siehe pwa-plan.md — eigenständiges Modul, teilt keinen Code mit :app.
include(":web")
