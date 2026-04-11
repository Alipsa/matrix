package formula

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.ext.NumberExtension
import se.alipsa.matrix.stats.formula.ContrastType
import se.alipsa.matrix.stats.formula.FormulaParseException
import se.alipsa.matrix.stats.formula.ModelFrame
import se.alipsa.matrix.stats.formula.ModelFrameResult
import se.alipsa.matrix.stats.formula.NaAction
import se.alipsa.matrix.stats.formula.Terms

/**
 * Tests for design matrix construction, expression evaluation, contrasts, and term metadata.
 */
@CompileStatic
@SuppressWarnings(['DuplicateNumberLiteral', 'DuplicateStringLiteral'])
class FormulaDesignMatrixTest {

  @Test
  void testLogTransform() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[1.0, 1.0], [2.0, NumberExtension.E]])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ log(x)', data).evaluate()

    assertEquals(['log_x'], result.predictorNames)
    assertEquals(0.0, result.data[0, 'log_x'] as BigDecimal)
    assertEquals(NumberExtension.E.log(), result.data[1, 'log_x'] as BigDecimal)
  }

  @Test
  void testSqrtTransform() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[1.0, 4.0], [2.0, 9.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ sqrt(x)', data).evaluate()

    assertEquals(['sqrt_x'], result.predictorNames)
    assertTrue((result.data[0, 'sqrt_x'] as BigDecimal) == 2.0)
    assertTrue((result.data[1, 'sqrt_x'] as BigDecimal) == 3.0)
  }

  @Test
  void testExpTransform() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[1.0, 0.0], [2.0, 1.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ exp(x)', data).evaluate()

    assertEquals(['exp_x'], result.predictorNames)
    assertEquals(1.0, result.data[0, 'exp_x'] as BigDecimal)
    assertEquals(1.0.exp(), result.data[1, 'exp_x'] as BigDecimal)
  }

  @Test
  void testIExpressionArithmetic() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[1.0, 1.0], [2.0, 2.0], [3.0, 3.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ I(x + 1)', data).evaluate()

    assertEquals(['I_x_plus_1'], result.predictorNames)
    assertTrue((result.data[0, 'I_x_plus_1'] as BigDecimal) == 2.0)
    assertTrue((result.data[1, 'I_x_plus_1'] as BigDecimal) == 3.0)
    assertTrue((result.data[2, 'I_x_plus_1'] as BigDecimal) == 4.0)
  }

  @Test
  void testIExpressionPower() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[1.0, 2.0], [2.0, 3.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ I(x^2)', data).evaluate()

    assertEquals(['I_x_pow_2'], result.predictorNames)
    assertTrue((result.data[0, 'I_x_pow_2'] as BigDecimal) == 4.0)
    assertTrue((result.data[1, 'I_x_pow_2'] as BigDecimal) == 9.0)
  }

  @Test
  void testIExpressionNested() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'z'])
      .rows([[1.0, 1.0, 2.0], [2.0, 2.0, 3.0]])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ I((x + 1) * z)', data).evaluate()

    assertEquals(['I_x_plus_1_times_z'], result.predictorNames)
    assertTrue((result.data[0, 'I_x_plus_1_times_z'] as BigDecimal) == 4.0)
    assertTrue((result.data[1, 'I_x_plus_1_times_z'] as BigDecimal) == 9.0)
  }

  @Test
  void testPolyExpansion() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[1.0, 2.0], [2.0, 3.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ poly(x, 3)', data).evaluate()

    assertEquals(['x_poly1', 'x_poly2', 'x_poly3'], result.predictorNames)
    assertTrue((result.data[0, 'x_poly1'] as BigDecimal) == 2.0)
    assertTrue((result.data[0, 'x_poly2'] as BigDecimal) == 4.0)
    assertTrue((result.data[0, 'x_poly3'] as BigDecimal) == 8.0)
    assertTrue((result.data[1, 'x_poly1'] as BigDecimal) == 3.0)
    assertTrue((result.data[1, 'x_poly2'] as BigDecimal) == 9.0)
    assertTrue((result.data[1, 'x_poly3'] as BigDecimal) == 27.0)
  }

  @Test
  void testPolyInInteraction() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'group'])
      .rows([
        [1.0, 2.0, 'A'],
        [2.0, 3.0, 'B'],
      ])
      .types([BigDecimal, BigDecimal, String])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ poly(x, 2):group', data).evaluate()

    // group_A is reference, so interaction with group_B only
    // Factors sorted alphabetically: group before poly(x, 2)
    assertEquals(['group_B:x_poly1', 'group_B:x_poly2'], result.predictorNames)
    assertTrue((result.data[0, 'group_B:x_poly1'] as BigDecimal) == 0.0)
    assertTrue((result.data[1, 'group_B:x_poly1'] as BigDecimal) == 3.0)
    assertTrue((result.data[1, 'group_B:x_poly2'] as BigDecimal) == 9.0)
  }

  @Test
  void testCategoricalTreatmentDefault() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'group'])
      .rows([
        [1.0, 'A'],
        [2.0, 'B'],
        [3.0, 'C'],
      ])
      .types([BigDecimal, String])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ group', data).evaluate()

    assertEquals(['group_B', 'group_C'], result.predictorNames)
    assertTrue((result.data[0, 'group_B'] as BigDecimal) == 0.0)
    assertTrue((result.data[1, 'group_B'] as BigDecimal) == 1.0)
    assertTrue((result.data[2, 'group_C'] as BigDecimal) == 1.0)
  }

  @Test
  void testCategoricalSumContrast() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'group'])
      .rows([
        [1.0, 'A'],
        [2.0, 'B'],
        [3.0, 'C'],
      ])
      .types([BigDecimal, String])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ group', data)
      .contrasts('group', ContrastType.SUM)
      .evaluate()

    // C is omitted (last alphabetically)
    assertEquals(['group_A', 'group_B'], result.predictorNames)
    assertTrue((result.data[0, 'group_A'] as BigDecimal) == 1.0)
    assertTrue((result.data[0, 'group_B'] as BigDecimal) == 0.0)
    // Row 2 is C -> omitted level gets -1
    assertTrue((result.data[2, 'group_A'] as BigDecimal) == -1.0)
    assertTrue((result.data[2, 'group_B'] as BigDecimal) == -1.0)
  }

  @Test
  void testCategoricalDeviationContrast() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'group'])
      .rows([
        [1.0, 'A'],
        [2.0, 'B'],
        [3.0, 'C'],
      ])
      .types([BigDecimal, String])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ group', data)
      .contrasts('group', ContrastType.DEVIATION)
      .evaluate()

    // C is omitted, gets -1/2
    assertEquals(['group_A', 'group_B'], result.predictorNames)
    assertTrue((result.data[0, 'group_A'] as BigDecimal) == 1.0)
    assertTrue((result.data[2, 'group_A'] as BigDecimal) == -0.5)
    assertTrue((result.data[2, 'group_B'] as BigDecimal) == -0.5)
  }

  @Test
  void testDefaultContrastWithOverride() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'g1', 'g2'])
      .rows([
        [1.0, 'A', 'X'],
        [2.0, 'B', 'Y'],
      ])
      .types([BigDecimal, String, String])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ g1 + g2', data)
      .defaultContrast(ContrastType.SUM)
      .contrasts('g1', ContrastType.TREATMENT)
      .evaluate()

    // g1 uses TREATMENT (override), g2 uses SUM (default)
    assertTrue(result.predictorNames.contains('g1_B'))
    assertTrue(result.predictorNames.contains('g2_X'))
    // g2 omitted level is Y (last), so g2_X has -1 for row 1 (Y)
    assertTrue((result.data[1, 'g2_X'] as BigDecimal) == -1.0)
  }

  @Test
  void testCategoricalNumericInteraction() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'group'])
      .rows([
        [1.0, 10.0, 'A'],
        [2.0, 20.0, 'B'],
        [3.0, 30.0, 'B'],
      ])
      .types([BigDecimal, BigDecimal, String])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ x:group', data).evaluate()

    // Factors are sorted alphabetically: group before x
    assertEquals(['group_B:x'], result.predictorNames)
    assertTrue((result.data[0, 'group_B:x'] as BigDecimal) == 0.0)
    assertTrue((result.data[1, 'group_B:x'] as BigDecimal) == 20.0)
    assertTrue((result.data[2, 'group_B:x'] as BigDecimal) == 30.0)
  }

  @Test
  void testCategoricalCategoricalInteraction() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'a', 'b'])
      .rows([
        [1.0, 'A1', 'B1'],
        [2.0, 'A2', 'B1'],
        [3.0, 'A1', 'B2'],
        // Keep the non-reference/non-reference combination so the interaction column
        // is exercised by a positive value rather than only zeros.
        [4.0, 'A2', 'B2'],
      ])
      .types([BigDecimal, String, String])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ a:b', data).evaluate()

    // a: A1(ref), A2; b: B1(ref), B2
    // Interaction columns: a_A2:b_B2 (only non-ref combination)
    assertEquals(['a_A2:b_B2'], result.predictorNames)
    assertTrue((result.data[0, 'a_A2:b_B2'] as BigDecimal) == 0.0)
    assertTrue((result.data[1, 'a_A2:b_B2'] as BigDecimal) == 0.0)
    assertTrue((result.data[2, 'a_A2:b_B2'] as BigDecimal) == 0.0)
    assertTrue((result.data[3, 'a_A2:b_B2'] as BigDecimal) == 1.0)
  }

  @Test
  void testNumericNumericInteraction() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'z'])
      .rows([
        [1.0, 2.0, 3.0],
        [2.0, 4.0, 5.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ x:z', data).evaluate()

    assertEquals(['x:z'], result.predictorNames)
    assertTrue((result.data[0, 'x:z'] as BigDecimal) == 6.0)
    assertTrue((result.data[1, 'x:z'] as BigDecimal) == 20.0)
  }

  @Test
  void testIExpressionWithCategoricalInteraction() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'species'])
      .rows([
        [1.0, 2.0, 'setosa'],
        [2.0, 3.0, 'versicolor'],
      ])
      .types([BigDecimal, BigDecimal, String])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ I(x^2):species', data).evaluate()

    // Factors sorted alphabetically: I(x^2) before species
    assertEquals(['I_x_pow_2:species_versicolor'], result.predictorNames)
    assertTrue((result.data[0, 'I_x_pow_2:species_versicolor'] as BigDecimal) == 0.0)
    assertTrue((result.data[1, 'I_x_pow_2:species_versicolor'] as BigDecimal) == 9.0)
  }

  @Test
  void testTransformedNumericWithCategoricalInteraction() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'group'])
      .rows([
        [1.0, 4.0, 'A'],
        [2.0, 9.0, 'B'],
      ])
      .types([BigDecimal, BigDecimal, String])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ log(x):group', data).evaluate()

    // Factors sorted alphabetically: group before log(x)
    assertEquals(['group_B:log_x'], result.predictorNames)
    assertTrue((result.data[0, 'group_B:log_x'] as BigDecimal) == 0.0)
    assertTrue((result.data[1, 'group_B:log_x'] as BigDecimal) == 9.0.log())
  }

  @Test
  void testIExpressionPlusOneWithCategoricalInteraction() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'group'])
      .rows([
        [1.0, 1.0, 'A'],
        [2.0, 2.0, 'B'],
      ])
      .types([BigDecimal, BigDecimal, String])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ I(x + 1):group', data).evaluate()

    // Factors sorted alphabetically: I(x + 1) before group
    assertEquals(['I_x_plus_1:group_B'], result.predictorNames)
    assertTrue((result.data[0, 'I_x_plus_1:group_B'] as BigDecimal) == 0.0)
    assertTrue((result.data[1, 'I_x_plus_1:group_B'] as BigDecimal) == 3.0)
  }

  @Test
  void testTermsMetadata() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'species'])
      .rows([
        [1.0, 10.0, 'setosa'],
        [2.0, 20.0, 'versicolor'],
        [3.0, 30.0, 'virginica'],
      ])
      .types([BigDecimal, BigDecimal, String])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ x + species', data).evaluate()

    assertNotNull(result.terms)
    assertEquals('y', result.terms.response.asFormulaString())
    assertTrue(result.terms.includeIntercept)
    assertEquals(2, result.terms.terms.size())

    Terms.TermInfo speciesTerm = result.terms.terms.find { Terms.TermInfo t -> t.label == 'species' }
    assertNotNull(speciesTerm)
    assertTrue(speciesTerm.isCategorical)
    assertEquals(['species_versicolor', 'species_virginica'], speciesTerm.columns)
    assertEquals(['setosa', 'versicolor', 'virginica'], speciesTerm.factorLevels)
    assertFalse(speciesTerm.isDropped)

    Terms.TermInfo xTerm = result.terms.terms.find { Terms.TermInfo t -> t.label == 'x' }
    assertNotNull(xTerm)
    assertFalse(xTerm.isCategorical)
    assertEquals(['x'], xTerm.columns)
    assertTrue(xTerm.factorLevels.isEmpty())
  }

  @Test
  void testTermsMetadataForInteraction() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'group'])
      .rows([
        [1.0, 10.0, 'A'],
        [2.0, 20.0, 'B'],
      ])
      .types([BigDecimal, BigDecimal, String])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ x:group', data).evaluate()

    Terms.TermInfo interactionTerm = result.terms.terms[0]
    assertEquals('group:x', interactionTerm.label)
    assertTrue(interactionTerm.isCategorical)
    assertEquals(['group_B:x'], interactionTerm.columns)
  }

  @Test
  void testTermsMetadataForDroppedTerm() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x', 'group'])
      .rows([
        [1.0, 10.0, 'A'],
        [2.0, 20.0, 'A'],
      ])
      .types([BigDecimal, String, String])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ x + group', data).evaluate()

    Terms.TermInfo groupTerm = result.terms.terms.find { Terms.TermInfo t -> t.label == 'group' }
    assertNotNull(groupTerm)
    assertTrue(groupTerm.isDropped)
    assertEquals('Term produced no columns', groupTerm.droppedReason)
    assertTrue(groupTerm.columns.isEmpty())
  }

  @Test
  void testSmoothTermMetadataAndColumnNames() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([
        [1.0, 1.0],
        [2.0, 2.0],
        [3.0, 3.0],
        [4.0, 4.0],
        [5.0, 5.0],
      ])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ s(x, 3)', data).evaluate()

    assertEquals(['s_x_1', 's_x_2', 's_x_3'], result.predictorNames)
    Terms.TermInfo smoothTerm = result.terms.terms.find { Terms.TermInfo t -> t.label == 's(x, 3)' }
    assertNotNull(smoothTerm)
    assertTrue(smoothTerm.isSmooth)
    assertFalse(smoothTerm.isCategorical)
    assertEquals(['s_x_1', 's_x_2', 's_x_3'], smoothTerm.columns)
  }

  @Test
  void testQuotedColumnNamesInDesignMatrix() {
    Matrix data = Matrix.builder()
      .columnNames(['y val', 'x val'])
      .rows([[1.0, 2.0], [3.0, 4.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('`y val` ~ `x val`', data).evaluate()

    assertEquals(['x val'], result.predictorNames)
    assertTrue((result.data[0, 'x val'] as BigDecimal) == 2.0)
  }

  @Test
  void testEnvironmentVariableInDesignMatrix() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[1.0, 1.0], [2.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    Map<String, List<?>> env = [z: [3.0, 4.0]]
    ModelFrameResult result = ModelFrame.of('y ~ x + z', data)
      .environment(env)
      .evaluate()

    assertEquals(['x', 'z'], result.predictorNames)
    assertTrue((result.data[0, 'z'] as BigDecimal) == 3.0)
    assertTrue((result.data[1, 'z'] as BigDecimal) == 4.0)
  }

  @Test
  void testNaOmitWithTransformedColumns() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([
        [1.0, 1.0],
        [2.0, null],
        [3.0, 3.0],
      ])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ log(x)', data)
      .naAction(NaAction.OMIT)
      .evaluate()

    assertEquals(2, result.data.rowCount())
    assertEquals([1], result.droppedRows)
    assertTrue((result.data[0, 'log_x'] as BigDecimal) == 0.0)
    assertTrue((result.data[1, 'log_x'] as BigDecimal) == 3.0.log())
  }

  @Test
  void testSubsetWithPolyExpansion() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([
        [1.0, 1.0],
        [2.0, 2.0],
        [3.0, 3.0],
      ])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ poly(x, 2)', data)
      .subset([false, true, true])
      .evaluate()

    assertEquals(2, result.data.rowCount())
    assertEquals(['x_poly1', 'x_poly2'], result.predictorNames)
    assertTrue((result.data[0, 'x_poly1'] as BigDecimal) == 2.0)
    assertTrue((result.data[0, 'x_poly2'] as BigDecimal) == 4.0)
  }

  @Test
  void testContrastUnknownColumnThrows() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[1.0, 1.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ x', data)
        .contrasts('unknown', ContrastType.SUM)
        .evaluate()
    }
    assertTrue(ex.message.contains("Contrast configured for unknown column 'unknown'"))
    assertTrue(ex.message.contains('Available columns'))
  }

  @Test
  void testContrastOnNumericColumnThrows() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[1.0, 1.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ x', data)
        .contrasts('x', ContrastType.SUM)
        .evaluate()
    }
    assertTrue(ex.message.contains("Contrast only applies to categorical columns, 'x' is numeric"))
  }

  @Test
  void testPolyDegreeMustBePositiveInteger() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[1.0, 1.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      ModelFrame.of('y ~ poly(x, 0)', data).evaluate()
    }
    assertTrue(ex.message.contains('poly() degree must be a positive integer'))
  }

  @Test
  void testTermsMetadataForPoly() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[1.0, 2.0], [2.0, 3.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ poly(x, 2)', data).evaluate()

    Terms.TermInfo polyTerm = result.terms.terms[0]
    assertEquals('poly(x, 2)', polyTerm.label)
    assertFalse(polyTerm.isCategorical)
    assertEquals(['x_poly1', 'x_poly2'], polyTerm.columns)
  }

  @Test
  void testBooleanCategoricalWithTerms() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'active'])
      .rows([
        [1.0, false],
        [2.0, true],
      ])
      .types([BigDecimal, Boolean])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ active', data).evaluate()

    Terms.TermInfo activeTerm = result.terms.terms[0]
    assertTrue(activeTerm.isCategorical)
    assertEquals(['active_true'], activeTerm.columns)
    assertEquals(['false', 'true'], activeTerm.factorLevels)
  }

  @Test
  void testTransformedResponseThrows() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([[1.0, 1.0], [2.0, 2.0]])
      .types([BigDecimal, BigDecimal])
      .build()

    FormulaParseException ex = assertThrows(FormulaParseException) {
      ModelFrame.of('log(y) ~ x', data).evaluate()
    }
    assertEquals('Transformed responses are not supported: log(y) ~ x. Move transforms to the predictor side.', ex.message)
  }

  @Test
  void testEnvironmentVariableAlignedAfterSubset() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([
        [1.0, 1.0],
        [2.0, 2.0],
        [3.0, 3.0],
      ])
      .types([BigDecimal, BigDecimal])
      .build()

    Map<String, List<?>> env = [z: [10.0, 20.0, 30.0]]
    ModelFrameResult result = ModelFrame.of('y ~ x + z', data)
      .environment(env)
      .subset([false, true, true])
      .evaluate()

    assertEquals(2, result.data.rowCount())
    assertTrue((result.data[0, 'z'] as BigDecimal) == 20.0)
    assertTrue((result.data[1, 'z'] as BigDecimal) == 30.0)
  }

  @Test
  void testEnvironmentVariableAlignedAfterNaOmit() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'x'])
      .rows([
        [1.0, 1.0],
        [null, 2.0],
        [3.0, 3.0],
      ])
      .types([BigDecimal, BigDecimal])
      .build()

    Map<String, List<?>> env = [z: [10.0, 20.0, 30.0]]
    ModelFrameResult result = ModelFrame.of('y ~ x + z', data)
      .environment(env)
      .naAction(NaAction.OMIT)
      .evaluate()

    assertEquals(2, result.data.rowCount())
    assertEquals([1], result.droppedRows)
    assertTrue((result.data[0, 'z'] as BigDecimal) == 10.0)
    assertTrue((result.data[1, 'z'] as BigDecimal) == 30.0)
  }

  @Test
  void testEmptyPredictorMatrix() {
    Matrix data = Matrix.builder()
      .columnNames(['y', 'group'])
      .rows([
        [1.0, 'A'],
        [2.0, 'A'],
      ])
      .types([BigDecimal, String])
      .build()

    ModelFrameResult result = ModelFrame.of('y ~ group', data).evaluate()

    assertTrue(result.predictorNames.isEmpty())
    assertEquals(0, result.data.columnCount())
    assertEquals(2, result.data.rowCount())
    assertEquals(2, result.response.size())
  }

}
