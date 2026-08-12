package com.param.currencyconverter.ui

import java.util.Locale

/**
 * Flaggen-Emoji zu einem Währungscode, oder `null`, wenn es keins gibt.
 *
 * Der Trick: Die ersten beiden Buchstaben eines ISO-4217-Codes sind fast immer
 * das Länderkürzel nach ISO 3166 (USD → US, JPY → JP). Ein Flaggen-Emoji ist
 * nichts anderes als genau dieses Kürzel, geschrieben in
 * "Regional Indicator Symbols" — einem eigenen Alphabet ab U+1F1E6, das die
 * Schrift zu einem Bild zusammenzieht.
 *
 * Deshalb braucht es keine einzige Bilddatei: 🇺🇸 ist U+1F1FA U+1F1F8, also
 * "U" und "S" aus diesem Alphabet.
 */
fun flagEmoji(currencyCode: String): String? {
    val region = currencyCode.take(2).uppercase(Locale.ROOT)
    if (region.length != 2 || region !in FLAG_REGIONS) return null

    return buildString {
        region.forEach { letter ->
            appendCodePoint(REGIONAL_INDICATOR_A + (letter - 'A'))
        }
    }
}

/** Erster Buchstabe des Regional-Indicator-Alphabets ("A"). */
private const val REGIONAL_INDICATOR_A = 0x1F1E6

/**
 * Wofür es überhaupt eine Flagge gibt.
 *
 * Die Prüfung ist der eigentliche Zweck der Funktion: Ohne sie würde ein Code
 * wie XAU (Gold) oder XDR (IWF-Sonderziehungsrechte) zu "XA" bzw. "XD" — kein
 * Land, also zwei leere Kästchen auf dem Bildschirm. Die Frankfurter-API führt
 * solche Codes aktuell nicht, könnte das aber jederzeit ändern.
 *
 * EU muss von Hand dazu: Die Europäische Union ist kein Staat und steht daher
 * nicht in [Locale.getISOCountries] — ein Emoji hat sie trotzdem.
 */
private val FLAG_REGIONS: Set<String> = Locale.getISOCountries().toSet() + "EU"
