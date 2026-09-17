@file:OptIn(InternalResourceApi::class)

package com.`param`.currencyconverter.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.LanguageQualifier
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.ResourceContentHash
import org.jetbrains.compose.resources.ResourceItem

private const val MD: String = "composeResources/com.param.currencyconverter.resources/"

@delegate:ResourceContentHash(2_013_014_103)
internal val Res.plurals.rates_age_days: PluralStringResource by lazy {
      PluralStringResource("plurals:rates_age_days", "rates_age_days", setOf(
        ResourceItem(setOf(LanguageQualifier("de"), ), "${MD}values-de/strings.commonMain.cvr", 10, 70),
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 10, 70),
      ))
    }

@delegate:ResourceContentHash(-1_826_134_471)
internal val Res.plurals.rates_age_hours: PluralStringResource by lazy {
      PluralStringResource("plurals:rates_age_hours", "rates_age_hours", setOf(
        ResourceItem(setOf(LanguageQualifier("de"), ), "${MD}values-de/strings.commonMain.cvr", 81, 79),
        ResourceItem(setOf(), "${MD}values/strings.commonMain.cvr", 81, 75),
      ))
    }

@InternalResourceApi
internal fun _collectCommonMainPlurals0Resources(map: MutableMap<String, PluralStringResource>) {
  map.put("rates_age_days", Res.plurals.rates_age_days)
  map.put("rates_age_hours", Res.plurals.rates_age_hours)
}
