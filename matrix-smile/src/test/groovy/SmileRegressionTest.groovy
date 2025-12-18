import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.smile.ml.SmileRegression

import static org.junit.jupiter.api.Assertions.*

class SmileRegressionTest {

  // Simple linear relationship: y = 2*x1 + 3*x2 + 1
  private Matrix createLinearData() {
    List<Double> x1 = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0]
    List<Double> x2 = [2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0]
    List<Double> y = x1.indices.collect { i ->
      2.0 * x1[i] + 3.0 * x2[i] + 1.0
    }

    return Matrix.builder()
        .data(
            x1: x1,
            x2: x2,
            y: y
        )
        .types([Double, Double, Double])
        .build()
  }

  // Data with some noise for more realistic testing
  private Matrix createNoisyData() {
    Random rand = new Random(42)
    List<Double> x1 = (1..20).collect { it as Double }
    List<Double> x2 = (1..20).collect { (it * 0.5) as Double }
    List<Double> y = x1.indices.collect { i ->
      2.0 * x1[i] + 3.0 * x2[i] + 1.0 + (rand.nextGaussian() * 0.5)
    }

    return Matrix.builder()
        .data(
            x1: x1,
            x2: x2,
            y: y
        )
        .types([Double, Double, Double])
        .build()
  }

  private Matrix createTestData() {
    return Matrix.builder()
        .data(
            x1: [2.5, 5.5, 8.5],
            x2: [3.5, 6.5, 9.5],
            y: [16.5, 31.5, 46.5] // Expected: 2*x1 + 3*x2 + 1
        )
        .types([Double, Double, Double])
        .build()
  }

  // OLS Tests

  @Test
  void testOLSRegression() {
    Matrix trainData = createLinearData()

    SmileRegression regression = SmileRegression.ols(trainData, 'y')

    assertNotNull(regression)
    assertEquals('y', regression.targetColumn)
    assertEquals(2, regression.featureColumns.length)
    assertTrue(regression.featureColumns.contains('x1'))
    assertTrue(regression.featureColumns.contains('x2'))
  }

  @Test
  void testOLSPredict() {
    Matrix trainData = createLinearData()
    Matrix testData = createTestData()

    SmileRegression regression = SmileRegression.ols(trainData, 'y')
    Matrix predictions = regression.predict(testData)

    assertEquals(3, predictions.rowCount())
    assertTrue(predictions.columnNames().contains('prediction'))
    assertEquals(4, predictions.columnCount()) // 3 original + 1 prediction
  }

  @Test
  void testOLSPredictValues() {
    Matrix trainData = createLinearData()
    Matrix testData = createTestData()

    SmileRegression regression = SmileRegression.ols(trainData, 'y')
    double[] predictions = regression.predictValues(testData)

    assertEquals(3, predictions.length)
    // Predictions should be close to expected values
    for (int i = 0; i < predictions.length; i++) {
      double expected = (testData['y'] as List<Double>)[i]
      assertEquals(expected, predictions[i], 0.1, "Prediction $i should be close to $expected")
    }
  }

  @Test
  void testOLSRSquared() {
    Matrix data = createLinearData()

    SmileRegression regression = SmileRegression.ols(data, 'y')
    double r2 = regression.rSquared(data)

    // Perfect linear fit should have R² ≈ 1.0
    assertTrue(r2 >= 0.99, "Expected R² >= 0.99 but got $r2")
  }

  @Test
  void testOLSMSE() {
    Matrix data = createLinearData()

    SmileRegression regression = SmileRegression.ols(data, 'y')
    double mse = regression.mse(data)

    // Perfect fit should have MSE ≈ 0
    assertTrue(mse < 0.01, "Expected MSE < 0.01 but got $mse")
  }

  @Test
  void testOLSRMSE() {
    Matrix data = createLinearData()

    SmileRegression regression = SmileRegression.ols(data, 'y')
    double rmse = regression.rmse(data)

    // RMSE should be sqrt(MSE)
    double mse = regression.mse(data)
    assertEquals(Math.sqrt(mse), rmse, 0.0001)
  }

  @Test
  void testOLSMAE() {
    Matrix data = createLinearData()

    SmileRegression regression = SmileRegression.ols(data, 'y')
    double mae = regression.mae(data)

    // Perfect fit should have MAE ≈ 0
    assertTrue(mae < 0.01, "Expected MAE < 0.01 but got $mae")
  }

  @Test
  void testOLSEvaluate() {
    Matrix data = createLinearData()

    SmileRegression regression = SmileRegression.ols(data, 'y')
    Matrix eval = regression.evaluate(data)

    assertEquals(4, eval.rowCount())
    assertTrue(eval.columnNames().contains('metric'))
    assertTrue(eval.columnNames().contains('value'))

    List<String> metrics = eval.column('metric') as List<String>
    assertTrue(metrics.contains('R²'))
    assertTrue(metrics.contains('MSE'))
    assertTrue(metrics.contains('RMSE'))
    assertTrue(metrics.contains('MAE'))
  }

  @Test
  void testOLSCoefficients() {
    Matrix trainData = createLinearData()

    SmileRegression regression = SmileRegression.ols(trainData, 'y')
    double[] coefficients = regression.coefficients

    assertNotNull(coefficients)
    assertEquals(2, coefficients.length)
  }

  @Test
  void testOLSIntercept() {
    Matrix trainData = createLinearData()

    SmileRegression regression = SmileRegression.ols(trainData, 'y')
    double intercept = regression.intercept

    assertFalse(Double.isNaN(intercept))
  }

  // Ridge Regression Tests

  @Test
  void testRidgeRegression() {
    Matrix trainData = createNoisyData()

    SmileRegression regression = SmileRegression.ridge(trainData, 'y', 0.1)

    assertNotNull(regression)
    assertEquals('y', regression.targetColumn)
    assertEquals(2, regression.featureColumns.length)
  }

  @Test
  void testRidgeRegressionPredict() {
    Matrix trainData = createNoisyData()
    Matrix testData = createTestData()

    SmileRegression regression = SmileRegression.ridge(trainData, 'y', 0.1)
    double[] predictions = regression.predictValues(testData)

    assertEquals(3, predictions.length)
    // Predictions should be reasonable
    for (double pred : predictions) {
      assertTrue(pred > 0, "Prediction should be positive")
    }
  }

  @Test
  void testRidgeRegressionRSquared() {
    Matrix data = createNoisyData()

    SmileRegression regression = SmileRegression.ridge(data, 'y', 0.1)
    double r2 = regression.rSquared(data)

    // Should have good fit even with regularization
    assertTrue(r2 >= 0.9, "Expected R² >= 0.9 but got $r2")
  }

  @Test
  void testRidgeCoefficients() {
    Matrix trainData = createNoisyData()

    SmileRegression regression = SmileRegression.ridge(trainData, 'y', 0.1)
    double[] coefficients = regression.coefficients

    assertNotNull(coefficients)
    assertEquals(2, coefficients.length)
  }

  // LASSO Regression Tests

  @Test
  void testLassoRegression() {
    Matrix trainData = createNoisyData()

    SmileRegression regression = SmileRegression.lasso(trainData, 'y', 0.1)

    assertNotNull(regression)
    assertEquals('y', regression.targetColumn)
    assertEquals(2, regression.featureColumns.length)
  }

  @Test
  void testLassoRegressionPredict() {
    Matrix trainData = createNoisyData()
    Matrix testData = createTestData()

    SmileRegression regression = SmileRegression.lasso(trainData, 'y', 0.1)
    double[] predictions = regression.predictValues(testData)

    assertEquals(3, predictions.length)
    for (double pred : predictions) {
      assertTrue(pred > 0, "Prediction should be positive")
    }
  }

  @Test
  void testLassoRegressionRSquared() {
    Matrix data = createNoisyData()

    SmileRegression regression = SmileRegression.lasso(data, 'y', 0.1)
    double r2 = regression.rSquared(data)

    // Should have reasonable fit
    assertTrue(r2 >= 0.8, "Expected R² >= 0.8 but got $r2")
  }

  @Test
  void testLassoCoefficients() {
    Matrix trainData = createNoisyData()

    SmileRegression regression = SmileRegression.lasso(trainData, 'y', 0.1)
    double[] coefficients = regression.coefficients

    assertNotNull(coefficients)
    assertEquals(2, coefficients.length)
  }

  // ElasticNet Regression Tests

  @Test
  void testElasticNetRegression() {
    Matrix trainData = createNoisyData()

    SmileRegression regression = SmileRegression.elasticNet(trainData, 'y', 0.1, 0.1)

    assertNotNull(regression)
    assertEquals('y', regression.targetColumn)
    assertEquals(2, regression.featureColumns.length)
  }

  @Test
  void testElasticNetRegressionPredict() {
    Matrix trainData = createNoisyData()
    Matrix testData = createTestData()

    SmileRegression regression = SmileRegression.elasticNet(trainData, 'y', 0.1, 0.1)
    double[] predictions = regression.predictValues(testData)

    assertEquals(3, predictions.length)
    for (double pred : predictions) {
      assertTrue(pred > 0, "Prediction should be positive")
    }
  }

  @Test
  void testElasticNetRegressionRSquared() {
    Matrix data = createNoisyData()

    SmileRegression regression = SmileRegression.elasticNet(data, 'y', 0.1, 0.1)
    double r2 = regression.rSquared(data)

    // Should have reasonable fit
    assertTrue(r2 >= 0.8, "Expected R² >= 0.8 but got $r2")
  }

  @Test
  void testElasticNetCoefficients() {
    Matrix trainData = createNoisyData()

    SmileRegression regression = SmileRegression.elasticNet(trainData, 'y', 0.1, 0.1)
    double[] coefficients = regression.coefficients

    assertNotNull(coefficients)
    assertEquals(2, coefficients.length)
  }

  // Default Parameters Tests

  @Test
  void testRidgeDefaultLambda() {
    Matrix trainData = createNoisyData()

    // Should work with default lambda = 1.0
    SmileRegression regression = SmileRegression.ridge(trainData, 'y')

    assertNotNull(regression)
    double r2 = regression.rSquared(trainData)
    assertTrue(r2 > 0, "R² should be positive")
  }

  @Test
  void testLassoDefaultLambda() {
    Matrix trainData = createNoisyData()

    // Should work with default lambda = 1.0
    SmileRegression regression = SmileRegression.lasso(trainData, 'y')

    assertNotNull(regression)
    double r2 = regression.rSquared(trainData)
    assertTrue(r2 > 0, "R² should be positive")
  }

  @Test
  void testElasticNetDefaultLambdas() {
    Matrix trainData = createNoisyData()

    // Should work with default lambdas = 0.5, 0.5
    SmileRegression regression = SmileRegression.elasticNet(trainData, 'y')

    assertNotNull(regression)
    double r2 = regression.rSquared(trainData)
    assertTrue(r2 > 0, "R² should be positive")
  }

  // Edge Cases

  @Test
  void testGetFeatureColumns() {
    Matrix trainData = createLinearData()

    SmileRegression regression = SmileRegression.ols(trainData, 'y')
    String[] features = regression.featureColumns

    assertEquals(2, features.length)
    assertTrue(features.contains('x1'))
    assertTrue(features.contains('x2'))
  }

  @Test
  void testGetTargetColumn() {
    Matrix trainData = createLinearData()

    SmileRegression regression = SmileRegression.ols(trainData, 'y')

    assertEquals('y', regression.targetColumn)
  }
}
