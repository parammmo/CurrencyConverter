# Handoff: Currency Converter v2 (Android)

## Overview
Reinvented, minimalist version of the calculator-style currency converter (EUR ⇄ XMR). Signature elements: the app icon's two overlapping circles reused as the swap control, a tile-less "floating glyph" keypad where the only filled key is a round '=', focus shown by dimming instead of borders, and an in-app light/dark theme toggle. Target: native Android (Kotlin / Jetpack Compose, Material 3) in Android Studio.

## About the Design Files
`Currency Converter v2.dc.html` is a **design reference created in HTML** — an interactive prototype showing intended look and behavior, not production code. Recreate it in the Android codebase with Jetpack Compose (or the project's existing UI stack). The HTML shows two phone frames side by side purely for presentation; each frame is the same single screen (one starts light, one dark).

## Fidelity
**High-fidelity.** Colors, spacing, radii, and typography are final; map px → dp at the 412×892 reference viewport.

## Screen: Converter
Vertical column filling the viewport under the status bar. Three zones separated by 1px hairlines: currency section, keypad, footer.

### 1. Currency section (two rows + swap control)
- Two rows (top = input by default), each: padding 34px 24px 30px (top row) / 30px 24px 34px (bottom row), row layout, space-between, baseline-aligned.
  - Left: currency code, 13px / 600, letter-spacing 0.14em. Color = accent when this row is active, secondary otherwise.
  - Right: amount, 56px / 500, line-height 1.15, tabular numerals, single line, ellipsis past ~270px.
- Inactive row: whole row at 45% opacity. No borders or rings.
- 1px hairline divider between the rows, inset 24px left/right.
- **Swap control** (app-icon motif): overlaps the divider, absolutely positioned, left 24px, vertically centered on the divider. Two 48px circles: back circle offset 26px right, muted rosy at ~50% opacity; front circle filled accent with a white (#FFF5EE) ⇅ glyph 20px. Tap = swap currencies (codes + rate line update, amounts recompute).
- Tapping a row makes it the input side and resets entry to 0.

### 2. Keypad — tile-less, 4-column grid, 5 rows, 4px gap, padding 6px 14px, fills remaining height, hairline top border
Rows: [C ⌫ % ÷] [7 8 9 ×] [4 5 6 −] [1 2 3 +] [0 , ⇅ =]
- Keys have **no background** — glyphs float on the app background. Radius 20px touch area.
- Digits: 28px / 500, primary text color. Operators (÷ × − +): 30px / 500, accent color. Utility (C ⌫ % ⇅): 22–24px / 500, secondary color.
- '=' is the only filled key: 62px circle, accent-filled bg, #FFF5EE glyph 30px, centered in its cell.
- Pressed state: scale 0.9 + opacity 0.6 ('=': scale 0.92), ~100ms.

### 3. Footer — row, space-between, padding 10px 20px 8px, hairline top border
- Left: refresh icon (↻), secondary color, 18px.
- Center: one line, 12px secondary: `1 EUR = 0,0035 XMR · DD/MM/YYYY HH:mm`.
- Right group (16px gap): **theme toggle** + overflow (⋯ 18px).
  - Theme toggle, 22px touch target, drawn shapes in the accent color (no icon font):
    - Light mode shows a **moon**: 13px circle with crescent via inset shadow (inset -4.5px -3px 0 0 accent).
    - Dark mode shows a **sun**: 15px circle with 1.5px accent ring + centered 7px accent dot.
  - Tap toggles the whole screen between themes (background animates ~350ms); status bar icons follow.

## Design Tokens

Font: Instrument Sans (Google Fonts), weights 400–700; fallback sans-serif.

### Light theme
- App background #F6F0EA · hairlines rgba(90,50,30,0.14)
- Amount #2E1B12 · digits #3A251A
- Accent (operators, active code, '=' bg, swap front, toggle): #A6401F
- Secondary (utility keys, footer, inactive code): #8A6A5C
- Swap back circle: rgba(201,146,126,0.5)

### Dark theme
- App background #1B120E · hairlines rgba(255,220,195,0.14)
- Amount #F8EFE8 · digits #EFDFD4
- Accent: #D97B52 · '=' / swap front bg: #B54B26
- Secondary: #9C7A67
- Swap back circle: rgba(165,115,92,0.45)

### Shared
- On-accent text: #FFF5EE
- Radii: key touch areas 20px; swap circles, badges, '=' fully round
- Inactive row opacity 0.45 · theme transition ~350ms

## Interactions & Behavior (calculator semantics)
- Digits type into the active row's entry (max 10 chars; leading 0 replaced).
- ',' adds one decimal separator (comma display default, dot optional).
- Operators chain like a standard calculator; '=' evaluates; '%' divides entry by 100; '⌫' deletes last char (falls back to 0); 'C' clears entry + pending op.
- '⇅' (swap circle or keypad key): swaps the two currencies.
- Non-active row always shows the converted value: `amount × valueEUR(from) / valueEUR(to)`, valueEUR(EUR)=1, valueEUR(XMR)=285.7 (placeholder — wire to a real rate API; ↻ re-fetches and updates the footer timestamp).

## State Management
`theme: LIGHT|DARK`, `entry: String`, `accumulator: Double?`, `pendingOp: Char?`, `freshEntry: Boolean`, `activeField: TOP|BOTTOM`, `currencies: Pair`, rate source + lastRefreshed. All display values derive from these. Persist theme choice (DataStore).

## Assets
No image assets. Swap control, sun, and moon are drawn shapes (circles/crescent) — implement as Compose Canvas/Box, not icon fonts. Glyph fallbacks used in the prototype: ⌫ ⇅ ↻ ⋯ → Material Symbols backspace, swap_vert, refresh, more_horiz.

## Files
- `Currency Converter v2.dc.html` — interactive prototype, both themes side by side with working toggle (open in a browser; logic in the embedded script).
