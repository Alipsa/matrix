package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.scale.ScaleUtils

import static org.junit.jupiter.api.Assertions.assertEquals as assertEqualsJUnit
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.fail

class ScaleUtilsTest {

  /**
   * Custom assertEquals for BigDecimal that uses compareTo instead of equals.
   * This compares numeric value only, ignoring scale differences.
   *
   * @param expected the expected BigDecimal value
   * @param actual the actual BigDecimal value
   * @param message optional failure message
   */
  static void assertEquals(BigDecimal expected, BigDecimal actual, String message = '') {
    if (expected == null && actual == null) return
    if (expected == null || actual == null) {
      fail(message ?: "Expected ${expected} but was ${actual}")
    }
    if (expected.compareTo(actual) != 0) {
      fail(message ?: "Expected ${expected} but was ${actual}")
    }
  }

  /**
   * Custom assertEquals overload that accepts Number as expected value.
   * Converts the Number to BigDecimal before comparison.
   *
   * @param expected the expected numeric value
   * @param actual the actual BigDecimal value
   * @param message optional failure message
   */
  static void assertEquals(Number expected, BigDecimal actual, String message = '') {
    assertEquals(expected as BigDecimal, actual, message)
  }

  // Delegate to JUnit's assertEquals for non-BigDecimal types
  static void assertEquals(int expected, int actual) {
    assertEqualsJUnit(expected, actual)
  }

  // ========== coerceToNumber tests ==========

  @Test
  void testCoerceToNumberWithBigDecimal() {
    BigDecimal result = ScaleUtils.coerceToNumber(new BigDecimal('42.5'))
    assertEquals(new BigDecimal('42.5'), result)
  }

  @Test
  void testCoerceToNumberWithInteger() {
    BigDecimal result = ScaleUtils.coerceToNumber(42)
    assertEquals(new BigDecimal('42'), result)
  }

  @Test
  void testCoerceToNumberWithDouble() {
    BigDecimal result = ScaleUtils.coerceToNumber(42.5d)
    assertEquals(42.5, result)
  }

  @Test
  void testCoerceToNumberWithString() {
    BigDecimal result = ScaleUtils.coerceToNumber('42.5')
    assertEquals(new BigDecimal('42.5'), result)
  }

  @Test
  void testCoerceToNumberWithNull() {
    BigDecimal result = ScaleUtils.coerceToNumber(null)
    assertNull(result)
  }

  @Test
  void testCoerceToNumberWithNaN() {
    BigDecimal result = ScaleUtils.coerceToNumber(Double.NaN)
    assertNull(result)
  }

  @Test
  void testCoerceToNumberWithInfinity() {
    BigDecimal result = ScaleUtils.coerceToNumber(Double.POSITIVE_INFINITY)
    assertNull(result)
  }

  @Test
  void testCoerceToNumberWithEmptyString() {
    BigDecimal result = ScaleUtils.coerceToNumber('')
    assertNull(result)
  }

  @Test
  void testCoerceToNumberWithNAString() {
    BigDecimal result = ScaleUtils.coerceToNumber('NA')
    assertNull(result)
  }

  @Test
  void testCoerceToNumberWithNaNString() {
    BigDecimal result = ScaleUtils.coerceToNumber('NaN')
    assertNull(result)
  }

  @Test
  void testCoerceToNumberWithNullString() {
    BigDecimal result = ScaleUtils.coerceToNumber('null')
    assertNull(result)
  }

  @Test
  void testCoerceToNumberWithInvalidString() {
    BigDecimal result = ScaleUtils.coerceToNumber('not a number')
    assertNull(result)
  }

  // ========== linearTransform tests ==========

  @Test
  void testLinearTransformBasic() {
    // Map value 5 from domain [0, 10] to range [0, 100]
    BigDecimal result = ScaleUtils.linearTransform(5.0G, 0.0G, 10.0G, 0.0G, 100.0G)
    assertEquals(50, result)
  }

  @Test
  void testLinearTransformAtDomainMin() {
    BigDecimal result = ScaleUtils.linearTransform(0.0G, 0.0G, 10.0G, 0.0G, 100.0G)
    assertEquals(0, result)
  }

  @Test
  void testLinearTransformAtDomainMax() {
    BigDecimal result = ScaleUtils.linearTransform(10.0G, 0.0G, 10.0G, 0.0G, 100.0G)
    assertEquals(100, result)
  }

  @Test
  void testLinearTransformWithNegativeDomain() {
    // Map value 0 from domain [-10, 10] to range [0, 100]
    BigDecimal result = ScaleUtils.linearTransform(0.0G, -10.0G, 10.0G, 0.0G, 100.0G)
    assertEquals(50, result)
  }

  @Test
  void testLinearTransformWithDifferentRange() {
    // Map value 5 from domain [0, 10] to range [100, 200]
    BigDecimal result = ScaleUtils.linearTransform(5.0G, 0.0G, 10.0G, 100.0G, 200.0G)
    assertEquals(150, result)
  }

  @Test
  void testLinearTransformWithZeroRangeDomain() {
    // When domain is zero-range, should return midpoint of range
    BigDecimal result = ScaleUtils.linearTransform(5.0G, 5.0G, 5.0G, 0.0G, 100.0G)
    assertEquals(50, result)
  }

  @Test
  void testLinearTransformWithNullValue() {
    BigDecimal result = ScaleUtils.linearTransform(null, 0.0G, 10.0G, 0.0G, 100.0G)
    assertNull(result)
  }

  @Test
  void testLinearTransformOutsideDomain() {
    // Value outside domain should still work (extrapolation)
    BigDecimal result = ScaleUtils.linearTransform(15.0G, 0.0G, 10.0G, 0.0G, 100.0G)
    assertEquals(150, result)
  }

  // ========== linearInverse tests ==========

  @Test
  void testLinearInverseBasic() {
    // Map value 50 from range [0, 100] back to domain [0, 10]
    BigDecimal result = ScaleUtils.linearInverse(50.0G, 0.0G, 10.0G, 0.0G, 100.0G)
    assertEquals(5.0G, result)
  }

  @Test
  void testLinearInverseAtRangeMin() {
    BigDecimal result = ScaleUtils.linearInverse(0.0G, 0.0G, 10.0G, 0.0G, 100.0G)
    assertEquals(0, result)
  }

  @Test
  void testLinearInverseAtRangeMax() {
    BigDecimal result = ScaleUtils.linearInverse(100.0G, 0.0G, 10.0G, 0.0G, 100.0G)
    assertEquals(10, result)
  }

  @Test
  void testLinearInverseWithZeroRangeOutput() {
    // When output range is zero, should return midpoint of domain
    BigDecimal result = ScaleUtils.linearInverse(50.0G, 0.0G, 10.0G, 50.0G, 50.0G)
    assertEquals(5, result)
  }

  @Test
  void testLinearInverseWithNullValue() {
    BigDecimal result = ScaleUtils.linearInverse(null, 0.0G, 10.0G, 0.0G, 100.0G)
    assertNull(result)
  }

  @Test
  void testLinearInverseRoundTrip() {
    // Transform and inverse should be reversible
    BigDecimal original = 7.5G
    BigDecimal transformed = ScaleUtils.linearTransform(original, 0.0G, 10.0G, 0.0G, 100.0G)
    BigDecimal inverted = ScaleUtils.linearInverse(transformed, 0.0G, 10.0G, 0.0G, 100.0G)
    assertEquals(original, inverted)
  }

  // ========== linearTransformReversed tests ==========

  @Test
  void testLinearTransformReversedBasic() {
    // Map value 5 from domain [0, 10] to REVERSED range [0, 100]
    // 5 is at 50% of domain, so should map to 50% from the END (i.e., 50)
    BigDecimal result = ScaleUtils.linearTransformReversed(5.0G, 0.0G, 10.0G, 0.0G, 100.0G)
    assertEquals(50, result)
  }

  @Test
  void testLinearTransformReversedAtDomainMin() {
    // Domain min should map to range MAX
    BigDecimal result = ScaleUtils.linearTransformReversed(0.0G, 0.0G, 10.0G, 0.0G, 100.0G)
    assertEquals(100, result)
  }

  @Test
  void testLinearTransformReversedAtDomainMax() {
    // Domain max should map to range MIN
    BigDecimal result = ScaleUtils.linearTransformReversed(10.0G, 0.0G, 10.0G, 0.0G, 100.0G)
    assertEquals(0, result)
  }

  @Test
  void testLinearTransformReversedWithZeroRangeDomain() {
    // When domain is zero-range, should return midpoint of range
    BigDecimal result = ScaleUtils.linearTransformReversed(5.0G, 5.0G, 5.0G, 0.0G, 100.0G)
    assertEquals(50, result)
  }

  @Test
  void testLinearTransformReversedWithNullValue() {
    BigDecimal result = ScaleUtils.linearTransformReversed(null, 0.0G, 10.0G, 0.0G, 100.0G)
    assertNull(result)
  }

  // ========== linearInverseReversed tests ==========

  @Test
  void testLinearInverseReversedBasic() {
    // Map value 50 from REVERSED range [0, 100] back to domain [0, 10]
    BigDecimal result = ScaleUtils.linearInverseReversed(50.0G, 0.0G, 10.0G, 0.0G, 100.0G)
    assertEquals(5, result)
  }

  @Test
  void testLinearInverseReversedAtRangeMin() {
    // Range min should map to domain MAX
    BigDecimal result = ScaleUtils.linearInverseReversed(0.0G, 0.0G, 10.0G, 0.0G, 100.0G)
    assertEquals(10, result)
  }

  @Test
  void testLinearInverseReversedAtRangeMax() {
    // Range max should map to domain MIN
    BigDecimal result = ScaleUtils.linearInverseReversed(100.0G, 0.0G, 10.0G, 0.0G, 100.0G)
    assertEquals(0, result)
  }

  @Test
  void testLinearInverseReversedWithZeroRangeOutput() {
    // When output range is zero, should return midpoint of domain
    BigDecimal result = ScaleUtils.linearInverseReversed(50.0G, 0.0G, 10.0G, 50.0G, 50.0G)
    assertEquals(5, result)
  }

  @Test
  void testLinearInverseReversedWithNullValue() {
    BigDecimal result = ScaleUtils.linearInverseReversed(null, 0.0G, 10.0G, 0.0G, 100.0G)
    assertNull(result)
  }

  @Test
  void testLinearInverseReversedRoundTrip() {
    // Transform and inverse should be reversible
    BigDecimal original = 7.5G
    BigDecimal transformed = ScaleUtils.linearTransformReversed(original, 0.0G, 10.0G, 0.0G, 100.0G)
    BigDecimal inverted = ScaleUtils.linearInverseReversed(transformed, 0.0G, 10.0G, 0.0G, 100.0G)
    assertEquals(original, inverted)
  }

  // ========== midpoint tests ==========

  @Test
  void testMidpointBasic() {
    BigDecimal result = ScaleUtils.midpoint(0.0G, 10.0G)
    assertEquals(5, result)
  }

  @Test
  void testMidpointWithNegatives() {
    BigDecimal result = ScaleUtils.midpoint(-10.0G, 10.0G)
    assertEquals(0, result)
  }

  @Test
  void testMidpointWithDecimals() {
    BigDecimal result = ScaleUtils.midpoint(1.5G, 2.5G)
    assertEquals(2, result)
  }

  @Test
  void testMidpointWithSameValues() {
    BigDecimal result = ScaleUtils.midpoint(5.0G, 5.0G)
    assertEquals(5, result)
  }

  // ========== niceNum tests ==========

  @Test
  void testNiceNumWithZero() {
    BigDecimal result = ScaleUtils.niceNum(0.0G, true)
    assertEquals(0, result)
  }

  @Test
  void testNiceNumRoundingSmallNumber() {
    // 7.3: exp=0, f=7.3, f>=7 so nf=10, result=10*10^0=10
    BigDecimal result = ScaleUtils.niceNum(7.3G, true)
    assertEquals(10, result)
  }

  @Test
  void testNiceNumRoundingToTen() {
    // 8.5 should round to 10 (since f=8.5 falls in f>=7 range, nf=10, exp=0)
    BigDecimal result = ScaleUtils.niceNum(8.5G, true)
    assertEquals(10, result)
  }

  @Test
  void testNiceNumRoundingToTwo() {
    // 2.3 should round to 2 (since f=2.3 falls in 1.5<=f<3 range, nf=2, exp=0)
    BigDecimal result = ScaleUtils.niceNum(2.3G, true)
    assertEquals(2, result)
  }

  @Test
  void testNiceNumRoundingToOne() {
    // 1.2 should round to 1 (since f=1.2 falls in f<1.5 range, nf=1, exp=0)
    BigDecimal result = ScaleUtils.niceNum(1.2G, true)
    assertEquals(1, result)
  }

  @Test
  void testNiceNumWithLargeNumber() {
    // 73: exp=1, f=7.3, f>=7 so nf=10, result=10*10^1=100
    BigDecimal result = ScaleUtils.niceNum(73.0G, true)
    assertEquals(100, result)
  }

  @Test
  void testNiceNumWithVeryLargeNumber() {
    // 730: exp=2, f=7.3, f>=7 so nf=10, result=10*10^2=1000
    BigDecimal result = ScaleUtils.niceNum(730.0G, true)
    assertEquals(1000, result)
  }

  @Test
  void testNiceNumCeilingMode() {
    // With round=false, should take ceiling
    // 1.2: f=1.2, f>1 so nf=2, result=2
    BigDecimal result = ScaleUtils.niceNum(1.2G, false)
    assertEquals(2, result)
  }

  @Test
  void testNiceNumCeilingModeWithFive() {
    // With round=false, 4.5: f=4.5, f>2 and f<=5 so nf=5, result=5
    BigDecimal result = ScaleUtils.niceNum(4.5G, false)
    assertEquals(5, result)
  }

  @Test
  void testNiceNumCeilingModeWithTen() {
    // With round=false, 7: f=7, f>5 so nf=10, result=10
    BigDecimal result = ScaleUtils.niceNum(7.0G, false)
    assertEquals(10, result)
  }

  @Test
  void testNiceNumWithSmallDecimal() {
    // 0.073: exp=-2, f=7.3, f>=7 so nf=10, result=10*10^-2=0.1
    BigDecimal result = ScaleUtils.niceNum(0.073G, true)
    assertEquals(0.1, result)
  }

  @Test
  void testNiceNumWithNegativeNumber() {
    // -7.3: absX=7.3, exp=0, f=7.3, f>=7 so nf=10, result=-10
    BigDecimal result = ScaleUtils.niceNum(-7.3G, true)
    assertEquals(-10, result)
  }

  @Test
  void testNiceNumWithNegativeSmallNumber() {
    // -2.3: absX=2.3, exp=0, f=2.3, 1.5<=f<3 so nf=2, result=-2
    BigDecimal result = ScaleUtils.niceNum(-2.3G, true)
    assertEquals(-2, result)
  }

  @Test
  void testNiceNumWithNegativeLargeNumber() {
    // -73: absX=73, exp=1, f=7.3, f>=7 so nf=10, result=-100
    BigDecimal result = ScaleUtils.niceNum(-73.0G, true)
    assertEquals(-100, result)
  }

  @Test
  void testNiceNumWithNegativeDecimal() {
    // -0.073: absX=0.073, exp=-2, f=7.3, f>=7 so nf=10, result=-0.1
    BigDecimal result = ScaleUtils.niceNum(-0.073G, true)
    assertEquals(-0.1, result)
  }

  @Test
  void testNiceNumCeilingModeWithNegative() {
    // With round=false, -1.2: absX=1.2, f=1.2, f>1 so nf=2, result=-2
    BigDecimal result = ScaleUtils.niceNum(-1.2G, false)
    assertEquals(-2, result)
  }

  // ========== interpolateRange tests ==========

  @Test
  void testInterpolateRangeBasic() {
    List<Number> result = ScaleUtils.interpolateRange(5, [0, 100])
    assertEquals(5, result.size())
    assertEquals(0, result[0] as BigDecimal)
    assertEquals(25, result[1] as BigDecimal)
    assertEquals(50, result[2] as BigDecimal)
    assertEquals(75, result[3] as BigDecimal)
    assertEquals(100, result[4] as BigDecimal)
  }

  @Test
  void testInterpolateRangeWithSingleValue() {
    List<Number> result = ScaleUtils.interpolateRange(1, [0, 100])
    assertEquals(1, result.size())
    assertEquals(50, result[0] as BigDecimal) // Midpoint
  }

  @Test
  void testInterpolateRangeWithZeroValues() {
    List<Number> result = ScaleUtils.interpolateRange(0, [0, 100])
    assertEquals(0, result.size())
  }

  @Test
  void testInterpolateRangeWithNegativeRange() {
    List<Number> result = ScaleUtils.interpolateRange(3, [-10, 10])
    assertEquals(3, result.size())
    assertEquals(-10.0G, result[0] as BigDecimal)
    assertEquals(0.0G, result[1] as BigDecimal)
    assertEquals(10.0G, result[2] as BigDecimal)
  }
}
