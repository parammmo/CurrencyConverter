package com.param.currencyconverter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.param.currencyconverter.ConverterUiState
import com.param.currencyconverter.ui.theme.Corner
import com.param.currencyconverter.ui.theme.Elevation
import com.param.currencyconverter.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Die verfügbaren Layout-Entwürfe.
 *
 * Zweck ist das Ausprobieren: Alle Varianten zeigen dieselben Daten und
 * bekommen dieselben Callbacks, unterscheiden sich also *nur* in der
 * Anordnung. Am Ende bleibt eine übrig, der Rest wird gelöscht.
 */
enum class LayoutVariant(val label: String) {
    CARDS("Zwei Karten"),
    SINGLE_CARD("Eine Karte"),
    CALCULATOR("Taschenrechner"),
    MINIMAL("Minimal"),
}

/**
 * Alles, was ein Layout zum Zeichnen braucht — gebündelt, damit eine neue
 * Variante nicht zwölf Parameter einzeln durchreichen muss.
 *
 * Kleiner Preis: Eine data class mit Lambdas gilt für Compose als "unstable",
 * die Layouts recomposen also öfter als nötig. Bei dieser Screengröße nicht
 * messbar, und für einen Entwurfsplatz ist Lesbarkeit wichtiger.
 */
data class ConverterLayoutData(
    val amountText: String,
    val onAmountChange: (String) -> Unit,
    val fromCurrency: String,
    val toCurrency: String,
    val currencies: List<String>,
    val resultText: String,
    val onFromSelected: (String) -> Unit,
    val onToSelected: (String) -> Unit,
    val onSwap: () -> Unit,
    val fetchedAt: Long?,
    val isStale: Boolean,
    val onReload: () -> Unit,
    /** Wie viele [toCurrency] man für 1 [fromCurrency] bekommt. */
    val rate: Double?,
    /**
     * Nur für Varianten, die eine eigene Palette mitbringen statt
     * `MaterialTheme.colorScheme` zu benutzen — siehe [CalculatorLayout].
     */
    val darkTheme: Boolean,
    /** Nur die Minimal-Variante hat den Theme-Umschalter im Screen selbst. */
    val onToggleTheme: () -> Unit,
)

/**
 * Ziffern, dazu höchstens ein Dezimaltrenner mit bis zu zwei Nachkommastellen.
 *
 * Der leere String muss erlaubt bleiben — sonst kann man das Feld nicht mehr
 * komplett leeren, weil das Löschen des letzten Zeichens abgelehnt würde.
 */
private val AMOUNT_PATTERN = Regex("""^\d*([.,]\d{0,2})?$""")

/**
 * Rechnet [amount] von [from] nach [to] um, ausgehend von Kursen, die alle
 * relativ zu [ConverterUiState.baseCurrency] sind (so liefert die API sie).
 *
 * Zweistufig gedacht: erst von [from] auf die Basiswährung umrechnen, dann
 * von der Basiswährung auf [to] – genau wie beim Geldwechsel über eine
 * gemeinsame Referenzwährung.
 */
private fun convert(amount: Double, from: String, to: String, state: ConverterUiState): Double? {
    fun rateOf(code: String): Double? =
        if (code == state.baseCurrency) 1.0 else state.rates[code]

    val fromRate = rateOf(from) ?: return null
    val toRate = rateOf(to) ?: return null

    val amountInBase = amount / fromRate
    return amountInBase * toRate
}

/**
 * Einstiegspunkt der UI. Lade- und Fehlerzustand sind für alle Varianten
 * gleich und werden hier einmal erledigt — die Varianten kümmern sich nur um
 * den Erfolgsfall.
 */
@Composable
fun ConverterContent(
    variant: LayoutVariant,
    uiState: ConverterUiState,
    onReload: () -> Unit,
    onFromSelected: (String) -> Unit,
    onToSelected: (String) -> Unit,
    onSwap: () -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var amountText by rememberSaveable { mutableStateOf("1") }

    // Verfügbare Codes: Basiswährung + alles, was wir an Kursen haben.
    val availableCurrencies = remember(uiState.rates, uiState.baseCurrency) {
        (uiState.rates.keys + uiState.baseCurrency).sorted()
    }

    // Lade- und Fehlerzustand füllen den Screen und zentrieren; der Inhalt
    // dagegen beginnt oben. Ein Formular, das vertikal in der Mitte schwebt,
    // wirkt zufällig platziert.
    when {
        uiState.isLoading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        uiState.error != null -> Box(
            modifier = modifier
                .fillMaxSize()
                .padding(Spacing.medium),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                Text(uiState.error, style = MaterialTheme.typography.bodyLarge)
                // Heuristik 5: nicht nur melden, sondern auch einen Weg
                // zurück anbieten — direkt neben dem Fehler, nicht irgendwo.
                Button(onClick = onReload) { Text("Erneut versuchen") }
            }
        }

        else -> {
            val amount = amountText.replace(",", ".").toDoubleOrNull()
            val result = amount?.let {
                convert(it, uiState.fromCurrency, uiState.toCurrency, uiState)
            }
            val data = ConverterLayoutData(
                amountText = amountText,
                onAmountChange = { input ->
                    if (AMOUNT_PATTERN.matches(input)) amountText = input
                },
                fromCurrency = uiState.fromCurrency,
                toCurrency = uiState.toCurrency,
                currencies = availableCurrencies,
                resultText = result
                    ?.let { String.format(Locale.GERMANY, "%.2f %s", it, uiState.toCurrency) }
                    ?: "—",
                onFromSelected = onFromSelected,
                onToSelected = onToSelected,
                onSwap = onSwap,
                fetchedAt = uiState.fetchedAt,
                isStale = uiState.isStale,
                onReload = onReload,
                rate = convert(1.0, uiState.fromCurrency, uiState.toCurrency, uiState),
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
            )

            when (variant) {
                LayoutVariant.CARDS -> CardsLayout(data, modifier)
                LayoutVariant.SINGLE_CARD -> SingleCardLayout(data, modifier)
                LayoutVariant.CALCULATOR -> CalculatorLayout(data, modifier)
                LayoutVariant.MINIMAL -> MinimalCalculatorLayout(data, modifier)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Variante 1: zwei Karten
// ---------------------------------------------------------------------------

/**
 * Eingabe und Ergebnis in getrennten Karten, Swap dazwischen.
 *
 * Die Trennung betont, dass es zwei Seiten gibt — links was du hast, rechts
 * was du bekommst.
 */
@Composable
fun CardsLayout(data: ConverterLayoutData, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        // Eingabe → tauschen → Ergebnis ist eine Einheit. Enger Abstand innen,
        // großer nach außen — der Abstand zeigt, was zusammengehört.
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            ConverterCard {
                AmountField(data.amountText, data.onAmountChange)
                CurrencyDropdown(
                    selected = data.fromCurrency,
                    options = data.currencies,
                    onSelect = data.onFromSelected,
                    label = "Von",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SwapButton(
                onSwap = data.onSwap,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            ConverterCard {
                CurrencyDropdown(
                    selected = data.toCurrency,
                    options = data.currencies,
                    onSelect = data.onToSelected,
                    label = "Nach",
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = data.resultText,
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
        }

        RatesFooter(data)
    }
}

// ---------------------------------------------------------------------------
// Variante 2: eine Karte
// ---------------------------------------------------------------------------

/**
 * Dieselben Elemente, aber in *einer* Karte mit Trennlinie in der Mitte.
 *
 * Der Unterschied ist bewusst klein: So sieht man im Vergleich, ob die
 * Trennung in zwei Karten wirklich etwas beiträgt oder nur Fläche kostet.
 * Der Swap-Button sitzt hier auf der Linie und unterbricht sie — er wird
 * damit zum Gelenk zwischen den Hälften statt zu einem Element dazwischen.
 */
@Composable
fun SingleCardLayout(data: ConverterLayoutData, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Corner.card),
            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                AmountField(data.amountText, data.onAmountChange)
                CurrencyDropdown(
                    selected = data.fromCurrency,
                    options = data.currencies,
                    onSelect = data.onFromSelected,
                    label = "Von",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                HorizontalDivider()
                // Eigene Fläche in Kartenfarbe, damit der Button die Linie
                // sauber unterbricht statt darauf zu liegen.
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    SwapButton(onSwap = data.onSwap)
                }
            }

            Column(
                modifier = Modifier.padding(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                CurrencyDropdown(
                    selected = data.toCurrency,
                    options = data.currencies,
                    onSelect = data.onToSelected,
                    label = "Nach",
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = data.resultText,
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
        }

        RatesFooter(data)
    }
}

// ---------------------------------------------------------------------------
// Gemeinsame Bausteine
// ---------------------------------------------------------------------------

/** Karte mit den Tokens aus Etappe 1 — damit keine Variante eigene Werte erfindet. */
@Composable
private fun ConverterCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Corner.card),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
        // In M3 trägt Farbe die Abhebung, nicht der Schatten: 2dp Elevation
        // allein sieht man kaum, surfaceContainer dagegen sofort.
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            content = content,
        )
    }
}

@Composable
private fun AmountField(
    amountText: String,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = amountText,
        onValueChange = onAmountChange,
        label = { Text("Betrag") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun SwapButton(onSwap: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onSwap, modifier = modifier) {
        Icon(
            imageVector = Icons.Default.SwapVert,
            // Kein Deko-Icon: Screenreader liest diesen Text vor.
            contentDescription = "Währungen tauschen",
        )
    }
}

/**
 * Zeitstempel und "Neu laden" gehören inhaltlich zusammen — beide beantworten
 * "wie aktuell sind diese Kurse". Deshalb in einer Zeile.
 */
@Composable
private fun RatesFooter(data: ConverterLayoutData, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RatesStatus(fetchedAt = data.fetchedAt, isStale = data.isStale)
        // TextButton statt Button: Neu laden ist eine Nebenaktion, die
        // gefüllte Primärfarbe gehört nicht hierher.
        TextButton(onClick = data.onReload) { Text("Neu laden") }
    }
}

/**
 * Meta-Zeile: wann wurden diese Kurse geholt.
 *
 * Beantwortet die Frage "sehe ich gerade was Aktuelles?", ohne dass der Nutzer
 * auf "Neu laden" tippen muss, um es herauszufinden.
 */
@Composable
fun RatesStatus(fetchedAt: Long?, isStale: Boolean, modifier: Modifier = Modifier) {
    if (fetchedAt == null) return

    // remember, damit die Formatierung nicht bei jedem Tastendruck im
    // Betragsfeld neu läuft — der Zeitstempel ändert sich ja nur beim Laden.
    val text = remember(fetchedAt, isStale) {
        val prefix = if (isStale) "Offline – Kurse von " else "Kurse von "
        prefix + formatFetchedAt(fetchedAt)
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        // Die eine Akzentfarbe für Status; sonst gedämpftes Grau, weil das
        // hier Nebeninformation ist und nicht mit dem Ergebnis konkurrieren soll.
        color = if (isStale) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier,
    )
}

/**
 * Uhrzeit, wenn der Abruf von heute ist — sonst zusätzlich das Datum. Ein
 * bloßes "14:32" wäre irreführend, wenn es von vorgestern stammt.
 */
private fun formatFetchedAt(fetchedAt: Long): String {
    val zone = ZoneId.systemDefault()
    val moment = Instant.ofEpochMilli(fetchedAt).atZone(zone)
    val time = moment.format(TIME_FORMATTER)

    return if (moment.toLocalDate() == LocalDate.now(zone)) {
        "$time Uhr"
    } else {
        "${moment.format(DATE_FORMATTER)}, $time Uhr"
    }
}

// Feste deutsche Formate statt ofLocalizedTime/-Date: Die Oberfläche ist
// durchgehend deutsch, also darf die Uhrzeit nicht plötzlich "3:13 PM"
// heißen, nur weil das Gerät auf US steht.
private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY)
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d. MMMM", Locale.GERMANY)

/**
 * Auswahlfeld für einen Währungscode.
 *
 * Sieht aus wie ein Textfeld (mit Label und Pfeil-Icon) und signalisiert damit
 * von selbst "hier gibt's was auszuwählen".
 *
 * `readOnly = true`: Tippen ist gesperrt, der Klick öffnet trotzdem das Menü.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyDropdown(
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            // menuAnchor verankert das Menü am Feld — ohne das öffnet es an
            // der falschen Stelle.
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { code ->
                DropdownMenuItem(
                    text = { Text(code) },
                    onClick = {
                        onSelect(code)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
