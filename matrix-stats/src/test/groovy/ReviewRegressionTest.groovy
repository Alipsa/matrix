import static org.junit.jupiter.api.Assertions.*

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.Normalize
import se.alipsa.matrix.stats.contingency.Fisher
import se.alipsa.matrix.stats.distribution.NormalDistribution
import se.alipsa.matrix.stats.distribution.TDistribution
import se.alipsa.matrix.stats.formula.ModelFrame
import se.alipsa.matrix.stats.linear.MatrixAlgebra
import se.alipsa.matrix.stats.normality.KolmogorovSmirnov
import se.alipsa.matrix.stats.util.LeastSquaresKernel

import java.time.Duration

class ReviewRegressionTest {

  @Test
  @CompileStatic
  void fisherAcceptsIntegerListsAndLiterals() {
    List<List<Integer>> counts = [[3, 1], [1, 3]]
    assertEquals(Fisher.test(counts).pValue, Fisher.test([[3, 1], [1, 3]]).pValue)
    assertEquals(Fisher.test(counts, Fisher.Alternative.GREATER).pValue,
        Fisher.test([[3, 1], [1, 3]], 'greater').pValue)
  }

  @Test
  void fisherResultsWithoutConfidenceLevelCanBeFormatted() {
    def result = new Fisher.FisherResult(confidenceInterval: [1G, 2G])
    assertTrue(result.toString().contains('95% CI'))
    assertEquals(Double.POSITIVE_INFINITY, Fisher.test([[5, 0], [1, 5]]).oddsRatio)
  }

  @Test
  void nonFiniteNormalizationReturnsMissing() {
    [Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Float.NaN,
     Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY].each { Number x ->
      assertNull(Normalize.minMaxNorm(x, 0, 10))
      assertTrue(Double.isNaN(Normalize.minMaxNorm(x, 0, 10, Normalize.MissingValueType.DOUBLE_NAN) as double))
      assertNull(Normalize.meanNorm(x, 5, 0, 10))
      assertNull(Normalize.stdScaleNorm(x, 5, 2))
    }
    assertEquals([null, null], Normalize.meanNorm([1d, Double.POSITIVE_INFINITY]))
    assertEquals([null, null], Normalize.stdScaleNorm([1d, Double.NaN] as Double[]))
    assertEquals([null, null], Normalize.minMaxNorm([1f, Float.NEGATIVE_INFINITY]))
    assertNull(Normalize.minMaxNorm(1d, 0, Double.POSITIVE_INFINITY))
    assertNull(Normalize.meanNorm(1d, Double.NaN, 0, 10))
    assertNull(Normalize.stdScaleNorm(1f, 0, Float.POSITIVE_INFINITY))
  }

  @Test
  void bigDecimalTTailsPreserveExtremeProbabilities() {
    // R 4.6.1: 2*pt(-c(50,1e5,1e7),10).
    def distribution = new TDistribution(10)
    [[50G, 2.4743103293026876e-13d], [1e5G, 2.4609374887207093e-46d],
     [1e7G, 2.4609374999989094e-66d]].each { row ->
      double expected = row[1] as double
      assertEquals(expected, distribution.twoTailedPValue(row[0] as BigDecimal) as double, expected * 1e-10)
      assertEquals(expected / 2, distribution.cdf(-(row[0] as BigDecimal)) as double, expected * 1e-10)
    }
  }

  @Test
  void exactKsRescalesLargeMatrixPowers() {
    def normal = new NormalDistribution()
    // R 4.6.1: ks.test(qnorm(((1:n)-.5)/n)+.1,'pnorm',exact=TRUE).
    [700: 0.19364764887762d, 750: 0.16523708842156d, 1000: 0.074646660750903d].each { int n, double expected ->
      def data = (1..n).collect { int i -> normal.inverseCumulativeProbability((i - 0.5d) / n) + 0.1G }
      assertEquals(expected, KolmogorovSmirnov.testStandardNormality(data, true).pValue as double, 1e-9)
    }
  }

  @Test
  void factorMarginsCanBeContainedInEarlierInteractions() {
    Matrix data = Matrix.builder().columns([y: 1..8, a: ['p','q','p','q','p','q','p','q'],
        b: ['u','u','v','v','u','u','v','v'], c: ['u','u','u','u','v','v','v','v']])
        .types(Integer, String, String, String).build()
    // R 4.6.1 model.matrix(y~a:b+a:c): intercept,ap:bu,aq:bu,ap:bv,aq:bv,ap:cv,aq:cv.
    def frame = ModelFrame.of('y ~ a:b + a:c', data).evaluate()
    assertEquals(['a_p:b_u','a_q:b_u','a_p:b_v','a_q:b_v','a_p:c_v','a_q:c_v'], frame.predictorNames)
    frame.data['a_q:c_v'].eachWithIndex { Number value, int i ->
      assertEquals(i == 5 || i == 7 ? 1d : 0d, value.doubleValue(), 0d)
    }
  }

  @Test
  void reusableQrSolvesIndependentResponsesAndCopiesDesign() {
    double[][] design = [[1d,0d],[1d,1d],[1d,2d],[1d,3d]] as double[][]
    def factor = LeastSquaresKernel.factorize(design)
    design[0][0] = 999
    assertArrayEquals([0.8d,2.3d] as double[], factor.solve([1d,3d,5d,8d] as double[]), 1e-12)
    assertArrayEquals([5d,-2d] as double[], factor.solve([5d,3d,1d,-1d] as double[]), 1e-12)
    assertThrows(IllegalArgumentException) { factor.solve([1d] as double[]) }
  }

  @Test
  void matrixValidationDoesNotRescanAtEveryEntry() {
    double[][] identity = new double[200][200]
    (0..<200).each { int i -> identity[i][i] = 1 }
    assertTimeout(Duration.ofSeconds(2), {
      long start = System.nanoTime()
      assertEquals(1d, MatrixAlgebra.cholesky(identity)[199][199], 0d)
      assertEquals(200, MatrixAlgebra.symmetricEigenvalues(identity).length)
      println "Cholesky and eigenvalues n=200: ${(System.nanoTime() - start) / 1e9} seconds"
    } as org.junit.jupiter.api.function.Executable)
  }

  @Test
  void matrixDecompositionsRemainScaleInvariant() {
    [1e-8d, 1d, 1e8d].each { double scale ->
      double[][] matrix = [[4*scale,scale],[scale,3*scale]] as double[][]
      double[][] lower = MatrixAlgebra.cholesky(matrix)
      assertEquals(4*scale, lower[0][0]**2, scale*1e-12)
      double[] eigenvalues = MatrixAlgebra.symmetricEigenvalues(matrix)
      Arrays.sort(eigenvalues)
      assertEquals((7-Math.sqrt(5))/2*scale, eigenvalues[0], scale*1e-12)
    }
  }
}
