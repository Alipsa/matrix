import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import se.alipsa.groovy.stats.Accuracy

class AccuracyTest {

  @Test
  void testEvaluatePredictions() {
    def actuals = [4, 16, 34, 20, 40, 42, 32, 54, 70, 85]
    def preds = [9.627845, 13.886057, 35.177120, 43.693545, 47.951757, 56.468182, 64.984607,
                 77.759245, 82.017457, 86.275670]
    def accuracy = Accuracy.evaluatePredictions(actuals, preds)

    Assertions.assertEquals(12.5069370, accuracy['mae'] as double, 0.00001 as double, 'mae')
    Assertions.assertEquals(267.0002421, accuracy['mse'] as double, 0.00001 as double, 'mse')
    Assertions.assertEquals(16.3401420, accuracy['rmse'] as double, 0.00001 as double, 'rmse')
    Assertions.assertEquals(0.4959096, accuracy['mape'] as double, 0.00001 as double, 'mape')

  }
}
