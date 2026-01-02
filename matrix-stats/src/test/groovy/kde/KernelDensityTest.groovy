package kde

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.kde.BandwidthSelector
import se.alipsa.matrix.stats.kde.Kernel
import se.alipsa.matrix.stats.kde.KernelDensity

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for Kernel Density Estimation.
 *
 * R reference code for verification:
 * <pre>
 * x <- c(1.2, 2.3, 1.8, 2.1, 3.5, 2.9)
 * d <- density(x, kernel = "gaussian")
 * d$bw      # bandwidth: 0.4311
 * d$x[256]  # midpoint x
 * d$y[256]  # midpoint density
 * </pre>
 */
class KernelDensityTest {

  static final double TOLERANCE = 1e-6

  // Test data
  static final List<Double> TEST_DATA = [1.2, 2.3, 1.8, 2.1, 3.5, 2.9]

  @Test
  void testKernelEvaluation() {
    // Gaussian at u=0 should be 1/sqrt(2*pi) ≈ 0.3989
    assertEquals(0.3989422804, Kernel.GAUSSIAN.evaluate(0), 1e-6, "Gaussian at u=0")
    assertEquals(0.2419707245, Kernel.GAUSSIAN.evaluate(1), 1e-6, "Gaussian at u=1")

    // Epanechnikov at u=0 should be 0.75
    assertEquals(0.75, Kernel.EPANECHNIKOV.evaluate(0), TOLERANCE, "Epanechnikov at u=0")
    assertEquals(0.0, Kernel.EPANECHNIKOV.evaluate(1.5), TOLERANCE, "Epanechnikov at u=1.5")

    // Uniform at u=0 should be 0.5
    assertEquals(0.5, Kernel.UNIFORM.evaluate(0), TOLERANCE, "Uniform at u=0")
    assertEquals(0.0, Kernel.UNIFORM.evaluate(1.5), TOLERANCE, "Uniform at u=1.5")

    // Triangular at u=0 should be 1.0
    assertEquals(1.0, Kernel.TRIANGULAR.evaluate(0), TOLERANCE, "Triangular at u=0")
    assertEquals(0.5, Kernel.TRIANGULAR.evaluate(0.5), TOLERANCE, "Triangular at u=0.5")
    assertEquals(0.0, Kernel.TRIANGULAR.evaluate(1.5), TOLERANCE, "Triangular at u=1.5")
  }

  @Test
  void testKernelFromString() {
    assertEquals(Kernel.GAUSSIAN, Kernel.fromString("gaussian"))
    assertEquals(Kernel.GAUSSIAN, Kernel.fromString("GAUSSIAN"))
    assertEquals(Kernel.GAUSSIAN, Kernel.fromString("normal"))
    assertEquals(Kernel.EPANECHNIKOV, Kernel.fromString("epanechnikov"))
    assertEquals(Kernel.EPANECHNIKOV, Kernel.fromString("epan"))
    assertEquals(Kernel.UNIFORM, Kernel.fromString("uniform"))
    assertEquals(Kernel.UNIFORM, Kernel.fromString("rectangular"))
    assertEquals(Kernel.TRIANGULAR, Kernel.fromString("triangular"))
    assertEquals(Kernel.TRIANGULAR, Kernel.fromString("triangle"))

    // Default for null
    assertEquals(Kernel.GAUSSIAN, Kernel.fromString(null))

    // Unknown kernel should throw
    assertThrows(IllegalArgumentException) {
      Kernel.fromString("unknown")
    }
  }

  @Test
  void testSilvermanBandwidth() {
    /*
     * R verification:
     * x <- c(1.2, 2.3, 1.8, 2.1, 3.5, 2.9)
     * bw.nrd0(x)  # Silverman's rule: 0.4311338
     */
    double[] data = TEST_DATA as double[]
    Arrays.sort(data)
    double bw = BandwidthSelector.silverman(data)

    // Should be approximately 0.43 (R gives 0.4311338)
    assertTrue(bw > 0.35 && bw < 0.55, "Silverman bandwidth should be ~0.43, got: ${bw}")
  }

  @Test
  void testScottBandwidth() {
    /*
     * Scott's original rule: h = 3.49 * σ * n^(-1/3)
     * For n=6 and σ≈0.79: h ≈ 3.49 * 0.79 * 0.55 ≈ 1.52
     * Note: R's bw.nrd uses a different formula (1.06 * σ * n^(-1/5))
     */
    double[] data = TEST_DATA as double[]
    Arrays.sort(data)
    double bw = BandwidthSelector.scott(data)

    // Scott's rule gives larger bandwidth than Silverman for small n
    assertTrue(bw > 1.0 && bw < 2.0, "Scott bandwidth should be ~1.5 for n=6, got: ${bw}")
  }

  @Test
  void testBasicKDE() {
    def kde = new KernelDensity(TEST_DATA)

    assertEquals(Kernel.GAUSSIAN, kde.kernel, "Default kernel should be Gaussian")
    assertEquals(512, kde.n, "Default n should be 512")
    assertTrue(kde.bandwidth > 0, "Bandwidth should be positive")
    assertEquals(TEST_DATA.size(), kde.dataCount, "Data count should match")

    // Density should be non-negative everywhere
    for (double d : kde.density) {
      assertTrue(d >= 0, "Density should be non-negative")
    }

    // Density should integrate to approximately 1 (within reason)
    // Note: numerical integration with finite grid may not be exactly 1,
    // especially when extending beyond data range is limited
    double sum = 0
    double step = (kde.x[kde.n - 1] - kde.x[0]) / (kde.n - 1)
    for (double d : kde.density) {
      sum += d * step
    }
    assertTrue(sum > 0.85 && sum < 1.15, "Density should integrate to ~1, got: ${sum}")
  }

  @Test
  void testDifferentKernels() {
    // All kernels should produce valid density estimates
    for (Kernel k : Kernel.values()) {
      def kde = new KernelDensity(TEST_DATA, [kernel: k])
      assertEquals(k, kde.kernel, "Kernel should match: ${k}")

      // All densities should be non-negative
      for (double d : kde.density) {
        assertTrue(d >= 0, "Density should be non-negative for kernel ${k}")
      }
    }
  }

  @Test
  void testCustomBandwidth() {
    def kde = new KernelDensity(TEST_DATA, [bandwidth: 0.5])
    assertEquals(0.5, kde.bandwidth, TOLERANCE, "Custom bandwidth should be used")
  }

  @Test
  void testAdjustParameter() {
    def kde1 = new KernelDensity(TEST_DATA)
    def kde2 = new KernelDensity(TEST_DATA, [adjust: 2.0])

    assertEquals(kde1.bandwidth * 2, kde2.bandwidth, TOLERANCE,
        "Adjust=2 should double bandwidth")
  }

  @Test
  void testDensityAtPoint() {
    def kde = new KernelDensity(TEST_DATA)

    // Density at mean should be relatively high
    double mean = TEST_DATA.sum() / TEST_DATA.size()
    double densityAtMean = kde.density(mean)
    assertTrue(densityAtMean > 0, "Density at mean should be positive")

    // Density far from data should be low
    double densityFarAway = kde.density(100.0)
    assertTrue(densityFarAway < densityAtMean / 100,
        "Density far from data should be much lower than at mean")
  }

  @Test
  void testDensityAtMultiplePoints() {
    def kde = new KernelDensity(TEST_DATA)
    List<Double> points = [1.0, 2.0, 3.0]
    List<Double> densities = kde.density(points)

    assertEquals(points.size(), densities.size(), "Should return same number of densities as points")
    for (double d : densities) {
      assertTrue(d >= 0, "All densities should be non-negative")
    }
  }

  @Test
  void testFromMatrix() {
    Matrix m = Matrix.builder()
        .matrixName("test")
        .columns([
            'values': TEST_DATA,
            'other': [1, 2, 3, 4, 5, 6]
        ])
        .types(Double, Integer)
        .build()

    def kde = new KernelDensity(m, 'values')
    assertEquals(TEST_DATA.size(), kde.dataCount, "Data count should match column length")
    assertTrue(kde.bandwidth > 0, "Bandwidth should be positive")
  }

  @Test
  void testFromMatrixWithParams() {
    Matrix m = Matrix.builder()
        .columns(['x': TEST_DATA])
        .types(Double)
        .build()

    def kde = new KernelDensity(m, 'x', [kernel: 'epanechnikov', n: 256])
    assertEquals(Kernel.EPANECHNIKOV, kde.kernel)
    assertEquals(256, kde.n)
  }

  @Test
  void testToMatrix() {
    def kde = new KernelDensity(TEST_DATA, [n: 100])
    Matrix result = kde.toMatrix()

    assertEquals(100, result.rowCount(), "Result should have n rows")
    assertEquals(['x', 'density'], result.columnNames(), "Should have x and density columns")

    // Verify x values are increasing
    for (int i = 1; i < result.rowCount(); i++) {
      assertTrue((result[i, 'x'] as double) > (result[i - 1, 'x'] as double),
          "X values should be increasing")
    }
  }

  @Test
  void testToPointList() {
    def kde = new KernelDensity(TEST_DATA, [n: 50])
    List<double[]> points = kde.toPointList()

    assertEquals(50, points.size(), "Should return n points")
    for (double[] pt : points) {
      assertEquals(2, pt.length, "Each point should be [x, density]")
      assertTrue(pt[1] >= 0, "Density should be non-negative")
    }
  }

  @Test
  void testTrimParameter() {
    def kdeNoTrim = new KernelDensity(TEST_DATA, [trim: false])
    def kdeTrim = new KernelDensity(TEST_DATA, [trim: true])

    // Without trim, evaluation range should extend beyond data
    assertTrue(kdeNoTrim.x[0] < kdeNoTrim.dataMin,
        "Without trim, evaluation should start before data min")
    assertTrue(kdeNoTrim.x[kdeNoTrim.n - 1] > kdeNoTrim.dataMax,
        "Without trim, evaluation should end after data max")

    // With trim, evaluation range should match data range
    assertTrue(Math.abs(kdeTrim.x[0] - kdeTrim.dataMin) < 0.01,
        "With trim, evaluation should start at data min")
    assertTrue(Math.abs(kdeTrim.x[kdeTrim.n - 1] - kdeTrim.dataMax) < 0.01,
        "With trim, evaluation should end at data max")
  }

  @Test
  void testCustomRange() {
    def kde = new KernelDensity(TEST_DATA, [from: 0.0, to: 5.0])

    assertEquals(0.0, kde.x[0], TOLERANCE, "Should start at custom 'from' value")
    assertEquals(5.0, kde.x[kde.n - 1], TOLERANCE, "Should end at custom 'to' value")
  }

  @Test
  void testNullHandling() {
    List<Double> dataWithNulls = [1.2, null, 2.3, null, 1.8, 2.1]
    def kde = new KernelDensity(dataWithNulls)

    assertEquals(4, kde.dataCount, "Should filter out nulls")
    assertTrue(kde.bandwidth > 0, "Should compute valid bandwidth with filtered data")
  }

  @Test
  void testMinimumDataRequirement() {
    // Should throw with less than 2 points
    assertThrows(IllegalArgumentException) {
      new KernelDensity([1.0])
    }

    assertThrows(IllegalArgumentException) {
      new KernelDensity([])
    }

    // Should work with exactly 2 points
    def kde = new KernelDensity([1.0, 2.0])
    assertTrue(kde.bandwidth > 0)
  }

  @Test
  void testIdenticalValues() {
    // All same values - bandwidth computation should handle this
    def kde = new KernelDensity([2.0, 2.0, 2.0, 2.0, 2.0])

    assertTrue(kde.bandwidth > 0, "Bandwidth should be positive even with identical values")
    assertTrue(kde.density(2.0) > 0, "Density at the repeated value should be positive")
  }

  @Test
  void testSummary() {
    def kde = new KernelDensity(TEST_DATA)
    String summary = kde.summary()

    assertTrue(summary.contains("Kernel"), "Summary should contain kernel info")
    assertTrue(summary.contains("Bandwidth"), "Summary should contain bandwidth")
    assertTrue(summary.contains("Data points"), "Summary should contain data count")
  }

  @Test
  void testToString() {
    def kde = new KernelDensity(TEST_DATA, [kernel: Kernel.EPANECHNIKOV])
    String str = kde.toString()

    assertTrue(str.contains("EPANECHNIKOV"), "toString should contain kernel name")
    assertTrue(str.contains("bandwidth"), "toString should contain bandwidth")
  }

  @Test
  void testGettersReturnCopies() {
    def kde = new KernelDensity(TEST_DATA)

    // Modifying returned arrays shouldn't affect internal state
    double[] xCopy = kde.getX()
    double originalFirst = xCopy[0]
    xCopy[0] = 999.0

    assertEquals(originalFirst, kde.getX()[0], TOLERANCE,
        "Modifying returned array should not affect internal state")
  }

  @Test
  void testBandwidthPrecision() {
    def kde = new KernelDensity(TEST_DATA)

    BigDecimal bw4 = kde.getBandwidth(4)
    assertEquals(4, bw4.scale(), "Bandwidth should have 4 decimal places")

    BigDecimal bw2 = kde.getBandwidth(2)
    assertEquals(2, bw2.scale(), "Bandwidth should have 2 decimal places")
  }

  @Test
  void testKernelAsString() {
    // Test passing kernel as string
    def kde = new KernelDensity(TEST_DATA, [kernel: 'triangular'])
    assertEquals(Kernel.TRIANGULAR, kde.kernel)
  }
}
