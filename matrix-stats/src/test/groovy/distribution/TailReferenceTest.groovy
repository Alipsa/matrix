package distribution

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.distribution.ChiSquaredDistribution
import se.alipsa.matrix.stats.distribution.FDistribution
import se.alipsa.matrix.stats.distribution.NormalDistribution
import se.alipsa.matrix.stats.distribution.SpecialFunctions
import se.alipsa.matrix.stats.distribution.TDistribution

class TailReferenceTest {

  @Test
  void normalLowerTailAndInverse() {
    NormalDistribution normal = new NormalDistribution()
    // R 4.6.1: pnorm(c(-10, -9)); qnorm(1e-20)
    assertEquals(7.61985302416047e-24d, normal.cumulativeProbability(-10.0d), 7.7e-30d)
    assertEquals(1.12858840595384e-19d, normal.cumulativeProbability(-9.0d), 1.2e-25d)
    assertEquals(-9.26234008979841d, normal.inverseCumulativeProbability(1e-20d), 1e-6d)
  }

  @Test
  void upperTailsRemainPositive() {
    // R 4.6.1: 2*pt(-10,100); pt(-5,30); pf(100,3,100,lower.tail=FALSE)
    assertEquals(9.90168898459409e-17d, TDistribution.pValue(10.0d, 100.0d), 1e-22d)
    assertEquals(9.90168898459409e-17d, TDistribution.pValue(10, 100) as double, 1e-22d)
    assertEquals(1.16483427335039e-5d, new TDistribution(30).oneTailedPValueLower(-5.0d), 1.2e-11d)
    assertEquals(5.50958900283287e-30d, FDistribution.pValue(100.0d, 3.0d, 100.0d), 5.6e-36d)
    // R 4.6.1: pchisq(200,2,lower.tail=FALSE)
    assertEquals(3.72007597602084e-44d, new ChiSquaredDistribution(2).survivalProbability(200) as double, 3.8e-50d)
  }

  @Test
  void largeShapeConverges() {
    // R 4.6.1: pchisq(c(1999,9999,99999), c(2000,10000,100000))
    [2000: 0.4978966241d, 10000: 0.4990596156d, 100000: 0.4997026438d].each { int df, double expected ->
      assertEquals(expected, new ChiSquaredDistribution(df).cumulativeProbability(df - 1) as double, 1e-6d)
    }
    assertTrue(Double.isFinite(SpecialFunctions.regularizedIncompleteGammaP(1e5d, 1e5d)))
  }
}
