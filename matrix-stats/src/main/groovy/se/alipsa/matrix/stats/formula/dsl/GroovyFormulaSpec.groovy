package se.alipsa.matrix.stats.formula.dsl

/**
 * Full Groovy formula DSL expression containing response and predictor sides.
 */
class GroovyFormulaSpec {

  final TermRef response
  final TermExpr predictors

  /**
   * Creates a formula specification.
   *
   * @param response the response term
   * @param predictors the predictor terms
   */
  GroovyFormulaSpec(TermRef response, TermExpr predictors) {
    this.response = response
    this.predictors = TermExpr.requireTerm(predictors, 'predictors')
  }

  /**
   * Renders this specification to R-style formula syntax.
   *
   * @return the rendered formula source
   */
  String render() {
    "${response.render()} ~ ${predictors.render()}"
  }

  @Override
  String toString() {
    render()
  }
}
