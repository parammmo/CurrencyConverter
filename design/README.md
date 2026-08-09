# Design-Vorlagen

Handoffs aus Claude Design, Quelle für die Layout-Varianten in
`app/src/main/java/com/param/currencyconverter/ui/`.

- `v1/` → `CalculatorLayout.kt` (Variante "Taschenrechner")
- `v2/` → `MinimalCalculatorLayout.kt` (Variante "Minimal")

Die `.dc.html`-Dateien sind **nicht lauffähig**: Sie referenzieren
`support.js` und `android-frame.jsx`, die nicht Teil des Handoffs sind. Im
Browser geöffnet bleibt die Seite leer. Als Spezifikation sind sie trotzdem
maßgeblich — Farben, Größen und die komplette Rechnerlogik stehen im Markup
bzw. im eingebetteten Skript.
