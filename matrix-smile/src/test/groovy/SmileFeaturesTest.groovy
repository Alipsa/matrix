import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.smile.data.SmileFeatures

import static org.junit.jupiter.api.Assertions.*

class SmileFeaturesTest {

  private Matrix createNumericMatrix() {
    return Matrix.builder()
        .data(
            x: [1.0, 2.0, 3.0, 4.0, 5.0],
            y: [10.0, 20.0, 30.0, 40.0, 50.0],
            z: [100.0, 200.0, 300.0, 400.0, 500.0]
        )
        .types([Double, Double, Double])
        .build()
  }

  private Matrix createMixedMatrix() {
    return Matrix.builder()
        .data(
            id: [1, 2, 3, 4, 5],
            value: [10.0, 20.0, 30.0, 40.0, 50.0],
            category: ['A', 'B', 'A', 'C', 'B']
        )
        .types([Integer, Double, String])
        .build()
  }

  // ============ standardize Tests ============

  @Test
  void testStandardizeAllColumns() {
    Matrix data = createNumericMatrix()

    Matrix standardized = SmileFeatures.standardize(data)

    assertEquals(5, standardized.rowCount())
    assertEquals(3, standardized.columnCount())

    // Each column should have mean ≈ 0 and std ≈ 1
    for (String colName : standardized.columnNames()) {
      List<Double> col = standardized.column(colName) as List<Double>
      double mean = col.sum() / col.size()
      assertEquals(0.0d, mean, 0.0001d, "Mean of $colName should be 0")
    }
  }

  @Test
  void testStandardizeSpecificColumns() {
    Matrix data = createNumericMatrix()

    Matrix standardized = SmileFeatures.standardize(data, ['x', 'y'])

    // x and y should be standardized
    List<Double> xCol = standardized.column('x') as List<Double>
    double meanX = xCol.sum() / xCol.size()
    assertEquals(0.0d, meanX, 0.0001d)

    // z should remain unchanged
    assertEquals([100.0, 200.0, 300.0, 400.0, 500.0], standardized.column('z'))
  }

  @Test
  void testStandardizeSingleColumn() {
    Matrix data = createNumericMatrix()

    Matrix standardized = SmileFeatures.standardize(data, 'x')

    List<Double> xCol = standardized.column('x') as List<Double>
    double meanX = xCol.sum() / xCol.size()
    assertEquals(0.0d, meanX, 0.0001d)

    // Other columns unchanged
    assertEquals([10.0, 20.0, 30.0, 40.0, 50.0], standardized.column('y'))
  }

  @Test
  void testStandardizeZeroVariance() {
    Matrix data = Matrix.builder()
        .data(x: [5.0, 5.0, 5.0, 5.0, 5.0])
        .types([Double])
        .build()

    Matrix standardized = SmileFeatures.standardize(data)

    // All values should be 0 when variance is 0
    List<Double> col = standardized.column('x') as List<Double>
    for (double val : col) {
      assertEquals(0.0d, val, 0.0001d)
    }
  }

  // ============ normalize Tests ============

  @Test
  void testNormalizeAllColumns() {
    Matrix data = createNumericMatrix()

    Matrix normalized = SmileFeatures.normalize(data)

    assertEquals(5, normalized.rowCount())

    // Each column should have min = 0 and max = 1
    for (String colName : normalized.columnNames()) {
      List<Double> col = normalized.column(colName) as List<Double>
      assertEquals(0.0d, col.min(), 0.0001d)
      assertEquals(1.0d, col.max(), 0.0001d)
    }
  }

  @Test
  void testNormalizeSpecificColumns() {
    Matrix data = createNumericMatrix()

    Matrix normalized = SmileFeatures.normalize(data, ['x'])

    List<Double> xCol = normalized.column('x') as List<Double>
    assertEquals(0.0d, xCol.min(), 0.0001d)
    assertEquals(1.0d, xCol.max(), 0.0001d)

    // y should remain unchanged
    assertEquals([10.0, 20.0, 30.0, 40.0, 50.0], normalized.column('y'))
  }

  @Test
  void testNormalizeToCustomRange() {
    Matrix data = createNumericMatrix()

    Matrix normalized = SmileFeatures.normalize(data, ['x'], -1.0, 1.0)

    List<Double> col = normalized.column('x') as List<Double>
    assertEquals(-1.0d, col.min(), 0.0001d)
    assertEquals(1.0d, col.max(), 0.0001d)
  }

  // ============ oneHotEncode Tests ============

  @Test
  void testOneHotEncodeBasic() {
    Matrix data = createMixedMatrix()

    Matrix encoded = SmileFeatures.oneHotEncode(data, 'category')

    // Original column should be replaced by 3 binary columns (A, B, C)
    assertFalse(encoded.columnNames().contains('category'))
    assertTrue(encoded.columnNames().contains('category_A'))
    assertTrue(encoded.columnNames().contains('category_B'))
    assertTrue(encoded.columnNames().contains('category_C'))
  }

  @Test
  void testOneHotEncodeValues() {
    Matrix data = createMixedMatrix()

    Matrix encoded = SmileFeatures.oneHotEncode(data, 'category')

    // Check first row (category = 'A')
    assertEquals(1, encoded['category_A'][0])
    assertEquals(0, encoded['category_B'][0])
    assertEquals(0, encoded['category_C'][0])

    // Check second row (category = 'B')
    assertEquals(0, encoded['category_A'][1])
    assertEquals(1, encoded['category_B'][1])
    assertEquals(0, encoded['category_C'][1])
  }

  @Test
  void testOneHotEncodeKeepOriginal() {
    Matrix data = createMixedMatrix()

    Matrix encoded = SmileFeatures.oneHotEncode(data, 'category', false)

    // Original column should still exist
    assertTrue(encoded.columnNames().contains('category'))
    assertTrue(encoded.columnNames().contains('category_A'))
  }

  @Test
  void testOneHotEncodeMultipleColumns() {
    Matrix data = Matrix.builder()
        .data(
            x: [1, 2, 3],
            cat1: ['A', 'B', 'A'],
            cat2: ['X', 'Y', 'X']
        )
        .types([Integer, String, String])
        .build()

    Matrix encoded = SmileFeatures.oneHotEncode(data, ['cat1', 'cat2'])

    assertTrue(encoded.columnNames().contains('cat1_A'))
    assertTrue(encoded.columnNames().contains('cat1_B'))
    assertTrue(encoded.columnNames().contains('cat2_X'))
    assertTrue(encoded.columnNames().contains('cat2_Y'))
  }

  // ============ labelEncode Tests ============

  @Test
  void testLabelEncodeBasic() {
    Matrix data = createMixedMatrix()

    Matrix encoded = SmileFeatures.labelEncode(data, 'category')

    // Category should now be integers
    assertEquals(Integer, encoded.type(encoded.columnNames().indexOf('category')))

    // Values should be 0, 1, 2 for A, B, C
    List<Integer> labels = encoded.column('category') as List<Integer>
    assertEquals([0, 1, 0, 2, 1], labels) // A=0, B=1, C=2 (sorted alphabetically)
  }

  @Test
  void testLabelEncodeMultipleColumns() {
    Matrix data = Matrix.builder()
        .data(
            x: [1, 2, 3],
            cat1: ['A', 'B', 'A'],
            cat2: ['X', 'Y', 'X']
        )
        .types([Integer, String, String])
        .build()

    Matrix encoded = SmileFeatures.labelEncode(data, ['cat1', 'cat2'])

    assertEquals([0, 1, 0], encoded.column('cat1'))
    assertEquals([0, 1, 0], encoded.column('cat2'))
  }

  // ============ logTransform Tests ============

  @Test
  void testLogTransform() {
    Matrix data = createNumericMatrix()

    Matrix transformed = SmileFeatures.logTransform(data, ['x'])

    List<Double> col = transformed.column('x') as List<Double>
    assertEquals(Math.log1p(1.0), col[0], 0.0001d)
    assertEquals(Math.log1p(2.0), col[1], 0.0001d)
    assertEquals(Math.log1p(3.0), col[2], 0.0001d)
  }

  @Test
  void testLogTransformWithZero() {
    Matrix data = Matrix.builder()
        .data(x: [0.0, 1.0, 2.0])
        .types([Double])
        .build()

    Matrix transformed = SmileFeatures.logTransform(data, 'x')

    List<Double> col = transformed.column('x') as List<Double>
    assertEquals(0.0d, col[0], 0.0001d) // log1p(0) = 0
  }

  // ============ sqrtTransform Tests ============

  @Test
  void testSqrtTransform() {
    Matrix data = Matrix.builder()
        .data(x: [1.0, 4.0, 9.0, 16.0])
        .types([Double])
        .build()

    Matrix transformed = SmileFeatures.sqrtTransform(data, ['x'])

    List<Double> col = transformed.column('x') as List<Double>
    assertEquals(1.0d, col[0], 0.0001d)
    assertEquals(2.0d, col[1], 0.0001d)
    assertEquals(3.0d, col[2], 0.0001d)
    assertEquals(4.0d, col[3], 0.0001d)
  }

  // ============ powerTransform Tests ============

  @Test
  void testPowerTransform() {
    Matrix data = Matrix.builder()
        .data(x: [1.0, 2.0, 3.0])
        .types([Double])
        .build()

    Matrix transformed = SmileFeatures.powerTransform(data, ['x'], 2.0)

    List<Double> col = transformed.column('x') as List<Double>
    assertEquals(1.0d, col[0], 0.0001d)
    assertEquals(4.0d, col[1], 0.0001d)
    assertEquals(9.0d, col[2], 0.0001d)
  }

  // ============ binning Tests ============

  @Test
  void testBinningEqualWidth() {
    Matrix data = Matrix.builder()
        .data(x: [1.0, 5.0, 10.0, 15.0, 20.0])
        .types([Double])
        .build()

    Matrix binned = SmileFeatures.binning(data, 'x', 4)

    List<Integer> bins = binned.column('x') as List<Integer>
    assertEquals(0, bins[0]) // 1 in first bin
    assertEquals(0, bins[1]) // 5 in first bin
    assertEquals(1, bins[2]) // 10 in second bin
    assertEquals(2, bins[3]) // 15 in third bin
    assertEquals(3, bins[4]) // 20 in fourth bin
  }

  @Test
  void testBinningCustomEdges() {
    Matrix data = Matrix.builder()
        .data(x: [5.0, 15.0, 25.0, 35.0])
        .types([Double])
        .build()

    Matrix binned = SmileFeatures.binning(data, 'x', [0.0, 10.0, 20.0, 30.0, 40.0], ['low', 'medium', 'high', 'very high'])

    List<String> bins = binned.column('x') as List<String>
    assertEquals('low', bins[0])
    assertEquals('medium', bins[1])
    assertEquals('high', bins[2])
    assertEquals('very high', bins[3])
  }

  // ============ fillna Tests ============

  @Test
  void testFillnaConstant() {
    Matrix data = Matrix.builder()
        .data(x: [1.0, null, 3.0, null, 5.0])
        .types([Double])
        .build()

    Matrix filled = SmileFeatures.fillna(data, 'x', 0.0)

    assertEquals([1.0, 0.0, 3.0, 0.0, 5.0], filled.column('x'))
  }

  @Test
  void testFillnaMean() {
    Matrix data = Matrix.builder()
        .data(x: [1.0, null, 3.0, null, 5.0])
        .types([Double])
        .build()

    Matrix filled = SmileFeatures.fillnaMean(data, 'x')

    // Mean of [1, 3, 5] = 3
    List<?> actual = filled.column('x') as List<?>
    assertEquals(5, actual.size())
    assertEquals(1.0d, (actual[0] as Number).doubleValue(), 0.0001d)
    assertEquals(3.0d, (actual[1] as Number).doubleValue(), 0.0001d)
    assertEquals(3.0d, (actual[2] as Number).doubleValue(), 0.0001d)
    assertEquals(3.0d, (actual[3] as Number).doubleValue(), 0.0001d)
    assertEquals(5.0d, (actual[4] as Number).doubleValue(), 0.0001d)
  }

  @Test
  void testFillnaMedian() {
    Matrix data = Matrix.builder()
        .data(x: [1.0, null, 2.0, null, 100.0])
        .types([Double])
        .build()

    Matrix filled = SmileFeatures.fillnaMedian(data, 'x')

    // Median of [1, 2, 100] = 2
    List<?> actual = filled.column('x') as List<?>
    assertEquals(5, actual.size())
    assertEquals(1.0d, (actual[0] as Number).doubleValue(), 0.0001d)
    assertEquals(2.0d, (actual[1] as Number).doubleValue(), 0.0001d)
    assertEquals(2.0d, (actual[2] as Number).doubleValue(), 0.0001d)
    assertEquals(2.0d, (actual[3] as Number).doubleValue(), 0.0001d)
    assertEquals(100.0d, (actual[4] as Number).doubleValue(), 0.0001d)
  }

  // ============ dropna Tests ============

  @Test
  void testDropnaAll() {
    Matrix data = Matrix.builder()
        .data(
            x: [1.0, null, 3.0, 4.0, null],
            y: [10.0, 20.0, null, 40.0, 50.0]
        )
        .types([Double, Double])
        .build()

    Matrix cleaned = SmileFeatures.dropna(data)

    assertEquals(2, cleaned.rowCount()) // Only rows 0 and 3 have no nulls
    assertEquals([1.0, 4.0], cleaned.column('x'))
    assertEquals([10.0, 40.0], cleaned.column('y'))
  }

  @Test
  void testDropnaSpecificColumns() {
    Matrix data = Matrix.builder()
        .data(
            x: [1.0, null, 3.0, 4.0],
            y: [10.0, 20.0, null, 40.0]
        )
        .types([Double, Double])
        .build()

    Matrix cleaned = SmileFeatures.dropna(data, ['x'])

    assertEquals(3, cleaned.rowCount()) // Rows 0, 2, 3 have non-null x
    assertEquals([1.0, 3.0, 4.0], cleaned.column('x'))
  }

  // ============ StandardScaler Tests ============

  @Test
  void testStandardScalerFitTransform() {
    Matrix data = createNumericMatrix()

    SmileFeatures.StandardScaler scaler = SmileFeatures.standardScaler()
    Matrix scaled = scaler.fitTransform(data, ['x', 'y'])

    // Check means are stored
    assertNotNull(scaler.means)
    assertEquals(3.0d, scaler.means['x'], 0.0001d) // Mean of [1,2,3,4,5]
    assertEquals(30.0d, scaler.means['y'], 0.0001d) // Mean of [10,20,30,40,50]

    // Scaled data should have mean ≈ 0
    List<Double> xCol = scaled.column('x') as List<Double>
    assertEquals(0.0d, xCol.sum() / xCol.size(), 0.0001d)
  }

  @Test
  void testStandardScalerNotFitted() {
    SmileFeatures.StandardScaler scaler = SmileFeatures.standardScaler()

    assertThrows(IllegalStateException) {
      scaler.transform(createNumericMatrix())
    }
  }

  // ============ MinMaxScaler Tests ============

  @Test
  void testMinMaxScalerFitTransform() {
    Matrix data = createNumericMatrix()

    SmileFeatures.MinMaxScaler scaler = SmileFeatures.minMaxScaler()
    Matrix scaled = scaler.fitTransform(data, ['x'])

    // Check mins and maxs are stored
    assertEquals(1.0d, scaler.mins['x'], 0.0001d)
    assertEquals(5.0d, scaler.maxs['x'], 0.0001d)

    // Scaled data should be in [0, 1]
    List<Double> xCol = scaled.column('x') as List<Double>
    assertEquals(0.0d, xCol.min(), 0.0001d)
    assertEquals(1.0d, xCol.max(), 0.0001d)
  }

  // ============ Null Handling Tests ============

  @Test
  void testStandardizeWithNulls() {
    Matrix data = Matrix.builder()
        .data(x: [1.0, null, 3.0, null, 5.0])
        .types([Double])
        .build()

    Matrix standardized = SmileFeatures.standardize(data)

    List<?> col = standardized.column('x')
    assertNull(col[1])
    assertNull(col[3])
    assertNotNull(col[0])
  }

  @Test
  void testNormalizeWithNulls() {
    Matrix data = Matrix.builder()
        .data(x: [1.0, null, 3.0, null, 5.0])
        .types([Double])
        .build()

    Matrix normalized = SmileFeatures.normalize(data)

    List<?> col = normalized.column('x')
    assertNull(col[1])
    assertNull(col[3])
    assertEquals(0.0d, col[0] as double, 0.0001d)
    assertEquals(1.0d, col[4] as double, 0.0001d)
  }

  // ============ Preserve Matrix Properties Tests ============

  @Test
  void testPreservesMatrixName() {
    Matrix data = Matrix.builder()
        .data(x: [1.0, 2.0, 3.0])
        .types([Double])
        .matrixName('TestMatrix')
        .build()

    Matrix standardized = SmileFeatures.standardize(data)

    assertEquals('TestMatrix', standardized.matrixName)
  }

  @Test
  void testPreservesColumnOrder() {
    Matrix data = createMixedMatrix()

    Matrix encoded = SmileFeatures.labelEncode(data, 'category')

    // id should still be first column
    assertEquals('id', encoded.columnName(0))
  }
}
