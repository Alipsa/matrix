package se.alipsa.matrix.stats.formula

import groovy.transform.CompileStatic

/**
 * Base type for parsed formula expressions.
 */
@CompileStatic
abstract class FormulaExpression {

  final int start
  final int end

  protected FormulaExpression(int start, int end) {
    this.start = start
    this.end = end
  }

  /**
   * Returns the expression using canonical formula syntax.
   *
   * @return a canonical formula representation
   */
  abstract String asFormulaString()

  @Override
  String toString() {
    asFormulaString()
  }

  /**
   * Variable reference, including optional backtick-quoted names.
   */
  @CompileStatic
  static final class Variable extends FormulaExpression {
    final String name
    final boolean quoted

    Variable(String name, boolean quoted, int start, int end) {
      super(start, end)
      this.name = requireNonBlank(name, 'name')
      this.quoted = quoted
    }

    @Override
    String asFormulaString() {
      if (quoted || !(name ==~ /[A-Za-z_]\w*/)) {
        return "`${name}`"
      }
      name
    }
  }

  /**
   * Dot placeholder used for later model-frame expansion.
   */
  @CompileStatic
  static final class Dot extends FormulaExpression {
    Dot(int start, int end) {
      super(start, end)
    }

    @Override
    String asFormulaString() {
      '.'
    }
  }

  /**
   * Numeric literal used inside formula expressions.
   */
  @CompileStatic
  static final class NumberLiteral extends FormulaExpression {
    final BigDecimal value
    final String sourceText

    NumberLiteral(BigDecimal value, String sourceText, int start, int end) {
      super(start, end)
      this.value = requireNonNullValue(value, 'value')
      this.sourceText = requireNonBlank(sourceText, 'sourceText')
    }

    @Override
    String asFormulaString() {
      sourceText
    }
  }

  /**
   * Function or transform call such as {@code log(x)} or {@code I(x + y)}.
   */
  @CompileStatic
  static final class FunctionCall extends FormulaExpression {
    final String name
    final List<FormulaExpression> arguments

    FunctionCall(String name, List<FormulaExpression> arguments, int start, int end) {
      super(start, end)
      this.name = requireNonBlank(name, 'name')
      this.arguments = copyExpressions(arguments, 'arguments')
    }

    @Override
    String asFormulaString() {
      "${name}(${arguments.collect { FormulaExpression arg -> arg.asFormulaString() }.join(', ')})"
    }
  }

  /**
   * Unary expression such as {@code -1} or {@code +x}.
   */
  @CompileStatic
  static final class Unary extends FormulaExpression {
    final String operator
    final FormulaExpression expression

    Unary(String operator, FormulaExpression expression, int start, int end) {
      super(start, end)
      this.operator = requireNonBlank(operator, 'operator')
      this.expression = requireNonNullValue(expression, 'expression')
    }

    @Override
    String asFormulaString() {
      String inner = expression instanceof Binary ? "(${expression.asFormulaString()})" : expression.asFormulaString()
      "${operator}${inner}"
    }
  }

  /**
   * Binary operator expression such as addition, interaction, or nesting.
   */
  @CompileStatic
  static final class Binary extends FormulaExpression {
    final String operator
    final FormulaExpression left
    final FormulaExpression right

    Binary(String operator, FormulaExpression left, FormulaExpression right, int start, int end) {
      super(start, end)
      this.operator = requireNonBlank(operator, 'operator')
      this.left = requireNonNullValue(left, 'left')
      this.right = requireNonNullValue(right, 'right')
    }

    @Override
    String asFormulaString() {
      "${formatOperand(left, true)} ${operator} ${formatOperand(right, false)}"
    }

    private String formatOperand(FormulaExpression expression, boolean leftSide) {
      if (expression instanceof Binary) {
        Binary binary = expression as Binary
        int currentPrecedence = FormulaSupport.operatorPrecedence(operator)
        int childPrecedence = FormulaSupport.operatorPrecedence(binary.operator)
        boolean needsParentheses = childPrecedence < currentPrecedence ||
          (!leftSide && operator == '^' && childPrecedence == currentPrecedence)
        if (needsParentheses) {
          return "(${binary.asFormulaString()})"
        }
      }
      expression.asFormulaString()
    }
  }

  /**
   * Parenthesized subexpression that preserves grouping during parsing.
   */
  @CompileStatic
  static final class Grouping extends FormulaExpression {
    final FormulaExpression expression

    Grouping(FormulaExpression expression, int start, int end) {
      super(start, end)
      this.expression = requireNonNullValue(expression, 'expression')
    }

    @Override
    String asFormulaString() {
      "(${expression.asFormulaString()})"
    }
  }

  private static String requireNonBlank(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("${label} cannot be null or blank")
    }
    value
  }

  private static <T> T requireNonNullValue(T value, String label) {
    if (value == null) {
      throw new IllegalArgumentException("${label} cannot be null")
    }
    value
  }

  private static List<FormulaExpression> copyExpressions(List<FormulaExpression> expressions, String label) {
    if (expressions == null) {
      throw new IllegalArgumentException("${label} cannot be null")
    }
    if (expressions.any { FormulaExpression expression -> expression == null }) {
      throw new IllegalArgumentException("${label} cannot contain null values")
    }
    List.copyOf(expressions)
  }
}
