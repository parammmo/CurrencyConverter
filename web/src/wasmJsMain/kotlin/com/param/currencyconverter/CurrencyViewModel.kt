package com.param.currencyconverter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.param.currencyconverter.data.ExchangeRateRepository
import com.param.currencyconverter.data.ThemeMode
import com.param.currencyconverter.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Ein State-Objekt für den ganzen Screen statt vieler einzelner
 * `mutableStateOf`-Variablen — bei mehr als 2-3 zusammenhängenden Werten
 * (hier: laden/Kurse/Fehler) wird das schnell übersichtlicher.
 */
data class ConverterUiState(
    val isLoading: Boolean = false,
    val baseCurrency: String = "EUR",
    val rates: Map<String, Double> = emptyMap(),
    /**
     * Nur ein Ja/Nein: Welcher Text daraus wird, entscheidet die UI — sonst
     * müsste das ViewModel Ressourcen kennen und wäre nicht übersetzbar.
     */
    val hasError: Boolean = false,
    val fromCurrency: String = UserPreferencesRepository.DEFAULT_FROM,
    val toCurrency: String = UserPreferencesRepository.DEFAULT_TO,
    /** Unix-Millis des letzten erfolgreichen Netzabrufs, null = noch nie geladen. */
    val fetchedAt: Long? = null,
    /** Kurse stammen aus einem abgelaufenen Cache, weil das Netz nicht ging. */
    val isStale: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Zuletzt gewählte Währungen, neueste zuerst — Kurzliste im Auswahl-Sheet. */
    val recentCurrencies: List<String> = emptyList(),
)

class CurrencyViewModel(
    private val repository: ExchangeRateRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConverterUiState())
    val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow()

    init {
        loadRates()
        observeCurrencyPair()
        observeThemeMode()
        observeRecents()
    }

    private fun observeRecents() {
        viewModelScope.launch {
            preferences.recentCurrencies.collect { recents ->
                _uiState.update { it.copy(recentCurrencies = recents) }
            }
        }
    }

    private fun observeThemeMode() {
        viewModelScope.launch {
            preferences.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
    }

    /**
     * Schaltet auf das Gegenteil dessen, was *gerade zu sehen* ist. Beim ersten
     * Tippen steht der Modus noch auf SYSTEM — dann entscheidet [currentlyDark],
     * wohin es geht, statt blind auf LIGHT zu springen.
     */
    fun toggleTheme(currentlyDark: Boolean) {
        viewModelScope.launch {
            preferences.setThemeMode(if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK)
        }
    }

    /**
     * Die gespeicherte Auswahl ist die *einzige* Quelle der Wahrheit: Ein Tipp
     * im Dropdown schreibt nur in den DataStore, und die Änderung kommt über
     * diesen Flow zurück in den UI-State. So kann UI und Platte nie
     * auseinanderlaufen.
     */
    private fun observeCurrencyPair() {
        viewModelScope.launch {
            preferences.currencyPair.collect { pair ->
                _uiState.update { it.copy(fromCurrency = pair.from, toCurrency = pair.to) }
            }
        }
    }

    fun selectFromCurrency(code: String) {
        viewModelScope.launch { preferences.setFrom(code) }
    }

    fun selectToCurrency(code: String) {
        viewModelScope.launch { preferences.setTo(code) }
    }

    fun swapCurrencies() {
        val current = _uiState.value
        viewModelScope.launch {
            preferences.setPair(from = current.toCurrency, to = current.fromCurrency)
        }
    }

    fun loadRates(base: String = _uiState.value.baseCurrency) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasError = false) }
            try {
                val result = repository.getRates(base)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        baseCurrency = result.base,
                        rates = result.rates,
                        fetchedAt = result.fetchedAt,
                        isStale = result.isStale,
                    )
                }
            } catch (e: Exception) {
                // Bewusst breit gefangen: Uns interessiert hier nur "hat's
                // geklappt oder nicht", nicht die genaue Exception-Art —
                // Timeout, kein Netz, Server-Fehler landen alle in der UI
                // als derselbe Zustand "Kurse nicht verfügbar".
                _uiState.update {
                    it.copy(isLoading = false, hasError = true)
                }
            }
        }
    }
}
