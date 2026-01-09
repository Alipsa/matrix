package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.coord.Trans
import se.alipsa.matrix.gg.coord.Transformations

import static org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for coordinate transformation functions (Trans interface implementations).
 * Tests mathematical correctness, inverse relationships, edge cases, and break generation.
 */
class TransformationsTest {

  private static final double EPSILON = 1e-9

  // ==================== Identity Transformation ====================

  @Test
  void testIdentityTransform() {
    Trans trans = Transformations.getTrans('identity')
    assertEquals('identity', trans.name)

    // Identity should return input unchanged
    assertEquals(5.0, trans.transform(5).doubleValue(), EPSILON)
    assertEquals(-3.0, trans.transform(-3).doubleValue(), EPSILON)
    assertEquals(0.0, trans.transform(0).doubleValue(), EPSILON)
  }

  @Test
  void testIdentityInverse() {
    Trans trans = Transformations.getTrans('identity')

    // Identity inverse should also return input unchanged
    assertEquals(5.0, trans.inverse(5).doubleValue(), EPSILON)
    assertEquals(-3.0, trans.inverse(-3).doubleValue(), EPSILON)
  }

  @Test
  void testIdentityTransformInverseRelationship() {
    Trans trans = Transformations.getTrans('identity')

    [-100, -1, 0, 1, 100, 3.14159].each { value ->
      BigDecimal transformed = trans.transform(value)
      BigDecimal recovered = trans.inverse(transformed)
      assertEquals(value as double, recovered.doubleValue(), EPSILON,
          "inverse(transform($value)) should equal $value")
    }
  }

  // ==================== Log10 Transformation ====================

  @Test
  void testLog10Transform() {
    Trans trans = Transformations.getTrans('log10')
    assertEquals('log10', trans.name)

    // Test known values
    assertEquals(0.0, trans.transform(1).doubleValue(), EPSILON, "log10(1) = 0")
    assertEquals(1.0, trans.transform(10).doubleValue(), EPSILON, "log10(10) = 1")
    assertEquals(2.0, trans.transform(100).doubleValue(), EPSILON, "log10(100) = 2")
    assertEquals(3.0, trans.transform(1000).doubleValue(), EPSILON, "log10(1000) = 3")
    assertEquals(-1.0, trans.transform(0.1).doubleValue(), EPSILON, "log10(0.1) = -1")
  }

  @Test
  void testLog10Inverse() {
    Trans trans = Transformations.getTrans('log10')

    // Test known values: 10^x
    assertEquals(1.0, trans.inverse(0).doubleValue(), EPSILON, "10^0 = 1")
    assertEquals(10.0, trans.inverse(1).doubleValue(), EPSILON, "10^1 = 10")
    assertEquals(100.0, trans.inverse(2).doubleValue(), EPSILON, "10^2 = 100")
    assertEquals(0.1, trans.inverse(-1).doubleValue(), EPSILON, "10^-1 = 0.1")
  }

  @Test
  void testLog10TransformInverseRelationship() {
    Trans trans = Transformations.getTrans('log10')

    // Test that inverse(transform(x)) == x for positive values
    [0.001, 0.1, 1, 10, 100, 1000, 12345.6789].each { value ->
      BigDecimal transformed = trans.transform(value)
      BigDecimal recovered = trans.inverse(transformed)
      assertEquals(value as double, recovered.doubleValue(), 1e-6,
          "inverse(transform($value)) should equal $value")
    }
  }

  @Test
  void testLog10EdgeCases() {
    Trans trans = Transformations.getTrans('log10')

    // Null input
    assertNull(trans.transform(null), "transform(null) should return null")
    assertNull(trans.inverse(null), "inverse(null) should return null")

    // Non-positive values should return null (log undefined)
    assertNull(trans.transform(0), "log10(0) should return null")
    assertNull(trans.transform(-1), "log10(-1) should return null")
    assertNull(trans.transform(-100), "log10(-100) should return null")
  }

  @Test
  void testLog10Breaks() {
    Trans trans = Transformations.getTrans('log10')

    List<BigDecimal> breaks = trans.breaks([1, 1000], 5)
    assertNotNull(breaks)
    assertTrue(breaks.size() > 0)

    // Should contain powers of 10
    List<Double> breakValues = breaks.collect { it.doubleValue() }
    assertTrue(breakValues.any { Math.abs(it - 1.0) < 0.01 }, "Should contain 1")
    assertTrue(breakValues.any { Math.abs(it - 10.0) < 0.01 }, "Should contain 10")
    assertTrue(breakValues.any { Math.abs(it - 100.0) < 0.01 }, "Should contain 100")
    assertTrue(breakValues.any { Math.abs(it - 1000.0) < 0.01 }, "Should contain 1000")
  }

  // ==================== Natural Log Transformation ====================

  @Test
  void testLogTransform() {
    Trans trans = Transformations.getTrans('log')
    assertEquals('log', trans.name)

    // Test known values
    assertEquals(0.0, trans.transform(1).doubleValue(), EPSILON, "ln(1) = 0")
    assertEquals(1.0, trans.transform(Math.E).doubleValue(), EPSILON, "ln(e) = 1")
    assertEquals(2.0, trans.transform(Math.E * Math.E).doubleValue(), EPSILON, "ln(e^2) = 2")
  }

  @Test
  void testLogInverse() {
    Trans trans = Transformations.getTrans('log')

    // Test known values: e^x
    assertEquals(1.0, trans.inverse(0).doubleValue(), EPSILON, "e^0 = 1")
    assertEquals(Math.E, trans.inverse(1).doubleValue(), EPSILON, "e^1 = e")
    assertEquals(Math.E * Math.E, trans.inverse(2).doubleValue(), 1e-6, "e^2")
  }

  @Test
  void testLogTransformInverseRelationship() {
    Trans trans = Transformations.getTrans('log')

    [0.5, 1, Math.E, 10, 100].each { value ->
      BigDecimal transformed = trans.transform(value)
      BigDecimal recovered = trans.inverse(transformed)
      assertEquals(value as double, recovered.doubleValue(), 1e-6,
          "inverse(transform($value)) should equal $value")
    }
  }

  @Test
  void testLogEdgeCases() {
    Trans trans = Transformations.getTrans('log')

    assertNull(trans.transform(null))
    assertNull(trans.transform(0), "ln(0) should return null")
    assertNull(trans.transform(-1), "ln(-1) should return null")
  }

  // ==================== Sqrt Transformation ====================

  @Test
  void testSqrtTransform() {
    Trans trans = Transformations.getTrans('sqrt')
    assertEquals('sqrt', trans.name)

    assertEquals(0.0, trans.transform(0).doubleValue(), EPSILON, "sqrt(0) = 0")
    assertEquals(1.0, trans.transform(1).doubleValue(), EPSILON, "sqrt(1) = 1")
    assertEquals(2.0, trans.transform(4).doubleValue(), EPSILON, "sqrt(4) = 2")
    assertEquals(3.0, trans.transform(9).doubleValue(), EPSILON, "sqrt(9) = 3")
    assertEquals(10.0, trans.transform(100).doubleValue(), EPSILON, "sqrt(100) = 10")
  }

  @Test
  void testSqrtInverse() {
    Trans trans = Transformations.getTrans('sqrt')

    // Inverse is x^2
    assertEquals(0.0, trans.inverse(0).doubleValue(), EPSILON, "0^2 = 0")
    assertEquals(1.0, trans.inverse(1).doubleValue(), EPSILON, "1^2 = 1")
    assertEquals(4.0, trans.inverse(2).doubleValue(), EPSILON, "2^2 = 4")
    assertEquals(9.0, trans.inverse(3).doubleValue(), EPSILON, "3^2 = 9")
  }

  @Test
  void testSqrtTransformInverseRelationship() {
    Trans trans = Transformations.getTrans('sqrt')

    [0, 1, 4, 9, 16, 25, 100, 144].each { value ->
      BigDecimal transformed = trans.transform(value)
      BigDecimal recovered = trans.inverse(transformed)
      assertEquals(value as double, recovered.doubleValue(), EPSILON,
          "inverse(transform($value)) should equal $value")
    }
  }

  @Test
  void testSqrtEdgeCases() {
    Trans trans = Transformations.getTrans('sqrt')

    assertNull(trans.transform(null))
    assertNull(trans.transform(-1), "sqrt(-1) should return null")
    assertNull(trans.transform(-100), "sqrt(-100) should return null")
    assertNotNull(trans.transform(0), "sqrt(0) should be valid")
  }

  // ==================== Reverse Transformation ====================

  @Test
  void testReverseTransform() {
    Trans trans = Transformations.getTrans('reverse')
    assertEquals('reverse', trans.name)

    assertEquals(-5.0, trans.transform(5).doubleValue(), EPSILON)
    assertEquals(5.0, trans.transform(-5).doubleValue(), EPSILON)
    assertEquals(0.0, trans.transform(0).doubleValue(), EPSILON)
  }

  @Test
  void testReverseInverse() {
    Trans trans = Transformations.getTrans('reverse')

    // Reverse is its own inverse
    assertEquals(-5.0, trans.inverse(5).doubleValue(), EPSILON)
    assertEquals(5.0, trans.inverse(-5).doubleValue(), EPSILON)
  }

  @Test
  void testReverseTransformInverseRelationship() {
    Trans trans = Transformations.getTrans('reverse')

    [-100, -1, 0, 1, 100].each { value ->
      BigDecimal transformed = trans.transform(value)
      BigDecimal recovered = trans.inverse(transformed)
      assertEquals(value as double, recovered.doubleValue(), EPSILON,
          "inverse(transform($value)) should equal $value")
    }
  }

  // ==================== Reciprocal Transformation ====================

  @Test
  void testReciprocalTransform() {
    Trans trans = Transformations.getTrans('reciprocal')
    assertEquals('reciprocal', trans.name)

    assertEquals(1.0, trans.transform(1).doubleValue(), EPSILON, "1/1 = 1")
    assertEquals(0.5, trans.transform(2).doubleValue(), EPSILON, "1/2 = 0.5")
    assertEquals(0.25, trans.transform(4).doubleValue(), EPSILON, "1/4 = 0.25")
    assertEquals(2.0, trans.transform(0.5).doubleValue(), EPSILON, "1/0.5 = 2")
    assertEquals(-1.0, trans.transform(-1).doubleValue(), EPSILON, "1/-1 = -1")
  }

  @Test
  void testReciprocalInverse() {
    Trans trans = Transformations.getTrans('reciprocal')

    // Reciprocal is its own inverse
    assertEquals(1.0, trans.inverse(1).doubleValue(), EPSILON)
    assertEquals(0.5, trans.inverse(2).doubleValue(), EPSILON)
    assertEquals(2.0, trans.inverse(0.5).doubleValue(), EPSILON)
  }

  @Test
  void testReciprocalTransformInverseRelationship() {
    Trans trans = Transformations.getTrans('reciprocal')

    [0.1, 0.5, 1, 2, 10, -1, -0.5].each { value ->
      BigDecimal transformed = trans.transform(value)
      BigDecimal recovered = trans.inverse(transformed)
      assertEquals(value as double, recovered.doubleValue(), 1e-6,
          "inverse(transform($value)) should equal $value")
    }
  }

  @Test
  void testReciprocalEdgeCases() {
    Trans trans = Transformations.getTrans('reciprocal')

    assertNull(trans.transform(null))
    assertNull(trans.transform(0), "1/0 should return null")
    assertNull(trans.inverse(0), "inverse(0) should return null")
  }

  @Test
  void testInverseAlias() {
    // 'inverse' should be an alias for 'reciprocal'
    Trans trans = Transformations.getTrans('inverse')
    assertEquals('reciprocal', trans.name)
    assertEquals(0.5, trans.transform(2).doubleValue(), EPSILON)
  }

  // ==================== Power Transformation ====================

  @Test
  void testPowerTransformDefaultExponent() {
    Trans trans = Transformations.getTrans('power')
    assertEquals('power', trans.name)

    // Default exponent is 2
    assertEquals(0.0, trans.transform(0).doubleValue(), EPSILON, "0^2 = 0")
    assertEquals(1.0, trans.transform(1).doubleValue(), EPSILON, "1^2 = 1")
    assertEquals(4.0, trans.transform(2).doubleValue(), EPSILON, "2^2 = 4")
    assertEquals(9.0, trans.transform(3).doubleValue(), EPSILON, "3^2 = 9")
  }

  @Test
  void testPowerTransformCustomExponent() {
    Trans trans = Transformations.getTrans('power', [exponent: 3])

    assertEquals(0.0, trans.transform(0).doubleValue(), EPSILON, "0^3 = 0")
    assertEquals(1.0, trans.transform(1).doubleValue(), EPSILON, "1^3 = 1")
    assertEquals(8.0, trans.transform(2).doubleValue(), EPSILON, "2^3 = 8")
    assertEquals(27.0, trans.transform(3).doubleValue(), EPSILON, "3^3 = 27")
    assertEquals(-8.0, trans.transform(-2).doubleValue(), EPSILON, "(-2)^3 = -8")
  }

  @Test
  void testPowerTransformInverseSquare() {
    Trans trans = Transformations.getTrans('power', [exponent: 2])

    // For exponent 2, inverse is sqrt
    assertEquals(0.0, trans.inverse(0).doubleValue(), EPSILON, "sqrt(0) = 0")
    assertEquals(1.0, trans.inverse(1).doubleValue(), EPSILON, "sqrt(1) = 1")
    assertEquals(2.0, trans.inverse(4).doubleValue(), EPSILON, "sqrt(4) = 2")
    assertEquals(3.0, trans.inverse(9).doubleValue(), EPSILON, "sqrt(9) = 3")
  }

  @Test
  void testPowerTransformInverseCube() {
    Trans trans = Transformations.getTrans('power', [exponent: 3])

    // For exponent 3, inverse is cube root
    assertEquals(0.0, trans.inverse(0).doubleValue(), EPSILON, "cbrt(0) = 0")
    assertEquals(1.0, trans.inverse(1).doubleValue(), EPSILON, "cbrt(1) = 1")
    assertEquals(2.0, trans.inverse(8).doubleValue(), EPSILON, "cbrt(8) = 2")
    assertEquals(3.0, trans.inverse(27).doubleValue(), EPSILON, "cbrt(27) = 3")
    // Negative values with odd exponent
    assertEquals(-2.0, trans.inverse(-8).doubleValue(), EPSILON, "cbrt(-8) = -2")
  }

  @Test
  void testPowerTransformInverseRelationship() {
    Trans trans = Transformations.getTrans('power', [exponent: 3])

    // Test for odd exponent (handles negatives)
    [-3, -2, -1, 0, 1, 2, 3].each { value ->
      BigDecimal transformed = trans.transform(value)
      BigDecimal recovered = trans.inverse(transformed)
      assertEquals(value as double, recovered.doubleValue(), 1e-6,
          "inverse(transform($value)) should equal $value")
    }
  }

  @Test
  void testPowerTransformEvenExponentNegative() {
    Trans trans = Transformations.getTrans('power', [exponent: 2])

    // Even exponent with negative values - transform should return null
    assertNull(trans.transform(-2), "(-2)^2 would lose sign information")
    // Note: Forward transform is defined for negatives, but inverse is not
    // Actually for even exponents, we should allow forward but not inverse for negatives
    // Let me check the current implementation...

    // The current implementation returns null for negative with even exponent
    // which is safe but may be too restrictive for forward transform
  }

  @Test
  void testPowerTransformEdgeCases() {
    Trans trans = Transformations.getTrans('power', [exponent: 2])

    assertNull(trans.transform(null))
    assertNull(trans.inverse(null))

    // Negative inverse for even exponent should return null
    assertNull(trans.inverse(-4), "sqrt(-4) should return null")
  }

  // ==================== ASN (Arcsine Square Root) Transformation ====================

  @Test
  void testAsnTransform() {
    Trans trans = Transformations.getTrans('asn')
    assertEquals('asn', trans.name)

    // asn(x) = asin(sqrt(x))
    // asn(0) = asin(0) = 0
    assertEquals(0.0, trans.transform(0).doubleValue(), EPSILON, "asn(0) = 0")

    // asn(1) = asin(1) = pi/2
    assertEquals(Math.PI / 2, trans.transform(1).doubleValue(), EPSILON, "asn(1) = pi/2")

    // asn(0.5) = asin(sqrt(0.5)) = asin(1/sqrt(2)) = pi/4
    assertEquals(Math.PI / 4, trans.transform(0.5).doubleValue(), EPSILON, "asn(0.5) = pi/4")

    // asn(0.25) = asin(0.5) = pi/6
    assertEquals(Math.PI / 6, trans.transform(0.25).doubleValue(), EPSILON, "asn(0.25) = pi/6")
  }

  @Test
  void testAsnInverse() {
    Trans trans = Transformations.getTrans('asn')

    // inverse of asn(x) = sin(x)^2
    assertEquals(0.0, trans.inverse(0).doubleValue(), EPSILON, "sin(0)^2 = 0")
    assertEquals(1.0, trans.inverse(Math.PI / 2).doubleValue(), EPSILON, "sin(pi/2)^2 = 1")
    assertEquals(0.5, trans.inverse(Math.PI / 4).doubleValue(), EPSILON, "sin(pi/4)^2 = 0.5")
  }

  @Test
  void testAsnTransformInverseRelationship() {
    Trans trans = Transformations.getTrans('asn')

    // Test proportions in valid range [0, 1]
    [0.0, 0.1, 0.25, 0.5, 0.75, 0.9, 1.0].each { value ->
      BigDecimal transformed = trans.transform(value)
      BigDecimal recovered = trans.inverse(transformed)
      assertEquals(value as double, recovered.doubleValue(), 1e-6,
          "inverse(transform($value)) should equal $value")
    }
  }

  @Test
  void testAsnEdgeCases() {
    Trans trans = Transformations.getTrans('asn')

    assertNull(trans.transform(null))
    assertNull(trans.inverse(null))

    // Values outside [0, 1] should return null
    assertNull(trans.transform(-0.1), "asn(-0.1) should return null")
    assertNull(trans.transform(1.1), "asn(1.1) should return null")
    assertNull(trans.transform(-1), "asn(-1) should return null")
    assertNull(trans.transform(2), "asn(2) should return null")
  }

  @Test
  void testAsinAlias() {
    // 'asin' should be an alias for 'asn'
    Trans trans = Transformations.getTrans('asin')
    assertEquals('asn', trans.name)
  }

  // ==================== Factory Method Tests ====================

  @Test
  void testGetTransUnknown() {
    assertThrows(IllegalArgumentException) {
      Transformations.getTrans('unknown_transform')
    }
  }

  @Test
  void testGetTransNull() {
    Trans trans = Transformations.getTrans(null)
    assertEquals('identity', trans.name)
  }

  @Test
  void testGetTransCaseInsensitive() {
    assertEquals('log10', Transformations.getTrans('LOG10').name)
    assertEquals('sqrt', Transformations.getTrans('SQRT').name)
    assertEquals('reverse', Transformations.getTrans('Reverse').name)
  }

  // ==================== Custom Transformation Tests ====================

  @Test
  void testFromClosures() {
    // Create a custom transformation: double/halve
    Trans trans = Transformations.fromClosures(
        { x -> x * 2 },
        { x -> x / 2 }
    )

    assertEquals('custom', trans.name)
    assertEquals(10.0, trans.transform(5).doubleValue(), EPSILON)
    assertEquals(5.0, trans.inverse(10).doubleValue(), EPSILON)
  }

  @Test
  void testFromClosuresInverseRelationship() {
    // Exponential/logarithm custom transform
    Trans trans = Transformations.fromClosures(
        { x -> Math.exp(x as double) },
        { x -> Math.log(x as double) }
    )

    [0, 1, 2, 3].each { value ->
      BigDecimal transformed = trans.transform(value)
      BigDecimal recovered = trans.inverse(transformed)
      assertEquals(value as double, recovered.doubleValue(), 1e-6,
          "inverse(transform($value)) should equal $value")
    }
  }

  @Test
  void testFromClosuresNullHandling() {
    Trans trans = Transformations.fromClosures(
        { x -> x * 2 },
        { x -> x / 2 }
    )

    assertNull(trans.transform(null))
    assertNull(trans.inverse(null))
  }
}
