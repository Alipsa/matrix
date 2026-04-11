package regression

import static org.junit.jupiter.api.Assertions.assertArrayEquals
import static org.junit.jupiter.api.Assertions.assertEquals
import static se.alipsa.matrix.stats.regression.FitDsl.gam
import static se.alipsa.matrix.stats.regression.FitDsl.lm
import static se.alipsa.matrix.stats.regression.FitDsl.loess

import groovy.transform.CompileDynamic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.formula.ModelFrame
import se.alipsa.matrix.stats.regression.FitRegistry
import se.alipsa.matrix.stats.regression.FitResult
import se.alipsa.matrix.stats.regression.GamOptions
import se.alipsa.matrix.stats.regression.LoessOptions

/**
 * Tests for the Groovy formula fit convenience DSL.
 */
@CompileDynamic
@SuppressWarnings('DuplicateNumberLiteral')
class FitDslTest {

  @Test
  void testLmDslMatchesRegistryFit() {
    Matrix data = Matrix.builder()
      .columnNames(['x', 'group', 'y'])
      .rows([
        [1.0, 'A', 3.0],
        [2.0, 'B', 6.0],
        [3.0, 'A', 7.0],
        [4.0, 'B', 10.0],
        [5.0, 'A', 11.0],
      ])
      .types([BigDecimal, String, BigDecimal])
      .build()

    FitResult expected = FitRegistry.instance().get('lm').fit(ModelFrame.of('y ~ x + group', data).evaluate())
    FitResult actual = lm(data) {
      y | x + group
    }

    assertFitResultEquals(expected, actual)
  }

  @Test
  void testLoessDslMatchesRegistryFit() {
    Matrix data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows((0..<30).collect { int i ->
        BigDecimal x = i * 0.2
        BigDecimal y = (Math.sin(x as double) as BigDecimal) + (i % 4 == 0 ? 0.1 : -0.05)
        [x, y]
      })
      .types([BigDecimal, BigDecimal])
      .build()

    LoessOptions options = new LoessOptions(0.35d)
    FitResult expected = FitRegistry.instance().get('loess').fit(ModelFrame.of('y ~ x', data).evaluate(), options)
    FitResult actual = loess(data, options) {
      y | x
    }

    assertFitResultEquals(expected, actual)
  }

  @Test
  void testGamDslMatchesRegistryFit() {
    Matrix data = Matrix.builder()
      .columnNames(['time', 'group', 'y'])
      .rows((0..<24).collect { int i ->
        BigDecimal time = i * 0.3
        String group = i % 2 == 0 ? 'A' : 'B'
        BigDecimal y = (Math.sin(time as double) as BigDecimal) + (group == 'B' ? 1.0 : 0.0)
        [time, group, y]
      })
      .types([BigDecimal, String, BigDecimal])
      .build()

    GamOptions options = new GamOptions(0.5d)
    FitResult expected = FitRegistry.instance().get('gam').fit(ModelFrame.of('y ~ s(time, 6) + group', data).evaluate(), options)
    FitResult actual = gam(data, options) {
      y | smooth(time, 6) + group
    }

    assertFitResultEquals(expected, actual)
  }

  @Test
  void testStaticImportStyleWorksForAllFitDslMethods() {
    Matrix linear = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows([[1.0, 3.0], [2.0, 5.0], [3.0, 7.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    Matrix local = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows((0..<12).collect { int i ->
        BigDecimal x = i * 0.2
        [x, Math.sin(x as double) as BigDecimal]
      })
      .types([BigDecimal, BigDecimal])
      .build()

    Matrix gamData = Matrix.builder()
      .columnNames(['time', 'group', 'y'])
      .rows((0..<12).collect { int i ->
        BigDecimal time = i * 0.4
        String group = i % 2 == 0 ? 'A' : 'B'
        BigDecimal y = (Math.sin(time as double) as BigDecimal) + (group == 'B' ? 0.5 : 0.0)
        [time, group, y]
      })
      .types([BigDecimal, String, BigDecimal])
      .build()

    FitResult lmResult = lm(linear) {
      y | x
    }
    FitResult loessResult = loess(local) {
      y | x
    }
    FitResult gamResult = gam(gamData) {
      y | smooth(time, 6) + group
    }

    assertEquals(2, lmResult.coefficients.length)
    assertEquals(local.rowCount(), loessResult.fittedValues.length)
    assertEquals(gamData.rowCount(), gamResult.fittedValues.length)
  }

  private static void assertFitResultEquals(FitResult expected, FitResult actual) {
    assertArrayEquals(expected.coefficients, actual.coefficients, 1e-12d)
    assertArrayEquals(expected.standardErrors, actual.standardErrors, 1e-12d)
    assertArrayEquals(expected.fittedValues, actual.fittedValues, 1e-12d)
    assertArrayEquals(expected.residuals, actual.residuals, 1e-12d)
    assertEquals(expected.rSquared, actual.rSquared, 1e-12d)
    assertEquals(expected.predictorNames, actual.predictorNames)
  }
}
