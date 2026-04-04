# Model Frame Evaluation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a `ModelFrame` pipeline that evaluates parsed formulas against Matrix data, producing expanded design matrices with categorical encoding, NA handling, subsetting, weights, and offset support.

**Architecture:** Single-pass pipeline in `ModelFrame` class with builder API. Parses formula, expands dots using data columns, normalizes, validates variables, applies subset/NA/encoding, and returns `ModelFrameResult` wrapping a `Matrix` with metadata. All classes in `se.alipsa.matrix.stats.formula` package.

**Tech Stack:** Groovy 5.0.3, JDK 21, `@CompileStatic`, JUnit Jupiter, matrix-core `Matrix`/`MatrixBuilder` API.

**Spec:** `docs/superpowers/specs/2026-03-30-model-frame-design.md`

---

## File Structure

| File | Responsibility |
|---|---|
| **Create:** `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/formula/NaAction.groovy` | Enum for NA handling policy |
| **Create:** `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/formula/ModelFrameResult.groovy` | Immutable result holding design matrix + metadata |
| **Create:** `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/formula/ModelFrame.groovy` | Builder + pipeline evaluator |
| **Modify:** `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/formula/FormulaSupport.groovy` | Add `expandDots()` method |
| **Create:** `matrix-stats/src/test/groovy/formula/FormulaModelFrameTest.groovy` | All tests |

---

### Task 1: NaAction Enum and ModelFrameResult

**Files:**
- Create: `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/formula/NaAction.groovy`
- Create: `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/formula/ModelFrameResult.groovy`
- Test: `matrix-stats/src/test/groovy/formula/FormulaModelFrameTest.groovy`

- [ ] **Step 1: Write the failing test for ModelFrameResult**

Create the test file with the first test verifying that `ModelFrameResult` holds and returns all fields correctly.

```groovy
// matrix-stats/src/test/groovy/formula/FormulaModelFrameTest.groovy
package formula

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertTrue
import static org.junit.jupiter.api.Assertions.assertThrows

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
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
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :matrix-stats:test --tests 'formula.FormulaModelFrameTest' -g ./.gradle-user`
Expected: Compilation failure — `NaAction`, `ModelFrameResult`, `ModelFrame` do not exist yet.

- [ ] **Step 3: Create NaAction enum**

```groovy
// matrix-stats/src/main/groovy/se/alipsa/matrix/stats/formula/NaAction.groovy
package se.alipsa.matrix.stats.formula

import groovy.transform.CompileStatic

/**
 * Policy for handling NA (null) values during model frame evaluation.
 */
@CompileStatic
enum NaAction {

  /** Drop rows containing null values in any formula-referenced column. */
  OMIT,

  /** Throw an exception if any formula-referenced column contains null values. */
  FAIL
}
```

- [ ] **Step 4: Create ModelFrameResult**

```groovy
// matrix-stats/src/main/groovy/se/alipsa/matrix/stats/formula/ModelFrameResult.groovy
package se.alipsa.matrix.stats.formula

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix

/**
 * Immutable result of model frame evaluation, containing the expanded design matrix
 * and associated metadata.
 */
@CompileStatic
final class ModelFrameResult {

  final Matrix data
  final List<Number> response
  final String responseName
  final boolean includeIntercept
  final List<String> predictorNames
  final List<Number> weights
  final List<Number> offset
  final List<Integer> droppedRows
  final NormalizedFormula formula

  /**
   * Creates a model frame result.
   *
   * @param data expanded design matrix containing only predictor columns
   * @param response response column values
   * @param responseName name of the response column
   * @param includeIntercept whether the intercept should be included
   * @param predictorNames column names in the design matrix
   * @param weights weight values aligned to surviving rows, or null
   * @param offset offset values aligned to surviving rows, or null
   * @param droppedRows original row indices removed by subset and NA handling
   * @param formula the fully resolved normalized formula
   */
  ModelFrameResult(
    Matrix data,
    List<Number> response,
    String responseName,
    boolean includeIntercept,
    List<String> predictorNames,
    List<Number> weights,
    List<Number> offset,
    List<Integer> droppedRows,
    NormalizedFormula formula
  ) {
    this.data = requireNonNull(data, 'data')
    this.response = List.copyOf(requireNonNull(response, 'response'))
    this.responseName = requireNonBlank(responseName, 'responseName')
    this.includeIntercept = includeIntercept
    this.predictorNames = List.copyOf(requireNonNull(predictorNames, 'predictorNames'))
    this.weights = weights != null ? List.copyOf(weights) : null
    this.offset = offset != null ? List.copyOf(offset) : null
    this.droppedRows = List.copyOf(requireNonNull(droppedRows, 'droppedRows'))
    this.formula = requireNonNull(formula, 'formula')
  }

  private static <T> T requireNonNull(T value, String label) {
    if (value == null) {
      throw new IllegalArgumentException("${label} cannot be null")
    }
    value
  }

  private static String requireNonBlank(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("${label} cannot be null or blank")
    }
    value
  }
}
```

- [ ] **Step 5: Create minimal ModelFrame stub**

Create a minimal stub so the test file compiles. The `evaluate()` method will be implemented in later tasks.

```groovy
// matrix-stats/src/main/groovy/se/alipsa/matrix/stats/formula/ModelFrame.groovy
package se.alipsa.matrix.stats.formula

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix

/**
 * Evaluates a formula against a Matrix dataset to produce an expanded design matrix.
 *
 * <p>Use the builder API to configure the evaluation:
 * <pre>
 * ModelFrameResult result = ModelFrame.of('y ~ x + z', data)
 *   .weights('w')
 *   .naAction(NaAction.OMIT)
 *   .evaluate()
 * </pre>
 */
@CompileStatic
final class ModelFrame {

  private ModelFrame() {
  }

  /**
   * Creates a model frame builder from a formula string and data.
   *
   * @param formula the formula source, for example {@code y ~ x + z}
   * @param data the input dataset
   * @return a new builder
   */
  static ModelFrame of(String formula, Matrix data) {
    throw new UnsupportedOperationException('Not yet implemented')
  }

  /**
   * Creates a model frame builder from a pre-normalized formula and data.
   *
   * @param formula the normalized formula
   * @param data the input dataset
   * @return a new builder
   */
  static ModelFrame of(NormalizedFormula formula, Matrix data) {
    throw new UnsupportedOperationException('Not yet implemented')
  }

  /**
   * Evaluates the formula against the data and returns the result.
   *
   * @return the model frame result
   */
  ModelFrameResult evaluate() {
    throw new UnsupportedOperationException('Not yet implemented')
  }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :matrix-stats:test --tests 'formula.FormulaModelFrameTest' -g ./.gradle-user`
Expected: PASS — both tests exercise `ModelFrameResult` constructor and field access.

- [ ] **Step 7: Run Spotless and CodeNarc**

Run: `./gradlew :matrix-stats:spotlessApply :matrix-stats:codenarcMain :matrix-stats:codenarcTest -g ./.gradle-user`
Expected: PASS with no new violations.

- [ ] **Step 8: Commit**

```bash
git add matrix-stats/src/main/groovy/se/alipsa/matrix/stats/formula/NaAction.groovy \
  matrix-stats/src/main/groovy/se/alipsa/matrix/stats/formula/ModelFrameResult.groovy \
  matrix-stats/src/main/groovy/se/alipsa/matrix/stats/formula/ModelFrame.groovy \
  matrix-stats/src/test/groovy/formula/FormulaModelFrameTest.groovy
git commit -m "Add NaAction, ModelFrameResult, and ModelFrame stub"
```

---

### Task 2: FormulaSupport.expandDots() and Simple Formula Evaluation

**Files:**
- Modify: `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/formula/FormulaSupport.groovy`
- Modify: `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/formula/ModelFrame.groovy`
- Modify: `matrix-stats/src/test/groovy/formula/FormulaModelFrameTest.groovy`

- [ ] **Step 1: Write failing tests for simple formula and dot expansion**

Add these tests to `FormulaModelFrameTest`:

```groovy
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :matrix-stats:test --tests 'formula.FormulaModelFrameTest' -g ./.gradle-user`
Expected: FAIL — `ModelFrame.of()` throws `UnsupportedOperationException`.

- [ ] **Step 3: Add expandDots() to FormulaSupport**

Add this method to `FormulaSupport.groovy` after the existing `replaceDots()` method (after line 133):

```groovy
static FormulaExpression expandDots(FormulaExpression expression, List<String> dotColumns) {
  if (dotColumns.isEmpty()) {
    return expression
  }
  if (expression instanceof FormulaExpression.Dot) {
    return buildAdditionChain(dotColumns, expression.start, expression.end)
  }
  if (expression instanceof FormulaExpression.Grouping) {
    FormulaExpression.Grouping grouping = expression as FormulaExpression.Grouping
    FormulaExpression expanded = expandDots(grouping.expression, dotColumns)
    if (expanded.is(grouping.expression)) {
      return expression
    }
    return new FormulaExpression.Grouping(expanded, grouping.start, grouping.end)
  }
  if (expression instanceof FormulaExpression.Unary) {
    FormulaExpression.Unary unary = expression as FormulaExpression.Unary
    FormulaExpression expanded = expandDots(unary.expression, dotColumns)
    if (expanded.is(unary.expression)) {
      return expression
    }
    return new FormulaExpression.Unary(unary.operator, expanded, unary.start, unary.end)
  }
  if (expression instanceof FormulaExpression.Binary) {
    FormulaExpression.Binary binary = expression as FormulaExpression.Binary
    FormulaExpression expandedLeft = expandDots(binary.left, dotColumns)
    FormulaExpression expandedRight = expandDots(binary.right, dotColumns)
    if (expandedLeft.is(binary.left) && expandedRight.is(binary.right)) {
      return expression
    }
    return new FormulaExpression.Binary(binary.operator, expandedLeft, expandedRight, binary.start, binary.end)
  }
  if (expression instanceof FormulaExpression.FunctionCall) {
    FormulaExpression.FunctionCall call = expression as FormulaExpression.FunctionCall
    List<FormulaExpression> expandedArgs = call.arguments.collect { FormulaExpression arg ->
      expandDots(arg, dotColumns)
    }
    boolean changed = false
    for (int i = 0; i < expandedArgs.size(); i++) {
      if (!expandedArgs[i].is(call.arguments[i])) {
        changed = true
        break
      }
    }
    if (!changed) {
      return expression
    }
    return new FormulaExpression.FunctionCall(call.name, expandedArgs, call.start, call.end)
  }
  expression
}

private static FormulaExpression buildAdditionChain(List<String> names, int start, int end) {
  FormulaExpression result = new FormulaExpression.Variable(names[0], false, start, end)
  for (int i = 1; i < names.size(); i++) {
    FormulaExpression right = new FormulaExpression.Variable(names[i], false, start, end)
    result = new FormulaExpression.Binary('+', result, right, start, end)
  }
  result
}
```

- [ ] **Step 4: Implement ModelFrame core pipeline (String path)**

Replace the full content of `ModelFrame.groovy` with the working implementation for numeric formulas, dot expansion, and basic evaluation:

```groovy
// matrix-stats/src/main/groovy/se/alipsa/matrix/stats/formula/ModelFrame.groovy
package se.alipsa.matrix.stats.formula

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.core.util.Logger

/**
 * Evaluates a formula against a Matrix dataset to produce an expanded design matrix.
 *
 * <p>Use the builder API to configure the evaluation:
 * <pre>
 * ModelFrameResult result = ModelFrame.of('y ~ x + z', data)
 *   .weights('w')
 *   .naAction(NaAction.OMIT)
 *   .evaluate()
 * </pre>
 */
@CompileStatic
@SuppressWarnings('DuplicateStringLiteral')
final class ModelFrame {

  private static final Logger log = Logger.getLogger(ModelFrame)

  private final ParsedFormula parsedFormula
  private final NormalizedFormula preNormalized
  private final Matrix data
  private List<? extends Number> weightsList
  private String weightsColumn
  private List<? extends Number> offsetList
  private String offsetColumn
  private Closure<Boolean> subsetClosure
  private List<Boolean> subsetMask
  private NaAction naActionPolicy = NaAction.OMIT
  private Map<String, List<?>> env

  private ModelFrame(ParsedFormula parsedFormula, NormalizedFormula preNormalized, Matrix data) {
    this.parsedFormula = parsedFormula
    this.preNormalized = preNormalized
    this.data = requireNonNull(data, 'data')
  }

  /**
   * Creates a model frame builder from a formula string and data.
   *
   * @param formula the formula source, for example {@code y ~ x + z}
   * @param data the input dataset
   * @return a new builder
   */
  static ModelFrame of(String formula, Matrix data) {
    if (formula == null || formula.isBlank()) {
      throw new IllegalArgumentException('formula cannot be null or blank')
    }
    ParsedFormula parsed = Formula.parse(formula)
    new ModelFrame(parsed, null, data)
  }

  /**
   * Creates a model frame builder from a pre-normalized formula and data.
   *
   * @param formula the normalized formula
   * @param data the input dataset
   * @return a new builder
   */
  static ModelFrame of(NormalizedFormula formula, Matrix data) {
    requireNonNull(formula, 'formula')
    new ModelFrame(null, formula, data)
  }

  /**
   * Sets weights from a numeric list aligned to data rows.
   *
   * @param weights the weight values
   * @return this builder
   */
  ModelFrame weights(List<? extends Number> weights) {
    this.weightsList = weights
    this.weightsColumn = null
    this
  }

  /**
   * Sets weights from a named column in the data.
   *
   * @param columnName the weights column name
   * @return this builder
   */
  ModelFrame weights(String columnName) {
    this.weightsColumn = columnName
    this.weightsList = null
    this
  }

  /**
   * Sets offset from a numeric list aligned to data rows.
   *
   * @param offset the offset values
   * @return this builder
   */
  ModelFrame offset(List<? extends Number> offset) {
    this.offsetList = offset
    this.offsetColumn = null
    this
  }

  /**
   * Sets offset from a named column in the data.
   *
   * @param columnName the offset column name
   * @return this builder
   */
  ModelFrame offset(String columnName) {
    this.offsetColumn = columnName
    this.offsetList = null
    this
  }

  /**
   * Sets the row subset filter as a closure.
   *
   * @param filter closure receiving a Row and returning true to include
   * @return this builder
   */
  ModelFrame subset(Closure<Boolean> filter) {
    this.subsetClosure = filter
    this.subsetMask = null
    this
  }

  /**
   * Sets the row subset filter as a boolean mask.
   *
   * @param mask boolean list aligned to data rows
   * @return this builder
   */
  ModelFrame subset(List<Boolean> mask) {
    this.subsetMask = mask
    this.subsetClosure = null
    this
  }

  /**
   * Sets the NA handling policy.
   *
   * @param action the NA action (default is OMIT)
   * @return this builder
   */
  ModelFrame naAction(NaAction action) {
    this.naActionPolicy = requireNonNull(action, 'naAction')
    this
  }

  /**
   * Sets an optional environment for resolving variables not found in data.
   *
   * @param env map of variable name to column values
   * @return this builder
   */
  ModelFrame environment(Map<String, List<?>> env) {
    this.env = env
    this
  }

  /**
   * Evaluates the formula against the data and returns the result.
   *
   * @return the model frame result
   */
  ModelFrameResult evaluate() {
    // Stage 1: Determine response name and collect excluded columns
    Set<String> excludeFromDot = [] as Set<String>
    if (weightsColumn != null) {
      validateColumnExists(weightsColumn, 'Weights')
      excludeFromDot << weightsColumn
    }
    if (offsetColumn != null) {
      validateColumnExists(offsetColumn, 'Offset')
      excludeFromDot << offsetColumn
    }

    NormalizedFormula normalized
    if (parsedFormula != null) {
      normalized = resolveFromParsed(parsedFormula, excludeFromDot)
    } else {
      normalized = resolveFromNormalized(preNormalized, excludeFromDot)
    }

    // Stage 4: Validate response
    String responseName = normalized.response.asFormulaString()
    validateResponseColumn(responseName)

    // Stage 4: Validate all predictor variables
    List<String> variableNames = collectVariableNames(normalized)
    validateVariables(variableNames)

    // Extract weights/offset from data columns
    List<Number> resolvedWeights = resolveWeights()
    List<Number> resolvedOffset = resolveOffset()

    // Stage 5: Subset
    List<Integer> originalIndices = (0..<data.rowCount()).toList()
    Matrix working = data
    SubsetResult subsetResult = applySubset(working, originalIndices)
    working = subsetResult.data
    originalIndices = subsetResult.indices

    // Stage 6: NA handling
    List<String> allFormulaColumns = [responseName]
    allFormulaColumns.addAll(variableNames)
    NaResult naResult = handleNa(working, originalIndices, allFormulaColumns)
    working = naResult.data
    List<Integer> droppedRows = naResult.droppedRows

    // Filter weights/offset to surviving original row indices
    resolvedWeights = filterToSurvivors(resolvedWeights, naResult.survivingIndices)
    resolvedOffset = filterToSurvivors(resolvedOffset, naResult.survivingIndices)

    // Validate non-empty
    if (working.rowCount() == 0) {
      throw new IllegalArgumentException('No observations remaining after subset and NA removal')
    }

    // Stage 7: Categorical encoding + build predictor matrix
    List<String> predictorNames = []
    List<List<BigDecimal>> predictorColumns = []
    for (FormulaTerm term : normalized.predictorTerms) {
      encodeTerm(term, working, predictorNames, predictorColumns)
    }

    // Stage 8: Build result
    List<Number> responseValues = extractResponseColumn(working, responseName)
    Matrix designMatrix = buildDesignMatrix(predictorNames, predictorColumns, working.rowCount())

    new ModelFrameResult(
      designMatrix, responseValues, responseName, normalized.includeIntercept,
      predictorNames, resolvedWeights, resolvedOffset, droppedRows, normalized
    )
  }

  // --- Stage 2/3: Dot expansion and normalization ---

  private NormalizedFormula resolveFromParsed(ParsedFormula parsed, Set<String> excludeFromDot) {
    String responseName = parsed.response.asFormulaString()
    List<String> dotColumns = computeDotColumns(responseName, parsed.predictors, excludeFromDot)
    FormulaExpression expandedPredictors = FormulaSupport.expandDots(parsed.predictors, dotColumns)
    FormulaExpression expandedResponse = parsed.response
    ParsedFormula expanded = new ParsedFormula(parsed.source, expandedResponse, expandedPredictors)
    FormulaSupport.normalize(expanded)
  }

  private NormalizedFormula resolveFromNormalized(NormalizedFormula normalized, Set<String> excludeFromDot) {
    boolean hasDots = normalized.predictorTerms.any { FormulaTerm term ->
      term.factors.any { FormulaExpression factor -> factor instanceof FormulaExpression.Dot }
    }
    if (!hasDots) {
      return normalized
    }

    String responseName = normalized.response.asFormulaString()
    Set<String> explicit = collectExplicitNames(normalized)
    List<String> dotColumns = data.columnNames().findAll { String col ->
      col != responseName && !explicit.contains(col) && !excludeFromDot.contains(col)
    }

    List<FormulaTerm> expandedTerms = []
    for (FormulaTerm term : normalized.predictorTerms) {
      expandedTerms.addAll(expandDotTerm(term, dotColumns))
    }

    List<FormulaTerm> sorted = expandedTerms.toList().sort { FormulaTerm a, FormulaTerm b ->
      int factorOrder = a.factors.size() <=> b.factors.size()
      factorOrder != 0 ? factorOrder : a.asFormulaString() <=> b.asFormulaString()
    } as List<FormulaTerm>

    new NormalizedFormula(normalized.response, normalized.includeIntercept, sorted)
  }

  private List<FormulaTerm> expandDotTerm(FormulaTerm term, List<String> dotColumns) {
    List<FormulaExpression> dotFactors = []
    List<FormulaExpression> nonDotFactors = []
    for (FormulaExpression factor : term.factors) {
      if (factor instanceof FormulaExpression.Dot) {
        dotFactors << factor
      } else {
        nonDotFactors << factor
      }
    }

    if (dotFactors.isEmpty()) {
      return [term]
    }

    // Each dot factor expands to all dot columns
    List<FormulaTerm> result = []
    List<List<FormulaExpression>> expanded = dotFactors.collect { FormulaExpression ignored ->
      dotColumns.collect { String col ->
        (FormulaExpression) new FormulaExpression.Variable(col, false, 0, 0)
      }
    }

    // Cartesian product of expanded dot factors
    List<List<FormulaExpression>> combos = cartesianProduct(expanded)
    for (List<FormulaExpression> combo : combos) {
      List<FormulaExpression> allFactors = []
      allFactors.addAll(nonDotFactors)
      allFactors.addAll(combo)
      result << new FormulaTerm(allFactors)
    }
    result
  }

  private static List<List<FormulaExpression>> cartesianProduct(List<List<FormulaExpression>> lists) {
    if (lists.isEmpty()) {
      return [[]]
    }
    List<List<FormulaExpression>> result = [[]]
    for (List<FormulaExpression> list : lists) {
      List<List<FormulaExpression>> newResult = []
      for (List<FormulaExpression> existing : result) {
        for (FormulaExpression item : list) {
          List<FormulaExpression> combo = [*existing]
          combo << item
          newResult << combo
        }
      }
      result = newResult
    }
    result
  }

  private List<String> computeDotColumns(String responseName, FormulaExpression predictors, Set<String> excludeFromDot) {
    Set<String> explicit = [] as Set<String>
    collectExplicitNamesFromExpression(predictors, explicit)
    data.columnNames().findAll { String col ->
      col != responseName && !explicit.contains(col) && !excludeFromDot.contains(col)
    }
  }

  private static void collectExplicitNamesFromExpression(FormulaExpression expression, Set<String> names) {
    if (expression instanceof FormulaExpression.Variable) {
      names << (expression as FormulaExpression.Variable).name
    } else if (expression instanceof FormulaExpression.Binary) {
      FormulaExpression.Binary binary = expression as FormulaExpression.Binary
      collectExplicitNamesFromExpression(binary.left, names)
      collectExplicitNamesFromExpression(binary.right, names)
    } else if (expression instanceof FormulaExpression.Unary) {
      collectExplicitNamesFromExpression((expression as FormulaExpression.Unary).expression, names)
    } else if (expression instanceof FormulaExpression.Grouping) {
      collectExplicitNamesFromExpression((expression as FormulaExpression.Grouping).expression, names)
    } else if (expression instanceof FormulaExpression.FunctionCall) {
      for (FormulaExpression arg : (expression as FormulaExpression.FunctionCall).arguments) {
        collectExplicitNamesFromExpression(arg, names)
      }
    }
  }

  private static Set<String> collectExplicitNames(NormalizedFormula normalized) {
    Set<String> names = [] as Set<String>
    for (FormulaTerm term : normalized.predictorTerms) {
      for (FormulaExpression factor : term.factors) {
        if (factor instanceof FormulaExpression.Variable) {
          names << (factor as FormulaExpression.Variable).name
        }
      }
    }
    names
  }

  // --- Stage 4: Variable resolution ---

  private List<String> collectVariableNames(NormalizedFormula normalized) {
    Set<String> names = [] as LinkedHashSet<String>
    for (FormulaTerm term : normalized.predictorTerms) {
      for (FormulaExpression factor : term.factors) {
        collectVariableNamesFromExpression(factor, names)
      }
    }
    names.toList()
  }

  private static void collectVariableNamesFromExpression(FormulaExpression expression, Set<String> names) {
    if (expression instanceof FormulaExpression.Variable) {
      names << (expression as FormulaExpression.Variable).name
    } else if (expression instanceof FormulaExpression.FunctionCall) {
      for (FormulaExpression arg : (expression as FormulaExpression.FunctionCall).arguments) {
        collectVariableNamesFromExpression(arg, names)
      }
    }
  }

  private void validateResponseColumn(String responseName) {
    if (data.columnIndex(responseName) < 0) {
      throw new IllegalArgumentException("Response variable '${responseName}' not found in data")
    }
    Class responseType = data.type(responseName)
    if (!Number.isAssignableFrom(responseType)) {
      throw new IllegalArgumentException(
        "Response column '${responseName}' must be numeric, found ${responseType.simpleName}"
      )
    }
  }

  private void validateVariables(List<String> variableNames) {
    List<String> dataColumns = data.columnNames()
    List<String> missing = variableNames.findAll { String name ->
      !dataColumns.contains(name) && (env == null || !env.containsKey(name))
    }
    if (!missing.isEmpty()) {
      throw new IllegalArgumentException(
        "Unknown variable(s) in formula: ${missing}. Available columns: ${dataColumns}"
      )
    }
  }

  private void validateColumnExists(String columnName, String label) {
    if (data.columnIndex(columnName) < 0) {
      throw new IllegalArgumentException("${label} column '${columnName}' not found in data")
    }
  }

  // --- Stage 5: Subset ---

  private SubsetResult applySubset(Matrix working, List<Integer> originalIndices) {
    if (subsetClosure != null) {
      List<Integer> kept = []
      List<Integer> keptOriginal = []
      for (int i = 0; i < working.rowCount(); i++) {
        if (subsetClosure.call(working.row(i))) {
          kept << i
          keptOriginal << originalIndices[i]
        }
      }
      Matrix subsetted = working.subset(kept)
      return new SubsetResult(subsetted, keptOriginal)
    }
    if (subsetMask != null) {
      if (subsetMask.size() != working.rowCount()) {
        throw new IllegalArgumentException(
          "Subset mask size (${subsetMask.size()}) does not match data row count (${working.rowCount()})"
        )
      }
      List<Integer> kept = []
      List<Integer> keptOriginal = []
      for (int i = 0; i < subsetMask.size(); i++) {
        if (subsetMask[i]) {
          kept << i
          keptOriginal << originalIndices[i]
        }
      }
      Matrix subsetted = working.subset(kept)
      return new SubsetResult(subsetted, keptOriginal)
    }
    new SubsetResult(working, originalIndices)
  }

  // --- Stage 6: NA handling ---

  private NaResult handleNa(Matrix working, List<Integer> currentIndices, List<String> columns) {
    List<String> columnsWithNulls = []
    Set<Integer> rowsWithNulls = [] as Set<Integer>

    for (String col : columns) {
      if (data.columnIndex(col) < 0) {
        continue // environment variable, not in data
      }
      for (int i = 0; i < working.rowCount(); i++) {
        if (working[i, col] == null) {
          columnsWithNulls << col
          rowsWithNulls << i
          break
        }
      }
    }

    if (rowsWithNulls.isEmpty()) {
      return new NaResult(working, [], currentIndices)
    }

    if (naActionPolicy == NaAction.FAIL) {
      List<String> uniqueColumns = columnsWithNulls.unique() as List<String>
      throw new IllegalArgumentException(
        "NA values found in columns: ${uniqueColumns} (na.action is FAIL)"
      )
    }

    // OMIT: find all rows with nulls in any formula column
    Set<Integer> allNullRows = [] as Set<Integer>
    for (String col : columns) {
      if (data.columnIndex(col) < 0) {
        continue
      }
      for (int i = 0; i < working.rowCount(); i++) {
        if (working[i, col] == null) {
          allNullRows << i
        }
      }
    }

    List<Integer> keptRows = []
    List<Integer> survivingIndices = []
    List<Integer> droppedOriginal = []
    for (int i = 0; i < working.rowCount(); i++) {
      if (allNullRows.contains(i)) {
        droppedOriginal << currentIndices[i]
      } else {
        keptRows << i
        survivingIndices << currentIndices[i]
      }
    }

    Matrix cleaned = working.subset(keptRows)
    new NaResult(cleaned, droppedOriginal, survivingIndices)
  }

  // --- Stage 7: Categorical encoding ---

  private void encodeTerm(FormulaTerm term, Matrix working,
                          List<String> outNames, List<List<BigDecimal>> outColumns) {
    if (term.isInteraction()) {
      encodeInteraction(term, working, outNames, outColumns)
    } else {
      encodeSingleFactor(term.factors[0], working, outNames, outColumns)
    }
  }

  private void encodeSingleFactor(FormulaExpression factor, Matrix working,
                                  List<String> outNames, List<List<BigDecimal>> outColumns) {
    String name = factor.asFormulaString()
    int colIdx = working.columnIndex(name)

    if (colIdx < 0 && env != null && env.containsKey(name)) {
      // Environment variable — treat as numeric column
      List<?> envValues = env[name]
      outNames << name
      outColumns << envValues.collect { Object val -> val as BigDecimal }
      return
    }

    Class colType = working.type(name)
    if (Number.isAssignableFrom(colType)) {
      outNames << name
      outColumns << (0..<working.rowCount()).collect { int i ->
        working[i, name] as BigDecimal
      }
    } else {
      encodeCategorical(name, working, outNames, outColumns)
    }
  }

  private void encodeCategorical(String columnName, Matrix working,
                                 List<String> outNames, List<List<BigDecimal>> outColumns) {
    List<Object> values = (0..<working.rowCount()).collect { int i -> working[i, columnName] }
    List<Object> uniqueValues = (values.unique(false) as List<Object>).sort() as List<Object>

    if (uniqueValues.size() <= 1) {
      log.warn("Categorical column '${columnName}' has only ${uniqueValues.size()} unique value(s) — produces no indicator columns")
      return
    }

    // Treatment contrasts: first level is reference (dropped)
    for (int k = 1; k < uniqueValues.size(); k++) {
      Object level = uniqueValues[k]
      String indicatorName = "${columnName}_${level}"
      List<BigDecimal> indicator = values.collect { Object val ->
        val == level ? 1.0 as BigDecimal : 0.0 as BigDecimal
      }
      outNames << indicatorName
      outColumns << indicator
    }
  }

  private void encodeInteraction(FormulaTerm term, Matrix working,
                                 List<String> outNames, List<List<BigDecimal>> outColumns) {
    // Expand each factor independently, then compute cross-products
    List<List<String>> factorNameSets = []
    List<List<List<BigDecimal>>> factorColumnSets = []

    for (FormulaExpression factor : term.factors) {
      List<String> factorNames = []
      List<List<BigDecimal>> factorCols = []
      encodeSingleFactor(factor, working, factorNames, factorCols)
      factorNameSets << factorNames
      factorColumnSets << factorCols
    }

    // Cartesian product of factor columns
    List<List<Integer>> indexCombos = cartesianProductIndices(factorNameSets.collect { List<String> names -> names.size() })
    for (List<Integer> combo : indexCombos) {
      List<String> nameParts = []
      for (int f = 0; f < combo.size(); f++) {
        nameParts << factorNameSets[f][combo[f]]
      }
      String interactionName = nameParts.join(':')

      List<BigDecimal> product = (0..<working.rowCount()).collect { int row ->
        BigDecimal result = 1.0
        for (int f = 0; f < combo.size(); f++) {
          result = result * factorColumnSets[f][combo[f]][row]
        }
        result
      }
      outNames << interactionName
      outColumns << product
    }
  }

  private static List<List<Integer>> cartesianProductIndices(List<Integer> sizes) {
    List<List<Integer>> result = [[]]
    for (int size : sizes) {
      List<List<Integer>> newResult = []
      for (List<Integer> existing : result) {
        for (int i = 0; i < size; i++) {
          List<Integer> combo = [*existing]
          combo << i
          newResult << combo
        }
      }
      result = newResult
    }
    result
  }

  // --- Stage 8: Build result ---

  private static List<Number> extractResponseColumn(Matrix working, String responseName) {
    (0..<working.rowCount()).collect { int i -> working[i, responseName] as Number }
  }

  private static Matrix buildDesignMatrix(List<String> names, List<List<BigDecimal>> columns, int rowCount) {
    if (names.isEmpty()) {
      return Matrix.builder()
        .columnNames([])
        .rows([])
        .build()
    }
    Matrix.builder()
      .columnNames(names)
      .columns(buildColumnMap(names, columns))
      .types(names.collect { BigDecimal } as List<Class>)
      .build()
  }

  private static Map<String, List> buildColumnMap(List<String> names, List<List<BigDecimal>> columns) {
    Map<String, List> map = [:]
    for (int i = 0; i < names.size(); i++) {
      map[names[i]] = columns[i]
    }
    map
  }

  // --- Weights and offset helpers ---

  private List<Number> resolveWeights() {
    if (weightsColumn != null) {
      List<Number> weights = (0..<data.rowCount()).collect { int i -> data[i, weightsColumn] as Number }
      validateNonNegative(weights)
      return weights
    }
    if (weightsList != null) {
      if (weightsList.size() != data.rowCount()) {
        throw new IllegalArgumentException(
          "Weights size (${weightsList.size()}) does not match data row count (${data.rowCount()})"
        )
      }
      List<Number> weights = weightsList.collect { Number n -> n } as List<Number>
      validateNonNegative(weights)
      return weights
    }
    null
  }

  private List<Number> resolveOffset() {
    if (offsetColumn != null) {
      return (0..<data.rowCount()).collect { int i -> data[i, offsetColumn] as Number }
    }
    if (offsetList != null) {
      if (offsetList.size() != data.rowCount()) {
        throw new IllegalArgumentException(
          "Offset size (${offsetList.size()}) does not match data row count (${data.rowCount()})"
        )
      }
      return offsetList.collect { Number n -> n } as List<Number>
    }
    null
  }

  private static void validateNonNegative(List<Number> values) {
    for (Number v : values) {
      if (v != null && (v as BigDecimal) < 0) {
        throw new IllegalArgumentException('Weights must be non-negative')
      }
    }
  }

  private static List<Number> filterToSurvivors(List<Number> values, List<Integer> survivingOriginalIndices) {
    if (values == null) {
      return null
    }
    survivingOriginalIndices.collect { Integer originalIdx -> values[originalIdx] } as List<Number>
  }

  private static <T> T requireNonNull(T value, String label) {
    if (value == null) {
      throw new IllegalArgumentException("${label} cannot be null")
    }
    value
  }

  @CompileStatic
  private static final class SubsetResult {
    final Matrix data
    final List<Integer> indices

    SubsetResult(Matrix data, List<Integer> indices) {
      this.data = data
      this.indices = indices
    }
  }

  @CompileStatic
  private static final class NaResult {
    final Matrix data
    final List<Integer> droppedRows
    final List<Integer> survivingIndices

    NaResult(Matrix data, List<Integer> droppedRows, List<Integer> survivingIndices) {
      this.data = data
      this.droppedRows = droppedRows
      this.survivingIndices = survivingIndices
    }
  }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :matrix-stats:test --tests 'formula.FormulaModelFrameTest' -g ./.gradle-user`
Expected: PASS — all 6 tests pass.

- [ ] **Step 6: Run Spotless and CodeNarc**

Run: `./gradlew :matrix-stats:spotlessApply :matrix-stats:codenarcMain :matrix-stats:codenarcTest -g ./.gradle-user`
Expected: PASS with no new violations.

- [ ] **Step 7: Commit**

```bash
git add matrix-stats/src/main/groovy/se/alipsa/matrix/stats/formula/FormulaSupport.groovy \
  matrix-stats/src/main/groovy/se/alipsa/matrix/stats/formula/ModelFrame.groovy \
  matrix-stats/src/test/groovy/formula/FormulaModelFrameTest.groovy
git commit -m "Implement ModelFrame core pipeline with dot expansion"
```

---

### Task 3: Dot-Power Expansion

**Files:**
- Modify: `matrix-stats/src/test/groovy/formula/FormulaModelFrameTest.groovy`
- (No source changes expected — the pipeline already normalizes after dot expansion)

- [ ] **Step 1: Write failing tests for dot-power expansion**

Add these tests to `FormulaModelFrameTest`:

```groovy
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
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `./gradlew :matrix-stats:test --tests 'formula.FormulaModelFrameTest' -g ./.gradle-user`
Expected: PASS — the string-path pipeline already expands dots before normalization, so `.^2` works. If tests fail, the `computeDotColumns` method may need adjustment for the `(. + x)^2` case to correctly exclude `x` from dot expansion.

- [ ] **Step 3: Commit**

```bash
git add matrix-stats/src/test/groovy/formula/FormulaModelFrameTest.groovy
git commit -m "Add dot-power expansion tests for ModelFrame"
```

---

### Task 4: Categorical Encoding

**Files:**
- Modify: `matrix-stats/src/test/groovy/formula/FormulaModelFrameTest.groovy`

- [ ] **Step 1: Write failing tests for categorical encoding**

Add these tests to `FormulaModelFrameTest`:

```groovy
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

  // species has 3 levels sorted: setosa (ref), versicolor, virginica
  assertEquals(['x', 'species_versicolor', 'species_virginica'], result.predictorNames)
  assertEquals(4, result.data.rowCount())

  // Row 0: setosa -> versicolor=0, virginica=0
  assertEquals(0.0 as BigDecimal, result.data[0, 'species_versicolor'])
  assertEquals(0.0 as BigDecimal, result.data[0, 'species_virginica'])

  // Row 1: versicolor -> versicolor=1, virginica=0
  assertEquals(1.0 as BigDecimal, result.data[1, 'species_versicolor'])
  assertEquals(0.0 as BigDecimal, result.data[1, 'species_virginica'])

  // Row 2: virginica -> versicolor=0, virginica=1
  assertEquals(0.0 as BigDecimal, result.data[2, 'species_versicolor'])
  assertEquals(1.0 as BigDecimal, result.data[2, 'species_virginica'])
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

  // Boolean: false (ref), true gets indicator
  assertEquals(['x', 'active_true'], result.predictorNames)
  assertEquals(0.0 as BigDecimal, result.data[0, 'active_true'])
  assertEquals(1.0 as BigDecimal, result.data[1, 'active_true'])
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

  // Row 1: green -> green:weight = 1 * 20 = 20
  assertEquals(20.0 as BigDecimal, result.data[1, 'color_green:weight'])
  assertEquals(0.0 as BigDecimal, result.data[1, 'color_red:weight'])

  // Row 2: red -> red:weight = 1 * 30 = 30
  assertEquals(0.0 as BigDecimal, result.data[2, 'color_green:weight'])
  assertEquals(30.0 as BigDecimal, result.data[2, 'color_red:weight'])
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
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `./gradlew :matrix-stats:test --tests 'formula.FormulaModelFrameTest' -g ./.gradle-user`
Expected: PASS — categorical encoding is implemented in Task 2.

- [ ] **Step 3: Commit**

```bash
git add matrix-stats/src/test/groovy/formula/FormulaModelFrameTest.groovy
git commit -m "Add categorical encoding tests for ModelFrame"
```

---

### Task 5: Weights, Offset, and Subset

**Files:**
- Modify: `matrix-stats/src/test/groovy/formula/FormulaModelFrameTest.groovy`

- [ ] **Step 1: Write failing tests for weights, offset, and subset**

Add these tests to `FormulaModelFrameTest`:

```groovy
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

  List<Number> weights = [1.0, 2.0] as List<Number>
  ModelFrameResult result = ModelFrame.of('y ~ x', data)
    .weights(weights)
    .evaluate()

  assertEquals([1.0, 2.0], result.weights)
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
      .weights([-1.0, 2.0] as List<Number>)
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
      .weights([1.0, 2.0, 3.0] as List<Number>)
      .evaluate()
  }
  assertTrue(ex.message.contains('Weights size'))
}
```

Add the missing `Row` import at the top of the test file:

```groovy
import se.alipsa.matrix.core.Row
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `./gradlew :matrix-stats:test --tests 'formula.FormulaModelFrameTest' -g ./.gradle-user`
Expected: PASS — weights, offset, and subset are implemented in the pipeline from Task 2.

- [ ] **Step 3: Commit**

```bash
git add matrix-stats/src/test/groovy/formula/FormulaModelFrameTest.groovy
git commit -m "Add weights, offset, and subset tests for ModelFrame"
```

---

### Task 6: NA Action Handling

**Files:**
- Modify: `matrix-stats/src/test/groovy/formula/FormulaModelFrameTest.groovy`

- [ ] **Step 1: Write failing tests for NA handling**

Add these tests to `FormulaModelFrameTest`:

```groovy
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
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `./gradlew :matrix-stats:test --tests 'formula.FormulaModelFrameTest' -g ./.gradle-user`
Expected: PASS — NA handling is implemented in the pipeline from Task 2.

- [ ] **Step 3: Commit**

```bash
git add matrix-stats/src/test/groovy/formula/FormulaModelFrameTest.groovy
git commit -m "Add NA action tests for ModelFrame"
```

---

### Task 7: Environment Resolution and Error Cases

**Files:**
- Modify: `matrix-stats/src/test/groovy/formula/FormulaModelFrameTest.groovy`

- [ ] **Step 1: Write failing tests for environment resolution and error cases**

Add these tests to `FormulaModelFrameTest`:

```groovy
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
    ModelFrame.of('y ~ x', null)
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
void testNormalizedFormulaPath() {
  Matrix data = Matrix.builder()
    .columnNames(['y', 'a', 'b', 'c'])
    .rows([
      [10.0, 1.0, 2.0, 3.0],
      [20.0, 4.0, 5.0, 6.0],
    ])
    .types([BigDecimal, BigDecimal, BigDecimal, BigDecimal])
    .build()

  // Pre-normalized formula with dot — dot gets expanded from NormalizedFormula path
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
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `./gradlew :matrix-stats:test --tests 'formula.FormulaModelFrameTest' -g ./.gradle-user`
Expected: PASS — all environment resolution and error handling is in the pipeline from Task 2.

- [ ] **Step 3: Commit**

```bash
git add matrix-stats/src/test/groovy/formula/FormulaModelFrameTest.groovy
git commit -m "Add environment resolution and error case tests for ModelFrame"
```

---

### Task 8: NaAction with Subset and Weights Interaction

**Files:**
- Modify: `matrix-stats/src/test/groovy/formula/FormulaModelFrameTest.groovy`

- [ ] **Step 1: Write integration tests for combined features**

Add these tests to `FormulaModelFrameTest`:

```groovy
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
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `./gradlew :matrix-stats:test --tests 'formula.FormulaModelFrameTest' -g ./.gradle-user`
Expected: PASS.

- [ ] **Step 3: Run full matrix-stats test suite to check for regressions**

Run: `./gradlew :matrix-stats:spotlessApply :matrix-stats:test -g ./.gradle-user`
Expected: PASS — no regressions in existing formula or other tests.

- [ ] **Step 4: Commit**

```bash
git add matrix-stats/src/test/groovy/formula/FormulaModelFrameTest.groovy
git commit -m "Add integration tests for combined subset, NA, and weights"
```

---

### Task 9: Final Validation and Roadmap Update

**Files:**
- Modify: `matrix-stats/req/v2.4.0-roadmap.md`

- [ ] **Step 1: Run full build with CodeNarc**

Run: `./gradlew :matrix-stats:spotlessApply :matrix-stats:build -g ./.gradle-user`
Expected: PASS — clean build with no violations.

- [ ] **Step 2: Run all formula tests explicitly**

Run: `./gradlew :matrix-stats:test --tests 'formula.*' -g ./.gradle-user`
Expected: PASS — all formula tests (existing FormulaTest + new FormulaModelFrameTest) pass.

- [ ] **Step 3: Update roadmap checkboxes**

In `matrix-stats/req/v2.4.0-roadmap.md`, update section 14:

```markdown
14.1 [x] Build a ModelFrame pipeline
- Resolve variables from data (data-first evaluation)
- Optional explicit environment resolution hook (if needed later)
- Support dot expansion and exclusion (-) before matrix building
- Expand dot-containing interaction powers such as `.^2` and `(. + x)^2` after the available data columns are known, preserving the intended interaction order semantics

14.2 [x] Support weights, offset, subset, and na.action
- weights: numeric vector aligned to rows
- offset: numeric vector applied to fitted values
- subset: row filter expression or boolean mask
- na.action: omit/fail/exclude behavior
```

Add test commands below section 14.2:

```markdown
Tests:
- `./gradlew :matrix-stats:test --tests 'formula.FormulaModelFrameTest' -g ./.gradle-user`
- `./gradlew :matrix-stats:spotlessApply :matrix-stats:build -g ./.gradle-user`
```

- [ ] **Step 4: Commit**

```bash
git add matrix-stats/req/v2.4.0-roadmap.md
git commit -m "Mark section 14 complete in roadmap"
```
