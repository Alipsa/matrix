package se.alipsa.matrix.stats.formula

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix

/**
 * Immutable result of model frame evaluation, containing the expanded design matrix
 * and associated metadata.
 */
@CompileStatic
final class ModelFrameResult {

  final Matrix data
  final List<Number> response
  final String responseName
  final boolean includeIntercept
  final List<String> predictorNames
  final List<Number> weights
  final List<Number> offset
  final List<Integer> droppedRows
  final NormalizedFormula formula

  /**
   * Creates a model frame result.
   *
   * @param data expanded design matrix containing only predictor columns
   * @param response response column values
   * @param responseName name of the response column
   * @param includeIntercept whether the intercept should be included
   * @param predictorNames column names in the design matrix
   * @param weights weight values aligned to surviving rows, or null
   * @param offset offset values aligned to surviving rows, or null
   * @param droppedRows original row indices removed by subset and NA handling
   * @param formula the fully resolved normalized formula
   */
  ModelFrameResult(
    Matrix data,
    List<Number> response,
    String responseName,
    boolean includeIntercept,
    List<String> predictorNames,
    List<Number> weights,
    List<Number> offset,
    List<Integer> droppedRows,
    NormalizedFormula formula
  ) {
    this.data = requireNonNull(data, 'data')
    this.response = List.copyOf(requireNonNull(response, 'response'))
    this.responseName = requireNonBlank(responseName, 'responseName')
    this.includeIntercept = includeIntercept
    this.predictorNames = List.copyOf(requireNonNull(predictorNames, 'predictorNames'))
    this.weights = weights != null ? List.copyOf(weights) : null
    this.offset = offset != null ? List.copyOf(offset) : null
    this.droppedRows = List.copyOf(requireNonNull(droppedRows, 'droppedRows'))
    this.formula = requireNonNull(formula, 'formula')
  }

  private static <T> T requireNonNull(T value, String label) {
    if (value == null) {
      throw new IllegalArgumentException("${label} cannot be null")
    }
    value
  }

  private static String requireNonBlank(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("${label} cannot be null or blank")
    }
    value
  }
}
