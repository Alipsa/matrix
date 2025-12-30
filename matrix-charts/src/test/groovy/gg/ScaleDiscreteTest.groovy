package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.scale.ScaleDiscrete
import se.alipsa.matrix.gg.scale.ScaleXDiscrete
import se.alipsa.matrix.gg.scale.ScaleYDiscrete

import static org.junit.jupiter.api.Assertions.*

class ScaleDiscreteTest {

  @Test
  void testTrainExtractsUniqueLevels() {
    ScaleDiscrete scale = new ScaleDiscrete()
    scale.train(['A', 'B', 'A', 'C', 'B', 'A'])

    assertEquals(['A', 'B', 'C'], scale.getLevels())
    assertTrue(scale.isTrained())
  }

  @Test
  void testTrainSortsComparableLevels() {
    ScaleDiscrete scale = new ScaleDiscrete()
    scale.train(['C', 'A', 'B'])

    assertEquals(['A', 'B', 'C'], scale.getLevels())
  }

  @Test
  void testTrainWithIntegerLevels() {
    ScaleDiscrete scale = new ScaleDiscrete()
    scale.train([3, 1, 2, 1, 3])

    assertEquals([1, 2, 3], scale.getLevels())
  }

  @Test
  void testTrainWithExplicitLimits() {
    ScaleDiscrete scale = new ScaleDiscrete()
    scale.limits = ['A', 'B']
    scale.train(['A', 'B', 'C', 'D'])

    assertEquals(['A', 'B'], scale.getLevels())
  }

  @Test
  void testTrainWithOrderLevels() {
    ScaleDiscrete scale = new ScaleDiscrete()
    scale.orderLevels = ['C', 'B', 'A']
    scale.train(['A', 'B', 'C'])

    assertEquals(['C', 'B', 'A'], scale.getLevels())
  }

  @Test
  void testTransformMapsToPositions() {
    ScaleDiscrete scale = new ScaleDiscrete()
    scale.range = [0, 100] as List<Number>
    scale.train(['A', 'B', 'C'])

    // With 3 levels, bandwidth is 100/3 ≈ 33.33
    // Positions should be at center of each band
    double expectedA = 100.0 / 6  // 16.67 (center of first band)
    double expectedB = 100.0 / 2  // 50.0 (center of middle band)
    double expectedC = 100.0 * 5 / 6  // 83.33 (center of last band)

    assertEquals(expectedA, scale.transform('A') as double, 0.01)
    assertEquals(expectedB, scale.transform('B') as double, 0.01)
    assertEquals(expectedC, scale.transform('C') as double, 0.01)
  }

  @Test
  void testTransformReturnsNullForUnknownValue() {
    ScaleDiscrete scale = new ScaleDiscrete()
    scale.train(['A', 'B'])

    assertNull(scale.transform('X'))
    assertNull(scale.transform(null))
  }

  @Test
  void testInverseFindsCorrectLevel() {
    ScaleDiscrete scale = new ScaleDiscrete()
    scale.range = [0, 100] as List<Number>
    scale.train(['A', 'B', 'C'])

    assertEquals('A', scale.inverse(10))
    assertEquals('B', scale.inverse(50))
    assertEquals('C', scale.inverse(90))
  }

  @Test
  void testGetBandwidth() {
    ScaleDiscrete scale = new ScaleDiscrete()
    scale.range = [0, 100] as List<Number>
    scale.train(['A', 'B', 'C', 'D'])

    assertEquals(25.0, scale.getBandwidth(), 0.01)
  }

  @Test
  void testGetComputedBreaksReturnsLevels() {
    ScaleDiscrete scale = new ScaleDiscrete()
    scale.train(['X', 'Y', 'Z'])

    assertEquals(['X', 'Y', 'Z'], scale.getComputedBreaks())
  }

  @Test
  void testGetComputedBreaksWithExplicitBreaks() {
    ScaleDiscrete scale = new ScaleDiscrete()
    scale.breaks = ['A', 'C']
    scale.train(['A', 'B', 'C'])

    assertEquals(['A', 'C'], scale.getComputedBreaks())
  }

  @Test
  void testGetComputedLabels() {
    ScaleDiscrete scale = new ScaleDiscrete()
    scale.train(['Apple', 'Banana', 'Cherry'])

    assertEquals(['Apple', 'Banana', 'Cherry'], scale.getComputedLabels())
  }

  @Test
  void testGetComputedLabelsWithExplicitLabels() {
    ScaleDiscrete scale = new ScaleDiscrete()
    scale.labels = ['A', 'B', 'C']
    scale.train(['Apple', 'Banana', 'Cherry'])

    assertEquals(['A', 'B', 'C'], scale.getComputedLabels())
  }

  @Test
  void testSingleLevel() {
    ScaleDiscrete scale = new ScaleDiscrete()
    scale.range = [0, 100] as List<Number>
    scale.train(['Only'])

    // Single level should be centered
    assertEquals(50.0, scale.transform('Only') as double, 0.01)
  }

  @Test
  void testResetClearsState() {
    ScaleDiscrete scale = new ScaleDiscrete()
    scale.train(['A', 'B'])

    assertTrue(scale.isTrained())
    assertEquals(2, scale.getLevelCount())

    scale.reset()

    assertFalse(scale.isTrained())
    assertEquals(0, scale.getLevelCount())
  }

  // --- ScaleXDiscrete Tests ---

  @Test
  void testScaleXDiscreteHasCorrectAesthetic() {
    ScaleXDiscrete scale = new ScaleXDiscrete()
    assertEquals('x', scale.aesthetic)
  }

  @Test
  void testScaleXDiscreteWithParams() {
    ScaleXDiscrete scale = new ScaleXDiscrete(name: 'Categories', position: 'top')
    assertEquals('Categories', scale.name)
    assertEquals('top', scale.position)
  }

  @Test
  void testScaleXDiscreteAppliesExpansion() {
    ScaleXDiscrete scale = new ScaleXDiscrete()
    scale.range = [0, 300] as List<Number>
    scale.discreteExpand = [0, 0.5] as List<Number>
    scale.train(['A', 'B', 'C'])

    // With expansion, the usable range is smaller
    double bandwidth = scale.getBandwidth()
    assertTrue(bandwidth < 100, "Bandwidth should be less than 100 due to expansion")
  }

  @Test
  void testScaleXDiscreteFluentApi() {
    ScaleXDiscrete scale = new ScaleXDiscrete()
        .limits(['A', 'B'])
        .labels(['First', 'Second'])
        .expand(0.1, 0.5)

    assertEquals(['A', 'B'], scale.limits)
    assertEquals(['First', 'Second'], scale.labels)
    assertEquals([0.1, 0.5], scale.discreteExpand)
  }

  // --- ScaleYDiscrete Tests ---

  @Test
  void testScaleYDiscreteHasCorrectAesthetic() {
    ScaleYDiscrete scale = new ScaleYDiscrete()
    assertEquals('y', scale.aesthetic)
  }

  @Test
  void testScaleYDiscreteHandlesInvertedRange() {
    // SVG has y=0 at top, so range is typically [height, 0]
    ScaleYDiscrete scale = new ScaleYDiscrete()
    scale.range = [300, 0] as List<Number>  // Inverted for SVG
    scale.train(['A', 'B', 'C'])

    // First level should be near bottom (higher y value)
    // Last level should be near top (lower y value)
    double posA = scale.transform('A') as double
    double posC = scale.transform('C') as double

    assertTrue(posA > posC, "A should have higher y position than C in inverted coords")
  }

  @Test
  void testScaleYDiscreteWithParams() {
    ScaleYDiscrete scale = new ScaleYDiscrete(position: 'right')
    assertEquals('right', scale.position)
  }
}
