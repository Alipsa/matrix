package charm.render

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.Charts
import se.alipsa.matrix.charm.GuideSpec
import se.alipsa.matrix.charm.GuideType
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.charm.Charts.plot

class CharmLegendRendererTest {

  @Test
  void testDiscreteLegendRendersKeysForColorMapping() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows([
            [1, 2, 'A'],
            [2, 3, 'B'],
            [3, 4, 'C']
        ])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; color = 'cat' }
      layers { geomPoint() }
    }.build()

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="legend"'), 'Legend group should be present')
    assertTrue(content.contains('charm-legend-key'), 'Legend keys should be present')
    assertTrue(content.contains('charm-legend-label'), 'Legend labels should be present')
    assertTrue(content.contains('>A<'), 'Label A should be present')
    assertTrue(content.contains('>B<'), 'Label B should be present')
    assertTrue(content.contains('>C<'), 'Label C should be present')
  }

  @Test
  void testDiscreteLegendRendersCirclesForPointGeom() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows([
            [1, 2, 'A'],
            [2, 3, 'B']
        ])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; color = 'cat' }
      layers { geomPoint() }
    }.build()

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    // Point geoms should render circles as legend keys
    assertTrue(content.contains('<circle'), 'Legend should use circles for point geom')
  }

  @Test
  void testGuideNoneSuppressesLegend() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows([
            [1, 2, 'A'],
            [2, 3, 'B']
        ])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; color = 'cat' }
      layers { geomPoint() }
      guides {
        color = none()
      }
    }.build()

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertFalse(content.contains('id="legend"'), 'Legend should be suppressed when guide is none')
  }

  @Test
  void testLegendPositionRight() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows([
            [1, 2, 'A'],
            [2, 3, 'B']
        ])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; color = 'cat' }
      layers { geomPoint() }
      theme {
        legendPosition = 'right'
      }
    }.build()

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="legend"'), 'Legend should be present at right position')
    assertTrue(content.contains('translate('), 'Legend should have translate transform')
  }

  @Test
  void testLegendPositionNone() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows([
            [1, 2, 'A'],
            [2, 3, 'B']
        ])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; color = 'cat' }
      layers { geomPoint() }
      theme {
        legendPosition = 'none'
      }
    }.build()

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertFalse(content.contains('id="legend"'), 'Legend should not be present when position is none')
  }

  @Test
  void testLegendTitle() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows([
            [1, 2, 'A'],
            [2, 3, 'B']
        ])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; color = 'cat' }
      layers { geomPoint() }
      labels {
        guides['color'] = 'Category'
      }
    }.build()

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('charm-legend-title'), 'Legend title should be present')
    assertTrue(content.contains('>Category<'), 'Legend title text should be "Category"')
  }

  @Test
  void testGuidesDslSetsGuideTypes() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows([[1, 2, 'A']])
        .build()

    def spec = plot(data) {
      mapping { x = 'x'; y = 'y'; color = 'cat' }
      layers { geomPoint() }
      guides {
        color = 'legend'
        fill = 'none'
      }
    }

    assertEquals(GuideType.LEGEND, spec.guides.getSpec('color').type)
    assertEquals(GuideType.NONE, spec.guides.getSpec('fill').type)
  }

  @Test
  void testGuidesDslWithGuideSpecObjects() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows([[1, 2, 'A']])
        .build()

    def spec = plot(data) {
      mapping { x = 'x'; y = 'y'; color = 'cat' }
      layers { geomPoint() }
      guides {
        color = colorbar()
        fill = colorsteps()
      }
    }

    assertEquals(GuideType.COLORBAR, spec.guides.getSpec('color').type)
    assertEquals(GuideType.COLORSTEPS, spec.guides.getSpec('fill').type)
  }

  @Test
  void testGuidesDslWithBooleanFalse() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows([[1, 2, 'A']])
        .build()

    def spec = plot(data) {
      mapping { x = 'x'; y = 'y'; color = 'cat' }
      layers { geomPoint() }
      guides {
        color = false
      }
    }

    assertEquals(GuideType.NONE, spec.guides.getSpec('color').type)
  }

  @Test
  void testLegendPositionLeftTopBottom() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows([
            [1, 2, 'A'],
            [2, 3, 'B']
        ])
        .build()

    ['left', 'top', 'bottom'].each { String pos ->
      Chart chart = plot(data) {
        mapping { x = 'x'; y = 'y'; color = 'cat' }
        layers { geomPoint() }
        theme {
          legendPosition = pos
        }
      }.build()

      Svg svg = chart.render()
      String content = SvgWriter.toXml(svg)
      assertTrue(content.contains('id="legend"'), "Legend should be present at ${pos} position")
    }
  }
}
