package se.alipsa.matrix.stats.formula

import groovy.transform.CompileStatic

/**
 * Immutable metadata describing the mapping from formula terms to design matrix columns.
 *
 * <p>This object is the single source of truth for downstream model adapters to understand
 * which design matrix columns belong to which predictor term, whether a term is categorical,
 * and what factor levels were detected.
 */
@CompileStatic
final class Terms {

  final FormulaExpression response
  final boolean includeIntercept
  final List<TermInfo> terms

  Terms(FormulaExpression response, boolean includeIntercept, List<TermInfo> terms) {
    this.response = response
    this.includeIntercept = includeIntercept
    this.terms = List.copyOf(terms ?: [])
  }

  /**
   * Metadata for a single predictor term.
   */
  @CompileStatic
  static final class TermInfo {
    final FormulaTerm sourceTerm
    final String label
    final List<String> columns
    final boolean isCategorical
    final List<String> factorLevels
    final boolean isSmooth
    final boolean isDropped
    final String droppedReason

    TermInfo(
      FormulaTerm sourceTerm,
      String label,
      List<String> columns,
      boolean isCategorical,
      List<String> factorLevels,
      boolean isSmooth,
      boolean isDropped,
      String droppedReason
    ) {
      this.sourceTerm = sourceTerm
      this.label = label
      this.columns = List.copyOf(columns ?: [])
      this.isCategorical = isCategorical
      this.factorLevels = List.copyOf(factorLevels ?: [])
      this.isSmooth = isSmooth
      this.isDropped = isDropped
      this.droppedReason = droppedReason
    }
  }
}
