package com.param.currencyconverter.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Bewusst ein *eigener* DataStore, getrennt vom Kurs-Cache in
 * [ExchangeRateRepository]: Der Cache ist jederzeit wegwerfbar (löschen tut
 * nicht weh, wird neu geladen), die Währungsauswahl dagegen ist echte
 * Nutzereingabe. Zwei Dateien heißt auch: den Cache leeren kann nie
 * versehentlich die Einstellungen mitnehmen.
 */
private val Context.settingsDataStore by preferencesDataStore(name = "user_settings")

/** Das zuletzt gewählte Währungspaar. */
data class CurrencyPair(
    val from: String,
    val to: String,
)

/**
 * Drei Zustände statt eines Booleans: "folge dem System" ist ein eigener
 * Zustand, nicht dasselbe wie "hell". Mit `isDark: Boolean` könnte man nicht
 * mehr ausdrücken, dass der Nutzer noch gar nichts entschieden hat.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

class UserPreferencesRepository(private val context: Context) {

    /**
     * Als [Flow], nicht als einmaliger Lesevorgang: DataStore meldet jede
     * Änderung von selbst weiter. Das ViewModel muss also nie aktiv nachfragen,
     * ob sich was geändert hat — dasselbe Prinzip wie bei Room in der NoteApp.
     */
    val currencyPair: Flow<CurrencyPair> = context.settingsDataStore.data.map { prefs ->
        CurrencyPair(
            from = prefs[FROM_KEY] ?: DEFAULT_FROM,
            to = prefs[TO_KEY] ?: DEFAULT_TO,
        )
    }

    /**
     * Unbekannte gespeicherte Werte fallen auf [ThemeMode.SYSTEM] zurück statt
     * zu crashen — z.B. wenn eine spätere App-Version einen Modus wieder
     * entfernt, der beim Nutzer noch gespeichert ist.
     */
    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        val stored = prefs[THEME_MODE_KEY]
        ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs -> prefs[THEME_MODE_KEY] = mode.name }
    }

    suspend fun setFrom(code: String) {
        context.settingsDataStore.edit { prefs -> prefs[FROM_KEY] = code }
    }

    suspend fun setTo(code: String) {
        context.settingsDataStore.edit { prefs -> prefs[TO_KEY] = code }
    }

    /**
     * Beide Werte in *einer* Transaktion. Zwei einzelne [setFrom]/[setTo]-Aufrufe
     * täten es fachlich auch, würden aber zwei Flow-Emissionen auslösen — und
     * dazwischen gäbe es einen kurzen Moment, in dem beide Währungen gleich
     * sind und die UI Unsinn anzeigt.
     */
    suspend fun setPair(from: String, to: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[FROM_KEY] = from
            prefs[TO_KEY] = to
        }
    }

    companion object {
        const val DEFAULT_FROM = "EUR"
        const val DEFAULT_TO = "USD"

        private val FROM_KEY = stringPreferencesKey("from_currency")
        private val TO_KEY = stringPreferencesKey("to_currency")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }
}
