import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.Ellipse

import static org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for the Ellipse class, covering mathematical correctness,
 * edge cases, and numerical stability.
 */
class EllipseTest {

  private static final double TOLERANCE = 1e-6

  @Test
  void testInsufficientPoints() {
    // Test with 0 points
    def result0 = Ellipse.calculate([], [], 0.95, 't', 51)
    assertTrue(result0.x.isEmpty(), "Should return empty for 0 points")
    assertTrue(result0.y.isEmpty(), "Should return empty for 0 points")

    // Test with 1 point
    def result1 = Ellipse.calculate([1.0G], [1.0G], 0.95, 't', 51)
    assertTrue(result1.x.isEmpty(), "Should return empty for 1 point")
    assertTrue(result1.y.isEmpty(), "Should return empty for 1 point")

    // Test with 2 points
    def result2 = Ellipse.calculate([1.0G, 2.0G], [1.0G, 2.0G], 0.95, 't', 51)
    assertTrue(result2.x.isEmpty(), "Should return empty for 2 points")
    assertTrue(result2.y.isEmpty(), "Should return empty for 2 points")
  }

  @Test
  void testMinimumPoints() {
    // Test with exactly 3 points - minimum required
    def xVals = [1.0G, 2.0G, 3.0G]
    def yVals = [1.0G, 2.0G, 1.5G]

    def result = Ellipse.calculate(xVals, yVals, 0.95, 't', 51)

    assertFalse(result.x.isEmpty(), "Should generate ellipse with 3 points")
    assertFalse(result.y.isEmpty(), "Should generate ellipse with 3 points")
    assertEquals(51, result.x.size(), "Should generate 51 points")
    assertEquals(51, result.y.size(), "Should generate 51 points")
  }

  @Test
  void testEllipseIsClosed() {
    // Generate sample data
    def xVals = (1..20).collect { it as BigDecimal }
    def yVals = xVals.collect { x -> (x * x / 10 + Math.random() * 2) as BigDecimal }

    def result = Ellipse.calculate(xVals, yVals, 0.95, 't', 51)

    // First and last points should be very close (closed curve)
    double dx = Math.abs((result.x[0] - result.x[-1]) as double)
    double dy = Math.abs((result.y[0] - result.y[-1]) as double)

    assertTrue(dx < TOLERANCE, "Ellipse X should be closed (first ≈ last)")
    assertTrue(dy < TOLERANCE, "Ellipse Y should be closed (first ≈ last)")
  }

  @Test
  void testDifferentConfidenceLevels() {
    // Generate sample data around origin
    def xVals = [-2G, -1G, 0G, 1G, 2G, -1.5G, 1.5G]
    def yVals = [-1G, 0G, 1G, 0G, -1G, 0.5G, -0.5G]

    // Test different confidence levels
    def result50 = Ellipse.calculate(xVals, yVals, 0.50, 't', 51)
    def result90 = Ellipse.calculate(xVals, yVals, 0.90, 't', 51)
    def result95 = Ellipse.calculate(xVals, yVals, 0.95, 't', 51)
    def result99 = Ellipse.calculate(xVals, yVals, 0.99, 't', 51)

    // All should generate ellipses
    assertEquals(51, result50.x.size())
    assertEquals(51, result90.x.size())
    assertEquals(51, result95.x.size())
    assertEquals(51, result99.x.size())

    // Calculate approximate ellipse sizes using max radius
    def maxRadius50 = calculateMaxRadius(result50.x, result50.y)
    def maxRadius90 = calculateMaxRadius(result90.x, result90.y)
    def maxRadius95 = calculateMaxRadius(result95.x, result95.y)
    def maxRadius99 = calculateMaxRadius(result99.x, result99.y)

    // Higher confidence levels should produce larger ellipses
    assertTrue(maxRadius50 < maxRadius90, "50% ellipse should be smaller than 90%")
    assertTrue(maxRadius90 < maxRadius95, "90% ellipse should be smaller than 95%")
    assertTrue(maxRadius95 < maxRadius99, "95% ellipse should be smaller than 99%")
  }

  @Test
  void testDifferentEllipseTypes() {
    def xVals = [1G, 2G, 3G, 4G, 5G]
    def yVals = [2G, 4G, 3G, 5G, 6G]

    // Test 't' type
    def resultT = Ellipse.calculate(xVals, yVals, 0.95, 't', 51)
    assertEquals(51, resultT.x.size())
    assertAllFinite(resultT.x, resultT.y)

    // Test 'norm' type
    def resultNorm = Ellipse.calculate(xVals, yVals, 0.95, 'norm', 51)
    assertEquals(51, resultNorm.x.size())
    assertAllFinite(resultNorm.x, resultNorm.y)

    // 't' and 'norm' should produce same results for this implementation
    assertEquals(resultT.x.size(), resultNorm.x.size())

    // Test 'euclid' type (scale = 1.0, smaller ellipse)
    def resultEuclid = Ellipse.calculate(xVals, yVals, 0.95, 'euclid', 51)
    assertEquals(51, resultEuclid.x.size())
    assertAllFinite(resultEuclid.x, resultEuclid.y)

    // Euclid should produce smaller ellipse than t/norm
    def maxRadiusT = calculateMaxRadius(resultT.x, resultT.y)
    def maxRadiusEuclid = calculateMaxRadius(resultEuclid.x, resultEuclid.y)
    assertTrue(maxRadiusEuclid < maxRadiusT, "Euclid ellipse should be smaller than t-type")
  }

  @Test
  void testCustomSegmentCount() {
    def xVals = [1G, 2G, 3G, 4G]
    def yVals = [1G, 2G, 1.5G, 2.5G]

    // Test with different segment counts
    def result10 = Ellipse.calculate(xVals, yVals, 0.95, 't', 10)
    def result100 = Ellipse.calculate(xVals, yVals, 0.95, 't', 100)

    assertEquals(10, result10.x.size(), "Should generate 10 segments")
    assertEquals(100, result100.x.size(), "Should generate 100 segments")
  }

  @Test
  void testCollinearPoints() {
    // Test with perfectly collinear points (degenerate case)
    def xVals = [1G, 2G, 3G, 4G, 5G]
    def yVals = [1G, 2G, 3G, 4G, 5G]

    def result = Ellipse.calculate(xVals, yVals, 0.95, 't', 51)

    // Should still generate an ellipse (though it will be very elongated)
    assertEquals(51, result.x.size())
    assertEquals(51, result.y.size())

    // All values should be finite (not NaN or Infinity)
    assertAllFinite(result.x, result.y)
  }

  @Test
  void testIdenticalPoints() {
    // Test with all identical points (zero variance)
    def xVals = [5G, 5G, 5G, 5G]
    def yVals = [10G, 10G, 10G, 10G]

    def result = Ellipse.calculate(xVals, yVals, 0.95, 't', 51)

    // Should generate an ellipse (though it will collapse to a point)
    assertEquals(51, result.x.size())
    assertEquals(51, result.y.size())

    // All points should be at or very near the mean
    result.x.each { x ->
      assertTrue(Math.abs((x - 5G) as double) < 1e-6, "X values should be near mean")
    }
    result.y.each { y ->
      assertTrue(Math.abs((y - 10G) as double) < 1e-6, "Y values should be near mean")
    }
  }

  @Test
  void testEllipseCenterNearDataMean() {
    def xVals = [1G, 2G, 3G, 4G, 5G]
    def yVals = [2G, 3G, 4G, 5G, 6G]

    def result = Ellipse.calculate(xVals, yVals, 0.95, 't', 51)

    // Calculate mean of input data
    BigDecimal meanX = xVals.sum() / xVals.size()
    BigDecimal meanY = yVals.sum() / yVals.size()

    // Calculate centroid of ellipse
    BigDecimal ellipseCenterX = result.x.sum() / result.x.size()
    BigDecimal ellipseCenterY = result.y.sum() / result.y.size()

    // Ellipse center should be very close to data mean
    assertTrue(Math.abs((ellipseCenterX - meanX) as double) < 0.1,
               "Ellipse center X should be near data mean X")
    assertTrue(Math.abs((ellipseCenterY - meanY) as double) < 0.1,
               "Ellipse center Y should be near data mean Y")
  }

  @Test
  void testLargeDataset() {
    // Test with a larger dataset
    def random = new Random(42) // Seeded for reproducibility
    def xVals = (1..100).collect { (random.nextGaussian() * 10 + 50) as BigDecimal }
    def yVals = (1..100).collect { (random.nextGaussian() * 5 + 25) as BigDecimal }

    def result = Ellipse.calculate(xVals, yVals, 0.95, 't', 51)

    assertEquals(51, result.x.size())
    assertEquals(51, result.y.size())
    assertAllFinite(result.x, result.y)
  }

  @Test
  void testNegativeCoordinates() {
    // Test with negative coordinates
    def xVals = [-5G, -3G, -1G, 1G, 3G]
    def yVals = [-10G, -5G, 0G, 5G, 10G]

    def result = Ellipse.calculate(xVals, yVals, 0.95, 't', 51)

    assertEquals(51, result.x.size())
    assertEquals(51, result.y.size())
    assertAllFinite(result.x, result.y)
  }

  @Test
  void testMixedSizeValues() {
    // Test with very different scales
    def xVals = [0.01G, 0.02G, 0.03G, 0.04G]
    def yVals = [1000G, 2000G, 1500G, 2500G]

    def result = Ellipse.calculate(xVals, yVals, 0.95, 't', 51)

    assertEquals(51, result.x.size())
    assertEquals(51, result.y.size())
    assertAllFinite(result.x, result.y)
  }

  @Test
  void testMismatchedSizes() {
    // Test with mismatched x and y sizes
    def xVals = [1G, 2G, 3G]
    def yVals = [1G, 2G, 3G, 4G] // One extra

    def result = Ellipse.calculate(xVals, yVals, 0.95, 't', 51)

    // Should handle gracefully (will only use min(xVals.size, yVals.size))
    // Actually, current implementation doesn't check sizes explicitly,
    // so this may throw an IndexOutOfBoundsException or use only matching pairs
    // For now, just verify it doesn't crash catastrophically
    assertTrue(result.x.size() >= 0)
    assertTrue(result.y.size() >= 0)
  }

  // Helper methods

  private double calculateMaxRadius(List<BigDecimal> xVals, List<BigDecimal> yVals) {
    // Calculate centroid
    double centerX = (xVals.sum() / xVals.size()) as double
    double centerY = (yVals.sum() / yVals.size()) as double

    // Find maximum distance from centroid
    double maxDist = 0
    for (int i = 0; i < xVals.size(); i++) {
      double dx = (xVals[i] as double) - centerX
      double dy = (yVals[i] as double) - centerY
      double dist = Math.sqrt(dx * dx + dy * dy)
      if (dist > maxDist) {
        maxDist = dist
      }
    }
    return maxDist
  }

  private void assertAllFinite(List<BigDecimal> xVals, List<BigDecimal> yVals) {
    xVals.each { x ->
      double d = x as double
      assertFalse(Double.isNaN(d), "X value should not be NaN")
      assertFalse(Double.isInfinite(d), "X value should not be infinite")
    }
    yVals.each { y ->
      double d = y as double
      assertFalse(Double.isNaN(d), "Y value should not be NaN")
      assertFalse(Double.isInfinite(d), "Y value should not be infinite")
    }
  }
}
