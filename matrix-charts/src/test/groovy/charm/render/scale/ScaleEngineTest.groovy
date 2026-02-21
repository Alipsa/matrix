package charm.render.scale

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.Charts
import se.alipsa.matrix.charm.PlotSpec
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.charm.render.RenderConfig
import se.alipsa.matrix.charm.render.scale.BinnedCharmScale
import se.alipsa.matrix.charm.render.scale.CharmScale
import se.alipsa.matrix.charm.render.scale.ColorCharmScale
import se.alipsa.matrix.charm.render.scale.ContinuousCharmScale
import se.alipsa.matrix.charm.render.scale.DiscreteCharmScale
import se.alipsa.matrix.charm.render.scale.ScaleEngine
import se.alipsa.matrix.charm.render.scale.TrainedScales
import se.alipsa.matrix.core.Matrix
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static org.junit.jupiter.api.Assertions.*

class ScaleEngineTest {

  @Test
  void testAutoDetectContinuousForNumericData() {
    CharmScale scale = ScaleEngine.trainPositionalScale(
        [1, 2, 3, 4, 5], null, 0, 500
    )

    assertTrue(scale instanceof ContinuousCharmScale)
    assertFalse(scale.isDiscrete())
  }

  @Test
  void testAutoDetectDiscreteForStringData() {
    CharmScale scale = ScaleEngine.trainPositionalScale(
        ['A', 'B', 'C'], null, 0, 300
    )

    assertTrue(scale instanceof DiscreteCharmScale)
    assertTrue(scale.isDiscrete())
  }

  @Test
  void testForceContinuousWithSpec() {
    CharmScale scale = ScaleEngine.trainPositionalScale(
        ['A', 'B', 'C'], Scale.continuous(), 0, 300
    )

    // When spec says continuous, strings still won't parse as numeric -> domain 0,1
    assertTrue(scale instanceof ContinuousCharmScale)
  }

  @Test
  void testForceDiscreteWithSpec() {
    CharmScale scale = ScaleEngine.trainPositionalScale(
        [1, 2, 3], Scale.discrete(), 0, 300
    )

    assertTrue(scale instanceof DiscreteCharmScale)
    assertTrue(scale.isDiscrete())
    DiscreteCharmScale discrete = scale as DiscreteCharmScale
    assertEquals(['1', '2', '3'], discrete.levels)
  }

  @Test
  void testMixedDataTreatedAsDiscrete() {
    // Some non-numeric values trigger discrete mode
    CharmScale scale = ScaleEngine.trainPositionalScale(
        [1, 'B', 3], null, 0, 300
    )

    assertTrue(scale instanceof DiscreteCharmScale)
  }

  @Test
  void testEmptyDataProducesDefaultDomain() {
    CharmScale scale = ScaleEngine.trainPositionalScale(
        [], null, 0, 400
    )

    // Should produce a continuous scale with default domain [0, 1]
    assertTrue(scale instanceof ContinuousCharmScale)
    ContinuousCharmScale continuous = scale as ContinuousCharmScale
    assertEquals(0.0, continuous.domainMin)
    assertEquals(1.0, continuous.domainMax)
  }

  @Test
  void testNullValuesFilteredDuringTraining() {
    CharmScale scale = ScaleEngine.trainPositionalScale(
        [null, 5, null, 15, null], null, 0, 300
    )

    assertTrue(scale instanceof ContinuousCharmScale)
    ContinuousCharmScale continuous = scale as ContinuousCharmScale
    assertTrue((5.0 as BigDecimal).compareTo(continuous.domainMin) == 0,
        "Expected domainMin 5 but got ${continuous.domainMin}")
    assertTrue((15.0 as BigDecimal).compareTo(continuous.domainMax) == 0,
        "Expected domainMax 15 but got ${continuous.domainMax}")
  }

  @Test
  void testColorScaleTraining() {
    ColorCharmScale colorScale = ScaleEngine.trainColorScale(
        ['A', 'B', 'C'], null
    )

    assertNotNull(colorScale)
    assertEquals(3, colorScale.levels.size())
    assertNotNull(colorScale.colorFor('A'))
    assertNotNull(colorScale.colorFor('B'))
    assertNotNull(colorScale.colorFor('C'))
  }

  @Test
  void testColorScaleWithBrewerSpec() {
    ColorCharmScale colorScale = ScaleEngine.trainColorScale(
        ['X', 'Y', 'Z'], Scale.brewer('Set2')
    )

    assertNotNull(colorScale)
    assertEquals('brewer', colorScale.colorType)
    assertEquals(3, colorScale.levels.size())
  }

  @Test
  void testTrainAllChannels() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'color')
        .rows([
            [1, 10, 'A'],
            [2, 20, 'B'],
            [3, 30, 'A']
        ])
        .build()

    Chart chart = Charts.plot(data) {
      aes {
        x = col.x
        y = col.y
        color = col.color
      }
      points {}
    }.build()

    RenderConfig config = new RenderConfig(width: 800, height: 600)

    TrainedScales trained = ScaleEngine.train(
        chart, config,
        [1, 2, 3],       // x values
        [10, 20, 30],    // y values
        ['A', 'B', 'A'], // color values
        []                // fill values (empty)
    )

    assertNotNull(trained.x)
    assertNotNull(trained.y)
    assertNotNull(trained.color)
    assertNull(trained.fill) // No fill data
  }

  @Test
  void testTrainWithEmptyColorDataSkipsColorScale() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [3, 4]])
        .build()

    Chart chart = Charts.plot(data) {
      aes {
        x = col.x
        y = col.y
      }
      points {}
    }.build()

    RenderConfig config = new RenderConfig(width: 800, height: 600)

    TrainedScales trained = ScaleEngine.train(
        chart, config,
        [1, 3],          // x values
        [2, 4],          // y values
        [null, null],    // color values (all null)
        [null, null]     // fill values (all null)
    )

    assertNotNull(trained.x)
    assertNotNull(trained.y)
    assertNull(trained.color) // No non-null color data
    assertNull(trained.fill)  // No non-null fill data
  }

  @Test
  void testSingleValueDomainExpands() {
    CharmScale scale = ScaleEngine.trainPositionalScale(
        [5, 5, 5], null, 0, 400
    )

    assertTrue(scale instanceof ContinuousCharmScale)
    ContinuousCharmScale continuous = scale as ContinuousCharmScale
    // When all values are the same, domain expands by 1
    assertTrue(continuous.domainMax > continuous.domainMin,
        "Domain should expand for single-value data")
  }

  @Test
  void testLog10ScaleTraining() {
    CharmScale scale = ScaleEngine.trainPositionalScale(
        [1, 10, 100, 1000], Scale.transform('log10'), 0, 400
    )

    assertTrue(scale instanceof ContinuousCharmScale)
    ContinuousCharmScale continuous = scale as ContinuousCharmScale
    assertNotNull(continuous.transformStrategy)
    assertEquals('log10', continuous.transformStrategy.id())
  }

  @Test
  void testReverseScaleTraining() {
    CharmScale scale = ScaleEngine.trainPositionalScale(
        [10, 20, 30], Scale.transform('reverse'), 0, 300
    )

    assertTrue(scale instanceof ContinuousCharmScale)
    ContinuousCharmScale continuous = scale as ContinuousCharmScale
    assertNotNull(continuous.transformStrategy)
    assertEquals('reverse', continuous.transformStrategy.id())
  }

  @Test
  void testDiscreteScalePreservesLevelOrder() {
    CharmScale scale = ScaleEngine.trainPositionalScale(
        ['Z', 'A', 'M', 'B'], null, 0, 400
    )

    assertTrue(scale instanceof DiscreteCharmScale)
    DiscreteCharmScale discrete = scale as DiscreteCharmScale
    // LinkedHashSet insertion order should be preserved
    assertEquals(['Z', 'A', 'M', 'B'], discrete.levels)
  }

  @Test
  void testSqrtScaleTraining() {
    CharmScale scale = ScaleEngine.trainPositionalScale([1, 4, 9, 16], Scale.transform('sqrt'), 0, 400)
    assertTrue(scale instanceof ContinuousCharmScale)
    ContinuousCharmScale continuous = scale as ContinuousCharmScale
    assertEquals('sqrt', continuous.transformStrategy.id())
  }

  @Test
  void testDateScaleTrainingWithTemporalValues() {
    CharmScale scale = ScaleEngine.trainPositionalScale(
        [LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 10)],
        Scale.date(),
        0,
        500
    )
    assertTrue(scale instanceof ContinuousCharmScale)
    ContinuousCharmScale continuous = scale as ContinuousCharmScale
    assertEquals('date', continuous.transformStrategy.id())
    assertTrue(continuous.domainMax > continuous.domainMin)
  }

  @Test
  void testDatetimeScaleTrainingWithTemporalValues() {
    CharmScale scale = ScaleEngine.trainPositionalScale(
        [LocalDateTime.of(2025, 1, 1, 12, 0), LocalDateTime.of(2025, 1, 2, 18, 0)],
        Scale.date(),
        0,
        500
    )
    assertTrue(scale instanceof ContinuousCharmScale)
    ContinuousCharmScale continuous = scale as ContinuousCharmScale
    assertEquals('date', continuous.transformStrategy.id())
    assertTrue(continuous.domainMax > continuous.domainMin)
  }

  @Test
  void testTimeScaleTrainingWithTemporalValues() {
    CharmScale scale = ScaleEngine.trainPositionalScale(
        [LocalTime.of(6, 30), LocalTime.of(9, 45), LocalTime.of(12, 0)],
        Scale.time(),
        0,
        300
    )
    assertTrue(scale instanceof ContinuousCharmScale)
    ContinuousCharmScale continuous = scale as ContinuousCharmScale
    assertEquals('time', continuous.transformStrategy.id())
    assertTrue(continuous.domainMax > continuous.domainMin)
  }

  @Test
  void testBinnedPositionalScaleTraining() {
    CharmScale scale = ScaleEngine.trainPositionalScale((1..30).collect { it }, Scale.binned(), 0, 300)
    assertTrue(scale instanceof BinnedCharmScale)
    BinnedCharmScale binned = scale as BinnedCharmScale
    assertFalse(binned.binBoundaries.isEmpty())
    assertFalse(binned.binCenters.isEmpty())
  }

  @Test
  void testTrainLinetypeChannel() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'linetype')
        .rows([
            [1, 2, 'solid'],
            [2, 3, 'dashed'],
            [3, 4, 'dotted']
        ])
        .build()

    Chart chart = Charts.plot(data) {
      aes {
        x = col.x
        y = col.y
        linetype = col.linetype
      }
      line {}
    }.build()

    RenderConfig config = new RenderConfig(width: 800, height: 600)
    TrainedScales trained = ScaleEngine.train(
        chart, config,
        [1, 2, 3],
        [2, 3, 4],
        [],
        [],
        [],
        [],
        [],
        ['solid', 'dashed', 'dotted']
    )

    assertNotNull(trained.linetype)
    assertTrue(trained.linetype instanceof DiscreteCharmScale)
  }

  @Test
  void testP1ColorScaleVariants() {
    ColorCharmScale distiller = ScaleEngine.trainColorScale([1, 2, 3, 4], Scale.distiller('RdYlBu'))
    assertEquals('distiller', distiller.colorType)
    assertNotEquals(distiller.colorFor(1), distiller.colorFor(4))

    ColorCharmScale fermenter = ScaleEngine.trainColorScale([1, 2, 3, 4], Scale.fermenter('YlOrRd', 1, 4))
    assertEquals('fermenter', fermenter.colorType)
    assertNotEquals(fermenter.colorFor(1), fermenter.colorFor(4))

    ColorCharmScale grey = ScaleEngine.trainColorScale(['A', 'B', 'C'], Scale.grey())
    assertEquals('grey', grey.colorType)
    assertNotEquals(grey.colorFor('A'), grey.colorFor('C'))

    ColorCharmScale hue = ScaleEngine.trainColorScale(['A', 'B', 'C'], Scale.hue())
    assertEquals('hue', hue.colorType)
    assertNotEquals(hue.colorFor('A'), hue.colorFor('B'))

    ColorCharmScale steps = ScaleEngine.trainColorScale([1, 2, 3, 4], Scale.steps('#000000', '#ffffff', 4))
    assertEquals('steps', steps.colorType)
    assertNotEquals(steps.colorFor(1), steps.colorFor(4))
  }

  @Test
  void testTrainNonPositionalScalesRespectSpecTypes() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'size', 'shape', 'alpha', 'linetype')
        .rows([
            [1, 1, 5, 'cat1', 0.2, 'a'],
            [2, 2, 10, 'cat2', 0.5, 'b'],
            [3, 3, 15, 'cat3', 0.8, 'c']
        ])
        .build()

    PlotSpec spec = Charts.plot(data) {
      aes {
        x = col.x
        y = col.y
        size = col.size
        shape = col.shape
        alpha = col.alpha
        linetype = col.linetype
      }
      points {}
    }
    spec.scale.size = Scale.binned()
    spec.scale.shape = Scale.discrete()
    spec.scale.alpha = Scale.discrete()
    spec.scale.linetype = Scale.discrete()
    Chart chart = spec.build()

    RenderConfig config = new RenderConfig(width: 800, height: 600)
    TrainedScales trained = ScaleEngine.train(
        chart, config,
        [1, 2, 3],
        [1, 2, 3],
        [],
        [],
        [5, 10, 15],
        ['cat1', 'cat2', 'cat3'],
        [0.2, 0.5, 0.8],
        ['a', 'b', 'c']
    )

    assertTrue(trained.size instanceof BinnedCharmScale)
    assertTrue(trained.shape instanceof DiscreteCharmScale)
    assertTrue(trained.alpha instanceof DiscreteCharmScale)
    assertTrue(trained.linetype instanceof DiscreteCharmScale)
  }

  @Test
  void testSecondaryAxisMetadataPreserved() {
    Scale xScale = Scale.continuous().secondaryAxis([name: 'secondary-x', transform: '*2'])
    CharmScale trained = ScaleEngine.trainPositionalScale([1, 2, 3], xScale, 0, 100)
    assertEquals('secondary-x', trained.scaleSpec.params.secondaryAxis.name)
    assertEquals('*2', trained.scaleSpec.params.secondaryAxis.transform)
  }
}
