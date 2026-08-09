package com.param.currencyconverter.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit-Interface für die Frankfurter-API (https://frankfurter.dev).
 *
 * Wir schreiben nur das Interface — Retrofit erzeugt zur Laufzeit die
 * Implementierung, die aus den Annotations den HTTP-Call zusammenbaut.
 */
interface FrankfurterApi {

    /**
     * GET /v1/latest?base=EUR&symbols=USD,GBP
     *
     * Ohne [symbols] liefert die API *alle* verfügbaren Kurse — genau das
     * wollen wir fürs Caching, damit ein Request für die ganze App reicht.
     */
    @GET("v1/latest")
    suspend fun latest(
        @Query("base") base: String,
        @Query("symbols") symbols: String? = null,
    ): LatestRatesDto

    /**
     * GET /v1/currencies
     *
     * Antwort ist eine flache Map aus Code -> Klarname:
     *   {"AUD":"Australian Dollar","BRL":"Brazilian Real", ...}
     *
     * Kein eigenes DTO nötig: Die Keys sind hier Daten (Währungscodes), keine
     * feste Struktur — deshalb Map statt data class.
     *
     * Brauchen wir später für den Währungs-Picker: "USD" allein ist eine
     * schlechte Auswahlliste, "USD – United States Dollar" eine gute.
     */
    @GET("v1/currencies")
    suspend fun currencies(): Map<String, String>
}
