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

  @CompileStatic
  static final class Variable extends FormulaExpression {
    final String name
    final boolean quoted

    Variable(String name, boolean quoted, int start, int end) {
      super(start, end)
      this.name = name
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

  @CompileStatic
  static final class NumberLiteral extends FormulaExpression {
    final BigDecimal value
    final String sourceText

    NumberLiteral(BigDecimal value, String sourceText, int start, int end) {
      super(start, end)
      this.value = value
      this.sourceText = sourceText
    }

    @Override
    String asFormulaString() {
      sourceText
    }
  }

  @CompileStatic
  static final class FunctionCall extends FormulaExpression {
    final String name
    final List<FormulaExpression> arguments

    FunctionCall(String name, List<FormulaExpression> arguments, int start, int end) {
      super(start, end)
      this.name = name
      this.arguments = List.copyOf(arguments)
    }

    @Override
    String asFormulaString() {
      "${name}(${arguments.collect { FormulaExpression arg -> arg.asFormulaString() }.join(', ')})"
    }
  }

  @CompileStatic
  static final class Unary extends FormulaExpression {
    final String operator
    final FormulaExpression expression

    Unary(String operator, FormulaExpression expression, int start, int end) {
      super(start, end)
      this.operator = operator
      this.expression = expression
    }

    @Override
    String asFormulaString() {
      String inner = expression instanceof Binary ? "(${expression.asFormulaString()})" : expression.asFormulaString()
      "${operator}${inner}"
    }
  }

  @CompileStatic
  static final class Binary extends FormulaExpression {
    final String operator
    final FormulaExpression left
    final FormulaExpression right

    Binary(String operator, FormulaExpression left, FormulaExpression right, int start, int end) {
      super(start, end)
      this.operator = operator
      this.left = left
      this.right = right
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

  @CompileStatic
  static final class Grouping extends FormulaExpression {
    final FormulaExpression expression

    Grouping(FormulaExpression expression, int start, int end) {
      super(start, end)
      this.expression = expression
    }

    @Override
    String asFormulaString() {
      "(${expression.asFormulaString()})"
    }
  }
}
