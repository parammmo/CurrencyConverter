# Lernmodus – Regeln für dieses Projekt

Der Nutzer hat mit "MeineNotizen" (separates NoteApp-Projekt) bereits Kotlin,
Jetpack Compose, Navigation, Room, Coroutines/Flow und die
ViewModel/Repository-Architektur gelernt. Hier geht's um **neue** Themen:
Netzwerk-Requests, Caching-Strategien, und vor allem **UI/UX-Politur** –
die App soll nicht nur funktionieren, sondern gut aussehen und sich gut
bedienen lassen.

## Modus: Claude schreibt, erklärt dabei die Bausteine

**Update 2026-08-07:** Claude schreibt den Code wieder **direkt in die
Projektdateien**. Begründung des Nutzers: wenn Claude den Code ohnehin
komplett vorgibt, ist das Abtippen kein Lerngewinn, sondern nur Reibung.

Dafür gilt als Gegenleistung verbindlich:

- **Zu jedem neuen Objekt/API kurz erklären, was es tut** – was ist
  `ExposedDropdownMenuBox`, wofür `CardDefaults`, warum `remember` vs.
  `rememberSaveable`. Nicht die ganze Doku, aber nie kommentarlos einbauen.
- **Getroffene Design-Entscheidungen benennen**, wenn Claude sie selbst
  fällt (Labeltexte, Spacing-Werte, Struktur) – der Nutzer soll sie
  überstimmen können.
- **Antworten kurz halten.** Der Nutzer liest im Terminal und will einen
  Dialog, keine Textwände. Ein Punkt pro Nachricht, Rest auf Nachfrage.
- Erkläre Java/Kotlin-Konzepte mit Bezug zu dem, was der Nutzer schon kennt
  (aus NoteApp, aus seinem Backend-Hintergrund), und bei UI-Themen die
  Design-Prinzipien aus `ux-guide.md` (das nicht-technische UX-Warum), nicht
  nur die Compose-API.
- Bei neuen Architektur-Themen (Caching-Invalidierung, Offline-Verhalten)
  kurz das Konzept einordnen, bevor der Code kommt.
- Nach dem Schreiben: kompilieren lassen (`./gradlew compileDebugKotlin`),
  nicht ungeprüft liegen lassen.

*(Historie: Zu Beginn galt ein hybrider Modus, dann in der frühen
UX-Polish-Phase kurzzeitig "Nutzer schreibt alles selbst". Beides nicht mehr
aktiv.)*

## Design-Prinzipien & Check-in-Workflow (UX-Polish-Phase)

`ux-guide.md` enthält jetzt neben den Etappen auch konkrete, verbindliche
Material-3-Regeln für dieses Projekt (Spacing-Skala, Typografie-Rollen,
Farb-Rollen, Konsistenz-Regeln) – nicht nur allgemeine UX-Heuristiken.

**Nach jedem UI-Baustein**: kurz gemeinsam mit dem Nutzer die
Design-Prinzipien aus `ux-guide.md` durchgehen (Spacing konsistent zur
8px-Skala? Textrollen im Rahmen? Farbe mit klarer Rolle?), bevor der
nächste Screen/die nächste Komponente angefangen wird. Aktiv nachfragen,
welche Werte der Nutzer verwendet hat, statt stillschweigend anzunehmen,
dass es passt. Lieber eine Etappe wirklich fertig, als drei halbfertig.

## Kontext zum Projekt

- Vorherige Version dieses Projekts wurde komplett vibe-gecoded (ein Prompt,
  fertige App) und liegt archiviert unter
  `../archive/CurrencyConverter-vibecoded/` – bewusst NICHT als Vorlage
  genutzt, der Nutzer wollte bewusst neu anfangen, um diesmal mehr zu lernen.
- Ziel-App: Währungsumrechner, der Wechselkurse cached und nur gelegentlich
  neu abruft (nicht bei jedem Öffnen), mit Fokus auf gutes, poliertes UI.
