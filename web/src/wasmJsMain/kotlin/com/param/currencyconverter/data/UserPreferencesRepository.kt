package com.param.currencyconverter.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Das zuletzt gewählte Währungspaar. */
data class CurrencyPair(
    val from: String,
    val to: String,
)

/** Drei Zustände statt eines Booleans — "folge dem System" ist ein eigener. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Port des Android-Repositories mit **identischer Schnittstelle** (dieselben
 * Flows, dieselben suspend-Funktionen), damit das ViewModel unverändert
 * übernommen werden kann.
 *
 * Unter der Haube anders: DataStore liefert von selbst einen Flow, der bei
 * jeder Änderung feuert. localStorage kann das nicht. Deshalb hält dieses
 * Repository den kompletten Stand in einem [MutableStateFlow] — der ist die
 * Wahrheit für die UI — und schreibt jede Änderung *zusätzlich* nach
 * localStorage, damit sie den Neustart überlebt. Beim Start wird der
 * StateFlow einmal aus localStorage befüllt.
 */
class UserPreferencesRepository(
    private val store: LocalStorageStore = LocalStorageStore("settings"),
) {

    /** Alles, was gespeichert wird, in einem Objekt — eine Quelle, ein Update. */
    private data class Settings(
        val from: String,
        val to: String,
        val themeMode: ThemeMode,
        val recents: List<String>,
    )

    private val settings = MutableStateFlow(load())

    val currencyPair: Flow<CurrencyPair> = settings.map { CurrencyPair(it.from, it.to) }

    val themeMode: Flow<ThemeMode> = settings.map { it.themeMode }

    /** Die zuletzt gewählten Währungen, neueste zuerst. */
    val recentCurrencies: Flow<List<String>> = settings.map { it.recents }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it.copy(themeMode = mode) }

    suspend fun setFrom(code: String) = edit { it.copy(from = code, recents = it.withRecent(code)) }

    suspend fun setTo(code: String) = edit { it.copy(to = code, recents = it.withRecent(code)) }

    /** Beide Werte in *einem* Update — kein Zwischenbild mit zwei gleichen Währungen. */
    suspend fun setPair(from: String, to: String) = edit { it.copy(from = from, to = to) }

    /**
     * `update` auf dem StateFlow ist das Pendant zu DataStores `edit { }`:
     * eine atomare Änderung, danach genau eine Emission. Der Schreibvorgang
     * nach localStorage hängt hinten dran.
     */
    private fun edit(transform: (Settings) -> Settings) {
        settings.update(transform)
        save(settings.value)
    }

    /** Neuer Code nach vorn, Dubletten raus, hinten abgeschnitten. */
    private fun Settings.withRecent(code: String): List<String> =
        (listOf(code) + recents.filter { it != code }).take(MAX_RECENTS)

    private fun load() = Settings(
        from = store[FROM_KEY] ?: DEFAULT_FROM,
        to = store[TO_KEY] ?: DEFAULT_TO,
        // Unbekannte Werte fallen auf SYSTEM zurück statt zu crashen.
        themeMode = ThemeMode.entries.firstOrNull { it.name == store[THEME_MODE_KEY] }
            ?: ThemeMode.SYSTEM,
        recents = store[RECENTS_KEY].orEmpty().split(',').filter { it.isNotBlank() },
    )

    private fun save(s: Settings) {
        store[FROM_KEY] = s.from
        store[TO_KEY] = s.to
        store[THEME_MODE_KEY] = s.themeMode.name
        store[RECENTS_KEY] = s.recents.joinToString(",")
    }

    companion object {
        const val DEFAULT_FROM = "EUR"
        const val DEFAULT_TO = "USD"
        const val MAX_RECENTS = 5

        private const val FROM_KEY = "from_currency"
        private const val TO_KEY = "to_currency"
        private const val THEME_MODE_KEY = "theme_mode"
        private const val RECENTS_KEY = "recent_currencies"
    }
}
