package com.param.currencyconverter.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.param.currencyconverter.ConverterUiState
import com.param.currencyconverter.R
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Alles, was das Layout zum Zeichnen braucht — gebündelt, damit die einzelnen
 * Bausteine nicht ein Dutzend Parameter durchreichen müssen.
 */
data class ConverterLayoutData(
    val fromCurrency: String,
    val toCurrency: String,
    val currencies: List<String>,
    /** Zuletzt gewählte Codes, neueste zuerst — die Kurzliste im Sheet. */
    val recentCurrencies: List<String>,
    val onFromSelected: (String) -> Unit,
    val onToSelected: (String) -> Unit,
    val onSwap: () -> Unit,
    /** Unix-Millis des letzten Netzabrufs. */
    val fetchedAt: Long?,
    /** Kurse stammen aus einem abgelaufenen Cache, weil das Netz nicht ging. */
    val isStale: Boolean,
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
    val colors = MaterialTheme.colorScheme

    when {
        uiState.isLoading -> Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = colors.primary)
        }

        uiState.hasError -> Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    text = stringResource(R.string.error_rates_load_failed),
                    color = colors.onBackground,
                    fontSize = 16.sp,
                )
                // Heuristik 5: nicht nur melden, sondern einen Weg zurück
                // anbieten. Als gefüllte Pille — dieselbe Rolle wie die
                // "="-Taste: die eine Aktion, die man jetzt tun soll.
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(colors.primaryContainer)
                        .clickable(onClick = onReload)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.action_try_again),
                        color = colors.onPrimaryContainer,
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
                recentCurrencies = uiState.recentCurrencies,
                onFromSelected = onFromSelected,
                onToSelected = onToSelected,
                onSwap = onSwap,
                fetchedAt = uiState.fetchedAt,
                isStale = uiState.isStale,
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

/** DIGIT_WIDE: verhält sich wie DIGIT, wird nur kleiner gesetzt ("000"). */
private enum class MinimalKeyKind { DIGIT, DIGIT_WIDE, OPERATOR, UTILITY, EQUALS }

private data class MinimalKey(val label: String, val code: String, val kind: MinimalKeyKind)

/**
 * Belegung laut Handoff, mit einer Abweichung: Statt eines zweiten
 * Tausch-Knopfes in der letzten Zeile steht dort "000". Das Tauschen erledigen
 * schon die beiden Kreise oben, und bei Währungen mit großen Zahlen (IDR, KRW)
 * spart die Taste jede Menge Tipperei. Dadurch stehen 0, 000 und Komma
 * nebeneinander — die drei Tasten, die Nachkommastellen betreffen.
 */
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
        MinimalKey("000", "000", MinimalKeyKind.DIGIT_WIDE),
        MinimalKey(",", ",", MinimalKeyKind.DIGIT),
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
    val colors = MaterialTheme.colorScheme
    // Der Entwurf verlangt einen weichen Übergang beim Themenwechsel (~350ms).
    val background by animateColorAsState(
        targetValue = colors.background,
        animationSpec = tween(350),
        label = "minimalBackground",
    )

    var calc by rememberSaveable(stateSaver = CalcStateSaver) { mutableStateOf(CalcState()) }

    val entered = parseEntry(calc.entry)
    // formatMoney statt formatAmount: Das hier ist ein Geldbetrag, kein
    // Rechenergebnis — zwei Nachkommastellen reichen. Der Kurs in der
    // Fußzeile bleibt bewusst bei formatAmount, siehe dort.
    val converted = data.rate?.let {
        formatMoney(if (calc.activeTop) entered * it else entered / it)
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
            colors = colors,
            topAmount = (if (calc.activeTop) calc.entry else converted).withDecimalSeparator(),
            bottomAmount = (if (calc.activeTop) converted else calc.entry).withDecimalSeparator(),
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
            //
            // freshEntry = true, weil der übernommene Wert ein *Ergebnis* ist:
            // Die erste getippte Ziffer beginnt eine neue Eingabe, statt sich
            // hinten anzuhängen. Seit die Beträge auf zwei Nachkommastellen
            // begrenzt sind, wäre das Anhängen sonst sogar wirkungslos —
            // "86,00" hat hinterm Komma keinen Platz mehr. Löschen mit ⌫
            // funktioniert weiterhin.
            onFocusTop = {
                if (!calc.activeTop) {
                    calc = CalcState(entry = carryOver, activeTop = true, freshEntry = true)
                }
            },
            onFocusBottom = {
                if (calc.activeTop) {
                    calc = CalcState(entry = carryOver, activeTop = false, freshEntry = true)
                }
            },
        )

        MinimalKeypad(
            colors = colors,
            onKey = { code -> calc = calc.onKey(code) },
            modifier = Modifier.weight(1f),
        )

        MinimalFooter(data = data, colors = colors)
    }
}

@Composable
private fun CurrencySection(
    data: ConverterLayoutData,
    colors: ColorScheme,
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
                colors = colors,
                options = data.currencies,
                recents = data.recentCurrencies,
                onSelect = data.onFromSelected,
                onFocus = onFocusTop,
                topPadding = 34.dp,
                bottomPadding = 30.dp,
            )
            Hairline(colors = colors, modifier = Modifier.padding(horizontal = 24.dp))
            CurrencyRow(
                code = data.toCurrency,
                amount = bottomAmount,
                active = !activeTop,
                colors = colors,
                options = data.currencies,
                recents = data.recentCurrencies,
                onSelect = data.onToSelected,
                onFocus = onFocusBottom,
                topPadding = 30.dp,
                bottomPadding = 34.dp,
            )
        }

        SwapCircles(
            colors = colors,
            onSwap = data.onSwap,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp),
        )
    }
}

/**
 * Die Ausgangsgröße des Betrags — der Wert aus dem Design-Handoff.
 */
private val MAX_AMOUNT_SP = 56.sp

/**
 * Wie klein der Betrag höchstens werden darf.
 *
 * Der Betrag ist das Wichtigste auf dem Bildschirm (siehe `ux-guide.md`,
 * "typografische Hierarchie"). Halbiert ist er immer noch die mit Abstand
 * größte Schrift der App — die Tasten sind 30sp. Ginge er tiefer, würde ein
 * langer Betrag optisch hinter das Tastenfeld zurückfallen.
 */
private val MIN_AMOUNT_SP = 28.sp

@Composable
private fun CurrencyRow(
    code: String,
    amount: String,
    active: Boolean,
    colors: ColorScheme,
    options: List<String>,
    recents: List<String>,
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
        // die Grundlinie ihres Inhalts nach außen weiter.
        Box(modifier = Modifier.alignByBaseline()) {
            // Flagge und Code teilen sich eine Trefferfläche: Erst dadurch ist
            // erkennbar, dass hier überhaupt etwas auszuwählen ist — der nackte
            // Code sah nach Beschriftung aus, nicht nach Knopf.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable(
                        onClickLabel = stringResource(R.string.cd_select_currency, code),
                    ) { pickerOpen = true }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CurrencyFlag(code)
                Text(
                    text = code,
                    color = if (active) colors.primary else colors.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.14.em,
                )
            }
            // Kein DropdownMenu mehr: Seit die Liste 166 Einträge hat, ist
            // Scrollen keine Auswahl. Das Sheet bringt Suche und Verlauf mit,
            // siehe [CurrencyPickerSheet].
            if (pickerOpen) {
                CurrencyPickerSheet(
                    codes = options,
                    recents = recents,
                    selected = code,
                    onSelect = {
                        onSelect(it)
                        pickerOpen = false
                    },
                    onDismiss = { pickerOpen = false },
                )
            }
        }

        Text(
            text = amount,
            color = colors.onBackground,
            // Statt große Beträge abzuschneiden ("1234…"), wird die Schrift
            // kleiner, bis der Betrag passt. Ein abgeschnittener Geldbetrag
            // ist keine Information mehr — man weiß nicht mal die
            // Größenordnung. Erst unterhalb von [MIN_AMOUNT_SP] greift die
            // Ellipse doch noch, als letzte Rettung.
            //
            // StepBased probiert Schriftgrößen in Schritten durch, bis eine
            // in die Zeile passt. 2sp-Schritte: fein genug, dass man die
            // Stufen nicht sieht, grob genug, dass es beim Tippen nicht bei
            // jeder Ziffer minimal zappelt.
            autoSize = TextAutoSize.StepBased(
                minFontSize = MIN_AMOUNT_SP,
                maxFontSize = MAX_AMOUNT_SP,
                stepSize = 2.sp,
            ),
            style = TextStyle(
                // Kein fontSize mehr — das bestimmt jetzt autoSize. Die
                // lineHeight bleibt aber *fest*: Sie hält die Zeilenhöhe
                // konstant, egal wie klein die Ziffern werden. Ohne das
                // würde die Trennlinie samt Tausch-Kreisen nach oben
                // wandern, sobald ein langer Betrag die Schrift schrumpfen
                // lässt — die beiden Zeilen sind über die Grundlinie
                // aneinander ausgerichtet.
                lineHeight = MAX_AMOUNT_SP * 1.15f,
                // Der eigentliche Trick, damit die lineHeight oben auch hält:
                // Compose trimmt bei einer einzelnen Zeile standardmäßig den
                // Zeilenabstand über und unter dem Text weg (Trim.Both). Dann
                // ist die Textbox wieder nur so hoch wie die Schrift selbst —
                // und schrumpft eben doch mit. Trim.None behält die volle
                // Zeilenhöhe, egal wie klein die Ziffern werden.
                //
                // Ohne das wandert das komplette Tastenfeld nach oben, sobald
                // ein langer Betrag die Schrift verkleinert.
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
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

/**
 * Flagge zum Währungscode — oder nichts, wenn es für den Code keine gibt.
 *
 * Eigenes Text-Element ohne `letterSpacing`: Ein Flaggen-Emoji ist eine Ligatur
 * aus zwei Zeichen, und Buchstabenabstand dazwischen zerlegt sie wieder in ihre
 * Einzelteile — statt der Flagge stünde dann "U S" da.
 *
 * Etwas größer gesetzt als der Code daneben, weil Emoji ihre Zeichenfläche
 * anders ausnutzen als Buchstaben und bei gleicher sp-Zahl kleiner wirken.
 */
@Composable
// internal statt private: Das Auswahl-Sheet zeigt dieselbe Flagge, und zwei
// Fassungen derselben Sache laufen früher oder später auseinander.
internal fun CurrencyFlag(code: String) {
    val flag = flagEmoji(code) ?: return
    Text(
        text = flag,
        fontSize = 16.sp,
        // Für den Screenreader unsichtbar: Welche Währung gemeint ist, sagen
        // der Code und das Klick-Label bereits. Die Flagge würde nur ein
        // zweites Mal "Vereinigte Staaten" danebensetzen.
        modifier = Modifier.clearAndSetSemantics {},
    )
}

@Composable
private fun Hairline(colors: ColorScheme, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.outlineVariant),
    )
}

/**
 * Das Motiv aus dem App-Icon als Bedienelement: zwei überlappende Kreise.
 * Der hintere ist gedämpft und um 26dp nach rechts versetzt, der vordere
 * trägt den Akzent und das Tauschsymbol.
 */
@Composable
private fun SwapCircles(
    colors: ColorScheme,
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
                .background(colors.secondaryContainer),
        )
        Box(
            modifier = Modifier
                .size(diameter.dp)
                .clip(CircleShape)
                .background(colors.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.SwapVert,
                contentDescription = stringResource(R.string.cd_swap_currencies),
                tint = colors.onPrimaryContainer,
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
    colors: ColorScheme,
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Hairline(colors = colors)
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
                            colors = colors,
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
    colors: ColorScheme,
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
        if (isEquals) EqualsKey(key, colors, pressed) else GlyphKey(key, colors, pressed)
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
private fun GlyphKey(key: MinimalKey, colors: ColorScheme, pressed: Boolean) {
    val (color, restingSize) = when (key.kind) {
        MinimalKeyKind.DIGIT -> colors.onSurface to 28f
        MinimalKeyKind.OPERATOR -> colors.primary to 30f
        // "000" ist dreimal so breit wie eine einzelne Ziffer — etwas kleiner
        // gesetzt, damit die Taste nicht optisch aus der Reihe fällt.
        MinimalKeyKind.DIGIT_WIDE -> colors.onSurface to 24f
        else -> colors.onSurfaceVariant to 22f
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
                .background(colors.primary.copy(alpha = scrimAlpha)),
        )
    }

    Text(
        // Die Beschriftung der Trenner-Taste folgt der Gerätesprache, der
        // interne Code bleibt ",".
        text = if (key.code == ",") key.label.withDecimalSeparator() else key.label,
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
private fun EqualsKey(key: MinimalKey, colors: ColorScheme, pressed: Boolean) {
    val diameter by animateFloatAsState(
        targetValue = if (pressed) 57f else 62f,
        animationSpec = tween(if (pressed) 90 else 160),
        label = "minimalEqualsSize",
    )

    Box(
        modifier = Modifier
            .size(diameter.dp)
            .clip(CircleShape)
            .background(colors.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = key.label,
            color = colors.onPrimaryContainer,
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * Ziel der Namensnennung in der Fußzeile — die Nutzungsbedingungen der
 * Open-Access-API verlangen einen Verweis auf den Anbieter.
 */
private const val ATTRIBUTION_URL = "https://www.exchangerate-api.com"

@Composable
private fun MinimalFooter(data: ConverterLayoutData, colors: ColorScheme) {
    Column {
        Hairline(colors = colors)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FooterGlyph(
                glyph = "↻",
                description = stringResource(R.string.cd_refresh_rates),
                tint = colors.onSurfaceVariant,
                onClick = data.onReload,
            )

            // Kurs oben, Abrufzeitpunkt darunter — zwei verschiedene
            // Aussagen ("wie viel bekomme ich" und "wie alt ist die Auskunft"),
            // die nebeneinander zu einer langen Zeile verschmolzen sind.
            //
            // Sind die Kurse veraltet, kommt "Offline" vor den Zeitpunkt (nicht
            // vor den Kurs — veraltet ist die Auskunft, nicht die Rechnung) und
            // beide Zeilen wechseln auf die Akzentfarbe. Bewusst kein eigenes
            // Rot: Der Entwurf kennt keinen Fehlerzustand, und ein zweiter
            // Signalton würde die Palette aufweichen.
            val footerColor = if (data.isStale) colors.primary else colors.onSurfaceVariant
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = data.rate?.let {
                        stringResource(
                            R.string.rate_line,
                            data.fromCurrency,
                            // Absichtlich formatAmount, nicht formatMoney:
                            // Ein Kurs ist kein Betrag. "1 EUR = 0,01 IDR"
                            // wäre auf zwei Stellen gerundet schlicht falsch.
                            formatAmount(it).withDecimalSeparator(),
                            data.toCurrency,
                        )
                    } ?: stringResource(R.string.rate_unavailable),
                    color = footerColor,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = ratesAgeText(fetchedAt = data.fetchedAt, isStale = data.isStale),
                    color = footerColor,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Pflichtangabe der Kursquelle (siehe [ExchangeRateApi]).
                // Bewusst die leiseste Zeile im ganzen Bild: 10sp und
                // zusätzlich abgedunkelt, und *nicht* an `footerColor`
                // gekoppelt — dass die Kurse veraltet sind, sagen schon die
                // beiden Zeilen darüber; die Quelle wechselt dabei nicht.
                // LocalUriHandler ist Compose' Weg, den Browser zu öffnen,
                // ohne selbst einen Intent zu bauen.
                val uriHandler = LocalUriHandler.current
                Text(
                    text = stringResource(R.string.attribution),
                    color = colors.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .alpha(0.6f)
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable { uriHandler.openUri(ATTRIBUTION_URL) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            // Der Entwurf hat hier noch ein Überlauf-Menü, legt aber nicht
            // fest, was drinsteht — ohne Inhalt wäre es ein Knopf, der nichts
            // tut. Nebeneffekt: Links und rechts sind jetzt beide 36dp breit,
            // dadurch sitzt die Mitte wirklich in der Mitte.
            ThemeGlyph(
                darkTheme = data.darkTheme,
                tint = colors.primary,
                onClick = data.onToggleTheme,
            )
        }
    }
}

/**
 * Kleines Fußzeilen-Symbol mit runder Trefferfläche. Ohne den Kreis-Clip
 * würde die Ripple als Rechteck um die Glyphe aufleuchten.
 */
@Composable
private fun FooterGlyph(
    glyph: String,
    description: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClickLabel = description, onClick = onClick)
            // Das Symbol ist ein Textzeichen — ein Screenreader würde sonst
            // dessen Unicode-Namen vorlesen. clearAndSetSemantics ersetzt den
            // Inhalt durch die Beschreibung.
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = glyph, color = tint, fontSize = 18.sp)
    }
}

/**
 * Mond bzw. Sonne für den Theme-Umschalter.
 *
 * Abweichung vom Handoff, der hier ausdrücklich gezeichnete Formen verlangt
 * (Kreis mit Sichel bzw. Ring mit Punkt). Die Material-Symbole sind als
 * Sonne und Mond schlicht besser lesbar, und sie passen zu den übrigen
 * Fußzeilen-Symbolen statt danebenzustehen.
 *
 * Das Icon zeigt das *Ziel*, nicht den Ist-Zustand: im Dunkeln die Sonne,
 * also "hier geht's nach hell".
 */
@Composable
private fun ThemeGlyph(darkTheme: Boolean, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            // 36dp wie die übrigen Fußzeilen-Symbole. Der Handoff sieht hier
            // nur 22dp vor — das ist als Tippziel zu klein.
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
            contentDescription = stringResource(
                if (darkTheme) R.string.cd_switch_to_light else R.string.cd_switch_to_dark
            ),
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Wie aktuell die angezeigten Kurse sind.
 *
 * Solange der Cache innerhalb der TTL liegt, gilt alles als frisch — dann
 * steht dort "Just now". Das ist bewusst keine Uhrzeitangabe: Die TTL *ist*
 * unsere Definition von "aktuell", und ein exakter Zeitstempel würde nur
 * unsere Cache-Mechanik verraten statt die Frage zu beantworten.
 *
 * Erst wenn das Repository auf einen abgelaufenen Cache zurückfällt, weil das
 * Netz nicht erreichbar war ([isStale]), wird das echte Alter ausgerechnet.
 */
@Composable
private fun ratesAgeText(fetchedAt: Long?, isStale: Boolean): String {
    if (!isStale || fetchedAt == null) return stringResource(R.string.rates_just_now)

    // Mindestens 1, damit nie "vor 0 Stunden" dasteht.
    val hours = ((System.currentTimeMillis() - fetchedAt) / 3_600_000L)
        .coerceAtLeast(1L)
        .toInt()
    val age = if (hours < 24) {
        pluralStringResource(R.plurals.rates_age_hours, hours, hours)
    } else {
        val days = hours / 24
        pluralStringResource(R.plurals.rates_age_days, days, days)
    }
    return stringResource(R.string.rates_offline, age)
}

/**
 * Dezimaltrenner für die *Anzeige*.
 *
 * [CalculatorCore] rechnet intern immer mit Komma — ein fester Trenner hält
 * die Logik frei von Locale-Fragen. Erst hier wird daraus das, was die
 * Gerätesprache erwartet: auf einem englischen Gerät ein Punkt.
 */
private fun String.withDecimalSeparator(): String {
    val separator = DecimalFormatSymbols.getInstance().decimalSeparator
    return if (separator == ',') this else replace(',', separator)
}
