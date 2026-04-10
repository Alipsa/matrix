package se.alipsa.matrix.stats.formula.dsl

/**
 * Dot placeholder expression for later model-frame expansion.
 */
class AllTermsExpr extends TermExpr {

  @Override
  String render() {
    '.'
  }
}
