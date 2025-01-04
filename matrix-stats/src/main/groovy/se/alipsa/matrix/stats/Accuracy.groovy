package se.alipsa.matrix.stats

import java.math.MathContext

class Accuracy {

  static final MathContext precision = new MathContext(9)

  static Map<String, BigDecimal> evaluatePredictions(List actuals, List predictions) {
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
    [mae: sae/N, mse: sse/N, rmse: (sse/N).sqrt(precision), mape: m]
  }

  /**
   * Calculate the Mean Absolute Percentage Error
   * Mean Absolute Percentage Error (MAPE) is a common method for calculating sales forecast accuracy.
   * It’s calculated by taking the difference between your forecast and the actual value,
   * and then dividing that difference by the actual value.
   *
   * @param forecast
   * @param actual
   * @return
   */
  static BigDecimal mape(BigDecimal forecast, BigDecimal actual) {
    (forecast - actual) / actual
  }

  /**
   * Calculate the Mean Absolute Error.
   * The Mean Absolute Error (MAE) is another way to measure the accuracy of an estimate.
   * It’s calculated by taking the difference between your forecast and the actual value,
   * and then dividing that difference by the square root of your sample size.
   *
   * @param forecast
   * @param actual
   * @param sampleSize
   * @return
   */
  static BigDecimal mae(BigDecimal forecast, BigDecimal actual, BigInteger sampleSize) {
    return (forecast - actual) / sampleSize.sqrt()
  }

  /**
   * Calculate the Root Mean Squared Error.
   * Root Mean Squared Error (RMSE) is the square root of MAE.
   * RMSE has usually considered a better indicator than MAPE or MAE because it can be used to compare models
   * with vastly different lengths—MAPE and MAE depend on actual value, but RMSE doesn’t.
   *
   * @param forecast
   * @param actual
   * @param sampleSize
   * @return
   */
  static BigDecimal rmse(BigDecimal forecast, BigDecimal actual, BigInteger sampleSize) {
    return mae(forecast, actual, sampleSize).sqrt(MathContext.DECIMAL32)
  }
}
