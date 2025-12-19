import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.smile.ml.SmileDimensionality

import static org.junit.jupiter.api.Assertions.*

class SmileDimensionalityTest {

  // Create test data with 4 features
  private Matrix createTestData() {
    return Matrix.builder()
        .data(
            x1: [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0],
            x2: [1.1, 2.1, 3.1, 4.1, 5.1, 6.1, 7.1, 8.1, 9.1, 10.1],
            x3: [2.0, 4.0, 6.0, 8.0, 10.0, 12.0, 14.0, 16.0, 18.0, 20.0],
            x4: [0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0]
        )
        .types([Double, Double, Double, Double])
        .build()
  }

  // Create iris-like test data with higher dimensionality
  private Matrix createIrisLikeData() {
    return Matrix.builder()
        .data(
            sepal_length: [5.1, 4.9, 4.7, 5.0, 5.4, 7.0, 6.4, 6.9, 5.5, 6.5],
            sepal_width: [3.5, 3.0, 3.2, 3.6, 3.9, 3.2, 3.2, 3.1, 2.3, 2.8],
            petal_length: [1.4, 1.4, 1.3, 1.4, 1.7, 4.7, 4.5, 4.9, 4.0, 4.6],
            petal_width: [0.2, 0.2, 0.2, 0.2, 0.4, 1.4, 1.5, 1.5, 1.3, 1.5]
        )
        .types([Double, Double, Double, Double])
        .build()
  }

  // PCA Tests

  @Test
  void testPCABasic() {
    Matrix data = createTestData()

    SmileDimensionality pca = SmileDimensionality.pca(data, 2)

    assertNotNull(pca)
    assertEquals(2, pca.numComponents)
    assertEquals(4, pca.featureColumns.length)
  }

  @Test
  void testPCATransform() {
    Matrix data = createTestData()

    SmileDimensionality pca = SmileDimensionality.pca(data, 2)
    Matrix transformed = pca.transform(data)

    assertEquals(10, transformed.rowCount())
    assertEquals(2, transformed.columnCount())
    assertTrue(transformed.columnNames().contains('PC1'))
    assertTrue(transformed.columnNames().contains('PC2'))
  }

  @Test
  void testPCATransformValues() {
    Matrix data = createTestData()

    SmileDimensionality pca = SmileDimensionality.pca(data, 2)
    double[][] transformed = pca.transformValues(data)

    assertEquals(10, transformed.length)
    assertEquals(2, transformed[0].length)
  }

  @Test
  void testPCAVariance() {
    Matrix data = createTestData()

    SmileDimensionality pca = SmileDimensionality.pca(data, 2)
    double[] variance = pca.variance

    assertNotNull(variance)
    assertTrue(variance.length > 0)
    // Variances should be positive
    for (double v : variance) {
      assertTrue(v >= 0, "Variance should be non-negative")
    }
  }

  @Test
  void testPCAVarianceProportion() {
    Matrix data = createTestData()

    SmileDimensionality pca = SmileDimensionality.pca(data, 2)
    double[] proportion = pca.varianceProportion

    assertNotNull(proportion)
    // Proportions should sum to approximately 1.0
    double sum = 0
    for (double p : proportion) {
      sum += p
      assertTrue(p >= 0 && p <= 1, "Proportion should be between 0 and 1")
    }
    assertEquals(1.0, sum, 0.01)
  }

  @Test
  void testPCACumulativeVarianceProportion() {
    Matrix data = createTestData()

    SmileDimensionality pca = SmileDimensionality.pca(data, 2)
    double[] cumulative = pca.cumulativeVarianceProportion

    assertNotNull(cumulative)
    // Last cumulative should be 1.0
    assertEquals(1.0, cumulative[cumulative.length - 1], 0.01)
    // Should be monotonically increasing
    for (int i = 1; i < cumulative.length; i++) {
      assertTrue(cumulative[i] >= cumulative[i - 1])
    }
  }

  @Test
  void testPCALoadings() {
    Matrix data = createTestData()

    SmileDimensionality pca = SmileDimensionality.pca(data, 2)
    double[][] loadings = pca.loadings

    assertNotNull(loadings)
    assertEquals(4, loadings.length) // 4 features
  }

  @Test
  void testPCALoadingsMatrix() {
    Matrix data = createTestData()

    SmileDimensionality pca = SmileDimensionality.pca(data, 2)
    Matrix loadingsMatrix = pca.loadingsMatrix

    assertNotNull(loadingsMatrix)
    assertEquals(4, loadingsMatrix.rowCount()) // 4 features
    assertTrue(loadingsMatrix.columnNames().contains('feature'))
    assertTrue(loadingsMatrix.columnNames().contains('PC1'))
  }

  @Test
  void testPCAVarianceSummary() {
    Matrix data = createTestData()

    SmileDimensionality pca = SmileDimensionality.pca(data, 2)
    Matrix summary = pca.varianceSummary()

    assertNotNull(summary)
    assertTrue(summary.columnNames().contains('component'))
    assertTrue(summary.columnNames().contains('variance'))
    assertTrue(summary.columnNames().contains('proportion'))
    assertTrue(summary.columnNames().contains('cumulative'))
  }

  @Test
  void testPCAByVariance() {
    Matrix data = createIrisLikeData()

    // Retain 95% of variance
    SmileDimensionality pca = SmileDimensionality.pcaByVariance(data, 0.95)

    assertNotNull(pca)
    assertTrue(pca.numComponents > 0)
    assertTrue(pca.numComponents <= 4)
  }

  @Test
  void testPCACorrelation() {
    Matrix data = createIrisLikeData()

    SmileDimensionality pca = SmileDimensionality.pcaCorrelation(data, 2)

    assertNotNull(pca)
    assertEquals(2, pca.numComponents)
  }

  @Test
  void testPCACenter() {
    Matrix data = createTestData()

    SmileDimensionality pca = SmileDimensionality.pca(data, 2)
    double[] center = pca.center

    assertNotNull(center)
    assertEquals(4, center.length) // 4 features
  }

  @Test
  void testPCAGetFeatureColumns() {
    Matrix data = createTestData()

    SmileDimensionality pca = SmileDimensionality.pca(data, 2)
    String[] features = pca.featureColumns

    assertEquals(4, features.length)
    assertTrue(features.contains('x1'))
    assertTrue(features.contains('x2'))
    assertTrue(features.contains('x3'))
    assertTrue(features.contains('x4'))
  }

  // Edge Cases

  @Test
  void testPCASingleComponent() {
    Matrix data = createTestData()

    SmileDimensionality pca = SmileDimensionality.pca(data, 1)
    Matrix transformed = pca.transform(data)

    assertEquals(10, transformed.rowCount())
    assertEquals(1, transformed.columnCount())
    assertTrue(transformed.columnNames().contains('PC1'))
  }

  @Test
  void testPCAAllComponents() {
    Matrix data = createTestData()

    SmileDimensionality pca = SmileDimensionality.pca(data, 4)
    Matrix transformed = pca.transform(data)

    assertEquals(10, transformed.rowCount())
    assertEquals(4, transformed.columnCount())
  }

  @Test
  void testPCAByVarianceInvalidRange() {
    Matrix data = createTestData()

    assertThrows(IllegalArgumentException) {
      SmileDimensionality.pcaByVariance(data, 0.0)
    }

    assertThrows(IllegalArgumentException) {
      SmileDimensionality.pcaByVariance(data, 1.5)
    }
  }

  @Test
  void testPCADimensionReduction() {
    Matrix data = createIrisLikeData()

    SmileDimensionality pca = SmileDimensionality.pca(data, 2)
    Matrix transformed = pca.transform(data)

    // Original data has 4 features, transformed has 2
    assertEquals(4, data.columnCount())
    assertEquals(2, transformed.columnCount())
    assertEquals(data.rowCount(), transformed.rowCount())
  }
}
