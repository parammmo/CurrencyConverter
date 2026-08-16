package com.param.currencyconverter.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit-Interface für die Frankfurter-API (https://frankfurter.dev).
 *
 * Wir schreiben nur das Interface — Retrofit erzeugt zur Laufzeit die
 * Implementierung, die aus den Annotations den HTTP-Call zusammenbaut.
 *
 * **Warum nur 30 Währungen?** Frankfurter reicht die Referenzkurse der EZB
 * durch, und die veröffentlicht genau diese 30. Währungen wie COP, ARS, AED
 * oder VND sind deshalb nicht bloß nicht eingebaut — sie existieren in dieser
 * Quelle nicht.
 *
 * Geprüft am 2026-08-16, bewusst so entschieden. Die Alternativen mit mehr
 * Währungen haben jeweils einen Preis:
 *  - open.er-api.com (166): verlangt eine sichtbare Namensnennung in der App.
 *  - fawazahmed0 via jsDelivr (339): Community-Projekt ohne Betriebszusage.
 *  - ExchangeRate-API Free (166): Schlüssel nötig, und 1.500 Anfragen pro
 *    Monat gelten für *alle* Installationen zusammen — bei 6h-Cache reicht das
 *    für gut ein Dutzend Nutzer.
 *
 * Die EZB-Kurse sind dafür amtlich und ohne Auflagen nutzbar.
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
