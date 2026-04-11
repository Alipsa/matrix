package se.alipsa.matrix.stats.formula.dsl

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
