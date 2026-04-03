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
 *
 * <p><strong>Categorical handling:</strong> Non-numeric columns are encoded using treatment
 * contrasts (dummy coding). The first level (alphabetically) is the reference and omitted.
 * Single-level categoricals produce no indicator columns and are silently dropped.
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
  private Map<String, ContrastType> contrastConfig = [:]
  private ContrastType defaultContrast = ContrastType.TREATMENT

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
   * @throws IllegalArgumentException if formula is null or blank, or data is null
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
   * @throws IllegalArgumentException if formula is null or data is null
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
   * @throws IllegalArgumentException if weights contain negative values or size does not match data
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
   * @throws IllegalArgumentException if column does not exist in data
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
   * @throws IllegalArgumentException if size does not match data row count
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
   * @throws IllegalArgumentException if column does not exist in data
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
   * Sets the default contrast type for all categorical columns.
   *
   * @param contrastType the default contrast type
   * @return this builder
   */
  ModelFrame defaultContrast(ContrastType contrastType) {
    this.defaultContrast = requireNonNull(contrastType, 'defaultContrast')
    this
  }

  /**
   * Sets the contrast type for a specific categorical column.
   *
   * @param columnName the categorical column name
   * @param contrastType the contrast type to use
   * @return this builder
   */
  ModelFrame contrasts(String columnName, ContrastType contrastType) {
    requireNonBlank(columnName, 'columnName')
    this.contrastConfig[columnName] = requireNonNull(contrastType, 'contrastType')
    this
  }

  /**
   * Sets contrast types for multiple categorical columns.
   *
   * @param contrasts map of column name to contrast type
   * @return this builder
   */
  ModelFrame contrasts(Map<String, ContrastType> contrasts) {
    requireNonNull(contrasts, 'contrasts')
    this.contrastConfig.putAll(contrasts)
    this
  }

  /**
   * Evaluates the formula against the data and returns the result.
   *
   * @return the model frame result
   * @throws IllegalArgumentException if response column is missing or non-numeric, variables not found,
   *                                  no observations remain after subset/NA removal, or weights are negative
   */
  ModelFrameResult evaluate() {
    // Stage 1: Determine excluded columns for dot expansion
    Set<String> excludeFromDot = [] as Set<String>
    if (weightsColumn != null) {
      validateColumnExists(weightsColumn, 'Weights')
      excludeFromDot << weightsColumn
    }
    if (offsetColumn != null) {
      validateColumnExists(offsetColumn, 'Offset')
      excludeFromDot << offsetColumn
    }

    // Stages 2-3: Dot expansion and normalization
    NormalizedFormula normalized
    if (parsedFormula != null) {
      normalized = resolveFromParsed(parsedFormula, excludeFromDot)
    } else {
      normalized = resolveFromNormalized(preNormalized, excludeFromDot)
    }

    // Stage 4: Validate response
    String responseName = extractResponseName(normalized.response)
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

    // Stage 6: NA handling (include weights/offset columns so nulls there are handled)
    List<String> allFormulaColumns = [responseName]
    allFormulaColumns.addAll(variableNames)
    if (weightsColumn != null) {
      allFormulaColumns << weightsColumn
    }
    if (offsetColumn != null) {
      allFormulaColumns << offsetColumn
    }
    NaResult naResult = handleNa(working, originalIndices, allFormulaColumns)
    working = naResult.data
    List<Integer> droppedRows = naResult.droppedRows

    // Filter weights/offset/env to surviving original row indices
    resolvedWeights = filterToSurvivors(resolvedWeights, naResult.survivingIndices)
    resolvedOffset = filterToSurvivors(resolvedOffset, naResult.survivingIndices)
    Map<String, List<?>> filteredEnv = filterEnvToSurvivors(env, naResult.survivingIndices)

    // Validate non-empty
    if (working.rowCount() == 0) {
      throw new IllegalArgumentException('No observations remaining after subset and NA removal')
    }

    // Stage 7: Validate contrast configuration
    validateContrastConfiguration(working, variableNames)

    // Stage 8: Build design matrix via dedicated builder
    List<Number> responseValues = extractResponseColumn(working, responseName)
    DesignMatrixBuilder designBuilder = new DesignMatrixBuilder(working, filteredEnv, contrastConfig, defaultContrast)
    DesignMatrixBuilder.DesignMatrix designMatrix = designBuilder.build(normalized)

    new ModelFrameResult(
      designMatrix.data, responseValues, responseName, normalized.includeIntercept,
      designMatrix.predictorNames, resolvedWeights, resolvedOffset, droppedRows, normalized,
      designMatrix.terms
    )
  }

  // --- Stage 2/3: Dot expansion and normalization ---

  private NormalizedFormula resolveFromParsed(ParsedFormula parsed, Set<String> excludeFromDot) {
    String responseName = parsed.response.asFormulaString()
    List<String> dotColumns = computeDotColumns(responseName, parsed.predictors, excludeFromDot)
    FormulaExpression expandedPredictors = FormulaSupport.expandDots(parsed.predictors, dotColumns)
    ParsedFormula expanded = new ParsedFormula(parsed.source, parsed.response, expandedPredictors)
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

  private static List<FormulaTerm> expandDotTerm(FormulaTerm term, List<String> dotColumns) {
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

    List<List<FormulaExpression>> expanded = dotFactors.collect { FormulaExpression ignored ->
      dotColumns.collect { String col ->
        (FormulaExpression) new FormulaExpression.Variable(col, false, 0, 0)
      }
    }

    List<List<FormulaExpression>> combos = cartesianProduct(expanded)
    List<FormulaTerm> result = []
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

  private List<String> computeDotColumns(
    String responseName, FormulaExpression predictors, Set<String> excludeFromDot
  ) {
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

  private static List<String> collectVariableNames(NormalizedFormula normalized) {
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

  private static String extractResponseName(FormulaExpression response) {
    FormulaExpression unwrapped = response
    while (unwrapped instanceof FormulaExpression.Grouping) {
      unwrapped = (unwrapped as FormulaExpression.Grouping).expression
    }
    if (unwrapped instanceof FormulaExpression.Variable) {
      return (unwrapped as FormulaExpression.Variable).name
    }
    throw new IllegalArgumentException(
      "Transformed responses are not yet supported; use a raw variable on the left-hand side, got: ${response.asFormulaString()}"
    )
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
      return new SubsetResult(working.subset(kept), keptOriginal)
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
      return new SubsetResult(working.subset(kept), keptOriginal)
    }
    new SubsetResult(working, originalIndices)
  }

  // --- Stage 6: NA handling ---

  private NaResult handleNa(Matrix working, List<Integer> currentIndices, List<String> columns) {
    // Find all rows with null in any formula column
    Set<Integer> rowsWithNulls = [] as Set<Integer>
    List<String> columnsWithNulls = []

    for (String col : columns) {
      if (data.columnIndex(col) < 0) {
        continue // environment variable, not in data
      }
      boolean found = false
      for (int i = 0; i < working.rowCount(); i++) {
        if (working[i, col] == null) {
          rowsWithNulls << i
          if (!found) {
            columnsWithNulls << col
            found = true
          }
        }
      }
    }

    if (rowsWithNulls.isEmpty()) {
      return new NaResult(working, [], currentIndices)
    }

    if (naActionPolicy == NaAction.FAIL) {
      throw new IllegalArgumentException(
        "NA values found in columns: ${columnsWithNulls} (na.action is FAIL)"
      )
    }

    // OMIT: drop rows with nulls
    List<Integer> keptRows = []
    List<Integer> survivingIndices = []
    List<Integer> droppedOriginal = []
    for (int i = 0; i < working.rowCount(); i++) {
      if (rowsWithNulls.contains(i)) {
        droppedOriginal << currentIndices[i]
      } else {
        keptRows << i
        survivingIndices << currentIndices[i]
      }
    }

    new NaResult(working.subset(keptRows), droppedOriginal, survivingIndices)
  }

  private void validateContrastConfiguration(Matrix working, List<String> variableNames) {
    List<String> dataColumns = working.columnNames()
    for (String colName : contrastConfig.keySet()) {
      if (!dataColumns.contains(colName) && (env == null || !env.containsKey(colName))) {
        throw new IllegalArgumentException(
          "Contrast configured for unknown column '${colName}'. Available columns: ${dataColumns}"
        )
      }
      int colIdx = working.columnIndex(colName)
      if (colIdx >= 0) {
        Class colType = working.type(colName)
        if (Number.isAssignableFrom(colType)) {
          throw new IllegalArgumentException("Contrast only applies to categorical columns, '${colName}' is numeric")
        }
      }
    }
  }

  // --- Stage 8: Build result ---

  private static List<Number> extractResponseColumn(Matrix working, String responseName) {
    (0..<working.rowCount()).collect { int i -> working[i, responseName] as Number }
  }

  private static String requireNonBlank(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("${label} cannot be null or blank")
    }
    value
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

  private static Map<String, List<?>> filterEnvToSurvivors(Map<String, List<?>> env, List<Integer> survivingOriginalIndices) {
    if (env == null) {
      return null
    }
    Map<String, List<?>> filtered = [:]
    for (Map.Entry<String, List<?>> entry : env.entrySet()) {
      filtered[entry.key] = survivingOriginalIndices.collect { Integer originalIdx -> entry.value[originalIdx] }
    }
    filtered
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
