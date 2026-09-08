# engine - Rendering a measured document

The `engine` module stops at geometry. It produces a
[`MeasuredDocument`](measured-model.md) with absolute positions but draws nothing.
Rendering is the consumer's job: walk the measured tree and emit whatever the
target needs (a canvas, a PDF content stream, a print graphics context). This page
describes the walk; it is conceptual and does not imply a platform module.

## The walk

```text
MeasuredDocument
└── pages: List<MeasuredPage>          // one output surface each
    └── blocks: List<MeasuredTextBlock>
        └── lines: List<MeasuredLine>
            └── parts: List<MeasuredTextPart>   // one draw call each
```

1. **Per page** - create a surface of `page.effectiveSize`. Establish the content
   origin from `page.contentArea` (`Rect` relative to the page origin, already
   shrunk by the margins). For a `MeasuredSinglePage` the height in
   `effectiveSize` may exceed the raw layout height.
2. **Per block** - `block.bounds` is the block rectangle relative to the content
   area. Nothing usually needs to be drawn for the block itself; it is a grouping
   level.
3. **Per line** - `line.lineBox` is relative to the content area. The text
   baseline is at `lineBox.y + line.baseline` (which equals `lineBox.y +
   line.ascent`). `line.alignment` and `line.lastLine` have already been applied
   to the part positions; you do not re-align.
4. **Per part** - `part.bounds` is the part rectangle relative to the content
   area. Draw `part.text` with the block's font so that its baseline sits on the
   line baseline. `part.bounds.x` is the horizontal pen position.

Absolute position of a part on the page:

```text
x = contentArea.x + part.bounds.x
y = contentArea.y + line.lineBox.y + line.baseline   // baseline, not top
```

## Values you consume

| Value | Where | Use |
|-------|-------|-----|
| `MeasuredPage.effectiveSize` | page | size of the output surface |
| `MeasuredPage.contentArea` | page | origin and extent of the drawable area |
| `MeasuredPage.pageIndex` | page | continuous page number across the document |
| `MeasuredTextBlock.bounds` | block | block rectangle (grouping) |
| `MeasuredTextBlock.style` | block | `MeasuredFont` (`family`, `size`, `weight`, `style`) and `FontMetrics` for the draw call |
| `MeasuredLine.lineBox` | line | line rectangle relative to the content area |
| `MeasuredLine.baseline` / `ascent` / `descent` | line | vertical placement of the glyphs |
| `MeasuredTextPart.bounds` | part | rectangle of the individual word or symbol |
| `MeasuredTextPart.text` | part | the string to draw |

## Units

All layout values are unit-less `Double`s. The engine does no unit conversion.
The renderer maps them to the target unit with a single factor - for example
treat one layout unit as one PostScript point for PDF, or multiply by the device
scale for a screen canvas. Because the mapping is linear, measuring once and
scaling on render keeps the layout stable across output resolutions.

## Example: a console renderer

The smallest useful renderer targets a character grid. It fixes a monospace
**cell** as the unit (`CELL_W` units wide, `CELL_H` units tall), uses a matching
`FontMeasureCalculator` so the layout snaps to that grid, and then stamps every
`MeasuredTextPart` into a `CharArray` grid the size of the page.

```kotlin
import org.pcsoft.framework.simplay.engine.FontMeasureCalculator
import org.pcsoft.framework.simplay.engine.SimpLayEngine
import org.pcsoft.framework.simplay.engine.geometry.Margins
import org.pcsoft.framework.simplay.engine.geometry.Size
import org.pcsoft.framework.simplay.engine.geometry.TextMetrics
import org.pcsoft.framework.simplay.engine.measure.MeasuredDocument
import org.pcsoft.framework.simplay.engine.measure.MeasuredPage
import org.pcsoft.framework.simplay.engine.model.Document
import org.pcsoft.framework.simplay.engine.model.FlowPage
import org.pcsoft.framework.simplay.engine.model.Font
import org.pcsoft.framework.simplay.engine.model.PageLayout
import org.pcsoft.framework.simplay.engine.model.TextBlock
import org.pcsoft.framework.simplay.engine.model.TextStyle
import kotlin.math.roundToInt

/** One monospace cell is this many layout units. */
private const val CELL_W = 6.0
private const val CELL_H = 12.0

/** A measurer that matches the console cell grid: every glyph is one cell. */
val consoleMeasurer = FontMeasureCalculator { _, text ->
    TextMetrics(width = text.length * CELL_W, ascent = CELL_H, descent = 0.0)
}

fun MeasuredDocument.renderToConsole(): String = buildString {
    pages.forEach { page ->
        if (isNotEmpty()) append("\n").append("-".repeat(40)).append("\n")
        append("Page ${page.pageIndex + 1}\n")
        append(page.renderToConsole())
    }
}

private fun MeasuredPage.renderToConsole(): String {
    val cols = (effectiveSize.width / CELL_W).roundToInt().coerceAtLeast(1)
    val rows = (effectiveSize.height / CELL_H).roundToInt().coerceAtLeast(1)
    val grid = Array(rows) { CharArray(cols) { ' ' } }

    for (block in blocks) {
        for (line in block.lines) {
            // top of the line box -> grid row (a console cell has no sub-cell baseline)
            val row = ((contentArea.y + line.lineBox.y) / CELL_H).roundToInt()
            if (row !in 0 until rows) continue
            for (part in line.parts) {
                val startCol = ((contentArea.x + part.bounds.x) / CELL_W).roundToInt()
                part.text.forEachIndexed { i, ch ->
                    val col = startCol + i
                    if (col in 0 until cols) grid[row][col] = ch
                }
            }
        }
    }
    return grid.joinToString("\n") { it.concatToString().trimEnd() }
        .lines()
        .dropWhile { it.isEmpty() }
        .dropLastWhile { it.isEmpty() }
        .joinToString("\n")
}

fun main() {
    val style = TextStyle(font = Font(family = "Mono", size = CELL_H))
    val document = Document(
        pages = listOf(
            FlowPage(
                layout = PageLayout(
                    size = Size(width = 200.0, height = 200.0),
                    margins = Margins(left = 20.0, top = 20.0, right = 20.0, bottom = 20.0),
                ),
                blocks = listOf(
                    TextBlock.of("The quick brown fox jumps over the lazy dog.", style),
                ),
            ),
        ),
    )

    val engine = SimpLayEngine.builder(consoleMeasurer).build()
    val measured = engine.measure(document)
    println(measured.renderToConsole())
}
```

The content width is `200 - 20 - 20 = 160` units, i.e. `160 / 6 ≈ 26` cells, so
the greedy line breaker wraps the block after `jumps`. The left margin of `20`
units maps to `20 / 6 ≈ 3` leading cells, so `main` prints:

```text
Page 1
   The quick brown fox jumps
   over the lazy dog.
```

Swap `consoleMeasurer` for a real text stack and the same walk drives a canvas or
a PDF - only the per-part draw call changes.

## Where platform modules plug in

A platform module (`fx`, `swing`, `j-pdf`, `j-print`) supplies two things:

* a `FontMeasureCalculator` backed by that platform's text stack, passed to
  `SimpLayEngine.builder(...)`;
* a renderer that performs the walk above against that platform's drawing API.

The measured model is the contract between the two. Nothing in `engine` depends on
the platform side.
