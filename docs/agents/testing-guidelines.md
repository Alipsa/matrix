# Testing Guidelines

Detailed testing patterns for this repository, companion to [AGENTS.md](../../AGENTS.md).
JUnit Jupiter is the primary test framework; tests live in each module's `src/test` tree.

## Groovy + JUnit Assertions

**CRITICAL:** Groovy test modules must include the `se.alipsa.groovy:groovier-junit` test dependency so Groovy-friendly JUnit assertions work correctly under static compilation.

When writing Groovy tests:
- Do **not** coerce GStrings to `String` just to satisfy `assertEquals` or other JUnit assertions.
- Do **not** add `.toString()`, `as String`, or `String expected = "..."` workarounds for interpolated values in assertions.
- Prefer idiomatic Groovy assertions such as:
```groovy
assertEquals("Cannot auto-detect format for path '$rootPath': no file extension was found", exception.message)
assertEquals("row${rows}", result[rows - 1, 'name'])
```

If a Groovy test needs GString-to-String coercion for JUnit assertions to compile or run, treat that as a missing `groovier-junit` dependency issue and fix the build instead of changing the test style.

## Testing SVG Chart Output (matrix-charts, matrix-ggplot)

**IMPORTANT:** Prefer direct SVG object access (`svg.descendants()`, children, element attributes) for structural chart assertions. It is faster and clearer for element-count, element-type, parent/child, and attribute checks. Use `svg.toXml()` or `SvgWriter.toXml()` when the test specifically needs serialized XML, CSS, exact text snippets, or parser-level assertions. **DO NOT use `svg.toString()`** - it returns the Java object representation (`"se.alipsa.groovy.svg.Svg@hashcode"`), not the SVG XML content.

### Correct Pattern

```groovy
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.Circle
import se.alipsa.groovy.svg.Path
import se.alipsa.matrix.core.Matrix
import static se.alipsa.matrix.gg.GgPlot.*

@Test
void testChartRendering() {
  def data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows([[1, 2], [2, 4], [3, 6]])
      .build()

  def chart = ggplot(data, aes(x: 'x', y: 'y')) + geom_line()
  Svg svg = chart.render()
  assertNotNull(svg)

  // Correct - use direct object access for structural assertions
  def paths = svg.descendants().findAll { it instanceof Path }
  assertTrue(paths.size() > 0)

  // Use serialization only for XML/text/CSS assertions
  // String svgContent = SvgWriter.toXml(svg)
  // assertTrue(svgContent.contains('stroke-dasharray'))
}
```

### Incorrect Pattern (DO NOT USE)

```groovy
@Test
void testChartRendering() {
  def chart = ggplot(data, aes(x: 'x', y: 'y')) + geom_line()
  def svg = chart.render()

  // Wrong - svg.toString() returns "se.alipsa.groovy.svg.Svg@66933239"
  assertTrue(svg.toString().contains('<svg'))  // Will FAIL!
}
```

### When to Serialize SVG

Use `svg.toXml()` or `SvgWriter.toXml()` when testing serialized output, for example CSS attributes, exact labels, generated XML snippets, or XML parser behavior:

```groovy
String svgContent = SvgWriter.toXml(svg)
assertTrue(svgContent.contains('stroke-dasharray'))
```

If you need to test specific SVG elements or attributes programmatically after serialization, use `SvgReader.fromXml()` to parse the XML and navigate the resulting object model.

### Required Imports for Chart Tests

```groovy
import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Circle
import se.alipsa.groovy.svg.Line
import se.alipsa.groovy.svg.Path
import se.alipsa.groovy.svg.Rect
import se.alipsa.groovy.svg.Text
import se.alipsa.groovy.svg.io.SvgWriter  // Include when serialization is needed
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*
```

### Available Methods for Direct Access

**Tree Navigation:**
- `svg.descendants()` - Get all nested elements recursively (most common)
- `svg.getChildren()` - Get only direct children
- `element.parent` - Navigate up the tree

**Element Filtering:**
```groovy
def descendants = svg.descendants()

// Filter by type
def circles = descendants.findAll { it instanceof Circle }
def rects = descendants.findAll { it instanceof Rect }
def lines = descendants.findAll { it instanceof Line }
def paths = descendants.findAll { it instanceof Path }
def textElements = descendants.findAll { it instanceof Text }

// Multiple types
assertTrue(circles.size() > 0 || paths.size() > 0, "Should contain elements")
```

**Text Content:**
```groovy
def textElements = svg.descendants().findAll { it instanceof Text }
def allText = textElements.collect { it.content }.join(' ')
assertTrue(allText.contains('Chart Title'))
```

**SVG Properties:**
```groovy
// Direct access to SVG attributes
int width = svg.width as int
int height = svg.height as int
assertTrue(width >= 800)
assertEquals(600, height)
```

### When to Keep File Writes

Keep file I/O for **visual regression testing** (5-10% of tests):
- One test per major geom type
- Complex multi-element charts
- Coordinate system examples

```groovy
@Test
void testComplexVisualization() {
    Svg svg = chart.render()

    // Use direct access for assertions
    def paths = svg.descendants().findAll { it instanceof Path }
    assertTrue(paths.size() > 0)

    // Keep file write for manual inspection
    File outputFile = new File('build/visual_regression_test.svg')
    write(svg, outputFile)
}
```

### Performance Impact

Direct object access vs serialization:
- **Speed**: 1.3x faster for structural checks
- **Memory**: No string allocation for large SVGs
- **Reliability**: Type-safe, no string parsing for element structure

**Benchmark Results:**
```
Direct access:     3ms per test
Serialization:     4ms per test
File I/O:         15ms+ per test
```

For a test suite with 200+ tests, this optimization saves ~30 seconds per run.
