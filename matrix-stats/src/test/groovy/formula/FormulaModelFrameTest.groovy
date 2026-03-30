package formula

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.stats.formula.Formula
import se.alipsa.matrix.stats.formula.ModelFrame
import se.alipsa.matrix.stats.formula.ModelFrameResult
import se.alipsa.matrix.stats.formula.NaAction
import se.alipsa.matrix.stats.formula.NormalizedFormula

/**
 * Tests for ModelFrame evaluation pipeline.
 */
@CompileStatic
@SuppressWarnings(['DuplicateNumberLiteral', 'DuplicateStringLiteral'])
class FormulaModelFrameTest {

  @Test
  void testModelFrameResultHoldsAllFields() {
    Matrix predictors = Matrix.builder()
      .columnNames(['x', 'z'])
      .rows([[1.0, 2.0], [3.0, 4.0]])
      .types([BigDecimal, BigDecimal])
      .build()
    List<Number> response = [10.0, 20.0] as List<Number>
    List<Number> weights = [1.0, 2.0] as List<Number>
    List<Number> offset = [0.1, 0.2] as List<Number>
    List<Integer> dropped = [2, 5]
    NormalizedFormula formula = Formula.normalize('y ~ x + z')

    ModelFrameResult result = new ModelFrameResult(
      predictors, response, 'y', true,
      ['x', 'z'], weights, offset, dropped, formula
    )

    assertEquals(predictors, result.data)
    assertEquals(response, result.response)
    assertEquals('y', result.responseName)
    assertTrue(result.includeIntercept)
    assertEquals(['x', 'z'], result.predictorNames)
    assertEquals(weights, result.weights)
    assertEquals(offset, result.offset)
    assertEquals(dropped, result.droppedRows)
    assertEquals(formula, result.formula)
  }

  @Test
  void testModelFrameResultNullableFields() {
    Matrix predictors = Matrix.builder()
      .columnNames(['x'])
      .rows([[1.0]])
      .types([BigDecimal])
      .build()
    List<Number> response = [10.0] as List<Number>
    NormalizedFormula formula = Formula.normalize('y ~ x')

    ModelFrameResult result = new ModelFrameResult(
      predictors, response, 'y', true,
      ['x'], null, null, [], formula
    )

    assertNull(result.weights)
    assertNull(result.offset)
    assertTrue(result.droppedRows.isEmpty())
  }

  @Test
  void testSimpleNumericFormula() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'z'])
      .rows([
        [10.0, 1.0, 2.0],
        [20.0, 3.0, 4.0],
        [30.0, 5.0, 6.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ x + z', data).evaluate()

    assertEquals('y', result.responseName)
    assertEquals([10.0, 20.0, 30.0], result.response)
    assertTrue(result.includeIntercept)
    assertEquals(['x', 'z'], result.predictorNames)
    assertEquals(3, result.data.rowCount())
    assertEquals(2, result.data.columnCount())
    assertTrue(result.droppedRows.isEmpty())
    assertNull(result.weights)
    assertNull(result.offset)
  }

  @Test
  void testDotExpansion() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'a', 'b', 'c'])
      .rows([
        [10.0, 1.0, 2.0, 3.0],
        [20.0, 4.0, 5.0, 6.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ .', data).evaluate()

    assertEquals(['a', 'b', 'c'], result.predictorNames)
    assertEquals(2, result.data.rowCount())
  }

  @Test
  void testDotWithExclusion() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'a', 'b', 'c'])
      .rows([
        [10.0, 1.0, 2.0, 3.0],
        [20.0, 4.0, 5.0, 6.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ . - b', data).evaluate()

    assertEquals(['a', 'c'], result.predictorNames)
  }

  @Test
  void testInterceptExcluded() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[10.0, 1.0], [20.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ 0 + x', data).evaluate()

    assertEquals(false, result.includeIntercept)
    assertEquals(['x'], result.predictorNames)
  }
}
