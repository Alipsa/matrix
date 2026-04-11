package se.alipsa.matrix.stats.formula.dsl

import se.alipsa.matrix.stats.formula.FormulaParseException

/**
 * Full Groovy formula DSL expression containing response and predictor sides.
 */
class GroovyFormulaSpec {

  final TermExpr response
  final TermExpr predictors

  /**
   * Creates a formula specification.
   *
   * @param response the response term
   * @param predictors the predictor terms
   */
  GroovyFormulaSpec(TermExpr response, TermExpr predictors) {
    this.response = requireResponse(response)
    this.predictors = requirePredictors(predictors)
  }

  /**
   * Renders this specification to R-style formula syntax.
   *
   * @return the rendered formula source
   */
  String render() {
    if (response instanceof FunctionTermExpr) {
      throw new FormulaParseException(
        "Transformed responses are not supported: ${response.render()} | ${predictors.render()}. Move transforms to the predictor side.",
        0
      )
    }
    "${response.render()} ~ ${predictors.render()}"
  }

  @Override
  String toString() {
    render()
  }

  private static TermExpr requireResponse(TermExpr response) {
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
