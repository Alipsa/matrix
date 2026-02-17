package charm.render.scale

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.Charts
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.charm.render.RenderConfig
import se.alipsa.matrix.charm.render.scale.CharmScale
import se.alipsa.matrix.charm.render.scale.ColorCharmScale
import se.alipsa.matrix.charm.render.scale.ContinuousCharmScale
import se.alipsa.matrix.charm.render.scale.DiscreteCharmScale
import se.alipsa.matrix.charm.render.scale.ScaleEngine
import se.alipsa.matrix.charm.render.scale.TrainedScales
import se.alipsa.matrix.core.Matrix

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
}
