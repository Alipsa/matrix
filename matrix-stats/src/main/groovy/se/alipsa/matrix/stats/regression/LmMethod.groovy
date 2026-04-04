package se.alipsa.matrix.stats.regression

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.formula.ModelFrameResult

/**
 * Ordinary least squares linear model adapter. Delegates to {@link MultipleLinearRegression}
 * after prepending an intercept column when requested by the model frame.
 *
 * <p>Weights and offsets are not yet supported. If the model frame carries non-null
 * weights or offsets, an {@link UnsupportedOperationException} is thrown.
 */
@CompileStatic
class LmMethod implements FitMethod {

  @Override
  FitResult fit(ModelFrameResult frame) {
    if (frame.weights != null) {
      throw new UnsupportedOperationException('lm does not yet support weights')
    }
    if (frame.offset != null) {
      throw new UnsupportedOperationException('lm does not yet support offsets')
    }
    int n = frame.response.size()
    Matrix designMatrix = frame.data
    boolean addIntercept = frame.includeIntercept

    List<String> names = FitUtils.buildPredictorNames(addIntercept, frame.predictorNames)
    double[][] predictors = FitUtils.buildDesignArray(designMatrix, n, addIntercept)
    double[] response = FitUtils.toDoubleArray(frame.response)

    MultipleLinearRegression mlr = new MultipleLinearRegression(response, predictors)

    double[] coefficients = mlr.coefficients
    double[] standardErrors = mlr.standardErrors
    double[] fittedValues = FitUtils.computeFitted(predictors, coefficients)
    double[] residuals = FitUtils.computeResiduals(response, fittedValues)
    double rSquared = FitUtils.computeRSquared(response, residuals)

    new FitResult(coefficients, standardErrors, fittedValues, residuals, rSquared, names)
  }

  @Override
  FitResult fit(ModelFrameResult frame, FitOptions options) {
    fit(frame) // lm has no method-specific options
  }
}
