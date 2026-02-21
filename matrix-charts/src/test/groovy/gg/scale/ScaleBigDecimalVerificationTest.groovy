package gg.scale

import gg.BaseTest
import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.scale.ScaleContinuous
import se.alipsa.matrix.gg.scale.ScaleXLog10
import se.alipsa.matrix.gg.scale.ScaleXSqrt
import se.alipsa.matrix.gg.scale.ScaleSizeContinuous

import static org.junit.jupiter.api.Assertions.*

/**
 * Manual verification tests for BigDecimal-based scale implementations.
 * Tests precision and behavior with known expected values.
 */
class ScaleBigDecimalVerificationTest extends BaseTest {

  @Test
  void testScaleContinuousPrecision() {
    ScaleContinuous scale = new ScaleContinuous()
    scale.expand = [0, 0] as List<Number>
    scale.range = [0, 100] as List<BigDecimal>
    scale.train([0, 10])

    // Test that transform returns BigDecimal
    def result = scale.transform(5)
    assertTrue(result instanceof BigDecimal, "transform should return BigDecimal but was ${result?.class?.simpleName}")
    assertEquals(50.0, result as BigDecimal, 0.01)

    // Test inverse returns BigDecimal
    def inverseResult = scale.inverse(50)
    assertTrue(inverseResult instanceof BigDecimal, "inverse should return BigDecimal but was ${inverseResult?.class?.simpleName}")
    assertEquals(5.0, inverseResult as BigDecimal, 0.01)

    // Test NA handling
    def naResult = scale.transform(null)
    assertNull(naResult)
  }

  @Test
  void testScaleLog10Precision() {
    ScaleXLog10 scale = new ScaleXLog10()
    scale.expand = null  // No expansion
    scale.range = [0, 300] as List<BigDecimal>
    scale.train([1, 10, 100, 1000])

    // Test transform returns BigDecimal
    def result = scale.transform(10)
    assertTrue(result instanceof BigDecimal, "transform should return BigDecimal but was ${result?.class?.simpleName}")

    // Verify round trip
    def inverted = scale.inverse(result)
    // Note: inverse may return BigDecimal or exact power (Integer)
    assertTrue(inverted instanceof BigDecimal, "inverse should return BigDecimal but was ${inverted?.class?.simpleName}")
    assertEquals(10.0, (inverted as BigDecimal), 1.0E-6)

    // Test breaks are BigDecimal
    def breaks = scale.getComputedBreaks()
    //println "Breaks: $breaks"
    assertTrue(breaks.every { it instanceof BigDecimal }, "Breaks should be BigDecimal but were ${breaks.collect { it?.class?.simpleName }}")
  }

  @Test
  void testScaleSqrtPrecision() {
    //println "\n=== ScaleXSqrt Precision Test ==="

    ScaleXSqrt scale = new ScaleXSqrt()
    scale.expand = null  // No expansion
    scale.range = [0, 100] as List<BigDecimal>
    scale.train([0, 4, 16, 64, 100])

    // Test transform returns BigDecimal
    def result = scale.transform(16)
    assertTrue(result instanceof BigDecimal, "transform should return BigDecimal but was ${result?.class?.simpleName}")

    // Verify round trip
    def inverted = scale.inverse(result)
    assertTrue(inverted instanceof BigDecimal, "inverse should return BigDecimal but was ${inverted?.class?.simpleName}")
    assertEquals(16, inverted as BigDecimal, 0.01)

    // Test breaks are BigDecimal
    def breaks = scale.getComputedBreaks()
    //println "Breaks: $breaks"
    assertTrue(breaks.every { it instanceof BigDecimal }, "Breaks should be BigDecimal")
  }

  @Test
  void testScaleSizeContinuousPrecision() {

    ScaleSizeContinuous scale = new ScaleSizeContinuous()
    scale.range = [1, 10] as List<BigDecimal>
    scale.train([0, 50, 100])

    // Test transform returns BigDecimal
    def result = scale.transform(50)
    //println "transform(50) = $result (type: ${result.class.simpleName})"
    assertTrue(result instanceof BigDecimal, "transform should return BigDecimal")

    // Verify range mapping
    def min = scale.transform(0)
    def max = scale.transform(100)
    //println "transform(0) = $min, transform(100) = $max"
    assertEquals(1G, min as BigDecimal, 0.01)
    assertEquals(10G, max as BigDecimal, 0.01)
  }

  @Test
  void testBreakGenerationWithBigDecimal() {
    ScaleContinuous scale = new ScaleContinuous()
    scale.expand = [0, 0] as List<BigDecimal>
    scale.train([0, 100])

    def breaks = scale.getComputedBreaks()

    // Verify all breaks are BigDecimal
    assertTrue(breaks.every { it instanceof BigDecimal }, "All breaks should be BigDecimal")

    // Check labels formatting
    def labels = scale.getComputedLabels()
    //println "Generated labels: $labels"
    assertTrue(labels.every { it instanceof String }, "All labels should be String")
  }

  @Test
  void testNAValueHandling() {
    ScaleContinuous scale = new ScaleContinuous()
    scale.train([0, 100])

    // Test various NA forms
    assertNull(scale.transform(null), "null should return null")
    assertNull(scale.transform("NA"), "NA string should return null")
    assertNull(scale.transform("NaN"), "NaN string should return null")
    assertNull(scale.transform(Double.NaN), "Double.NaN should return null")
  }
}
