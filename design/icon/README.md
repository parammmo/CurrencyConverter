# Münz-Ebenen — Android Launcher Icons

Kopiere den Inhalt von `res/` in `app/src/main/res/` deines Android-Studio-Projekts (Ordner zusammenführen). In `AndroidManifest.xml`:

    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round"

Enthalten:
- `mipmap-<dpi>/ic_launcher.png` — Legacy-Icons (48–192px, abgerundetes Quadrat)
- `mipmap-<dpi>/ic_launcher_round.png` — runde Legacy-Variante
- `mipmap-<dpi>/ic_launcher_foreground.png` + `mipmap-anydpi-v26/*.xml` + `values/ic_launcher_background.xml` — Adaptive Icon (Android 8+; System maskiert Kreis/Squircle selbst, Hintergrundfarbe #993c1d)
- `play_store_512.png` — 512×512 für den Play-Store-Eintrag
