package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for CSS attributes in Geom classes.
 */
class GeomCssAttributesTest {

  @Test
  void testGeomPointWithCssAttributesEnabled() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 4]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        css_attributes(enabled: true) +
        geom_point()

    Svg svg = chart.render()
    String svgString = svg.toXml()

    // Should contain CSS classes
    assertTrue(svgString.contains('class="gg-point"'),
        "SVG should contain gg-point CSS class")

    // Should contain element IDs
    assertTrue(svgString.contains('id="gg-layer-0-point-0"'),
        "SVG should contain first point ID")
    assertTrue(svgString.contains('id="gg-layer-0-point-1"'),
        "SVG should contain second point ID")
    assertTrue(svgString.contains('id="gg-layer-0-point-2"'),
        "SVG should contain third point ID")
  }

  @Test
  void testGeomPointWithCssAttributesDisabled() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 4]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        css_attributes(enabled: false) +
        geom_point()

    Svg svg = chart.render()
    String svgString = svg.toXml()

    // Should NOT contain CSS classes or IDs for points
    assertFalse(svgString.contains('class="gg-point"'),
        "SVG should not contain gg-point CSS class when disabled")
    assertFalse(svgString.contains('id="gg-layer-0-point-0"'),
        "SVG should not contain point IDs when disabled")
  }

  @Test
  void testGeomBarWithCssAttributes() {
    def data = Matrix.builder()
        .columnNames('category', 'value')
        .rows([['A', 10], ['B', 20], ['C', 15]])
        .types(String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'category')) +
        css_attributes(enabled: true) +
        geom_bar()

    Svg svg = chart.render()
    String svgString = svg.toXml()

    assertTrue(svgString.contains('class="gg-bar"'),
        "SVG should contain gg-bar CSS class")
    assertTrue(svgString.contains('id="gg-layer-0-bar-'),
        "SVG should contain bar element IDs")
  }

  @Test
  void testGeomLineWithCssAttributes() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 4]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        css_attributes(enabled: true) +
        geom_line()

    Svg svg = chart.render()
    String svgString = svg.toXml()

    assertTrue(svgString.contains('class="gg-line"'),
        "SVG should contain gg-line CSS class")
  }

  @Test
  void testCustomChartIdPrefix() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        css_attributes(enabled: true, chartIdPrefix: 'iris') +
        geom_point()

    Svg svg = chart.render()
    String svgString = svg.toXml()

    assertTrue(svgString.contains('id="iris-layer-0-point-0"'),
        "SVG should use custom chart ID prefix")
    assertTrue(svgString.contains('id="iris-layer-0-point-1"'),
        "SVG should use custom chart ID prefix for all elements")
  }

  @Test
  void testMultipleLayersIncrementLayerIndex() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        css_attributes(enabled: true) +
        geom_point() +
        geom_line()

    Svg svg = chart.render()
    String svgString = svg.toXml()

    // Layer 0 (points)
    assertTrue(svgString.contains('id="gg-layer-0-point-'),
        "SVG should contain layer 0 point IDs")

    // Layer 1 (line)
    assertTrue(svgString.contains('id="gg-layer-1-line-'),
        "SVG should contain layer 1 line IDs")
  }

  @Test
  void testOnlyClassesEnabled() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        css_attributes(enabled: true, includeIds: false) +
        geom_point()

    Svg svg = chart.render()
    String svgString = svg.toXml()

    assertTrue(svgString.contains('class="gg-point"'),
        "SVG should contain CSS classes")
    assertFalse(svgString.contains('id="gg-layer-0-point-'),
        "SVG should not contain element IDs when disabled")
  }

  @Test
  void testOnlyIdsEnabled() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        css_attributes(enabled: true, includeClasses: false) +
        geom_point()

    Svg svg = chart.render()
    String svgString = svg.toXml()

    assertFalse(svgString.contains('class="gg-point"'),
        "SVG should not contain CSS classes when disabled")
    assertTrue(svgString.contains('id="gg-layer-0-point-'),
        "SVG should contain element IDs")
  }

  @Test
  void testGeomHistogramWithCssAttributes() {
    def data = Matrix.builder()
        .columnNames('value')
        .rows([[1], [2], [3], [4], [5]])
        .types(Integer)
        .build()

    def chart = ggplot(data, aes(x: 'value')) +
        css_attributes(enabled: true) +
        geom_histogram(bins: 3)

    Svg svg = chart.render()
    String svgString = svg.toXml()

    assertTrue(svgString.contains('class="gg-histogram"'),
        "SVG should contain gg-histogram CSS class")
  }

  @Test
  void testGeomAreaWithCssAttributes() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 4]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        css_attributes(enabled: true) +
        geom_area()

    Svg svg = chart.render()
    String svgString = svg.toXml()

    assertTrue(svgString.contains('class="gg-area"'),
        "SVG should contain gg-area CSS class")
  }

  @Test
  void testGeomPointWithDataAttributesEnabled() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        css_attributes(enabled: true, includeDataAttributes: true) +
        geom_point()

    Svg svg = chart.render()
    String svgString = svg.toXml()

    assertTrue(svgString.contains('data-x="1"') || svgString.contains('data-x="2"'),
        "SVG should include data-x attributes")
    assertTrue(svgString.contains('data-y="2"') || svgString.contains('data-y="3"'),
        "SVG should include data-y attributes")
    assertTrue(svgString.contains('data-row="0"'),
        "SVG should include data-row attributes")
    assertTrue(svgString.contains('data-layer="0"'),
        "SVG should include data-layer attributes")
    assertFalse(svgString.contains('data-panel='),
        "Non-faceted charts should not include data-panel")
  }

  @Test
  void testInvalidChartPrefixFallsBackToIdPrefix() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        css_attributes(enabled: true, chartIdPrefix: '123invalid', idPrefix: 'custom') +
        geom_point()

    Svg svg = chart.render()
    String svgString = svg.toXml()

    assertTrue(svgString.contains('id="custom-layer-0-point-0"'),
        "Invalid chartIdPrefix should fall back to idPrefix")
  }

  @Test
  void testDataRowUsesSourceRowIndexForMultiElementShapes() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        css_attributes(enabled: true, includeDataAttributes: true) +
        geom_point(shape: 'plus')

    Svg svg = chart.render()
    String svgString = svg.toXml()

    assertTrue(svgString.contains('data-row="0"'),
        "First source row should emit data-row=0")
    assertTrue(svgString.contains('data-row="1"'),
        "Second source row should emit data-row=1")
    assertFalse(svgString.contains('data-row="2"') || svgString.contains('data-row="3"'),
        "Multi-element point shapes should not use element index as data-row")
  }
}
