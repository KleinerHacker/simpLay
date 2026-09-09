# swing - Styling the paper sheet component

`PaperSheetView` has no equivalent of JavaFX CSS. Its sheet chrome, drop shadow,
selection highlight, caret and the two layout values are read from the
`PaperSheetView.*` keys of the active Look-and-Feel, mirrored by Kotlin
properties, and a programmatic setter always wins.

## The keys

`PaperSheetLookAndFeel.installDefaults()` seeds every missing key with its
built-in default. The key names are also available as constants on
`PaperSheetLookAndFeel`.

| Key | Kotlin property | Type | Default |
|-----|-----------------|------|---------|
| `PaperSheetView.sheetBackground` | `sheetBackground` | `java.awt.Paint` | white |
| `PaperSheetView.sheetBorderColor` | `sheetBorderColor` | `java.awt.Paint` | `#8C8C8C` |
| `PaperSheetView.sheetBorderWidth` | `sheetBorderWidth` | `Double` | `1.0` |
| `PaperSheetView.shadowColor` | `shadowColor` | `java.awt.Paint` | `rgba(0,0,0,0.25)` |
| `PaperSheetView.shadowOffset` | `shadowOffset` | `Double` | `4.0` |
| `PaperSheetView.selectionColor` | `selectionColor` | `java.awt.Paint` | `rgba(66,133,244,0.35)` |
| `PaperSheetView.selectionColorReadonly` | (none) | `java.awt.Paint` | `rgba(120,120,120,0.30)` |
| `PaperSheetView.caretColor` | `caretColor` | `java.awt.Color` | `#141414` |
| `PaperSheetView.outerMargin` | `outerMargin` | `Double` | `24.0` |
| `PaperSheetView.pageGap` | `pageGap` | `Double` | `16.0` |

The fill values are `java.awt.Paint`, so a `GradientPaint` or `TexturePaint`
works too; `caretColor` is a plain `Color`. `selectionColorReadonly` is used for
the selection highlight while `mode` is `PaperSheetMode.READONLY` and no
`selectionColor` was set programmatically.

## Overriding from the Look-and-Feel

Put the values into `UIManager` before the view is created:

```kotlin
UIManager.put("PaperSheetView.sheetBackground", Color(0x2B, 0x2B, 0x2B))
UIManager.put("PaperSheetView.sheetBorderColor", Color(0x55, 0x59, 0x5D))
UIManager.put("PaperSheetView.shadowColor", Color(0, 0, 0, 150))
UIManager.put("PaperSheetView.selectionColor", Color(0xFF, 0xC8, 0x50, 100))
UIManager.put("PaperSheetView.caretColor", Color(0xF0, 0xF0, 0xF0))

val view = PaperSheetView()   // picks up the values above
```

`PaperSheetLookAndFeel.applyTo(view)` re-applies the current keys to an existing
view, skipping every value the caller set programmatically.

## Overriding per instance

```kotlin
view.sheetBackground = Color.WHITE
view.selectionColor = Color(0x42, 0x85, 0xF4, 90)
```

A programmatic setter is recorded, so a later Look-and-Feel change no longer
touches that value.
