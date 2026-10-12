package formula

import static org.junit.jupiter.api.Assertions.assertArrayEquals
import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.formula.ModelFrame
import se.alipsa.matrix.stats.regression.LmMethod

class FactorCodingReferenceTest {

  @Test
  void marginsAndNoInterceptFollowR() {
    Matrix data = Matrix.builder().columnNames(['y', 'x', 'g', 'h']).rows([
      [1, 1, 'a', 'u'], [2, 3, 'b', 'v'], [3, 2, 'c', 'u'], [4, 5, 'a', 'v'],
      [5, 4, 'b', 'u'], [6, 6, 'c', 'v'], [7, 8, 'a', 'u'], [8, 7, 'b', 'v']
    ]).types([Integer, Integer, String, String]).build()
    // R 4.6.1: colnames(model.matrix(formula, data)); see formula.R.
    [
      'y ~ 0 + g': ['g_a', 'g_b', 'g_c'],
      'y ~ g:x': ['g_a:x', 'g_b:x', 'g_c:x'],
      'y ~ g + g:x': ['g_b', 'g_c', 'g_a:x', 'g_b:x', 'g_c:x'],
      'y ~ x + g:x': ['x', 'x:g_b', 'x:g_c'],
      'y ~ 0 + g + h': ['g_a', 'g_b', 'g_c', 'h_v'],
      'y ~ 0 + x + g': ['x', 'g_a', 'g_b', 'g_c'],
      'y ~ g:h': ['g_a:h_u', 'g_b:h_u', 'g_c:h_u', 'g_a:h_v', 'g_b:h_v', 'g_c:h_v'],
      'y ~ 0 + g + g:h': ['g_a', 'g_b', 'g_c', 'g_a:h_v', 'g_b:h_v', 'g_c:h_v']
    ].each { String formula, List<String> expected ->
      assertEquals(expected, ModelFrame.of(formula, data).evaluate().predictorNames, formula)
    }
    // R 4.6.1: coef(lm(y ~ 0 + g, data)) = c(4,5,4.5).
    def fit = new LmMethod().fit(ModelFrame.of('y ~ 0 + g', data).evaluate())
    assertArrayEquals([4.0d, 5.0d, 4.5d] as double[], fit.coefficients, 1e-12d)
  }
}
