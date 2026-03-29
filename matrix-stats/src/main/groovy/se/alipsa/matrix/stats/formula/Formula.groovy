package se.alipsa.matrix.stats.formula

import groovy.transform.CompileStatic

/**
 * Public entry point for formula parsing, normalization, and updates.
 */
@CompileStatic
class Formula {

  private Formula() {
  }

  /**
   * Parses a formula that explicitly contains a response and predictor side.
   *
   * @param formula the formula source, for example {@code y ~ x + z}
   * @return the parsed formula AST
   */
  static ParsedFormula parse(String formula) {
    FormulaSupport.parse(formula, null)
  }

  /**
   * Parses a formula, defaulting to {@code defaultResponse ~ formula} when no {@code ~} is present.
   *
   * @param formula the formula or predictor-only source
   * @param defaultResponse the default response variable to use when needed
   * @return the parsed formula AST
   */
  static ParsedFormula parse(String formula, String defaultResponse) {
    FormulaSupport.parse(formula, defaultResponse)
  }

  /**
   * Creates a parsed formula directly from response and predictor fragments.
   *
   * @param response the response fragment
   * @param predictors the predictor fragment
   * @return the parsed formula AST
   */
  static ParsedFormula of(String response, String predictors) {
    parse("${response} ~ ${predictors}")
  }

  /**
   * Normalizes a formula to canonical terms and intercept handling.
   *
   * @param formula the formula source
   * @return the normalized formula
   */
  static NormalizedFormula normalize(String formula) {
    parse(formula).normalize()
  }

  /**
   * Normalizes a formula, defaulting to {@code defaultResponse ~ formula} when no {@code ~} is present.
   *
   * @param formula the formula or predictor-only source
   * @param defaultResponse the response to default when needed
   * @return the normalized formula
   */
  static NormalizedFormula normalize(String formula, String defaultResponse) {
    parse(formula, defaultResponse).normalize()
  }

  /**
   * Returns the canonical normalized formula string.
   *
   * @param formula the formula source
   * @return the canonical normalized formula string
   */
  static String canonicalize(String formula) {
    normalize(formula).asFormulaString()
  }

  /**
   * Updates a formula using R-style dot substitution.
   *
   * @param baseFormula the existing formula
   * @param updateFormula the update expression, for example {@code . ~ . - z + w}
   * @return the updated parsed formula
   */
  static ParsedFormula update(String baseFormula, String updateFormula) {
    FormulaSupport.update(baseFormula, updateFormula)
  }
}
