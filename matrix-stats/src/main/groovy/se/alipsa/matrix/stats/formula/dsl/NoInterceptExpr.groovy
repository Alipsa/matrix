package se.alipsa.matrix.stats.formula.dsl

/**
 * Formula term expression that removes the intercept.
 */
class NoInterceptExpr extends TermExpr {

  @Override
  String render() {
    '0'
  }
}
