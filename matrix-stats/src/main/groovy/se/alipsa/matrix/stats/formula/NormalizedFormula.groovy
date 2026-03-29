package se.alipsa.matrix.stats.formula

import groovy.transform.CompileStatic

/**
 * Canonical formula representation with explicit intercept handling and normalized predictor terms.
 */
@CompileStatic
class NormalizedFormula {

  final FormulaExpression response
  final boolean includeIntercept
  final List<FormulaTerm> predictorTerms

  /**
   * Creates a normalized formula.
   *
   * @param response the normalized single response expression
   * @param includeIntercept whether the intercept is included
   * @param predictorTerms the canonical predictor terms
   */
  NormalizedFormula(FormulaExpression response, boolean includeIntercept, List<FormulaTerm> predictorTerms) {
    this.response = response
    this.includeIntercept = includeIntercept
    this.predictorTerms = List.copyOf(predictorTerms)
  }

  /**
   * Returns the canonical formula string.
   *
   * @return the normalized formula string
   */
  String asFormulaString() {
    List<String> parts = [includeIntercept ? '1' : '0']
    parts.addAll(predictorTerms.collect { FormulaTerm term -> term.asFormulaString() })
    "${response.asFormulaString()} ~ ${parts.join(' + ')}"
  }

  @Override
  String toString() {
    asFormulaString()
  }
}
