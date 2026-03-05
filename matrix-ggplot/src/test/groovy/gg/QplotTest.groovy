package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Circle
import se.alipsa.groovy.svg.Line
import se.alipsa.groovy.svg.Rect
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.Text
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.GgChart
import se.alipsa.matrix.gg.geom.GeomBar
import se.alipsa.matrix.gg.geom.GeomCol
import se.alipsa.matrix.gg.geom.GeomHistogram
import se.alipsa.matrix.gg.geom.GeomLine
import se.alipsa.matrix.gg.geom.GeomPoint

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class QplotTest {

  Matrix numericData = Matrix.builder()
      .columnNames(['x', 'y', 'value'])
      .rows([
          [1, 2, 10],
          [2, 4, 20],
          [3, 6, 30],
          [4, 8, 40],
          [5, 10, 50],
      ])
      .types([int, int, int])
      .build()

  Matrix mixedData = Matrix.builder()
      .columnNames(['category', 'amount', 'group'])
      .rows([
          ['A', 10, 'g1'],
          ['B', 20, 'g1'],
          ['C', 30, 'g2'],
          ['A', 15, 'g2'],
          ['B', 25, 'g1'],
      ])
      .types([String, int, String])
      .build()

  @Test
  void testScatterInferred() {
    // Two numeric columns -> geom_point
    GgChart chart = qplot(data: numericData, x: 'x', y: 'y')
    assertInstanceOf(GeomPoint, chart.layers[0].geom)
    Svg svg = chart.render()
    assertNotNull(svg)
    def circles = svg.descendants().findAll { it instanceof Circle }
    assertTrue(circles.size() > 0, 'Scatter plot should render circles')
  }

  @Test
  void testHistogramInferred() {
    // Single numeric column -> geom_histogram
    GgChart chart = qplot(data: numericData, x: 'value')
    assertInstanceOf(GeomHistogram, chart.layers[0].geom)
    Svg svg = chart.render()
    assertNotNull(svg)
    def rects = svg.descendants().findAll { it instanceof Rect }
    assertTrue(rects.size() > 0, 'Histogram should render rects')
  }

  @Test
  void testBarInferred() {
    // Single non-numeric column -> geom_bar
    GgChart chart = qplot(data: mixedData, x: 'category')
    assertInstanceOf(GeomBar, chart.layers[0].geom)
    Svg svg = chart.render()
    assertNotNull(svg)
    def rects = svg.descendants().findAll { it instanceof Rect }
    assertTrue(rects.size() > 0, 'Bar chart should render rects')
  }

  @Test
  void testColInferred() {
    // Discrete x + numeric y -> geom_col
    GgChart chart = qplot(data: mixedData, x: 'category', y: 'amount')
    assertInstanceOf(GeomCol, chart.layers[0].geom)
    Svg svg = chart.render()
    assertNotNull(svg)
    def rects = svg.descendants().findAll { it instanceof Rect }
    assertTrue(rects.size() > 0, 'Column chart should render rects')
  }

  @Test
  void testExplicitGeomLine() {
    GgChart chart = qplot(data: numericData, x: 'x', y: 'y', geom: 'line')
    assertInstanceOf(GeomLine, chart.layers[0].geom)
    Svg svg = chart.render()
    assertNotNull(svg)
    def lines = svg.descendants().findAll { it instanceof Line }
    assertTrue(lines.size() > 0, 'Line chart should render lines')
  }

  @Test
  void testColorAesthetic() {
    Svg svg = qplot(data: mixedData, x: 'amount', y: 'amount', color: 'group').render()
    assertNotNull(svg)
    def circles = svg.descendants().findAll { it instanceof Circle }
    assertTrue(circles.size() > 0, 'Colored scatter should render circles')
  }

  @Test
  void testTitle() {
    Svg svg = qplot(data: numericData, x: 'x', y: 'y', title: 'My Test Plot').render()
    assertNotNull(svg)
    def textElements = svg.descendants().findAll { it instanceof Text }
    def allText = textElements.collect { (it as Text).content }.join(' ')
    assertTrue(allText.contains('My Test Plot'), "Title should appear in SVG text: ${allText}")
  }

  @Test
  void testClosureBased() {
    GgChart chart = qplot(numericData) { x = x; y = y }
    assertInstanceOf(GeomPoint, chart.layers[0].geom)
    Svg svg = chart.render()
    assertNotNull(svg)
    def circles = svg.descendants().findAll { it instanceof Circle }
    assertTrue(circles.size() > 0, 'Closure-based qplot should render circles')
  }

  @Test
  void testClosureWithParams() {
    GgChart chart = qplot(numericData, geom: 'line') { x = x; y = y }
    assertInstanceOf(GeomLine, chart.layers[0].geom)
    Svg svg = chart.render()
    assertNotNull(svg)
    def lines = svg.descendants().findAll { it instanceof Line }
    assertTrue(lines.size() > 0, 'Closure-based qplot with geom override should render lines')
  }

  @Test
  void testHistogramWithBins() {
    GgChart chart = qplot(data: numericData, x: 'value', bins: 5)
    assertInstanceOf(GeomHistogram, chart.layers[0].geom)
    Svg svg = chart.render()
    assertNotNull(svg)
    def rects = svg.descendants().findAll { it instanceof Rect }
    assertTrue(rects.size() > 0, 'Histogram with custom bins should render rects')
  }

  @Test
  void testUnknownGeomThrows() {
    assertThrows(IllegalArgumentException) {
      qplot(data: numericData, x: 'x', geom: 'nonexistent')
    }
  }

  @Test
  void testMissingDataThrows() {
    assertThrows(IllegalArgumentException) {
      qplot(data: null, x: 'x')
    }
  }

  @Test
  void testMissingXThrows() {
    assertThrows(IllegalArgumentException) {
      qplot(data: numericData)
    }
  }

  @Test
  void testNullParamsMapThrows() {
    assertThrows(IllegalArgumentException) {
      qplot((Map) null)
    }
  }

  @Test
  void testUnknownColumnThrows() {
    assertThrows(IllegalArgumentException) {
      qplot(data: numericData, x: 'nonexistent')
    }
  }

  @Test
  void testZeroBinsThrows() {
    assertThrows(IllegalArgumentException) {
      qplot(data: numericData, x: 'value', bins: 0)
    }
  }

  @Test
  void testNegativeBinsThrows() {
    assertThrows(IllegalArgumentException) {
      qplot(data: numericData, x: 'value', bins: -5)
    }
  }

  @Test
  void testAxisLabels() {
    Svg svg = qplot(data: numericData, x: 'x', y: 'y', xlab: 'X Axis', ylab: 'Y Axis').render()
    assertNotNull(svg)
    def textElements = svg.descendants().findAll { it instanceof Text }
    def allText = textElements.collect { (it as Text).content }.join(' ')
    assertTrue(allText.contains('X Axis'), "X label should appear: ${allText}")
    assertTrue(allText.contains('Y Axis'), "Y label should appear: ${allText}")
  }
}
