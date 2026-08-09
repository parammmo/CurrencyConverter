package com.param.currencyconverter.ui.theme

import androidx.compose.ui.graphics.Color

// Farbwerte aus dem Handoff `design/v2`.
//
// Die Namen beschreiben die *Rolle im Entwurf*, nicht den Farbton — bleibt
// also richtig, falls das Terracotta später einem anderen Ton weicht. Welche
// Material-3-Rolle daraus wird, steht in Theme.kt.

// --- Light ---

/** Bildschirmhintergrund. Der Entwurf kennt keine abgesetzten Flächen. */
val BackgroundLight = Color(0xFFF6F0EA)

/** Die großen Beträge — der stärkste Kontrast im Screen. */
val AmountLight = Color(0xFF2E1B12)

/** Ziffern des Tastenfelds, minimal weicher als die Beträge. */
val DigitLight = Color(0xFF3A251A)

/** Operatoren, aktiver Währungscode, gefüllte Flächen, Theme-Symbol. */
val AccentLight = Color(0xFFA6401F)

/** Hilfstasten, Fußzeile, inaktiver Währungscode. */
val SecondaryLight = Color(0xFF8A6A5C)

/** Der hintere der beiden Swap-Kreise. */
val SwapBackLight = Color(0x80C9927E)

/** Die drei 1px-Linien, die den Screen in Zonen teilt. */
val HairlineLight = Color(0x245A321E)

/** Popup-Flächen (Währungsmenü) — leicht heller als der Hintergrund. */
val ElevatedSurfaceLight = Color(0xFFFCF7F3)

/** Text auf gefüllten Akzentflächen. */
val OnAccent = Color(0xFFFFF5EE)

// --- Dark ---

val BackgroundDark = Color(0xFF1B120E)
val AmountDark = Color(0xFFF8EFE8)
val DigitDark = Color(0xFFEFDFD4)

/**
 * Im Dunkeln heller und weniger gesättigt als im Hellen — gesättigte Farben
 * flimmern auf dunklem Grund.
 */
val AccentDark = Color(0xFFD97B52)

/**
 * Für *gefüllte* Flächen nimmt der Entwurf im Dunkeln einen dunkleren Ton als
 * für Text: Ein heller Akzent über 62dp Fläche würde blenden.
 */
val AccentFillDark = Color(0xFFB54B26)

val SecondaryDark = Color(0xFF9C7A67)
val SwapBackDark = Color(0x73A5735C)
val HairlineDark = Color(0x24FFDCC3)
val ElevatedSurfaceDark = Color(0xFF261A14)
