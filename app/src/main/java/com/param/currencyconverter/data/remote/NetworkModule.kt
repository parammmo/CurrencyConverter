package com.param.currencyconverter.data.remote

import com.param.currencyconverter.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.Inet4Address
import java.net.InetAddress

/**
 * Android-Emulatoren haben oft ein virtuelles Netzwerk mit kaputtem/hängendem
 * IPv6 – OkHttp versucht trotzdem zuerst IPv6 und läuft dabei in den
 * Connect-Timeout, bevor es auf IPv4 zurückfällt (`SocketTimeoutException`).
 * Dieser Resolver filtert IPv6-Adressen einfach komplett raus.
 */
private object Ipv4OnlyDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val all = Dns.SYSTEM.lookup(hostname)
        val ipv4Only = all.filterIsInstance<Inet4Address>()
        // Fallback auf "alle Adressen", falls es ausschließlich IPv6 gäbe —
        // sonst wäre die App auf einem echten IPv6-only-Netz komplett offline.
        return ipv4Only.ifEmpty { all }
    }
}

/**
 * Baut den Retrofit-Client zusammen.
 *
 * Bewusst ein simples `object` (= Singleton) statt Dependency Injection:
 * Solange es genau einen Client gibt, wäre Hilt hier nur Zeremonie. Falls das
 * Projekt wächst, ist das hier die eine Stelle, die man austauscht.
 */
object NetworkModule {

    private const val BASE_URL = "https://api.frankfurter.dev/"

    private val json = Json {
        // Neue Felder in der API-Antwort sollen die App nicht crashen lassen.
        ignoreUnknownKeys = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .dns(Ipv4OnlyDns)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                // Nur im Debug-Build: BODY loggt die komplette Antwort ins Logcat.
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        )
        .build()

    val frankfurterApi: FrankfurterApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(FrankfurterApi::class.java)
}
