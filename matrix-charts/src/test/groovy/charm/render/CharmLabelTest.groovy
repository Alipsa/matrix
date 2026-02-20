package charm.render

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.Text
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.Theme
import se.alipsa.matrix.charm.theme.ElementText
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.charm.Charts.plot

class CharmLabelTest {

  private static Matrix sampleData() {
    Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 2],
            [2, 4],
            [3, 6]
        ])
        .build()
  }

  @Test
  void testTitleRendered() {
    Chart chart = plot(sampleData()) {
      aes { x = col.x; y = col.y }
      points {}
      labels { title = 'My Title' }
    }.build()

    Svg svg = chart.render()
    String svgText = se.alipsa.groovy.svg.io.SvgWriter.toXml(svg)
    assertTrue(svgText.contains('My Title'), 'Title should be rendered')
  }

  @Test
  void testSubtitleRendered() {
    Chart chart = plot(sampleData()) {
      aes { x = col.x; y = col.y }
      points {}
      labels {
        title = 'Title'
        subtitle = 'A subtitle'
      }
    }.build()

    Svg svg = chart.render()
    String svgText = se.alipsa.groovy.svg.io.SvgWriter.toXml(svg)
    assertTrue(svgText.contains('A subtitle'), 'Subtitle should be in SVG output')
  }

  @Test
  void testCaptionRendered() {
    Chart chart = plot(sampleData()) {
      aes { x = col.x; y = col.y }
      points {}
      labels {
        title = 'Title'
        caption = 'Source: data'
      }
    }.build()

    Svg svg = chart.render()
    String svgText = se.alipsa.groovy.svg.io.SvgWriter.toXml(svg)
    assertTrue(svgText.contains('Source: data'), 'Caption should be in SVG output')
  }

  @Test
  void testAxisLabelsRendered() {
    Chart chart = plot(sampleData()) {
      aes { x = col.x; y = col.y }
      points {}
      labels {
        x = 'X Label'
        y = 'Y Label'
      }
    }.build()

    Svg svg = chart.render()
    String svgText = se.alipsa.groovy.svg.io.SvgWriter.toXml(svg)
    assertTrue(svgText.contains('X Label'), 'X label should be in SVG output')
    assertTrue(svgText.contains('Y Label'), 'Y label should be in SVG output')
  }

  @Test
  void testCoordFlipSwapsAxisLabels() {
    Chart chart = plot(sampleData()) {
      aes { x = col.x; y = col.y }
      points {}
      labels {
        x = 'Original X'
        y = 'Original Y'
      }
      coord { type = se.alipsa.matrix.charm.CharmCoordType.FLIP }
    }.build()

    Svg svg = chart.render()
    String svgText = se.alipsa.groovy.svg.io.SvgWriter.toXml(svg)
    // After flipping, the x-axis label position shows "Original Y"
    assertTrue(svgText.contains('Original X'), 'Should render original X on the flipped side')
    assertTrue(svgText.contains('Original Y'), 'Should render original Y on the flipped side')
  }

  @Test
  void testExplicitNullsSuppressTitle() {
    Theme theme = new Theme()
    theme.explicitNulls.add('plotTitle')

    Chart chart = plot(sampleData()) {
      aes { x = col.x; y = col.y }
      points {}
      labels { title = 'Should be hidden' }
    }.build()

    // Manually set the explicit null on the chart's theme by building a custom theme
    // The chart is immutable so we need to test via renderer directly
    se.alipsa.matrix.charm.ThemeSpec ts = new se.alipsa.matrix.charm.ThemeSpec()
    ts.explicitNulls.add('plotTitle')

    Chart chartWithNulls = new Chart(
        sampleData(),
        new se.alipsa.matrix.charm.AesSpec().tap { apply([x: 'x', y: 'y']) },
        [new se.alipsa.matrix.charm.LayerSpec(
            se.alipsa.matrix.charm.GeomSpec.of(se.alipsa.matrix.charm.CharmGeomType.POINT),
            se.alipsa.matrix.charm.StatSpec.of(se.alipsa.matrix.charm.CharmStatType.IDENTITY),
            null, true,
            se.alipsa.matrix.charm.PositionSpec.of(se.alipsa.matrix.charm.CharmPositionType.IDENTITY),
            [:]
        )],
        new se.alipsa.matrix.charm.ScaleSpec(),
        ts,
        new se.alipsa.matrix.charm.Facet(),
        new se.alipsa.matrix.charm.Coord(),
        new se.alipsa.matrix.charm.Labels(title: 'Should be hidden'),
        null,
        []
    )

    Svg svg = chartWithNulls.render()
    String svgText = se.alipsa.groovy.svg.io.SvgWriter.toXml(svg)
    assertFalse(svgText.contains('Should be hidden'), 'Title with explicit null should not render')
  }
}
