package se.alipsa.matrix.stats.formula.dsl

import groovy.transform.CompileDynamic

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

  /**
   * Builds a full formula expression using this term as the response.
   *
   * @param predictors the predictor-side term expression
   * @return the full formula specification
   */
  @CompileDynamic
  GroovyFormulaSpec or(TermExpr predictors) {
    GroovyFormulaSpec.from(this, requireTerm(predictors, 'predictors'))
  }

  @Override
  String render() {
    IdentifierRenderingSupport.renderIdentifier(name, quoted)
  }
}
