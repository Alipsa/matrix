package charm.core

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.charm.Aes
import se.alipsa.matrix.charm.CharmCoordType
import se.alipsa.matrix.charm.CharmExpression
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmPositionType
import se.alipsa.matrix.charm.CharmStatType
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.ColumnExpr
import se.alipsa.matrix.charm.ColumnRef
import se.alipsa.matrix.charm.CoordSpec
import se.alipsa.matrix.charm.GeomSpec
import se.alipsa.matrix.charm.Layer
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.PlotSpec
import se.alipsa.matrix.charm.PositionSpec
import se.alipsa.matrix.charm.StatSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.gg.aes.AfterScale
import se.alipsa.matrix.gg.aes.AfterStat
import se.alipsa.matrix.gg.aes.CutWidth
import se.alipsa.matrix.gg.aes.Expression
import se.alipsa.matrix.gg.aes.Factor
import se.alipsa.matrix.gg.aes.Identity

import se.alipsa.matrix.charm.CharmValidationException

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertNotSame
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.charm.Charts.plot

/**
 * Phase 2 tests for expanded Charm model types.
 */
class CharmModelExpansionTest {

  @Test
  void testCharmGeomTypeValues() {
    CharmGeomType[] values = CharmGeomType.values()
    assertEquals(56, values.length)
    assertNotNull(CharmGeomType.POINT)
    assertNotNull(CharmGeomType.VIOLIN)
    assertNotNull(CharmGeomType.VLINE)
    assertNotNull(CharmGeomType.CONTOUR_FILLED)
    assertNotNull(CharmGeomType.DENSITY_2D_FILLED)
  }

  @Test
  void testCharmStatTypeHas26Values() {
    CharmStatType[] values = CharmStatType.values()
    assertEquals(26, values.length)
    assertNotNull(CharmStatType.IDENTITY)
    assertNotNull(CharmStatType.ALIGN)
    assertNotNull(CharmStatType.SPOKE)
  }

  @Test
  void testCharmPositionTypeHas7Values() {
    CharmPositionType[] values = CharmPositionType.values()
    assertEquals(7, values.length)
    assertNotNull(CharmPositionType.IDENTITY)
    assertNotNull(CharmPositionType.NUDGE)
    assertNotNull(CharmPositionType.DODGE2)
  }

  @Test
  void testCharmCoordTypeHas9Values() {
    CharmCoordType[] values = CharmCoordType.values()
    assertEquals(9, values.length)
    assertNotNull(CharmCoordType.CARTESIAN)
    assertNotNull(CharmCoordType.SF)
    assertNotNull(CharmCoordType.RADIAL)
  }

  @Test
  void testGeomSpecConstructionAndCopy() {
    GeomSpec spec = new GeomSpec(
        CharmGeomType.POINT,
        [size: 3],
        ['x', 'y'],
        [color: 'black'],
        CharmStatType.IDENTITY,
        CharmPositionType.IDENTITY
    )
    assertEquals(CharmGeomType.POINT, spec.type)
    assertEquals(3, spec.params['size'])
    assertEquals(['x', 'y'], spec.requiredAes)
    assertEquals('black', spec.defaultAes.color)
    assertEquals(CharmStatType.IDENTITY, spec.defaultStat)
    assertEquals(CharmPositionType.IDENTITY, spec.defaultPosition)

    GeomSpec copy = spec.copy()
    assertNotSame(spec, copy)
    assertEquals(spec.type, copy.type)
    assertEquals(spec.params, copy.params)
  }

  @Test
  void testGeomSpecOfFactory() {
    GeomSpec spec = GeomSpec.of(CharmGeomType.BAR)
    assertEquals(CharmGeomType.BAR, spec.type)
    assertTrue(spec.params.isEmpty())

    GeomSpec specWithParams = GeomSpec.of(CharmGeomType.HISTOGRAM, [bins: 30])
    assertEquals(CharmGeomType.HISTOGRAM, specWithParams.type)
    assertEquals(30, specWithParams.params.bins)
  }

  @Test
  void testStatSpecWithParams() {
    StatSpec spec = StatSpec.of(CharmStatType.BIN, [bins: 30])
    assertEquals(CharmStatType.BIN, spec.type)
    assertEquals(30, spec.params.bins)

    StatSpec copy = spec.copy()
    assertNotSame(spec, copy)
    assertEquals(spec.type, copy.type)
  }

  @Test
  void testStatSpecSmoothMethod() {
    StatSpec spec = StatSpec.of(CharmStatType.SMOOTH, [method: 'lm'])
    assertEquals(CharmStatType.SMOOTH, spec.type)
    assertEquals('lm', spec.params.method)
  }

  @Test
  void testPositionSpecWithParams() {
    PositionSpec spec = PositionSpec.of(CharmPositionType.DODGE, [width: 0.9])
    assertEquals(CharmPositionType.DODGE, spec.type)
    assertEquals(0.9, spec.width as double, 0.001)

    PositionSpec padded = PositionSpec.of(CharmPositionType.DODGE2, [padding: 0.1])
    assertEquals(0.1, padded.padding as double, 0.001)

    PositionSpec reversed = PositionSpec.of(CharmPositionType.STACK, [reverse: true])
    assertTrue(reversed.reverse)

    PositionSpec seeded = PositionSpec.of(CharmPositionType.JITTER, [seed: 42L])
    assertEquals(42L, seeded.seed)
  }

  @Test
  void testPositionSpecCopy() {
    PositionSpec spec = PositionSpec.of(CharmPositionType.JITTER, [width: 0.5, seed: 123L])
    PositionSpec copy = spec.copy()
    assertNotSame(spec, copy)
    assertEquals(spec.type, copy.type)
    assertEquals(spec.params, copy.params)
  }

  @Test
  void testCoordSpecExpandedParams() {
    CoordSpec spec = new CoordSpec()
    spec.type = 'polar'
    spec.theta = 'y'
    spec.start = 0
    spec.direction = 1
    spec.clip = 'on'

    assertEquals(CharmCoordType.POLAR, spec.type)
    assertEquals('y', spec.theta)
    assertEquals(0, spec.start as int)
    assertEquals(1, spec.direction)
    assertEquals('on', spec.clip)
  }

  @Test
  void testCoordSpecFixedWithLimitsAndRatio() {
    CoordSpec spec = new CoordSpec()
    spec.type = 'fixed'
    spec.xlim = [0, 10]
    spec.ylim = [0, 20]
    spec.ratio = 1.5

    assertEquals(CharmCoordType.FIXED, spec.type)
    assertEquals([0, 10], spec.xlim)
    assertEquals([0, 20], spec.ylim)
    assertEquals(1.5, spec.ratio as double, 0.001)
  }

  @Test
  void testExpandedLayerDataCarriesNewAestheticChannels() {
    LayerData ld = new LayerData()
    ld.x = 1
    ld.y = 2
    ld.xend = 3
    ld.yend = 4
    ld.xmin = 0
    ld.xmax = 5
    ld.ymin = 0
    ld.ymax = 6
    ld.size = 3
    ld.shape = 'circle'
    ld.alpha = 0.5
    ld.linetype = 'dashed'
    ld.group = 'A'
    ld.label = 'point1'
    ld.weight = 1.0

    assertEquals(1, ld.x)
    assertEquals(2, ld.y)
    assertEquals(3, ld.xend)
    assertEquals(4, ld.yend)
    assertEquals(0, ld.xmin)
    assertEquals(5, ld.xmax)
    assertEquals(0, ld.ymin)
    assertEquals(6, ld.ymax)
    assertEquals(3, ld.size)
    assertEquals('circle', ld.shape)
    assertEquals(0.5, ld.alpha)
    assertEquals('dashed', ld.linetype)
    assertEquals('A', ld.group)
    assertEquals('point1', ld.label)
    assertEquals(1.0, ld.weight)
  }

  @Test
  void testExpandedLayerDataDefaultsToNull() {
    LayerData ld = new LayerData()
    assertNull(ld.xend)
    assertNull(ld.yend)
    assertNull(ld.xmin)
    assertNull(ld.xmax)
    assertNull(ld.ymin)
    assertNull(ld.ymax)
    assertNull(ld.size)
    assertNull(ld.shape)
    assertNull(ld.alpha)
    assertNull(ld.linetype)
    assertNull(ld.group)
    assertNull(ld.label)
    assertNull(ld.weight)
  }

  @Test
  void testLayerConstructionWithSpecObjects() {
    GeomSpec geom = GeomSpec.of(CharmGeomType.VIOLIN)
    StatSpec stat = StatSpec.of(CharmStatType.YDENSITY)
    PositionSpec pos = PositionSpec.of(CharmPositionType.DODGE)

    Layer layer = new Layer(geom, stat, null, true, pos, [trim: true])
    assertEquals(CharmGeomType.VIOLIN, layer.geomType)
    assertEquals(CharmStatType.YDENSITY, layer.statType)
    assertEquals(CharmPositionType.DODGE, layer.positionType)
    assertEquals(true, layer.params.trim)
  }

  @Test
  void testLayerSpecCopyPreservesSpecs() {
    GeomSpec geom = GeomSpec.of(CharmGeomType.DENSITY)
    StatSpec stat = StatSpec.of(CharmStatType.DENSITY, [bw: 'nrd0'])
    PositionSpec pos = PositionSpec.of(CharmPositionType.IDENTITY)

    LayerSpec layer = new LayerSpec(geom, stat, null, true, pos, [:])
    LayerSpec copy = layer.copy()
    assertNotSame(layer, copy)
    assertEquals(layer.geomType, copy.geomType)
    assertEquals(layer.statType, copy.statType)
    assertEquals(layer.positionType, copy.positionType)
  }

  @Test
  void testCharmExpressionImplementedByGgExpressionTypes() {
    // Factor
    Factor factor = new Factor('cyl')
    assertTrue(factor instanceof CharmExpression)
    assertEquals('factor(cyl)', factor.describe())

    // CutWidth
    CutWidth cw = new CutWidth('displ', 1)
    assertTrue(cw instanceof CharmExpression)
    assertEquals('cut_width(displ, 1)', cw.describe())

    // Expression
    Expression expr = Expression.of({ it -> 1.0 } as Closure<Number>, 'test')
    assertTrue(expr instanceof CharmExpression)
    assertEquals('expr(test)', expr.describe())

    // AfterStat
    AfterStat afterStat = new AfterStat('count')
    assertTrue(afterStat instanceof CharmExpression)
    assertEquals('after_stat(count)', afterStat.describe())

    // AfterScale
    AfterScale afterScale = new AfterScale('fill')
    assertTrue(afterScale instanceof CharmExpression)
    assertEquals('after_scale(fill)', afterScale.describe())

    // Identity
    Identity identity = new Identity('red')
    assertTrue(identity instanceof CharmExpression)
    assertEquals('I(red)', identity.describe())
  }

  @Test
  void testColumnExprIsCharmExpression() {
    ColumnExpr ref = new ColumnRef('x')
    assertTrue(ref instanceof CharmExpression)
    assertEquals('column(x)', ref.describe())
  }

  @Test
  void testPlotSpecLayerRejectsUnsupportedGeomTypes() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [3, 4]])
        .build()

    PlotSpec spec = plot(data)
    spec.aes(x: 'x', y: 'y')

    CharmValidationException ex = assertThrows(CharmValidationException) {
      spec.layer(CharmGeomType.CONTOUR, [:])
    }
    assertTrue(ex.message.contains('CONTOUR'))
  }

  @Test
  void testPlotSpecLayerAcceptsSupportedGeomTypes() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [3, 4]])
        .build()

    PlotSpec spec = plot(data)
    spec.aes(x: 'x', y: 'y')
    spec.layer(CharmGeomType.POINT, [:])
    spec.layer(CharmGeomType.LINE, [:])
    spec.layer(CharmGeomType.DENSITY, [stat: CharmStatType.DENSITY])
    spec.layer(CharmGeomType.VIOLIN, [stat: CharmStatType.YDENSITY])
    spec.layer(CharmGeomType.TEXT, [label: 'ok'])

    assertEquals(5, spec.layers.size())
    assertEquals(CharmGeomType.POINT, spec.layers[0].geomType)
    assertEquals(CharmGeomType.LINE, spec.layers[1].geomType)
    assertEquals(CharmGeomType.DENSITY, spec.layers[2].geomType)
    assertEquals(CharmGeomType.VIOLIN, spec.layers[3].geomType)
    assertEquals(CharmGeomType.TEXT, spec.layers[4].geomType)
  }

  @Test
  void testPlotSpecPointsLineConvenienceMethodsStillWork() {
    Chart chart = plot(Dataset.mpg()) {
      aes {
        x = col.cty
        y = col.hwy
      }
      points {}
      line {}
    }.build()

    assertEquals(2, chart.layers.size())
    assertEquals(CharmGeomType.POINT, chart.layers[0].geomType)
    assertEquals(CharmGeomType.LINE, chart.layers[1].geomType)
  }

  @Test
  void testEndToEndRenderThroughUpdatedPipeline() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 3],
            [2, 5],
            [3, 4],
            [4, 6]
        ])
        .build()

    Chart chart = plot(data) {
      aes {
        x = col.x
        y = col.y
      }
      points {}
      theme {
        legend { position = 'none' }
      }
    }.build()

    Svg svg = chart.render()
    assertNotNull(svg)
    String svgXml = SvgWriter.toXml(svg)
    assertTrue(svgXml.contains('<svg'), 'Expected valid SVG XML output')
  }

  @Test
  void testAesExpandedChannels() {
    Aes aes = new Aes()
    aes.apply([
        x       : 'x',
        y       : 'y',
        xend    : 'xend',
        yend    : 'yend',
        xmin    : 'xmin',
        xmax    : 'xmax',
        ymin    : 'ymin',
        ymax    : 'ymax',
        alpha   : 'alpha',
        linetype: 'lt',
        label   : 'lbl',
        weight  : 'wt'
    ])

    assertEquals('x', aes.x.columnName())
    assertEquals('y', aes.y.columnName())
    assertEquals('xend', aes.xend.columnName())
    assertEquals('yend', aes.yend.columnName())
    assertEquals('xmin', aes.xmin.columnName())
    assertEquals('xmax', aes.xmax.columnName())
    assertEquals('ymin', aes.ymin.columnName())
    assertEquals('ymax', aes.ymax.columnName())
    assertEquals('alpha', aes.alpha.columnName())
    assertEquals('lt', aes.linetype.columnName())
    assertEquals('lbl', aes.label.columnName())
    assertEquals('wt', aes.weight.columnName())
  }

  @Test
  void testAesCopyPreservesExpandedChannels() {
    Aes aes = new Aes()
    aes.apply([xend: 'xe', yend: 'ye', alpha: 'a', label: 'l'])
    Aes copy = aes.copy()
    assertEquals('xe', copy.xend.columnName())
    assertEquals('ye', copy.yend.columnName())
    assertEquals('a', copy.alpha.columnName())
    assertEquals('l', copy.label.columnName())
  }

  @Test
  void testAesMappingsIncludesExpandedChannels() {
    Aes aes = new Aes()
    aes.apply([x: 'x', xmin: 'xm', weight: 'w'])
    Map<String, ColumnExpr> mappings = aes.mappings()
    assertTrue(mappings.containsKey('x'))
    assertTrue(mappings.containsKey('xmin'))
    assertTrue(mappings.containsKey('weight'))
    assertEquals(3, mappings.size())
  }
}
