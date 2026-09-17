plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    // Nur von :web benutzt. Trotzdem hier: Alle Kotlin-Plugins müssen aus
    // *einer* Stelle mit *einer* Version auf den Classpath, sonst kann Gradle
    // die Kompatibilität nicht prüfen ("already on the classpath with an
    // unknown version").
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
}
