package charm.render.geom

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull
import static se.alipsa.matrix.charm.Charts.plot

import org.junit.jupiter.api.Test

import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.geom.PolygonBuilder
import se.alipsa.matrix.core.Matrix

/** Polygon alpha must affect the fill only, like area, histogram and hex geoms. */
class PolygonRendererTest {

  private static Matrix triangle() {
    Matrix.builder().columns([x: [0.0, 1.0, 0.5], y: [0.0, 0.0, 1.0], g: ['a', 'a', 'a']])
        .types([BigDecimal, BigDecimal, String]).build()
  }

  @Test
  void alphaIsRenderedAsFillOpacity() {
    Chart chart = plot(triangle()) {
      mapping([x: 'x', y: 'y', group: 'g'])
      addLayer(new PolygonBuilder().alpha(0.4))
    }.build()
    Svg svg = chart.render()
    def polygon = svg.descendants().find { it.getAttribute('class')?.toString() == 'charm-polygon' }

    assertEquals('0.4', polygon.getAttribute('fill-opacity')?.toString())
    assertNull(polygon.getAttribute('opacity'))
  }

  @Test
  void fullAlphaAddsNoOpacityAttributes() {
    Chart chart = plot(triangle()) {
      mapping([x: 'x', y: 'y', group: 'g'])
      addLayer(new PolygonBuilder())
    }.build()
    def polygon = chart.render().descendants().find { it.getAttribute('class')?.toString() == 'charm-polygon' }

    assertNull(polygon.getAttribute('fill-opacity'))
    assertNull(polygon.getAttribute('opacity'))
  }

}
