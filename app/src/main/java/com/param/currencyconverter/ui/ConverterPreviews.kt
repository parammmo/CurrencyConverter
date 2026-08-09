package com.param.currencyconverter.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.param.currencyconverter.ConverterUiState
import com.param.currencyconverter.ui.theme.CurrencyConverterTheme

/**
 * Alle Previews ziehen sich denselben Zustand aus dieser einen Funktion —
 * unterschiedliche Daten würden Unterschiede vortäuschen, die es nicht gibt.
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
private fun PreviewScreen(state: ConverterUiState, darkTheme: Boolean) {
    CurrencyConverterTheme(darkTheme = darkTheme) {
        ConverterScreen(
            uiState = state,
            onReload = {},
            onFromSelected = {},
            onToSelected = {},
            onSwap = {},
            darkTheme = darkTheme,
            onToggleTheme = {},
        )
    }
}

// Referenzgröße des Handoffs: 412x892.

@Preview(name = "Hell", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ConverterLight() = PreviewScreen(previewState(), darkTheme = false)

@Preview(name = "Dunkel", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ConverterDark() = PreviewScreen(previewState(), darkTheme = true)

// --- Sonderzustände ---

@Preview(name = "Lädt", showBackground = true, widthDp = 412, heightDp = 400)
@Composable
private fun LoadingPreview() =
    PreviewScreen(ConverterUiState(isLoading = true), darkTheme = false)

@Preview(name = "Fehler", showBackground = true, widthDp = 412, heightDp = 400)
@Composable
private fun ErrorPreview() = PreviewScreen(
    ConverterUiState(error = "Kurse konnten nicht geladen werden"),
    darkTheme = false,
)

@Preview(name = "Fehler – dunkel", showBackground = true, widthDp = 412, heightDp = 400)
@Composable
private fun ErrorDarkPreview() = PreviewScreen(
    ConverterUiState(error = "Kurse konnten nicht geladen werden"),
    darkTheme = true,
)
