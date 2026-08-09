package com.param.currencyconverter.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.param.currencyconverter.data.remote.FrankfurterApi
import com.param.currencyconverter.data.remote.LatestRatesDto
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Top-Level-Property (Kotlin-Idiom für ein Singleton-DataStore pro Context) —
 * vergleichbar mit einer statischen Factory-Methode, die immer dieselbe
 * Instanz zurückgibt, solange derselbe Context genutzt wird.
 */
private val Context.ratesDataStore by preferencesDataStore(name = "rates_cache")

/**
 * Was [ExchangeRateRepository.getRates] zurückgibt — das DTO reicht nicht mehr,
 * weil die UI auch wissen muss, *wann* diese Kurse geholt wurden.
 *
 * Wichtig zu trennen: [date] ist das Datum der EZB-Kurse selbst, [fetchedAt]
 * der Zeitpunkt unseres Abrufs. Beim Cache-Treffer liegen die weit auseinander.
 */
data class RatesResult(
    val base: String,
    val date: String,
    val rates: Map<String, Double>,
    /** Unix-Millis, wann diese Daten vom Netz geholt wurden. */
    val fetchedAt: Long,
    /** true = Cache ist eigentlich abgelaufen, das Netz war aber nicht erreichbar. */
    val isStale: Boolean,
)

/**
 * Cached die *komplette* Kurstabelle für eine Basiswährung (nicht pro
 * Währungspaar — die API liefert eh alle Kurse in einem Call, siehe
 * [FrankfurterApi.latest]).
 *
 * Cache-Strategie: "stale-while-revalidate light" — ist der Cache jünger als
 * [CACHE_TTL_MILLIS], wird er direkt zurückgegeben, kein Netzwerk-Call.
 * Sonst wird neu geladen und der Cache aktualisiert.
 */
class ExchangeRateRepository(
    private val context: Context,
    private val api: FrankfurterApi,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Liefert die aktuellen Kurse für [base].
     *
     * Drei Wege, in dieser Reihenfolge:
     * 1. Cache jünger als [CACHE_TTL_MILLIS] → direkt zurück, kein Netzwerk.
     * 2. Sonst vom Netz laden und cachen.
     * 3. Netz nicht erreichbar, aber alter Cache vorhanden → den zurückgeben,
     *    markiert als [RatesResult.isStale].
     *
     * Punkt 3 ist der Grund für den ganzen Umbau: Wechselkurse von gestern
     * sind fast so nützlich wie die von jetzt, eine leere Fehlermeldung ist
     * es nicht. Nur wenn *gar nichts* da ist, fliegt die Exception weiter.
     */
    suspend fun getRates(base: String): RatesResult {
        val cached = readCache(base)
        val isFresh = cached != null &&
            System.currentTimeMillis() - cached.second < CACHE_TTL_MILLIS

        if (cached != null && isFresh) {
            return cached.first.toResult(fetchedAt = cached.second, isStale = false)
        }

        return try {
            val fresh = api.latest(base = base)
            val now = System.currentTimeMillis()
            writeCache(base, fresh)
            fresh.toResult(fetchedAt = now, isStale = false)
        } catch (e: Exception) {
            // Kein Netz und kein Cache — dann kann auch das Repository nichts
            // mehr retten, die Exception gehört ins ViewModel.
            cached?.first?.toResult(fetchedAt = cached.second, isStale = true) ?: throw e
        }
    }

    private fun LatestRatesDto.toResult(fetchedAt: Long, isStale: Boolean) = RatesResult(
        base = base,
        date = date,
        rates = rates,
        fetchedAt = fetchedAt,
        isStale = isStale,
    )

    private suspend fun readCache(base: String): Pair<LatestRatesDto, Long>? {
        val prefs = context.ratesDataStore.data.first()
        val storedBase = prefs[BASE_KEY]
        // Cache gilt nur, wenn er zur angefragten Basiswährung passt —
        if (storedBase != base) return null

        val json = prefs[RATES_JSON_KEY] ?: return null
        val timestamp = prefs[TIMESTAMP_KEY] ?: return null
        val dto = this.json.decodeFromString(LatestRatesDto.serializer(), json)
        return dto to timestamp
    }

    private suspend fun writeCache(base: String, dto: LatestRatesDto) {
        context.ratesDataStore.edit { prefs ->
            prefs[BASE_KEY] = base
            prefs[RATES_JSON_KEY] = json.encodeToString(dto)
            prefs[TIMESTAMP_KEY] = System.currentTimeMillis()
        }
    }

    companion object {
        // Zum Testen des Stale-Fallbacks kurzzeitig auf z.B. 10 * 1000L setzen.
        private val CACHE_TTL_MILLIS = 6 * 60 * 60 * 1000L // 6 Stunden
        private val BASE_KEY = stringPreferencesKey("cached_base")
        private val RATES_JSON_KEY = stringPreferencesKey("cached_rates_json")
        private val TIMESTAMP_KEY = longPreferencesKey("cached_timestamp")
    }
}
