package normality

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.stats.normality.CramerVonMises
import se.alipsa.matrix.stats.normality.KSquared
import se.alipsa.matrix.stats.normality.KolmogorovSmirnov
import se.alipsa.matrix.stats.normality.Lilliefors
import se.alipsa.matrix.stats.normality.ShapiroFrancia
import se.alipsa.matrix.stats.normality.ShapiroWilk

class NormalityReferenceTest {

  private static final List<Integer> CLASSIC = [148, 154, 158, 160, 161, 162, 166, 170, 182, 195, 236]

  @Test
  void shapiroWilkReferences() {
    // R 4.6.1: shapiro.test(c(148,154,158,160,161,162,166,170,182,195,236))
    def result = ShapiroWilk.test(CLASSIC)
    assertEquals(0.7888147d, result.W as double, 1e-6d)
    assertEquals(0.00670381d, result.pValue as double, 1e-7d)
    // R 4.6.1: shapiro.test(c(1,2,4)); shapiro.test(c(2.1,3.5,1.2,8.9,4.4))
    assertEquals(0.6368868d, ShapiroWilk.test([1, 2, 4]).pValue as double, 1e-6d)
    assertEquals(0.394673d, ShapiroWilk.test([2.1, 3.5, 1.2, 8.9, 4.4]).pValue as double, 1e-6d)
    def large = ShapiroWilk.test((1..300).collect { Math.log(it) })
    assertTrue(large.W > 0 && large.W <= 1)
  }

  @Test
  void estimatedNormalReferences() {
    // nortest 1.0.4: lillie.test(x), sf.test(x), cvm.test(x); x is CLASSIC.
    assertEquals(0.259215d, Lilliefors.testNormality(CLASSIC).statistic as double, 1e-6d)
    assertEquals(0.0374076d, Lilliefors.testNormality(CLASSIC).pValue as double, 1e-6d)
    assertEquals(0.771382d, ShapiroFrancia.test(CLASSIC).W as double, 1e-6d)
    assertEquals(0.00734764d, ShapiroFrancia.test(CLASSIC).pValue as double, 1e-7d)
    assertEquals(0.163917d, CramerVonMises.testNormality(CLASSIC).rawStatistic as double, 1e-6d)
    assertEquals(0.0125236d, CramerVonMises.testNormality(CLASSIC).pValue as double, 1e-6d)
    // nortest 1.0.4: cvm.test(c(rep(1,29),50)) (approximation floor).
    assertEquals(7.37e-10d, CramerVonMises.testNormality([*Collections.nCopies(29, 1), 50]).pValue as double, 1e-16d)
  }

  @Test
  void kSquaredCountProductsDoNotOverflow() {
    // fBasics 4052.98: dagoTest(log(1:211)); pchisq(K2,2,lower.tail=FALSE).
    def result = KSquared.test((1..211).collect { Math.log(it) })
    assertEquals(76.875496014d, result.statistic as double, 8e-5d)
    assertEquals(-7.463146798d, result.zSkewness as double, 8e-6d)
    assertEquals(4.601840489d, result.zKurtosis as double, 5e-6d)
    assertEquals(2.026273869e-17d, result.pValue as double, 2.1e-23d)
  }

  @Test
  void irisAndMtcarsReferences() {
    // R 4.6.1 shapiro.test; nortest 1.0.4 lillie.test, sf.test, cvm.test; fBasics 4052.98 dagoTest.
    [[Dataset.iris()['Sepal Length'], 0.976090, 0.0101812, 0.00578839, 0.0262108, 0.0470646, 5.735584],
     [Dataset.mtcars()['mpg'], 0.947565, 0.122881, 0.217119, 0.149488, 0.155805, 2.714135]].each { row ->
      List data = row[0]
      assertEquals(row[1] as double, ShapiroWilk.test(data).W as double, 1e-6)
      assertEquals(row[2] as double, ShapiroWilk.test(data).pValue as double, 1e-6)
      assertEquals(row[3] as double, Lilliefors.testNormality(data).pValue as double, 1e-6)
      assertEquals(row[4] as double, ShapiroFrancia.test(data).pValue as double, 1e-6)
      assertEquals(row[5] as double, CramerVonMises.testNormality(data).pValue as double, 1e-6)
      assertEquals(row[6] as double, KSquared.test(data).statistic as double, 1e-5)
      assertEquals(Lilliefors.testNormality(data).statistic as double, KolmogorovSmirnov.testNormality(data).dStatistic as double, 1e-12)
    }
    def outliers = [*Collections.nCopies(29, 1), 50]
    assertEquals(1.12245e-25d, Lilliefors.testNormality(outliers).pValue as double, 1.2e-30d)
    assertTrue(Double.isFinite(KSquared.test((1..300).collect { Math.log(it) }).statistic as double))
  }

  @Test
  void approximationsAreMonotoneAndKsSeriesIsContinuous() {
    def lillie = Lilliefors.getDeclaredMethod('calculatePValue', Double.TYPE, Integer.TYPE)
    def cvm = CramerVonMises.getDeclaredMethod('calculatePValue', Double.TYPE)
    def ks = KolmogorovSmirnov.getDeclaredMethod('kolmogorovProbability', Double.TYPE)
    [lillie, cvm, ks].each { it.accessible = true }
    double lastL = 1
    double lastC = 1
    (1..2000).each { int i ->
      double l = lillie.invoke(null, i / 2000d, 32) as double
      double c = cvm.invoke(null, i / 1000d) as double
      assertTrue(l <= lastL + 1e-10)
      assertTrue(c <= lastC + 1e-10)
      lastL = l
      lastC = c
    }
    assertEquals(1d, ks.invoke(null, 0.01d) as double, 0d)
    assertEquals(ks.invoke(null, 1.18d - 1e-14) as double, ks.invoke(null, 1.18d + 1e-14) as double, 1e-12)
  }
}
