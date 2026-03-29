package se.alipsa.matrix.stats.formula

import groovy.transform.CompileStatic

/**
 * Parsed formula containing the raw expression AST for the response and predictors.
 */
@CompileStatic
class ParsedFormula {

  final String source
  final FormulaExpression response
  final FormulaExpression predictors

  /**
   * Creates a parsed formula.
   *
   * @param source the original formula source
   * @param response the parsed response expression
   * @param predictors the parsed predictor expression
   */
  ParsedFormula(String source, FormulaExpression response, FormulaExpression predictors) {
    this.source = source
    this.response = response
    this.predictors = predictors
  }

  /**
   * Normalizes the formula to canonical terms and intercept handling.
   *
   * @return the normalized formula
   */
  NormalizedFormula normalize() {
    FormulaSupport.normalize(this)
  }

  /**
   * Returns the canonical formula string after normalization.
   *
   * @return a canonical normalized formula string
   */
  String normalizedFormulaString() {
    normalize().asFormulaString()
  }

  @Override
  String toString() {
    "${response.asFormulaString()} ~ ${predictors.asFormulaString()}"
  }
}
