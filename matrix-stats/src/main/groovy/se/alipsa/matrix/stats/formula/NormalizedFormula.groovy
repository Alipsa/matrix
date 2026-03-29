package se.alipsa.matrix.stats.formula

import groovy.transform.CompileStatic

/**
 * Canonical formula representation with explicit intercept handling and normalized predictor terms.
 */
@CompileStatic
final class NormalizedFormula {

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
    this.response = requireNonNullValue(response, 'response')
    this.includeIntercept = includeIntercept
    this.predictorTerms = copyTerms(predictorTerms, 'predictorTerms')
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

  private static <T> T requireNonNullValue(T value, String label) {
    if (value == null) {
      throw new IllegalArgumentException("${label} cannot be null")
    }
    value
  }

  private static List<FormulaTerm> copyTerms(List<FormulaTerm> terms, String label) {
    if (terms == null) {
      throw new IllegalArgumentException("${label} cannot be null")
    }
    if (terms.any { FormulaTerm term -> term == null }) {
      throw new IllegalArgumentException("${label} cannot contain null values")
    }
    List.copyOf(terms)
  }
}
