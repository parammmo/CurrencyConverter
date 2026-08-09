package com.param.currencyconverter.data.remote

import kotlinx.serialization.Serializable

/**
 * Bildet die Antwort von GET /v1/latest 1:1 ab:
 *
 * {"amount":1.0,"base":"EUR","date":"2026-08-03","rates":{"GBP":0.85633,"USD":1.1535}}
 *
 * Bewusst nah am Wire-Format: [date] bleibt String, die Umwandlung nach LocalDate
 * passiert erst beim Mappen ins Domain-Model.
 */
@Serializable
data class LatestRatesDto(
    val amount: Double,
    val base: String,
    val date: String,
    val rates: Map<String, Double>,
)
