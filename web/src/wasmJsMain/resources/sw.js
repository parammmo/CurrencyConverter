// Service Worker: macht aus der Website eine App, die auch ohne Netz startet.
//
// Ein Service Worker ist ein Skript, das der Browser *neben* der Seite laufen
// lässt und das jede Anfrage der Seite abfangen darf — ein Proxy im Browser.
// Wir nutzen genau eine Fähigkeit davon: Dateien, die einmal geladen wurden,
// beim nächsten Mal aus dem Cache statt vom Netz liefern.
//
// Strategie "cache first, fill on miss": Alles, was zur App-Shell gehört
// (HTML, JS, Wasm, Ressourcen), landet beim ersten Laden im Cache und kommt
// danach immer von dort. Die Kurs-API wird *nicht* abgefangen — deren
// Caching macht das Repository selbst, mit TTL und Stale-Fallback.
//
// Versionierung: BUILD_ID wird vom Gradle-Build eingesetzt (Git-Commit).
// Ein neuer Build ergibt ein neues Skript → der Browser installiert es,
// legt einen neuen Cache an und räumt den alten beim Aktivieren weg. So
// bleibt nie eine Mischung aus alter und neuer Version im Cache liegen.

const BUILD_ID = "@buildId@";
const CACHE_NAME = "currency-converter-" + BUILD_ID;

// Alles, was ohne Netz da sein muss. Die Liste setzt der Gradle-Build nach
// dem Bündeln ein (siehe web/build.gradle.kts) — die Wasm-Dateien haben
// gehashte Namen, die vorher nicht feststehen.
const PRECACHE = ["./"];

self.addEventListener("install", (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then((cache) => cache.addAll(PRECACHE))
            // Nicht auf das Schließen aller Tabs warten — sofort übernehmen.
            .then(() => self.skipWaiting())
    );
});

self.addEventListener("activate", (event) => {
    event.waitUntil(
        caches.keys()
            .then((names) => Promise.all(
                names.filter((n) => n !== CACHE_NAME).map((n) => caches.delete(n))
            ))
            // Auch schon offene Seiten diesem Worker zuordnen.
            .then(() => self.clients.claim())
    );
});

self.addEventListener("fetch", (event) => {
    const url = new URL(event.request.url);
    // Nur eigene Dateien; die Kurs-API und alles Fremde geht direkt durch.
    if (url.origin !== self.location.origin || event.request.method !== "GET") return;

    event.respondWith(
        caches.match(event.request).then((cached) => {
            if (cached) return cached;
            // Sollte nach dem Precache nicht mehr vorkommen — Sicherheitsnetz,
            // falls doch mal eine Datei fehlt.
            return fetch(event.request).then((response) => {
                if (response.ok) {
                    const copy = response.clone();
                    caches.open(CACHE_NAME).then((cache) => cache.put(event.request, copy));
                }
                return response;
            });
        })
    );
});
