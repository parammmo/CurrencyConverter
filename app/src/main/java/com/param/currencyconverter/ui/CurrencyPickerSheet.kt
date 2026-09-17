package com.param.currencyconverter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.param.currencyconverter.R
import java.util.Currency
import java.util.Locale

/**
 * Ein Währungscode mit seinem Klarnamen, sofern das Gerät einen kennt.
 *
 * [name] ist `null` für die Handvoll Codes, die [Currency] nicht führt
 * (CNH, GGP, JEP, ZWG …). Dann steht eben nur der Code da — besser als ein
 * erfundener Name.
 */
internal data class CurrencyOption(val code: String, val name: String?)

/**
 * Klarname zu einem Code, über die JVM statt über eine eigene Tabelle.
 *
 * [Currency.getInstance] wirft bei unbekannten Codes, deshalb `runCatching`.
 * [Currency.getDisplayName] liefert den Namen in der **Gerätesprache** —
 * dieselbe Übersetzung, die Android und die JVM ohnehin mitbringen, ohne dass
 * wir 166 Namen pro Sprache pflegen müssen.
 *
 * Geprüft: 157 der 166 Codes der API haben so einen deutschen Namen.
 */
internal fun currencyDisplayName(code: String): String? =
    runCatching { Currency.getInstance(code).displayName }
        .getOrNull()
        ?.takeIf { it != code }

/**
 * Auswahl-Sheet für die Währung.
 *
 * **Warum ein Sheet und kein `DropdownMenu` mehr:** Mit 166 Einträgen ist
 * Scrollen keine Auswahl mehr. Ein Menü, das an einem 40dp breiten Chip
 * hängt, hat weder Platz für ein Suchfeld noch Raum, wenn die Tastatur
 * aufgeht. Das `ModalBottomSheet` ist der Material-3-Behälter dafür.
 *
 * Aufbau von oben nach unten, in der Reihenfolge der Wahrscheinlichkeit:
 * zuletzt benutzt → alles. Der Verlauf ist die persönliche Kurzliste; eine
 * feste "wichtige Währungen"-Liste gibt es bewusst nicht, weil niemand
 * entscheiden kann, welche 30 für *dich* die wichtigen sind.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CurrencyPickerSheet(
    codes: List<String>,
    recents: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    // rememberSaveable, nicht remember: Beim Drehen des Geräts soll eine
    // getippte Suche nicht verloren gehen.
    var query by rememberSaveable { mutableStateOf("") }

    // Die Klarnamen einmal nachschlagen, nicht bei jedem Tastendruck neu:
    // remember(codes) hängt die Berechnung an die Liste, und die ändert sich
    // nur, wenn neue Kurse kommen.
    val options = remember(codes) { codes.map { CurrencyOption(it, currencyDisplayName(it)) } }
    val filtered = remember(options, query) { options.filterAndRank(query) }
    val recentOptions = remember(options, recents) {
        // Über die options-Liste gefiltert statt direkt über recents: So
        // fliegt ein Code raus, den die aktuelle Kurstabelle nicht (mehr)
        // führt — auswählbar wäre er ohnehin nicht.
        recents.mapNotNull { code -> options.firstOrNull { it.code == code } }
    }
    val showRecents = query.isBlank() && recentOptions.isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // skipPartiallyExpanded: Ein halb offenes Sheet mit Suchfeld ist ein
        // Zwischenzustand, in dem man nichts tun kann. Also gleich ganz auf.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surfaceContainer,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.picker_search_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                // Erst sichtbar, wenn es etwas zu löschen gibt — ein Knopf,
                // der nichts tut, ist schlimmer als keiner.
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_clear_search),
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )

        if (filtered.isEmpty()) {
            // Heuristik 5: nicht wortlos leer bleiben, sondern sagen, was los
            // ist. Der gesuchte Text wird zitiert, damit ein Tippfehler
            // auffällt.
            Text(
                text = stringResource(R.string.picker_no_match, query),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
            )
            return@ModalBottomSheet
        }

        LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
            if (showRecents) {
                item { SectionLabel(stringResource(R.string.picker_recent)) }
                items(recentOptions, key = { "recent-${it.code}" }) { option ->
                    CurrencyListItem(option, option.code == selected, onSelect)
                }
                item {
                    HorizontalDivider(
                        color = colors.outlineVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }
                item { SectionLabel(stringResource(R.string.picker_all)) }
            }

            items(filtered, key = { it.code }) { option ->
                CurrencyListItem(option, option.code == selected, onSelect)
            }
        }
    }
}

/**
 * Filtert nach Code *und* Klarname und sortiert die besten Treffer nach oben.
 *
 * Der Rang ist der Punkt, an dem Suche sich richtig anfühlt: Bei "us" soll
 * USD oben stehen, nicht "Belarussischer Rubel" — obwohl beide "us"
 * enthalten. Deshalb drei Stufen: Code beginnt so (0), Name beginnt so (1),
 * kommt irgendwo vor (2). `sortedBy` ist stabil, innerhalb einer Stufe bleibt
 * es also alphabetisch.
 */
private fun List<CurrencyOption>.filterAndRank(query: String): List<CurrencyOption> {
    val q = query.trim().lowercase(Locale.getDefault())
    if (q.isEmpty()) return this

    return mapNotNull { option ->
        val code = option.code.lowercase(Locale.getDefault())
        val name = option.name?.lowercase(Locale.getDefault()).orEmpty()
        val rank = when {
            code.startsWith(q) -> 0
            name.startsWith(q) -> 1
            // Auch Wortanfänge mitten im Namen: "denar" findet
            // "Mazedonischer Denar".
            name.split(' ').any { it.startsWith(q) } -> 1
            code.contains(q) || name.contains(q) -> 2
            else -> return@mapNotNull null
        }
        rank to option
    }.sortedBy { it.first }.map { it.second }
}

/** Überschrift einer Sektion — die leiseste Textrolle, es ist nur ein Etikett. */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

/**
 * Eine Zeile: Flagge, Code, Klarname — und ein Haken, wenn es die aktuelle
 * Auswahl ist.
 *
 * Der Haken ist Heuristik 1 (Sichtbarkeit des Systemstatus): Ohne ihn müsste
 * man sich merken, was vor dem Öffnen eingestellt war.
 */
@Composable
private fun CurrencyListItem(
    option: CurrencyOption,
    isSelected: Boolean,
    onSelect: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(option.code) }
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Feste Breite statt einfach nur [CurrencyFlag]: Codes ohne Flagge
        // (ANG, XOF, XDR …) geben sonst gar nichts aus, und die ganze Zeile
        // rutscht nach links — in einer langen Liste sieht man das sofort als
        // ausgefransten Rand. Der reservierte Platz hält alle Zeilen auf einer
        // Kante, auch die leeren.
        Box(
            modifier = Modifier.width(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            CurrencyFlag(option.code)
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = option.code,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                // Dieselbe Sperrung wie beim Code in der Umrechnungszeile —
                // sonst sähe derselbe Code an zwei Stellen anders aus.
                letterSpacing = 0.14.em,
                color = if (isSelected) colors.primary else colors.onSurface,
            )
            // Nur wenn es einen gibt: Eine Zeile, die den Code bloß wiederholt,
            // wäre reines Rauschen.
            option.name?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.cd_selected),
                tint = colors.primary,
            )
        }
    }
}
