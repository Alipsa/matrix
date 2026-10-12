import static org.junit.jupiter.api.Assertions.*

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.stats.Anova
import se.alipsa.matrix.stats.Correlation
import se.alipsa.matrix.stats.Ellipse
import se.alipsa.matrix.stats.Normalize
import se.alipsa.matrix.stats.cluster.GroupEstimator
import se.alipsa.matrix.stats.cluster.KMeansPlusPlus
import se.alipsa.matrix.stats.contingency.Barnard
import se.alipsa.matrix.stats.contingency.Boschloo
import se.alipsa.matrix.stats.contingency.Fisher
import se.alipsa.matrix.stats.kde.BandwidthSelector
import se.alipsa.matrix.stats.kde.Kernel
import se.alipsa.matrix.stats.kde.KernelDensity
import se.alipsa.matrix.stats.normality.KolmogorovSmirnov
import se.alipsa.matrix.stats.regression.PolynomialRegression
import se.alipsa.matrix.stats.regression.RegressionUtils

class RemainingFixesReferenceTest {

  @Test
  @CompileStatic
  void integerNormalisationPreservesFractionsAndPrecision() {
    Number result = Normalize.minMaxNorm(5, 0, 10)
    assertTrue(result instanceof BigDecimal)
    assertEquals(0.5G, result)
    long offset = 1L << 40
    assertEquals(0.25G, Normalize.minMaxNorm(offset + 1, offset, offset + 4))
    assertTrue(Normalize.minMaxNorm(5d, 0, 10) instanceof Double)
    assertTrue(Normalize.minMaxNorm(5G, 0, 10) instanceof BigDecimal)
    assertEquals([null], Normalize.stdScaleNorm([5]))
    assertEquals([null], Normalize.stdScaleNorm([5] as Integer[]))
    Matrix data = Matrix.builder().columns([x: [0, 1, 2]]).types(Integer).build()
    assertEquals(BigDecimal, Normalize.minMaxNorm(data).type('x'))
  }

  @Test
  void fisherConditionalRootsMatchR() {
    // R 4.6.1 fisherPrecise() in contingency.R solves fisher.test's equations with tol=1e-14.
    [[3, 1, 1, 3, 6.4083196582d, 0.211735595447d, 626.243530589d, 0.313573767505d, 306.236807859d, 0.985714285714d, 0.242857142857d],
     [200, 10, 10, 200, 381.440510681d, 151.609800418d, 1090.73376272d, 171.531182238d, 925.755514237d, 1d, 1.3010157698476808e-92d],
     [2, 7, 8, 2, 0.0858523999512d, 0.0046735742421d, 0.895773642488d, 0.00714636636381d, 0.678527080793d, 0.0185217259521d, 0.9990149169715733d]].each { row ->
      def table = [[row[0], row[1]], [row[2], row[3]]]
      def fit = Fisher.test(table)
      [fit.oddsRatio, *fit.confidenceInterval].eachWithIndex { Number actual, int i ->
        assertEquals(row[i + 4] as double, actual.doubleValue(), Math.abs(row[i + 4] as double) * 1e-8)
      }
      def less = Fisher.test(table, 'less')
      def greater = Fisher.test(table, 'greater')
      def ninety = Fisher.test(table, 'two.sided', 0.90)
      assertEquals(row[7] as double, ninety.confidenceInterval[0] as double, (row[7] as double) * 1e-8)
      assertEquals(row[8] as double, ninety.confidenceInterval[1] as double, (row[8] as double) * 1e-8)
      assertEquals(row[9] as double, less.pValue as double, (row[9] as double) * 1e-6)
      assertEquals(row[10] as double, greater.pValue as double, (row[10] as double) * 1e-6)
      assertEquals(ninety.confidenceInterval[1] as double, less.confidenceInterval[1] as double, 1e-9)
      assertEquals(ninety.confidenceInterval[0] as double, greater.confidenceInterval[0] as double, 1e-9)
      assertEquals(Double.POSITIVE_INFINITY, greater.confidenceInterval[1])
    }
    def boundary = Fisher.test([[10, 0], [3, 12]])
    assertEquals(Double.POSITIVE_INFINITY, boundary.oddsRatio)
    assertEquals(4.76506246914d, boundary.confidenceInterval[0] as double, 5e-8)
    assertEquals(6.41658159036d, Fisher.test([[10, 0], [3, 12]], 'two.sided', 0.90).confidenceInterval[0] as double, 7e-8)
    assertEquals(8.74949522143e-5d, Fisher.test([[10, 0], [3, 12]], 'greater').pValue as double, 1e-10)
    assertEquals(1d, Fisher.test([[10, 0], [3, 12]], 'less').pValue as double, 1e-12)
    assertThrows(IllegalArgumentException) { Fisher.test([[1.5, 2], [3, 4]]) }
    Matrix invalid = Matrix.builder().columns([a: [1.5, 3], b: [2, 4]]).build()
    assertThrows(IllegalArgumentException) { Fisher.test(invalid) }
    assertThrows(IllegalArgumentException) { Fisher.test([[1, 2], [3, 4]], 'less', 1) }
  }

  @Test
  void unconditionalTestsMatchExact() {
    // Exact 3.3 exact.test(matrix(t,2,byrow=TRUE), model='Binomial',cond.row=TRUE).
    [[7, 12, 8, 3, 0.067514d, 0.085726d], [10, 2, 3, 9, 0.006611d, 0.006611d],
     [5, 5, 9, 1, 0.062310d, 0.062310d]].each { row ->
      int[][] table = [[row[0], row[1]], [row[2], row[3]]] as int[][]
      assertEquals(row[4] as double, Barnard.test(table).pValue as double, 1e-6)
      assertEquals(row[5] as double, Boschloo.test(table).pValue as double, 1e-6)
    }
  }

  @Test
  void densityMatchesRUnitVarianceKernels() {
    def iris = Dataset.iris()['Sepal Length']
    // R 4.6.1 bw.nrd(iris$Sepal.Length), bw.nrd0(iris$Sepal.Length).
    assertEquals(0.322220d, BandwidthSelector.scott(iris) as double, 1e-6)
    assertEquals(0.273583d, BandwidthSelector.silverman(iris) as double, 1e-6)
    def data = [1.2, 2.3, 2.9, 3.1, 3.8, 4.4, 5.0, 5.2, 6.7, 7.1]
    // R exact sums (kde.R), avoiding density() FFT/grid interpolation error.
    [0.1827428d, 0.1945641d, 0.2165064d, 0.1879464d].eachWithIndex { double expected, int i ->
      assertEquals(expected, new KernelDensity(data, [kernel: Kernel.values()[i], bandwidth: 0.8]).density(4) as double, 1e-7)
    }
    assertThrows(IllegalArgumentException) { new KernelDensity(data, [n: 1]) }
    assertThrows(IllegalArgumentException) { new KernelDensity(data, [bandwidth: 0]) }
    assertThrows(IllegalArgumentException) { new KernelDensity(data, [adjust: -1]) }
    Kernel.values().each { Kernel kernel ->
      // Midpoint quadrature on the precise compact support (Gaussian truncated at +/-10).
      double width = [10d, Math.sqrt(5d), Math.sqrt(3d), Math.sqrt(6d)][kernel.ordinal()]
      int count = 20000
      double step = 2 * width / count
      double integral = 0
      double variance = 0
      (0..<count).each { int j ->
        double x = -width + (j + 0.5d) * step
        double weight = kernel.evaluateValue(x) * step
        integral += weight
        variance += x * x * weight
      }
      assertEquals(1d, integral, 1e-6)
      assertEquals(1d, variance, 1e-6)
    }
  }

  @Test
  void clusteringMatchesRIrisOptimumAndUsesMediansForL1() {
    double[][] points = Dataset.iris().rows().collect { row -> row[0..3]*.doubleValue() } as double[][]
    def fit = new KMeansPlusPlus.Builder(3, points).randomSeed(42).iterations(50).build()
    // R 4.6.1 kmeans(iris[,1:4],3,nstart=50): tot.withinss, sizes.
    assertEquals(78.8514414261d, fit.WCSS, 8e-5)
    assertEquals([38, 50, 62], fit.assignment.groupBy { it.clusterId }.values()*.size().sort())
    def median = new KMeansPlusPlus.Builder(1, [[0], [1], [100]]).useL1norm(true).build()
    assertEquals(1d, median.centroids[0][0], 1e-12)
    assertEquals(100d, median.WCSS, 1e-12)
    assertEquals(2, GroupEstimator.estimateKByElbow([[0], [1]]))
    assertThrows(IllegalArgumentException) { new KMeansPlusPlus.Builder(1, [[0]]).maxIterationsPerRun(0) }
  }

  @Test
  void ellipseMatchesGgplotGeometry() {
    def xs = [-1G, 1G, -1G, 1G]
    def ys = [-10G, -10G, 10G, 10G]
    // ggplot2 4.0.3 calculate_ellipse; MASS 7.3.65 cov.trob.
    ['norm': 5.047004259d, 't': 4.370833901d].each { String type, double extent ->
      def ellipse = Ellipse.calculate(xs, ys, 0.95, type, 101)
      assertEquals(extent, ellipse.x.max() as double, 1e-6)
      assertEquals(extent * 10, ellipse.y.max() as double, 1e-5)
    }
    def mtcars = Dataset.mtcars()
    def ellipse = Ellipse.calculate(mtcars['wt'], mtcars['mpg'], 0.95, 't', 101)
    double cx = 3.190695691d
    double cy = 19.704170660d
    double vx = 0.6121549684d
    double vy = 24.233577869d
    double cov = -3.327567388d
    ellipse.x.eachWithIndex { BigDecimal x, int i ->
      double dx = x.doubleValue() - cx
      double dy = ellipse.y[i].doubleValue() - cy
      double distance = (vy * dx * dx - 2 * cov * dx * dy + vx * dy * dy) / (vx * vy - cov * cov)
      assertEquals(2.570920945d ** 2 as double, distance, 1e-8)
    }
    def circle = Ellipse.calculate(mtcars['wt'], mtcars['mpg'], 0.95, 'euclid', 101)
    double meanX = mtcars['wt'].sum() / 32d
    double meanY = mtcars['mpg'].sum() / 32d
    circle.x.eachWithIndex { BigDecimal x, int i ->
      assertEquals(0.95d, Math.hypot(x.doubleValue() - meanX, circle.y[i].doubleValue() - meanY), 1e-12)
    }
    assertThrows(IllegalArgumentException) { Ellipse.calculate(xs, ys, 0.95, 'unknown') }
    assertThrows(IllegalArgumentException) { Ellipse.calculate(xs, ys, 0.95, 't', 2) }
  }

  @Test
  void exactKsMatchesR() {
    List x = [0.61, 0.29, 0.06, 0.59, -1.73, -0.74, 0.51, -0.56, 0.39, 1.64, 0.05, -0.06,
              0.64, -0.82, 0.37, 1.77, 1.09, -1.28, 2.36, 1.31, 1.05, -0.32, -0.4, 1.06, -2.47]
    // R 4.6.1 ks.test(x,'pnorm'), ks.test(x,'pnorm',alternative='greater').
    assertEquals(0.389741d, KolmogorovSmirnov.testStandardNormality(x).pValue as double, 1e-6)
    assertEquals(0.906139d, KolmogorovSmirnov.testStandardNormality(x, null, 'greater').pValue as double, 1e-6)
    def uniform = getClass().getResource('/r-reference/uniform150.csv').readLines().collect { new BigDecimal(it.trim()) }
    def uniformDistribution = { Number value -> value as BigDecimal } as se.alipsa.matrix.stats.distribution.ContinuousDistribution
    // R 4.6.1 set.seed(7); ks.test(runif(150),'punif'): asymptotic n>=100.
    assertEquals(0.576115d, KolmogorovSmirnov.testAgainstDistribution(uniform, uniformDistribution).pValue as double, 1e-6)
    List y = [2.2, 1.66, 1.38, 0.2, 0.36, 0, 0.96, 1.56, 0.44, 1.5, -0.3, 0.66, 2.31, 3.29, -0.27, -0.37, 0.38, 0.7, 0.52, -0.71]
    assertEquals(0.528636d, KolmogorovSmirnov.twoSampleTest(x, y).pValue as double, 1e-6)
    assertEquals(0.357142857d, KolmogorovSmirnov.twoSampleTest(x[0..4], y[0..4]).pValue as double, 1e-9)
  }

  @Test
  void leverageIsStableAtLargeOffsets() {
    def x = (2000..2030).toList()
    def fit = new PolynomialRegression(x, x.collect { Math.sin(it) }, 3)
    def inverse = RegressionUtils.polynomialXtxInverse(x, 3, fit.center, fit.scale)
    // R 4.6.1: sum(predict(lm(y~poly(x,3)), se.fit=TRUE)$se.fit^2)/sigma(fit)^2 = rank = 4.
    double total = x.sum { RegressionUtils.polynomialLeverage(inverse, it as BigDecimal, 3, fit.center, fit.scale) } as double
    assertEquals(4d, total, 1e-9)
    def wellConditioned = (0..10).toList()
    def stable = new PolynomialRegression(wellConditioned, wellConditioned, 2)
    def rawInverse = RegressionUtils.polynomialXtxInverse(wellConditioned, 2)
    def scaledInverse = RegressionUtils.polynomialXtxInverse(wellConditioned, 2, stable.center, stable.scale)
    wellConditioned.each { Number value ->
      double raw = RegressionUtils.polynomialLeverage(rawInverse, value as BigDecimal, 2) as double
      double scaled = RegressionUtils.polynomialLeverage(scaledInverse, value as BigDecimal, 2, stable.center, stable.scale) as double
      assertEquals(raw, scaled, raw * 1e-9)
    }
  }

  @Test
  void degenerateInputsHaveDescriptiveFailures() {
    assertThrows(IllegalArgumentException) { Anova.aov([a: [1, 1], b: [2, 2]]) }
    assertThrows(IllegalArgumentException) { Anova.aov([a: [1], b: [2]]) }
    assertThrows(IllegalArgumentException) { Anova.aov([a: [], b: [2, 3]]) }
    assertThrows(IllegalArgumentException) { Correlation.cor([1, null], [1, 2]) }
    assertThrows(IllegalArgumentException) { Correlation.cor([1, Double.POSITIVE_INFINITY], [1, 2]) }
  }
}
