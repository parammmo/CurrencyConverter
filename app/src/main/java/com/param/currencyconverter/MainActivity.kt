package com.param.currencyconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CurrencyConverterApp()
        }
    }
}

/**
 * Die oberste Ebene: hier entsteht das ViewModel, und hier wird entschieden,
 * ob hell oder dunkel gezeichnet wird.
 *
 * Das muss *über* [CurrencyConverterTheme] passieren — das Theme umschließt
 * alles andere, also kann die Entscheidung nicht weiter unten im Baum fallen.
 *
 * Ohne TopAppBar: Der Entwurf füllt den Bildschirm bis unter die Statusleiste,
 * und den Theme-Umschalter trägt die Fußzeile. Eine Titelleiste würde nur
 * Platz kosten und den Namen wiederholen, den schon das App-Icon nennt.
 */
@Composable
fun CurrencyConverterApp() {
    val context = LocalContext.current
    val viewModel: CurrencyViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CurrencyViewModel(
                    ExchangeRateRepository(context.applicationContext, NetworkModule.frankfurterApi),
                    UserPreferencesRepository(context.applicationContext),
                )
            }
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Der gespeicherte Wunsch wird hier zur konkreten Ja/Nein-Frage aufgelöst.
    // SYSTEM ist kein dritter Zeichenmodus — es heißt nur "frag das Gerät".
    val darkTheme = when (uiState.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    CurrencyConverterTheme(darkTheme = darkTheme) {
        // Das Scaffold bleibt allein wegen der Fenster-Insets: Es rechnet aus,
        // wie viel Platz Status- und Navigationsleiste brauchen.
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
