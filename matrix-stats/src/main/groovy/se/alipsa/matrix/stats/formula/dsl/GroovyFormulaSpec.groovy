package se.alipsa.matrix.stats.formula.dsl

import se.alipsa.matrix.stats.formula.FormulaParseException

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
    this.response = requireResponse(response)
    this.predictors = requirePredictors(predictors)
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

  private static TermRef requireResponse(TermRef response) {
    if (response == null) {
      throw new FormulaParseException('Formula expression must define a response term such as y | x + group', 0)
    }
    response
  }

  private static TermExpr requirePredictors(TermExpr predictors) {
    if (predictors == null) {
      throw new FormulaParseException('Formula expression must define predictor terms such as y | x + group', 0)
    }
    predictors
  }
}
