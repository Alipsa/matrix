package se.alipsa.matrix.stats.formula

import groovy.transform.CompileStatic

/**
 * A normalized predictor term made up of one or more factors.
 */
@CompileStatic
final class FormulaTerm {

  final List<FormulaExpression> factors

  /**
   * Creates a normalized term from a list of factor expressions.
   *
   * @param factors the factors included in the term
   */
  FormulaTerm(List<FormulaExpression> factors) {
    if (factors == null || factors.isEmpty()) {
      throw new IllegalArgumentException('A formula term must contain at least one factor')
    }
    Map<String, FormulaExpression> orderedFactors = [:]
    for (FormulaExpression factor : factors) {
      orderedFactors[factor.asFormulaString()] = factor
    }
    List<String> keys = orderedFactors.keySet().toList().sort()
    this.factors = List.copyOf(keys.collect { String key -> orderedFactors[key] })
  }

  /**
   * Creates a main-effect term from a single factor.
   *
   * @param factor the factor to wrap
   * @return a single-factor term
   */
  static FormulaTerm of(FormulaExpression factor) {
    new FormulaTerm([factor])
  }

  /**
   * Combines multiple normalized terms into a single interaction term.
   *
   * @param terms the terms to combine
   * @return the combined interaction term
   */
  static FormulaTerm combine(Collection<FormulaTerm> terms) {
    new FormulaTerm(terms.collectMany { FormulaTerm term -> term.factors })
  }

  /**
   * Returns the canonical formula representation for this term.
   *
   * @return the term label
   */
  String asFormulaString() {
    factors.collect { FormulaExpression factor -> factor.asFormulaString() }.join(':')
  }

  /**
   * Returns whether the term represents an interaction.
   *
   * @return {@code true} when more than one factor is present
   */
  boolean isInteraction() {
    factors.size() > 1
  }

  @Override
  String toString() {
    asFormulaString()
  }

  @Override
  boolean equals(Object other) {
    if (this.is(other)) {
      return true
    }
    if (!(other instanceof FormulaTerm)) {
      return false
    }
    asFormulaString() == (other as FormulaTerm).asFormulaString()
  }

  @Override
  int hashCode() {
    asFormulaString().hashCode()
  }
}
