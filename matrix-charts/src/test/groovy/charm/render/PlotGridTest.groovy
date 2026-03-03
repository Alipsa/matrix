package charm.render

import org.dom4j.Attribute
import org.dom4j.Element
import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.Text
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.PlotGrid
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.charm.Charts.plot
import static se.alipsa.matrix.charm.Charts.plotGrid

class PlotGridTest {

  @Test
  void test2x2Grid() {
    List<Chart> charts = (1..4).collect { buildSimpleChart() }
    PlotGrid grid = plotGrid(charts, 2)
    Svg svg = grid.render(800, 600)
    assertNotNull(svg)

    List<Svg> nestedSvgs = svg.descendants(Svg)
    assertEquals(4, nestedSvgs.size(), 'Should have 4 nested <svg> elements for 2x2 grid')
  }

  @Test
  void testUnevenLayout() {
    List<Chart> charts = (1..3).collect { buildSimpleChart() }
    PlotGrid grid = plotGrid(charts, 2)
    Svg svg = grid.render(800, 600)

    List<Svg> nestedSvgs = svg.descendants(Svg)
    assertEquals(3, nestedSvgs.size(), 'Should have 3 nested <svg> elements (one slot empty)')
  }

  @Test
  void testRelativeWidths() {
    List<Chart> charts = (1..2).collect { buildSimpleChart() }
    PlotGrid grid = new PlotGrid(charts, 2, null, [1.0, 2.0])
    Svg svg = grid.render(900, 600)

    List<Svg> nestedSvgs = svg.descendants(Svg)
    assertEquals(2, nestedSvgs.size())

    int w0 = nestedSvgs[0].getAttribute('width') as int
    int w1 = nestedSvgs[1].getAttribute('width') as int
    // With spacing=10 and 2 cols: usable = 900-10 = 890
    // col0 weight 1/3 => ~296, col1 weight 2/3 => ~594
    assertTrue(w1 > w0, "Second column (w=$w1) should be wider than first (w=$w0)")
    // Roughly 2:1 ratio within tolerance
    BigDecimal ratio = w1 / w0
    assertTrue(ratio > 1.5 && ratio < 2.5, "Width ratio should be ~2.0, got $ratio")
  }

  @Test
  void testRelativeHeights() {
    List<Chart> charts = (1..2).collect { buildSimpleChart() }
    PlotGrid grid = new PlotGrid(charts, 1, null, null, [1.0, 3.0])
    Svg svg = grid.render(800, 800)

    List<Svg> nestedSvgs = svg.descendants(Svg)
    assertEquals(2, nestedSvgs.size())

    int h0 = nestedSvgs[0].getAttribute('height') as int
    int h1 = nestedSvgs[1].getAttribute('height') as int
    assertTrue(h1 > h0, "Second row (h=$h1) should be taller than first (h=$h0)")
    BigDecimal ratio = h1 / h0
    assertTrue(ratio > 2.0 && ratio < 4.0, "Height ratio should be ~3.0, got $ratio")
  }

  @Test
  void testTitleRendering() {
    List<Chart> charts = [buildSimpleChart()]
    PlotGrid grid = new PlotGrid(charts, 1, null, null, null, 'Test Dashboard')
    Svg svg = grid.render(800, 600)

    List<Text> textElements = svg.descendants(Text)
    boolean titleFound = textElements.any { Text t -> t.element.text == 'Test Dashboard' }
    assertTrue(titleFound, 'Grid title text should be present in SVG')
  }

  @Test
  void testNoSvgIdCollisions() {
    // Two identical charts should have prefixed IDs that don't collide
    Chart chart = buildChartWithColorMapping()
    PlotGrid grid = plotGrid([chart, chart], 2)
    Svg svg = grid.render(800, 600)

    List<String> ids = collectIds(svg)

    Set<String> unique = new HashSet<>(ids)
    assertEquals(unique.size(), ids.size(),
        "All IDs should be unique. Duplicates: ${ids.findAll { String id -> ids.count(id) > 1 }.unique()}")
  }

  @Test
  void testClipPathReferencesValid() {
    Chart chart = buildChartWithColorMapping()
    PlotGrid grid = plotGrid([chart, chart], 2)
    Svg svg = grid.render(800, 600)

    Set<String> definedIds = collectIds(svg) as Set<String>
    Set<String> hrefAttrs = ['href', 'xlink:href'] as Set<String>

    // Traverse element tree and validate all url(#...) and href="#..." references
    collectAllElements(svg.element).each { Element elem ->
      (elem.attributes() as List<Attribute>).each { Attribute attr ->
        String val = attr.value
        if (val == null) return

        // Check url(#ref) references
        int searchFrom = 0
        while (true) {
          int start = val.indexOf('url(#', searchFrom)
          if (start < 0) break
          start += 5
          int end = val.indexOf(')', start)
          if (end > start) {
            String refId = val.substring(start, end)
            assertTrue(definedIds.contains(refId),
                "url(#$refId) references an undefined ID. Defined IDs: $definedIds")
          }
          searchFrom = (end > start) ? end + 1 : start
        }

        // Check href="#..." and xlink:href="#..." references
        if (hrefAttrs.contains(attr.name) || hrefAttrs.contains(attr.qualifiedName)) {
          if (val.startsWith('#')) {
            String refId = val.substring(1)
            assertTrue(definedIds.contains(refId),
                "href=\"#$refId\" references an undefined ID. Defined IDs: $definedIds")
          }
        }
      }
    }
  }

  @Test
  void testPerSubplotLegendsPreserved() {
    Chart chart = buildChartWithColorMapping()
    PlotGrid grid = plotGrid([chart, chart], 2)
    // Should render without errors even with color-mapped charts
    Svg svg = grid.render(800, 600)
    assertNotNull(svg)

    List<Svg> nestedSvgs = svg.descendants(Svg)
    assertTrue(nestedSvgs.size() >= 2, 'Should contain nested SVG subplots')
  }

  @Test
  void testDslConstruction() {
    Chart c1 = buildSimpleChart()
    Chart c2 = buildSimpleChart()

    PlotGrid grid = plotGrid {
      add c1, c2
      ncol 2
      title 'DSL Dashboard'
      spacing 15
      widths([1.0, 2.0])
    }

    assertEquals(2, grid.charts.size())
    assertEquals(2, grid.ncol)
    assertEquals(1, grid.nrow)
    assertEquals('DSL Dashboard', grid.title)
    assertEquals(15, grid.spacing)
    assertNotNull(grid.widths)

    Svg svg = grid.render(800, 600)
    assertNotNull(svg)
  }

  @Test
  void testEmptyChartsThrows() {
    assertThrows(IllegalArgumentException) {
      new PlotGrid([])
    }
  }

  @Test
  void testNcolAutoComputesNrow() {
    List<Chart> charts = (1..5).collect { buildSimpleChart() }
    PlotGrid grid = plotGrid(charts, 3)
    assertEquals(2, grid.nrow, 'ceil(5/3) should be 2')
  }

  private static Chart buildSimpleChart() {
    Matrix data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 2], [2, 4], [3, 6]])
        .build()

    plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers { geomPoint() }
    }.build()
  }

  private static Chart buildChartWithColorMapping() {
    Matrix data = Matrix.builder()
        .columnNames(['x', 'y', 'group'])
        .rows([[1, 2, 'A'], [2, 4, 'B'], [3, 6, 'A'], [4, 8, 'B']])
        .build()

    plot(data) {
      mapping { x = 'x'; y = 'y'; color = 'group' }
      layers { geomPoint() }
    }.build()
  }

  /** Collects all id attribute values from the SVG DOM tree. */
  private static List<String> collectIds(Svg svg) {
    collectAllElements(svg.element)
        .collect { it.attributeValue('id') }
        .findAll { it != null && !it.isEmpty() }
  }

  /** Recursively collects an element and all its descendants. */
  private static List<Element> collectAllElements(Element root) {
    List<Element> result = [root]
    root.elements().each { Element child ->
      result.addAll(collectAllElements(child))
    }
    result
  }
}
