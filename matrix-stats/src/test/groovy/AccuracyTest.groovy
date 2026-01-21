import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.Accuracy

import static org.junit.jupiter.api.Assertions.*

class AccuracyTest {

  @Test
  void testEvaluatePredictions() {
    def actuals = [4, 16, 34, 20, 40, 42, 32, 54, 70, 85]
    def preds = [9.627845, 13.886057, 35.177120, 43.693545, 47.951757, 56.468182, 64.984607,
                 77.759245, 82.017457, 86.275670]
    def accuracy = Accuracy.evaluatePredictions(actuals, preds)

    assertEquals(12.5069370, accuracy['mae'] as double, 0.00001 as double, 'mae')
    assertEquals(267.0002421, accuracy['mse'] as double, 0.00001 as double, 'mse')
    assertEquals(16.3401420, accuracy['rmse'] as double, 0.00001 as double, 'rmse')
    assertEquals(0.4959096, accuracy['mape'] as double, 0.00001 as double, 'mape')
  }

  @Test
  void testMae() {
    def actuals = [4, 16, 34, 20, 40, 42, 32, 54, 70, 85]
    def preds = [9.627845, 13.886057, 35.177120, 43.693545, 47.951757, 56.468182, 64.984607,
                 77.759245, 82.017457, 86.275670]

    BigDecimal mae = Accuracy.mae(actuals, preds)
    assertEquals(12.5069370, mae as double, 0.00001 as double, 'mae')
  }

  @Test
  void testMse() {
    def actuals = [4, 16, 34, 20, 40, 42, 32, 54, 70, 85]
    def preds = [9.627845, 13.886057, 35.177120, 43.693545, 47.951757, 56.468182, 64.984607,
                 77.759245, 82.017457, 86.275670]

    BigDecimal mse = Accuracy.mse(actuals, preds)
    assertEquals(267.0002421, mse as double, 0.00001 as double, 'mse')
  }

  @Test
  void testRmse() {
    def actuals = [4, 16, 34, 20, 40, 42, 32, 54, 70, 85]
    def preds = [9.627845, 13.886057, 35.177120, 43.693545, 47.951757, 56.468182, 64.984607,
                 77.759245, 82.017457, 86.275670]

    BigDecimal rmse = Accuracy.rmse(actuals, preds)
    assertEquals(16.3401420, rmse as double, 0.00001 as double, 'rmse')
  }

  @Test
  void testMape() {
    def actuals = [4, 16, 34, 20, 40, 42, 32, 54, 70, 85]
    def preds = [9.627845, 13.886057, 35.177120, 43.693545, 47.951757, 56.468182, 64.984607,
                 77.759245, 82.017457, 86.275670]

    BigDecimal mape = Accuracy.mape(actuals, preds)
    assertEquals(0.4959096, mape as double, 0.00001 as double, 'mape')
  }

  @Test
  void testSmape() {
    def actuals = [10, 20, 30, 40, 50]
    def preds = [12, 18, 28, 45, 48]

    BigDecimal smape = Accuracy.smape(actuals, preds)
    // SMAPE = (1/5) * (2*2/(10+12) + 2*2/(20+18) + 2*2/(30+28) + 2*5/(40+45) + 2*2/(50+48))
    // = (1/5) * (4/22 + 4/38 + 4/58 + 10/85 + 4/98)
    // = (1/5) * (0.1818 + 0.1053 + 0.0690 + 0.1176 + 0.0408)
    // = (1/5) * 0.5145 = 0.1029
    assertEquals(0.1029, smape as double, 0.001 as double, 'smape')
  }

  @Test
  void testBias() {
    // Predictions consistently over-estimate
    def actuals = [10, 20, 30, 40, 50]
    def preds = [12, 22, 32, 42, 52]

    BigDecimal bias = Accuracy.bias(actuals, preds)
    assertEquals(2.0, bias as double, 0.001 as double, 'bias should be positive for over-prediction')

    // Predictions consistently under-estimate
    def preds2 = [8, 18, 28, 38, 48]
    BigDecimal bias2 = Accuracy.bias(actuals, preds2)
    assertEquals(-2.0, bias2 as double, 0.001 as double, 'bias should be negative for under-prediction')
  }

  @Test
  void testMaxError() {
    def actuals = [10, 20, 30, 40, 50]
    def preds = [12, 18, 35, 41, 48]

    BigDecimal maxErr = Accuracy.maxError(actuals, preds)
    // Errors: |10-12|=2, |20-18|=2, |30-35|=5, |40-41|=1, |50-48|=2
    // Max = 5
    assertEquals(5.0, maxErr as double, 0.001 as double, 'maxError')
  }

  @Test
  void testMedianAbsoluteError() {
    def actuals = [10, 20, 30, 40, 50]
    def preds = [12, 18, 35, 41, 48]

    BigDecimal medianAE = Accuracy.medianAbsoluteError(actuals, preds)
    // Errors: 2, 2, 5, 1, 2
    // Sorted: 1, 2, 2, 2, 5
    // Median = 2
    assertEquals(2.0, medianAE as double, 0.001 as double, 'medianAbsoluteError')
  }

  @Test
  void testR2PerfectFit() {
    def actuals = [10, 20, 30, 40, 50]
    def preds = [10, 20, 30, 40, 50]  // Perfect predictions

    BigDecimal r2 = Accuracy.r2(actuals, preds)
    assertEquals(1.0, r2 as double, 0.001 as double, 'R² should be 1 for perfect fit')
  }

  @Test
  void testR2ReasonableFit() {
    def actuals = [4, 16, 34, 20, 40, 42, 32, 54, 70, 85]
    def preds = [9.627845, 13.886057, 35.177120, 43.693545, 47.951757, 56.468182, 64.984607,
                 77.759245, 82.017457, 86.275670]

    BigDecimal r2 = Accuracy.r2(actuals, preds)
    // For this data, R² should be positive but less than 1
    assertTrue(r2 > 0.0, 'R² should be positive for reasonable predictions')
    assertTrue(r2 < 1.0, 'R² should be less than 1 for imperfect predictions')
    // Expected R² ≈ 0.514 for this dataset
    assertEquals(0.514, r2 as double, 0.01 as double, 'R² approximate value')
  }

  @Test
  void testR2WorseThanMean() {
    def actuals = [10, 20, 30, 40, 50]
    def preds = [50, 10, 40, 20, 30]  // Random predictions

    BigDecimal r2 = Accuracy.r2(actuals, preds)
    // R² can be negative when predictions are worse than just using the mean
    assertTrue(r2 < 0.5, 'R² should be low for poor predictions')
  }

  @Test
  void testR2ConstantActuals() {
    def actuals = [25, 25, 25, 25, 25]  // All same value
    def preds = [25, 25, 25, 25, 25]

    BigDecimal r2 = Accuracy.r2(actuals, preds)
    assertEquals(1.0, r2 as double, 0.001 as double, 'R² should be 1 when all actuals are constant and predicted perfectly')
  }
}
