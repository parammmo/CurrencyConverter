package com.param.currencyconverter.ui.theme

import androidx.compose.ui.graphics.Color

// Jede Farbe hat hier genau eine Rolle. Namen sagen die Rolle, nicht den
// Farbton — "PrimaryLight" bleibt richtig, auch wenn wir den Grünton später
// austauschen. Hieße die Konstante "Teal600", müssten wir sie mit umbenennen.

// --- Light ---
val PrimaryLight = Color(0xFF166B5C)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFA3F2DE)
val OnPrimaryContainerLight = Color(0xFF00201A)

val BackgroundLight = Color(0xFFFBFDFB)
val OnBackgroundLight = Color(0xFF1A1C1B)
val SurfaceLight = Color(0xFFFBFDFB)
val OnSurfaceLight = Color(0xFF1A1C1B)

/** Die Card-Fläche. Muss sich vom Hintergrund abheben, sonst ist die Card unsichtbar. */
val SurfaceContainerLight = Color(0xFFEDF3F0)
val SurfaceVariantLight = Color(0xFFDBE5E0)

/** Gedämpft für Nebeninfos wie den Zeitstempel — nie für Wichtiges. */
val OnSurfaceVariantLight = Color(0xFF3F4946)
val OutlineLight = Color(0xFF6F7976)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)

// --- Dark ---
// Nicht einfach die hellen Werte invertiert: Im Dunkeln braucht die
// Primärfarbe *weniger* Sättigung und *mehr* Helligkeit, sonst flimmert sie
// auf dunklem Grund.
val PrimaryDark = Color(0xFF7FD8C4)
val OnPrimaryDark = Color(0xFF00382F)
val PrimaryContainerDark = Color(0xFF005045)
val OnPrimaryContainerDark = Color(0xFFA3F2DE)

val BackgroundDark = Color(0xFF0F1513)
val OnBackgroundDark = Color(0xFFDDE4E1)
val SurfaceDark = Color(0xFF0F1513)
val OnSurfaceDark = Color(0xFFDDE4E1)

val SurfaceContainerDark = Color(0xFF1B2320)
val SurfaceVariantDark = Color(0xFF3F4946)

val OnSurfaceVariantDark = Color(0xFFBEC9C5)
val OutlineDark = Color(0xFF899390)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
