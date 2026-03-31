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
    List<Number> response = [10.0, 20.0]
    List<Number> weights = [1.0, 2.0]
    List<Number> offset = [0.1, 0.2]
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
    List<Number> response = [10.0]
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

  @Test
  void testDotPowerExpansion() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'a', 'b', 'c'])
      .rows([
        [10.0, 1.0, 2.0, 3.0],
        [20.0, 4.0, 5.0, 6.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ .^2', data).evaluate()

    // .^2 expands to (a + b + c)^2 = a + b + c + a:b + a:c + b:c
    assertEquals(['a', 'b', 'c', 'a:b', 'a:c', 'b:c'], result.predictorNames)
  }

  @Test
  void testMixedDotPowerExpansion() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'a', 'b', 'x'])
      .rows([
        [10.0, 1.0, 2.0, 3.0],
        [20.0, 4.0, 5.0, 6.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal, BigDecimal])
      .build()

    // dot expands to a, b (x is explicitly named so excluded from dot)
    // (a + b + x)^2 = a + b + x + a:b + a:x + b:x
    ModelFrameResult result = ModelFrame.of('y ~ (. + x)^2', data).evaluate()

    assertEquals(['a', 'b', 'x', 'a:b', 'a:x', 'b:x'], result.predictorNames)
  }

  @Test
  void testCategoricalTreatmentContrasts() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'species'])
      .rows([
        [1.0, 10.0, 'setosa'],
        [2.0, 20.0, 'versicolor'],
        [3.0, 30.0, 'virginica'],
        [4.0, 40.0, 'setosa'],
      ])
      .types([BigDecimal, BigDecimal, String])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ x + species', data).evaluate()

    // Normalization sorts alphabetically: species before x
    assertEquals(['species_versicolor', 'species_virginica', 'x'], result.predictorNames)
    assertEquals(4, result.data.rowCount())

    // Row 0: setosa -> versicolor=0, virginica=0
    assertEquals(0.0G, result.data[0, 'species_versicolor'])
    assertEquals(0.0G, result.data[0, 'species_virginica'])

    // Row 1: versicolor -> versicolor=1, virginica=0
    assertEquals(1.0G, result.data[1, 'species_versicolor'])
    assertEquals(0.0G, result.data[1, 'species_virginica'])

    // Row 2: virginica -> versicolor=0, virginica=1
    assertEquals(0.0G, result.data[2, 'species_versicolor'])
    assertEquals(1.0G, result.data[2, 'species_virginica'])
  }

  @Test
  void testBooleanCategoricalEncoding() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'active'])
      .rows([
        [1.0, 10.0, false],
        [2.0, 20.0, true],
        [3.0, 30.0, true],
      ])
      .types([BigDecimal, BigDecimal, Boolean])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ x + active', data).evaluate()

    // Normalization sorts alphabetically: active before x
    assertEquals(['active_true', 'x'], result.predictorNames)
    assertEquals(0.0G, result.data[0, 'active_true'])
    assertEquals(1.0G, result.data[1, 'active_true'])
  }

  @Test
  void testCategoricalInteraction() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'weight', 'color'])
      .rows([
        [1.0, 10.0, 'blue'],
        [2.0, 20.0, 'green'],
        [3.0, 30.0, 'red'],
      ])
      .types([BigDecimal, BigDecimal, String])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ color:weight', data).evaluate()

    // color: blue (ref), green, red -> interaction with weight
    assertEquals(['color_green:weight', 'color_red:weight'], result.predictorNames)

    // Row 1: green -> green:weight = 1 * 20 = 20 (use == for BigDecimal scale-insensitive comparison)
    assertTrue((result.data[1, 'color_green:weight'] as BigDecimal) == 20.0)
    assertTrue((result.data[1, 'color_red:weight'] as BigDecimal) == 0.0)

    // Row 2: red -> red:weight = 1 * 30 = 30
    assertTrue((result.data[2, 'color_green:weight'] as BigDecimal) == 0.0)
    assertTrue((result.data[2, 'color_red:weight'] as BigDecimal) == 30.0)
  }

  @Test
  void testSingleLevelCategoricalProducesNoColumns() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'group'])
      .rows([
        [1.0, 10.0, 'A'],
        [2.0, 20.0, 'A'],
      ])
      .types([BigDecimal, BigDecimal, String])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ x + group', data).evaluate()

    // Single level -> no indicator columns
    assertEquals(['x'], result.predictorNames)
  }

  @Test
  void testWeightsByColumnName() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'w'])
      .rows([
        [10.0, 1.0, 0.5],
        [20.0, 2.0, 1.0],
        [30.0, 3.0, 1.5],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ .', data)
      .weights('w')
      .evaluate()

    // w should be excluded from dot expansion
    assertEquals(['x'], result.predictorNames)
    assertEquals([0.5, 1.0, 1.5], result.weights)
  }

  @Test
  void testWeightsByList() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[10.0, 1.0], [20.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    List<Number> weights = [1.0, 2.0]
    ModelFrameResult result = ModelFrame.of('y ~ x', data)
      .weights(weights)
      .evaluate()

    assertEquals([1.0, 2.0], result.weights)
  }

  @Test
  void testZeroWeightsAllowed() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[10.0, 1.0], [20.0, 2.0], [30.0, 3.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ x', data)
      .weights([0.0, 1.0, 0.0])
      .evaluate()

    assertEquals([0.0, 1.0, 0.0], result.weights)
  }

  @Test
  void testNegativeWeightsThrows() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[10.0, 1.0], [20.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ x', data)
        .weights([-1.0, 2.0])
        .evaluate()
    }
    assertTrue(ex.message.contains('non-negative'))
  }

  @Test
  void testOffsetByColumnName() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'off'])
      .rows([
        [10.0, 1.0, 0.1],
        [20.0, 2.0, 0.2],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ .', data)
      .offset('off')
      .evaluate()

    assertEquals(['x'], result.predictorNames)
    assertEquals([0.1, 0.2], result.offset)
  }

  @Test
  void testSubsetByClosure() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[10.0, 1.0], [20.0, 2.0], [30.0, 3.0], [40.0, 4.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ x', data)
      .subset { Row row -> (row['x'] as BigDecimal) > 1.0 }
      .evaluate()

    assertEquals([20.0, 30.0, 40.0], result.response)
    assertEquals(3, result.data.rowCount())
  }

  @Test
  void testSubsetByBooleanMask() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[10.0, 1.0], [20.0, 2.0], [30.0, 3.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ x', data)
      .subset([true, false, true])
      .evaluate()

    assertEquals([10.0, 30.0], result.response)
    assertEquals(2, result.data.rowCount())
  }

  @Test
  void testSubsetMaskSizeMismatchThrows() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[10.0, 1.0], [20.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ x', data)
        .subset([true, false, true])
        .evaluate()
    }
    assertTrue(ex.message.contains('Subset mask size'))
  }

  @Test
  void testWeightsSizeMismatchThrows() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[10.0, 1.0], [20.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ x', data)
        .weights([1.0, 2.0, 3.0])
        .evaluate()
    }
    assertTrue(ex.message.contains('Weights size'))
  }

  @Test
  void testNaOmitDropsRowsWithNulls() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'z'])
      .rows([
        [10.0, 1.0, 2.0],
        [null, 3.0, 4.0],
        [30.0, null, 6.0],
        [40.0, 7.0, 8.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ x + z', data)
      .naAction(NaAction.OMIT)
      .evaluate()

    assertEquals([10.0, 40.0], result.response)
    assertEquals(2, result.data.rowCount())
    assertEquals([1, 2], result.droppedRows)
  }

  @Test
  void testNaFailThrowsOnNulls() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([
        [10.0, 1.0],
        [null, 2.0],
      ])
      .types([BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ x', data)
        .naAction(NaAction.FAIL)
        .evaluate()
    }
    assertTrue(ex.message.contains('NA values found'))
    assertTrue(ex.message.contains('FAIL'))
  }

  @Test
  void testNoNullsProducesEmptyDroppedRows() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[10.0, 1.0], [20.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ x', data).evaluate()

    assertTrue(result.droppedRows.isEmpty())
  }

  @Test
  void testEmptyDataAfterNaOmitThrows() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[null, 1.0], [null, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ x', data).evaluate()
    }
    assertTrue(ex.message.contains('No observations remaining'))
  }

  @Test
  void testEnvironmentResolvesUnknownVariable() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[10.0, 1.0], [20.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    Map<String, List<?>> env = [z: [3.0, 4.0]]
    ModelFrameResult result = ModelFrame.of('y ~ x + z', data)
      .environment(env)
      .evaluate()

    assertEquals(['x', 'z'], result.predictorNames)
    assertEquals(2, result.data.rowCount())
  }

  @Test
  void testMissingVariableThrows() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[10.0, 1.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ x + z + w', data).evaluate()
    }
    assertTrue(ex.message.contains('Unknown variable(s)'))
    assertTrue(ex.message.contains('z'))
    assertTrue(ex.message.contains('w'))
  }

  @Test
  void testNonNumericEnvironmentVariableThrows() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[10.0, 1.0], [20.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    Map<String, List<?>> env = [z: ['not-a-number', 'also-not-numeric']]

    assertThrows(Exception) {
      ModelFrame.of('y ~ x + z', data)
        .environment(env)
        .evaluate()
    }
  }

  @Test
  void testEnvironmentVariableWithNullThrows() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[10.0, 1.0], [20.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    Map<String, List<?>> env = [z: [3.0, null]]

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ x + z', data)
        .environment(env)
        .evaluate()
    }
    assertTrue(ex.message.contains("Environment variable 'z' contains null values"))
  }

  @Test
  void testMissingResponseThrows() {
    Matrix data = Matrix.builder()
      .columnNames(['a', 'b'])
      .rows([[1.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ a + b', data).evaluate()
    }
    assertTrue(ex.message.contains("Response variable 'y' not found"))
  }

  @Test
  void testNonNumericResponseThrows() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([['hello', 1.0]])
      .types([String, BigDecimal])
      .build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ x', data).evaluate()
    }
    assertTrue(ex.message.contains('must be numeric'))
    assertTrue(ex.message.contains('String'))
  }

  @Test
  void testNullDataThrows() {
    assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ x', (Matrix) null)
    }
  }

  @Test
  void testNullFormulaStringThrows() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[1.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    assertThrows(IllegalArgumentException) {
      ModelFrame.of((String) null, data)
    }
  }

  @Test
  void testNormalizedFormulaPathWithDot() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'a', 'b', 'c'])
      .rows([
        [10.0, 1.0, 2.0, 3.0],
        [20.0, 4.0, 5.0, 6.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal, BigDecimal])
      .build()

    NormalizedFormula normalized = Formula.normalize('y ~ .')
    ModelFrameResult result = ModelFrame.of(normalized, data).evaluate()

    assertEquals(['a', 'b', 'c'], result.predictorNames)
  }

  @Test
  void testNormalizedFormulaWithoutDots() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'z'])
      .rows([[10.0, 1.0, 2.0], [20.0, 3.0, 4.0]])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    NormalizedFormula normalized = Formula.normalize('y ~ x + z')
    ModelFrameResult result = ModelFrame.of(normalized, data).evaluate()

    assertEquals(['x', 'z'], result.predictorNames)
    assertEquals([10.0, 20.0], result.response)
  }

  @Test
  void testSubsetThenNaOmit() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([
        [10.0, 1.0],
        [20.0, 2.0],
        [null, 3.0],
        [40.0, 4.0],
        [50.0, 5.0],
      ])
      .types([BigDecimal, BigDecimal])
      .build()

    // Subset keeps rows 1-4 (x > 1), then NA omit drops row 2 (null y)
    ModelFrameResult result = ModelFrame.of('y ~ x', data)
      .subset { Row row -> (row['x'] as BigDecimal) > 1.0 }
      .naAction(NaAction.OMIT)
      .evaluate()

    assertEquals([20.0, 40.0, 50.0], result.response)
    assertEquals([2], result.droppedRows) // original row index 2
  }

  @Test
  void testWeightsFilteredBySubsetAndNaOmit() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'w'])
      .rows([
        [10.0, 1.0, 0.5],
        [20.0, 2.0, 1.0],
        [null, 3.0, 1.5],
        [40.0, 4.0, 2.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    // Subset keeps rows 1-3 (x > 1), then NA omit drops row 2 (null y)
    // Surviving weights: row 1 (1.0) and row 3 (2.0)
    ModelFrameResult result = ModelFrame.of('y ~ x', data)
      .weights('w')
      .subset { Row row -> (row['x'] as BigDecimal) > 1.0 }
      .evaluate()

    assertEquals([20.0, 40.0], result.response)
    assertEquals([1.0, 2.0], result.weights)
  }

  @Test
  void testWeightsColumnNotFoundThrows() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[10.0, 1.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ x', data).weights('w').evaluate()
    }
    assertTrue(ex.message.contains("Weights column 'w' not found"))
  }

  @Test
  void testOffsetColumnNotFoundThrows() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[10.0, 1.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ x', data).offset('off').evaluate()
    }
    assertTrue(ex.message.contains("Offset column 'off' not found"))
  }
}
