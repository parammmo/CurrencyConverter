package com.param.currencyconverter.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
    val view = LocalView.current
    if (!view.isInEditMode) {
        // Die Icons in Status- und Navigationsleiste zeichnet das System, nicht
        // Compose. Sie müssen trotzdem zu unserem Modus passen — sonst stehen
        // dunkle Systemicons auf dunklem Grund, sobald der Nutzer manuell
        // umschaltet und vom Systemmodus abweicht.
        //
        // SideEffect statt LaunchedEffect: Das ist ein Aufruf in die
        // Android-Welt hinaus, der nach jeder erfolgreichen Recomposition
        // laufen soll — keine Coroutine.
        val window = (view.context as Activity).window
        SideEffect {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
