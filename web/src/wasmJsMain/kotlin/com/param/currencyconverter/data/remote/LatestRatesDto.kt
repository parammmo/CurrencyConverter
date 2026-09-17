package com.param.currencyconverter.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Bildet die Antwort von GET /v6/latest/{base} ab — nur die Felder, die wir
 * brauchen. Die Antwort enthält noch `provider`, `documentation`,
 * `terms_of_use` und `time_next_update_*`; die fallen unter
 * `ignoreUnknownKeys` weg.
 *
 * {"result":"success","base_code":"EUR","time_last_update_unix":1788739351,
 *  "rates":{"EUR":1,"AED":4.265034, … ,"MKD":61.497, …}}
 *
 * `@SerialName` bildet die JSON-Schreibweise (snake_case) auf unsere
 * Kotlin-Namen ab — das Wire-Format bestimmt der Server, die Namen im Code
 * bestimmen wir.
 *
 * Bewusst nah am Wire-Format: [lastUpdateUnix] bleibt eine Zahl, die
 * Umwandlung in ein Datum passiert erst beim Mappen ins Domain-Model.
 */
@Serializable
data class LatestRatesDto(
    /** "success" oder "error" — die API meldet Fachfehler auch mit HTTP 200. */
    val result: String,
    @SerialName("base_code") val base: String,
    /** Unix-Zeit in **Sekunden**, wann die Quelle die Kurse zuletzt gesetzt hat. */
    @SerialName("time_last_update_unix") val lastUpdateUnix: Long,
    val rates: Map<String, Double>,
)
