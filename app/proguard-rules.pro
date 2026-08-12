# R8-Regeln für den Release-Build.
#
# Die meisten Bibliotheken bringen ihre eigenen Regeln mit (Retrofit, OkHttp und
# kotlinx.serialization liefern sie als "consumer rules" im Artefakt aus) — hier
# steht nur, was R8 aus unserem eigenen Code nicht ableiten kann.

# --- Retrofit ---
# Das API-Interface wird nie direkt aufgerufen: Retrofit erzeugt zur Laufzeit
# eine Implementierung und liest dafür die Annotationen und die generischen
# Rückgabetypen per Reflection aus. R8 sieht nur ein ungenutztes Interface und
# würde Signaturen und Annotationen wegwerfen.
-keep,allowobfuscation interface com.param.currencyconverter.data.remote.FrankfurterApi
-keepattributes Signature, RuntimeVisibleAnnotations, AnnotationDefault

# --- kotlinx.serialization ---
# Zu jeder @Serializable-Klasse erzeugt das Compiler-Plugin eine
# $$serializer-Klasse, die ebenfalls nur per Reflection gefunden wird.
-keepclassmembers class com.param.currencyconverter.** {
    *** Companion;
}
-keepclasseswithmembers class com.param.currencyconverter.** {
    kotlinx.serialization.KSerializer serializer(...);
}
