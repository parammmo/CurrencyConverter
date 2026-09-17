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
            implementation(libs.cmp.material.icons.core)
            // Für SwapVert, DarkMode, LightMode — nicht im core-Set.
            implementation(libs.cmp.material.icons.extended)
            // strings.xml als `Res.string.*` — das Multiplatform-Pendant zu R.string.
            implementation(libs.cmp.components.resources)
            // ViewModel + collectAsStateWithLifecycle, JetBrains' KMP-Ausgabe
            // derselben androidx-Artefakte.
            implementation(libs.jb.lifecycle.viewmodel.compose)
            implementation(libs.jb.lifecycle.runtime.compose)
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

/**
 * Setzt in sw.js die Build-Kennung ein (`@buildId@` → kurzer Git-Commit).
 * Der Service Worker nutzt sie als Cache-Namen; nur so erkennt der Browser
 * einen neuen Build und räumt den alten Cache weg.
 *
 * ReplaceTokens statt Gradles `expand`: expand ist eine Groovy-Template-
 * Engine und liest jedes `${…}` im JS als Platzhalter. ReplaceTokens
 * ersetzt stumpf `@name@` und lässt alles andere in Ruhe.
 */
val buildId: String = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}.standardOutput.asText.map { it.trim() }.orElse("dev").get()

tasks.named<ProcessResources>("wasmJsProcessResources") {
    // Lokale Kopie: Die Closure unten darf keine Skript-Variable anfassen,
    // sonst kann der Configuration-Cache sie nicht serialisieren.
    val id = buildId
    inputs.property("buildId", id)
    filesMatching("sw.js") {
        filter(org.apache.tools.ant.filters.ReplaceTokens::class, "tokens" to mapOf("buildId" to id))
    }
}

/**
 * Trägt nach dem Bündeln die vollständige Dateiliste in sw.js ein.
 *
 * Warum nicht "beim ersten Abruf cachen": Beim allerersten Laden wird der
 * Service Worker erst *während* des Seitenaufbaus installiert, und web.js
 * fordert die Wasm-Dateien an, bevor er Anfragen abfangen darf. Die wären
 * dann beim ersten Offline-Start nicht da. Also muss der Worker alles
 * vorab laden — und die Namen der Wasm-Dateien (mit Hash) kennt erst dieser
 * Schritt, nach webpack.
 */
tasks.named<Sync>("wasmJsBrowserDistribution") {
    doLast {
        val dist = destinationDir
        val files = dist.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(dist).path }
            .filterNot { it == "sw.js" || it.endsWith(".map") || it.endsWith(".LICENSE.txt") }
            .sorted()
            .joinToString(", ") { "\"./$it\"" }
        val sw = dist.resolve("sw.js")
        sw.writeText(
            sw.readText().replace(Regex("const PRECACHE = .*;"), "const PRECACHE = [\"./\", $files];")
        )
    }
}

compose.resources {
    // Sonst hieße das generierte Paket `currencyconverter.web.generated.resources`.
    packageOfResClass = "com.param.currencyconverter.resources"
    // "auto" schaut nach der Resources-Abhängigkeit in commonMain — unsere
    // steht in wasmJsMain, also explizit einschalten.
    generateResClass = org.jetbrains.compose.resources.ResourcesExtension.ResourceClassGeneration.Always
}
// Die strings.xml liegen unter src/commonMain/composeResources, obwohl es
// nur ein Target gibt: Der Ressourcen-Generator erzeugt die Res-Klasse aus
// commonMain, target-spezifische Ordner überlagern nur.
