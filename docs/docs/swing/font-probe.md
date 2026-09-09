# swing - Font probe

`SwingFontProbe` in `org.pcsoft.framework.simplay.swing` checks a `Font` against
the AWT text stack: whether its family is still installed, and whether it still
resolves to the face a stored `FontFingerprint` was taken from. It is the Swing
counterpart of the `fx` module's `FxFontProbe`.

Measurement runs headlessly and is cached per `(Font, text)` pair. A fingerprint
taken here only compares against another one taken here - AWT and JavaFX measure
the same font differently.

```kotlin
val probe = SwingFontProbe()
```

## Availability

| Method | Result |
|--------|--------|
| `isFamilyAvailable(family: String): Boolean` | `true` if `family` is an AWT logical family (`Dialog`, `Serif`, `SansSerif`, `Monospaced`, ...) or a real installed family (case-insensitive). |
| `checkAvailability(font: Font): FontAvailability` | `AVAILABLE` if the family resolves to itself, `MISSING` if AWT fell back to a default family, `SUBSTITUTED` if it answered with a different real family. |

`FontAvailability` lives in `org.pcsoft.framework.simplay.uicommon`.

## Fingerprint

| Method | Result |
|--------|--------|
| `fingerprint(font: Font): FontFingerprint` | A size-independent signature of `font` as AWT currently resolves it. |
| `verify(font: Font, expected: FontFingerprint, tolerance: Double = FontFingerprint.DEFAULT_TOLERANCE): Boolean` | Whether `font` still resolves to the face `expected` was taken from. |
| `stamp(document: Document, overwrite: Boolean = true): Document` | A copy of `document` whose every block font carries a fresh fingerprint; run before persisting. With `overwrite = false` a font that already has one keeps it. |

## Typical use

```kotlin
val probe = SwingFontProbe()

// on save
val toPersist = probe.stamp(document)

// on load, elsewhere
val measured = toPersist.measure(SwingFontMeasureCalculator())
if (measured.fingerprintDeviations.isNotEmpty()) {
    // a font is missing or was silently replaced - warn or offer a remap
}

// ad-hoc, one family
when (probe.checkAvailability(style.font)) {
    FontAvailability.AVAILABLE -> Unit
    FontAvailability.SUBSTITUTED, FontAvailability.MISSING -> warnFontMissing()
}
```

See [engine - Font fingerprint and availability](../engine/implementation.md#font-fingerprint-and-availability)
for the model side.
