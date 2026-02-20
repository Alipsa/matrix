package charm.render

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.GuideSpec
import se.alipsa.matrix.charm.GuideType
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.charm.Charts.plot

class CharmAxisGuideTest {

  @Test
  void testAxisGuideWithLabelRotation() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 2],
            [2, 3],
            [3, 4]
        ])
        .build()

    Chart chart = plot(data) {
      aes { x = col.x; y = col.y }
      points {}
      guides {
        x = axis(angle: -45)
      }
    }.build()

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('rotate(-45'), 'X-axis labels should have rotation transform')
    assertTrue(content.contains('id="x-axis"'), 'X-axis group should be present')
  }

  @Test
  void testAxisGuideDefault() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 2],
            [2, 3]
        ])
        .build()

    Chart chart = plot(data) {
      aes { x = col.x; y = col.y }
      points {}
      guides {
        x = axis()
      }
    }.build()

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="x-axis"'), 'X-axis group should be present')
    assertTrue(content.contains('charm-axis-label'), 'Axis labels should be present')
  }

  @Test
  void testAxisGuideNoneDoesNotRender() {
    // This test verifies that when we DON'T set the guide to none for axes,
    // the axis still renders (there's no guide_none for axes typically,
    // axis rendering is controlled by theme explicitNulls)
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 2],
            [2, 3]
        ])
        .build()

    Chart chart = plot(data) {
      aes { x = col.x; y = col.y }
      points {}
    }.build()

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="axes"'), 'Axes group should be present')
  }

  @Test
  void testAxisLogticksGuide() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [10, 100],
            [100, 1000]
        ])
        .build()

    Chart chart = plot(data) {
      aes { x = col.x; y = col.y }
      points {}
      scale {
        x = se.alipsa.matrix.charm.Scale.transform('log10')
      }
      guides {
        x = axisLogticks()
      }
    }.build()

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="x-axis"'), 'Logticks axis group should be present')
    assertTrue(content.contains('<line'), 'Tick marks should be present')
  }

  @Test
  void testAxisStackGuide() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 2],
            [2, 3],
            [3, 4]
        ])
        .build()

    Chart chart = plot(data) {
      aes { x = col.x; y = col.y }
      points {}
      guides {
        x = GuideSpec.axisStack([
            first: GuideSpec.axis(),
            additional: [GuideSpec.axis(angle: 45)]
        ])
      }
    }.build()

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="x-axis"'), 'Primary X-axis group should be present')
    assertTrue(content.contains('id="x-axis-stack-0"'), 'Stacked axis wrapper group should have unique id')
    assertTrue(content.contains('translate(0,'), 'Stacked axis should be offset from primary axis')
    assertTrue(content.contains('rotate(45'), 'Stacked axis should include rotated labels')
  }
}
