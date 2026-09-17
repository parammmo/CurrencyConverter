package com.param.currencyconverter.ui

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
    val region = currencyCode.take(2).uppercase()
    if (region.length != 2 || region !in FLAG_REGIONS) return null

    // appendCodePoint gibt es nur auf der JVM. Ein Kotlin-String besteht aus
    // 16-Bit-Chars; Zeichen jenseits davon (wie die Regional Indicators ab
    // U+1F1E6) werden als *zwei* Chars gespeichert, dem Surrogat-Paar. Die
    // Formel dafür steht im Unicode-Standard und ist genau das, was
    // appendCodePoint intern auch tut.
    return buildString {
        region.forEach { letter ->
            val offset = REGIONAL_INDICATOR_A + (letter - 'A') - 0x10000
            append((0xD800 + (offset shr 10)).toChar())
            append((0xDC00 + (offset and 0x3FF)).toChar())
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
 * Land, also zwei leere Kästchen. XDR, XAF, XOF, XCD und XPF stehen wirklich
 * in der Liste der API.
 *
 * Auf Android kam die Liste aus `Locale.getISOCountries()`. Das gibt es in
 * Kotlin/Wasm nicht, also steht sie hier fest — ISO 3166-1 alpha-2 plus EU.
 * Ändert sich selten genug, dass eine Konstante die ehrlichere Lösung ist als
 * eine Abhängigkeit.
 */
private val FLAG_REGIONS: Set<String> = (
    "AD AE AF AG AI AL AM AO AQ AR AS AT AU AW AX AZ BA BB BD BE BF BG BH BI BJ BL BM BN BO BQ BR " +
        "BS BT BV BW BY BZ CA CC CD CF CG CH CI CK CL CM CN CO CR CU CV CW CX CY CZ DE DJ DK DM DO " +
        "DZ EC EE EG EH ER ES ET FI FJ FK FM FO FR GA GB GD GE GF GG GH GI GL GM GN GP GQ GR GS GT " +
        "GU GW GY HK HM HN HR HT HU ID IE IL IM IN IO IQ IR IS IT JE JM JO JP KE KG KH KI KM KN KP " +
        "KR KW KY KZ LA LB LC LI LK LR LS LT LU LV LY MA MC MD ME MF MG MH MK ML MM MN MO MP MQ MR " +
        "MS MT MU MV MW MX MY MZ NA NC NE NF NG NI NL NO NP NR NU NZ OM PA PE PF PG PH PK PL PM PN " +
        "PR PS PT PW PY QA RE RO RS RU RW SA SB SC SD SE SG SH SI SJ SK SL SM SN SO SR SS ST SV SX " +
        "SY SZ TC TD TF TG TH TJ TK TL TM TN TO TR TT TV TW TZ UA UG UM US UY UZ VA VC VE VG VI VN " +
        "VU WF WS YE YT ZA ZM ZW EU"
    ).split(' ').toSet()
