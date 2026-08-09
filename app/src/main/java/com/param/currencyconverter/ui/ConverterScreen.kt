package com.param.currencyconverter.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.param.currencyconverter.ConverterUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------------------------------------------------------------------------
// Farbpalette (v2)
// ---------------------------------------------------------------------------

/**
 * Deutlich kleiner als die Palette von v1: Der Entwurf kommt mit fünf Rollen
 * aus, weil es keine Kachelflächen mehr gibt, die eigene Farben bräuchten.
 * Alles steht direkt auf dem App-Hintergrund.
 */
private data class MinimalPalette(
    val appBackground: Color,
    val hairline: Color,
    val amount: Color,
    val digit: Color,
    /** Operatoren, aktiver Währungscode, "="-Fläche, vorderer Swap-Kreis, Theme-Symbol. */
    val accent: Color,
    /** Hilfstasten, Fußzeile, inaktiver Währungscode. */
    val secondary: Color,
    val filledAccent: Color,
    val swapBack: Color,
)

private val MinimalLight = MinimalPalette(
    appBackground = Color(0xFFF6F0EA),
    hairline = Color(0x245A321E),
    amount = Color(0xFF2E1B12),
    digit = Color(0xFF3A251A),
    accent = Color(0xFFA6401F),
    secondary = Color(0xFF8A6A5C),
    filledAccent = Color(0xFFA6401F),
    swapBack = Color(0x80C9927E),
)

private val MinimalDark = MinimalPalette(
    appBackground = Color(0xFF1B120E),
    hairline = Color(0x24FFDCC3),
    amount = Color(0xFFF8EFE8),
    digit = Color(0xFFEFDFD4),
    accent = Color(0xFFD97B52),
    secondary = Color(0xFF9C7A67),
    // Gefüllte Flächen nehmen im Dunkeln einen dunkleren Ton als der Akzent:
    // ein heller Akzent als große Fläche würde blenden.
    filledAccent = Color(0xFFB54B26),
    swapBack = Color(0x73A5735C),
)

private val OnAccent = Color(0xFFFFF5EE)

/**
 * Alles, was das Layout zum Zeichnen braucht — gebündelt, damit die einzelnen
 * Bausteine nicht ein Dutzend Parameter durchreichen müssen.
 */
data class ConverterLayoutData(
    val fromCurrency: String,
    val toCurrency: String,
    val currencies: List<String>,
    val onFromSelected: (String) -> Unit,
    val onToSelected: (String) -> Unit,
    val onSwap: () -> Unit,
    val fetchedAt: Long?,
    val onReload: () -> Unit,
    /** Wie viele [toCurrency] man für 1 [fromCurrency] bekommt. */
    val rate: Double?,
    val darkTheme: Boolean,
    val onToggleTheme: () -> Unit,
)

/**
 * Umrechnungskurs zwischen zwei Währungen, ausgehend von Kursen, die alle
 * relativ zur Basiswährung sind (so liefert die API sie).
 *
 * Zweistufig: erst von [from] auf die Basiswährung, dann auf [to] — genau wie
 * beim Geldwechsel über eine gemeinsame Referenzwährung.
 */
private fun rateBetween(from: String, to: String, state: ConverterUiState): Double? {
    fun rateOf(code: String): Double? =
        if (code == state.baseCurrency) 1.0 else state.rates[code]

    val fromRate = rateOf(from) ?: return null
    val toRate = rateOf(to) ?: return null
    return toRate / fromRate
}

/**
 * Einstiegspunkt der UI: Lade-, Fehler- und Erfolgszustand.
 *
 * Auch die beiden Sonderzustände nutzen die Terracotta-Palette dieses
 * Entwurfs, nicht `MaterialTheme.colorScheme` — sonst blitzte beim Laden
 * kurz das alte Teal-Schema auf.
 */
@Composable
fun ConverterScreen(
    uiState: ConverterUiState,
    onReload: () -> Unit,
    onFromSelected: (String) -> Unit,
    onToSelected: (String) -> Unit,
    onSwap: () -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = if (darkTheme) MinimalDark else MinimalLight

    when {
        uiState.isLoading -> Box(
            modifier = modifier
                .fillMaxSize()
                .background(palette.appBackground),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = palette.accent)
        }

        uiState.error != null -> Box(
            modifier = modifier
                .fillMaxSize()
                .background(palette.appBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(text = uiState.error, color = palette.amount, fontSize = 16.sp)
                // Heuristik 5: nicht nur melden, sondern einen Weg zurück
                // anbieten. Als gefüllte Pille — dieselbe Rolle wie die
                // "="-Taste: die eine Aktion, die man jetzt tun soll.
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(palette.filledAccent)
                        .clickable(onClick = onReload)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "Erneut versuchen",
                        color = OnAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        else -> ConverterLayout(
            data = ConverterLayoutData(
                fromCurrency = uiState.fromCurrency,
                toCurrency = uiState.toCurrency,
                currencies = remember(uiState.rates, uiState.baseCurrency) {
                    (uiState.rates.keys + uiState.baseCurrency).sorted()
                },
                onFromSelected = onFromSelected,
                onToSelected = onToSelected,
                onSwap = onSwap,
                fetchedAt = uiState.fetchedAt,
                onReload = onReload,
                rate = rateBetween(uiState.fromCurrency, uiState.toCurrency, uiState),
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
            ),
            modifier = modifier,
        )
    }
}

// ---------------------------------------------------------------------------
// Tastenfeld (v2 — andere Belegung als v1)
// ---------------------------------------------------------------------------

private enum class MinimalKeyKind { DIGIT, OPERATOR, UTILITY, EQUALS }

private data class MinimalKey(val label: String, val code: String, val kind: MinimalKeyKind)

/** Belegung laut Handoff: `%` oben, `⇅` in der letzten Zeile. */
private val MinimalKeyRows: List<List<MinimalKey>> = listOf(
    listOf(
        MinimalKey("C", "C", MinimalKeyKind.UTILITY),
        MinimalKey("⌫", "bs", MinimalKeyKind.UTILITY),
        MinimalKey("%", "%", MinimalKeyKind.UTILITY),
        MinimalKey("÷", "/", MinimalKeyKind.OPERATOR),
    ),
    listOf(
        MinimalKey("7", "7", MinimalKeyKind.DIGIT),
        MinimalKey("8", "8", MinimalKeyKind.DIGIT),
        MinimalKey("9", "9", MinimalKeyKind.DIGIT),
        MinimalKey("×", "*", MinimalKeyKind.OPERATOR),
    ),
    listOf(
        MinimalKey("4", "4", MinimalKeyKind.DIGIT),
        MinimalKey("5", "5", MinimalKeyKind.DIGIT),
        MinimalKey("6", "6", MinimalKeyKind.DIGIT),
        MinimalKey("−", "-", MinimalKeyKind.OPERATOR),
    ),
    listOf(
        MinimalKey("1", "1", MinimalKeyKind.DIGIT),
        MinimalKey("2", "2", MinimalKeyKind.DIGIT),
        MinimalKey("3", "3", MinimalKeyKind.DIGIT),
        MinimalKey("+", "+", MinimalKeyKind.OPERATOR),
    ),
    listOf(
        MinimalKey("0", "0", MinimalKeyKind.DIGIT),
        MinimalKey(",", ",", MinimalKeyKind.DIGIT),
        MinimalKey("⇅", "swap", MinimalKeyKind.UTILITY),
        MinimalKey("=", "=", MinimalKeyKind.EQUALS),
    ),
)

// ---------------------------------------------------------------------------
// Variante 4: minimal
// ---------------------------------------------------------------------------

/**
 * Umsetzung von `design/v2` — das gewählte Design.
 *
 * Keine Karten, keine Tastenflächen, keine Rahmen. Struktur entsteht nur durch Haarlinien, Weißraum und Farbe.
 *
 * Die drei Kernideen des Entwurfs:
 * - **Fokus durch Abdunkeln** statt durch einen Rahmen — die inaktive Zeile
 *   steht auf 45% Deckkraft. Das ist leiser als ein Ring und sagt trotzdem
 *   eindeutig, wo die Eingabe landet.
 * - **Genau eine gefüllte Fläche**, die "="-Taste. Alles andere schwebt, also
 *   zieht das Auge sofort dorthin.
 * - **Das App-Icon als Bedienelement**: die zwei überlappenden Kreise sitzen
 *   auf der Trennlinie und tauschen die Währungen.
 */
@Composable
private fun ConverterLayout(data: ConverterLayoutData, modifier: Modifier = Modifier) {
    val palette = if (data.darkTheme) MinimalDark else MinimalLight
    // Der Entwurf verlangt einen weichen Übergang beim Themenwechsel (~350ms).
    val background by animateColorAsState(
        targetValue = palette.appBackground,
        animationSpec = tween(350),
        label = "minimalBackground",
    )

    var calc by rememberSaveable(stateSaver = CalcStateSaver) { mutableStateOf(CalcState()) }

    val entered = parseEntry(calc.entry)
    val converted = data.rate?.let {
        formatAmount(if (calc.activeTop) entered * it else entered / it)
    } ?: "—"
    // Was beim Seitenwechsel übernommen wird. Ohne Kurs steht in der anderen
    // Zeile "—", und das wäre als Eingabe unbrauchbar.
    val carryOver = if (data.rate != null) converted else "0"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background),
    ) {
        CurrencySection(
            data = data,
            palette = palette,
            topAmount = if (calc.activeTop) calc.entry else converted,
            bottomAmount = if (calc.activeTop) converted else calc.entry,
            activeTop = calc.activeTop,
            // Abweichung vom Handoff ("resets entry to 0"): Der Wert, der in
            // der angetippten Zeile ohnehin schon steht, wird zur Eingabe.
            // Dadurch springt beim Seitenwechsel optisch nichts — es wechselt
            // nur, welche Zeile hell ist. Ein Zurücksetzen auf 0 würde eine
            // gerade eingetippte Rechnung wegwerfen.
            //
            // Eine angefangene Rechenoperation fällt trotzdem weg: Ein
            // "12 +" bezog sich auf die alte Währung und wäre nach dem
            // Wechsel sinnlos.
            onFocusTop = {
                if (!calc.activeTop) calc = CalcState(entry = carryOver, activeTop = true)
            },
            onFocusBottom = {
                if (calc.activeTop) calc = CalcState(entry = carryOver, activeTop = false)
            },
        )

        MinimalKeypad(
            palette = palette,
            onKey = { code ->
                if (code == "swap") data.onSwap() else calc = calc.onKey(code)
            },
            modifier = Modifier.weight(1f),
        )

        MinimalFooter(data = data, palette = palette)
    }
}

@Composable
private fun CurrencySection(
    data: ConverterLayoutData,
    palette: MinimalPalette,
    topAmount: String,
    bottomAmount: String,
    activeTop: Boolean,
    onFocusTop: () -> Unit,
    onFocusBottom: () -> Unit,
) {
    // Box statt Column, weil das Swap-Element über der Trennlinie *liegt*.
    // Beide Zeilen sind gleich hoch (34+30 oben, 30+34 unten), die Linie sitzt
    // also exakt in der Mitte — deshalb reicht CenterStart zum Positionieren.
    Box(modifier = Modifier.fillMaxWidth()) {
        Column {
            CurrencyRow(
                code = data.fromCurrency,
                amount = topAmount,
                active = activeTop,
                palette = palette,
                options = data.currencies,
                onSelect = data.onFromSelected,
                onFocus = onFocusTop,
                topPadding = 34.dp,
                bottomPadding = 30.dp,
            )
            Hairline(palette = palette, modifier = Modifier.padding(horizontal = 24.dp))
            CurrencyRow(
                code = data.toCurrency,
                amount = bottomAmount,
                active = !activeTop,
                palette = palette,
                options = data.currencies,
                onSelect = data.onToSelected,
                onFocus = onFocusBottom,
                topPadding = 30.dp,
                bottomPadding = 34.dp,
            )
        }

        SwapCircles(
            palette = palette,
            onSwap = data.onSwap,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp),
        )
    }
}

@Composable
private fun CurrencyRow(
    code: String,
    amount: String,
    active: Boolean,
    palette: MinimalPalette,
    options: List<String>,
    onSelect: (String) -> Unit,
    onFocus: () -> Unit,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    // Abweichung wie in Variante 3: Der Entwurf kennt keine Währungsauswahl,
    // nur Tauschen. Ein langer Druck auf den Code öffnet sie hier, damit der
    // kurze Druck weiter das Fokussieren bleibt.
    var pickerOpen by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Ohne indication: Eine Ripple über die volle Zeilenbreite wäre ein
            // riesiges Rechteck. Die Rückmeldung ist ohnehin das Aufhellen der
            // Zeile von 45% auf volle Deckkraft.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFocus,
            )
            // Deckkraft auf die *ganze* Zeile, nicht auf einzelne Texte —
            // sonst müsste man für jeden Farbwert eine gedimmte Variante pflegen.
            .alpha(if (active) 1f else 0.45f)
            .padding(start = 24.dp, end = 24.dp, top = topPadding, bottom = bottomPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        // alignByBaseline gehört an das direkte Kind der Row — die Box reicht
        // die Grundlinie ihres Textes nach außen weiter.
        Box(modifier = Modifier.alignByBaseline()) {
            Text(
                text = code,
                color = if (active) palette.accent else palette.secondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.14.em,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable { pickerOpen = true }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            DropdownMenu(expanded = pickerOpen, onDismissRequest = { pickerOpen = false }) {
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
            text = amount,
            color = palette.amount,
            style = TextStyle(
                fontSize = 56.sp,
                lineHeight = 56.sp * 1.15f,
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum",
                textAlign = TextAlign.End,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .alignByBaseline()
                .weight(1f)
                .padding(start = 16.dp),
        )
    }
}

@Composable
private fun Hairline(palette: MinimalPalette, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(palette.hairline),
    )
}

/**
 * Das Motiv aus dem App-Icon als Bedienelement: zwei überlappende Kreise.
 * Der hintere ist gedämpft und um 26dp nach rechts versetzt, der vordere
 * trägt den Akzent und das Tauschsymbol.
 */
@Composable
private fun SwapCircles(
    palette: MinimalPalette,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // Jeder Tausch dreht das Icon um eine weitere halbe Umdrehung. Der Wert
    // wächst also immer weiter — das ist Absicht: animateFloatAsState dreht
    // dadurch immer in dieselbe Richtung statt zwischen 0 und 180 zu pendeln.
    var halfTurns by remember { mutableStateOf(0) }
    val rotation by animateFloatAsState(
        targetValue = halfTurns * 180f,
        animationSpec = tween(320),
        label = "swapRotation",
    )

    // Wie bei der "="-Taste der Durchmesser statt eines Layer-Scales.
    val diameter by animateFloatAsState(
        targetValue = if (pressed) 45f else 48f,
        animationSpec = tween(if (pressed) 90 else 160),
        label = "swapDiameter",
    )

    Box(
        modifier = modifier
            .width(74.dp)
            .height(48.dp)
            // indication = null: Die Standard-Ripple füllt den rechteckigen
            // Rahmen dieser Box und leuchtet als Kasten hinter den Kreisen auf.
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    halfTurns++
                    onSwap()
                },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = 26.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(palette.swapBack),
        )
        Box(
            modifier = Modifier
                .size(diameter.dp)
                .clip(CircleShape)
                .background(palette.filledAccent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.SwapVert,
                contentDescription = "Währungen tauschen",
                tint = OnAccent,
                modifier = Modifier
                    .size(26.dp)
                    // Vektorgrafik: Drehen per graphicsLayer ist hier
                    // unbedenklich, anders als bei Text bleibt sie scharf.
                    .graphicsLayer { rotationZ = rotation },
            )
        }
    }
}

@Composable
private fun MinimalKeypad(
    palette: MinimalPalette,
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Hairline(palette = palette)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MinimalKeyRows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    row.forEach { key ->
                        MinimalKeyButton(
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
}

@Composable
private fun MinimalKeyButton(
    key: MinimalKey,
    palette: MinimalPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val isEquals = key.kind == MinimalKeyKind.EQUALS

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isEquals) EqualsKey(key, palette, pressed) else GlyphKey(key, palette, pressed)
    }
}

/**
 * Schwebende Taste ohne Fläche.
 *
 * Zwei bewusste Abweichungen von der Vorlage (dort: `scale(0.9)` +
 * `opacity 0.6` auf die ganze Taste):
 *
 * 1. **Kein `graphicsLayer`-Scale mehr.** Der rastert den Text einmal und
 *    staucht dann das Pixelbild — die Glyphe wird beim Drücken sichtbar
 *    unscharf. Stattdessen wandert die Schriftgröße selbst, dann zeichnet
 *    Compose die Schrift in jeder Zwischengröße neu und sie bleibt scharf.
 * 2. **Runder Schimmer statt Abdunkeln.** Die Trefferfläche ist eine breite
 *    rechteckige Zelle; passiert darin nur ein Schrumpfen, fühlt sich das
 *    Feedback rechteckig an. Ein Kreis unter dem Finger sagt "hier ist der
 *    Druckpunkt" und passt zur runden "="-Taste.
 */
@Composable
private fun GlyphKey(key: MinimalKey, palette: MinimalPalette, pressed: Boolean) {
    val (color, restingSize) = when (key.kind) {
        MinimalKeyKind.DIGIT -> palette.digit to 28f
        MinimalKeyKind.OPERATOR -> palette.accent to 30f
        else -> palette.secondary to if (key.code == "swap") 24f else 22f
    }

    val fontSize by animateFloatAsState(
        targetValue = if (pressed) restingSize * 0.92f else restingSize,
        animationSpec = tween(if (pressed) 90 else 160),
        label = "minimalKeyFontSize",
    )
    val scrimAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.14f else 0f,
        animationSpec = tween(if (pressed) 90 else 160),
        label = "minimalKeyScrim",
    )

    // Der Kreis ist so groß wie die Zelle hoch ist — dadurch passt er in
    // jede Zeilenhöhe, ohne dass wir eine feste dp-Zahl raten müssen.
    if (scrimAlpha > 0f) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(palette.accent.copy(alpha = scrimAlpha)),
        )
    }

    Text(
        text = key.label,
        color = color,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Medium,
    )
}

/**
 * Die einzige gefüllte Taste. Hier animiert der **Durchmesser** statt eines
 * Layer-Scales: Der Kreis ist eine gezeichnete Form und bleibt bei jeder
 * Größe scharf, und das "=" darin wird ebenfalls neu gesetzt statt gestaucht.
 */
@Composable
private fun EqualsKey(key: MinimalKey, palette: MinimalPalette, pressed: Boolean) {
    val diameter by animateFloatAsState(
        targetValue = if (pressed) 57f else 62f,
        animationSpec = tween(if (pressed) 90 else 160),
        label = "minimalEqualsSize",
    )

    Box(
        modifier = Modifier
            .size(diameter.dp)
            .clip(CircleShape)
            .background(palette.filledAccent),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = key.label,
            color = OnAccent,
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun MinimalFooter(data: ConverterLayoutData, palette: MinimalPalette) {
    Column {
        Hairline(palette = palette)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FooterGlyph(glyph = "↻", tint = palette.secondary, onClick = data.onReload)

            // Kurs und Zeitpunkt in *einer* Zeile, getrennt durch einen
            // Mittelpunkt — v1 stapelte beides noch übereinander.
            Text(
                text = buildString {
                    append(
                        data.rate
                            ?.let { "1 ${data.fromCurrency} = ${formatAmount(it)} ${data.toCurrency}" }
                            ?: "Kein Kurs verfügbar"
                    )
                    data.fetchedAt?.let { append(" · ${formatMinimalTimestamp(it)}") }
                },
                color = palette.secondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ThemeGlyph(
                    darkTheme = data.darkTheme,
                    tint = palette.accent,
                    onClick = data.onToggleTheme,
                )
                FooterGlyph(glyph = "⋯", tint = palette.secondary, onClick = {})
            }
        }
    }
}

/**
 * Kleines Fußzeilen-Symbol mit runder Trefferfläche. Ohne den Kreis-Clip
 * würde die Ripple als Rechteck um die Glyphe aufleuchten.
 */
@Composable
private fun FooterGlyph(glyph: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = glyph, color = tint, fontSize = 18.sp)
    }
}

/**
 * Mond bzw. Sonne als gezeichnete Formen — der Entwurf schreibt ausdrücklich
 * keine Icon-Schrift vor.
 *
 * Der Mond entsteht wie im Prototyp: ein Kreis minus derselbe Kreis, um
 * (-4.5, -3) versetzt. Übrig bleibt die Sichel. In CSS macht das ein
 * `inset box-shadow`, in Compose eine Pfad-Differenz.
 */
@Composable
private fun ThemeGlyph(darkTheme: Boolean, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (darkTheme) {
            // Im Dunkeln die Sonne: Ring plus Punkt.
            Box(
                modifier = Modifier.size(15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = tint,
                        radius = size.minDimension / 2 - 0.75.dp.toPx(),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.5.dp.toPx(),
                        ),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(tint),
                )
            }
        } else {
            Canvas(modifier = Modifier.size(13.dp)) {
                val full = Path().apply {
                    addOval(Rect(0f, 0f, size.width, size.height))
                }
                val cut = Path().apply {
                    addOval(
                        Rect(
                            -4.5.dp.toPx(),
                            -3.dp.toPx(),
                            size.width - 4.5.dp.toPx(),
                            size.height - 3.dp.toPx(),
                        )
                    )
                }
                val crescent = Path().apply { op(full, cut, PathOperation.Difference) }
                drawPath(crescent, tint)
            }
        }
    }
}

private val MinimalTimestampFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.GERMANY)

private fun formatMinimalTimestamp(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(MinimalTimestampFormatter)
