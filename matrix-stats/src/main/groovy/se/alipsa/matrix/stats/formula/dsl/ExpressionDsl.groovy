package se.alipsa.matrix.stats.formula.dsl

import groovy.transform.CompileDynamic
import groovy.transform.PackageScope

/**
 * Internal arithmetic-expression DSL used inside {@code I { ... }} closures.
 */
@PackageScope
final class ExpressionDsl {

  private static final String INVALID_EXPRESSION_MESSAGE =
    'I { ... } must return an arithmetic expression such as x + 1'

  @CompileDynamic
  static ExpressionExpr evaluate(
    @DelegatesTo(value = ExpressionDsl, strategy = Closure.DELEGATE_FIRST)
    Closure<?> closure
  ) {
    if (closure == null) {
      throw new IllegalArgumentException('I { ... } cannot be null')
    }
    ExpressionDsl dsl = new ExpressionDsl()
    Closure cloned = closure.rehydrate(dsl, closure.owner, closure.thisObject)
    cloned.resolveStrategy = Closure.DELEGATE_FIRST
    def result
    use(ExpressionNumberOperators) {
      result = cloned.call()
    }
    if (result instanceof ExpressionExpr) {
      return result as ExpressionExpr
    }
    if (result instanceof Number) {
      return new ExpressionNumberExpr(result as Number)
    }
    throw new IllegalArgumentException(INVALID_EXPRESSION_MESSAGE)
  }

  ExpressionVariableExpr propertyMissing(String name) {
    new ExpressionVariableExpr(name)
  }

  ExpressionVariableExpr col(String name) {
    new ExpressionVariableExpr(name, true)
  }

  ExpressionFunctionExpr log(Object expression) {
    new ExpressionFunctionExpr('log', [ExpressionExpr.coerce(expression, 'expression')])
  }

  ExpressionFunctionExpr sqrt(Object expression) {
    new ExpressionFunctionExpr('sqrt', [ExpressionExpr.coerce(expression, 'expression')])
  }

  ExpressionFunctionExpr exp(Object expression) {
    new ExpressionFunctionExpr('exp', [ExpressionExpr.coerce(expression, 'expression')])
  }
}

@PackageScope
abstract class ExpressionExpr {

  ExpressionExpr plus(Object other) {
    new ExpressionBinaryExpr(this, '+', coerce(other, 'other'))
  }

  ExpressionExpr minus(Object other) {
    new ExpressionBinaryExpr(this, '-', coerce(other, 'other'))
  }

  ExpressionExpr multiply(Object other) {
    new ExpressionBinaryExpr(this, '*', coerce(other, 'other'))
  }

  ExpressionExpr div(Object other) {
    new ExpressionBinaryExpr(this, '/', coerce(other, 'other'))
  }

  ExpressionExpr power(Object other) {
    new ExpressionBinaryExpr(this, '^', coerce(other, 'other'))
  }

  ExpressionExpr negative() {
    new ExpressionUnaryExpr('-', this)
  }

  ExpressionExpr positive() {
    this
  }

  Object remainder(Object other) {
    throw new IllegalArgumentException("Unsupported arithmetic operator '%' in I { ... }")
  }

  abstract String render()

  static ExpressionExpr coerce(Object value, String label) {
    if (value instanceof ExpressionExpr) {
      return value as ExpressionExpr
    }
    if (value instanceof Number) {
      return new ExpressionNumberExpr(value as Number)
    }
    throw new IllegalArgumentException("${label} must be a number or arithmetic expression")
  }

  static String renderNumber(Number value) {
    BigDecimal decimal = value as BigDecimal
    BigDecimal normalized = decimal.stripTrailingZeros()
    normalized.toPlainString()
  }

  static int precedence(String operator) {
    switch (operator) {
      case '^' -> 3
      case '*', '/' -> 2
      case '+', '-' -> 1
      default -> 0
    }
  }
}

@PackageScope
final class ExpressionVariableExpr extends ExpressionExpr {

  final String name
  final boolean quoted

  ExpressionVariableExpr(String name, boolean quoted = false) {
    this.name = IdentifierRenderingSupport.requireNonBlank(name, 'name')
    this.quoted = quoted
  }

  @Override
  String render() {
    IdentifierRenderingSupport.renderIdentifier(name, quoted)
  }
}

@PackageScope
final class ExpressionNumberExpr extends ExpressionExpr {

  final Number value

  ExpressionNumberExpr(Number value) {
    if (value == null) {
      throw new IllegalArgumentException('value cannot be null')
    }
    this.value = value
  }

  @Override
  String render() {
    renderNumber(value)
  }
}

@PackageScope
final class ExpressionUnaryExpr extends ExpressionExpr {

  final String operator
  final ExpressionExpr expression

  ExpressionUnaryExpr(String operator, ExpressionExpr expression) {
    this.operator = IdentifierRenderingSupport.requireNonBlank(operator, 'operator')
    this.expression = ExpressionExpr.coerce(expression, 'expression')
  }

  @Override
  String render() {
    String inner = expression instanceof ExpressionBinaryExpr ? "(${expression.render()})" : expression.render()
    "${operator}${inner}"
  }
}

@PackageScope
final class ExpressionBinaryExpr extends ExpressionExpr {

  final ExpressionExpr left
  final String operator
  final ExpressionExpr right

  ExpressionBinaryExpr(ExpressionExpr left, String operator, ExpressionExpr right) {
    this.left = ExpressionExpr.coerce(left, 'left')
    this.operator = requireOperator(operator)
    this.right = ExpressionExpr.coerce(right, 'right')
  }

  @Override
  String render() {
    "${formatOperand(left, true)} ${operator} ${formatOperand(right, false)}"
  }

  private String formatOperand(ExpressionExpr expression, boolean leftSide) {
    if (expression instanceof ExpressionBinaryExpr) {
      ExpressionBinaryExpr binary = expression as ExpressionBinaryExpr
      int currentPrecedence = precedence(operator)
      int childPrecedence = precedence(binary.operator)
      boolean needsParentheses = childPrecedence < currentPrecedence ||
        (!leftSide && operator == '^' && childPrecedence == currentPrecedence)
      if (needsParentheses) {
        return "(${binary.render()})"
      }
    }
    expression.render()
  }

  private static String requireOperator(String operator) {
    if (!(operator in ['+', '-', '*', '/', '^'])) {
      throw new IllegalArgumentException("Unsupported arithmetic operator: ${operator}")
    }
    operator
  }
}

@PackageScope
final class ExpressionFunctionExpr extends ExpressionExpr {

  final String name
  final List<ExpressionExpr> arguments

  ExpressionFunctionExpr(String name, List<ExpressionExpr> arguments) {
    this.name = IdentifierRenderingSupport.requireNonBlank(name, 'name')
    this.arguments = copyArguments(arguments)
  }

  @Override
  String render() {
    "${name}(${arguments.collect { ExpressionExpr argument -> argument.render() }.join(', ')})"
  }

  private static List<ExpressionExpr> copyArguments(List<ExpressionExpr> arguments) {
    if (arguments == null || arguments.isEmpty()) {
      throw new IllegalArgumentException('arguments cannot be null or empty')
    }
    if (arguments.any { ExpressionExpr argument -> argument == null }) {
      throw new IllegalArgumentException('arguments cannot contain null values')
    }
    List.copyOf(arguments)
  }
}

@PackageScope
final class ExpressionNumberOperators {

  private ExpressionNumberOperators() {
  }

  static ExpressionExpr plus(Number self, ExpressionExpr other) {
    new ExpressionBinaryExpr(new ExpressionNumberExpr(self), '+', other)
  }

  static ExpressionExpr minus(Number self, ExpressionExpr other) {
    new ExpressionBinaryExpr(new ExpressionNumberExpr(self), '-', other)
  }

  static ExpressionExpr multiply(Number self, ExpressionExpr other) {
    new ExpressionBinaryExpr(new ExpressionNumberExpr(self), '*', other)
  }

  static ExpressionExpr div(Number self, ExpressionExpr other) {
    new ExpressionBinaryExpr(new ExpressionNumberExpr(self), '/', other)
  }

  static ExpressionExpr power(Number self, ExpressionExpr other) {
    new ExpressionBinaryExpr(new ExpressionNumberExpr(self), '^', other)
  }

  static Object remainder(Number self, ExpressionExpr other) {
    throw new IllegalArgumentException("Unsupported arithmetic operator '%' in I { ... }")
  }
}
