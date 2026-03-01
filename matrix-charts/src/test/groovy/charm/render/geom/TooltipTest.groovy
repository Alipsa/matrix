package charm.render.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.charm.Charts.plot

class TooltipTest {

  @Test
  void testTooltipDefaultIsOff() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3]])
        .build()

    def chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers { geomPoint() }
    }.build()

    String xml = SvgWriter.toXml(chart.render())
    assertFalse(xml.contains('<title>'))
  }

  @Test
  void testMappedTooltipRendersSvgTitleOnPoints() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'tip')
        .rows([
            [1, 2, 'first point'],
            [2, 3, 'second point']
        ])
        .build()

    def chart = plot(data) {
      mapping { x = 'x'; y = 'y'; tooltip = 'tip' }
      layers { geomPoint() }
    }.build()

    String xml = SvgWriter.toXml(chart.render())
    assertTrue(xml.contains('<title>first point</title>'))
    assertTrue(xml.contains('<title>second point</title>'))
  }

  @Test
  void testLayerTemplateTakesPrecedenceOverMappedTooltip() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'category', 'tip')
        .rows([
            [1, 10, 'A', 'mapped-A'],
            [2, 20, 'B', 'mapped-B']
        ])
        .build()

    def chart = plot(data) {
      mapping { x = 'x'; y = 'y'; tooltip = 'tip' }
      layers {
        geomCol().tooltip('Category {category}: {y}')
      }
    }.build()

    String xml = SvgWriter.toXml(chart.render())
    assertTrue(xml.contains('<title>Category A: 10</title>'))
    assertTrue(xml.contains('<title>Category B: 20</title>'))
    assertFalse(xml.contains('<title>mapped-A</title>'))
    assertFalse(xml.contains('<title>mapped-B</title>'))
  }

  @Test
  void testTooltipCanBeExplicitlyDisabled() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'tip')
        .rows([[1, 2, 'hidden']])
        .build()

    def chart = plot(data) {
      mapping { x = 'x'; y = 'y'; tooltip = 'tip' }
      layers { geomPoint().tooltip(false) }
    }.build()

    String xml = SvgWriter.toXml(chart.render())
    assertFalse(xml.contains('<title>hidden</title>'))
  }

  @Test
  void testTooltipEnabledWithoutMappingUsesAutoText() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'category')
        .rows([[1, 2, 'A']])
        .build()

    def chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers { geomPoint().tooltip(true) }
    }.build()

    String xml = SvgWriter.toXml(chart.render())
    assertTrue(xml.contains('<title>x: 1, y: 2, category: A</title>'))
  }

  @Test
  void testHighUsageRenderersEmitTooltips() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'tip')
        .rows([
            [1, 2, 't1'],
            [2, 3, 't2'],
            [3, 4, 't3']
        ])
        .build()

    Svg pointSvg = plot(data) {
      mapping { x = 'x'; y = 'y'; tooltip = 'tip' }
      layers { geomPoint() }
    }.build().render()

    Svg barSvg = plot(data) {
      mapping { x = 'x'; y = 'y'; tooltip = 'tip' }
      layers { geomCol() }
    }.build().render()

    Svg lineSvg = plot(data) {
      mapping { x = 'x'; y = 'y'; tooltip = 'tip' }
      layers { geomLine() }
    }.build().render()

    Svg areaSvg = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers { geomArea().tooltip('area x={x}') }
    }.build().render()

    assertTrue(SvgWriter.toXml(pointSvg).contains('<title>t1</title>'))
    assertTrue(SvgWriter.toXml(barSvg).contains('<title>t1</title>'))
    assertTrue(SvgWriter.toXml(lineSvg).contains('<title>t1</title>'))
    assertTrue(SvgWriter.toXml(areaSvg).contains('<title>area x=1'))
  }
}
