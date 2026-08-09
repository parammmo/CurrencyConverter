package com.param.currencyconverter.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.MathContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

// ---------------------------------------------------------------------------
// Farbpalette
// ---------------------------------------------------------------------------

/**
 * Der Entwurf bringt seine **eigene** Palette mit (Terracotta) und ignoriert
 * unser Teal-Theme bewusst.
 *
 * Deshalb liegen die Farben hier lokal und nicht in `ui/theme/Color.kt`: Diese
 * Variante ist ein Gegenvorschlag zum bestehenden Farbschema, kein Teil davon.
 * Entscheidest du dich für sie, wandern die Werte in das richtige Theme —
 * bis dahin soll sie unser Farbsystem nicht verwässern.
 */
private data class CalcPalette(
    val appBackground: Color,
    val cardBackground: Color,
    val cardLabel: Color,
    val amount: Color,
    val numberKeyBackground: Color,
    val numberKeyText: Color,
    val numberKeyShadow: Dp,
    val utilityKeyBackground: Color,
    val utilityKeyText: Color,
    val operatorKeyBackground: Color,
    val operatorKeyText: Color,
    val equalsKeyBackground: Color,
    val footerIcon: Color,
    val footerDate: Color,
    val footerRate: Color,
    val activeRing: Color,
)

private val CalcLight = CalcPalette(
    appBackground = Color(0xFFF4ECE6),
    cardBackground = Color(0xFFFCF7F3),
    cardLabel = Color(0xFF332016),
    amount = Color(0xFF2E1B12),
    numberKeyBackground = Color(0xFFFFFFFF),
    numberKeyText = Color(0xFF3A251A),
    numberKeyShadow = 1.dp,
    utilityKeyBackground = Color(0xFFEAD9CE),
    utilityKeyText = Color(0xFF7A4530),
    operatorKeyBackground = Color(0xFFA6401F),
    operatorKeyText = Color(0xFFFFF7F2),
    equalsKeyBackground = Color(0xFF8F3418),
    footerIcon = Color(0xFF7A4530),
    footerDate = Color(0xFFA6401F),
    footerRate = Color(0xFF8A6A5C),
    activeRing = Color(0xFFA6401F),
)

private val CalcDark = CalcPalette(
    appBackground = Color(0xFF1B120E),
    cardBackground = Color(0xFF261A14),
    cardLabel = Color(0xFFF5E9E1),
    amount = Color(0xFFF8EFE8),
    numberKeyBackground = Color(0xFF2E1F17),
    numberKeyText = Color(0xFFF1E2D8),
    // Im Dunkelmodus verzichtet der Entwurf bewusst auf Schatten — auf
    // dunklem Grund würde er nur schmutzig aussehen statt zu heben.
    numberKeyShadow = 0.dp,
    utilityKeyBackground = Color(0xFF3B2A20),
    utilityKeyText = Color(0xFFE0B9A4),
    operatorKeyBackground = Color(0xFFB54B26),
    operatorKeyText = Color(0xFFFFF3EC),
    equalsKeyBackground = Color(0xFFC55A31),
    footerIcon = Color(0xFFE0B9A4),
    footerDate = Color(0xFFD97B52),
    footerRate = Color(0xFFA5826F),
    activeRing = Color(0xFFC55A31),
)

/** Der Entwurf definiert genau zwei Abzeichenfarben: EUR blau, alles andere terracotta. */
private val BadgeEur = Color(0xFF3B5BA5)
private val BadgeOther = Color(0xFFC0562B)
private val BadgeForeground = Color(0xFFFFF7F2)

private val CardRadius = 20.dp
private val KeyRadius = 18.dp

// ---------------------------------------------------------------------------
// Rechner-Zustand
// ---------------------------------------------------------------------------

/**
 * Taschenrechner-Zustand, 1:1 aus dem Prototyp übernommen.
 *
 * [entry] ist bewusst ein String und keine Zahl: "0," oder "1,50" sind gültige
 * Zwischenstände beim Tippen, die als Double nicht darstellbar wären.
 */
private data class CalcState(
    val entry: String = "0",
    val accumulator: Double? = null,
    val pendingOp: Char? = null,
    /** true = der nächste Ziffernanschlag beginnt eine neue Eingabe. */
    val freshEntry: Boolean = false,
    /** true = die obere Karte ist die Eingabeseite. */
    val activeTop: Boolean = true,
)

private val CalcStateSaver = listSaver<CalcState, Any?>(
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

private fun parseEntry(s: String): Double = s.replace(',', '.').toDoubleOrNull() ?: 0.0

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
private fun formatAmount(n: Double): String {
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

private fun CalcState.onKey(key: String): CalcState = when {
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

// ---------------------------------------------------------------------------
// Tastenfeld-Definition
// ---------------------------------------------------------------------------

private enum class KeyKind { NUMBER, UTILITY, OPERATOR, EQUALS }

private data class KeySpec(val label: String, val code: String, val kind: KeyKind)

private val KeyRows: List<List<KeySpec>> = listOf(
    listOf(
        KeySpec("C", "C", KeyKind.UTILITY),
        KeySpec("⌫", "bs", KeyKind.UTILITY),
        KeySpec("⇅", "swap", KeyKind.UTILITY),
        KeySpec("÷", "/", KeyKind.OPERATOR),
    ),
    listOf(
        KeySpec("7", "7", KeyKind.NUMBER),
        KeySpec("8", "8", KeyKind.NUMBER),
        KeySpec("9", "9", KeyKind.NUMBER),
        KeySpec("×", "*", KeyKind.OPERATOR),
    ),
    listOf(
        KeySpec("4", "4", KeyKind.NUMBER),
        KeySpec("5", "5", KeyKind.NUMBER),
        KeySpec("6", "6", KeyKind.NUMBER),
        KeySpec("−", "-", KeyKind.OPERATOR),
    ),
    listOf(
        KeySpec("1", "1", KeyKind.NUMBER),
        KeySpec("2", "2", KeyKind.NUMBER),
        KeySpec("3", "3", KeyKind.NUMBER),
        KeySpec("+", "+", KeyKind.OPERATOR),
    ),
    listOf(
        KeySpec("0", "0", KeyKind.NUMBER),
        KeySpec(",", ",", KeyKind.NUMBER),
        KeySpec("%", "%", KeyKind.NUMBER),
        KeySpec("=", "=", KeyKind.EQUALS),
    ),
)

// ---------------------------------------------------------------------------
// Variante 3: Taschenrechner
// ---------------------------------------------------------------------------

/**
 * Umsetzung der Design-Vorlage aus `Currency Exchange App Design.zip`.
 *
 * Grundgedanke des Entwurfs: Ein Umrechner *ist* ein Taschenrechner. Statt
 * eines Eingabefeldes plus Systemtastatur gibt es ein eigenes Tastenfeld mit
 * Rechenoperationen — man kann also "3 × 12,50 €" direkt eintippen, statt
 * vorher im Kopf zu rechnen. Die beiden Karten sind gleichwertig: Man tippt
 * die an, in die man eingeben will, die andere zeigt das Ergebnis.
 */
@Composable
fun CalculatorLayout(data: ConverterLayoutData, modifier: Modifier = Modifier) {
    val palette = if (data.darkTheme) CalcDark else CalcLight
    var calc by rememberSaveable(stateSaver = CalcStateSaver) { mutableStateOf(CalcState()) }

    val entered = parseEntry(calc.entry)
    val converted = data.rate?.let {
        formatAmount(if (calc.activeTop) entered * it else entered / it)
    } ?: "—"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.appBackground)
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CurrencyCard(
            code = data.fromCurrency,
            amount = if (calc.activeTop) calc.entry else converted,
            active = calc.activeTop,
            palette = palette,
            // Kartentipp macht die Seite zur Eingabeseite und setzt zurück —
            // sonst stünde dort plötzlich ein umgerechneter Wert als Eingabe.
            onFocus = { calc = CalcState(activeTop = true) },
            options = data.currencies,
            onSelect = data.onFromSelected,
        )

        CurrencyCard(
            code = data.toCurrency,
            amount = if (calc.activeTop) converted else calc.entry,
            active = !calc.activeTop,
            palette = palette,
            onFocus = { calc = CalcState(activeTop = false) },
            options = data.currencies,
            onSelect = data.onToSelected,
        )

        Keypad(
            palette = palette,
            onKey = { code ->
                if (code == "swap") data.onSwap() else calc = calc.onKey(code)
            },
            modifier = Modifier.weight(1f),
        )

        Footer(data = data, palette = palette)
    }
}

@Composable
private fun CurrencyCard(
    code: String,
    amount: String,
    active: Boolean,
    palette: CalcPalette,
    onFocus: () -> Unit,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    // Abweichung vom Entwurf: Der zeigt nur Tauschen, keine Auswahl. Ohne
    // einen Weg zur Währungsliste wäre die Variante aber nicht bedienbar —
    // also hängt sie am Abzeichen, das dafür ein eigenes Tippziel bekommt.
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(palette.cardBackground)
            .then(
                if (active) {
                    Modifier.border(2.dp, palette.activeRing, RoundedCornerShape(CardRadius))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onFocus)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (code == "EUR") BadgeEur else BadgeOther)
                        .clickable { pickerOpen = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = currencyGlyph(code),
                        color = BadgeForeground,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                DropdownMenu(
                    expanded = pickerOpen,
                    onDismissRequest = { pickerOpen = false },
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onSelect(option)
                                pickerOpen = false
                            },
                        )
                    }
                }
            }
            Text(
                text = code,
                color = palette.cardLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.04.em,
            )
        }

        Text(
            text = amount,
            color = palette.amount,
            style = TextStyle(
                fontSize = 52.sp,
                fontWeight = FontWeight.SemiBold,
                // Tabellenziffern: alle Ziffern gleich breit, damit der Wert
                // beim Tippen nicht zappelt.
                fontFeatureSettings = "tnum",
                textAlign = TextAlign.End,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        )
    }
}

@Composable
private fun Keypad(
    palette: CalcPalette,
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KeyRows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    CalcKey(
                        key = key,
                        palette = palette,
                        onClick = { onKey(key.code) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalcKey(
    key: KeySpec,
    palette: CalcPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // Der Entwurf gibt scale(0.94) als Druckfeedback vor — kein Material-Ripple.
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "keyPressScale",
    )

    val background = when (key.kind) {
        KeyKind.NUMBER -> palette.numberKeyBackground
        KeyKind.UTILITY -> palette.utilityKeyBackground
        KeyKind.OPERATOR -> palette.operatorKeyBackground
        KeyKind.EQUALS -> palette.equalsKeyBackground
    }
    val foreground = when (key.kind) {
        KeyKind.NUMBER -> palette.numberKeyText
        KeyKind.UTILITY -> palette.utilityKeyText
        KeyKind.OPERATOR, KeyKind.EQUALS -> palette.operatorKeyText
    }
    val fontSize = when (key.kind) {
        KeyKind.NUMBER -> 26.sp
        KeyKind.UTILITY -> 24.sp
        KeyKind.OPERATOR, KeyKind.EQUALS -> 28.sp
    }

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (key.kind == KeyKind.NUMBER && palette.numberKeyShadow > 0.dp) {
                    Modifier.shadow(palette.numberKeyShadow, RoundedCornerShape(KeyRadius))
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(KeyRadius))
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = key.label,
            color = foreground,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun Footer(data: ConverterLayoutData, palette: CalcPalette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphButton(glyph = "↻", tint = palette.footerIcon, onClick = data.onReload)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = data.fetchedAt?.let { formatCalcTimestamp(it) } ?: "—",
                color = palette.footerDate,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = data.rate
                    ?.let { "1 ${data.fromCurrency} = ${formatAmount(it)} ${data.toCurrency}" }
                    ?: "Kein Kurs verfügbar",
                color = palette.footerRate,
                fontSize = 12.sp,
            )
        }

        // Der Entwurf zeigt hier ein Überlauf-Menü, legt aber nicht fest, was
        // drinsteht. Bleibt vorerst ohne Funktion.
        GlyphButton(glyph = "⋯", tint = palette.footerIcon, onClick = {})
    }
}

@Composable
private fun GlyphButton(glyph: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = glyph, color = tint, fontSize = 20.sp)
    }
}

/** Datum/Uhrzeit im Format des Entwurfs: `DD/MM/YYYY HH:mm`. */
private val CalcTimestampFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.GERMANY)

private fun formatCalcTimestamp(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(CalcTimestampFormatter)

/**
 * Symbol fürs Abzeichen. Der Entwurf zeigt "€" und "ɱ"; für die ~30 Währungen
 * unserer API nehmen wir das offizielle Symbol aus [Currency], und wenn es
 * keins gibt (viele Codes liefern nur den Code zurück), den ersten Buchstaben.
 */
private fun currencyGlyph(code: String): String {
    val symbol = runCatching { Currency.getInstance(code).symbol }.getOrNull()
    return when {
        symbol == null || symbol == code -> code.take(1)
        symbol.length <= 2 -> symbol
        else -> code.take(1)
    }
}
