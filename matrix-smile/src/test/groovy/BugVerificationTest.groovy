import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.smile.DataframeConverter
import se.alipsa.matrix.smile.data.SmileData
import se.alipsa.matrix.smile.data.SmileFeatures
import se.alipsa.matrix.smile.ml.SmileCluster
import se.alipsa.matrix.smile.ml.SmileDimensionality

/**
 * Tests that verify each confirmed bug or usability gap found in the module review.
 * Each test is named after the issue it covers, and is written before the fix (TDD Red phase).
 */
class BugVerificationTest {

  // ===== Issue 1: dropna(columns) silently uses last column for unknown column names =====
  // indexOf returns -1 for a missing column name; Groovy row[-1] accesses the last element
  // silently, so nulls in the requested column are never actually detected.

  @Test
  void testDropnaSpecificColumnsThrowsForUnknownColumnName() {
    Matrix data = Matrix.builder()
        .data(
            x: [1.0, null, 3.0],
            y: [4.0, 5.0, null]
        )
        .types([Double, Double])
        .build()

    def ex = assertThrows(IllegalArgumentException) {
      SmileFeatures.dropna(data, ['nonexistent'])
    }
    assertTrue(ex.message.contains('nonexistent'),
        "Expected message to mention unknown column name, got: ${ex.message}")
  }

  // ===== Issue 2: fillnaMean() on an all-null column divides by zero → NaN fill =====
  // When every value is null, numericCol is empty; 0.0d / 0 == NaN and
  // fillna then writes NaN into every cell without any error.

  @Test
  void testFillnaMeanAllNullColumnThrows() {
    Matrix data = Matrix.builder()
        .data(x: [null, null, null])
        .types([Double])
        .build()

    def ex = assertThrows(IllegalArgumentException) {
      SmileFeatures.fillnaMean(data, 'x')
    }
    assertTrue(ex.message.toLowerCase().contains('non-null') || ex.message.toLowerCase().contains('empty'),
        "Expected message to explain no non-null values, got: ${ex.message}")
  }

  // ===== Issue 3: SmileData.bootstrap(sampleSize=0) silently uses full matrix size =====
  // The guard is `sampleSize > 0`; sampleSize=0 falls through to `matrix.rowCount()`
  // even though the documented "use full size" sentinel is -1, making 0 surprising.

  @Test
  void testBootstrapZeroSampleSizeThrows() {
    Matrix data = Matrix.builder()
        .data(x: [1.0, 2.0, 3.0, 4.0, 5.0])
        .types([Double])
        .build()

    def ex = assertThrows(IllegalArgumentException) {
      SmileData.bootstrap(data, 3, 0)
    }
    assertTrue(ex.message.toLowerCase().contains('sample') || ex.message.contains('0'),
        "Expected message to mention invalid sampleSize, got: ${ex.message}")
  }

  @Test
  void testBootstrapNegativeSampleSizeThrows() {
    Matrix data = Matrix.builder()
        .data(x: [1.0, 2.0, 3.0, 4.0, 5.0])
        .types([Double])
        .build()

    def ex = assertThrows(IllegalArgumentException) {
      SmileData.bootstrap(data, 3, -2)
    }
    assertTrue(ex.message.toLowerCase().contains('sample') || ex.message.contains('-2'),
        "Expected message to mention invalid sampleSize, got: ${ex.message}")
  }

  // ===== Issue 4: binning(edges) silently returns null for values outside the edge range =====
  // A value below edges[0] or above edges.last() (but not equal) falls through all
  // loop branches and produces a null with no warning.

  @Test
  void testBinningEdgesOutOfRangeValueThrows() {
    Matrix data = Matrix.builder()
        .data(x: [1.0, 5.0, 15.0])   // 1.0 is below edges[0]=2.0
        .types([Double])
        .build()

    def ex = assertThrows(IllegalArgumentException) {
      SmileFeatures.binning(data, 'x', [2.0, 8.0, 12.0], ['low', 'medium'])
    }
    assertTrue(ex.message.contains('1.0') || ex.message.toLowerCase().contains('outside')
        || ex.message.toLowerCase().contains('range'),
        "Expected message to mention out-of-range value, got: ${ex.message}")
  }

  // ===== Issue 5: kmeans() passes k < 2 directly to Smile with no user-readable error =====

  @Test
  void testKmeansKLessThanTwoThrowsClearMessage() {
    Matrix data = Matrix.builder()
        .data(
            x: [1.0, 2.0, 3.0, 4.0, 5.0],
            y: [1.0, 2.0, 3.0, 4.0, 5.0]
        )
        .types([Double, Double])
        .build()

    def ex = assertThrows(IllegalArgumentException) {
      SmileCluster.kmeans(data, 1)
    }
    assertTrue(ex.message.contains('k') || ex.message.contains('cluster') || ex.message.contains('2'),
        "Expected user-readable message mentioning k or clusters, got: ${ex.message}")
  }

  // ===== Issue 6: pca() passes k <= 0 directly to Smile with no user-readable error =====

  @Test
  void testPcaKZeroThrowsClearMessage() {
    Matrix data = Matrix.builder()
        .data(
            x: [1.0, 2.0, 3.0, 4.0, 5.0],
            y: [2.0, 3.0, 4.0, 5.0, 6.0]
        )
        .types([Double, Double])
        .build()

    def ex = assertThrows(IllegalArgumentException) {
      SmileDimensionality.pca(data, 0)
    }
    assertTrue(ex.message.contains('k') || ex.message.contains('component') || ex.message.contains('0'),
        "Expected user-readable message mentioning k or components, got: ${ex.message}")
  }

  // ===== Issue 7: normalize(min, max) silently produces wrong output when min >= max =====

  @Test
  void testNormalizeToRangeMinEqualMaxThrows() {
    Matrix data = Matrix.builder()
        .data(x: [1.0, 2.0, 3.0])
        .types([Double])
        .build()

    assertThrows(IllegalArgumentException) {
      SmileFeatures.normalize(data, ['x'], 5.0d, 5.0d)
    }
  }

  @Test
  void testNormalizeToRangeMinGreaterThanMaxThrows() {
    Matrix data = Matrix.builder()
        .data(x: [1.0, 2.0, 3.0])
        .types([Double])
        .build()

    assertThrows(IllegalArgumentException) {
      SmileFeatures.normalize(data, ['x'], 10.0d, 1.0d)
    }
  }

  // ===== Issue 8: sqrtTransform on a negative value throws ArithmeticException without column context =====
  // NumberExtension.sqrt(Number) converts to BigDecimal and calls BigDecimal.sqrt() which throws
  // ArithmeticException ("Square root of a negative BigDecimal") – no column name, no row index.

  @Test
  void testSqrtTransformNegativeValueThrowsIllegalArgumentExceptionWithColumnName() {
    Matrix data = Matrix.builder()
        .data(score: [1.0, -4.0, 9.0])
        .types([Double])
        .build()

    def ex = assertThrows(IllegalArgumentException) {
      SmileFeatures.sqrtTransform(data, ['score'])
    }
    assertTrue(ex.message.contains('score'),
        "Expected message to contain column name 'score', got: ${ex.message}")
  }

  // ===== Issue 9: logTransform on value < -1 throws IAE from NumberExtension without column context =====
  // NumberExtension.log1p already throws IAE, but the message says nothing about which
  // column or row caused the problem.

  @Test
  void testLogTransformBelowMinusOneThrowsWithColumnName() {
    Matrix data = Matrix.builder()
        .data(revenue: [0.0, -2.0, 1.0])   // -2.0 → log1p(-2) undefined
        .types([Double])
        .build()

    def ex = assertThrows(IllegalArgumentException) {
      SmileFeatures.logTransform(data, ['revenue'])
    }
    assertTrue(ex.message.contains('revenue'),
        "Expected message to contain column name 'revenue', got: ${ex.message}")
  }

  // ===== Issue 10: DataframeConverter converts BigDecimal columns to a non-numeric Smile vector =====
  // ValueVector.of(String, BigDecimal[]) may produce a nominal/object column instead of a
  // numeric double column; round-trip should preserve numeric values.

  @Test
  void testDataframeConverterBigDecimalRoundTripPreservesValues() {
    Matrix matrix = Matrix.builder()
        .data(price: [1.5G, 2.5G, 3.5G])
        .types([BigDecimal])
        .build()

    def df = DataframeConverter.convert(matrix)
    Matrix result = DataframeConverter.convert(df)

    assertEquals(3, result.rowCount())
    assertFalse(result[0, 'price'] instanceof String,
        "BigDecimal value should not become String after round-trip, was: ${result[0, 'price']}")
    assertEquals(1.5, (result[0, 'price'] as Number).doubleValue(), 0.001)
    assertEquals(2.5, (result[1, 'price'] as Number).doubleValue(), 0.001)
    assertEquals(3.5, (result[2, 'price'] as Number).doubleValue(), 0.001)
  }

}
