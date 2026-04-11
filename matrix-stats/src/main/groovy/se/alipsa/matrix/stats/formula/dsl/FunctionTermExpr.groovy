package se.alipsa.matrix.stats.formula.dsl

import groovy.transform.CompileDynamic
import groovy.transform.PackageScope

/**
 * Internal function-style term such as {@code log(x)} or {@code s(x, 6)}.
 *
 * <p>The heterogeneous {@code arguments} list is internal storage for already-validated
 * DSL nodes: {@link TermExpr}, {@link ExpressionExpr}, and numeric literals.</p>
 */
@PackageScope
final class FunctionTermExpr extends TermExpr {

  final String name
  final List<Object> arguments

  FunctionTermExpr(String name, List<Object> arguments) {
    this.name = IdentifierRenderingSupport.requireNonBlank(name, 'name')
    this.arguments = copyArguments(arguments)
  }

  /**
   * Builds a full formula expression using this term as the response.
   *
   * @param predictors the predictor-side term expression
   * @return the full formula specification
   */
  @CompileDynamic
  GroovyFormulaSpec or(TermExpr predictors) {
    new GroovyFormulaSpec(this, requireTerm(predictors, 'predictors'))
  }

  @Override
  String render() {
    "${name}(${arguments.collect { Object argument -> renderArgument(argument) }.join(', ')})"
  }

  private static List<Object> copyArguments(List<Object> arguments) {
    if (arguments == null || arguments.isEmpty()) {
      throw new IllegalArgumentException('arguments cannot be null or empty')
    }
    if (arguments.any { Object argument -> argument == null }) {
      throw new IllegalArgumentException('arguments cannot contain null values')
    }
    List.copyOf(arguments)
  }

  private static String renderArgument(Object argument) {
    if (argument instanceof TermExpr) {
      return (argument as TermExpr).render()
    }
    if (argument instanceof ExpressionExpr) {
      return (argument as ExpressionExpr).render()
    }
    if (argument instanceof Number) {
      return ExpressionExpr.renderNumber(argument as Number)
    }
    throw new IllegalArgumentException("Unsupported function argument type: ${argument.class.simpleName}")
  }
}
