package se.alipsa.matrix.stats.formula.dsl

/**
 * Reference to a formula variable or backtick-quoted column name.
 */
class TermRef extends TermExpr {

  final String name
  final boolean quoted

  /**
   * Creates a term reference.
   *
   * @param name the column or variable name
   * @param quoted whether the name must be rendered with backticks
   */
  TermRef(String name, boolean quoted = false) {
    this.name = IdentifierRenderingSupport.requireNonBlank(name, 'name')
    this.quoted = quoted
  }

  @Override
  String render() {
    IdentifierRenderingSupport.renderIdentifier(name, quoted)
  }
}
