package se.alipsa.matrix.stats.formula.dsl

import groovy.transform.CompileDynamic

/**
 * Binary additive or subtractive formula term expression.
 */
class BinaryTermExpr extends TermExpr {

  final TermExpr left
  final String operator
  final TermExpr right

  /**
   * Creates a binary term expression.
   *
   * @param left the left term expression
   * @param operator the formula operator
   * @param right the right term expression
   */
  BinaryTermExpr(TermExpr left, String operator, TermExpr right) {
    this.left = requireTerm(left, 'left')
    this.operator = requireOperator(operator)
    this.right = requireTerm(right, 'right')
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
    "${left.render()} ${operator} ${right.render()}"
  }

  private static String requireOperator(String operator) {
    if (!(operator in ['+', '-'])) {
      throw new IllegalArgumentException("Unsupported binary formula operator: ${operator}")
    }
    operator
  }
}
