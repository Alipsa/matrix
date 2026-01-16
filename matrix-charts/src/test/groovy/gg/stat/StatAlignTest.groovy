package gg.stat

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.stat.GgStat

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for stat_align - data alignment for stacked area charts.
 */
class StatAlignTest {

  @Test
  void testBasicAlignment() {
    // Two groups with partially overlapping x-values
    def data = Matrix.builder()
        .data([
            x: [1, 2, 3, 2, 3, 4],
            y: [10, 20, 30, 15, 25, 35],
            group: ['A', 'A', 'A', 'B', 'B', 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def aes = aes(x: 'x', y: 'y', fill: 'group')
    def aligned = GgStat.align(data, aes)

    assertNotNull(aligned, 'Alignment should return data')
    // Should have union of x-values: 1, 2, 3, 4
    // Each group should have 4 rows
    assertEquals(8, aligned.rowCount(), 'Should have 8 total rows (2 groups x 4 x-values)')
  }

  @Test
  void testNonOverlappingGroups() {
    // Group A: x = [1, 2], Group B: x = [3, 4]
    def data = Matrix.builder()
        .data([
            x: [1, 2, 3, 4],
            y: [10, 20, 15, 25],
            group: ['A', 'A', 'B', 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def aes = aes(x: 'x', y: 'y', fill: 'group')
    def aligned = GgStat.align(data, aes)

    assertNotNull(aligned)
    // Union: [1, 2, 3, 4], so each group gets 4 points
    assertEquals(8, aligned.rowCount(), 'Should have 8 rows after alignment')

    // Verify group A has interpolated values at x=3 and x=4
    def groupARows = aligned.rows().findAll { it.group == 'A' }
    assertEquals(4, groupARows.size(), 'Group A should have 4 rows')

    // Verify group B has interpolated values at x=1 and x=2
    def groupBRows = aligned.rows().findAll { it.group == 'B' }
    assertEquals(4, groupBRows.size(), 'Group B should have 4 rows')
  }

  @Test
  void testSingleGroup() {
    // Single group - should pass through unchanged (or with minor reorganization)
    def data = Matrix.builder()
        .data([
            x: [1, 2, 3],
            y: [10, 20, 30]
        ])
        .types(Integer, Integer)
        .build()

    def aes = aes('x', 'y')
    def aligned = GgStat.align(data, aes)

    assertNotNull(aligned)
    assertEquals(3, aligned.rowCount(), 'Single group should have same number of rows')
  }

  @Test
  void testExactInterpolation() {
    // Test that interpolation math is correct
    def data = Matrix.builder()
        .data([
            x: [1, 3, 2],
            y: [10, 30, 20],
            group: ['A', 'A', 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def aes = aes(x: 'x', y: 'y', fill: 'group')
    def aligned = GgStat.align(data, aes)

    // Group B needs interpolation at x=1 and x=3
    // Only has point at x=2, y=20
    def groupBRows = aligned.rows().findAll { it.group == 'B' }.sort { it.x }

    // At x=1 (before data): should extrapolate using nearest value (y=20)
    def bAtX1 = groupBRows.find { it.x == 1 }
    assertNotNull(bAtX1, 'Group B should have value at x=1')
    assertEquals(20 as BigDecimal, bAtX1.y as BigDecimal, 0.01, 'Should extrapolate using nearest value')

    // At x=2: exact match, should be y=20
    def bAtX2 = groupBRows.find { it.x == 2 }
    assertNotNull(bAtX2)
    assertEquals(20 as BigDecimal, bAtX2.y as BigDecimal, 0.01)

    // At x=3 (after data): should extrapolate using nearest value (y=20)
    def bAtX3 = groupBRows.find { it.x == 3 }
    assertNotNull(bAtX3, 'Group B should have value at x=3')
    assertEquals(20 as BigDecimal, bAtX3.y as BigDecimal, 0.01, 'Should extrapolate using nearest value')
  }

  @Test
  void testLinearInterpolation() {
    // Test linear interpolation between two points
    def data = Matrix.builder()
        .data([
            x: [1, 3, 1, 2, 3],
            y: [10, 30, 5, 15, 25],
            group: ['A', 'A', 'B', 'B', 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def aes = aes(x: 'x', y: 'y', fill: 'group')
    def aligned = GgStat.align(data, aes)

    // Group A has points at x=1 (y=10) and x=3 (y=30)
    // At x=2, should interpolate: y = 10 + (30-10) * (2-1)/(3-1) = 10 + 20*0.5 = 20
    def groupARows = aligned.rows().findAll { it.group == 'A' }.sort { it.x }
    def aAtX2 = groupARows.find { it.x == 2 }
    assertNotNull(aAtX2, 'Group A should have interpolated value at x=2')
    assertEquals(20 as BigDecimal, aAtX2.y as BigDecimal, 0.01, 'Linear interpolation should give y=20')
  }

  @Test
  void testNullHandling() {
    // Test that null x or y values are handled gracefully
    def data = Matrix.builder()
        .data([
            x: [1, 2, null, 3],
            y: [10, 20, 15, 30],
            group: ['A', 'A', 'B', 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def aes = aes(x: 'x', y: 'y', fill: 'group')
    def aligned = GgStat.align(data, aes)

    assertNotNull(aligned, 'Should handle null values')
    // Null x should be skipped
    assertTrue(aligned.rowCount() >= 3, 'Should have at least valid rows')
  }

  @Test
  void testEmptyData() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'group')
        .types(Integer, Integer, String)
        .build()

    def aes = aes(x: 'x', y: 'y', fill: 'group')
    def aligned = GgStat.align(data, aes)

    assertNotNull(aligned)
    assertEquals(0, aligned.rowCount(), 'Empty data should stay empty')
  }

  @Test
  void testSingleXValue() {
    // All groups have only one x-value
    def data = Matrix.builder()
        .data([
            x: [1, 1],
            y: [10, 20],
            group: ['A', 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def aes = aes(x: 'x', y: 'y', fill: 'group')
    def aligned = GgStat.align(data, aes)

    assertNotNull(aligned)
    // No alignment needed when all x-values are the same
    assertEquals(2, aligned.rowCount())
  }

  @Test
  void testWithColorGrouping() {
    // Test with color aesthetic instead of fill
    def data = Matrix.builder()
        .data([
            x: [1, 2, 2, 3],
            y: [10, 20, 15, 25],
            category: ['A', 'A', 'B', 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def aes = aes(x: 'x', y: 'y', color: 'category')
    def aligned = GgStat.align(data, aes)

    assertNotNull(aligned)
    // Union of x: [1, 2, 3], 2 groups = 6 rows
    assertEquals(6, aligned.rowCount())
  }

  @Test
  void testStatAlignFactory() {
    // Test stat_align() factory method
    def stat = stat_align()
    assertNotNull(stat, 'stat_align() should create instance')
  }

  @Test
  void testStatAlignWithParams() {
    def stat = stat_align([:])
    assertNotNull(stat)
  }

  @Test
  void testAlignmentPreservesOtherColumns() {
    // Test that alignment preserves additional columns (like fill, color, etc.)
    def data = Matrix.builder()
        .data([
            x: [1, 2, 2, 3],
            y: [10, 20, 15, 25],
            group: ['A', 'A', 'B', 'B'],
            extra: ['foo', 'bar', 'baz', 'qux']
        ])
        .types(Integer, Integer, String, String)
        .build()

    def aes = aes(x: 'x', y: 'y', fill: 'group')
    def aligned = GgStat.align(data, aes)

    assertNotNull(aligned)
    assertTrue(aligned.columnNames().contains('extra'), 'Should preserve extra column')
  }

  @Test
  void testThreeGroups() {
    // Test with three groups
    def data = Matrix.builder()
        .data([
            x: [1, 2, 2, 3, 3, 4],
            y: [10, 20, 15, 25, 18, 28],
            group: ['A', 'A', 'B', 'B', 'C', 'C']
        ])
        .types(Integer, Integer, String)
        .build()

    def aes = aes(x: 'x', y: 'y', fill: 'group')
    def aligned = GgStat.align(data, aes)

    assertNotNull(aligned)
    // Union of x: [1, 2, 3, 4], 3 groups = 12 rows
    assertEquals(12, aligned.rowCount(), 'Three groups with 4 x-values each = 12 rows')
  }

  @Test
  void testDecimalXValues() {
    // Test with non-integer x-values
    def data = Matrix.builder()
        .data([
            x: [1.0, 1.5, 2.0, 1.5, 2.0, 2.5],
            y: [10, 15, 20, 12, 18, 24],
            group: ['A', 'A', 'A', 'B', 'B', 'B']
        ])
        .types(BigDecimal, Integer, String)
        .build()

    def aes = aes(x: 'x', y: 'y', fill: 'group')
    def aligned = GgStat.align(data, aes)

    assertNotNull(aligned)
    // Union: [1.0, 1.5, 2.0, 2.5], 2 groups = 8 rows
    assertEquals(8, aligned.rowCount())
  }

  @Test
  void testMissingXorY() {
    // Test data without x or y columns
    def data = Matrix.builder()
        .data([
            a: [1, 2, 3],
            b: [10, 20, 30]
        ])
        .types(Integer, Integer)
        .build()

    def aes = aes(x: 'a', y: 'missing')
    def aligned = GgStat.align(data, aes)

    // Should return original data if y is missing
    assertNotNull(aligned)
    assertEquals(3, aligned.rowCount())
  }

  @Test
  void testExtrapolationAtBoundaries() {
    // Group A: [1,10], [3,30]
    // Group B: [2,20]
    // Group B needs extrapolation at x=1 and x=3
    def data = Matrix.builder()
        .data([
            x: [1, 3, 2],
            y: [10, 30, 20],
            group: ['A', 'A', 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def aes = aes(x: 'x', y: 'y', fill: 'group')
    def aligned = GgStat.align(data, aes)

    def groupBRows = aligned.rows().findAll { it.group == 'B' }
    assertEquals(3, groupBRows.size(), 'Group B should have 3 rows')

    // Group B only has data at x=2
    // At x=1 (before): should use nearest value y=20
    // At x=3 (after): should use nearest value y=20
    def bValues = groupBRows.collectEntries { [(it.x as Integer): it.y] }
    assertEquals(20 as BigDecimal, bValues[1] as BigDecimal, 0.01, 'Extrapolation at x=1')
    assertEquals(20 as BigDecimal, bValues[2] as BigDecimal, 0.01, 'Exact at x=2')
    assertEquals(20 as BigDecimal, bValues[3] as BigDecimal, 0.01, 'Extrapolation at x=3')
  }
}
