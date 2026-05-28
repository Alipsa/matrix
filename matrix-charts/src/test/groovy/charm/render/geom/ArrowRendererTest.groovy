package charm.render.geom

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.charm.Charts.*

import org.junit.jupiter.api.Test
import testutil.Slow

import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.charm.ArrowSpec
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.core.Matrix

class ArrowRendererTest {

  @Test
  @Slow
  void testSegmentArrowRendersStartAndEndMarkers() {
    Matrix data = segmentData()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; xend = 'xend'; yend = 'yend' }
      layers {
        geomSegment().arrow(ArrowSpec.both(8, 6)).color('#336699')
      }
    }.build()

    String xml = SvgWriter.toXml(chart.render())

    assertTrue(xml.contains('<marker'))
    assertTrue(xml.contains('marker-start="url(#charm-arrow-0-0)"'))
    assertTrue(xml.contains('marker-end="url(#charm-arrow-0-0)"'))
    assertTrue(xml.contains('fill="#336699"'))
  }

  @Test
  @Slow
  void testCurveArrowRendersEndMarker() {
    Matrix data = segmentData()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; xend = 'xend'; yend = 'yend' }
      layers {
        geomCurve().arrow(ArrowSpec.end(7, 5)).color('#cc6677')
      }
    }.build()

    String xml = SvgWriter.toXml(chart.render())

    assertTrue(xml.contains('<marker'))
    assertFalse(xml.contains('marker-start="url(#charm-arrow-0-0)"'))
    assertTrue(xml.contains('marker-end="url(#charm-arrow-0-0)"'))
    assertTrue(xml.contains('fill="#cc6677"'))
  }

  private static Matrix segmentData() {
    Matrix.builder()
        .columnNames('x', 'y', 'xend', 'yend')
        .rows([[1, 1, 3, 3]])
        .build()
  }

}
