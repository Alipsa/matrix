package formula

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.formula.ModelFrame
import se.alipsa.matrix.stats.formula.NaAction
import se.alipsa.matrix.stats.regression.GamMethod
import se.alipsa.matrix.stats.regression.LmMethod
import se.alipsa.matrix.stats.regression.LoessMethod

class FormulaOrderReferenceTest {

  @Test
  void columnsFollowRFirstAppearanceAndInteractionDegree() {
    Matrix data = Matrix.builder().columns([y: 1..8, x: [1, 3, 2, 5, 4, 6, 8, 7],
        z: [2, 1, 4, 3, 6, 5, 8, 9], w: [1, 1, 2, 2, 3, 3, 4, 5],
        g: ['a', 'b', 'c', 'a', 'b', 'c', 'a', 'b']]).types(Integer, Integer, Integer, Integer, String).build()
    // R 4.6.1 colnames(model.matrix(f,data)); matrix-stats transform/factor naming.
    ['y ~ x + I(x^2)': ['x', 'I_x_pow_2'], 'y ~ z + x': ['z', 'x'],
     'y ~ w + g + x': ['w', 'g_b', 'g_c', 'x'],
     'y ~ g*x': ['g_b', 'g_c', 'x', 'g_b:x', 'g_c:x'],
     'y ~ x:z + w': ['w', 'x:z'],
     'y ~ (x + z + w)^2': ['x', 'z', 'w', 'x:z', 'x:w', 'z:w']].each { String formula, List expected ->
      assertEquals(expected, ModelFrame.of(formula, data).evaluate().predictorNames, formula)
    }
  }

  @Test
  void auxiliaryNaNsFollowNaPolicyAndInfiniteFitsFail() {
    Matrix data = Matrix.builder().columns([x: [1, 2, 3, 4], y: [2, 5, 6, 9]]).types(Integer, Integer).build()
    def frame = ModelFrame.of('y ~ x', data).weights([1, Double.NaN, 1, 1])
        .offset([0, 0, Float.NaN, 0]).naAction(NaAction.OMIT).evaluate()
    assertEquals([1, 2], frame.droppedRows)
    assertEquals([1, 1], frame.weights)
    assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ x', data).weights([1, Double.NaN, 1, 1]).naAction(NaAction.FAIL).evaluate()
    }
    Matrix infinite = Matrix.builder().columns([x: [1d, 2d, Double.POSITIVE_INFINITY, 4d], y: [2d, 5d, 6d, 9d]]).types(Double, Double).build()
    [new LmMethod(), new GamMethod(), new LoessMethod()].each { method ->
      assertThrows(IllegalArgumentException) { method.fit(ModelFrame.of('y ~ x', infinite).evaluate()) }
    }
  }
}
