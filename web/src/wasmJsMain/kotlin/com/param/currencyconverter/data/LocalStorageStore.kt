package com.param.currencyconverter.data

import kotlinx.browser.localStorage

/**
 * Web-Pendant zu *einem* Preferences-DataStore: ein Bündel von
 * Schlüssel/Wert-Paaren, das App-Neustarts überlebt.
 *
 * `localStorage` ist der Browser-Speicher pro Origin — synchron, nur
 * Strings, in der Praxis ~5 MB. Der Kurs-Cache und die Einstellungen liegen
 * beide darin; getrennt werden sie über [prefix]. Das ist dasselbe wie die
 * zwei DataStore-*Dateien* in :app ("rates_cache", "user_settings"): Den
 * Cache wegwerfen kann nie die Einstellungen mitnehmen, solange man nur
 * seine Schlüssel anfasst.
 *
 * Kein Flow hier: localStorage meldet Änderungen nur an *andere* Tabs
 * (`storage`-Event), nicht an den eigenen. Das Beobachten übernehmen die
 * Repositories mit einem eigenen StateFlow.
 */
class LocalStorageStore(private val prefix: String) {

    operator fun get(key: String): String? = localStorage.getItem("$prefix.$key")

    operator fun set(key: String, value: String) = localStorage.setItem("$prefix.$key", value)
}
