package se.alipsa.matrix.stats.formula

import se.alipsa.matrix.core.Matrix

/**
 * Builds the predictor-side design matrix for a normalized formula.
 * <p>
 * The builder is responsible for turning normalized {@link FormulaTerm} instances into
 * concrete matrix columns, including arithmetic expressions, categorical contrasts,
 * interaction expansion, and special formula helpers such as {@code poly(...)} and
 * {@code s(...)}. It also records the emitted term metadata used by downstream fit
 * methods to understand which columns came from each logical term.
 * <p>
 * When a term expands to no columns, for example because a categorical predictor has
 * only a reference level after filtering, the returned {@link Terms} metadata preserves
 * that dropped-term state even though no predictor column is emitted.
 */
final class DesignMatrixBuilder {

  private final Matrix data
  private final Map<String, List<?>> env
  private final CategoricalEncoder encoder
  private final ExpressionEvaluator evaluator
  private final Map<String, ContrastType> contrastConfig
  private final ContrastType defaultContrast

  DesignMatrixBuilder(
    Matrix data,
    Map<String, List<?>> env,
    Map<String, ContrastType> contrastConfig,
    ContrastType defaultContrast
  ) {
    this.data = data
    this.env = env
    this.encoder = new CategoricalEncoder(data)
    this.evaluator = new ExpressionEvaluator(data, env)
    this.contrastConfig = contrastConfig ?: [:]
    this.defaultContrast = defaultContrast ?: ContrastType.TREATMENT
  }

  /**
   * Builds the design matrix and term metadata from a normalized formula.
   *
   * @param formula the normalized formula whose predictor terms should be encoded
   * @return a {@link DesignMatrix} containing the predictor matrix, emitted column names,
   *   and aligned {@link Terms} metadata
   */
  DesignMatrix build(NormalizedFormula formula) {
    List<String> predictorNames = []
    List<List<BigDecimal>> predictorColumns = []
    List<Terms.TermInfo> termInfos = []

    for (FormulaTerm term : formula.predictorTerms) {
      List<ColumnInfo> columns = encodeTerm(term)
      if (columns.isEmpty()) {
        termInfos << new Terms.TermInfo(
          term,
          term.asFormulaString(),
          [],
          false,
          [],
          false,
          true,
          'Term produced no columns'
        )
      } else {
        boolean termIsCategorical = columns.any { ColumnInfo col -> col.isCategorical }
        List<String> allLevels = columns.collectMany { ColumnInfo col -> col.factorLevels }.unique()
        List<String> colNames = columns.collect { ColumnInfo col -> col.name }
        boolean termIsSmooth = columns.any { ColumnInfo col -> col.isSmooth }
        termInfos << new Terms.TermInfo(
          term,
          term.asFormulaString(),
          colNames,
          termIsCategorical,
          allLevels,
          termIsSmooth,
          false,
          null
        )
        for (ColumnInfo col : columns) {
          predictorNames << col.name
          predictorColumns << col.values
        }
      }
    }

    Terms terms = new Terms(formula.response, formula.includeIntercept, termInfos)
    Matrix designMatrix = buildMatrix(predictorNames, predictorColumns)

    new DesignMatrix(designMatrix, predictorNames, terms)
  }

  private List<ColumnInfo> encodeTerm(FormulaTerm term) {
    if (term.isInteraction()) {
      encodeInteraction(term)
    } else {
      encodeFactor(term.factors[0])
    }
  }

  private List<ColumnInfo> encodeFactor(FormulaExpression factor) {
    // Special handling for poly(x, n) and s(x) / s(x, df)
    if (factor instanceof FormulaExpression.FunctionCall) {
      FormulaExpression.FunctionCall call = factor as FormulaExpression.FunctionCall
      if (call.name.equalsIgnoreCase('poly')) {
        return encodePoly(call)
      }
      if (call.name.equalsIgnoreCase('s')) {
        return encodeSmooth(call)
      }
    }

    String baseName = generateColumnName(factor)

    // Check if this is a simple variable reference to a categorical column
    if (factor instanceof FormulaExpression.Variable) {
      String varName = (factor as FormulaExpression.Variable).name
      if (encoder.isCategorical(varName)) {
        ContrastType contrast = contrastConfig.getOrDefault(varName, defaultContrast)
        Map<String, List<BigDecimal>> encoded = encoder.encode(varName, contrast)
        List<String> levels = encoder.levels(varName)
        if (encoded.isEmpty()) {
          return []
        }
        return encoded.collect { String colName, List<BigDecimal> values ->
          new ColumnInfo(colName, values, true, levels)
        }
      }
    }

    // Numeric expression evaluation
    List<BigDecimal> values = evaluator.evaluate(factor)
    [new ColumnInfo(baseName, values, false, [])]
  }

  private List<ColumnInfo> encodePoly(FormulaExpression.FunctionCall call) {
    if (call.arguments.size() != 2) {
      throw new IllegalArgumentException("poly() requires exactly 2 arguments, got ${call.arguments.size()}")
    }

    FormulaExpression varExpr = call.arguments[0]
    FormulaExpression degreeExpr = call.arguments[1]

    if (!(degreeExpr instanceof FormulaExpression.NumberLiteral)) {
      throw new IllegalArgumentException("poly() degree must be a numeric literal")
    }

    BigDecimal degreeValue = (degreeExpr as FormulaExpression.NumberLiteral).value
    if (degreeValue.scale() > 0 || degreeValue <= 0) {
      throw new IllegalArgumentException("poly() degree must be a positive integer, got ${degreeValue}")
    }
    int degree = degreeValue.intValue()

    List<BigDecimal> baseValues = evaluator.evaluate(varExpr)
    String baseName = generateColumnName(varExpr)

    List<ColumnInfo> result = []
    for (int d = 1; d <= degree; d++) {
      String colName = "${baseName}_poly${d}"
      List<BigDecimal> values = baseValues.collect { BigDecimal v ->
        (Math.pow(v as double, d)) as BigDecimal
      }
      result << new ColumnInfo(colName, values, false, [])
    }
    result
  }

  private static final int DEFAULT_SMOOTH_DF = 4

  private List<ColumnInfo> encodeSmooth(FormulaExpression.FunctionCall call) {
    if (call.arguments.isEmpty() || call.arguments.size() > 2) {
      throw new IllegalArgumentException("s() requires 1 or 2 arguments (variable and optional df), got ${call.arguments.size()}")
    }

    FormulaExpression varExpr = call.arguments[0]
    int df = DEFAULT_SMOOTH_DF
    if (call.arguments.size() == 2) {
      FormulaExpression dfExpr = call.arguments[1]
      if (!(dfExpr instanceof FormulaExpression.NumberLiteral)) {
        throw new IllegalArgumentException('s() df must be a numeric literal')
      }
      BigDecimal dfValue = (dfExpr as FormulaExpression.NumberLiteral).value
      if (dfValue.scale() > 0 || dfValue < 1) {
        throw new IllegalArgumentException("s() df must be an integer >= 1, got ${dfValue}")
      }
      df = dfValue.intValue()
    }

    List<BigDecimal> baseValues = evaluator.evaluate(varExpr)
    String baseName = generateColumnName(varExpr)

    double[] x = new double[baseValues.size()]
    for (int i = 0; i < baseValues.size(); i++) {
      x[i] = baseValues[i].doubleValue()
    }
    double[][] basis = SplineBasisExpander.naturalCubicSplineBasis(x, df)

    List<ColumnInfo> result = []
    for (int j = 0; j < df; j++) {
      String colName = "s_${baseName}_${j + 1}"
      List<BigDecimal> values = (0..<baseValues.size()).collect { int i -> basis[i][j] as BigDecimal }
      // Mark as smooth so GamMethod can apply penalty selectively
      result << new ColumnInfo(colName, values, false, [], true)
    }
    result
  }

  private List<ColumnInfo> encodeInteraction(FormulaTerm term) {
    List<List<ColumnInfo>> factorColumns = []
    for (FormulaExpression factor : term.factors) {
      List<ColumnInfo> cols = encodeFactor(factor)
      if (cols.isEmpty()) {
        return []
      }
      if (cols.any { ColumnInfo col -> col.isSmooth }) {
        throw new IllegalArgumentException(
          "Smooth terms cannot be used in interactions: ${term.asFormulaString()}"
        )
      }
      factorColumns << cols
    }

    List<List<Integer>> indexCombos = cartesianProductIndices(
      factorColumns.collect { List<ColumnInfo> cols -> cols.size() }
    )

    List<ColumnInfo> result = []
    for (List<Integer> combo : indexCombos) {
      List<String> nameParts = []
      boolean isCategorical = false
      List<String> allLevels = []
      for (int f = 0; f < combo.size(); f++) {
        ColumnInfo col = factorColumns[f][combo[f]]
        nameParts << col.name
        if (col.isCategorical) {
          isCategorical = true
          allLevels.addAll(col.factorLevels)
        }
      }
      String interactionName = nameParts.join(':')

      List<BigDecimal> product = (0..<data.rowCount()).collect { int row ->
        BigDecimal prod = 1.0
        for (int f = 0; f < combo.size(); f++) {
          prod = prod * factorColumns[f][combo[f]].values[row]
        }
        prod
      }
      result << new ColumnInfo(interactionName, product, isCategorical, allLevels.unique())
    }
    result
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

  private String generateColumnName(FormulaExpression expression) {
    switch (expression) {
      case FormulaExpression.Variable -> (expression as FormulaExpression.Variable).name
      case FormulaExpression.NumberLiteral -> (expression as FormulaExpression.NumberLiteral).sourceText
      case FormulaExpression.FunctionCall -> generateFunctionName(expression as FormulaExpression.FunctionCall)
      case FormulaExpression.Unary -> {
        FormulaExpression.Unary unary = expression as FormulaExpression.Unary
        String inner = generateColumnName(unary.expression)
        "${unary.operator}${inner}"
      }
      case FormulaExpression.Binary -> generateBinaryName(expression as FormulaExpression.Binary)
      case FormulaExpression.Grouping -> generateColumnName((expression as FormulaExpression.Grouping).expression)
      default -> "expr_${expression.hashCode()}"
    }
  }

  private String generateFunctionName(FormulaExpression.FunctionCall call) {
    String name = call.name.toLowerCase()
    switch (name) {
      case 'log':
      case 'sqrt':
      case 'exp':
        if (call.arguments.size() == 1) {
          return "${name}_${generateColumnName(call.arguments[0])}"
        }
        break
      case 'i':
        if (call.arguments.size() == 1) {
          String innerName = generateStructuralName(call.arguments[0])
          return "I_${innerName}"
        }
        break
    }
    "${name}_${call.arguments.collect { FormulaExpression arg -> generateColumnName(arg) }.join('_')}"
  }

  private String generateBinaryName(FormulaExpression.Binary binary) {
    String left = generateColumnName(binary.left)
    String right = generateColumnName(binary.right)
    String opName = switch (binary.operator) {
      case '+' -> 'plus'
      case '-' -> 'minus'
      case '*' -> 'times'
      case '/' -> 'div'
      case '^' -> 'pow'
      default -> binary.operator
    }
    "${left}_${opName}_${right}"
  }

  private String generateStructuralName(FormulaExpression expression) {
    String binaryName = generateColumnName(expression)
    // If the binary name is very long, use a hash-based fallback for stability
    if (binaryName.length() > 50) {
      return Integer.toHexString(expression.asFormulaString().hashCode())
    }
    binaryName
  }

  private Matrix buildMatrix(List<String> names, List<List<BigDecimal>> columns) {
    if (names.isEmpty()) {
      return Matrix.builder()
        .rows((0..<data.rowCount()).collect { [] as List })
        .build()
    }
    Map<String, List> columnMap = [:]
    for (int i = 0; i < names.size(); i++) {
      columnMap[names[i]] = columns[i]
    }
    Matrix.builder()
      .columnNames(names)
      .columns(columnMap)
      .types(names.collect { BigDecimal } as List<Class>)
      .build()
  }

  static final class DesignMatrix {
    final Matrix data
    final List<String> predictorNames
    final Terms terms

    DesignMatrix(Matrix data, List<String> predictorNames, Terms terms) {
      this.data = data
      this.predictorNames = List.copyOf(predictorNames)
      this.terms = terms
    }
  }

  private static final class ColumnInfo {
    final String name
    final List<BigDecimal> values
    final boolean isCategorical
    final List<String> factorLevels
    final boolean isSmooth

    ColumnInfo(String name, List<BigDecimal> values, boolean isCategorical,
               List<String> factorLevels, boolean isSmooth = false) {
      this.name = name
      this.values = List.copyOf(values)
      this.isCategorical = isCategorical
      this.factorLevels = List.copyOf(factorLevels)
      this.isSmooth = isSmooth
    }
  }
}
