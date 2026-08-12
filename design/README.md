# Design-Vorlagen

Handoffs aus Claude Design, Quelle für die Layout-Varianten in
`app/src/main/java/com/param/currencyconverter/ui/`.

- `v1/` → verworfene Variante "Taschenrechner" (nur noch in der Git-Historie)
- `v2/` → `ui/ConverterScreen.kt`, das gewählte Design
- `icon/` → App-Icon: zwei überlappende Münzen, dasselbe Motiv wie das
  Swap-Element im Screen. `play_store_512.png` ist für den Store-Eintrag und
  gehört bewusst nicht ins APK.

Die `.dc.html`-Dateien sind **nicht lauffähig**: Sie referenzieren
`support.js` und `android-frame.jsx`, die nicht Teil des Handoffs sind. Im
Browser geöffnet bleibt die Seite leer. Als Spezifikation sind sie trotzdem
maßgeblich — Farben, Größen und die komplette Rechnerlogik stehen im Markup
bzw. im eingebetteten Skript.
