package test.alipsa.matrix.api

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.stats.Correlation
import se.alipsa.matrix.stats.Normalize
import se.alipsa.matrix.stats.cluster.KMeans
import se.alipsa.matrix.stats.linalg.Linalg
import se.alipsa.matrix.stats.regression.LinearRegression
import se.alipsa.matrix.stats.solver.GoalSeek

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

/** Covers the documented statistics, regression, normalization, solver, clustering, and linalg APIs. */
@Tag('stats')
class StatsApiIT implements ApiItSupport {

  @Test
  void correlationNormalizationAndRegression() {
    assertEquals(1G, Correlation.cor([1, 2, 3], [2, 4, 6]))
    def normalized = Normalize.minMaxNorm([1, 2, 3])
    assertTrue(normalized[0].toBigDecimal().compareTo(0G) == 0)
    assertTrue(normalized[1].toBigDecimal().compareTo(0.5G) == 0)
    assertTrue(normalized[2].toBigDecimal().compareTo(1G) == 0)
    def model = new LinearRegression([2.7, 3, 5, 7, 9, 11, 14], [4, 5, 7, 10.8, 15, 20, 40])
    assertEquals(2.81388732G, model.getSlope(8))
    assertTrue((GoalSeek.solve(4, 0, 4) { it * it }.value as BigDecimal).subtract(2G).abs() < 0.000001G)
  }

  @Test
  void linearAlgebraAndClustering() {
    Matrix source = Matrix.builder().columnNames(['x', 'y']).rows([[4.0, 7.0], [2.0, 6.0]])
        .types([Double, Double]).build()
    assertEquals(10G, Linalg.det(source))
    assertTrue(((Linalg.inverse(source)[0, 0] as BigDecimal) - 0.6G).abs() < 0.000001G)
    Matrix clustered = new KMeans(Normalize.minMaxNorm(mtcars().select(['mpg', 'wt'])))
        .fit(['mpg', 'wt'], 2, 10, 'cluster', false)
    assertEquals(mtcars().rowCount(), clustered.rowCount())
    def converted = ListConverter.toBigDecimals([100, 200])
    assertTrue(converted[0].toBigDecimal().compareTo(100G) == 0)
    assertTrue(converted[1].toBigDecimal().compareTo(200G) == 0)
  }
}
