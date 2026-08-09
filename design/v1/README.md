# Handoff: Currency Converter (Android)

## Overview
A calculator-style currency converter app screen (EUR ⇄ XMR): two currency amount cards, a 4×5 calculator keypad with an accent operator column, and a footer showing last-refresh time and the current rate. Delivered in a light and a dark theme with a warm terracotta palette. Target: native Android (Kotlin / Jetpack Compose, Material 3) in Android Studio.

## About the Design Files
The bundled `Currency Converter.dc.html` is a **design reference created in HTML** — an interactive prototype showing intended look and behavior, not production code. The task is to **recreate this design in the Android codebase** using Jetpack Compose (or the project's existing UI stack), following its established patterns. The HTML shows both themes side-by-side in phone frames; the frames are presentation only.

## Fidelity
**High-fidelity.** Colors, spacing, radii, and typography are final; recreate pixel-perfectly, mapping to Compose/Material 3 idioms (e.g. dp ≈ px here at 412×892 reference viewport).

## Screen: Converter
Single screen, vertical column, 10–12px outer padding, 10px gap, fills viewport under the status bar.

### 1. Currency cards (×2, top = input by default)
- Row, space-between, padding 18px 20px, radius 20px.
- Left cluster (column, centered, 7px gap): currency badge — 44px circle, white glyph 21px/600 (EUR: € on #3B5BA5; XMR: ɱ on #C0562B) — and currency code label 14px/600, letter-spacing 0.04em.
- Right: amount, 52px / 600, tabular numerals, single line, ellipsis past ~250px.
- Active (focused) card: 2px accent ring (light #A6401F, dark #C55A31). Tapping a card makes it the input side and resets entry to 0.

### 2. Keypad — 4-column grid, 5 rows, 8px gap, fills remaining height
Rows: [C, ⌫, ⇅, ÷] [7 8 9 ×] [4 5 6 −] [1 2 3 +] [0 , % =]
- All keys: radius 18px, centered glyph, pressed state scale(0.94).
- Number keys: 26px/500. Utility keys (C ⌫ ⇅): 24px/500. Operators + '=': 28px/500.

### 3. Footer — row, space-between, padding 8px 10px 6px
- Left: refresh icon (↻) 36px round touch target.
- Center column: date/time `DD/MM/YYYY HH:mm` 13px/600 accent color; rate line `1 EUR = 0,0035 XMR` 12px secondary color. Rate line follows the current direction after swap.
- Right: overflow icon (⋯) 36px round touch target.

## Design Tokens

Font: Instrument Sans (Google Fonts); fallback sans-serif. Weights 400–700.

### Light theme
- App background: #F4ECE6 · page/canvas #EDE4DC
- Card background: #FCF7F3 · card label #332016 · amount #2E1B12
- Number key: bg #FFFFFF, text #3A251A, shadow 0 1px 2px rgba(60,30,15,0.06)
- Utility key: bg #EAD9CE, text #7A4530
- Operator key: bg #A6401F, text #FFF7F2 · '=' key bg #8F3418
- Footer: icons #7A4530, date #A6401F, rate #8A6A5C
- Active ring: #A6401F

### Dark theme
- App background: #1B120E
- Card background: #261A14 · card label #F5E9E1 · amount #F8EFE8
- Number key: bg #2E1F17, text #F1E2D8 (no shadow)
- Utility key: bg #3B2A20, text #E0B9A4
- Operator key: bg #B54B26, text #FFF3EC · '=' key bg #C55A31
- Footer: icons #E0B9A4, date #D97B52, rate #A5826F
- Active ring: #C55A31

### Shared
- Currency badge colors: EUR #3B5BA5, XMR #C0562B; glyph/text on badge #FFF7F2
- Radii: cards 20px, keys 18px, badges circle
- Spacing: outer padding 10–12px, grid/stack gap 8–10px

## Interactions & Behavior (calculator semantics)
- Digits type into the active card's entry (max 10 chars; leading 0 replaced).
- ',' adds a decimal separator once (comma display by default; dot optional).
- Operators (÷ × − +): chain like a standard calculator — pressing an operator with one pending evaluates first; next digit starts a fresh entry.
- '=': evaluates pending operation; result becomes entry.
- '%': divides entry by 100. '⌫': deletes last char (falls back to 0). 'C': clears entry + pending op.
- '⇅': swaps the two currencies (badges, codes, rate line); amounts recompute.
- The non-active card always shows the converted value: `amount × valueEUR(from) / valueEUR(to)` with valueEUR(EUR)=1, valueEUR(XMR)=285.7 (placeholder — wire to a real rate API; refresh button re-fetches and updates the footer timestamp).
- Key press feedback: scale 0.94, ~100ms.

## State Management
Per screen: `entry: String`, `accumulator: Double?`, `pendingOp: Char?`, `freshEntry: Boolean`, `activeField: TOP|BOTTOM`, `currencies: Pair`, `rate source + lastRefreshed timestamp`. All display values derive from these.

## Assets
No image assets. Currency badges are drawn (colored circle + glyph); replace with real EU flag / Monero logo assets if available. Icons used as glyphs in the prototype (⌫ ⇅ ↻ ⋯) — use Material Symbols equivalents (backspace, swap_vert, refresh, more_horiz).

## Files
- `Currency Converter.dc.html` — interactive prototype, light + dark side by side (open in a browser; logic in the embedded script).
