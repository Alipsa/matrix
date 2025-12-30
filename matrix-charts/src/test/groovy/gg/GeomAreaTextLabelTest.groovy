package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.geom.GeomArea
import se.alipsa.matrix.gg.geom.GeomText
import se.alipsa.matrix.gg.geom.GeomLabel

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class GeomAreaTextLabelTest {

  // ==================== GeomArea Tests ====================

  @Test
  void testGeomAreaDefaults() {
    GeomArea geom = new GeomArea()

    assertEquals('gray', geom.fill)
    assertEquals('black', geom.color)
    assertEquals(0.5, geom.linewidth)
    assertEquals(0.7, geom.alpha)
    assertEquals('solid', geom.linetype)
    assertEquals(['x', 'y'], geom.requiredAes)
  }

  @Test
  void testGeomAreaWithParams() {
    GeomArea geom = new GeomArea(
        fill: 'blue',
        color: 'red',
        linewidth: 2,
        alpha: 0.5
    )

    assertEquals('blue', geom.fill)
    assertEquals('red', geom.color)
    assertEquals(2, geom.linewidth)
    assertEquals(0.5, geom.alpha)
  }

  @Test
  void testSimpleAreaChart() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 25],
            [3, 15],
            [4, 30],
            [5, 20]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_area(fill: 'steelblue', alpha: 0.5) +
        labs(title: 'Simple Area Chart')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<path'), "Should contain path element for area")
    assertTrue(content.contains('fill="steelblue"'), "Should have fill color")

    File outputFile = new File('build/simple_area_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testAreaChartWithLine() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 30],
            [3, 20],
            [4, 40],
            [5, 25]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_area(fill: 'lightblue', alpha: 0.5, color: 'blue', linewidth: 1) +
        geom_line(color: 'darkblue', size: 2) +
        labs(title: 'Area with Line')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/area_with_line.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testGroupedAreaChart() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'group')
        .rows([
            [1, 10, 'A'],
            [2, 20, 'A'],
            [3, 15, 'A'],
            [1, 5, 'B'],
            [2, 15, 'B'],
            [3, 10, 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', fill: 'group')) +
        geom_area(alpha: 0.6) +
        labs(title: 'Grouped Area Chart')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/grouped_area_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ==================== GeomText Tests ====================

  @Test
  void testGeomTextDefaults() {
    GeomText geom = new GeomText()

    assertEquals('black', geom.color)
    assertEquals(10, geom.size)
    assertEquals('sans-serif', geom.family)
    assertEquals('normal', geom.fontface)
    assertEquals(1.0, geom.alpha)
    assertEquals(0.5, geom.hjust)
    assertEquals(0.5, geom.vjust)
    assertEquals(0, geom.angle)
  }

  @Test
  void testGeomTextWithParams() {
    GeomText geom = new GeomText(
        color: 'red',
        size: 14,
        family: 'monospace',
        fontface: 'bold',
        hjust: 0,
        vjust: 1
    )

    assertEquals('red', geom.color)
    assertEquals(14, geom.size)
    assertEquals('monospace', geom.family)
    assertEquals('bold', geom.fontface)
    assertEquals(0, geom.hjust)
    assertEquals(1, geom.vjust)
  }

  @Test
  void testTextLabelsOnScatter() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'name')
        .rows([
            [1, 10, 'Point A'],
            [2, 25, 'Point B'],
            [3, 15, 'Point C'],
            [4, 30, 'Point D']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', label: 'name')) +
        geom_point(size: 4, color: 'blue') +
        geom_text(nudge_y: 2, size: 10) +
        labs(title: 'Scatter with Text Labels')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<text'), "Should contain text elements")
    assertTrue(content.contains('Point A'), "Should have label text")

    File outputFile = new File('build/text_labels_scatter.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testRotatedText() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'label')
        .rows([
            [1, 10, 'Rotated'],
            [2, 20, 'Text'],
            [3, 15, 'Labels']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', label: 'label')) +
        geom_point() +
        geom_text(angle: 45, size: 12, color: 'red') +
        labs(title: 'Rotated Text')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('rotate'), "Should have rotation transform")

    File outputFile = new File('build/rotated_text.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testTextWithDifferentAlignments() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'label')
        .rows([
            [1, 10, 'Left'],
            [2, 20, 'Center'],
            [3, 15, 'Right']
        ])
        .types(Integer, Integer, String)
        .build()

    // Test left alignment
    def chartLeft = ggplot(data, aes(x: 'x', y: 'y', label: 'label')) +
        geom_point() +
        geom_text(hjust: 0)

    Svg svgLeft = chartLeft.render()
    assertNotNull(svgLeft)
  }

  // ==================== GeomLabel Tests ====================

  @Test
  void testGeomLabelDefaults() {
    GeomLabel geom = new GeomLabel()

    assertEquals('black', geom.color)
    assertEquals('white', geom.fill)
    assertEquals(10, geom.size)
    assertEquals(4, geom.label_padding)
    assertEquals(2, geom.label_r)
    assertEquals(0.5, geom.label_size)
  }

  @Test
  void testGeomLabelWithParams() {
    GeomLabel geom = new GeomLabel(
        fill: 'yellow',
        color: 'black',
        size: 12,
        label_padding: 6,
        label_r: 4
    )

    assertEquals('yellow', geom.fill)
    assertEquals('black', geom.color)
    assertEquals(12, geom.size)
    assertEquals(6, geom.label_padding)
    assertEquals(4, geom.label_r)
  }

  @Test
  void testLabelsOnScatter() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'name')
        .rows([
            [1, 10, 'A'],
            [2, 25, 'B'],
            [3, 15, 'C'],
            [4, 30, 'D']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', label: 'name')) +
        geom_point(size: 4, color: 'blue') +
        geom_label(fill: 'lightyellow', nudge_y: 3) +
        labs(title: 'Scatter with Labels')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<rect'), "Should contain rect for label background")
    assertTrue(content.contains('<text'), "Should contain text elements")

    File outputFile = new File('build/labels_scatter.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testLabelWithCustomStyling() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'label')
        .rows([
            [1, 10, 'Important'],
            [2, 20, 'Data'],
            [3, 15, 'Point']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', label: 'label')) +
        geom_point(color: 'red', size: 5) +
        geom_label(
            fill: 'white',
            color: 'darkblue',
            size: 11,
            fontface: 'bold',
            label_padding: 5,
            label_r: 3
        ) +
        labs(title: 'Styled Labels')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/styled_labels.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ==================== Combined Tests ====================

  @Test
  void testAreaWithLabels() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'label')
        .rows([
            [1, 10, 'Start'],
            [2, 25, 'Peak'],
            [3, 15, 'Dip'],
            [4, 30, 'High'],
            [5, 20, 'End']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', label: 'label')) +
        geom_area(fill: 'lightblue', alpha: 0.5) +
        geom_point(size: 4) +
        geom_text(nudge_y: 2, size: 9) +
        labs(title: 'Area Chart with Labels')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/area_with_labels.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testFactoryMethods() {
    def area = geom_area()
    assertNotNull(area)
    assertTrue(area instanceof GeomArea)

    def areaParams = geom_area(fill: 'blue', alpha: 0.5)
    assertEquals('blue', areaParams.fill)
    assertEquals(0.5, areaParams.alpha)

    def text = geom_text()
    assertNotNull(text)
    assertTrue(text instanceof GeomText)

    def textParams = geom_text(color: 'red', size: 14)
    assertEquals('red', textParams.color)
    assertEquals(14, textParams.size)

    def label = geom_label()
    assertNotNull(label)
    assertTrue(label instanceof GeomLabel)

    def labelParams = geom_label(fill: 'yellow', size: 12)
    assertEquals('yellow', labelParams.fill)
    assertEquals(12, labelParams.size)
  }
}
