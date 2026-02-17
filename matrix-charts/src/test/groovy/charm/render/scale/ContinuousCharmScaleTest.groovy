package charm.render.scale

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.Log10ScaleTransform
import se.alipsa.matrix.charm.ReverseScaleTransform
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.charm.render.scale.ContinuousCharmScale

import static org.junit.jupiter.api.Assertions.*

class ContinuousCharmScaleTest {

  private static void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual, String message = null) {
    assertNotNull(actual, message ?: "Expected $expected but got null")
    assertTrue(expected.compareTo(actual) == 0,
        message ?: "Expected $expected but got $actual")
  }

  private static void assertApproxEquals(BigDecimal expected, BigDecimal actual, BigDecimal tolerance = 0.01) {
    assertNotNull(actual, "Expected $expected but got null")
    assertTrue((expected - actual).abs() < tolerance,
        "Expected approximately $expected but got $actual")
  }

  @Test
  void testLinearTransformMapsToPixelRange() {
    ContinuousCharmScale scale = new ContinuousCharmScale(
        scaleSpec: Scale.continuous(),
        rangeStart: 0.0,
        rangeEnd: 400.0,
        domainMin: 0.0,
        domainMax: 100.0
    )

    assertBigDecimalEquals(0.0, scale.transform(0))
    assertBigDecimalEquals(200.0, scale.transform(50))
    assertBigDecimalEquals(400.0, scale.transform(100))
  }

  @Test
  void testTransformWithNonZeroDomain() {
    ContinuousCharmScale scale = new ContinuousCharmScale(
        scaleSpec: Scale.continuous(),
        rangeStart: 50.0,
        rangeEnd: 350.0,
        domainMin: 10.0,
        domainMax: 60.0
    )

    assertBigDecimalEquals(50.0, scale.transform(10))
    assertBigDecimalEquals(200.0, scale.transform(35))
    assertBigDecimalEquals(350.0, scale.transform(60))
  }

  @Test
  void testTransformNullReturnsNull() {
    ContinuousCharmScale scale = new ContinuousCharmScale(
        scaleSpec: Scale.continuous(),
        rangeStart: 0.0,
        rangeEnd: 100.0,
        domainMin: 0.0,
        domainMax: 10.0
    )

    assertNull(scale.transform(null))
  }

  @Test
  void testTransformNonNumericReturnsNull() {
    ContinuousCharmScale scale = new ContinuousCharmScale(
        scaleSpec: Scale.continuous(),
        rangeStart: 0.0,
        rangeEnd: 100.0,
        domainMin: 0.0,
        domainMax: 10.0
    )

    assertNull(scale.transform('abc'))
  }

  @Test
  void testTransformEqualDomainReturnsMidpoint() {
    ContinuousCharmScale scale = new ContinuousCharmScale(
        scaleSpec: Scale.continuous(),
        rangeStart: 0.0,
        rangeEnd: 400.0,
        domainMin: 5.0,
        domainMax: 5.0
    )

    assertBigDecimalEquals(200.0, scale.transform(5))
  }

  @Test
  void testNiceBreaksGeneratesRoundNumbers() {
    ContinuousCharmScale scale = new ContinuousCharmScale(
        scaleSpec: Scale.continuous(),
        rangeStart: 0.0,
        rangeEnd: 500.0,
        domainMin: 0.0,
        domainMax: 100.0
    )

    List<Object> ticks = scale.ticks(5)
    assertFalse(ticks.isEmpty())
    // Ticks should be round numbers in the range
    ticks.each { Object tick ->
      BigDecimal bd = tick as BigDecimal
      assertTrue(bd >= -1 && bd <= 101, "Tick $tick should be near domain range")
    }
  }

  @Test
  void testTicksWithSmallDomain() {
    ContinuousCharmScale scale = new ContinuousCharmScale(
        scaleSpec: Scale.continuous(),
        rangeStart: 0.0,
        rangeEnd: 400.0,
        domainMin: 0.0,
        domainMax: 1.0
    )

    List<Object> ticks = scale.ticks(5)
    assertFalse(ticks.isEmpty())
    ticks.each { Object tick ->
      BigDecimal bd = tick as BigDecimal
      assertTrue(bd >= -0.1 && bd <= 1.1, "Tick $tick should be near [0, 1]")
    }
  }

  @Test
  void testTickLabelsFormatsNumbers() {
    ContinuousCharmScale scale = new ContinuousCharmScale(
        scaleSpec: Scale.continuous(),
        rangeStart: 0.0,
        rangeEnd: 500.0,
        domainMin: 0.0,
        domainMax: 100.0
    )

    List<String> labels = scale.tickLabels(5)
    assertFalse(labels.isEmpty())
    assertEquals(scale.ticks(5).size(), labels.size())
    // Labels should be non-empty strings
    labels.each { String label ->
      assertFalse(label.isEmpty())
    }
  }

  @Test
  void testLog10TransformProducesPowerOf10Ticks() {
    ContinuousCharmScale scale = new ContinuousCharmScale(
        scaleSpec: Scale.transform('log10'),
        rangeStart: 0.0,
        rangeEnd: 400.0,
        domainMin: 0.0,  // log10(1) = 0
        domainMax: 3.0,   // log10(1000) = 3
        transformStrategy: new Log10ScaleTransform()
    )

    List<Object> ticks = scale.ticks(5)
    assertFalse(ticks.isEmpty())
    // Should include powers of 10
    List<BigDecimal> tickValues = ticks.collect { it as BigDecimal }
    assertTrue(tickValues.any { it == 1 || it == 10 || it == 100 || it == 1000 },
        "Log10 ticks should include powers of 10, got: $tickValues")
  }

  @Test
  void testLog10TickLabelsFormatCorrectly() {
    ContinuousCharmScale scale = new ContinuousCharmScale(
        scaleSpec: Scale.transform('log10'),
        rangeStart: 0.0,
        rangeEnd: 400.0,
        domainMin: 0.0,
        domainMax: 3.0,
        transformStrategy: new Log10ScaleTransform()
    )

    List<String> labels = scale.tickLabels(5)
    assertFalse(labels.isEmpty())
    // Log labels for powers of 10 should be clean integers
    assertTrue(labels.any { it == '1' || it == '10' || it == '100' || it == '1000' },
        "Log10 tick labels should include clean integer powers, got: $labels")
  }

  @Test
  void testReverseTransformReversesTickOrder() {
    ContinuousCharmScale scale = new ContinuousCharmScale(
        scaleSpec: Scale.transform('reverse'),
        rangeStart: 0.0,
        rangeEnd: 400.0,
        domainMin: -100.0,
        domainMax: 0.0,
        transformStrategy: new ReverseScaleTransform()
    )

    List<Object> ticks = scale.ticks(5)
    assertFalse(ticks.isEmpty())

    // Reversed scale should produce ticks in descending order
    for (int i = 0; i < ticks.size() - 1; i++) {
      BigDecimal current = ticks[i] as BigDecimal
      BigDecimal next = ticks[i + 1] as BigDecimal
      assertTrue(current >= next, "Reversed ticks should be in descending order: $ticks")
    }
  }

  @Test
  void testIsDiscreteReturnsFalse() {
    ContinuousCharmScale scale = new ContinuousCharmScale(
        scaleSpec: Scale.continuous(),
        rangeStart: 0.0,
        rangeEnd: 100.0,
        domainMin: 0.0,
        domainMax: 10.0
    )

    assertFalse(scale.isDiscrete())
  }

  @Test
  void testTicksWithNullDomainReturnsEmpty() {
    ContinuousCharmScale scale = new ContinuousCharmScale(
        scaleSpec: Scale.continuous(),
        rangeStart: 0.0,
        rangeEnd: 100.0,
        domainMin: null,
        domainMax: null
    )

    List<Object> ticks = scale.ticks(5)
    assertTrue(ticks.isEmpty())
  }

  @Test
  void testTransformWithStringNumbers() {
    ContinuousCharmScale scale = new ContinuousCharmScale(
        scaleSpec: Scale.continuous(),
        rangeStart: 0.0,
        rangeEnd: 100.0,
        domainMin: 0.0,
        domainMax: 10.0
    )

    // String representation of numbers should be coerced
    BigDecimal result = scale.transform('5')
    assertBigDecimalEquals(50.0, result)
  }

  @Test
  void testTransformWithLog10Strategy() {
    ContinuousCharmScale scale = new ContinuousCharmScale(
        scaleSpec: Scale.transform('log10'),
        rangeStart: 0.0,
        rangeEnd: 300.0,
        domainMin: 0.0,  // log10(1)
        domainMax: 3.0,   // log10(1000)
        transformStrategy: new Log10ScaleTransform()
    )

    // log10(10) = 1.0, which maps to 100.0 in [0, 300]
    BigDecimal result = scale.transform(10)
    assertApproxEquals(100.0, result)

    // log10(1000) = 3.0, which maps to 300.0
    BigDecimal result2 = scale.transform(1000)
    assertApproxEquals(300.0, result2)
  }
}
