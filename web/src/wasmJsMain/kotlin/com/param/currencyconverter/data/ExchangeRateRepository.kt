package com.param.currencyconverter.data

import com.param.currencyconverter.data.remote.ExchangeRateApi
import com.param.currencyconverter.data.remote.LatestRatesDto
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Was [ExchangeRateRepository.getRates] zurückgibt — das DTO reicht nicht,
 * weil die UI auch wissen muss, *wann* diese Kurse geholt wurden.
 *
 * Wichtig zu trennen: [date] ist der Stand der Kurse bei der Quelle, [fetchedAt]
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
 * Port des Android-Repositories. Die Cache-Strategie ("stale-while-revalidate
 * light", siehe dort) ist unverändert; getauscht sind nur die Unterbauten:
 * DataStore → [LocalStorageStore], java.time → kotlinx-datetime,
 * `System.currentTimeMillis()` → `Clock.System.now()`.
 */
@OptIn(ExperimentalTime::class)
class ExchangeRateRepository(
    private val api: ExchangeRateApi,
    private val store: LocalStorageStore = LocalStorageStore("rates"),
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Drei Wege, in dieser Reihenfolge:
     * 1. Cache jünger als [CACHE_TTL_MILLIS] → direkt zurück, kein Netzwerk.
     * 2. Sonst vom Netz laden und cachen.
     * 3. Netz nicht erreichbar, aber alter Cache vorhanden → den zurückgeben,
     *    markiert als [RatesResult.isStale]. Nur wenn *gar nichts* da ist,
     *    fliegt die Exception weiter.
     */
    suspend fun getRates(base: String): RatesResult {
        val cached = readCache(base)
        val isFresh = cached != null && now() - cached.second < CACHE_TTL_MILLIS

        if (cached != null && isFresh) {
            return cached.first.toResult(fetchedAt = cached.second, isStale = false)
        }

        return try {
            val fresh = api.latest(base = base)
            // Die Quelle meldet Fachfehler mit HTTP 200 und `result: "error"`.
            if (fresh.result != "success") error("API-Antwort: ${fresh.result}")
            val fetchedAt = now()
            writeCache(base, fresh, fetchedAt)
            fresh.toResult(fetchedAt = fetchedAt, isStale = false)
        } catch (e: Exception) {
            cached?.first?.toResult(fetchedAt = cached.second, isStale = true) ?: throw e
        }
    }

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    private fun LatestRatesDto.toResult(fetchedAt: Long, isStale: Boolean) = RatesResult(
        base = base,
        // kotlinx-datetime: Instant → LocalDateTime braucht immer eine
        // explizite Zeitzone, es gibt kein stilles "Systemzone"-Default.
        // UTC, weil es der Stand der Quelle ist, nicht ein Zeitpunkt beim Nutzer.
        date = Instant.fromEpochSeconds(lastUpdateUnix)
            .toLocalDateTime(TimeZone.UTC)
            .date
            .toString(),
        rates = rates,
        fetchedAt = fetchedAt,
        isStale = isStale,
    )

    private fun readCache(base: String): Pair<LatestRatesDto, Long>? {
        if (store[BASE_KEY] != base) return null
        val cachedJson = store[RATES_JSON_KEY] ?: return null
        val timestamp = store[TIMESTAMP_KEY]?.toLongOrNull() ?: return null
        // Unlesbaren Cache wie keinen Cache behandeln — statt beim Start zu
        // crashen, wenn sich das Format mal ändert.
        val dto = runCatching { json.decodeFromString(LatestRatesDto.serializer(), cachedJson) }
            .getOrNull() ?: return null
        return dto to timestamp
    }

    private fun writeCache(base: String, dto: LatestRatesDto, fetchedAt: Long) {
        store[BASE_KEY] = base
        store[RATES_JSON_KEY] = json.encodeToString(LatestRatesDto.serializer(), dto)
        store[TIMESTAMP_KEY] = fetchedAt.toString()
    }

    companion object {
        private const val CACHE_TTL_MILLIS = 6 * 60 * 60 * 1000L // 6 Stunden
        private const val BASE_KEY = "cached_base"
        private const val RATES_JSON_KEY = "cached_rates_json"
        private const val TIMESTAMP_KEY = "cached_timestamp"
    }
}
