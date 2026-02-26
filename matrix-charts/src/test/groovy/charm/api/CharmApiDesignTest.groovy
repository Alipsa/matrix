package charm.api

import groovy.lang.GroovyShell
import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.Cols
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.PlotSpec
import se.alipsa.matrix.charm.RectAnnotationSpec
import se.alipsa.matrix.charm.SegmentAnnotationSpec
import se.alipsa.matrix.charm.TextAnnotationSpec
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.GgChart

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.charm.Charts.chart
import static se.alipsa.matrix.charm.Charts.plot

class CharmApiDesignTest {

  @Test
  void testClosureDslPrimarySyntaxBuildAndRender() {
    Matrix mpg = Dataset.mpg()
    PlotSpec spec = plot(mpg) {
      mapping {
        x = 'cty'
        y = 'hwy'
        color = 'class'
      }
      points {
        size = 2
        alpha = 0.7
      }
      smooth {
        method = 'lm'
      }
      labels {
        title = 'City vs Highway MPG'
      }
    }

    Chart compiled = spec.build()
    Svg svg = compiled.render()
    assertNotNull(svg)
    assertEquals(2, compiled.layers.size())
    assertEquals('City vs Highway MPG', compiled.labels.title)
    assertEquals('cty', compiled.mapping.x.columnName())
  }

  @Test
  void testDynamicColDotSyntaxResolvesColumnExpr() {
    Cols col = new Cols()
    assertEquals('cty', col.cty.columnName())
    assertEquals('hwy', col.hwy.columnName())
  }

  @Test
  void testScaleThemeAndCoordDslExamples() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping {
        x = 'cty'
        y = 'hwy'
      }
      points {}
      scale {
        x = log10()
        y = sqrt()
      }
      theme {
        legendPosition = 'top'
        axisLineWidth = 0.75
      }
      coord {
        type = 'polar'
        theta = 'y'
        start = 0
      }
    }

    assertEquals('log10', spec.scale.x.transform)
    assertEquals('sqrt', spec.scale.y.transform)
    assertEquals('top', spec.theme.legendPosition)
    assertEquals(0.75, spec.theme.axisLineX.size)
    assertEquals('y', spec.coord.params.theta)
  }

  @Test
  void testCompileStaticColumnBracketSyntax() {
    Chart chart = StaticApiSample.build(Dataset.mpg())
    Svg svg = chart.render()
    assertNotNull(svg)
    assertEquals('class', chart.mapping.color.columnName())
  }

  @Test
  void testImportAliasStrategyCompilesAndRuns() {
    Matrix mpg = Dataset.mpg()
    GroovyShell shell = new GroovyShell(new Binding([mpg: mpg]))
    Object result = shell.evaluate('''
      import se.alipsa.matrix.charm.Chart as CharmChart
      import se.alipsa.matrix.charts.Chart as LegacyChart
      import static se.alipsa.matrix.charm.Charts.chart

      CharmChart c = chart(mpg) {
        mapping {
          x = 'cty'
          y = 'hwy'
        }
        points {}
      }.build()

      assert LegacyChart != null
      c.render()
    ''')
    assertTrue(result instanceof Svg)
  }

  @Test
  void testGgFacadeSecondarySyntaxStillRenders() {
    Matrix mpg = Dataset.mpg()
    GgChart gg = se.alipsa.matrix.gg.GgPlot.ggplot(
        mpg,
        se.alipsa.matrix.gg.GgPlot.aes(x: 'cty', y: 'hwy', colour: 'class')
    ) +
        se.alipsa.matrix.gg.GgPlot.geom_point(size: 2, alpha: 0.7) +
        se.alipsa.matrix.gg.GgPlot.geom_smooth(method: 'lm') +
        se.alipsa.matrix.gg.GgPlot.labs(title: 'City vs Highway MPG')
    Svg svg = gg.render()
    assertNotNull(svg)
  }

  @Test
  void testPlainStringMappingRendersIdenticallyToColProxy() {
    Matrix mpg = Dataset.mpg()
    Chart chart = plot(mpg) {
      mapping {
        x = 'cty'
        y = 'hwy'
      }
      points {}
    }.build()

    Svg svg = chart.render()
    assertNotNull(svg)
    assertEquals('cty', chart.mapping.x.columnName())
    assertEquals('hwy', chart.mapping.y.columnName())
  }

  @Test
  void testColourAliasProducesSameChartAsColor() {
    Matrix mpg = Dataset.mpg()
    Chart colorChart = plot(mpg) {
      mapping {
        x = 'cty'
        y = 'hwy'
        color = 'drv'
      }
      points {}
    }.build()

    Chart colourChart = plot(mpg) {
      mapping {
        x = 'cty'
        y = 'hwy'
        colour = 'drv'
      }
      points {}
    }.build()

    assertEquals(
        colorChart.mapping.color.columnName(),
        colourChart.mapping.color.columnName()
    )
    assertNotNull(colorChart.render())
    assertNotNull(colourChart.render())
  }

  @Test
  void testLayerDslExplicitFieldsRenderCorrectly() {
    Matrix mpg = Dataset.mpg()
    Chart chart = plot(mpg) {
      mapping {
        x = 'cty'
        y = 'hwy'
      }
      points {
        size = 3
        alpha = 0.7
        color = '#ff0000'
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(3, chart.layers.first().params['size'])
    assertEquals(0.7, chart.layers.first().params['alpha'])
    assertEquals('#ff0000', chart.layers.first().params['color'])
  }

  @Test
  void testLayerDslLinetypeStringAndIntegerBothWork() {
    Matrix mpg = Dataset.mpg()
    Chart stringChart = plot(mpg) {
      mapping { x = 'cty'; y = 'hwy' }
      line { linetype = 'dashed'; color = 'blue' }
    }.build()

    Chart intChart = plot(mpg) {
      mapping { x = 'cty'; y = 'hwy' }
      line { linetype = 2 }
    }.build()

    assertNotNull(stringChart.render())
    assertNotNull(intChart.render())
    assertEquals('dashed', stringChart.layers.first().params.linetype)
    assertEquals(2, intChart.layers.first().params.linetype)
  }

  @Test
  void testLayerDslUnrecognisedGeomPropsStillWork() {
    Matrix mpg = Dataset.mpg()
    Chart chart = plot(mpg) {
      mapping { x = 'cty'; y = 'hwy' }
      smooth {
        method = 'lm'
        se = false
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals('lm', chart.layers.first().params.method)
    assertEquals(false, chart.layers.first().params.se)
  }

  @Test
  void testAnnotationSpecExplicitFieldsWriteThroughToParams() {
    Matrix mpg = Dataset.mpg()
    Chart chart = plot(mpg) {
      mapping {
        x = 'cty'
        y = 'hwy'
      }
      points {}
      annotate {
        text {
          x = 15
          y = 35
          label = 'Note'
          color = '#ff0000'
          alpha = 0.8
        }
        rect {
          xmin = 10
          xmax = 20
          ymin = 25
          ymax = 40
          fill = '#0000ff'
          alpha = 0.3
        }
        segment {
          x = 10
          y = 25
          xend = 20
          yend = 40
          colour = '#00ff00'
        }
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(3, chart.annotations.size())

    def textSpec = chart.annotations[0] as TextAnnotationSpec
    assertEquals('#ff0000', textSpec.params['color'])
    assertEquals(0.8, textSpec.params['alpha'])

    def rectSpec = chart.annotations[1] as RectAnnotationSpec
    assertEquals('#0000ff', rectSpec.params['fill'])
    assertEquals(0.3, rectSpec.params['alpha'])

    def segSpec = chart.annotations[2] as SegmentAnnotationSpec
    assertEquals('#00ff00', segSpec.params['color'])
  }

  @Test
  void testFlatThemeSettersProduceCorrectThemeState() {
    PlotSpec spec = plot(Dataset.mpg()) {
      mapping {
        x = 'cty'
        y = 'hwy'
      }
      points {}
      theme {
        legendPosition = 'top'
        legendDirection = 'horizontal'
        axisLineWidth = 0.5
        axisColor = '#ff0000'
        axisTickLength = 8
        textColor = '#333333'
        textSize = 10
        titleSize = 16
        gridColor = '#cccccc'
        gridLineWidth = 0.25
        gridMinor = '#eeeeee'
        baseFamily = 'monospace'
        baseSize = 14
        baseLineHeight = 1.5
      }
    }

    assertEquals('top', spec.theme.legendPosition)
    assertEquals('horizontal', spec.theme.legendDirection)
    assertEquals(0.5, spec.theme.axisLineX.size)
    assertEquals('#ff0000', spec.theme.axisLineX.color)
    assertEquals(0.5, spec.theme.axisLineY.size)
    assertEquals('#ff0000', spec.theme.axisLineY.color)
    assertEquals(8, spec.theme.axisTickLength)
    assertEquals('#333333', spec.theme.axisTextX.color)
    assertEquals(10, spec.theme.axisTextX.size)
    assertEquals('#333333', spec.theme.axisTextY.color)
    assertEquals(10, spec.theme.axisTextY.size)
    assertEquals(16, spec.theme.plotTitle.size)
    assertEquals('#cccccc', spec.theme.panelGridMajor.color)
    assertEquals(0.25, spec.theme.panelGridMajor.size)
    assertEquals('#eeeeee', spec.theme.panelGridMinor.color)
    assertEquals('monospace', spec.theme.baseFamily)
    assertEquals(14, spec.theme.baseSize)
    assertEquals(1.5, spec.theme.baseLineHeight)
  }

  @CompileStatic
  private static class StaticApiSample {

    static Chart build(Matrix data) {
      PlotSpec spec = chart(data)
      spec.mapping(x: 'cty', y: 'hwy', color: 'class')
      spec.layer(CharmGeomType.POINT, [size: 2, alpha: 0.7])
      spec.layer(CharmGeomType.SMOOTH, [method: 'lm'])
      spec.build()
    }
  }
}
