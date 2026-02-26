package charm.api

import groovy.lang.GroovyShell
import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.Cols
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType
import se.alipsa.matrix.charm.PlotSpec
import se.alipsa.matrix.charm.RectAnnotationSpec
import se.alipsa.matrix.charm.SegmentAnnotationSpec
import se.alipsa.matrix.charm.TextAnnotationSpec
import se.alipsa.matrix.charm.geom.Geoms
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

  @Test
  void testLayersDslClosureStyleRendersPointElements() {
    Matrix mpg = Dataset.mpg()
    Chart chart = plot(mpg) {
      mapping {
        x = 'cty'
        y = 'hwy'
      }
      layers {
        geomPoint().size(2).alpha(0.7)
      }
    }.build()

    Svg svg = chart.render()
    assertNotNull(svg)
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.POINT, chart.layers.first().geomType)
    assertEquals(2, chart.layers.first().params['size'])
    assertEquals(0.7, chart.layers.first().params['alpha'])
  }

  @Test
  void testLayersDslPureChainStyleRendersPointElements() {
    Matrix mpg = Dataset.mpg()
    Chart chart = plot(mpg)
        .mapping(x: 'cty', y: 'hwy')
        .layers {
          geomPoint().size(2)
        }.build()

    Svg svg = chart.render()
    assertNotNull(svg)
    assertEquals(1, chart.layers.size())
    assertEquals(2, chart.layers.first().params['size'])
  }

  @Test
  void testAddLayerEscapeHatchRendersPointElements() {
    Matrix mpg = Dataset.mpg()
    Chart chart = plot(mpg) {
      mapping {
        x = 'cty'
        y = 'hwy'
      }
      addLayer Geoms.geomPoint().size(2).color('#ff0000')
    }.build()

    Svg svg = chart.render()
    assertNotNull(svg)
    assertEquals(1, chart.layers.size())
    assertEquals(2, chart.layers.first().params['size'])
    assertEquals('#ff0000', chart.layers.first().params['color'])
  }

  @Test
  void testLayerBuilderMappingAndPositionWork() {
    Matrix mpg = Dataset.mpg()
    Chart chart = plot(mpg) {
      mapping {
        x = 'displ'
        y = 'hwy'
      }
      layers {
        geomPoint()
            .mapping { x = 'cty'; y = 'hwy' }
            .inheritMapping(false)
            .position('jitter')
            .size(3)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertNotNull(chart.layers.first().mapping)
    assertEquals('cty', chart.layers.first().mapping.x.columnName())
    assertEquals(false, chart.layers.first().inheritMapping)
    assertEquals(se.alipsa.matrix.charm.CharmPositionType.JITTER, chart.layers.first().positionType)
  }

  @Test
  void testLineBuilderRendersLineLayer() {
    Matrix mpg = Dataset.mpg()
    Chart chart = plot(mpg) {
      mapping { x = 'cty'; y = 'hwy' }
      layers {
        geomLine().color('#336699').linetype('dashed').size(1.5)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.LINE, chart.layers.first().geomType)
    assertEquals('#336699', chart.layers.first().params['color'])
    assertEquals('dashed', chart.layers.first().params['linetype'])
    assertEquals(1.5, chart.layers.first().params['size'])
  }

  @Test
  void testSmoothBuilderRendersSmoothLayer() {
    Matrix mpg = Dataset.mpg()
    Chart chart = plot(mpg) {
      mapping { x = 'cty'; y = 'hwy' }
      layers {
        geomSmooth().method('lm').se(false).color('#cc0000')
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.SMOOTH, chart.layers.first().geomType)
    assertEquals('lm', chart.layers.first().params['method'])
    assertEquals(false, chart.layers.first().params['se'])
  }

  @Test
  void testAreaBuilderRendersAreaLayerWithAlignStat() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 3], [2, 5], [3, 4], [4, 6]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomArea().fill('#336699').alpha(0.5)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.AREA, chart.layers.first().geomType)
    assertEquals(CharmStatType.ALIGN, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['fill'])
  }

  @Test
  void testRibbonBuilderRendersRibbonLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'ymin', 'ymax')
        .rows([[1, 2, 4], [2, 3, 6], [3, 4, 7]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; ymin = 'ymin'; ymax = 'ymax' }
      layers {
        geomRibbon().fill('#cc6677').alpha(0.3)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.RIBBON, chart.layers.first().geomType)
    assertEquals('#cc6677', chart.layers.first().params['fill'])
    assertEquals(0.3, chart.layers.first().params['alpha'])
  }

  @Test
  void testLineBuilderChainStyleWorks() {
    Matrix mpg = Dataset.mpg()
    Chart chart = plot(mpg)
        .mapping(x: 'cty', y: 'hwy')
        .addLayer(Geoms.geomLine().alpha(0.8))
        .build()

    assertNotNull(chart.render())
    assertEquals(CharmGeomType.LINE, chart.layers.first().geomType)
    assertEquals(0.8, chart.layers.first().params['alpha'])
  }

  @Test
  void testSmoothBuilderChainStyleWorks() {
    Matrix mpg = Dataset.mpg()
    Chart chart = plot(mpg)
        .mapping(x: 'cty', y: 'hwy')
        .addLayer(Geoms.geomSmooth().method('loess').span(0.5))
        .build()

    assertNotNull(chart.render())
    assertEquals(CharmGeomType.SMOOTH, chart.layers.first().geomType)
    assertEquals('loess', chart.layers.first().params['method'])
    assertEquals(0.5, chart.layers.first().params['span'])
  }

  @Test
  void testBarBuilderRendersBarLayer() {
    Matrix data = Matrix.builder()
        .columnNames('category')
        .rows([['A'], ['B'], ['A'], ['C'], ['B'], ['A']])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'category' }
      layers {
        geomBar().fill('#336699').alpha(0.8)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.BAR, chart.layers.first().geomType)
    assertEquals(CharmStatType.COUNT, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['fill'])
    assertEquals(0.8, chart.layers.first().params['alpha'])
  }

  @Test
  void testColBuilderRendersColLayer() {
    Matrix data = Matrix.builder()
        .columnNames('category', 'value')
        .rows([['A', 10], ['B', 20], ['C', 15]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'category'; y = 'value' }
      layers {
        geomCol().fill('#cc6677').width(0.7)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.COL, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals('#cc6677', chart.layers.first().params['fill'])
    assertEquals(0.7, chart.layers.first().params['width'])
  }

  @Test
  void testHistogramBuilderRendersHistogramLayer() {
    Matrix data = Matrix.builder()
        .columnNames('value')
        .rows([[1.2], [2.3], [2.8], [3.5], [4.1], [4.5], [5.0]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'value' }
      layers {
        geomHistogram().bins(10).fill('#88aa55').alpha(0.7)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.HISTOGRAM, chart.layers.first().geomType)
    assertEquals(CharmStatType.BIN, chart.layers.first().statType)
    assertEquals(10, chart.layers.first().params['bins'])
    assertEquals('#88aa55', chart.layers.first().params['fill'])
  }

  @Test
  void testBoxplotBuilderRendersBoxplotLayer() {
    Matrix data = Matrix.builder()
        .columnNames('group', 'value')
        .rows([['A', 1], ['A', 2], ['A', 3], ['B', 4], ['B', 5], ['B', 6]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'group'; y = 'value' }
      layers {
        geomBoxplot().fill('#eeeeee').notch(true).alpha(0.9)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.BOXPLOT, chart.layers.first().geomType)
    assertEquals(CharmStatType.BOXPLOT, chart.layers.first().statType)
    assertEquals('#eeeeee', chart.layers.first().params['fill'])
    assertEquals(true, chart.layers.first().params['notch'])
  }

  @Test
  void testViolinBuilderRendersViolinLayer() {
    Matrix data = Matrix.builder()
        .columnNames('group', 'value')
        .rows([['A', 1], ['A', 2], ['A', 3], ['B', 4], ['B', 5], ['B', 6]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'group'; y = 'value' }
      layers {
        geomViolin().fill('#cc6677').alpha(0.7)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.VIOLIN, chart.layers.first().geomType)
    assertEquals(CharmStatType.YDENSITY, chart.layers.first().statType)
    assertEquals('#cc6677', chart.layers.first().params['fill'])
    assertEquals(0.7, chart.layers.first().params['alpha'])
  }

  @Test
  void testDotplotBuilderRendersDotplotLayer() {
    Matrix data = Matrix.builder()
        .columnNames('value')
        .rows([[1.0], [1.5], [2.0], [2.5], [3.0], [3.5]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'value' }
      layers {
        geomDotplot().fill('#336699').binwidth(0.5)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.DOTPLOT, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['fill'])
    assertEquals(0.5, chart.layers.first().params['binwidth'])
  }

  @Test
  void testTileBuilderRendersTileLayer() {
    Matrix data = Matrix.builder()
        .columnNames('col', 'row', 'value')
        .rows([['A', 'X', 1], ['B', 'X', 2], ['A', 'Y', 3], ['B', 'Y', 4]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'col'; y = 'row' }
      layers {
        geomTile().fill('#336699').alpha(0.8)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.TILE, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['fill'])
    assertEquals(0.8, chart.layers.first().params['alpha'])
  }

  @Test
  void testPieBuilderRendersPieLayer() {
    Matrix data = Matrix.builder()
        .columnNames('category', 'value')
        .rows([['A', 30], ['B', 50], ['C', 20]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'category'; y = 'value' }
      layers {
        geomPie().fill('#cc6677').alpha(0.9)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.PIE, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals('#cc6677', chart.layers.first().params['fill'])
  }

  @Test
  void testRectBuilderRendersRectLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x1', 'x2', 'y1', 'y2')
        .rows([[1, 3, 1, 3], [4, 6, 4, 6]])
        .build()

    Chart chart = plot(data) {
      mapping { xmin = 'x1'; xmax = 'x2'; ymin = 'y1'; ymax = 'y2' }
      layers {
        geomRect().fill('#88aa55').alpha(0.5)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.RECT, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals('#88aa55', chart.layers.first().params['fill'])
    assertEquals(0.5, chart.layers.first().params['alpha'])
  }

  @Test
  void testHexBuilderRendersHexLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 4], [4, 5], [5, 6]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomHex().fill('#336699').bins(10)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.HEX, chart.layers.first().geomType)
    assertEquals(CharmStatType.BIN_HEX, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['fill'])
    assertEquals(10, chart.layers.first().params['bins'])
  }

  @Test
  void testBin2dBuilderRendersBin2dLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 4], [4, 5], [5, 6]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomBin2d().fill('#cc6677').bins(15)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.BIN2D, chart.layers.first().geomType)
    assertEquals(CharmStatType.BIN2D, chart.layers.first().statType)
    assertEquals('#cc6677', chart.layers.first().params['fill'])
    assertEquals(15, chart.layers.first().params['bins'])
  }

  @Test
  void testRasterBuilderRendersRasterLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'z')
        .rows([[1, 1, 0.5], [2, 1, 0.8], [1, 2, 0.3], [2, 2, 0.9]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; fill = 'z' }
      layers {
        geomRaster().alpha(0.9).interpolate(true)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.RASTER, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals(0.9, chart.layers.first().params['alpha'])
    assertEquals(true, chart.layers.first().params['interpolate'])
  }

  @Test
  void testTextBuilderRendersTextLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'name')
        .rows([[1, 2, 'A'], [3, 4, 'B'], [5, 6, 'C']])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; label = 'name' }
      layers {
        geomText().size(4).color('#333333').family('serif')
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.TEXT, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals(4, chart.layers.first().params['size'])
    assertEquals('#333333', chart.layers.first().params['color'])
    assertEquals('serif', chart.layers.first().params['family'])
  }

  @Test
  void testLabelBuilderRendersLabelLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'name')
        .rows([[1, 2, 'A'], [3, 4, 'B'], [5, 6, 'C']])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; label = 'name' }
      layers {
        geomLabel().size(4).fill('#ffffff').color('#333333')
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.LABEL, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals(4, chart.layers.first().params['size'])
    assertEquals('#ffffff', chart.layers.first().params['fill'])
    assertEquals('#333333', chart.layers.first().params['color'])
  }

  @Test
  void testSegmentBuilderRendersSegmentLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'xend', 'yend')
        .rows([[1, 1, 3, 3], [2, 2, 4, 4]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; xend = 'xend'; yend = 'yend' }
      layers {
        geomSegment().color('#336699').size(1).linetype('dashed')
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.SEGMENT, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['color'])
    assertEquals(1, chart.layers.first().params['size'])
    assertEquals('dashed', chart.layers.first().params['linetype'])
  }

  @Test
  void testCurveBuilderRendersCurveLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'xend', 'yend')
        .rows([[1, 1, 3, 3], [2, 2, 4, 4]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; xend = 'xend'; yend = 'yend' }
      layers {
        geomCurve().curvature(0.3).color('#cc6677')
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.CURVE, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals(0.3, chart.layers.first().params['curvature'])
    assertEquals('#cc6677', chart.layers.first().params['color'])
  }

  @Test
  void testAblineBuilderRendersAblineLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [3, 4], [5, 6]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomPoint()
        geomAbline().intercept(0).slope(1).color('#cc0000').linetype('dashed')
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(2, chart.layers.size())
    assertEquals(CharmGeomType.ABLINE, chart.layers[1].geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers[1].statType)
    assertEquals(0, chart.layers[1].params['intercept'])
    assertEquals(1, chart.layers[1].params['slope'])
    assertEquals('#cc0000', chart.layers[1].params['color'])
    assertEquals('dashed', chart.layers[1].params['linetype'])
  }

  @Test
  void testHlineBuilderRendersHlineLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [3, 4], [5, 6]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomPoint()
        geomHline().yintercept(3).color('#cc0000').linetype('dashed')
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(2, chart.layers.size())
    assertEquals(CharmGeomType.HLINE, chart.layers[1].geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers[1].statType)
    assertEquals(3, chart.layers[1].params['yintercept'])
    assertEquals('#cc0000', chart.layers[1].params['color'])
    assertEquals('dashed', chart.layers[1].params['linetype'])
  }

  @Test
  void testVlineBuilderRendersVlineLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [3, 4], [5, 6]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomPoint()
        geomVline().xintercept(3).color('#cc0000').linetype('dotted')
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(2, chart.layers.size())
    assertEquals(CharmGeomType.VLINE, chart.layers[1].geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers[1].statType)
    assertEquals(3, chart.layers[1].params['xintercept'])
    assertEquals('#cc0000', chart.layers[1].params['color'])
    assertEquals('dotted', chart.layers[1].params['linetype'])
  }

  @Test
  void testDensityBuilderRendersDensityLayer() {
    Matrix data = Matrix.builder()
        .columnNames('value')
        .rows([[1.0], [1.5], [2.0], [2.5], [3.0], [3.5], [4.0]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'value' }
      layers {
        geomDensity().fill('#336699').alpha(0.5).adjust(1.5)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.DENSITY, chart.layers.first().geomType)
    assertEquals(CharmStatType.DENSITY, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['fill'])
    assertEquals(0.5, chart.layers.first().params['alpha'])
    assertEquals(1.5, chart.layers.first().params['adjust'])
  }

  @Test
  void testFreqpolyBuilderRendersFreqpolyLayer() {
    Matrix data = Matrix.builder()
        .columnNames('value')
        .rows([[1.0], [1.5], [2.0], [2.5], [3.0], [3.5], [4.0]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'value' }
      layers {
        geomFreqpoly().bins(10).color('#336699')
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.FREQPOLY, chart.layers.first().geomType)
    assertEquals(CharmStatType.BIN, chart.layers.first().statType)
    assertEquals(10, chart.layers.first().params['bins'])
    assertEquals('#336699', chart.layers.first().params['color'])
  }

  @Test
  void testQqBuilderRendersQqLayer() {
    Matrix data = Matrix.builder()
        .columnNames('sample')
        .rows([[1.0], [2.0], [3.0], [4.0], [5.0]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'sample' }
      layers {
        geomQq().color('#336699').size(2)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.QQ, chart.layers.first().geomType)
    assertEquals(CharmStatType.QQ, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['color'])
    assertEquals(2, chart.layers.first().params['size'])
  }

  @Test
  void testQqLineBuilderRendersQqLineLayer() {
    Matrix data = Matrix.builder()
        .columnNames('sample')
        .rows([[1.0], [2.0], [3.0], [4.0], [5.0]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'sample' }
      layers {
        geomQqLine().color('#cc0000').linetype('dashed')
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.QQ_LINE, chart.layers.first().geomType)
    assertEquals(CharmStatType.QQ_LINE, chart.layers.first().statType)
    assertEquals('#cc0000', chart.layers.first().params['color'])
    assertEquals('dashed', chart.layers.first().params['linetype'])
  }

  @Test
  void testQuantileBuilderRendersQuantileLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 5], [4, 4], [5, 7]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomQuantile().quantiles([0.25, 0.5, 0.75]).color('#336699')
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.QUANTILE, chart.layers.first().geomType)
    assertEquals(CharmStatType.QUANTILE, chart.layers.first().statType)
    assertEquals([0.25, 0.5, 0.75], chart.layers.first().params['quantiles'])
    assertEquals('#336699', chart.layers.first().params['color'])
  }

  @Test
  void testErrorbarBuilderRendersErrorbarLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'lo', 'hi')
        .rows([[1, 1, 3], [2, 2, 5], [3, 3, 6]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; ymin = 'lo'; ymax = 'hi' }
      layers {
        geomErrorbar().width(0.2).color('#333333').size(0.5)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.ERRORBAR, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals(0.2, chart.layers.first().params['width'])
    assertEquals('#333333', chart.layers.first().params['color'])
    assertEquals(0.5, chart.layers.first().params['size'])
  }

  @Test
  void testCrossbarBuilderRendersCrossbarLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'mid', 'lo', 'hi')
        .rows([[1, 2, 1, 3], [2, 4, 3, 5], [3, 5, 4, 6]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'mid'; ymin = 'lo'; ymax = 'hi' }
      layers {
        geomCrossbar().fill('#eeeeee').width(0.5).fatten(2.5)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.CROSSBAR, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals('#eeeeee', chart.layers.first().params['fill'])
    assertEquals(0.5, chart.layers.first().params['width'])
    assertEquals(2.5, chart.layers.first().params['fatten'])
  }

  @Test
  void testLinerangeBuilderRendersLinerangeLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'lo', 'hi')
        .rows([[1, 1, 3], [2, 2, 5], [3, 3, 6]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; ymin = 'lo'; ymax = 'hi' }
      layers {
        geomLinerange().color('#336699').size(1)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.LINERANGE, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['color'])
    assertEquals(1, chart.layers.first().params['size'])
  }

  @Test
  void testPointrangeBuilderRendersPointrangeLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'mid', 'lo', 'hi')
        .rows([[1, 2, 1, 3], [2, 4, 3, 5], [3, 5, 4, 6]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'mid'; ymin = 'lo'; ymax = 'hi' }
      layers {
        geomPointrange().color('#336699').size(1).fatten(3)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.POINTRANGE, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['color'])
    assertEquals(1, chart.layers.first().params['size'])
    assertEquals(3, chart.layers.first().params['fatten'])
  }

  // ── Phase 10: Path/step/jitter builders ──

  @Test
  void testPathBuilderRendersPathLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3], [4, 5]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomPath().color('#336699').size(1).alpha(0.8).lineend('round').linejoin('mitre')
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.PATH, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['color'])
    assertEquals(1, chart.layers.first().params['size'])
    assertEquals(0.8, chart.layers.first().params['alpha'])
    assertEquals('round', chart.layers.first().params['lineend'])
    assertEquals('mitre', chart.layers.first().params['linejoin'])
  }

  @Test
  void testStepBuilderRendersStepLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3], [4, 5]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomStep().color('#336699').size(1).direction('hv')
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.STEP, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['color'])
    assertEquals(1, chart.layers.first().params['size'])
    assertEquals('hv', chart.layers.first().params['direction'])
  }

  @Test
  void testJitterBuilderRendersJitterLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([['a', 2], ['a', 3], ['b', 4], ['b', 5]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomJitter().width(0.2).height(0).color('#336699').size(3).alpha(0.5)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.JITTER, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals(0.2, chart.layers.first().params['width'])
    assertEquals(0, chart.layers.first().params['height'])
    assertEquals('#336699', chart.layers.first().params['color'])
    assertEquals(3, chart.layers.first().params['size'])
    assertEquals(0.5, chart.layers.first().params['alpha'])
  }

  @Test
  void testRugBuilderRendersRugLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3], [4, 5]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomPoint().size(2)
        geomRug().color('#336699').size(0.5).sides('bl').outside(true)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(2, chart.layers.size())
    assertEquals(CharmGeomType.RUG, chart.layers[1].geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers[1].statType)
    assertEquals('#336699', chart.layers[1].params['color'])
    assertEquals(0.5, chart.layers[1].params['size'])
    assertEquals('bl', chart.layers[1].params['sides'])
    assertEquals(true, chart.layers[1].params['outside'])
  }

  @Test
  void testCountBuilderRendersCountLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([['a', 'x'], ['a', 'y'], ['b', 'x'], ['b', 'x']])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomCount().color('#336699').fill('#99ccff').size(3)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.COUNT, chart.layers.first().geomType)
    assertEquals(CharmStatType.COUNT, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['color'])
    assertEquals('#99ccff', chart.layers.first().params['fill'])
    assertEquals(3, chart.layers.first().params['size'])
  }

  @Test
  void testContourBuilderRendersContourLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3], [4, 5]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomContour().color('#336699').size(1).bins(10)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.CONTOUR, chart.layers.first().geomType)
    assertEquals(CharmStatType.CONTOUR, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['color'])
    assertEquals(1, chart.layers.first().params['size'])
    assertEquals(10, chart.layers.first().params['bins'])
  }

  @Test
  void testFunctionBuilderRendersFunctionLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3], [4, 5]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomPoint().size(2)
        geomFunction().color('#336699').size(1).n(200)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(2, chart.layers.size())
    assertEquals(CharmGeomType.FUNCTION, chart.layers[1].geomType)
    assertEquals(CharmStatType.FUNCTION, chart.layers[1].statType)
    assertEquals('#336699', chart.layers[1].params['color'])
    assertEquals(1, chart.layers[1].params['size'])
    assertEquals(200, chart.layers[1].params['n'])
  }

  // ── Phase 11: Spatial + remaining builders ──

  @Test
  void testSfBuilderRendersSfLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomSf().fill('#336699').color('#000000').alpha(0.8).size(1)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.SF, chart.layers.first().geomType)
    assertEquals(CharmStatType.SF, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['fill'])
    assertEquals('#000000', chart.layers.first().params['color'])
    assertEquals(0.8, chart.layers.first().params['alpha'])
    assertEquals(1, chart.layers.first().params['size'])
  }

  @Test
  void testSfLabelBuilderRendersSfLabelLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'label')
        .rows([[1, 2, 'A'], [2, 4, 'B'], [3, 3, 'C']])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; label = 'label' }
      layers {
        geomPoint().size(2)
        geomSfLabel().size(3).color('#000000').family('serif')
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(2, chart.layers.size())
    assertEquals(CharmGeomType.SF_LABEL, chart.layers[1].geomType)
    assertEquals(CharmStatType.SF_COORDINATES, chart.layers[1].statType)
    assertEquals(3, chart.layers[1].params['size'])
    assertEquals('#000000', chart.layers[1].params['color'])
    assertEquals('serif', chart.layers[1].params['family'])
  }

  @Test
  void testSfTextBuilderRendersSfTextLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'label')
        .rows([[1, 2, 'A'], [2, 4, 'B'], [3, 3, 'C']])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; label = 'label' }
      layers {
        geomPoint().size(2)
        geomSfText().size(3).color('#000000').family('sans-serif')
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(2, chart.layers.size())
    assertEquals(CharmGeomType.SF_TEXT, chart.layers[1].geomType)
    assertEquals(CharmStatType.SF_COORDINATES, chart.layers[1].statType)
    assertEquals(3, chart.layers[1].params['size'])
    assertEquals('#000000', chart.layers[1].params['color'])
    assertEquals('sans-serif', chart.layers[1].params['family'])
  }

  @Test
  void testPolygonBuilderRendersPolygonLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'group')
        .rows([[0, 0, 'a'], [1, 0, 'a'], [1, 1, 'a'], [0, 1, 'a']])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; group = 'group' }
      layers {
        geomPolygon().fill('#336699').color('#000000').alpha(0.5).size(1)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.POLYGON, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['fill'])
    assertEquals('#000000', chart.layers.first().params['color'])
    assertEquals(0.5, chart.layers.first().params['alpha'])
    assertEquals(1, chart.layers.first().params['size'])
  }

  @Test
  void testMapBuilderRendersMapLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'group')
        .rows([[0, 0, 'a'], [1, 0, 'a'], [1, 1, 'a'], [0, 1, 'a']])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y'; group = 'group' }
      layers {
        geomMap().fill('#336699').color('#000000').alpha(0.5)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.MAP, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['fill'])
    assertEquals('#000000', chart.layers.first().params['color'])
    assertEquals(0.5, chart.layers.first().params['alpha'])
  }

  @Test
  void testDensity2dBuilderRendersDensity2dLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3], [4, 5]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomDensity2d().color('#336699').size(1).bins(10)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.DENSITY_2D, chart.layers.first().geomType)
    assertEquals(CharmStatType.DENSITY_2D, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['color'])
    assertEquals(1, chart.layers.first().params['size'])
    assertEquals(10, chart.layers.first().params['bins'])
  }

  @Test
  void testDensity2dFilledBuilderRendersDensity2dFilledLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3], [4, 5]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomDensity2dFilled().fill('#336699').alpha(0.5).bins(10)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.DENSITY_2D_FILLED, chart.layers.first().geomType)
    assertEquals(CharmStatType.DENSITY_2D, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['fill'])
    assertEquals(0.5, chart.layers.first().params['alpha'])
    assertEquals(10, chart.layers.first().params['bins'])
  }

  @Test
  void testContourFilledBuilderRendersContourFilledLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3], [4, 5]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomContourFilled().fill('#336699').alpha(0.5).bins(10)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.CONTOUR_FILLED, chart.layers.first().geomType)
    assertEquals(CharmStatType.CONTOUR, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['fill'])
    assertEquals(0.5, chart.layers.first().params['alpha'])
    assertEquals(10, chart.layers.first().params['bins'])
  }

  @Test
  void testSpokeBuilderRendersSpokeLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3], [4, 5]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomPoint().size(2)
        geomSpoke().color('#336699').size(1).alpha(0.7)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(2, chart.layers.size())
    assertEquals(CharmGeomType.SPOKE, chart.layers[1].geomType)
    assertEquals(CharmStatType.SPOKE, chart.layers[1].statType)
    assertEquals('#336699', chart.layers[1].params['color'])
    assertEquals(1, chart.layers[1].params['size'])
    assertEquals(0.7, chart.layers[1].params['alpha'])
  }

  @Test
  void testMagBuilderRendersMagLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3], [4, 5]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomPoint().size(2)
        geomMag().color('#336699').size(1).alpha(0.7)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(2, chart.layers.size())
    assertEquals(CharmGeomType.MAG, chart.layers[1].geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers[1].statType)
    assertEquals('#336699', chart.layers[1].params['color'])
    assertEquals(1, chart.layers[1].params['size'])
    assertEquals(0.7, chart.layers[1].params['alpha'])
  }

  @Test
  void testParallelBuilderRendersParallelLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3], [4, 5]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomParallel().color('#336699').size(1).alpha(0.3)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.PARALLEL, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals('#336699', chart.layers.first().params['color'])
    assertEquals(1, chart.layers.first().params['size'])
    assertEquals(0.3, chart.layers.first().params['alpha'])
  }

  @Test
  void testLogticksBuilderRendersLogticksLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3], [4, 5]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomPoint().size(2)
        geomLogticks().sides('bl').color('#333333').size(0.5)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(2, chart.layers.size())
    assertEquals(CharmGeomType.LOGTICKS, chart.layers[1].geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers[1].statType)
    assertEquals('bl', chart.layers[1].params['sides'])
    assertEquals('#333333', chart.layers[1].params['color'])
    assertEquals(0.5, chart.layers[1].params['size'])
  }

  @Test
  void testBlankBuilderRendersBlankLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomBlank()
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.BLANK, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
  }

  @Test
  void testRasterAnnBuilderRendersRasterAnnLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomPoint().size(2)
        geomRasterAnn().alpha(0.8).interpolate(true)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(2, chart.layers.size())
    assertEquals(CharmGeomType.RASTER_ANN, chart.layers[1].geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers[1].statType)
    assertEquals(0.8, chart.layers[1].params['alpha'])
    assertEquals(true, chart.layers[1].params['interpolate'])
  }

  @Test
  void testErrorbarhBuilderRendersErrorbarhLayer() {
    Matrix data = Matrix.builder()
        .columnNames('y', 'lo', 'hi')
        .rows([[1, 0.5, 1.5], [2, 1.5, 2.5], [3, 2.5, 3.5]])
        .build()

    Chart chart = plot(data) {
      mapping { y = 'y'; xmin = 'lo'; xmax = 'hi' }
      layers {
        geomErrorbarh().height(0.2).color('#333333').size(1)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(1, chart.layers.size())
    assertEquals(CharmGeomType.ERRORBARH, chart.layers.first().geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers.first().statType)
    assertEquals(0.2, chart.layers.first().params['height'])
    assertEquals('#333333', chart.layers.first().params['color'])
    assertEquals(1, chart.layers.first().params['size'])
  }

  @Test
  void testCustomBuilderRendersCustomLayer() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomPoint().size(2)
        geomCustom().color('#336699').fill('#99ccff').size(3)
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(2, chart.layers.size())
    assertEquals(CharmGeomType.CUSTOM, chart.layers[1].geomType)
    assertEquals(CharmStatType.IDENTITY, chart.layers[1].statType)
    assertEquals('#336699', chart.layers[1].params['color'])
    assertEquals('#99ccff', chart.layers[1].params['fill'])
    assertEquals(3, chart.layers[1].params['size'])
  }

  @Test
  void testCustomBuilderRendererStoredInParams() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 3]])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      layers {
        geomPoint().size(2)
        geomCustom().renderer { svg, layerData -> }
      }
    }.build()

    assertNotNull(chart.render())
    assertEquals(2, chart.layers.size())
    assertNotNull(chart.layers[1].params['renderer'])
    assertTrue(chart.layers[1].params['renderer'] instanceof Closure)
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
