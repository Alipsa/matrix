package se.alipsa.matrix.stats.regression

import groovy.transform.CompileStatic

import se.alipsa.matrix.stats.formula.ModelFrameResult

/**
 * Contract for a named model fitting method. Implementations accept a fully
 * evaluated model frame and return a uniform {@link FitResult}.
 *
 * <p>Call {@link #fit(ModelFrameResult)} for default parameters, or
 * {@link #fit(ModelFrameResult, FitOptions)} with a typed options object
 * (e.g. {@link LoessOptions}, {@link GamOptions}) for method-specific tuning.
 * Both methods are available through the {@link FitRegistry} without
 * downcasting to a concrete class.
 */
@CompileStatic
interface FitMethod {

  /**
   * Fits a model using default parameters.
   *
   * @param frame the evaluated model frame containing design matrix, response, and metadata
   * @return the fit result
   * @throws IllegalArgumentException if the frame is invalid for this method
   * @throws UnsupportedOperationException if the frame carries unsupported metadata (e.g. weights, offsets)
   */
  FitResult fit(ModelFrameResult frame)

  /**
   * Fits a model with method-specific options.
   *
   * @param frame the evaluated model frame
   * @param options method-specific options (e.g. {@link LoessOptions}, {@link GamOptions})
   * @return the fit result
   * @throws IllegalArgumentException if options type is wrong for this method, or frame is invalid
   */
  FitResult fit(ModelFrameResult frame, FitOptions options)
}
