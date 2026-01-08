package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.CutWidth

import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * Tests for CutWidth bin boundary handling.
 */
class CutWidthTest {

  /**
   * Values equal to the bin start should fall into the previous bin when closed right.
   */
  @Test
  void testClosedRightAssignsBoundaryToPreviousBin() {
    def data = Matrix.builder()
        .columnNames('x')
        .rows([
            [1.5],
            [2.5],
            [3.5]
        ])
        .types(BigDecimal)
        .build()

    def cutWidth = new CutWidth('x', 1)
    String colName = cutWidth.addToMatrix(data)
    def labels = data[colName] as List<String>

    // For closed-right, value 2.5 is included in the bin ending at 2.5 (right boundary included).
    // The first bin uses [a,b] notation (both closed) as a special case to include the minimum.
    assertEquals('[1.5,2.5]', labels[1], '2.5 should be in the first bin [1.5,2.5] (both brackets closed for first bin)')
    // Subsequent bins use (a,b] notation (open left, closed right)
    assertEquals('(2.5,3.5]', labels[2], '3.5 should be in bin (2.5,3.5] (standard closed-right notation)')
  }

  /**
   * Values equal to the bin start are included in that bin when closed left.
   */
  @Test
  void testClosedLeftAssignsBoundaryToNextBin() {
    def data = Matrix.builder()
        .columnNames('x')
        .rows([
            [1.5],
            [2.5],
            [3.5]
        ])
        .types(BigDecimal)
        .build()

    def cutWidth = new CutWidth('x', 1, null, null, false)
    String colName = cutWidth.addToMatrix(data)
    def labels = data[colName] as List<String>

    assertEquals('[2.5,3.5)', labels[1], '2.5 should be in the upper bin')
    assertEquals('[3.5,4.5)', labels[2], '3.5 should be in the upper bin')
  }

  /**
   * Test computeBins directly with a simple range.
   * Verifies that binIndex (BigDecimal) works correctly as array index (lines 171-172).
   */
  @Test
  void testComputeBinsWithSimpleRange() {
    CutWidth cw = new CutWidth('x', 1.0)
    List<Number> values = [1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0]

    List<String> labels = cw.computeBins(values)

    // Verify all values get binned (no null labels)
    assertEquals(7, labels.size())
    labels.each { label ->
      assert label != null : "All values should be binned"
    }

    // Verify bin format
    assertEquals('[0.5,1.5]', labels[0], '1.0 should be in first bin')
    assertEquals('[0.5,1.5]', labels[1], '1.5 should be in first bin')
    assertEquals('(1.5,2.5]', labels[2], '2.0 should be in second bin')
    assertEquals('(1.5,2.5]', labels[3], '2.5 should be in second bin')
    assertEquals('(2.5,3.5]', labels[4], '3.0 should be in third bin')
    assertEquals('(2.5,3.5]', labels[5], '3.5 should be in third bin')
    assertEquals('(3.5,4.5]', labels[6], '4.0 should be in fourth bin')
  }

  /**
   * Test computeBins with values at exact bin boundaries.
   * This specifically tests the code path where binIndex is clamped (line 171)
   * and then used as array index (line 172).
   */
  @Test
  void testComputeBinsWithBoundaryValues() {
    CutWidth cw = new CutWidth('x', 10.0)
    // Test with values exactly on boundaries
    List<Number> values = [0, 10, 20, 30, 40, 50]

    List<String> labels = cw.computeBins(values)

    assertEquals(6, labels.size())
    assertEquals('[-5,5]', labels[0], '0 should be in first bin')
    assertEquals('(5,15]', labels[1], '10 should be in second bin (closed right)')
    assertEquals('(15,25]', labels[2], '20 should be in third bin')
    assertEquals('(25,35]', labels[3], '30 should be in fourth bin')
    assertEquals('(35,45]', labels[4], '40 should be in fifth bin')
    assertEquals('(45,55]', labels[5], '50 should be in sixth bin')
  }

  /**
   * Test computeBins with a large range of values.
   * Ensures that binIndex calculation and array indexing works across many bins.
   */
  @Test
  void testComputeBinsWithLargeRange() {
    CutWidth cw = new CutWidth('x', 5.0)
    List<Number> values = [1, 10, 25, 50, 75, 100]

    List<String> labels = cw.computeBins(values)

    assertEquals(6, labels.size())
    // All values should be binned
    labels.each { label ->
      assert label != null : "All values should be binned"
      assert label.contains(',') : "Label should contain a comma"
    }

    // Check first and last
    assert labels[0].startsWith('[')
    assert labels[5].endsWith(']')
  }

  /**
   * Test computeBins with negative values.
   * Verifies binIndex calculation works with negative numbers.
   */
  @Test
  void testComputeBinsWithNegativeValues() {
    CutWidth cw = new CutWidth('x', 10.0)
    List<Number> values = [-25, -15, -5, 5, 15, 25]

    List<String> labels = cw.computeBins(values)

    assertEquals(6, labels.size())
    labels.each { label ->
      assert label != null : "All values should be binned"
    }

    // Verify negative bins are formatted correctly
    assert labels[0].contains('-')
    assert labels[1].contains('-')
  }

  /**
   * Test computeBins with fractional width.
   * Tests that binIndex (BigDecimal from floor operation) works as array index.
   */
  @Test
  void testComputeBinsWithFractionalWidth() {
    CutWidth cw = new CutWidth('x', 0.5)
    List<Number> values = [1.0, 1.25, 1.5, 1.75, 2.0, 2.25]

    List<String> labels = cw.computeBins(values)

    assertEquals(6, labels.size())
    labels.each { label ->
      assert label != null : "All values should be binned"
    }

    // With width 0.5, we should have bins like [0.75,1.25], (1.25,1.75], etc.
    assertEquals('[0.75,1.25]', labels[0])
    assertEquals('[0.75,1.25]', labels[1])
    assertEquals('(1.25,1.75]', labels[2])
    assertEquals('(1.25,1.75]', labels[3])
    assertEquals('(1.75,2.25]', labels[4])
    assertEquals('(1.75,2.25]', labels[5])
  }

  /**
   * Test computeBins with mixed null and numeric values.
   * Ensures null handling doesn't break binIndex array access.
   */
  @Test
  void testComputeBinsWithNullValues() {
    CutWidth cw = new CutWidth('x', 1.0)
    List<?> values = [1.0, null, 2.0, null, 3.0]

    List<String> labels = cw.computeBins(values)

    assertEquals(5, labels.size())
    assertEquals('[0.5,1.5]', labels[0])
    assertEquals(null, labels[1], 'Null values should produce null labels')
    assertEquals('(1.5,2.5]', labels[2])
    assertEquals(null, labels[3], 'Null values should produce null labels')
    assertEquals('(2.5,3.5]', labels[4])
  }

  /**
   * Test computeBins with a single value.
   * Edge case where binIndex should be 0 for all values.
   */
  @Test
  void testComputeBinsWithSingleValue() {
    CutWidth cw = new CutWidth('x', 1.0)
    List<Number> values = [5.0, 5.0, 5.0]

    List<String> labels = cw.computeBins(values)

    assertEquals(3, labels.size())
    // All values should be in the same bin
    assertEquals(labels[0], labels[1])
    assertEquals(labels[1], labels[2])
  }

  /**
   * Test computeBins with values requiring clamping (line 171).
   * This specifically tests the max/min clamping logic where binIndex is ensured
   * to be within [0, breaks.size() - 2].
   */
  @Test
  void testComputeBinsRequiringClamping() {
    CutWidth cw = new CutWidth('x', 100.0)
    // Use a wide range with small width to create many bins
    List<Number> values = [0.0, 0.1, 99.9, 100.0]

    List<String> labels = cw.computeBins(values)

    assertEquals(4, labels.size())
    labels.each { label ->
      assert label != null : "All values should be binned without error"
    }
  }

  /**
   * Test that demonstrates binIndex (BigDecimal) successfully works as array index.
   * This is the specific concern from the code review about lines 171-172.
   */
  @Test
  void testBinIndexArrayAccessWorks() {
    // This test will fail at compile time if BigDecimal cannot be used as array index
    // with @CompileStatic, or at runtime if the conversion fails
    CutWidth cw = new CutWidth('x', 1.0)

    // Create a range that will exercise different binIndex values
    List<Number> values = []
    for (int i = 0; i < 100; i++) {
      values.add(i * 0.1)
    }

    List<String> labels = cw.computeBins(values)

    // If we get here without error, binIndex array access works
    assertEquals(100, labels.size())
    labels.each { label ->
      assert label != null : "All 100 values should be successfully binned"
      assert label.contains(',') : "Each label should be a valid interval"
    }
  }
}
