package se.alipsa.matrix.stats.formula

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix

/**
 * Evaluates a formula against a Matrix dataset to produce an expanded design matrix.
 *
 * <p>Use the builder API to configure the evaluation:
 * <pre>
 * ModelFrameResult result = ModelFrame.of('y ~ x + z', data)
 *   .weights('w')
 *   .naAction(NaAction.OMIT)
 *   .evaluate()
 * </pre>
 */
@CompileStatic
final class ModelFrame {

  private ModelFrame() {
  }

  /**
   * Creates a model frame builder from a formula string and data.
   *
   * @param formula the formula source, for example {@code y ~ x + z}
   * @param data the input dataset
   * @return a new builder
   */
  static ModelFrame of(String formula, Matrix data) {
    throw new UnsupportedOperationException('Not yet implemented')
  }

  /**
   * Creates a model frame builder from a pre-normalized formula and data.
   *
   * @param formula the normalized formula
   * @param data the input dataset
   * @return a new builder
   */
  static ModelFrame of(NormalizedFormula formula, Matrix data) {
    throw new UnsupportedOperationException('Not yet implemented')
  }

  /**
   * Evaluates the formula against the data and returns the result.
   *
   * @return the model frame result
   */
  ModelFrameResult evaluate() {
    throw new UnsupportedOperationException('Not yet implemented')
  }
}
