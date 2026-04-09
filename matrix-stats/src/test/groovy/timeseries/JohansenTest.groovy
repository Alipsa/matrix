package timeseries

import static org.junit.jupiter.api.Assertions.*

import org.apache.commons.math3.linear.DecompositionSolver
import org.apache.commons.math3.linear.EigenDecomposition
import org.apache.commons.math3.linear.LUDecomposition
import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.RealMatrix
import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.timeseries.Johansen

/**
 * Tests for Johansen cointegration test.
 */
class JohansenTest {

  @Test
  void testCointegratedSeries() {
    // Create two cointegrated series
    // y1 is a random walk, y2 = y1 + noise (should be cointegrated)
    Random rnd = new Random(42)
    int n = 100
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian() * 0.5
      y2[i] = y1[i] + rnd.nextGaussian() * 0.1  // Cointegrated with y1
    }

    def result = Johansen.test([y1, y2], 1)

    assertNotNull(result)
    assertEquals(2, result.numVariables)
    assertEquals(1, result.lags)
    assertEquals('const', result.type)

    // Should have 2 eigenvalues
    assertEquals(2, result.eigenvalues.size())
    assertEquals(2, result.traceStatistics.size())

    // Eigenvalues should be between 0 and 1
    for (BigDecimal eigen : result.eigenvalues) {
      assertTrue(eigen >= 0 && eigen <= 1, "Eigenvalue should be in [0,1]: ${eigen}")
    }

    // Trace statistics should be positive
    for (BigDecimal trace : result.traceStatistics) {
      assertTrue(trace >= 0, "Trace statistic should be non-negative: ${trace}")
    }
  }

  @Test
  void testIndependentSeries() {
    // Create two independent random walks (not cointegrated)
    Random rnd = new Random(123)
    int n = 100
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian()
      y2[i] = y2[i-1] + rnd.nextGaussian()  // Independent random walk
    }

    def result = Johansen.test([y1, y2], 1)

    assertNotNull(result)
    assertEquals(2, result.numVariables)

    // Should detect no cointegration (typically)
    assertNotNull(result.interpret())
  }

  @Test
  void testThreeVariables() {
    // Test with three variables
    Random rnd = new Random(456)
    int n = 80
    double[] y1 = new double[n]
    double[] y2 = new double[n]
    double[] y3 = new double[n]

    y1[0] = 0
    y2[0] = 0
    y3[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian() * 0.5
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
      y3[i] = y1[i] + y2[i] + rnd.nextGaussian() * 0.3
    }

    def result = Johansen.test([y1, y2, y3], 1)

    assertNotNull(result)
    assertEquals(3, result.numVariables)
    assertEquals(3, result.eigenvalues.size())
    assertEquals(3, result.traceStatistics.size())
  }

  @Test
  void testDifferentLags() {
    Random rnd = new Random(789)
    int n = 100
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian()
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
    }

    def result1 = Johansen.test([y1, y2], 1)
    def result2 = Johansen.test([y1, y2], 2)
    def result3 = Johansen.test([y1, y2], 3)

    assertEquals(1, result1.lags)
    assertEquals(2, result2.lags)
    assertEquals(3, result3.lags)

    // Different lags should give different sample sizes
    assertTrue(result1.sampleSize > result2.sampleSize)
    assertTrue(result2.sampleSize > result3.sampleSize)
  }

  @Test
  void testDifferentTypes() {
    Random rnd = new Random(111)
    int n = 100
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian()
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
    }

    def resultNone = Johansen.test([y1, y2], 1, 'none')
    def resultConst = Johansen.test([y1, y2], 1, 'const')
    def resultTrend = Johansen.test([y1, y2], 1, 'trend')

    assertEquals('none', resultNone.type)
    assertEquals('const', resultConst.type)
    assertEquals('trend', resultTrend.type)
  }

  @Test
  void testValidation() {
    // Null data
    assertThrows(IllegalArgumentException) {
      Johansen.test(null, 1)
    }

    // Empty list
    assertThrows(IllegalArgumentException) {
      Johansen.test([], 1)
    }

    // Invalid lags
    double[] y1 = new double[100]
    double[] y2 = new double[100]
    assertThrows(IllegalArgumentException) {
      Johansen.test([y1, y2], 0)
    }

    assertThrows(IllegalArgumentException) {
      Johansen.test([y1, y2], -1)
    }

    // Invalid type
    assertThrows(IllegalArgumentException) {
      Johansen.test([y1, y2], 1, 'invalid')
    }

    // Mismatched lengths
    double[] y3 = new double[50]
    assertThrows(IllegalArgumentException) {
      Johansen.test([y1, y3], 1)
    }

    // Too few observations
    double[] ySmall1 = new double[15]
    double[] ySmall2 = new double[15]
    assertThrows(IllegalArgumentException) {
      Johansen.test([ySmall1, ySmall2], 1)
    }
  }

  @Test
  void testResultInterpret() {
    Random rnd = new Random(222)
    int n = 100
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian()
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
    }

    def result = Johansen.test([y1, y2], 1)
    String interpretation = result.interpret()

    assertNotNull(interpretation)
    assertTrue(interpretation.contains('Johansen'))
    assertTrue(interpretation.contains('H0'))
    assertTrue(interpretation.contains('Conclusion'))
  }

  @Test
  void testResultToString() {
    Random rnd = new Random(333)
    int n = 100
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian()
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
    }

    def result = Johansen.test([y1, y2], 1)
    String str = result

    assertTrue(str.contains('Johansen'))
    assertTrue(str.contains('Number of variables'))
    assertTrue(str.contains('Sample size'))
    assertTrue(str.contains('Lags'))
    assertTrue(str.contains('Eigenvalues'))
  }

  @Test
  void testEigenvalueOrder() {
    Random rnd = new Random(444)
    int n = 100
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian()
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
    }

    def result = Johansen.test([y1, y2], 1)

    // Eigenvalues should be sorted in descending order
    for (int i = 0; i < result.eigenvalues.size() - 1; i++) {
      assertTrue(result.eigenvalues[i] >= result.eigenvalues[i+1],
                 "Eigenvalues should be in descending order")
    }
  }

  @Test
  void testTraceStatisticOrder() {
    Random rnd = new Random(555)
    int n = 100
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian()
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
    }

    def result = Johansen.test([y1, y2], 1)

    // Trace statistics should decrease as r increases
    for (int i = 0; i < result.traceStatistics.size() - 1; i++) {
      assertTrue(result.traceStatistics[i] >= result.traceStatistics[i+1],
                 "Trace statistics should be non-increasing")
    }
  }

  @Test
  void testCriticalValues() {
    Random rnd = new Random(666)
    int n = 100
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian()
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
    }

    def result = Johansen.test([y1, y2], 1)

    // Critical values should be available
    assertNotNull(result.criticalValues5pct)
    assertFalse(result.criticalValues5pct.isEmpty())
  }

  @Test
  void testFourVariables() {
    // Test with four variables
    Random rnd = new Random(777)
    int n = 80
    double[] y1 = new double[n]
    double[] y2 = new double[n]
    double[] y3 = new double[n]
    double[] y4 = new double[n]

    y1[0] = 0
    y2[0] = 0
    y3[0] = 0
    y4[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian() * 0.5
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
      y3[i] = y1[i] + y2[i] + rnd.nextGaussian() * 0.3
      y4[i] = y2[i] + rnd.nextGaussian() * 0.25
    }

    def result = Johansen.test([y1, y2, y3, y4], 1)

    assertNotNull(result)
    assertEquals(4, result.numVariables)
    assertEquals(4, result.eigenvalues.size())
    assertEquals(4, result.traceStatistics.size())
  }

  @Test
  void testStationarySeries() {
    // Stationary series should not be cointegrated (already stationary)
    Random rnd = new Random(888)
    int n = 100
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    for (int i = 0; i < n; i++) {
      y1[i] = rnd.nextGaussian()
      y2[i] = rnd.nextGaussian()
    }

    def result = Johansen.test([y1, y2], 1)

    assertNotNull(result)
    // Test completes without error
    assertNotNull(result.interpret())
  }

  @Test
  void testLongerLags() {
    Random rnd = new Random(999)
    int n = 150
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian()
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
    }

    def result = Johansen.test([y1, y2], 5)

    assertEquals(5, result.lags)
    assertNotNull(result.eigenvalues)
    assertTrue(result.sampleSize < n, "Effective sample size should be less than n")
  }

  @Test
  void testMinimumSampleSize() {
    Random rnd = new Random(1010)
    int n = 25  // Minimum: lags + 20 = 1 + 20 = 21
    double[] y1 = new double[n]
    double[] y2 = new double[n]

    y1[0] = 0
    y2[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i-1] + rnd.nextGaussian()
      y2[i] = y1[i] + rnd.nextGaussian() * 0.2
    }

    def result = Johansen.test([y1, y2], 1)

    assertNotNull(result)
    assertEquals(n - 1, result.sampleSize)
  }

  @Test
  void testMatchesApacheReferenceForConstModel() {
    List<double[]> data = sampleCointegratedData(2026, 120)

    def actual = Johansen.test(data, 2, 'const')
    Map<String, Object> expected = apacheJohansenReference(data, 2, 'const')

    assertArrayEquals(expected.eigenvalues as double[], actual.eigenvalues.collect { it as double } as double[], 1e-10)
    assertArrayEquals(expected.traceStatistics as double[], actual.traceStatistics.collect { it as double } as double[], 1e-9)
  }

  @Test
  void testMatchesApacheReferenceForTrendModel() {
    List<double[]> data = sampleCointegratedData(3030, 110)

    def actual = Johansen.test(data, 1, 'trend')
    Map<String, Object> expected = apacheJohansenReference(data, 1, 'trend')

    assertArrayEquals(expected.eigenvalues as double[], actual.eigenvalues.collect { it as double } as double[], 1e-10)
    assertArrayEquals(expected.traceStatistics as double[], actual.traceStatistics.collect { it as double } as double[], 1e-9)
  }

  @Test
  void testGroovyFacingInputsAndValueAccessors() {
    Random rnd = new Random(987)
    int n = 80
    List<BigDecimal> y1 = []
    List<BigDecimal> y2 = []

    BigDecimal current = 0
    for (int i = 0; i < n; i++) {
      current += rnd.nextGaussian() * 0.3d
      y1 << current
      y2 << current + rnd.nextGaussian() * 0.05d
    }

    def result = Johansen.test([y1, y2], 1)

    assertEquals(2, result.eigenvalueValues.size())
    assertEquals(2, result.traceStatisticValues.size())
    assertFalse(result.criticalValueRows5pct.isEmpty())
    assertFalse(result.criticalValueRows5pct[0].isEmpty())
  }

  private static List<double[]> sampleCointegratedData(int seed, int n) {
    Random rnd = new Random(seed)
    double[] y1 = new double[n]
    double[] y2 = new double[n]
    double[] y3 = new double[n]

    y1[0] = 0
    y2[0] = 0
    y3[0] = 0

    for (int i = 1; i < n; i++) {
      y1[i] = y1[i - 1] + rnd.nextGaussian() * 0.6
      y2[i] = y1[i] + rnd.nextGaussian() * 0.15
      y3[i] = 0.5 * y1[i] - 0.25 * y2[i] + rnd.nextGaussian() * 0.2
    }

    [y1, y2, y3]
  }

  private static Map<String, Object> apacheJohansenReference(List<double[]> data, int lags, String type) {
    int k = data.size()
    int n = data[0].length
    int effectiveN = n - lags
    double[][] deltaY = new double[effectiveN][k]
    double[][] laggedY = new double[effectiveN][k]

    for (int t = 0; t < effectiveN; t++) {
      int actualT = t + lags
      for (int i = 0; i < k; i++) {
        deltaY[t][i] = data[i][actualT] - data[i][actualT - 1]
        laggedY[t][i] = data[i][actualT - 1]
      }
    }

    int numRegressors = (lags > 1 ? k * (lags - 1) : 0) + (type == 'const' ? 1 : 0) + (type == 'trend' ? 1 : 0)
    RealMatrix r0
    RealMatrix r1

    if (numRegressors == 0) {
      r0 = MatrixUtils.createRealMatrix(deltaY)
      r1 = MatrixUtils.createRealMatrix(laggedY)
    } else {
      double[][] z = new double[effectiveN][numRegressors]
      for (int t = 0; t < effectiveN; t++) {
        int actualT = t + lags
        int col = 0
        if (lags > 1) {
          for (int lag = 1; lag < lags; lag++) {
            for (int i = 0; i < k; i++) {
              z[t][col++] = data[i][actualT - lag] - data[i][actualT - lag - 1]
            }
          }
        }
        if (type == 'const') {
          z[t][col++] = 1.0d
        }
        if (type == 'trend') {
          z[t][col++] = t + 1.0d
        }
      }

      RealMatrix zMatrix = MatrixUtils.createRealMatrix(z)
      RealMatrix deltaYMatrix = MatrixUtils.createRealMatrix(deltaY)
      RealMatrix laggedYMatrix = MatrixUtils.createRealMatrix(laggedY)
      RealMatrix ztZ = zMatrix.transpose() * zMatrix
      DecompositionSolver solver = new LUDecomposition(ztZ).solver
      RealMatrix projection = (zMatrix * solver.inverse) * zMatrix.transpose()

      r0 = deltaYMatrix.subtract(projection * deltaYMatrix)
      r1 = laggedYMatrix.subtract(projection * laggedYMatrix)
    }

    double scale = 1.0d / effectiveN
    RealMatrix s00 = (r0.transpose() * r0).scalarMultiply(scale)
    RealMatrix s11 = (r1.transpose() * r1).scalarMultiply(scale)
    RealMatrix s01 = (r0.transpose() * r1).scalarMultiply(scale)

    RealMatrix s00Inv = new LUDecomposition(s00).solver.inverse
    RealMatrix s11Inv = new LUDecomposition(s11).solver.inverse
    RealMatrix product = ((s11Inv * s01.transpose()) * s00Inv) * s01

    double[] eigenvalues = sortDescending(new EigenDecomposition(product).realEigenvalues)
    double[] traceStatistics = new double[k]
    for (int r = 0; r < k; r++) {
      double sum = 0.0d
      for (int i = r; i < k; i++) {
        sum += Math.log(1.0d - eigenvalues[i])
      }
      traceStatistics[r] = -effectiveN * sum
    }

    [eigenvalues: eigenvalues, traceStatistics: traceStatistics]
  }

  private static double[] sortDescending(double[] values) {
    Arrays.sort(values)
    double[] sorted = new double[values.length]
    for (int i = 0; i < values.length; i++) {
      sorted[i] = values[values.length - 1 - i]
    }
    sorted
  }
}
