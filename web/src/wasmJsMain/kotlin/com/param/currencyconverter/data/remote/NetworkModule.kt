package com.param.currencyconverter.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Baut den Ktor-Client zusammen — dieselbe Rolle wie das NetworkModule in
 * :app, nur ohne den IPv4-Workaround (der galt dem Android-Emulator) und
 * ohne Logging-Interceptor (im Browser zeigt das Netzwerk-Tab der DevTools
 * ohnehin jede Anfrage samt Antwort).
 */
object NetworkModule {

    private const val BASE_URL = "https://open.er-api.com/"

    private val json = Json {
        // Neue Felder in der API-Antwort sollen die App nicht crashen lassen.
        ignoreUnknownKeys = true
    }

    /**
     * `HttpClient { }` ohne Engine-Argument: Ktor nimmt die Engine, die auf
     * dem Classpath liegt — hier `ktor-client-js`, ein Wrapper um `fetch()`.
     *
     * `install(Plugin)` ist Ktors Erweiterungsmechanismus, vergleichbar mit
     * OkHttps Interceptors: ContentNegotiation hängt sich in jede Antwort
     * und wandelt JSON in Kotlin-Objekte.
     */
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        // Retrofits `baseUrl`: relative Pfade in den API-Methoden werden
        // hieran angehängt.
        defaultRequest {
            url(BASE_URL)
        }
    }

    val exchangeRateApi = ExchangeRateApi(client)
}
