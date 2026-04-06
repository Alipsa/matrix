package se.alipsa.matrix.stats

import se.alipsa.matrix.core.Stat

import java.math.MathContext

/**
 * Statistical accuracy metrics for evaluating predictions against actual values.
 * All methods accept lists of actual and predicted values and return accuracy measures.
 */
@SuppressWarnings(['DuplicateNumberLiteral', 'VariableName'])
class Accuracy {

  static final MathContext PRECISION = new MathContext(9)

  /**
   * Evaluate predictions against actuals and return all common accuracy metrics.
   * Percentage-based metrics reject zero actual values because the percentage error is undefined.
   *
   * @param actuals List of actual values
   * @param predictions List of predicted values
   * @return Map containing mae, mse, rmse, and mape
   */
  static Map<String, BigDecimal> evaluatePredictions(List actuals, List predictions) {
    validatePredictionInputs(actuals, predictions)
    validateNonZeroActuals(actuals, 'evaluatePredictions')

    def sae = 0.0G
    def sse = 0.0G
    def m = 0.0G
    def N = actuals.size() as BigDecimal
    actuals.eachWithIndex { Object entry, int i ->
      def act = entry as BigDecimal
      def pred = predictions[i] as BigDecimal
      sae += (act - pred).abs()
      sse += (act - pred)**2
      m += ((act-pred)/act).abs()/N
    }
    [mae: sae/N, mse: sse/N, rmse: (sse/N).sqrt(PRECISION), mape: m]
  }

  /**
   * Calculate the Mean Absolute Error (MAE).
   * MAE measures the average magnitude of errors in predictions, treating all errors equally.
   * Formula: MAE = (1/n) * Σ|actual - predicted|
   *
   * @param actuals List of actual values
   * @param predictions List of predicted values
   * @return Mean absolute error
   */
  static BigDecimal mae(List actuals, List predictions) {
    validatePredictionInputs(actuals, predictions)

    def sae = 0.0G
    def N = actuals.size() as BigDecimal
    actuals.eachWithIndex { Object entry, int i ->
      def act = entry as BigDecimal
      def pred = predictions[i] as BigDecimal
      sae += (act - pred).abs()
    }
    sae / N
  }

  /**
   * Calculate the Mean Squared Error (MSE).
   * MSE measures the average of squared errors, giving more weight to large errors.
   * Formula: MSE = (1/n) * Σ(actual - predicted)²
   *
   * @param actuals List of actual values
   * @param predictions List of predicted values
   * @return Mean squared error
   */
  static BigDecimal mse(List actuals, List predictions) {
    validatePredictionInputs(actuals, predictions)

    def sse = 0.0G
    def N = actuals.size() as BigDecimal
    actuals.eachWithIndex { Object entry, int i ->
      def act = entry as BigDecimal
      def pred = predictions[i] as BigDecimal
      sse += (act - pred)**2
    }
    sse / N
  }

  /**
   * Calculate the Root Mean Squared Error (RMSE).
   * RMSE is the square root of MSE and is in the same units as the predicted values.
   * It is more sensitive to large errors than MAE.
   * Formula: RMSE = √(MSE)
   *
   * @param actuals List of actual values
   * @param predictions List of predicted values
   * @return Root mean squared error
   */
  static BigDecimal rmse(List actuals, List predictions) {
    mse(actuals, predictions).sqrt(PRECISION)
  }

  /**
   * Calculate the Mean Absolute Percentage Error (MAPE).
   * MAPE expresses error as a percentage of actual values, useful for comparing forecasts
   * across different scales. This implementation rejects zero actual values because the
   * percentage error is undefined in that case.
   * Formula: MAPE = (1/n) * Σ|(actual - predicted) / actual|
   *
   * @param actuals List of actual values
   * @param predictions List of predicted values
   * @return Mean absolute percentage error (as a decimal, multiply by 100 for percentage)
   */
  static BigDecimal mape(List actuals, List predictions) {
    validatePredictionInputs(actuals, predictions)
    validateNonZeroActuals(actuals, 'mape')

    def m = 0.0G
    def N = actuals.size() as BigDecimal
    actuals.eachWithIndex { Object entry, int i ->
      def act = entry as BigDecimal
      def pred = predictions[i] as BigDecimal
      m += ((act - pred) / act).abs() / N
    }
    m
  }

  /**
   * Calculate the Symmetric Mean Absolute Percentage Error (SMAPE).
   * SMAPE is a variation of MAPE that handles zero values better and is bounded between 0 and 2.
   * Formula: SMAPE = (1/n) * Σ(2 * |actual - predicted| / (|actual| + |predicted|))
   *
   * @param actuals List of actual values
   * @param predictions List of predicted values
   * @return Symmetric mean absolute percentage error (as a decimal, multiply by 100 for percentage)
   */
  static BigDecimal smape(List actuals, List predictions) {
    validatePredictionInputs(actuals, predictions)

    def s = 0.0G
    def N = actuals.size() as BigDecimal
    actuals.eachWithIndex { Object entry, int i ->
      def act = entry as BigDecimal
      def pred = predictions[i] as BigDecimal
      def numerator = 2 * (act - pred).abs()
      def denominator = act.abs() + pred.abs()
      if (denominator != 0) {
        s += numerator / denominator
      }
    }
    s / N
  }

  /**
   * Calculate the bias (mean error).
   * Bias measures systematic over-prediction (positive) or under-prediction (negative).
   * Unlike MAE, errors don't cancel out, revealing directional bias in predictions.
   * Formula: Bias = (1/n) * Σ(predicted - actual)
   *
   * @param actuals List of actual values
   * @param predictions List of predicted values
   * @return Mean error (bias)
   */
  static BigDecimal bias(List actuals, List predictions) {
    validatePredictionInputs(actuals, predictions)

    def sumError = 0.0G
    def N = actuals.size() as BigDecimal
    actuals.eachWithIndex { Object entry, int i ->
      def act = entry as BigDecimal
      def pred = predictions[i] as BigDecimal
      sumError += (pred - act)
    }
    sumError / N
  }

  /**
   * Calculate the maximum absolute error.
   * MaxError identifies the worst-case prediction error, useful for understanding
   * the upper bound of prediction errors.
   * Formula: MaxError = max(|actual - predicted|)
   *
   * @param actuals List of actual values
   * @param predictions List of predicted values
   * @return Maximum absolute error
   */
  static BigDecimal maxError(List actuals, List predictions) {
    validatePredictionInputs(actuals, predictions)

    def maxErr = 0.0G
    actuals.eachWithIndex { Object entry, int i ->
      def act = entry as BigDecimal
      def pred = predictions[i] as BigDecimal
      def err = (act - pred).abs()
      if (err > maxErr) {
        maxErr = err
      }
    }
    maxErr
  }

  /**
   * Calculate the median absolute error.
   * Median absolute error is more robust to outliers than MAE, providing a better
   * measure of typical error when the error distribution has heavy tails.
   * Formula: MedianAE = median(|actual - predicted|)
   *
   * @param actuals List of actual values
   * @param predictions List of predicted values
   * @return Median absolute error
   */
  static BigDecimal medianAbsoluteError(List actuals, List predictions) {
    validatePredictionInputs(actuals, predictions)

    List<BigDecimal> errors = []
    actuals.eachWithIndex { Object entry, int i ->
      def act = entry as BigDecimal
      def pred = predictions[i] as BigDecimal
      errors << (act - pred).abs()
    }
    Stat.median(errors)
  }

  /**
   * Calculate the coefficient of determination (R²).
   * R² measures the proportion of variance in actuals explained by predictions.
   * R² = 1 indicates perfect predictions, R² = 0 means predictions are no better than
   * the mean, and negative R² indicates predictions are worse than using the mean.
   * Formula: R² = 1 - (SS_res / SS_tot) where SS_res = Σ(actual - predicted)²
   * and SS_tot = Σ(actual - mean(actual))²
   *
   * @param actuals List of actual values
   * @param predictions List of predicted values
   * @return Coefficient of determination (R²)
   */
  static BigDecimal r2(List actuals, List predictions) {
    validatePredictionInputs(actuals, predictions)

    BigDecimal mean = Stat.mean(actuals)
    def ssRes = 0.0G  // Residual sum of squares
    def ssTot = 0.0G  // Total sum of squares

    actuals.eachWithIndex { Object entry, int i ->
      def act = entry as BigDecimal
      def pred = predictions[i] as BigDecimal
      ssRes += (act - pred) ** 2
      ssTot += (act - mean) ** 2
    }

    if (ssTot == 0) {
      return 1.0G  // Perfect fit if all actuals are the same
    }

    1 - (ssRes / ssTot)
  }

  private static void validatePredictionInputs(List actuals, List predictions) {
    if (actuals == null || predictions == null) {
      throw new IllegalArgumentException('Actuals and predictions cannot be null')
    }
    if (actuals.size() != predictions.size()) {
      throw new IllegalArgumentException("Actuals and predictions must have the same size (got ${actuals.size()} and ${predictions.size()})")
    }
    if (actuals.isEmpty()) {
      throw new IllegalArgumentException('Actuals and predictions cannot be empty')
    }
  }

  private static void validateNonZeroActuals(List actuals, String methodName) {
    actuals.eachWithIndex { Object entry, int i ->
      BigDecimal actual = entry as BigDecimal
      if (actual == 0) {
        throw new IllegalArgumentException("Actual values must be non-zero for ${methodName} (found 0 at index ${i})")
      }
    }
  }
}
