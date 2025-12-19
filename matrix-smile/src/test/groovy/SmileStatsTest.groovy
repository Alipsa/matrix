import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.smile.stats.SmileStats
import smile.stat.distribution.GaussianDistribution
import smile.stat.distribution.PoissonDistribution
import smile.stat.distribution.BinomialDistribution
import smile.stat.distribution.ExponentialDistribution
import smile.stat.hypothesis.TTest
import smile.stat.hypothesis.FTest
import smile.stat.hypothesis.KSTest
import smile.stat.hypothesis.CorTest

import static org.junit.jupiter.api.Assertions.*

class SmileStatsTest {

  // ==================== Probability Distribution Tests ====================

  @Test
  void testNormalDistribution() {
    def dist = SmileStats.normal(0.0, 1.0)

    assertNotNull(dist)
    assertEquals(0.0, dist.mean(), 0.0001)
    assertEquals(1.0, dist.sd(), 0.0001)

    // Test PDF at mean
    double pdf = dist.p(0.0)
    assertTrue(pdf > 0.3 && pdf < 0.4) // ~0.3989
  }

  @Test
  void testNormalFit() {
    double[] data = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0] as double[]

    def dist = SmileStats.normalFit(data)

    assertNotNull(dist)
    assertEquals(5.5, dist.mean(), 0.0001) // Mean of 1..10
  }

  @Test
  void testNormalFitFromMatrix() {
    Matrix matrix = Matrix.builder()
        .data(values: [1.0, 2.0, 3.0, 4.0, 5.0])
        .types([Double])
        .build()

    def dist = SmileStats.normalFit(matrix, 'values')

    assertNotNull(dist)
    assertEquals(3.0, dist.mean(), 0.0001)
  }

  @Test
  void testPoissonDistribution() {
    def dist = SmileStats.poisson(5.0)

    assertNotNull(dist)
    assertEquals(5.0, dist.mean(), 0.0001)
  }

  @Test
  void testBinomialDistribution() {
    def dist = SmileStats.binomial(10, 0.5)

    assertNotNull(dist)
    assertEquals(5.0, dist.mean(), 0.0001)
    assertEquals(10 * 0.5 * 0.5, dist.variance(), 0.0001)
  }

  @Test
  void testExponentialDistribution() {
    def dist = SmileStats.exponential(2.0)

    assertNotNull(dist)
    assertEquals(0.5, dist.mean(), 0.0001) // Mean = 1/lambda
  }

  @Test
  void testGammaDistribution() {
    def dist = SmileStats.gamma(2.0, 3.0)

    assertNotNull(dist)
    assertEquals(6.0, dist.mean(), 0.0001) // Mean = shape * scale
  }

  @Test
  void testBetaDistribution() {
    def dist = SmileStats.beta(2.0, 5.0)

    assertNotNull(dist)
    // Mean = alpha / (alpha + beta) = 2/7
    assertEquals(2.0 / 7.0, dist.mean(), 0.0001)
  }

  @Test
  void testChiSquareDistribution() {
    def dist = SmileStats.chiSquare(5)

    assertNotNull(dist)
    assertEquals(5.0, dist.mean(), 0.0001) // Mean = df
  }

  @Test
  void testStudentTDistribution() {
    def dist = SmileStats.studentT(10)

    assertNotNull(dist)
    assertEquals(0.0, dist.mean(), 0.0001) // Mean = 0 for df > 1
  }

  @Test
  void testFDistribution() {
    def dist = SmileStats.fDistribution(5, 10)

    assertNotNull(dist)
    // Mean = d2 / (d2 - 2) for d2 > 2
    assertEquals(10.0 / 8.0, dist.mean(), 0.0001)
  }

  @Test
  void testUniformRandom() {
    double val = SmileStats.uniformRandom(0.0, 10.0)

    assertTrue(val >= 0.0 && val <= 10.0)
  }

  @Test
  void testLogNormalDistribution() {
    def dist = SmileStats.logNormal(0.0, 1.0)

    assertNotNull(dist)
    // Mean = exp(mu + sigma^2/2)
    assertEquals(Math.exp(0.5), dist.mean(), 0.0001)
  }

  @Test
  void testWeibullDistribution() {
    def dist = SmileStats.weibull(2.0, 1.0)

    assertNotNull(dist)
    assertTrue(dist.mean() > 0)
  }

  @Test
  void testBernoulliDistribution() {
    def dist = SmileStats.bernoulli(0.7)

    assertNotNull(dist)
    assertEquals(0.7, dist.mean(), 0.0001)
  }

  @Test
  void testGeometricDistribution() {
    def dist = SmileStats.geometric(0.5)

    assertNotNull(dist)
    // Mean = (1-p)/p = 0.5/0.5 = 1
    assertEquals(1.0, dist.mean(), 0.0001)
  }

  // ==================== Hypothesis Test Tests ====================

  @Test
  void testTTestOneSample() {
    double[] data = [10.2, 9.8, 10.1, 9.9, 10.0, 10.3, 9.7, 10.1, 10.0, 9.9] as double[]

    TTest result = SmileStats.tTestOneSample(data, 10.0)

    assertNotNull(result)
    assertTrue(result.pvalue() > 0.05) // Should not reject H0: mean = 10
  }

  @Test
  void testTTestOneSampleReject() {
    double[] data = [12.0, 11.5, 12.2, 11.8, 12.1, 11.9, 12.0, 11.7, 12.3, 11.6] as double[]

    TTest result = SmileStats.tTestOneSample(data, 10.0)

    assertNotNull(result)
    assertTrue(result.pvalue() < 0.05) // Should reject H0: mean = 10
  }

  @Test
  void testTTestOneSampleFromMatrix() {
    Matrix matrix = Matrix.builder()
        .data(values: [10.2, 9.8, 10.1, 9.9, 10.0, 10.3, 9.7, 10.1, 10.0, 9.9])
        .types([Double])
        .build()

    TTest result = SmileStats.tTestOneSample(matrix, 'values', 10.0)

    assertNotNull(result)
    assertTrue(result.pvalue() > 0.05)
  }

  @Test
  void testTTestTwoSample() {
    double[] group1 = [10.1, 10.2, 9.9, 10.0, 10.3] as double[]
    double[] group2 = [9.8, 9.7, 10.0, 9.9, 9.6] as double[]

    TTest result = SmileStats.tTestTwoSample(group1, group2)

    assertNotNull(result)
    assertNotNull(result.pvalue())
  }

  @Test
  void testTTestTwoSampleFromMatrix() {
    Matrix matrix = Matrix.builder()
        .data(
            group1: [10.1, 10.2, 9.9, 10.0, 10.3],
            group2: [9.8, 9.7, 10.0, 9.9, 9.6]
        )
        .types([Double, Double])
        .build()

    TTest result = SmileStats.tTestTwoSample(matrix, 'group1', 'group2')

    assertNotNull(result)
  }

  @Test
  void testTTestPaired() {
    double[] before = [85.0, 90.0, 78.0, 92.0, 88.0] as double[]
    double[] after = [88.0, 92.0, 82.0, 95.0, 90.0] as double[]

    TTest result = SmileStats.tTestPaired(before, after)

    assertNotNull(result)
    assertNotNull(result.pvalue())
  }

  @Test
  void testFTest() {
    double[] group1 = [10.1, 10.2, 9.9, 10.0, 10.3, 9.8, 10.1] as double[]
    double[] group2 = [15.1, 8.2, 12.9, 7.0, 14.3, 9.8, 11.1] as double[]

    FTest result = SmileStats.fTest(group1, group2)

    assertNotNull(result)
    assertNotNull(result.pvalue())
  }

  @Test
  void testFTestFromMatrix() {
    Matrix matrix = Matrix.builder()
        .data(
            narrow: [10.1, 10.2, 9.9, 10.0, 10.3, 9.8, 10.1],
            wide: [15.1, 8.2, 12.9, 7.0, 14.3, 9.8, 11.1]
        )
        .types([Double, Double])
        .build()

    FTest result = SmileStats.fTest(matrix, 'narrow', 'wide')

    assertNotNull(result)
    // Wide variance group should have significantly different variance
  }

  @Test
  void testKSTestNormality() {
    // Generate normal-ish data
    double[] data = [
        -1.2, -0.8, -0.5, -0.2, 0.0, 0.2, 0.5, 0.8, 1.2, 1.5,
        -1.0, -0.6, -0.3, 0.1, 0.3, 0.6, 1.0, 1.3, -0.4, 0.4
    ] as double[]

    KSTest result = SmileStats.ksTestNormality(data)

    assertNotNull(result)
    assertNotNull(result.pvalue)
  }

  @Test
  void testKSTestTwoSample() {
    double[] sample1 = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0] as double[]
    double[] sample2 = [2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0] as double[]

    KSTest result = SmileStats.ksTestTwoSample(sample1, sample2)

    assertNotNull(result)
    assertNotNull(result.pvalue)
  }

  // ==================== Correlation Tests ====================

  @Test
  void testCorrelationTestPearson() {
    double[] x = [1.0, 2.0, 3.0, 4.0, 5.0] as double[]
    double[] y = [2.0, 4.0, 6.0, 8.0, 10.0] as double[]

    CorTest result = SmileStats.correlationTest(x, y)

    assertNotNull(result)
    assertEquals(1.0, result.cor(), 0.0001) // Perfect positive correlation
    assertTrue(result.pvalue() < 0.05)
  }

  @Test
  void testCorrelationTestNegative() {
    double[] x = [1.0, 2.0, 3.0, 4.0, 5.0] as double[]
    double[] y = [10.0, 8.0, 6.0, 4.0, 2.0] as double[]

    CorTest result = SmileStats.correlationTest(x, y)

    assertNotNull(result)
    assertEquals(-1.0, result.cor(), 0.0001) // Perfect negative correlation
  }

  @Test
  void testCorrelationTestFromMatrix() {
    Matrix matrix = Matrix.builder()
        .data(
            x: [1.0, 2.0, 3.0, 4.0, 5.0],
            y: [2.1, 4.2, 5.8, 8.1, 10.2]
        )
        .types([Double, Double])
        .build()

    CorTest result = SmileStats.correlationTest(matrix, 'x', 'y')

    assertNotNull(result)
    assertTrue(result.cor() > 0.99) // Strong positive correlation
  }

  @Test
  void testSpearmanTest() {
    double[] x = [1.0, 2.0, 3.0, 4.0, 5.0] as double[]
    double[] y = [1.5, 3.0, 4.5, 6.0, 7.5] as double[]

    CorTest result = SmileStats.spearmanTest(x, y)

    assertNotNull(result)
    assertEquals(1.0, result.cor(), 0.0001) // Perfect rank correlation
  }

  @Test
  void testKendallTest() {
    double[] x = [1.0, 2.0, 3.0, 4.0, 5.0] as double[]
    double[] y = [1.0, 2.0, 3.0, 4.0, 5.0] as double[]

    CorTest result = SmileStats.kendallTest(x, y)

    assertNotNull(result)
    assertEquals(1.0, result.cor(), 0.0001) // Perfect concordance
  }

  // ==================== Correlation Matrix Tests ====================

  @Test
  void testCorrelationMatrix() {
    Matrix matrix = Matrix.builder()
        .data(
            a: [1.0, 2.0, 3.0, 4.0, 5.0],
            b: [2.0, 4.0, 6.0, 8.0, 10.0],
            c: [5.0, 4.0, 3.0, 2.0, 1.0]
        )
        .types([Double, Double, Double])
        .build()

    Matrix corMatrix = SmileStats.correlationMatrix(matrix)

    assertNotNull(corMatrix)
    assertEquals(3, corMatrix.rowCount())
    assertEquals(4, corMatrix.columnCount()) // includes row name column

    // Check diagonal (self-correlation = 1)
    assertEquals(1.0, corMatrix[0, 'a'] as double, 0.0001)
    assertEquals(1.0, corMatrix[1, 'b'] as double, 0.0001)
    assertEquals(1.0, corMatrix[2, 'c'] as double, 0.0001)

    // a and b should be perfectly correlated
    assertEquals(1.0, corMatrix[0, 'b'] as double, 0.0001)

    // a and c should be negatively correlated
    assertEquals(-1.0, corMatrix[0, 'c'] as double, 0.0001)
  }

  @Test
  void testCorrelationMatrixSpecificColumns() {
    Matrix matrix = Matrix.builder()
        .data(
            a: [1.0, 2.0, 3.0, 4.0, 5.0],
            b: [2.0, 4.0, 6.0, 8.0, 10.0],
            c: [5.0, 4.0, 3.0, 2.0, 1.0],
            d: [10.0, 20.0, 30.0, 40.0, 50.0]
        )
        .types([Double, Double, Double, Double])
        .build()

    Matrix corMatrix = SmileStats.correlationMatrix(matrix, ['a', 'b'])

    assertNotNull(corMatrix)
    assertEquals(2, corMatrix.rowCount())
  }

  @Test
  void testCorrelationMatrixSpearman() {
    Matrix matrix = Matrix.builder()
        .data(
            x: [1.0, 2.0, 3.0, 4.0, 5.0],
            y: [2.0, 4.0, 6.0, 8.0, 10.0]
        )
        .types([Double, Double])
        .build()

    Matrix corMatrix = SmileStats.correlationMatrix(matrix, null, 'spearman')

    assertNotNull(corMatrix)
    assertEquals(1.0, corMatrix[0, 'y'] as double, 0.0001)
  }

  @Test
  void testPValueMatrix() {
    Matrix matrix = Matrix.builder()
        .data(
            a: [1.0, 2.0, 3.0, 4.0, 5.0],
            b: [2.0, 4.0, 6.0, 8.0, 10.0],
            c: [1.5, 3.2, 2.8, 4.1, 5.5]
        )
        .types([Double, Double, Double])
        .build()

    Matrix pMatrix = SmileStats.pValueMatrix(matrix)

    assertNotNull(pMatrix)
    assertEquals(3, pMatrix.rowCount())

    // Diagonal should be 0 (perfect self-correlation)
    assertEquals(0.0, pMatrix[0, 'a'] as double, 0.0001)

    // Perfect correlation between a and b should have very low p-value
    assertTrue((pMatrix[0, 'b'] as double) < 0.01)
  }

  @Test
  void testCorrelationWithSignificance() {
    Matrix matrix = Matrix.builder()
        .data(
            x: [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0],
            y: [2.0, 4.1, 5.8, 8.2, 9.9, 11.8, 14.2, 16.1],
            z: [8.0, 7.0, 6.0, 5.0, 4.0, 3.0, 2.0, 1.0]
        )
        .types([Double, Double, Double])
        .build()

    Map<String, Matrix> result = SmileStats.correlationWithSignificance(matrix)

    assertNotNull(result)
    assertTrue(result.containsKey('correlation'))
    assertTrue(result.containsKey('pvalue'))

    Matrix corMatrix = result['correlation']
    Matrix pMatrix = result['pvalue']

    assertEquals(3, corMatrix.rowCount())
    assertEquals(3, pMatrix.rowCount())

    // x and y should be strongly positively correlated
    assertTrue((corMatrix[0, 'y'] as double) > 0.99)

    // x and z should be negatively correlated
    assertEquals(-1.0, corMatrix[0, 'z'] as double, 0.0001)
  }

  // ==================== Random Sample Generation Tests ====================

  @Test
  void testRandomNormal() {
    double[] samples = SmileStats.randomNormal(1000, 10.0, 2.0)

    assertEquals(1000, samples.length)

    // Check approximate mean and std
    double mean = samples.sum() / samples.length
    assertTrue(mean > 9.0 && mean < 11.0) // Should be close to 10
  }

  @Test
  void testRandomNormalDefault() {
    double[] samples = SmileStats.randomNormal(500)

    assertEquals(500, samples.length)

    double mean = samples.sum() / samples.length
    assertTrue(mean > -0.5 && mean < 0.5) // Should be close to 0
  }

  @Test
  void testRandomPoisson() {
    int[] samples = SmileStats.randomPoisson(1000, 5.0)

    assertEquals(1000, samples.length)

    double mean = samples.sum() / (double) samples.length
    assertTrue(mean > 4.0 && mean < 6.0) // Should be close to lambda=5
  }

  @Test
  void testRandomBinomial() {
    int[] samples = SmileStats.randomBinomial(1000, 10, 0.5)

    assertEquals(1000, samples.length)

    double mean = samples.sum() / (double) samples.length
    assertTrue(mean > 4.0 && mean < 6.0) // Should be close to n*p=5
  }

  @Test
  void testRandomUniform() {
    double[] samples = SmileStats.randomUniform(1000, 0.0, 10.0)

    assertEquals(1000, samples.length)

    // All samples should be in [0, 10]
    for (double s : samples) {
      assertTrue(s >= 0.0 && s <= 10.0)
    }

    double mean = samples.sum() / samples.length
    assertTrue(mean > 4.0 && mean < 6.0) // Should be close to 5
  }

  @Test
  void testRandomExponential() {
    double[] samples = SmileStats.randomExponential(1000, 2.0)

    assertEquals(1000, samples.length)

    // All samples should be positive
    for (double s : samples) {
      assertTrue(s >= 0.0)
    }

    double mean = samples.sum() / samples.length
    assertTrue(mean > 0.3 && mean < 0.7) // Should be close to 1/lambda = 0.5
  }

  // ==================== Chi-Square Tests ====================

  @Test
  void testChiSquareGoodnessOfFit() {
    // Example: testing if a die is fair
    // The expected array should contain probabilities (summing to 1)
    int[] observed = [18, 20, 17, 22, 21, 22] as int[] // observed counts
    double prob = 1.0 / 6.0  // fair die probability
    double[] expected = [prob, prob, prob, prob, prob, prob] as double[] // probabilities sum to 1

    def result = SmileStats.chiSquareTest(observed, expected)

    assertNotNull(result)
    assertNotNull(result.pvalue)
    // Should not reject H0 (die is fair) at 0.05 level
    assertTrue(result.pvalue > 0.05)
  }

  @Test
  void testChiSquareIndependence() {
    // 2x2 contingency table
    int[][] table = [
        [10, 20] as int[],
        [30, 40] as int[]
    ] as int[][]

    def result = SmileStats.chiSquareTestIndependence(table)

    assertNotNull(result)
    assertNotNull(result.pvalue)
  }

  // ==================== Edge Cases ====================

  @Test
  void testNullHandlingInToDoubleArray() {
    Matrix matrix = Matrix.builder()
        .data(values: [1.0, null, 3.0, null, 5.0])
        .types([Double])
        .build()

    // Should not throw, nulls should be filtered
    def dist = SmileStats.normalFit(matrix, 'values')

    assertNotNull(dist)
    assertEquals(3.0, dist.mean(), 0.0001) // Mean of [1, 3, 5]
  }
}
