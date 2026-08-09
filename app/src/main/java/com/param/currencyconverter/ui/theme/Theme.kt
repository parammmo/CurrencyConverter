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

private val LightColors = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    error = ErrorLight,
    onError = OnErrorLight,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = ErrorDark,
    onError = OnErrorDark,
)

/**
 * Bewusst *kein* Dynamic Color (Material You, Android 12+): Die App hätte dann
 * auf jedem Gerät eine andere Primärfarbe, je nach Hintergrundbild des Nutzers.
 * Für ein Projekt, in dem wir Farbrollen gezielt festlegen wollen, wäre das
 * kontraproduktiv — die Entscheidung läge beim Zufall statt bei uns.
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
