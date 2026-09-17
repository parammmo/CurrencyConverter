package com.param.currencyconverter.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit-Interface für die Open-Access-API von ExchangeRate-API
 * (https://open.er-api.com, Doku: https://www.exchangerate-api.com/docs/free).
 *
 * Wir schreiben nur das Interface — Retrofit erzeugt zur Laufzeit die
 * Implementierung, die aus den Annotations den HTTP-Call zusammenbaut.
 *
 * **Warum nicht mehr Frankfurter/EZB?** Frankfurter reichte die
 * EZB-Referenzkurse durch, und die umfassen nur ~30 Währungen. Alles darüber
 * hinaus (MKD, COP, ARS, AED, VND …) existierte in dieser Quelle schlicht
 * nicht — das war keine Lücke im Code, sondern in den Daten.
 *
 * Diese Quelle hat 166 Währungen, braucht keinen API-Schlüssel und kein
 * Kontingent. Der Preis: Die Nutzungsbedingungen verlangen eine **sichtbare
 * Namensnennung** in der App (siehe `attribution`-String in der Fußzeile).
 * Umgestellt am 2026-09-07.
 *
 * Zweiter Unterschied zu vorher: kein `/currencies`-Endpunkt mit Klarnamen.
 * Den hatten wir zwar deklariert, aber nie aufgerufen — die Auswahlliste baut
 * sich aus den Keys der Kurstabelle. Deshalb hier ersatzlos gestrichen.
 */
interface ExchangeRateApi {

    /**
     * GET /v6/latest/EUR
     *
     * Anders als bei Frankfurter steht die Basiswährung im **Pfad**, nicht in
     * der Query — deshalb `@Path` statt `@Query`. Es gibt auch keinen
     * `symbols`-Filter: Die Antwort enthält immer alle Kurse. Für uns ideal,
     * genau das wollen wir fürs Caching (ein Request für die ganze App).
     */
    @GET("v6/latest/{base}")
    suspend fun latest(@Path("base") base: String): LatestRatesDto
}
