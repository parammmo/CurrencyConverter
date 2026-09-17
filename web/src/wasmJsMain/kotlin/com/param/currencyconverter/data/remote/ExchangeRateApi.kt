package com.param.currencyconverter.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * Bei Retrofit war das ein Interface mit Annotationen, aus dem Retrofit zur
 * Laufzeit per Reflection eine Implementierung baut. Reflection gibt es in
 * Kotlin/Wasm nicht — Ktor geht daher den umgekehrten Weg: Wir schreiben
 * die Methode selbst, sie ist ein Einzeiler.
 *
 * `body<T>()` ist das Gegenstück zum Retrofit-Converter: Ktor sucht sich
 * anhand des Typs den passenden Deserializer (hier kotlinx-serialization).
 */
class ExchangeRateApi(private val client: HttpClient) {

    suspend fun latest(base: String): LatestRatesDto =
        client.get("v6/latest/$base").body()
}
