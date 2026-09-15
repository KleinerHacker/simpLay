# fx - Font probe

`FxFontProbe` in `org.pcsoft.framework.simplay.fx` checks a `Font` against the
JavaFX text stack: whether its family is still installed, and whether it still
resolves to the face a stored `FontFingerprint` was taken from. It is the JavaFX
counterpart of the `swing` module's `SwingFontProbe`.

The JavaFX toolkit must already be initialised when a method is first called (an
`Application` is running, or a headless toolkit such as Monocle was started). A
fingerprint taken here only compares against another one taken here - AWT and
JavaFX measure the same font differently.

```kotlin
val probe = FxFontProbe()
```

## Availability

| Method | Result |
|--------|--------|
| `isFamilyAvailable(family: String): Boolean` | `true` if `family` is a JavaFX standard family (`System`, `Serif`, `SansSerif`, `Monospaced`) or a real installed family (case-insensitive). |
| `checkAvailability(font: Font): FontAvailability` | `AVAILABLE` if the family resolves to itself, `MISSING` if JavaFX fell back to the `System` family, `SUBSTITUTED` if it answered with a different real family. |

`FontAvailability` lives in `org.pcsoft.framework.simplay.uicommon`.

## Fingerprint

| Method | Result |
|--------|--------|
| `fingerprint(font: Font): FontFingerprint` | A size-independent signature of `font` as JavaFX currently resolves it. |
| `verify(font: Font, expected: FontFingerprint, tolerance: Double = FontFingerprint.DEFAULT_TOLERANCE): Boolean` | Whether `font` still resolves to the face `expected` was taken from. |
| `stamp(document: Document, overwrite: Boolean = true): Document` | A copy of `document` whose every block font carries a fresh fingerprint; run before persisting. With `overwrite = false` a font that already has one keeps it. |

## Typical use

```kotlin
val probe = FxFontProbe()

// on save
val toPersist = probe.stamp(document)

// on load, elsewhere
val measured = toPersist.measure(FxFontMeasureCalculator())
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
