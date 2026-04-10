package se.alipsa.matrix.stats.formula.dsl

/**
 * Formula interaction expression made up of two or more terms.
 */
class InteractionExpr extends TermExpr {

  final List<TermExpr> terms

  /**
   * Creates an interaction expression.
   *
   * @param terms the terms to interact
   */
  InteractionExpr(List<? extends TermExpr> terms) {
    this.terms = copyTerms(terms)
  }

  @Override
  String render() {
    terms.collect { TermExpr term -> term.render() }.join(':')
  }

  private static List<TermExpr> copyTerms(List<? extends TermExpr> terms) {
    if (terms == null || terms.size() < 2) {
      throw new IllegalArgumentException('interaction(...) requires at least two terms')
    }
    if (terms.any { TermExpr term -> term == null }) {
      throw new IllegalArgumentException('interaction(...) cannot contain null terms')
    }
    List.copyOf(terms)
  }
}
