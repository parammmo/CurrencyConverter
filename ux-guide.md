# UX-Polish-Guide – Currency Converter

Leichtgewichtige Version des NoteApp-Lernplans: kurze Konzepte, ein
konkreter Schritt pro Etappe, **kein Pflicht-Quiz**. Jede Etappe hat zwei
Teile: **Design-Prinzip** (die nicht-technische Warum-Frage, unabhängig von
Compose) und **Umsetzung** (wie du es in Code baust).

Reihenfolge: **Tokens festlegen → Struktur → Interaktion → Vollständigkeit
der Zustände → Farbe (Light+Dark zusammen) → Branding**. Farbe/Theming kommt
bewusst spät, aber Light+Dark **zusammen**, nicht nacheinander – siehe
Design-Prinzipien unten, Punkt 3.

Aktueller Stand: Umrechnung funktioniert, UI ist rein funktional (nackte
`Button`s als Dropdown, kein Theming, kein Feinschliff bei Lade-/Fehler-
Zuständen).

**Vorgehen:** Nach jeder Etappe kurz zusammen die Design-Prinzipien
durchgehen, bevor die nächste beginnt – lieber eine Etappe wirklich clean
abschließen als drei halbfertig parallel anfangen.

---

## Referenzrahmen 1: 5 Heuristiken (das allgemeine Warum)

Aus Jakob Nielsens Usability-Heuristiken, verkürzt auf das Relevante hier:

1. **Sichtbarkeit des Systemstatus** – der Nutzer soll immer wissen, was
   gerade passiert (lädt es? fertig? Fehler?).
2. **Wiedererkennbarkeit statt Erinnerung** – Optionen/Zustände sichtbar
   machen, statt dass der Nutzer sich was merken muss.
3. **Konsistenz & Standards** – gleiche Aktion sieht überall gleich aus.
4. **Ästhetisches, minimalistisches Design** – jedes zusätzliche Element
   konkurriert um Aufmerksamkeit mit dem, was wirklich wichtig ist.
5. **Fehler erkennen, melden, beheben lassen** – nicht nur "Fehler"
   anzeigen, sondern auch einen Weg zurück anbieten.

## Referenzrahmen 2: Konkrete Material-3-Regeln für dieses Projekt (das Wie)

### Spacing
- Ausschließlich eine 8px-Skala: **4, 8, 16, 24, 32, 48** – keine
  willkürlichen Werte wie 10dp oder 23dp.
- Zusammengehörige Elemente → kleinere Abstände (4-8dp), getrennte Gruppen
  → größere (24-32dp). Der Abstand selbst zeigt Zusammengehörigkeit.

### Typografische Hierarchie
- Maximal 3-4 Textrollen pro Screen, ausschließlich
  `MaterialTheme.typography`-Tokens (z. B. `headlineLarge` für den
  Hauptbetrag, `bodyLarge` für Eingaben, `labelMedium`/`bodySmall` für
  Meta-Infos wie "zuletzt aktualisiert"). Keine frei erfundenen
  Schriftgrößen.
- Auf den ersten Blick muss klar sein, was das Wichtigste auf dem Screen
  ist (hier: der umgerechnete Betrag).

### Farbe
- Eine Primärfarbe für Hauptaktionen (z. B. Swap-Button), Graustufen
  (`onSurface`, `onSurfaceVariant`) für Text, maximal eine Akzentfarbe für
  Status (z. B. Fehler). Jede Farbe braucht eine Rolle, keine "weil hübsch".
- Dark Mode von Anfang an mitdenken, nicht nachträglich anflicken.

### Weißraum & Kontrast
- Screens dürfen atmen – nicht jede Fläche füllen.
- Wichtige Elemente (Betrag, Ergebnis) bekommen mehr Weißraum um sich herum
  als Nebensächliches.

### Konsistenz statt Einzelentscheidung
- Eckenradius, Elevation/Schatten, Icon-Größen: einmal festlegen, überall
  gleich anwenden.
- Vor einer neuen Komponente: erst schauen, ob's dafür schon ein Muster im
  Screen gibt, statt neu zu erfinden.

---

## Etappe 1: Design-Tokens festlegen

**Design-Prinzip:** *Konsistenz statt Einzelentscheidung* – wenn du
Eckenradius, Elevation und Icon-Größen erst beim Bauen jeder einzelnen
Komponente entscheidest, driften sie garantiert auseinander. Einmal
zentral festlegen verhindert das strukturell, nicht nur durch Disziplin.

**Umsetzung:** Leg dir (z. B. als `object Spacing` und ein paar `val`s)
deine Spacing-Skala (4/8/16/24/32/48dp) sowie einen einheitlichen
Eckenradius für Cards/Buttons und eine Elevation-Stufe fest. Nutze ab jetzt
nur noch diese Werte, nirgends mehr freihändige `dp`-Zahlen.

---

## Etappe 2: Layout-Hierarchie & Spacing

**Design-Prinzip:** *Visuelle Hierarchie* – Größe/Position/Gewichtung sagt
dem Auge, was zuerst wahrgenommen werden soll. Das **Ergebnis** ist die
wichtigste Information, nicht die Eingabefelder. Dazu das *Gestalt-Prinzip
der Nähe*: räumlich nahe Dinge wirken automatisch zusammengehörig – der
Grund, warum Gruppierung (Cards) überhaupt funktioniert.

**Umsetzung:** Gruppier Betrag+Von in einer `Card`, Ergebnis+Zu in einer
zweiten – mit den Tokens aus Etappe 1. Ergebnis-Text auf `headlineLarge`
(max. 3-4 Textrollen insgesamt einhalten, siehe Referenzrahmen 2). Wichtige
Elemente (Ergebnis) bekommen mehr Weißraum um sich herum als Nebensächliches.

**Check-in:** Welche Abstände hast du verwendet, und sind sie konsistent
zur Skala aus Etappe 1?

**Nachschlagen:** `Card`, `Modifier.padding`, `MaterialTheme.typography`.

---

## Etappe 3: Echtes Dropdown statt Button-Behelfslösung

**Design-Prinzip:** *Konsistenz & Standards* (Heuristik 3) und *Affordanz*
– dein aktueller Button-Dropdown-Hack sieht nicht wie ein Auswahlfeld aus,
der Nutzer muss ausprobieren, was klickbar ist. Ein Textfeld-artiges
Dropdown signalisiert sofort "hier gibt's eine Auswahl".

**Umsetzung:** Ersetz `CurrencyDropdown` durch `ExposedDropdownMenuBox`.
✅ erledigt.

**Nachschlagen:** `ExposedDropdownMenuBox`, `TextFieldDefaults.trailingIcon`.

---

## Etappe 4: Swap-Button (Von ↔ Zu tauschen)

**Design-Prinzip:** *Effizienz für wiederkehrende Aktionen* – "Kurs andersrum
anschauen" ist eine der häufigsten Aktionen in jedem Umrechner. Ohne
Swap-Button muss der Nutzer beide Dropdowns einzeln neu setzen.

**Umsetzung:** Icon-Button zwischen den beiden Dropdowns, tauscht
`fromCurrency`/`toCurrency`. Icon-Größe konsistent zu deinen Tokens aus
Etappe 1 (falls du dort schon eine festgelegt hast).

---

## Etappe 5: Lade-/Fehler-Zustände mit Feinschliff

**Design-Prinzip:** *Sichtbarkeit des Systemstatus* (Heuristik 1) und
*Fehlerbehebung ermöglichen* (Heuristik 5) – die am meisten übersehenen
Zustände im UI-Design. Die meisten Entwürfe zeigen nur den "Happy Path";
eine App, die bei einem Netzwerkfehler ratlos wirkt, verliert Vertrauen.

**Umsetzung:**

1. Fehlerzustand bekommt einen "Erneut versuchen"-Button direkt daneben
   (`viewModel.loadRates()`).
2. **Zeitpunkt des letzten Kursabrufs anzeigen** (Wunsch vom 2026-08-07,
   fest eingeplant, kein Bonus mehr). Gemeint ist die *Uhrzeit* ("Kurse von
   14:32"), nicht nur die relative Angabe ("vor 3 Stunden") – die Uhrzeit
   sagt schneller, ob man gerade frische Daten sieht. Textrolle
   `labelMedium`/`bodySmall`.

   Technisch hängt daran mehr, als es aussieht: Das Repository kennt den
   `cached_timestamp` bereits (siehe `ExchangeRateRepository`), gibt aber
   nur das `LatestRatesDto` zurück – der Zeitstempel geht dabei verloren.
   Der Rückgabetyp muss also um ihn erweitert werden (eigene kleine
   Ergebnisklasse statt DTO), dann bis in den `ConverterUiState` durch.

   Nicht verwechseln: `LatestRatesDto.date` ist das Datum *der EZB-Kurse*,
   nicht der Zeitpunkt unseres Abrufs. Angezeigt werden soll Letzteres.
3. Dazu passend der Cache-Fallback: Ist der Cache älter als die TTL **und**
   das Netz nicht erreichbar, sollen die alten Kurse trotzdem angezeigt
   werden (mit Hinweis auf ihr Alter), statt nur eine Fehlermeldung. Aktuell
   wirft `getRates` in dem Fall und der Nutzer sieht nichts.

---

## Etappe 6: Farbschema – Light UND Dark zusammen

**Design-Prinzip:** Farbe ist **Kommunikationsmittel**, kein Dekor – sie
lenkt Aufmerksamkeit (Ergebnis hervorheben) und trägt Bedeutung (Rot für
Fehler, nicht willkürlich). *Kontrast*: WCAG-Faustregel mindestens 4.5:1
zwischen Text- und Hintergrundfarbe – "sieht schön aus" reicht nicht, wenn
es bei Sonnenlicht unlesbar wird. Und: Dark Mode **von Anfang an**
mitdenken, nicht nachträglich anflicken – deshalb hier beides in einer
Etappe, nicht wie ursprünglich geplant nacheinander.

**Umsetzung:** `Theme.kt` mit **beiden** `ColorScheme`s gleichzeitig
(`lightColorScheme(...)` und `darkColorScheme(...)`), Auswahl über
`isSystemInDarkTheme()`. Farbrollen streng nach Referenzrahmen 2: eine
Primärfarbe (Hauptaktionen), Graustufen (`onSurface`/`onSurfaceVariant`)
für Text, maximal eine Akzentfarbe für Status. Bonus:
`dynamicLightColorScheme`/`dynamicDarkColorScheme` als Alternative auf
Android 12+.

**Nachschlagen:** `lightColorScheme()`, `darkColorScheme()`,
`isSystemInDarkTheme()`, `androidx.compose.ui.graphics.Color`.

---

## Etappe 7: App-Icon & letzter Schliff

**Design-Prinzip:** Das Icon ist der **erste Kontaktpunkt**, noch vor dem
Öffnen der App. Sollte auf einen Blick "Währungsrechner" sagen, nicht
generisch aussehen.

**Umsetzung:** Eigenes App-Icon (Image Asset Studio, kennst du aus
NoteApp), App-Name final setzen, einmal durchklicken und nach
Copy-Paste-Resten/Platzhaltertexten ("Test Button" o. ä.) suchen.

---

## Maybe later (nicht eingeplant, erst bei Bedarf entscheiden)

- **Klarnamen im Dropdown** ("USD – US-Dollar" statt nur "USD"). Reduziert
  *Erinnerungslast* (Heuristik 2). Der `currencies()`-Endpunkt in
  `FrankfurterApi` existiert schon, ist aber nirgends angebunden. Offen ist,
  ob das den Aufwand wert ist – bei ~30 Währungen sind die Codes eventuell
  vertraut genug.
- Icons (Flaggen der Länder) bei der Auswahl der Währungen

## Danach (falls noch Lust)

- Animierter Übergang beim Ergebnis-Wechsel (`AnimatedContent`) – *Feedback
  auf Änderungen sichtbar machen*, statt dass der neue Wert kommentarlos
  aufpoppt.
- Haptisches Feedback beim Swap-Button.
- Mehrere zuletzt genutzte Währungspaare als Schnellauswahl (baut auf dem
  Repository/Cache auf) – *Wiedererkennbarkeit statt Erinnerung*
  (Heuristik 2) auf die Spitze getrieben.
