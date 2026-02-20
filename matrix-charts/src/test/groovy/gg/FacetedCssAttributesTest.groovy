package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for CSS attributes in faceted charts.
 */
class FacetedCssAttributesTest {

  @Test
  void testFacetWrapWithCssAttributes() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'group')
        .rows([
          [1, 2, 'A'],
          [2, 3, 'A'],
          [1, 4, 'B'],
          [2, 5, 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        css_attributes(enabled: true) +
        geom_point() +
        facet_wrap('group')

    Svg svg = chart.render()
    String svgString = svg.toXml()

    // Should contain panel coordinates in IDs
    assertTrue(svgString.contains('id="gg-panel-0-0-layer-0-point-'),
        "SVG should contain panel coordinates in IDs for facet_wrap")
  }

  @Test
  void testFacetGridWithCssAttributes() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'row_facet', 'col_facet')
        .rows([
          [1, 2, 'R1', 'C1'],
          [2, 3, 'R1', 'C1'],
          [1, 4, 'R1', 'C2'],
          [2, 5, 'R1', 'C2'],
          [1, 6, 'R2', 'C1'],
          [2, 7, 'R2', 'C1']
        ])
        .types(Integer, Integer, String, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        css_attributes(enabled: true) +
        geom_point() +
        facet_grid([rows: 'row_facet', cols: 'col_facet'])

    Svg svg = chart.render()
    String svgString = svg.toXml()

    // Should contain panel coordinates
    assertTrue(svgString.contains('id="gg-panel-') && svgString.contains('-layer-0-point-'),
        "SVG should contain panel coordinates in IDs for facet_grid")
  }

  @Test
  void testFacetedChartWithCustomPrefix() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'group')
        .rows([
          [1, 2, 'A'],
          [2, 3, 'A'],
          [1, 4, 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        css_attributes(enabled: true, chartIdPrefix: 'custom') +
        geom_point() +
        facet_wrap('group')

    Svg svg = chart.render()
    String svgString = svg.toXml()

    assertTrue(svgString.contains('id="custom-panel-'),
        "SVG should use custom prefix in faceted charts")
  }

  @Test
  void testFacetedChartElementUniqueness() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'group')
        .rows([
          [1, 2, 'A'],
          [2, 3, 'A'],
          [1, 2, 'B'],  // Same x,y as first point but different panel
          [2, 3, 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        css_attributes(enabled: true) +
        geom_point() +
        facet_wrap('group')

    Svg svg = chart.render()
    String svgString = svg.toXml()

    // Extract all IDs
    def idPattern = ~/id="([^"]+)"/
    def ids = []
    svgString.findAll(idPattern) { match ->
      ids << match[1]
    }

    // Check that all point IDs are unique
    def pointIds = ids.findAll { it.contains('point-') }
    assertEquals(pointIds.size(), pointIds.toSet().size(),
        "All point IDs should be unique across panels")

    // Verify we have IDs from both panels
    assertTrue(pointIds.any { it.contains('panel-0-0-') },
        "Should have IDs from first panel")
    assertTrue(pointIds.any { it.contains('panel-0-1-') } || pointIds.any { it.contains('panel-1-0-') },
        "Should have IDs from second panel")
  }

  @Test
  void testFacetedChartAddsDataPanelAttributes() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'group')
        .rows([
          [1, 2, 'A'],
          [2, 3, 'A'],
          [1, 4, 'B'],
          [2, 5, 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        css_attributes(enabled: true, includeDataAttributes: true) +
        geom_point() +
        facet_wrap('group')

    Svg svg = chart.render()
    String svgString = svg.toXml()

    assertTrue(svgString.contains('data-panel="0-0"'),
        "Faceted charts should include data-panel for first panel")
    assertTrue(svgString.contains('data-panel="0-1"') || svgString.contains('data-panel="1-0"'),
        "Faceted charts should include data-panel for additional panels")
  }
}
