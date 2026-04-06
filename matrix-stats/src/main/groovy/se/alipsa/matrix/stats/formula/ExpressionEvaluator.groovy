package se.alipsa.matrix.stats.formula

import se.alipsa.matrix.core.Matrix

/**
 * Evaluates a {@link FormulaExpression} against matrix data to produce numeric values.
 *
 * <p>Supports variables, numeric literals, arithmetic operators, and transform functions
 * such as {@code log}, {@code sqrt}, and {@code exp}. The {@code I(...)} function is
 * treated as an arithmetic expression wrapper.
 */
final class ExpressionEvaluator {

  private final Matrix data
  private final Map<String, List<?>> env

  ExpressionEvaluator(Matrix data, Map<String, List<?>> env) {
    this.data = data
    this.env = env
  }

  /**
   * Evaluates the expression for all rows and returns a list of BigDecimal values.
   *
   * @param expression the expression to evaluate
   * @return one value per row in the data matrix
   * @throws IllegalArgumentException if a variable is not found or the expression is unsupported
   */
  List<BigDecimal> evaluate(FormulaExpression expression) {
    List<BigDecimal> result = []
    for (int i = 0; i < data.rowCount(); i++) {
      result << evaluateRow(expression, i)
    }
    result
  }

  /**
   * Evaluates the expression for a single row.
   *
   * @param expression the expression to evaluate
   * @param rowIndex the row index
   * @return the numeric result
   */
  BigDecimal evaluateRow(FormulaExpression expression, int rowIndex) {
    switch (expression) {
      case FormulaExpression.Variable -> evaluateVariable(expression as FormulaExpression.Variable, rowIndex)
      case FormulaExpression.NumberLiteral -> (expression as FormulaExpression.NumberLiteral).value
      case FormulaExpression.FunctionCall -> evaluateFunctionCall(expression as FormulaExpression.FunctionCall, rowIndex)
      case FormulaExpression.Unary -> evaluateUnary(expression as FormulaExpression.Unary, rowIndex)
      case FormulaExpression.Binary -> evaluateBinary(expression as FormulaExpression.Binary, rowIndex)
      case FormulaExpression.Grouping -> evaluateRow((expression as FormulaExpression.Grouping).expression, rowIndex)
      default -> throw new IllegalArgumentException("Unsupported expression type: ${expression.class.simpleName}")
    }
  }

  private BigDecimal evaluateVariable(FormulaExpression.Variable variable, int rowIndex) {
    String name = variable.name
    int colIdx = data.columnIndex(name)
    if (colIdx >= 0) {
      Object value = data[rowIndex, name]
      if (value == null) {
        throw new IllegalArgumentException("Variable '${name}' contains null values")
      }
      return value as BigDecimal
    }
    if (env != null && env.containsKey(name)) {
      Object value = env[name][rowIndex]
      if (value == null) {
        throw new IllegalArgumentException("Environment variable '${name}' contains null values")
      }
      return value as BigDecimal
    }
    throw new IllegalArgumentException("Variable '${name}' not found in data or environment")
  }

  private BigDecimal evaluateFunctionCall(FormulaExpression.FunctionCall call, int rowIndex) {
    String name = call.name.toLowerCase()
    if (name == 'log') {
      if (call.arguments.size() != 1) {
        throw new IllegalArgumentException("log() requires exactly 1 argument, got ${call.arguments.size()}")
      }
      BigDecimal value = evaluateRow(call.arguments[0], rowIndex)
      return Math.log(value as double) as BigDecimal
    }
    if (name == 'sqrt') {
      if (call.arguments.size() != 1) {
        throw new IllegalArgumentException("sqrt() requires exactly 1 argument, got ${call.arguments.size()}")
      }
      BigDecimal value = evaluateRow(call.arguments[0], rowIndex)
      return Math.sqrt(value as double) as BigDecimal
    }
    if (name == 'exp') {
      if (call.arguments.size() != 1) {
        throw new IllegalArgumentException("exp() requires exactly 1 argument, got ${call.arguments.size()}")
      }
      BigDecimal value = evaluateRow(call.arguments[0], rowIndex)
      return Math.exp(value as double) as BigDecimal
    }
    if (name == 'i') {
      if (call.arguments.size() != 1) {
        throw new IllegalArgumentException("I() requires exactly 1 argument, got ${call.arguments.size()}")
      }
      return evaluateRow(call.arguments[0], rowIndex)
    }
    throw new IllegalArgumentException("Unsupported function in expression evaluator: ${call.name}")
  }

  private BigDecimal evaluateUnary(FormulaExpression.Unary unary, int rowIndex) {
    BigDecimal value = evaluateRow(unary.expression, rowIndex)
    switch (unary.operator) {
      case '+' -> value
      case '-' -> -value
      default -> throw new IllegalArgumentException("Unsupported unary operator: ${unary.operator}")
    }
  }

  private BigDecimal evaluateBinary(FormulaExpression.Binary binary, int rowIndex) {
    BigDecimal left = evaluateRow(binary.left, rowIndex)
    BigDecimal right = evaluateRow(binary.right, rowIndex)
    switch (binary.operator) {
      case '+' -> left + right
      case '-' -> left - right
      case '*' -> left * right
      case '/' -> {
        if (right == 0.0) {
          throw new IllegalArgumentException("Division by zero in expression: ${binary.asFormulaString()}")
        }
        left / right
      }
      case '^' -> (Math.pow(left as double, right as double)) as BigDecimal
      default -> throw new IllegalArgumentException("Unsupported binary operator: ${binary.operator}")
    }
  }
}
