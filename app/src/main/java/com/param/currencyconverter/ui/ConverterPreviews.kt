package com.param.currencyconverter.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.param.currencyconverter.ConverterUiState
import com.param.currencyconverter.ui.theme.CurrencyConverterTheme

/**
 * Alle Previews ziehen sich denselben Zustand aus dieser einen Funktion.
 *
 * Der Punkt dabei: Zwei Layouts nebeneinander sind nur dann vergleichbar,
 * wenn sie exakt dieselben Daten zeigen. Läge in jeder Preview ein eigener
 * Datensatz, würden Unterschiede in der Textlänge wie Layout-Unterschiede
 * aussehen.
 */
private fun previewState() = ConverterUiState(
    baseCurrency = "EUR",
    rates = mapOf("USD" to 1.09, "GBP" to 0.85, "CHF" to 0.94, "THB" to 38.16),
    fromCurrency = "EUR",
    toCurrency = "THB",
    // Fest statt System.currentTimeMillis(): Ein wandernder Wert würde die
    // Preview bei jedem Rendern anders aussehen lassen.
    fetchedAt = 1_754_500_000_000L,
)

@Composable
private fun PreviewFrame(darkTheme: Boolean, content: @Composable () -> Unit) {
    CurrencyConverterTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}

@Composable
private fun VariantPreview(variant: LayoutVariant, darkTheme: Boolean) {
    PreviewFrame(darkTheme) {
        ConverterContent(
            variant = variant,
            uiState = previewState(),
            onReload = {},
            onFromSelected = {},
            onToSelected = {},
            onSwap = {},
            darkTheme = darkTheme,
        )
    }
}

// --- Variante 1: zwei Karten ---

@Preview(name = "Zwei Karten – hell", showBackground = true, heightDp = 620)
@Composable
private fun CardsLight() = VariantPreview(LayoutVariant.CARDS, darkTheme = false)

@Preview(name = "Zwei Karten – dunkel", showBackground = true, heightDp = 620)
@Composable
private fun CardsDark() = VariantPreview(LayoutVariant.CARDS, darkTheme = true)

// --- Variante 2: eine Karte ---

@Preview(name = "Eine Karte – hell", showBackground = true, heightDp = 620)
@Composable
private fun SingleCardLight() = VariantPreview(LayoutVariant.SINGLE_CARD, darkTheme = false)

@Preview(name = "Eine Karte – dunkel", showBackground = true, heightDp = 620)
@Composable
private fun SingleCardDark() = VariantPreview(LayoutVariant.SINGLE_CARD, darkTheme = true)

// --- Variante 3: Taschenrechner (Design-Vorlage) ---
// heightDp größer: Das Tastenfeld füllt die Resthöhe, in einer kurzen
// Preview wären die Tasten unrealistisch flach.

@Preview(name = "Taschenrechner – hell", showBackground = true, heightDp = 892, widthDp = 412)
@Composable
private fun CalculatorLight() = VariantPreview(LayoutVariant.CALCULATOR, darkTheme = false)

@Preview(name = "Taschenrechner – dunkel", showBackground = true, heightDp = 892, widthDp = 412)
@Composable
private fun CalculatorDark() = VariantPreview(LayoutVariant.CALCULATOR, darkTheme = true)

// --- Sonderzustände (variantenunabhängig) ---

@Preview(name = "Lädt", showBackground = true, heightDp = 320)
@Composable
private fun LoadingPreview() = PreviewFrame(darkTheme = false) {
    ConverterContent(
        variant = LayoutVariant.CARDS,
        uiState = ConverterUiState(isLoading = true),
        onReload = {},
        onFromSelected = {},
        onToSelected = {},
        onSwap = {},
        darkTheme = false,
    )
}

@Preview(name = "Fehler", showBackground = true, heightDp = 320)
@Composable
private fun ErrorPreview() = PreviewFrame(darkTheme = false) {
    ConverterContent(
        variant = LayoutVariant.CARDS,
        uiState = ConverterUiState(error = "Kurse konnten nicht geladen werden"),
        onReload = {},
        onFromSelected = {},
        onToSelected = {},
        onSwap = {},
        darkTheme = false,
    )
}

@Preview(name = "Veraltete Kurse", showBackground = true, heightDp = 620)
@Composable
private fun StalePreview() = PreviewFrame(darkTheme = false) {
    ConverterContent(
        variant = LayoutVariant.CARDS,
        uiState = previewState().copy(isStale = true),
        onReload = {},
        onFromSelected = {},
        onToSelected = {},
        onSwap = {},
        darkTheme = false,
    )
}
