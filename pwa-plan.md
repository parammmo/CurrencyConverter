# Plan: Currency Converter als PWA fürs iPhone

Stand: 2026-09-17. Ziel: dieselbe App als Website, die man in Safari über
"Zum Home-Bildschirm" installiert. Kein Mac, kein Apple-Account, kein App
Store. Der Android-Code in `app/` wird **nicht angefasst**.

## Grundentscheidungen

**Technik: Compose Multiplatform mit Kotlin/Wasm.** Die bestehende Compose-UI
wird in ein zweites Gradle-Modul kopiert und dort für den Browser gebaut.
Alles bleibt Kotlin; die UI rendert auf ein `<canvas>` und sieht aus wie auf
Android. Voraussetzung iOS 18.2+ (Safari mit WasmGC) — erfüllt.

**Struktur: eigenes Modul `web/` im selben Repo, Code wird kopiert.**
Warum kopieren statt teilen: Echtes Teilen (`shared`-Modul, von dem `app`
und `web` abhängen) würde bedeuten, die Android-Dateien zu verschieben und
`app/build.gradle.kts` umzubauen — genau das, was nicht passieren soll.
Preis der Kopie: Änderungen an der Android-UI wandern nicht automatisch in
die Web-Version. Für ein Nebenprojekt in Ordnung; das Zusammenführen in ein
`shared`-Modul ist später ein eigener, sauber abgrenzbarer Schritt.

**Warum ein Repo, nicht zwei:** Gradle-Wrapper, Version-Catalog,
`design/`, `ux-guide.md` und `CLAUDE.md` gelten für beide. Zwei Repos hieße,
den Prototyp und die Design-Regeln zu duplizieren oder zu verlinken. Ein
Repo, zwei Module, ein Android-Studio-Projekt.

## Was am bestehenden Projekt trotzdem passiert (nur Wurzelebene)

- `settings.gradle.kts`: `include(":web")` dazu.
- `gradle/libs.versions.toml`: neue Einträge (Kotlin-Multiplatform-Plugin,
  `org.jetbrains.compose`, Ktor, kotlinx-datetime, kotlinx-coroutines-core).
  Bestehende Einträge bleiben unverändert — `app/` bemerkt davon nichts.
- Neuer Ordner `web/`.

Prüfstein nach jedem Schritt: `./gradlew :app:compileDebugKotlin` läuft
weiter durch.

## Modul-Aufbau `web/`

```
web/
  build.gradle.kts            kotlin("multiplatform") + compose, Target wasmJs
  src/wasmJsMain/
    kotlin/…/                 kopierte + angepasste Kotlin-Dateien
    composeResources/
      values/strings.xml      1:1 aus app/src/main/res/values
      values-de/strings.xml
    resources/
      index.html              Canvas-Container, Meta-Tags, Manifest-Link
      manifest.webmanifest    Name, Icons, display: standalone, Farben
      sw.js                   Service Worker (App-Shell offline)
      icons/                  aus design/icon/play_store_512.png abgeleitet
```

Ein einziges Source-Set (`wasmJsMain`, kein `commonMain`): Wir bauen nur für
ein Ziel, ein zweites Source-Set wäre Struktur ohne Nutzen.

## Datei für Datei: Was übernommen werden kann, was ersetzt wird

| Datei | Übernahme | Was sich ändert |
|---|---|---|
| `ui/ConverterScreen.kt` | fast 1:1 | `R.string.x` → `Res.string.x` (compose-resources, gleiche XML-Dateien). `DecimalFormatSymbols`/`Locale` am Ende → Komma fest oder über JS-`Intl`. |
| `ui/CurrencyPickerSheet.kt` | 1:1 | Nur `R`→`Res`. **Früh auf dem iPhone testen**: das `OutlinedTextField` ist die einzige Texteingabe der App; Canvas-Textfelder + iOS-Tastatur sind der bekannte Schwachpunkt von Compose-Web. |
| `ui/CalculatorCore.kt` | Logik 1:1 | `BigDecimal`, `MathContext`, `RoundingMode`, `String.format` sind `java.*`. Ersatz: `com.ionspin.kotlin:bignum` (KMP-BigDecimal, fast gleiche API — kleinster Diff) oder Rundung von Hand über die Dezimal-Zeichenkette. Der 1,005-Fall aus dem Kommentar bleibt der Testfall. |
| `ui/CurrencyFlag.kt` | Idee 1:1 | `Locale.getISOCountries()` gibt's nicht → feste Menge der ISO-3166-Codes als String-Konstante. `appendCodePoint` → `Char.toChars`/String-Konstruktion aus Surrogates. |
| `ui/theme/*` | 1:1 | `isSystemInDarkTheme()` funktioniert in CMP (liest `prefers-color-scheme`). |
| `ui/ConverterPreviews.kt` | weglassen | Previews sind Android-Studio-Tooling, für die Web-Variante ohne Nutzen. |
| `CurrencyViewModel.kt` | 1:1 | `lifecycle-viewmodel` gibt es als KMP-Artefakt inkl. wasmJs; sonst Plain-Class mit eigenem `CoroutineScope`. |
| `data/ExchangeRateRepository.kt` | Logik 1:1 | `java.time.Instant` → `kotlinx-datetime`. `System.currentTimeMillis()` → `Clock.System.now().toEpochMilliseconds()`. Speicher: siehe unten. |
| `data/UserPreferencesRepository.kt` | Schnittstelle 1:1 | `Flow<CurrencyPair>` etc. bleiben; darunter statt DataStore ein `MutableStateFlow`, der aus `localStorage` initialisiert wird und bei jeder Änderung dorthin schreibt. Zwei Schlüssel-Präfixe (`rates.` / `settings.`) statt zwei Dateien — dieselbe Trennung wie jetzt. |
| `data/remote/ExchangeRateApi.kt`, `NetworkModule.kt` | neu, ~30 Zeilen | Retrofit/OkHttp sind JVM-only → Ktor Client (`ktor-client-js`), `ContentNegotiation` mit kotlinx-serialization. Dieselbe URL `https://open.er-api.com/v6/latest/{base}`, kein Key, CORS erlaubt (geprüft am 2026-09-17: `access-control-allow-origin: *`). |
| `data/remote/LatestRatesDto.kt` | 1:1 | reines kotlinx-serialization. |
| `MainActivity.kt` | neu, ~15 Zeilen | `fun main() = ComposeViewport(document.body!!) { App() }`. Kein `enableEdgeToEdge`, dafür `viewport-fit=cover` in `index.html` + `env(safe-area-inset-*)` als CSS-Padding fürs Notch/Home-Indicator. |

Warum DataStore nicht: Die KMP-Version deckt Android/JVM/iOS ab, wasmJs ist
nicht sicher dabei, und `localStorage` ist auf dem Web ohnehin das
naheliegende Pendant (synchron, String-Key/Value, überlebt Neustart —
genau das Profil von Preferences-DataStore).

## PWA-Bausteine (das, was aus "Website" eine "App" macht)

1. **`manifest.webmanifest`**: `name`, `short_name`, `start_url`,
   `display: "standalone"` (kein Browser-Chrome), `background_color`,
   `theme_color`, Icons 192/512 px.
2. **iOS-spezifische Meta-Tags** in `index.html` — Safari liest das Manifest
   nur teilweise: `apple-mobile-web-app-capable`,
   `apple-mobile-web-app-status-bar-style`, `apple-touch-icon` (180 px, kein
   Alpha — iOS setzt sonst schwarz drunter).
3. **Service Worker** (`sw.js`): cacht die App-Shell (`index.html`, die
   `.js`- und `.wasm`-Dateien) beim ersten Laden. Danach startet die App
   offline, und die Kurse kommen aus `localStorage` — das Stale-Verhalten
   aus dem Repository greift wie auf Android. Strategie "cache first" für
   die Shell; die API-Calls gehen am Worker vorbei, weil das Repository
   das Caching schon selbst macht. Versionsschlüssel im Worker hochzählen,
   damit ein neuer Build den alten Cache ablöst.
4. **`viewport`**: `width=device-width, initial-scale=1,
   viewport-fit=cover, user-scalable=no` — ohne `user-scalable=no` zoomt
   Safari bei schnellen Doppeltipps auf dem Ziffernblock.

## Hosting

Das Repo ist privat. GitHub Pages braucht dafür GitHub Pro, also entweder:

- **Repo auf public stellen** (Keystore-Passwörter liegen in der
  gitignorten `keystore.properties`, nichts Geheimes im Repo) → GitHub
  Pages + Actions-Workflow, oder
- **Cloudflare Pages / Netlify** (kostenlos, auch für private Repos, bauen
  direkt aus dem Repo).

Beide liefern `.wasm` mit dem richtigen MIME-Type. HTTPS ist Pflicht für
Service Worker — beide haben es automatisch.

Build-Befehl: `./gradlew :web:wasmJsBrowserDistribution`, Ergebnis liegt
in `web/build/dist/wasmJs/productionExecutable/`. Ein
GitHub-Actions-Workflow, der das bei Push auf `main` baut und deployt,
ist ~25 Zeilen.

## Reihenfolge (jeder Schritt einzeln testbar)

1. **Gerüst**: Modul `web/` mit leerem `Text("Hallo")`-Composable, im
   Desktop-Browser starten (`./gradlew :web:wasmJsBrowserDevelopmentRun`).
   Zeigt, dass Gradle/Kotlin/CMP-Versionen zusammenpassen — das ist der
   Schritt mit dem größten Überraschungsrisiko, deshalb zuerst.
2. **Datenschicht**: DTO, Ktor-API, Repository mit localStorage. Test:
   Kurse laden, Seite neu laden, Kurse kommen aus dem Cache.
3. **CalculatorCore** portieren, dann ViewModel. Kleiner Testlauf mit den
   Fällen aus den Kommentaren (1,005 → 1,01; "000"-Taste; Ketten-Operatoren).
4. **UI** kopieren: Theme → ConverterScreen → PickerSheet, mit Strings als
   compose-resources.
5. **Deploy** als nackte Website, auf ihrem iPhone in Safari öffnen.
   Hier zeigt sich, ob Textfeld und Ziffernblock-Tipps sauber funktionieren
   — bevor der PWA-Feinschliff Zeit kostet.
6. **PWA-Schicht**: Manifest, Meta-Tags, Service Worker, Icons. Test:
   "Zum Home-Bildschirm", Flugmodus an, App starten.

## Bekannte Risiken, ehrlich sortiert

- **Texteingabe im Picker-Sheet** (Schritt 5 klärt es). Plan B: Suche
  durch die Verlaufs-Chips plus alphabetische Liste ersetzen.
- **Erster Ladevorgang**: Compose-Web ist ~5–8 MB (Skia im Wasm). Beim
  ersten Öffnen dauert es Sekunden, danach kommt alles aus dem
  Service-Worker-Cache. Auf dem Homescreen-Icon merkt man das nicht mehr.
- **Safari-Eigenheiten mit Canvas-Apps**: Scroll-Physik im Bottom-Sheet,
  Haptik fehlt. Der Rechner selbst (nur Taps) ist davon nicht betroffen.
- **Versionsdrift zur Android-App**: durch die Kopie bewusst in Kauf
  genommen, siehe Grundentscheidungen.
