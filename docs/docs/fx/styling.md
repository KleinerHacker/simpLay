# fx - Styling the paper sheet component

[`PaperSheetView`](paper-sheet-component.md) is styleable through the standard
JavaFX CSS mechanism. The sheet chrome, the drop shadow, the selection highlight,
the caret and the two layout values are all `-fx-` properties, mirrored by Kotlin
properties, and a default user-agent stylesheet ships with the module.

## Style class and pseudo-classes

* Style class: `paper-sheet-view` (added to every instance).
* `:static`, `:selectable`, `:navigable`, `:editable` - exactly one of them is
  active, matching the current `PaperSheetMode`.
* `:focused` - the inherited JavaFX `Node` pseudo-class, working as usual.
* `:overwrite` - active while `caretMode` is `CaretMode.OVERWRITE`; independent
  of and combinable with the mode pseudo-classes (e.g. `:editable:overwrite`).

## Properties

| CSS property | Kotlin property | Type | Default | Effect |
|--------------|-----------------|------|---------|--------|
| `-fx-sheet-background` | `sheetBackground` | `Paint` | `white` | Fill of every sheet. |
| `-fx-sheet-border-color` | `sheetBorderColor` | `Paint` | `#8c8c8c` | Stroke colour of every sheet border. |
| `-fx-sheet-border-width` | `sheetBorderWidth` | `Double` | `1.0` | Stroke width of every sheet border, in layout units. |
| `-fx-shadow-color` | `shadowColor` | `Paint` | `rgba(0,0,0,0.25)` | Fill of the drop shadow behind every sheet. |
| `-fx-shadow-offset` | `shadowOffset` | `Double` | `4.0` | Offset of the drop shadow to the lower right, in layout units. |
| `-fx-selection-color` | `selectionColor` | `Paint` | `rgba(66,133,244,0.35)` | Fill of the text selection highlight. |
| `-fx-caret-color` | `caretColor` | `Color` | `#141414` | Stroke colour of the edit caret. |
| `-fx-outer-margin` | `outerMargin` | `Double` | `24.0` | Space around the whole sheet stack, in layout units. |
| `-fx-page-gap` | `pageGap` | `Double` | `16.0` | Vertical space between two sheets, in layout units. |

Every colour value is a `Paint`, so a gradient or an image pattern works too -
except `-fx-caret-color`, which is a plain `Color`.

A programmatic setter (`view.selectionColor = ...`) always wins over the
user-agent stylesheet, exactly as elsewhere in JavaFX.

## The default user-agent stylesheet

`getUserAgentStylesheet()` returns a bundled `paper-sheet-view.css` whose values
are identical to the built-in property defaults - removing a rule changes
nothing. It exists so the values can be discovered and overridden from a caller
stylesheet. It also greys the selection highlight in the non-editable modes:

```css
.paper-sheet-view {
    -fx-sheet-background: white;
    -fx-sheet-border-color: #8c8c8c;
    -fx-sheet-border-width: 1.0;
    -fx-shadow-color: rgba(0, 0, 0, 0.25);
    -fx-shadow-offset: 4.0;
    -fx-selection-color: rgba(66, 133, 244, 0.35);
    -fx-caret-color: #141414;
    -fx-outer-margin: 24.0;
    -fx-page-gap: 16.0;
}

.paper-sheet-view:selectable,
.paper-sheet-view:navigable {
    -fx-selection-color: rgba(120, 120, 120, 0.30);
}
```

## Overriding from your own stylesheet

Add a stylesheet to the scene (or to any parent) and restyle the class. A
scene-level stylesheet overrides the user-agent one:

```css
/* dark-sheets.css */
.paper-sheet-view {
    -fx-sheet-background: linear-gradient(to bottom, #33373b, #2b2b2b);
    -fx-sheet-border-color: #55595d;
    -fx-shadow-color: rgba(0, 0, 0, 0.6);
    -fx-shadow-offset: 6.0;
    -fx-selection-color: rgba(255, 200, 80, 0.40);
    -fx-caret-color: #f0f0f0;
}

.paper-sheet-view:selectable,
.paper-sheet-view:navigable {
    -fx-selection-color: rgba(180, 180, 180, 0.35);
}
```

```kotlin
scene.stylesheets += javaClass.getResource("dark-sheets.css")!!.toExternalForm()
```

## Next

* [Paper sheet component](paper-sheet-component.md) - the host control and its
  non-styling properties.
