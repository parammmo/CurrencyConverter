package com.param.currencyconverter

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.param.currencyconverter.data.ExchangeRateRepository
import com.param.currencyconverter.data.ThemeMode
import com.param.currencyconverter.data.UserPreferencesRepository
import com.param.currencyconverter.data.remote.NetworkModule
import com.param.currencyconverter.ui.ConverterScreen
import com.param.currencyconverter.ui.theme.CurrencyConverterTheme
import kotlinx.browser.document

/**
 * Das Web-Pendant zu `MainActivity.onCreate` + `setContent`: [ComposeViewport]
 * hängt ein <canvas> in das übergebene DOM-Element und rendert die
 * Composables dort hinein.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.getElementById("app")!!) {
        CurrencyConverterApp()
    }
}

/**
 * 1:1 aus MainActivity.kt übernommen, bis auf einen Unterschied: Die
 * Repositories brauchen keinen Context mehr — localStorage ist global,
 * es gibt nichts, was man ihnen reichen müsste.
 */
@Composable
fun CurrencyConverterApp() {
    val viewModel: CurrencyViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CurrencyViewModel(
                    ExchangeRateRepository(NetworkModule.exchangeRateApi),
                    UserPreferencesRepository(),
                )
            }
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val darkTheme = when (uiState.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    CurrencyConverterTheme(darkTheme = darkTheme) {
        // Auf dem Web liefert das Scaffold keine System-Insets (die regelt
        // index.html per CSS mit den iOS-Safe-Areas), aber es bleibt die
        // Fläche mit der Hintergrundfarbe des Themes.
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            ConverterScreen(
                uiState = uiState,
                onReload = { viewModel.loadRates() },
                onFromSelected = viewModel::selectFromCurrency,
                onToSelected = viewModel::selectToCurrency,
                onSwap = viewModel::swapCurrencies,
                darkTheme = darkTheme,
                onToggleTheme = { viewModel.toggleTheme(currentlyDark = darkTheme) },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
