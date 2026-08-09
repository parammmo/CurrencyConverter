package com.param.currencyconverter.ui

import androidx.compose.runtime.saveable.listSaver
import java.math.BigDecimal
import java.math.MathContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

// Gemeinsame Rechnerlogik beider Taschenrechner-Varianten (v1 und v2 der
// Design-Vorlage). Beide Handoffs beschreiben exakt dieselbe Semantik, also
// liegt sie hier einmal statt zweimal — nur die Darstellung unterscheidet sich.

// ---------------------------------------------------------------------------
// Rechner-Zustand
// ---------------------------------------------------------------------------

/**
 * Taschenrechner-Zustand, 1:1 aus dem Prototyp übernommen.
 *
 * [entry] ist bewusst ein String und keine Zahl: "0," oder "1,50" sind gültige
 * Zwischenstände beim Tippen, die als Double nicht darstellbar wären.
 */
internal data class CalcState(
    val entry: String = "0",
    val accumulator: Double? = null,
    val pendingOp: Char? = null,
    /** true = der nächste Ziffernanschlag beginnt eine neue Eingabe. */
    val freshEntry: Boolean = false,
    /** true = die obere Karte ist die Eingabeseite. */
    val activeTop: Boolean = true,
)

internal val CalcStateSaver = listSaver<CalcState, Any?>(
    save = {
        listOf(it.entry, it.accumulator, it.pendingOp?.toString(), it.freshEntry, it.activeTop)
    },
    restore = {
        CalcState(
            entry = it[0] as String,
            accumulator = it[1] as Double?,
            pendingOp = (it[2] as String?)?.firstOrNull(),
            freshEntry = it[3] as Boolean,
            activeTop = it[4] as Boolean,
        )
    },
)

internal fun parseEntry(s: String): Double = s.replace(',', '.').toDoubleOrNull() ?: 0.0

private fun applyOp(a: Double, op: Char, b: Double): Double = when (op) {
    '+' -> a + b
    '-' -> a - b
    '*' -> a * b
    else -> if (b == 0.0) 0.0 else a / b
}

/** Wie JS `String(zahl)`: ganze Zahlen ohne ".0", sonst kürzeste Darstellung. */
private fun plainString(v: Double): String =
    if (v == Math.floor(v) && abs(v) < 1e15) v.toLong().toString() else v.toString()

/**
 * Zahl → Anzeigetext, portiert aus `raw()` im Prototyp: auf 6 Nachkommastellen
 * runden, sehr große Zahlen exponentiell, sehr lange auf 8 signifikante
 * Stellen kürzen, Punkt zu Komma.
 */
internal fun formatAmount(n: Double): String {
    if (n.isNaN() || n.isInfinite()) return "0"
    var s = if (abs(n) >= 1e10) {
        String.format(Locale.US, "%.4e", n)
    } else {
        plainString((n * 1e6).roundToLong() / 1e6)
    }
    if (s.length > 11) {
        s = plainString(BigDecimal(n).round(MathContext(8)).toDouble())
    }
    return s.replace('.', ',')
}

internal fun CalcState.onKey(key: String): CalcState = when {
    key.length == 1 && key[0].isDigit() -> when {
        freshEntry || entry == "0" -> copy(entry = key, freshEntry = false)
        entry.length < 10 -> copy(entry = entry + key)
        else -> this
    }

    key == "," -> when {
        freshEntry -> copy(entry = "0,", freshEntry = false)
        !entry.contains(',') -> copy(entry = "$entry,")
        else -> this
    }

    key == "C" -> CalcState(activeTop = activeTop)

    key == "bs" -> copy(
        entry = if (entry.length > 1) entry.dropLast(1) else "0",
        freshEntry = false,
    )

    key == "%" -> copy(entry = formatAmount(parseEntry(entry) / 100), freshEntry = true)

    key == "=" -> if (pendingOp != null) {
        copy(
            entry = formatAmount(applyOp(accumulator ?: 0.0, pendingOp, parseEntry(entry))),
            pendingOp = null,
            accumulator = null,
            freshEntry = true,
        )
    } else {
        this
    }

    else -> {
        // Operator: Steht schon einer an und wurde seitdem getippt, wird erst
        // ausgerechnet — so kettet sich "2 + 3 + 4" wie auf einem echten Rechner.
        val evaluated = if (pendingOp != null && !freshEntry) {
            formatAmount(applyOp(accumulator ?: 0.0, pendingOp, parseEntry(entry)))
        } else {
            entry
        }
        copy(
            entry = evaluated,
            accumulator = parseEntry(evaluated),
            pendingOp = key[0],
            freshEntry = true,
        )
    }
}

