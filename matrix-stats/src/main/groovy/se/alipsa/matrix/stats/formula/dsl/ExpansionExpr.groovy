package se.alipsa.matrix.stats.formula.dsl

import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Formula shorthand interaction expansion expression.
 */
class ExpansionExpr extends TermExpr {

  final TermExpr base
  final int degree

  /**
   * Creates a shorthand expansion expression.
   *
   * @param base the base expression to expand
   * @param degree the positive integer expansion degree
   */
  ExpansionExpr(TermExpr base, Number degree) {
    this.base = requireTerm(base, 'base')
    this.degree = NumericConversion.toExactInt(degree, 'degree')
    if (this.degree < 1) {
      throw new IllegalArgumentException("degree must be positive, got ${degree}")
    }
  }

  @Override
  String render() {
    "(${base.render()})^${degree}"
  }
}
