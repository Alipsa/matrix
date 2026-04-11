package formula

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.ext.NumberExtension
import se.alipsa.matrix.stats.formula.Formula
import se.alipsa.matrix.stats.formula.FormulaParseException
import se.alipsa.matrix.stats.formula.ModelFrame
import se.alipsa.matrix.stats.formula.ModelFrameResult
import se.alipsa.matrix.stats.formula.NaAction
import se.alipsa.matrix.stats.formula.NormalizedFormula
import se.alipsa.matrix.stats.formula.Terms
import se.alipsa.matrix.stats.formula.dsl.GroovyFormulaSpec
import se.alipsa.matrix.stats.formula.dsl.TermRef

/**
 * Tests for ModelFrame evaluation pipeline.
 */
@CompileStatic
@SuppressWarnings(['ClassSize', 'DuplicateNumberLiteral', 'DuplicateStringLiteral'])
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
    Terms terms = new Terms(formula.response, true, [])

    ModelFrameResult result = new ModelFrameResult(
      predictors, response, 'y', true,
      ['x', 'z'], weights, offset, dropped, formula, terms
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
    Terms terms = new Terms(formula.response, true, [])

    ModelFrameResult result = new ModelFrameResult(
      predictors, response, 'y', true,
      ['x'], null, null, [], formula, terms
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
  void testWeightsAndOffsetStayAlignedAfterSubsetAndNaOmit() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([
        [10.0, 1.0],
        [20.0, null],
        [30.0, 3.0],
        [40.0, 4.0],
      ])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ x', data)
      .weights([1.0, 2.0, null, 4.0])
      .offset([0.1, 0.2, 0.3, 0.4])
      .subset([true, true, true, true])
      .naAction(NaAction.OMIT)
      .evaluate()

    assertEquals([10.0, 40.0], result.response)
    assertEquals([1.0, 4.0], result.weights)
    assertEquals([0.1, 0.4], result.offset)
    assertEquals([1, 2], result.droppedRows)
    assertEquals(2, result.data.rowCount())
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
  void testOffsetByList() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[10.0, 1.0], [20.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ x', data)
      .offset([0.25, 0.5])
      .evaluate()

    assertEquals([0.25, 0.5], result.offset)
  }

  @Test
  void testOffsetSizeMismatchThrows() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[10.0, 1.0], [20.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ x', data)
        .offset([0.1, 0.2, 0.3])
        .evaluate()
    }
    assertTrue(ex.message.contains('Offset size'))
  }

  @Test
  void testOffsetUnknownColumnThrows() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[10.0, 1.0], [20.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ x', data)
        .offset('missing')
        .evaluate()
    }
    assertTrue(ex.message.contains("Offset column 'missing' not found"))
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
  void testDotExpansionWithWeightsOffsetAndExclusionAfterFiltering() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'z', 'w', 'off'])
      .rows([
        [10.0, 1.0, 5.0, 0.5, 0.1],
        [20.0, 2.0, 6.0, 1.0, 0.2],
        [30.0, 3.0, 7.0, 1.5, 0.3],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ . - z', data)
      .weights('w')
      .offset('off')
      .subset([true, false, true])
      .evaluate()

    assertEquals(['x'], result.predictorNames)
    assertEquals([0.5, 1.5], result.weights)
    assertEquals([0.1, 0.3], result.offset)
    assertEquals([10.0, 30.0], result.response)
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
  void testNaActionEnumIncludesDocumentedModes() {
    assertTrue(NaAction.values().toList().containsAll([NaAction.OMIT, NaAction.FAIL]))
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

  @Test
  void testNaOmitHandlesNullInWeightsColumn() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'w'])
      .rows([
        [10.0, 1.0, 0.5],
        [20.0, 2.0, null],
        [30.0, 3.0, 1.5],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ x', data)
      .weights('w')
      .naAction(NaAction.OMIT)
      .evaluate()

    assertEquals([10.0, 30.0], result.response)
    assertEquals([0.5, 1.5], result.weights)
    assertEquals([1], result.droppedRows)
  }

  @Test
  void testNaOmitHandlesNullInOffsetColumn() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'off'])
      .rows([
        [10.0, 1.0, 0.1],
        [20.0, 2.0, null],
        [30.0, 3.0, 0.3],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ x', data)
      .offset('off')
      .naAction(NaAction.OMIT)
      .evaluate()

    assertEquals([10.0, 30.0], result.response)
    assertEquals([0.1, 0.3], result.offset)
    assertEquals([1], result.droppedRows)
  }

  @Test
  void testNaFailRejectsNullInWeightsColumn() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'w'])
      .rows([
        [10.0, 1.0, 0.5],
        [20.0, 2.0, null],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ x', data)
        .weights('w')
        .naAction(NaAction.FAIL)
        .evaluate()
    }
    assertTrue(ex.message.contains('NA values found'))
  }

  @Test
  void testDotExpandsToInterceptOnlyWhenEmpty() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'w'])
      .rows([
        [10.0, 0.5],
        [20.0, 1.0],
      ])
      .types([BigDecimal, BigDecimal])
      .build()

    // With y as response and w as weights, dot has no columns to expand into
    ModelFrameResult result = ModelFrame.of('y ~ .', data)
      .weights('w')
      .evaluate()

    assertTrue(result.predictorNames.isEmpty())
    assertTrue(result.includeIntercept)
    assertEquals([10.0, 20.0], result.response)
  }

  @Test
  void testNaOmitHandlesNullInListBasedWeights() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([
        [10.0, 1.0],
        [20.0, 2.0],
        [30.0, 3.0],
      ])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ x', data)
      .weights([1.0, null, 2.0])
      .naAction(NaAction.OMIT)
      .evaluate()

    assertEquals([10.0, 30.0], result.response)
    assertEquals([1.0, 2.0], result.weights)
    assertEquals([1], result.droppedRows)
  }

  @Test
  void testNaOmitHandlesNullInListBasedOffset() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([
        [10.0, 1.0],
        [20.0, 2.0],
        [30.0, 3.0],
      ])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ x', data)
      .offset([0.1, null, 0.3])
      .naAction(NaAction.OMIT)
      .evaluate()

    assertEquals([10.0, 30.0], result.response)
    assertEquals([0.1, 0.3], result.offset)
    assertEquals([1], result.droppedRows)
  }

  @Test
  void testNaFailRejectsNullInListBasedWeights() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([
        [10.0, 1.0],
        [20.0, 2.0],
      ])
      .types([BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ x', data)
        .weights([1.0, null])
        .naAction(NaAction.FAIL)
        .evaluate()
    }
    assertTrue(ex.message.contains('NA values found'))
  }

  @Test
  void testNestedDotExpandsToInterceptWhenEmpty() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'w', 'x'])
      .rows([
        [10.0, 0.5, 1.0],
        [20.0, 1.0, 2.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    // dot expands to nothing (only x and w remain; w is weights, x is explicit)
    // so (. + x)^2 simplifies to (1 + x)^2 which is just x
    ModelFrameResult result = ModelFrame.of('y ~ (. + x)^2', data)
      .weights('w')
      .evaluate()

    assertEquals(['x'], result.predictorNames)
    assertEquals([10.0, 20.0], result.response)
  }

  @Test
  @CompileDynamic
  void testDslModelFrameMatchesStringFormulaForNumericPredictors() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'z'])
      .rows([
        [10.0, 1.0, 2.0],
        [20.0, 3.0, 4.0],
        [30.0, 5.0, 6.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult expected = ModelFrame.of('y ~ x + z', data).evaluate()
    ModelFrameResult actual = ModelFrame.of(data) {
      y | x + z
    }.evaluate()

    assertModelFrameParity(expected, actual)
  }

  @Test
  @CompileDynamic
  void testDslModelFrameMatchesStringFormulaForCategoricalEncoding() {
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

    ModelFrameResult expected = ModelFrame.of('y ~ x + species', data).evaluate()
    ModelFrameResult actual = ModelFrame.of(data) {
      y | x + species
    }.evaluate()

    assertModelFrameParity(expected, actual)
  }

  @Test
  @CompileDynamic
  void testDslModelFrameMatchesStringFormulaForInteractions() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'group'])
      .rows([
        [1.0, 2.0, 'A'],
        [2.0, 3.0, 'B'],
        [3.0, 4.0, 'A'],
      ])
      .types([BigDecimal, BigDecimal, String])
      .build()

    ModelFrameResult expected = ModelFrame.of('y ~ x + group + x:group', data).evaluate()
    ModelFrameResult actual = ModelFrame.of(data) {
      y | x + group + (x % group)
    }.evaluate()

    assertModelFrameParity(expected, actual)
  }

  @Test
  @CompileDynamic
  void testDslModelFrameMatchesStringFormulaForInteractionFunctionAlias() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'group'])
      .rows([
        [1.0, 2.0, 'A'],
        [2.0, 3.0, 'B'],
        [3.0, 4.0, 'A'],
      ])
      .types([BigDecimal, BigDecimal, String])
      .build()

    ModelFrameResult expected = ModelFrame.of('y ~ x + group + x:group', data).evaluate()
    ModelFrameResult actual = ModelFrame.of(data) {
      y | x + group + interaction(x, group)
    }.evaluate()

    assertModelFrameParity(expected, actual)
  }

  @Test
  @CompileDynamic
  void testDslModelFrameMatchesStringFormulaForAllExpansionAndNoIntercept() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'z', 'id'])
      .rows([
        [10.0, 1.0, 2.0, 100.0],
        [20.0, 3.0, 4.0, 200.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult allExpected = ModelFrame.of('y ~ . - id', data).evaluate()
    ModelFrameResult allActual = ModelFrame.of(data) {
      y | all - id
    }.evaluate()
    assertModelFrameParity(allExpected, allActual)

    ModelFrameResult noInterceptExpected = ModelFrame.of('y ~ 0 + x + z', data).evaluate()
    ModelFrameResult noInterceptActual = ModelFrame.of(data) {
      y | noIntercept + x + z
    }.evaluate()
    assertModelFrameParity(noInterceptExpected, noInterceptActual)
  }

  @Test
  @CompileDynamic
  void testDslModelFrameMatchesStringFormulaForQuotedIdentifiers() {
    Matrix data = Matrix.builder()
      .columnNames(['y value', 'gross margin', 'unit price'])
      .rows([
        [10.0, 1.5, 3.0],
        [20.0, 2.5, 4.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult expected = ModelFrame.of('`y value` ~ `gross margin` + `unit price`', data).evaluate()
    ModelFrameResult actual = ModelFrame.of(data) {
      col('y value') | col('gross margin') + col('unit price')
    }.evaluate()

    assertModelFrameParity(expected, actual)
  }

  @Test
  @CompileDynamic
  void testDslModelFrameMatchesStringFormulaForTransforms() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'z', 'w'])
      .rows([
        [10.0, 1.0, 4.0, 0.0],
        [20.0, 2.0, 9.0, 1.0],
        [30.0, NumberExtension.E, 16.0, 2.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult expected = ModelFrame.of('y ~ log(x) + sqrt(z) + exp(w)', data).evaluate()
    ModelFrameResult actual = ModelFrame.of(data) {
      y | log(x) + sqrt(z) + exp(w)
    }.evaluate()

    assertModelFrameParity(expected, actual)
  }

  @Test
  @CompileDynamic
  void testDslModelFrameMatchesStringFormulaForIExpression() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'z'])
      .rows([
        [10.0, 1.0, 2.0],
        [20.0, 2.0, 3.0],
        [30.0, 3.0, 4.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult expected = ModelFrame.of('y ~ I((x + 1) * z)', data).evaluate()
    ModelFrameResult actual = ModelFrame.of(data) {
      y | I { (x + 1) * z }
    }.evaluate()

    assertModelFrameParity(expected, actual)
  }

  @Test
  @CompileDynamic
  void testDslModelFrameMatchesStringFormulaForPolynomialTerms() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([
        [10.0, 1.0],
        [20.0, 2.0],
        [30.0, 3.0],
      ])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult expected = ModelFrame.of('y ~ poly(x, 3)', data).evaluate()
    ModelFrameResult actual = ModelFrame.of(data) {
      y | poly(x, 3)
    }.evaluate()

    assertModelFrameParity(expected, actual)
  }

  @Test
  @CompileDynamic
  void testDslModelFrameMatchesStringFormulaForSmoothTerms() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'time'])
      .rows((1..12).collect { int value -> [value * 2.0, value as BigDecimal] })
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult expected = ModelFrame.of('y ~ s(time, 6)', data).evaluate()
    ModelFrameResult smoothActual = ModelFrame.of(data) {
      y | smooth(time, 6)
    }.evaluate()
    ModelFrameResult aliasActual = ModelFrame.of(data) {
      y | s(time, 6)
    }.evaluate()

    assertModelFrameParity(expected, smoothActual)
    assertModelFrameParity(expected, aliasActual)
  }

  @Test
  @CompileDynamic
  void testDslModelFramePreservesBuilderMethodsAndMatchesStringFormula() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'group', 'w', 'off', 'active', 'id'])
      .rows([
        [10.0, 1.0, 'A', 0.5, 0.1, true, 101.0],
        [20.0, null, 'B', 1.0, 0.2, true, 102.0],
        [30.0, 3.0, 'A', 1.5, 0.3, false, 103.0],
        [40.0, 4.0, 'B', 2.0, 0.4, true, 104.0],
      ])
      .types([BigDecimal, BigDecimal, String, BigDecimal, BigDecimal, Boolean, BigDecimal])
      .build()

    ModelFrame expected = ModelFrame.of('y ~ . - id', data)
      .weights('w')
      .offset('off')
      .subset { Row row -> row['active'] }
      .naAction(NaAction.OMIT)

    ModelFrame actual = ModelFrame.of(data) {
      y | all - id
    }
      .weights('w')
      .offset('off')
      .subset { Row row -> row['active'] }
      .naAction(NaAction.OMIT)

    assertModelFrameParity(expected.evaluate(), actual.evaluate())
  }

  @Test
  @CompileDynamic
  void testDslOwnerPropertyDoesNotOverrideFormulaColumnReference() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[10.0, 1.0], [20.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()
    Expando owner = new Expando(x: 'owner-value')
    Closure<GroovyFormulaSpec> formula = ({ col('y') | x } as Closure<GroovyFormulaSpec>).rehydrate(owner, owner, owner)

    ModelFrameResult expected = ModelFrame.of('`y` ~ x', data).evaluate()
    ModelFrameResult actual = ModelFrame.of(data, formula).evaluate()

    assertModelFrameParity(expected, actual)
  }

  @Test
  void testDslModelFrameNullDataThrowsClearMessage() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      ModelFrame.of((Matrix) null, { null } as Closure<GroovyFormulaSpec>)
    }

    assertEquals('data cannot be null', exception.message)
  }

  @Test
  void testDslModelFrameNullClosureThrowsClearMessage() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[1.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      ModelFrame.of(data, (Closure<GroovyFormulaSpec>) null)
    }

    assertEquals('Formula closure cannot be null', exception.message)
  }

  @Test
  @CompileDynamic
  void testDslModelFrameClosureReturningNonFormulaValueThrowsClearMessage() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'z'])
      .rows([[1.0, 2.0, 3.0]])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      ModelFrame.of(data) {
        x + z
      }
    }

    assertEquals('Formula closure must return a Groovy formula expression such as y | x + group', exception.message)
  }

  @Test
  void testDslModelFrameRejectsMissingResponseSpec() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[1.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    FormulaParseException exception = assertThrows(FormulaParseException) {
      ModelFrame.of(data) {
        new GroovyFormulaSpec(null, new TermRef('x'))
      }
    }

    assertEquals('Formula expression must define a response term such as y | x + group', exception.message)
  }

  @Test
  void testDslModelFrameRejectsMissingPredictorsSpec() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[1.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    FormulaParseException exception = assertThrows(FormulaParseException) {
      ModelFrame.of(data) {
        new GroovyFormulaSpec(new TermRef('y'), null)
      }
    }

    assertEquals('Formula expression must define predictor terms such as y | x + group', exception.message)
  }

  @Test
  @CompileDynamic
  void testDslModelFrameUnknownColumnMatchesStringPathError() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[1.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      ModelFrame.of(data) {
        y | x + z
      }.evaluate()
    }

    assertTrue(exception.message.contains('Unknown variable(s) in formula'))
    assertTrue(exception.message.contains('z'))
  }

  @Test
  @CompileDynamic
  void testDslModelFrameRejectsZeroArgumentInteraction() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[1.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      ModelFrame.of(data) {
        y | interaction()
      }
    }

    assertEquals('interaction(...) requires at least two terms', exception.message)
  }

  @Test
  @CompileDynamic
  void testDslModelFrameRejectsOneArgumentInteraction() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[1.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      ModelFrame.of(data) {
        y | interaction(x)
      }
    }

    assertEquals('interaction(...) requires at least two terms', exception.message)
  }

  @Test
  @CompileDynamic
  void testDslModelFrameRejectsTransformedResponseExplicitly() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[1.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    FormulaParseException exception = assertThrows(FormulaParseException) {
      ModelFrame.of(data) {
        log(y) | x
      }
    }

    assertEquals('Transformed responses are not supported: log(y) | x. Move transforms to the predictor side.', exception.message)
  }

  @Test
  @CompileDynamic
  void testDslModelFrameRejectsSmoothInteraction() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'z'])
      .rows([
        [3.0, 1.0, 2.0],
        [5.0, 2.0, 3.0],
        [7.0, 3.0, 4.0],
        [9.0, 4.0, 5.0],
        [11.0, 5.0, 6.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      ModelFrame.of(data) {
        y | smooth(x) % z
      }.evaluate()
    }

    assertTrue(exception.message.contains('Smooth terms cannot be used in interactions'))
    assertTrue(exception.message.contains('s(x):z'))
  }

  @Test
  @CompileDynamic
  void testDslModelFrameRejectsExpandedSmoothInteraction() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'z'])
      .rows([
        [3.0, 1.0, 2.0],
        [5.0, 2.0, 3.0],
        [7.0, 3.0, 4.0],
        [9.0, 4.0, 5.0],
        [11.0, 5.0, 6.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      ModelFrame.of(data) {
        y | (smooth(x) + z) ** 2
      }.evaluate()
    }

    assertTrue(exception.message.contains('Smooth terms cannot be used in interactions'))
    assertTrue(exception.message.contains('s(x):z'))
  }

  private static void assertModelFrameParity(ModelFrameResult expected, ModelFrameResult actual) {
    assertEquals(expected.responseName, actual.responseName)
    assertEquals(expected.response, actual.response)
    assertEquals(expected.includeIntercept, actual.includeIntercept)
    assertEquals(expected.predictorNames, actual.predictorNames)
    assertEquals(expected.weights, actual.weights)
    assertEquals(expected.offset, actual.offset)
    assertEquals(expected.droppedRows, actual.droppedRows)
    assertEquals(expected.formula.asFormulaString(), actual.formula.asFormulaString())
    assertEquals(expected.data.rowCount(), actual.data.rowCount())
    assertEquals(expected.data.columnCount(), actual.data.columnCount())

    expected.predictorNames.each { String columnName ->
      for (int row = 0; row < expected.data.rowCount(); row++) {
        assertTrue(expected.data[row, columnName] == actual.data[row, columnName])
      }
    }

    assertTermsParity(expected.terms, actual.terms)
  }

  private static void assertTermsParity(Terms expected, Terms actual) {
    assertEquals(expected.response.asFormulaString(), actual.response.asFormulaString())
    assertEquals(expected.includeIntercept, actual.includeIntercept)
    assertEquals(expected.terms.size(), actual.terms.size())

    for (int i = 0; i < expected.terms.size(); i++) {
      Terms.TermInfo expectedTerm = expected.terms[i]
      Terms.TermInfo actualTerm = actual.terms[i]
      assertEquals(expectedTerm.sourceTerm.asFormulaString(), actualTerm.sourceTerm.asFormulaString())
      assertEquals(expectedTerm.label, actualTerm.label)
      assertEquals(expectedTerm.columns, actualTerm.columns)
      assertEquals(expectedTerm.isCategorical, actualTerm.isCategorical)
      assertEquals(expectedTerm.factorLevels, actualTerm.factorLevels)
      assertEquals(expectedTerm.isSmooth, actualTerm.isSmooth)
      assertEquals(expectedTerm.isDropped, actualTerm.isDropped)
      assertEquals(expectedTerm.droppedReason, actualTerm.droppedReason)
    }
  }

}
