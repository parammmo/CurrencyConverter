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
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        // Name der erzeugten JS/Wasm-Dateien (web.js, web.wasm) — index.html
        // bindet sie unter diesem Namen ein.
        outputModuleName.set("web")
        browser()
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
        }
    }
}
