package se.alipsa.matrix.stats.formula.dsl

import groovy.transform.CompileDynamic

import se.alipsa.matrix.stats.formula.FormulaParseException

/**
 * Full Groovy formula DSL expression containing response and predictor sides.
 */
@CompileDynamic
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
   * Creates a formula specification from response and predictor terms.
   *
   * @param response the response term
   * @param predictors the predictor terms
   * @return the formula specification
   */
  static GroovyFormulaSpec from(TermExpr response, TermExpr predictors) {
    new GroovyFormulaSpec(response, predictors)
  }

  /**
   * Renders this specification to R-style formula syntax.
   *
   * @return the rendered formula source
   */
  String render() {
    if (!(response instanceof TermRef)) {
      String renderedDsl = "${response.render()} | ${predictors.render()}"
      if (response instanceof FunctionTermExpr) {
        throw new FormulaParseException(
          "Transformed responses are not supported: ${renderedDsl}. Move transforms to the predictor side.",
          0
        )
      }
      throw new FormulaParseException(
        "Response must be a single variable in the Groovy formula DSL: ${renderedDsl}",
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
