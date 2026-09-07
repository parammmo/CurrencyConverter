package com.param.currencyconverter.ui

import androidx.compose.runtime.saveable.listSaver
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
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

/** Wie viele Nachkommastellen ein *Geldbetrag* höchstens hat. */
internal const val MAX_MONEY_DECIMALS = 2

/**
 * Zahl → Anzeigetext für **Geldbeträge**: immer genau zwei Nachkommastellen.
 *
 * Getrennt von [formatAmount], weil die beiden verschiedene Fragen
 * beantworten. [formatAmount] zeigt eine Zahl so genau wie nötig — richtig
 * für einen Wechselkurs, wo die dritte Stelle noch etwas aussagt.
 * [formatMoney] zeigt einen Betrag so, wie Geld nun mal geschrieben wird.
 *
 * Feste zwei Stellen statt "höchstens zwei" (also "86,00" statt "86"): Sonst
 * würde die 56sp-Zeile beim Tippen ständig die Breite wechseln, und aus
 * "1,50 €" würde "1,5 €". Umstellbar, falls dir das zu laut ist.
 *
 * [RoundingMode.HALF_UP] ist die kaufmännische Rundung, die man von Geld
 * erwartet — Kotlins `roundToLong` und Javas Default `HALF_EVEN` runden 0,125
 * beide anders, als eine Rechnung es täte.
 */
internal fun formatMoney(n: Double): String {
    if (n.isNaN() || n.isInfinite()) return "0"
    // Ab dieser Größe sind Cent-Stellen ohnehin bedeutungslos, und
    // formatAmount hat für den Fall schon die lesbarere Exponentialform.
    if (abs(n) >= 1e10) return formatAmount(n)
    // BigDecimal.valueOf(n), nicht BigDecimal(n): Der Konstruktor nimmt den
    // *exakten* Binärwert eines Double, und der ist für 1,005 in Wahrheit
    // 1,00499999… — gerundet also 1,00, was niemand erwartet. valueOf geht
    // über Double.toString und rundet daher 1,005 zu 1,01.
    return BigDecimal.valueOf(n)
        .setScale(MAX_MONEY_DECIMALS, RoundingMode.HALF_UP)
        .toPlainString()
        .replace('.', ',')
}

/**
 * Wie viele Ziffern nach dem Komma noch getippt werden dürfen.
 * Ohne Komma: beliebig viele (die Gesamtlänge begrenzt [onKey] getrennt).
 */
private fun String.decimalRoom(): Int {
    val comma = indexOf(',')
    if (comma < 0) return Int.MAX_VALUE
    return (MAX_MONEY_DECIMALS - (length - comma - 1)).coerceAtLeast(0)
}

internal fun CalcState.onKey(key: String): CalcState = when {
    // Auch mehrstellig, wegen der "000"-Taste.
    key.isNotEmpty() && key.all { it.isDigit() } -> when {
        freshEntry || entry == "0" -> copy(
            // "000" auf einer 0 (oder als erste Eingabe) bleibt eine 0 —
            // "000" als Betrag stehen zu lassen wäre Unsinn.
            entry = if (key.all { it == '0' }) "0" else key,
            freshEntry = false,
        )
        // take(10) statt Ablehnen: Bei "000" nahe der Grenze sollen die
        // Nullen angehängt werden, die noch passen, statt gar keine.
        // Dieselbe Logik hinterm Komma: Nach "1,2" nimmt "000" noch genau
        // eine Null an, danach ist Schluss — sonst könnte man 1,23456
        // eintippen und sähe es auch, während die Gegenseite auf zwei Stellen
        // gerundet anzeigt. Zwei Zahlen, die nicht zueinander passen, sind
        // schlimmer als eine Taste, die mal nichts tut.
        else -> {
            val accepted = key.take(entry.decimalRoom())
            if (accepted.isEmpty()) this else copy(entry = (entry + accepted).take(10))
        }
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

    key == "%" -> copy(entry = formatMoney(parseEntry(entry) / 100), freshEntry = true)

    key == "=" -> if (pendingOp != null) {
        copy(
            entry = formatMoney(applyOp(accumulator ?: 0.0, pendingOp, parseEntry(entry))),
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
            formatMoney(applyOp(accumulator ?: 0.0, pendingOp, parseEntry(entry)))
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

