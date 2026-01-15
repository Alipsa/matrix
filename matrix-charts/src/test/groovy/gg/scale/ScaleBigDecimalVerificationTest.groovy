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
    //println "\n=== ScaleContinuous Precision Test ==="

    ScaleContinuous scale = new ScaleContinuous()
    scale.expand = [0, 0] as List<Number>
    scale.range = [0, 100] as List<BigDecimal>
    scale.train([0, 10])

    // Test that transform returns BigDecimal
    def result = scale.transform(5)
    //println "transform(5) = $result (type: ${result.class.simpleName})"
    assertTrue(result instanceof BigDecimal, "transform should return BigDecimal")
    assertEquals(50.0, (result as BigDecimal).doubleValue(), 0.01)

    // Test inverse returns BigDecimal
    def inverseResult = scale.inverse(50)
    //println "inverse(50) = $inverseResult (type: ${inverseResult.class.simpleName})"
    assertTrue(inverseResult instanceof BigDecimal, "inverse should return BigDecimal")
    assertEquals(5.0, (inverseResult as BigDecimal).doubleValue(), 0.01)

    // Test NA handling
    def naResult = scale.transform(null)
    //println "transform(null) = $naResult"
    assertNull(naResult)

    //println "✓ ScaleContinuous BigDecimal precision verified"
  }

  @Test
  void testScaleLog10Precision() {
    //println "\n=== ScaleXLog10 Precision Test ==="

    ScaleXLog10 scale = new ScaleXLog10()
    scale.expand = null  // No expansion
    scale.range = [0, 300] as List<BigDecimal>
    scale.train([1, 10, 100, 1000])

    // Test transform returns BigDecimal
    def result = scale.transform(10)
    //println "transform(10) = $result (type: ${result.class.simpleName})"
    assertTrue(result instanceof BigDecimal, "transform should return BigDecimal")

    // Verify round trip
    def inverted = scale.inverse(result)
    //println "inverse($result) = $inverted (type: ${inverted.class.simpleName})"
    // Note: inverse may return BigDecimal or exact power (Integer)
    assertTrue(inverted instanceof BigDecimal, "inverse should return BigDecimal")
    assertEquals(10, inverted)

    // Test breaks are BigDecimal
    def breaks = scale.getComputedBreaks()
    //println "Breaks: $breaks"
    assertTrue(breaks.every { it instanceof BigDecimal }, "Breaks should be BigDecimal")

    //println "✓ ScaleXLog10 BigDecimal precision verified"
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
    //println "transform(16) = $result (type: ${result.class.simpleName})"
    assertTrue(result instanceof BigDecimal, "transform should return BigDecimal")

    // Verify round trip
    def inverted = scale.inverse(result)
    //println "inverse($result) = $inverted (type: ${inverted.class.simpleName})"
    assertTrue(inverted instanceof BigDecimal, "inverse should return BigDecimal")
    assertEquals(16G, inverted as BigDecimal, 0.01G)

    // Test breaks are BigDecimal
    def breaks = scale.getComputedBreaks()
    //println "Breaks: $breaks"
    assertTrue(breaks.every { it instanceof BigDecimal }, "Breaks should be BigDecimal")

    //println "✓ ScaleXSqrt BigDecimal precision verified"
  }

  @Test
  void testScaleSizeContinuousPrecision() {
    //println "\n=== ScaleSizeContinuous Precision Test ==="

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
    assertEquals(1G, min as BigDecimal, 0.01G)
    assertEquals(10G, max as BigDecimal, 0.01G)

    //println "✓ ScaleSizeContinuous BigDecimal precision verified"
  }

  @Test
  void testBreakGenerationWithBigDecimal() {
    //println "\n=== Break Generation BigDecimal Test ==="

    ScaleContinuous scale = new ScaleContinuous()
    scale.expand = [0, 0] as List<BigDecimal>
    scale.train([0, 100])

    def breaks = scale.getComputedBreaks()
    //println "Generated breaks: $breaks"

    // Verify all breaks are BigDecimal
    assertTrue(breaks.every { it instanceof BigDecimal }, "All breaks should be BigDecimal")

    /*
    // Verify breaks are "nice" numbers
    breaks.each { brk ->
      println "  Break: $brk (type: ${brk.class.simpleName})"
    }*/

    // Check labels formatting
    def labels = scale.getComputedLabels()
    //println "Generated labels: $labels"
    assertTrue(labels.every { it instanceof String }, "All labels should be String")

    //println "✓ Break generation BigDecimal verified"
  }

  @Test
  void testNAValueHandling() {
    //println "\n=== NA Value Handling Test ==="

    ScaleContinuous scale = new ScaleContinuous()
    scale.train([0, 100])

    // Test various NA forms
    assertNull(scale.transform(null), "null should return null")
    assertNull(scale.transform("NA"), "NA string should return null")
    assertNull(scale.transform("NaN"), "NaN string should return null")
    assertNull(scale.transform(Double.NaN), "Double.NaN should return null")

    /*
    println "NA values correctly handled:"
    println "  transform(null) = ${scale.transform(null)}"
    println "  transform('NA') = ${scale.transform('NA')}"
    println "  transform('NaN') = ${scale.transform('NaN')}"
    println "  transform(Double.NaN) = ${scale.transform(Double.NaN)}"

    println "✓ NA value handling verified"
     */
  }
}
