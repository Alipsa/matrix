package charm.render.scale

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.charm.render.scale.BinnedCharmScale

import static org.junit.jupiter.api.Assertions.*

class BinnedCharmScaleTest {

  private static void assertApproxEquals(BigDecimal expected, BigDecimal actual, BigDecimal tolerance = 0.01) {
    assertNotNull(actual, "Expected $expected but got null")
    assertTrue((expected - actual).abs() < tolerance,
        "Expected approximately $expected but got $actual")
  }

  @Test
  void testBinAssignment() {
    BinnedCharmScale scale = new BinnedCharmScale(
        scaleSpec: Scale.binned(),
        rangeStart: 0.0,
        rangeEnd: 400.0,
        domainMin: 0.0,
        domainMax: 30.0,
        binBoundaries: [0.0, 10.0, 20.0, 30.0],
        binCenters: [5.0, 15.0, 25.0]
    )

    // Value 7 is in bin [0, 10], center = 5
    BigDecimal result = scale.transform(7)
    assertNotNull(result)

    // Value 15 is in bin [10, 20], center = 15
    BigDecimal result2 = scale.transform(15)
    assertNotNull(result2)

    // Different bins should produce different pixel values
    assertNotEquals(result, result2)
  }

  @Test
  void testBinCenterTransform() {
    BinnedCharmScale scale = new BinnedCharmScale(
        scaleSpec: Scale.binned(),
        rangeStart: 0.0,
        rangeEnd: 300.0,
        domainMin: 0.0,
        domainMax: 30.0,
        binBoundaries: [0.0, 10.0, 20.0, 30.0],
        binCenters: [5.0, 15.0, 25.0]
    )

    // Bin center 5 in domain [0, 30] -> pixel 50 in [0, 300]
    BigDecimal result1 = scale.transform(5)
    assertApproxEquals(50.0, result1)

    // Bin center 15 in domain [0, 30] -> pixel 150 in [0, 300]
    BigDecimal result2 = scale.transform(15)
    assertApproxEquals(150.0, result2)

    // Bin center 25 in domain [0, 30] -> pixel 250 in [0, 300]
    BigDecimal result3 = scale.transform(25)
    assertApproxEquals(250.0, result3)
  }

  @Test
  void testTicksReturnsBoundaries() {
    BinnedCharmScale scale = new BinnedCharmScale(
        scaleSpec: Scale.binned(),
        rangeStart: 0.0,
        rangeEnd: 300.0,
        domainMin: 0.0,
        domainMax: 30.0,
        binBoundaries: [0.0, 10.0, 20.0, 30.0],
        binCenters: [5.0, 15.0, 25.0]
    )

    List<Object> ticks = scale.ticks(5)
    assertEquals(4, ticks.size())
    assertEquals([0.0, 10.0, 20.0, 30.0], ticks)
  }

  @Test
  void testTickLabels() {
    BinnedCharmScale scale = new BinnedCharmScale(
        scaleSpec: Scale.binned(),
        rangeStart: 0.0,
        rangeEnd: 300.0,
        domainMin: 0.0,
        domainMax: 30.0,
        binBoundaries: [0.0, 10.0, 20.0, 30.0],
        binCenters: [5.0, 15.0, 25.0]
    )

    List<String> labels = scale.tickLabels(5)
    assertEquals(4, labels.size())
    assertEquals(['0', '10', '20', '30'], labels)
  }

  @Test
  void testTransformNullReturnsNull() {
    BinnedCharmScale scale = new BinnedCharmScale(
        scaleSpec: Scale.binned(),
        rangeStart: 0.0,
        rangeEnd: 300.0,
        domainMin: 0.0,
        domainMax: 30.0,
        binBoundaries: [0.0, 10.0, 20.0, 30.0],
        binCenters: [5.0, 15.0, 25.0]
    )

    assertNull(scale.transform(null))
  }

  @Test
  void testTransformNonNumericReturnsNull() {
    BinnedCharmScale scale = new BinnedCharmScale(
        scaleSpec: Scale.binned(),
        rangeStart: 0.0,
        rangeEnd: 300.0,
        domainMin: 0.0,
        domainMax: 30.0,
        binBoundaries: [0.0, 10.0, 20.0, 30.0],
        binCenters: [5.0, 15.0, 25.0]
    )

    assertNull(scale.transform('abc'))
  }

  @Test
  void testTransformEmptyBoundariesReturnsNull() {
    BinnedCharmScale scale = new BinnedCharmScale(
        scaleSpec: Scale.binned(),
        rangeStart: 0.0,
        rangeEnd: 300.0,
        domainMin: 0.0,
        domainMax: 30.0,
        binBoundaries: [],
        binCenters: []
    )

    assertNull(scale.transform(5))
  }

  @Test
  void testIsDiscreteReturnsFalse() {
    BinnedCharmScale scale = new BinnedCharmScale(
        scaleSpec: Scale.binned(),
        rangeStart: 0.0,
        rangeEnd: 300.0,
        domainMin: 0.0,
        domainMax: 30.0,
        binBoundaries: [0.0, 10.0, 20.0, 30.0],
        binCenters: [5.0, 15.0, 25.0]
    )

    assertFalse(scale.isDiscrete())
  }

  @Test
  void testValueClampsToBoundaryBins() {
    BinnedCharmScale scale = new BinnedCharmScale(
        scaleSpec: Scale.binned(),
        rangeStart: 0.0,
        rangeEnd: 300.0,
        domainMin: 0.0,
        domainMax: 30.0,
        binBoundaries: [0.0, 10.0, 20.0, 30.0],
        binCenters: [5.0, 15.0, 25.0]
    )

    // Value below first boundary clamps to first bin center
    BigDecimal below = scale.transform(-5)
    assertNotNull(below)

    // Value above last boundary clamps to last bin center
    BigDecimal above = scale.transform(35)
    assertNotNull(above)
  }
}
