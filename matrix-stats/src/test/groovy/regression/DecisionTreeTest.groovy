package regression

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.regression.DecisionTree

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for the DecisionTree regression class.
 */
class DecisionTreeTest {

  @Test
  void testListBasedConstructor() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
    List<Double> y = [1.0, 1.0, 1.0, 1.0, 5.0, 5.0, 5.0, 5.0]

    def tree = new DecisionTree(x, y)

    assertNotNull(tree, 'DecisionTree should not be null')
    assertTrue(tree.getDepth() >= 1, 'Tree should have at least depth 1')
    assertTrue(tree.getLeafCount() >= 1, 'Tree should have at least 1 leaf')
  }

  @Test
  void testMatrixBasedConstructor() {
    def data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows([
        [1.0, 1.0], [2.0, 1.0], [3.0, 1.0], [4.0, 1.0],
        [5.0, 5.0], [6.0, 5.0], [7.0, 5.0], [8.0, 5.0]
      ])
      .build()

    def tree = new DecisionTree(data, 'x', 'y')

    assertNotNull(tree, 'DecisionTree should not be null')
    assertEquals('x', tree.x, 'X variable name should be set')
    assertEquals('y', tree.y, 'Y variable name should be set')
  }

  @Test
  void testCustomMaxDepthAndMinSamplesLeaf() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
    List<Double> y = [1.0, 1.0, 1.0, 1.0, 5.0, 5.0, 5.0, 5.0]

    def tree = new DecisionTree(x, y, 3, 1)

    assertEquals(3, tree.getMaxDepth(), 'maxDepth should be 3')
    assertEquals(1, tree.getMinSamplesLeaf(), 'minSamplesLeaf should be 1')
    assertTrue(tree.getDepth() <= 3, 'Actual depth should not exceed maxDepth')
  }

  @Test
  void testStepFunctionPerfectFit() {
    // Step function: y = 1 for x <= 4, y = 5 for x > 4
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
    List<Double> y = [1.0, 1.0, 1.0, 1.0, 5.0, 5.0, 5.0, 5.0]

    def tree = new DecisionTree(x, y)

    // Predictions at training points should be correct
    assertEquals(1.0, tree.predict(1.0), 0.001, 'Prediction at x=1 should be 1.0')
    assertEquals(1.0, tree.predict(3.0), 0.001, 'Prediction at x=3 should be 1.0')
    assertEquals(5.0, tree.predict(5.0), 0.001, 'Prediction at x=5 should be 5.0')
    assertEquals(5.0, tree.predict(7.0), 0.001, 'Prediction at x=7 should be 5.0')

    // R² should be 1.0 for perfect fit
    assertEquals(1.0, tree.getRSquared(), 0.001, 'R² should be 1.0 for perfect fit')
  }

  @Test
  void testLinearishDataApproximateFit() {
    // Approximately linear data
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
    List<Double> y = [2.1, 4.2, 5.8, 8.1, 9.9, 12.0, 14.1, 16.2]

    def tree = new DecisionTree(x, y)

    // R² should be reasonable but not perfect
    assertTrue(tree.getRSquared() > 0.5, 'R² should be reasonable')
    assertTrue(tree.getRSquared() <= 1.0, 'R² should not exceed 1.0')
  }

  @Test
  void testConstantYValues() {
    // All Y values are the same
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0]
    List<Double> y = [5.0, 5.0, 5.0, 5.0, 5.0]

    def tree = new DecisionTree(x, y)

    // All predictions should be 5.0
    assertEquals(5.0, tree.predict(1.0), 0.001, 'Prediction should be 5.0')
    assertEquals(5.0, tree.predict(10.0), 0.001, 'Prediction should be 5.0')

    // R² should be 1.0 (no variance to explain)
    assertEquals(1.0, tree.getRSquared(), 0.001, 'R² should be 1.0 for constant Y')

    // MSE should be 0
    assertEquals(0.0, tree.getMse(), 0.001, 'MSE should be 0 for constant Y')
  }

  @Test
  void testSingleValuePrediction() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
    List<Double> y = [1.0, 1.0, 1.0, 1.0, 5.0, 5.0, 5.0, 5.0]

    def tree = new DecisionTree(x, y)

    BigDecimal prediction = tree.predict(2.5)
    assertNotNull(prediction, 'Prediction should not be null')
    assertTrue(prediction >= 0, 'Prediction should be non-negative')
  }

  @Test
  void testSingleValuePredictionWithRounding() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
    List<Double> y = [1.0, 1.0, 1.0, 1.0, 5.0, 5.0, 5.0, 5.0]

    def tree = new DecisionTree(x, y)

    BigDecimal prediction = tree.predict(2.5, 2)
    assertEquals(2, prediction.scale(), 'Prediction should be rounded to 2 decimals')
  }

  @Test
  void testListPredictions() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
    List<Double> y = [1.0, 1.0, 1.0, 1.0, 5.0, 5.0, 5.0, 5.0]

    def tree = new DecisionTree(x, y)

    List<BigDecimal> predictions = tree.predict([1.0, 2.0, 5.0, 6.0])
    assertEquals(4, predictions.size(), 'Should have 4 predictions')
  }

  @Test
  void testListPredictionsWithRounding() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
    List<Double> y = [1.0, 1.0, 1.0, 1.0, 5.0, 5.0, 5.0, 5.0]

    def tree = new DecisionTree(x, y)

    List<BigDecimal> predictions = tree.predict([1.0, 2.0, 5.0, 6.0], 3)
    assertEquals(4, predictions.size(), 'Should have 4 predictions')
    predictions.each { pred ->
      assertEquals(3, pred.scale(), 'Each prediction should be rounded to 3 decimals')
    }
  }

  @Test
  void testRSquaredCalculation() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
    List<Double> y = [1.0, 1.0, 1.0, 1.0, 5.0, 5.0, 5.0, 5.0]

    def tree = new DecisionTree(x, y)

    BigDecimal r2 = tree.getRSquared()
    assertTrue(r2 >= 0 && r2 <= 1, 'R² should be between 0 and 1')
  }

  @Test
  void testRSquaredWithRounding() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
    List<Double> y = [1.0, 1.0, 1.0, 1.0, 5.0, 5.0, 5.0, 5.0]

    def tree = new DecisionTree(x, y)

    BigDecimal r2 = tree.getRSquared(2)
    assertEquals(2, r2.scale(), 'R² should be rounded to 2 decimals')
  }

  @Test
  void testMseRmseMaeCalculation() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
    List<Double> y = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]

    def tree = new DecisionTree(x, y)

    BigDecimal mse = tree.getMse()
    BigDecimal rmse = tree.getRmse()
    BigDecimal mae = tree.getMae()

    assertTrue(mse >= 0, 'MSE should be non-negative')
    assertTrue(rmse >= 0, 'RMSE should be non-negative')
    assertTrue(mae >= 0, 'MAE should be non-negative')

    // RMSE should be sqrt(MSE)
    assertEquals(mse.sqrt(java.math.MathContext.DECIMAL128), rmse, 0.0001, 'RMSE should be sqrt(MSE)')
  }

  @Test
  void testMetricsWithRounding() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
    List<Double> y = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]

    def tree = new DecisionTree(x, y)

    assertEquals(3, tree.getMse(3).scale(), 'MSE should be rounded to 3 decimals')
    assertEquals(3, tree.getRmse(3).scale(), 'RMSE should be rounded to 3 decimals')
    assertEquals(3, tree.getMae(3).scale(), 'MAE should be rounded to 3 decimals')
  }

  @Test
  void testTreeDepthAndLeafCount() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
    List<Double> y = [1.0, 1.0, 1.0, 1.0, 5.0, 5.0, 5.0, 5.0]

    def tree = new DecisionTree(x, y)

    assertTrue(tree.getDepth() >= 0, 'Depth should be non-negative')
    assertTrue(tree.getLeafCount() >= 1, 'Leaf count should be at least 1')
    assertTrue(tree.getDepth() <= tree.getMaxDepth(), 'Actual depth should not exceed maxDepth')
  }

  @Test
  void testMismatchedSizesThrowsException() {
    assertThrows(IllegalArgumentException) {
      new DecisionTree([1.0, 2.0], [1.0, 2.0, 3.0])
    }
  }

  @Test
  void testEmptyDataThrowsException() {
    assertThrows(IllegalArgumentException) {
      new DecisionTree([], [])
    }
  }

  @Test
  void testTooFewPointsThrowsException() {
    assertThrows(IllegalArgumentException) {
      new DecisionTree([1.0], [1.0])
    }
  }

  @Test
  void testNullInputThrowsException() {
    assertThrows(IllegalArgumentException) {
      new DecisionTree(null as List, [1.0, 2.0])
    }

    assertThrows(IllegalArgumentException) {
      new DecisionTree([1.0, 2.0], null as List)
    }
  }

  @Test
  void testInvalidMaxDepthThrowsException() {
    assertThrows(IllegalArgumentException) {
      new DecisionTree([1.0, 2.0, 3.0], [1.0, 2.0, 3.0], 0, 2)
    }
  }

  @Test
  void testInvalidMinSamplesLeafThrowsException() {
    assertThrows(IllegalArgumentException) {
      new DecisionTree([1.0, 2.0, 3.0], [1.0, 2.0, 3.0], 5, 0)
    }
  }

  @Test
  void testAllXValuesIdentical() {
    // All X values are the same - should create a single leaf
    List<Double> x = [5.0, 5.0, 5.0, 5.0, 5.0]
    List<Double> y = [1.0, 2.0, 3.0, 4.0, 5.0]

    def tree = new DecisionTree(x, y)

    // Should have only 1 leaf since no split is possible
    assertEquals(1, tree.getLeafCount(), 'Should have 1 leaf when all X values are identical')

    // Prediction should be mean of Y values
    assertEquals(3.0, tree.predict(5.0), 0.001, 'Prediction should be mean of Y values')
  }

  @Test
  void testMaxDepthOne_Stump() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
    List<Double> y = [1.0, 1.0, 1.0, 1.0, 5.0, 5.0, 5.0, 5.0]

    def tree = new DecisionTree(x, y, 1, 1)

    assertEquals(1, tree.getMaxDepth(), 'maxDepth should be 1')
    assertEquals(1, tree.getDepth(), 'Actual depth should be 1')
    assertEquals(2, tree.getLeafCount(), 'Stump should have 2 leaves')
  }

  @Test
  void testLargeMinSamplesLeafForcesShallowTree() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
    List<Double> y = [1.0, 1.0, 1.0, 1.0, 5.0, 5.0, 5.0, 5.0]

    // With minSamplesLeaf = 8, no split should be possible
    def tree = new DecisionTree(x, y, 5, 8)

    assertEquals(1, tree.getLeafCount(), 'Should have 1 leaf when minSamplesLeaf equals data size')
    assertEquals(0, tree.getDepth(), 'Depth should be 0 when no splits are possible')
  }

  @Test
  void testToString() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
    List<Double> y = [1.0, 1.0, 1.0, 1.0, 5.0, 5.0, 5.0, 5.0]

    def tree = new DecisionTree(x, y)

    String str = tree.toString()
    assertTrue(str.contains('DecisionTree'), 'toString should contain class name')
    assertTrue(str.contains('depth='), 'toString should contain depth')
    assertTrue(str.contains('leaves='), 'toString should contain leaf count')
    assertTrue(str.contains('R²='), 'toString should contain R²')
  }

  @Test
  void testSummary() {
    def data = Matrix.builder()
      .columnNames(['feature', 'target'])
      .rows([
        [1.0, 1.0], [2.0, 1.0], [3.0, 1.0], [4.0, 1.0],
        [5.0, 5.0], [6.0, 5.0], [7.0, 5.0], [8.0, 5.0]
      ])
      .build()

    def tree = new DecisionTree(data, 'feature', 'target')

    String summary = tree.summary()
    assertTrue(summary.contains('Decision Tree Regression'), 'Summary should contain title')
    assertTrue(summary.contains('Parameters:'), 'Summary should contain parameters section')
    assertTrue(summary.contains('maxDepth:'), 'Summary should contain maxDepth')
    assertTrue(summary.contains('minSamplesLeaf:'), 'Summary should contain minSamplesLeaf')
    assertTrue(summary.contains('Training Metrics:'), 'Summary should contain metrics section')
    assertTrue(summary.contains('R²:'), 'Summary should contain R²')
    assertTrue(summary.contains('MSE:'), 'Summary should contain MSE')
    assertTrue(summary.contains('RMSE:'), 'Summary should contain RMSE')
    assertTrue(summary.contains('MAE:'), 'Summary should contain MAE')
    assertTrue(summary.contains('Tree Rules:'), 'Summary should contain tree rules section')
    assertTrue(summary.contains('feature'), 'Summary should use variable name from Matrix')
  }

  @Test
  void testExtrapolationBeyondTrainingRange() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
    List<Double> y = [1.0, 1.0, 1.0, 1.0, 5.0, 5.0, 5.0, 5.0]

    def tree = new DecisionTree(x, y)

    // Prediction outside training range should still work
    BigDecimal predLow = tree.predict(-10.0)
    BigDecimal predHigh = tree.predict(100.0)

    assertNotNull(predLow, 'Prediction at low value should not be null')
    assertNotNull(predHigh, 'Prediction at high value should not be null')

    // Should extrapolate to nearest leaf
    assertEquals(1.0, predLow, 0.001, 'Low extrapolation should be 1.0')
    assertEquals(5.0, predHigh, 0.001, 'High extrapolation should be 5.0')
  }

  @Test
  void testIntegerInputs() {
    List<Integer> x = [1, 2, 3, 4, 5, 6, 7, 8]
    List<Integer> y = [1, 1, 1, 1, 5, 5, 5, 5]

    def tree = new DecisionTree(x, y)

    assertNotNull(tree, 'Should handle integer inputs')
    assertEquals(1.0, tree.predict(2), 0.001, 'Prediction should work with integer input')
    assertEquals(5.0, tree.predict(6), 0.001, 'Prediction should work with integer input')
  }

  @Test
  void testMixedNumericTypes() {
    List<Number> x = [1, 2.0, 3L, 4.0f, 5, 6.0, 7, 8.0]
    List<Number> y = [1, 1, 1, 1, 5, 5, 5, 5]

    def tree = new DecisionTree(x, y)

    assertNotNull(tree, 'Should handle mixed numeric types')
    assertTrue(tree.getRSquared() > 0, 'R² should be positive')
  }
}
