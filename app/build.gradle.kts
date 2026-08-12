import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * Zugangsdaten für die Release-Signatur.
 *
 * Sie stehen in `keystore.properties` im Projektwurzelverzeichnis, die
 * absichtlich *nicht* im Repository liegt (siehe .gitignore) — Passwörter
 * gehören nicht in die Versionsverwaltung, auch nicht in ein privates Repo.
 *
 * Fehlt die Datei, wird der Release-Build einfach unsigniert gebaut, statt
 * mit einem Fehler abzubrechen. Sonst könnte niemand das Projekt auschecken
 * und bauen, ohne vorher einen Schlüssel zu haben.
 */
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseSigning = keystorePropertiesFile.exists()

android {
    namespace = "com.param.currencyconverter"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.param.currencyconverter"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                // "~" auflösen, damit in keystore.properties auch ein Pfad wie
                // ~/keys/... stehen darf und nicht nur ein absoluter.
                storeFile = file(
                    keystoreProperties.getProperty("storeFile")
                        .replaceFirst("~", System.getProperty("user.home"))
                )
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            // R8 wirft ungenutzten Code und ungenutzte Ressourcen raus. Ohne
            // das landen z.B. sämtliche Material-Icons im APK, obwohl wir drei
            // davon benutzen.
            optimization {
                enable = true
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        // Für BuildConfig.DEBUG — steuert, ob der HTTP-Logger anspringt.
        buildConfig = true
    }
}

dependencies {
    implementation(libs.material)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    // Für SwapVert — das Swap-Icon ist nicht im core-Set enthalten.
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.runtime)

    // ViewModel in Compose + collectAsStateWithLifecycle()
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.logging.interceptor)

    // Persistenter Cache (überlebt App-Neustart)
    implementation(libs.datastore.preferences)

    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
