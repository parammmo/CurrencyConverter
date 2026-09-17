package com.param.currencyconverter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.browser.document

/*
 * Zuordnung der Entwurfsrollen zu den Material-3-Rollen.
 *
 * Der Umweg lohnt sich, obwohl der Screen kaum Material-Komponenten benutzt:
 * Das Währungs-Dropdown, die Ladeanzeige und jede Ripple lesen ihre Farben aus
 * `MaterialTheme.colorScheme`. Läge die Palette daneben in einem eigenen
 * Objekt, hätten genau diese Elemente weiterhin fremde Farben — das
 * Auswahlmenü stünde in Teal auf terracotta Grund.
 *
 *   background / surface  ← Bildschirmhintergrund
 *   onBackground          ← Beträge
 *   onSurface             ← Ziffern
 *   onSurfaceVariant      ← Hilfstasten, Fußzeile, inaktiver Code
 *   primary               ← Akzent für Text und Symbole
 *   primaryContainer      ← Akzent für gefüllte Flächen ("=", Swap-Kreis)
 *   onPrimaryContainer    ← Text darauf
 *   secondaryContainer    ← hinterer Swap-Kreis
 *   outlineVariant        ← Haarlinien
 *   surfaceContainer      ← Popup-Flächen (Währungsmenü)
 */

private val LightColors = lightColorScheme(
    primary = AccentLight,
    onPrimary = OnAccent,
    primaryContainer = AccentLight,
    onPrimaryContainer = OnAccent,
    secondaryContainer = SwapBackLight,
    background = BackgroundLight,
    onBackground = AmountLight,
    surface = BackgroundLight,
    onSurface = DigitLight,
    onSurfaceVariant = SecondaryLight,
    surfaceContainer = ElevatedSurfaceLight,
    surfaceContainerHigh = ElevatedSurfaceLight,
    outlineVariant = HairlineLight,
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = BackgroundDark,
    primaryContainer = AccentFillDark,
    onPrimaryContainer = OnAccent,
    secondaryContainer = SwapBackDark,
    background = BackgroundDark,
    onBackground = AmountDark,
    surface = BackgroundDark,
    onSurface = DigitDark,
    onSurfaceVariant = SecondaryDark,
    surfaceContainer = ElevatedSurfaceDark,
    surfaceContainerHigh = ElevatedSurfaceDark,
    outlineVariant = HairlineDark,
)

/**
 * Bewusst *kein* Dynamic Color (Material You): Die App hätte dann auf jedem
 * Gerät eine andere Primärfarbe, je nach Hintergrundbild des Nutzers — und
 * damit wäre der Entwurf, für den wir uns entschieden haben, hinfällig.
 *
 * [darkTheme] ist als Parameter herausgezogen, damit Previews beide Modi
 * erzwingen können, ohne die Systemeinstellung zu ändern.
 */
@Composable
fun CurrencyConverterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Web-Pendant zur Statusleisten-Färbung auf Android: Compose zeichnet nur
    // in sein <canvas>. Was der Browser *drumherum* zeigt — die Fläche hinter
    // dem Canvas beim Start, der Rand am Home-Indicator, bei iOS-Web-Apps die
    // Statusleiste über `theme-color` — kommt aus dem HTML. Das muss zum
    // Modus passen, sonst blitzt beim Umschalten ein weißer Rand um eine
    // dunkle App auf.
    //
    // SideEffect aus demselben Grund wie auf Android: ein Aufruf nach draußen
    // (ins DOM), nach jeder erfolgreichen Recomposition, keine Coroutine.
    val background = if (darkTheme) DarkColors.background else LightColors.background
    SideEffect {
        val css = background.toCssHex()
        document.body?.style?.backgroundColor = css
        document.querySelector("meta[name=theme-color]")?.setAttribute("content", css)
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

/** Compose-Farbe → "#rrggbb", wie CSS es erwartet. */
private fun Color.toCssHex(): String =
    "#" + (toArgb() and 0xFFFFFF).toString(16).padStart(6, '0')
