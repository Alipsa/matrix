package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.layer.Layer

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Test annotation_custom() functionality.
 */
class AnnotationCustomTest {

  @Test
  void testCustomClosureBasic() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [2, 2], [3, 3], [4, 4]])
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        annotation_custom(
            grob: { G g, Map b ->
              g.addRect()
                  .x(b.xmin as int)
                  .y(b.ymin as int)
                  .width((b.xmax - b.xmin) as int)
                  .height((b.ymax - b.ymin) as int)
                  .fill('red')
                  .addAttribute('opacity', 0.2)
            },
            xmin: 1, xmax: 3, ymin: 1, ymax: 3
        )

    assertNotNull(chart)
    assertEquals(2, chart.layers.size())

    // Verify the custom layer
    def customLayer = chart.layers[1]
    assertNotNull(customLayer)
    assertNotNull(customLayer.geom)
    assertEquals('GeomCustom', customLayer.geom.class.simpleName)
    assertFalse(customLayer.inheritAes)

    // Render to ensure no errors
    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<svg'))
    assertTrue(svgContent.contains('</svg>'))
  }

  @Test
  void testCustomInfBounds() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [2, 2], [3, 3], [4, 4]])
        .build()

    // Default bounds should fill entire panel
    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        annotation_custom(
            grob: { G g, Map b ->
              g.addCircle()
                  .cx(((b.xmin + b.xmax) / 2) as int)
                  .cy(((b.ymin + b.ymax) / 2) as int)
                  .r(10)
                  .fill('blue')
            }
        )

    assertNotNull(chart)
    assertEquals(2, chart.layers.size())

    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<circle'))
  }

  @Test
  void testCustomSpecificBounds() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [2, 2], [3, 3], [4, 4], [5, 5]])
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        annotation_custom(
            grob: { G g, Map b ->
              g.addRect()
                  .x(b.xmin as int)
                  .y(b.ymin as int)
                  .width((b.xmax - b.xmin) as int)
                  .height((b.ymax - b.ymin) as int)
                  .fill('yellow')
                  .addAttribute('opacity', 0.3)
            },
            xmin: 2, xmax: 4, ymin: 2, ymax: 4
        )

    assertNotNull(chart)

    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<rect'))
  }

  @Test
  void testCustomLayer() {
    Layer layer = annotation_custom(
        grob: { G g, Map b ->
          g.addCircle().cx(100).cy(100).r(20).fill('green')
        },
        xmin: 1, xmax: 2, ymin: 1, ymax: 2
    )

    assertNotNull(layer)
    assertNotNull(layer.geom)
    assertEquals('GeomCustom', layer.geom.class.simpleName)
    assertNotNull(layer.data)
    assertNull(layer.aes)
    assertFalse(layer.inheritAes)

    // Verify data contains bounds
    assertTrue(layer.data.columnNames().contains('xmin'))
    assertTrue(layer.data.columnNames().contains('xmax'))
    assertTrue(layer.data.columnNames().contains('ymin'))
    assertTrue(layer.data.columnNames().contains('ymax'))
  }

  @Test
  void testCustomRequiresGrob() {
    def exception = assertThrows(IllegalArgumentException) {
      annotation_custom(xmin: 1, xmax: 2)
    }

    assertTrue(exception.message.contains('grob'))
  }

  @Test
  void testCustomSvgString() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [2, 2], [3, 3], [4, 4]])
        .build()

    // Test with SVG string grob
    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        annotation_custom(
            grob: '<rect width="100" height="50" fill="purple" opacity="0.4"/>',
            xmin: 1.5, xmax: 3.5, ymin: 1.5, ymax: 3.5
        )

    assertNotNull(chart)
    assertEquals(2, chart.layers.size())

    // Render to ensure SVG string parsing works
    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<rect'))
    assertTrue(svgContent.contains('purple'))
  }

  @Test
  void testCustomSvgStringComplex() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [2, 2], [3, 3]])
        .build()

    // Test with more complex SVG markup containing multiple elements
    def svgMarkup = '''
      <g>
        <circle cx="50" cy="50" r="20" fill="red"/>
        <text x="50" y="50" text-anchor="middle" fill="white">Test</text>
      </g>
    '''

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        annotation_custom(
            grob: svgMarkup,
            xmin: 1, xmax: 2, ymin: 1, ymax: 2
        )

    assertNotNull(chart)

    // Render and verify
    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<circle'))
    assertTrue(svgContent.contains('<text'))
  }

  @Test
  void testCustomMalformedSvgString() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [2, 2], [3, 3]])
        .build()

    // Test with malformed SVG markup (unclosed tag)
    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        annotation_custom(
            grob: '<rect width="100" height="50"',  // Missing closing >
            xmin: 1, xmax: 2, ymin: 1, ymax: 2
        )

    assertNotNull(chart)

    // Rendering should throw IllegalArgumentException with helpful message
    def exception = assertThrows(IllegalArgumentException) {
      chart.render()
    }

    assertTrue(exception.message.contains('Failed to parse SVG string'))
    assertTrue(exception.message.contains('annotation_custom'))
    assertTrue(exception.message.contains('valid'))
  }
}
