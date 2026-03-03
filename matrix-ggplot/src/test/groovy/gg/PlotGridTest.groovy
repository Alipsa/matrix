package gg

import org.dom4j.Element
import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.GgChart

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Integration tests for the {@code plot_grid} API in GgPlot, verifying
 * that GgChart instances compile and compose correctly into PlotGrid SVGs.
 */
class PlotGridTest {

  private static final Matrix DATA = Matrix.builder()
      .columnNames(['x', 'y', 'group'])
      .rows([[1, 2, 'A'], [2, 4, 'B'], [3, 6, 'A'], [4, 8, 'B']])
      .build()

  @Test
  void testPlotGridVarargs() {
    GgChart c1 = ggplot(DATA, aes(x: 'x', y: 'y')) + geom_point()
    GgChart c2 = ggplot(DATA, aes(x: 'x', y: 'y')) + geom_line()

    def grid = plot_grid(c1, c2)
    Svg svg = grid.render(800, 600)
    assertNotNull(svg)

    List<Svg> nested = svg.descendants(Svg)
    assertEquals(2, nested.size(), 'Should have 2 nested <svg> elements')
  }

  @Test
  void testPlotGridListWithNcol() {
    GgChart c1 = ggplot(DATA, aes(x: 'x', y: 'y')) + geom_point()
    GgChart c2 = ggplot(DATA, aes(x: 'x', y: 'y')) + geom_line()
    GgChart c3 = ggplot(DATA, aes(x: 'x', y: 'y')) + geom_point()
    GgChart c4 = ggplot(DATA, aes(x: 'x', y: 'y')) + geom_line()

    def grid = plot_grid([c1, c2, c3, c4], 2)
    Svg svg = grid.render(800, 600)

    List<Svg> nested = svg.descendants(Svg)
    assertEquals(4, nested.size(), 'Should have 4 nested <svg> elements for 2x2 grid')
  }

  @Test
  void testPlotGridMapParams() {
    GgChart c1 = ggplot(DATA, aes(x: 'x', y: 'y')) + geom_point()
    GgChart c2 = ggplot(DATA, aes(x: 'x', y: 'y')) + geom_line()

    def grid = plot_grid(
        charts: [c1, c2],
        ncol: 2,
        title: 'Test Grid',
        spacing: 15
    )
    Svg svg = grid.render(800, 600)
    assertNotNull(svg)

    List<Svg> nested = svg.descendants(Svg)
    assertEquals(2, nested.size(), 'Should have 2 nested <svg> elements')
    assertEquals('Test Grid', grid.title)
    assertEquals(15, grid.spacing)
  }

  @Test
  void testPlotGridNoIdCollisions() {
    GgChart c1 = ggplot(DATA, aes(x: 'x', y: 'y', color: 'group')) + geom_point()
    GgChart c2 = ggplot(DATA, aes(x: 'x', y: 'y', color: 'group')) + geom_point()

    def grid = plot_grid([c1, c2], 2)
    Svg svg = grid.render(800, 600)

    // Collect all id attribute values via DOM traversal
    List<String> ids = collectIds(svg)

    Set<String> unique = new HashSet<>(ids)
    assertEquals(unique.size(), ids.size(),
        "All IDs should be unique. Duplicates: ${ids.findAll { String id -> ids.count(id) > 1 }.unique()}")
  }

  @Test
  void testPlotGridNullParamsThrows() {
    assertThrows(IllegalArgumentException) {
      plot_grid((Map) null)
    }
  }

  @Test
  void testPlotGridEmptyChartsThrows() {
    assertThrows(IllegalArgumentException) {
      plot_grid(charts: [])
    }
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
