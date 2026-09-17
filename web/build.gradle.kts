plugins {
    // Multiplatform statt android.application: Dieses Modul hat kein Android-
    // Target, nur wasmJs. "Multiplatform" ist trotzdem der richtige Plugin-
    // Typ, weil nur er Nicht-JVM-Targets kennt.
    alias(libs.plugins.kotlin.multiplatform)
    // JetBrains' Compose-Plugin: liefert die `compose.*`-Abhängigkeits-
    // Accessors und die wasmJs-Build-Tasks (webpack, Distribution).
    alias(libs.plugins.compose.multiplatform)
    // Derselbe Compose-Compiler wie in :app — ohne ihn sind @Composable-
    // Funktionen nur normale Funktionen.
    alias(libs.plugins.compose.compiler)
    // @Serializable fürs DTO — wie in :app.
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        // Name der erzeugten JS/Wasm-Dateien (web.js, web.wasm) — index.html
        // bindet sie unter diesem Namen ein.
        outputModuleName.set("web")
        browser {
            // Tests laufen im echten Browser (Karma + Chrome headless), nicht
            // auf der JVM — nur so testet man das Wasm-Verhalten von z.B.
            // Double.toString(), und genau darauf baut die Rundung auf.
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        // Ohne das entsteht nur eine Library, kein startbares Programm.
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(libs.cmp.runtime)
            implementation(libs.cmp.foundation)
            implementation(libs.cmp.material3)
            implementation(libs.cmp.ui)
            // `document`, `window`, localStorage — in Kotlin/Wasm nicht mehr
            // Teil der Stdlib, sondern eigene Lib.
            implementation(libs.kotlinx.browser)
            implementation(libs.kotlinx.coroutines.core)
            // Ersatz für java.time — Instant/LocalDate ohne JVM.
            implementation(libs.kotlinx.datetime)

            // Networking: Ktor statt Retrofit/OkHttp (die sind JVM-only).
            // Aufbau ist derselbe: Client + Engine + JSON-Konverter.
            implementation(libs.ktor.client.core)
            // Die Engine: im Browser ist das ein Wrapper um `fetch()`.
            implementation(libs.ktor.client.js)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
        }
        wasmJsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
