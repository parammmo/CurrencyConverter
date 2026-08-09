package com.param.currencyconverter.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 8px-Skala für alle Abstände. Zusammengehörige Elemente bekommen kleine
 * Werte, getrennte Gruppen große — der Abstand selbst zeigt schon, was
 * zusammengehört.
 */
object Spacing {
    val extraSmall: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 16.dp
    val large: Dp = 24.dp
    val extraLarge: Dp = 32.dp
    val huge: Dp = 48.dp
}

/**
 * Ein Eckenradius für alle Cards — nicht pro Komponente neu entscheiden.
 * 12dp ist der Material-3-Wert für Cards.
 *
 * Bewusst als [Dp] und nicht als Shape: In Etappe 6 (Theme.kt) wandert das
 * nach `MaterialTheme.shapes`, bis dahin reicht `RoundedCornerShape(Corner.card)`.
 */
object Corner {
    val card: Dp = 12.dp
}

/**
 * Eine einzige Elevation-Stufe. Mehr Stufen hieße mehr Einzelentscheidungen,
 * und die driften garantiert auseinander.
 */
object Elevation {
    val card: Dp = 2.dp
}
