package charm.render

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.charm.Charts.plot

import org.junit.jupiter.api.Test

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.Text
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.GuideType
import se.alipsa.matrix.charm.LegendPosition
import se.alipsa.matrix.charm.PlotSpec
import se.alipsa.matrix.charm.theme.ElementText
import se.alipsa.matrix.core.Matrix

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
        legendPosition = RIGHT
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
        legendPosition = NONE
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

  /**
   * Verifies multi-scale legends fall back to aesthetic names using legend title styling.
   */
  @Test
  void testMultiScaleLegendTitlesFallbackToAestheticNames() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'kind', 'source')
        .rows([
            ['A', 10, 'baseline', 'observed'],
            ['B', 14, 'target', 'model'],
            ['C', 9, 'baseline', 'observed']
        ])
        .build()

    PlotSpec spec = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomCol().mapping(fill: 'kind')
        geomPoint().mapping(color: 'source')
      }
    }
    spec.theme.legendTitle = new ElementText(size: 16, color: 'navy')
    spec.theme.legendText = new ElementText(size: 7, color: 'gray')
    Chart chart = spec.build()

    Svg svg = chart.render()
    List<Text> textElements = svg.descendants()
        .findAll { it instanceof Text }
        .collect { it as Text }
    List<String> text = textElements*.content
    List<Text> headingText = textElements.findAll { it.content in ['fill', 'color'] }

    assertTrue(text.contains('fill'), text.join(' '))
    assertTrue(text.contains('color'), text.join(' '))
    assertTrue(headingText.every { it.getAttribute('fill') == 'navy' })
    assertTrue(headingText.every { it.getAttribute('font-size') == '16' })
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
  void testLegendPositionCoords() {
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
      legendPosition([100, 200])
    }.build()

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="legend"'), 'Legend should be present with coordinate positioning')
    assertTrue(content.contains('translate(100'), 'Legend x should use specified coordinate')
  }

  @Test
  void testLegendPositionNoneOverridesCoords() {
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
      legendPosition([100, 200])
      theme {
        legendPosition = NONE
      }
    }.build()

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertFalse(content.contains('id="legend"'), 'NONE should suppress legend even when coords are set')
  }

  @Test
  void testLegendPositionEnumClearsCoordsLastWriteWins() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows([
            [1, 2, 'A'],
            [2, 3, 'B']
        ])
        .build()

    // Set coords first, then override with enum position
    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; color = 'cat' }
      layers { geomPoint() }
      legendPosition([100, 200])
      legendPosition(LegendPosition.LEFT)
    }.build()

    assertNull(chart.theme.legendPositionCoords,
        'Setting enum position should clear coords')
    assertEquals(LegendPosition.LEFT, chart.theme.legendPosition)
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

    [LegendPosition.LEFT, LegendPosition.TOP, LegendPosition.BOTTOM].each { LegendPosition pos ->
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
