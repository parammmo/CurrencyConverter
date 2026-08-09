package com.param.currencyconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.param.currencyconverter.ui.ConverterContent
import com.param.currencyconverter.ui.LayoutVariant
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
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    // Bewusst nur `rememberSaveable`, nicht im DataStore: Die Layout-Wahl ist
    // ein Entwurfswerkzeug und fliegt wieder raus, sobald wir uns entschieden
    // haben. Was temporär ist, soll auch nicht persistiert werden.
    var layoutVariant by rememberSaveable { mutableStateOf(LayoutVariant.CARDS) }

    // Der gespeicherte Wunsch wird hier zur konkreten Ja/Nein-Frage aufgelöst.
    // SYSTEM ist kein dritter Zeichenmodus — es heißt nur "frag das Gerät".
    val darkTheme = when (uiState.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    CurrencyConverterTheme(darkTheme = darkTheme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Währungsrechner") },
                    actions = {
                        LayoutVariantPicker(
                            selected = layoutVariant,
                            onSelect = { layoutVariant = it },
                        )
                        IconButton(onClick = { viewModel.toggleTheme(currentlyDark = darkTheme) }) {
                            Icon(
                                // Das Icon zeigt das *Ziel*, nicht den Ist-Zustand:
                                // im Dunkeln die Sonne ("hier geht's nach hell").
                                imageVector = if (darkTheme) {
                                    Icons.Default.LightMode
                                } else {
                                    Icons.Default.DarkMode
                                },
                                contentDescription = if (darkTheme) {
                                    "Zu hellem Design wechseln"
                                } else {
                                    "Zu dunklem Design wechseln"
                                },
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            ConverterContent(
                variant = layoutVariant,
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

/**
 * Temporärer Umschalter zwischen den Layout-Entwürfen.
 *
 * Previews zeigen die Varianten nebeneinander, aber nicht, wie sich Tippen
 * anfühlt oder ob die Tastatur etwas verdeckt. Dafür ist dieser Schalter da —
 * er wandert wieder raus, wenn die Entscheidung gefallen ist.
 */
@Composable
private fun LayoutVariantPicker(
    selected: LayoutVariant,
    onSelect: (LayoutVariant) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Layout-Variante wählen",
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        LayoutVariant.entries.forEach { variant ->
            DropdownMenuItem(
                text = { Text(variant.label) },
                leadingIcon = {
                    RadioButton(
                        selected = variant == selected,
                        onClick = null, // Klick behandelt das ganze MenuItem
                    )
                },
                onClick = {
                    onSelect(variant)
                    expanded = false
                },
            )
        }
    }
}
