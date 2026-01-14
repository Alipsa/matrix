package gg.stat

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.stat.GgStat

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for stat_summary_2d - 2D rectangular binned summary statistics.
 */
class StatsSummary2dTest {

  @Test
  void testSummary2dMean() {
    // Create points with known z values in two distinct areas
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'z'])
        .rows([
            [0, 0, 10], [0, 0, 20],  // Two points in same bin, mean=15
            [5, 5, 100], [5, 5, 200]  // Two points in another bin, mean=150
        ])
        .types(Integer, Integer, Integer)
        .build()

    def aes = new Aes(x: 'x', y: 'y', fill: 'z')
    def result = GgStat.summary2d(data, aes, [bins: 2, fun: 'mean'])

    // Verify correct columns
    assertTrue(result.columnNames().containsAll(['x', 'y', 'value']),
        "Result should have x, y, value columns, got: ${result.columnNames()}")

    // Should have 2 bins
    assertEquals(2, result.rowCount(), "Should have 2 bins")

    // Check mean values
    List<BigDecimal> values = result['value'] as List<BigDecimal>
    List<BigDecimal> sortedValues = values.sort()

    assertEquals(15.0, sortedValues[0] as double, 0.001, "First bin mean should be 15")
    assertEquals(150.0, sortedValues[1] as double, 0.001, "Second bin mean should be 150")
  }

  @Test
  void testSummary2dMedian() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'value'])
        .rows([
            [0, 0, 1], [0, 0, 2], [0, 0, 3], [0, 0, 100],  // Median=2.5
            [5, 5, 10], [5, 5, 20]  // Median=15
        ])
        .types(Integer, Integer, Integer)
        .build()

    def aes = new Aes(x: 'x', y: 'y', fill: 'value')
    def result = GgStat.summary2d(data, aes, [bins: 2, fun: 'median'])

    assertEquals(2, result.rowCount())
    List<BigDecimal> values = result['value'] as List<BigDecimal>
    List<BigDecimal> sortedValues = values.sort()

    assertEquals(2.5, sortedValues[0] as double, 0.001, "Median should handle outliers")
    assertEquals(15.0, sortedValues[1] as double, 0.001)
  }

  @Test
  void testSummary2dSum() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'value'])
        .rows([
            [0, 0, 10], [0, 0, 20], [0, 0, 30],  // Sum=60
            [5, 5, 5], [5, 5, 10]  // Sum=15
        ])
        .types(Integer, Integer, Integer)
        .build()

    def aes = new Aes(x: 'x', y: 'y', fill: 'value')
    def result = GgStat.summary2d(data, aes, [bins: 2, fun: 'sum'])

    assertEquals(2, result.rowCount())
    List<BigDecimal> values = result['value'] as List<BigDecimal>
    List<BigDecimal> sortedValues = values.sort()

    assertEquals(15.0, sortedValues[0] as double, 0.001)
    assertEquals(60.0, sortedValues[1] as double, 0.001)
  }

  @Test
  void testSummary2dMin() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'value'])
        .rows([
            [0, 0, 10], [0, 0, 20], [0, 0, 5],  // Min=5
            [5, 5, 100], [5, 5, 50]  // Min=50
        ])
        .types(Integer, Integer, Integer)
        .build()

    def aes = new Aes(x: 'x', y: 'y', fill: 'value')
    def result = GgStat.summary2d(data, aes, [bins: 2, fun: 'min'])

    assertEquals(2, result.rowCount())
    List<BigDecimal> values = result['value'] as List<BigDecimal>
    assertTrue(values.contains(5 as BigDecimal))
    assertTrue(values.contains(50 as BigDecimal))
  }

  @Test
  void testSummary2dMax() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'value'])
        .rows([
            [0, 0, 10], [0, 0, 20], [0, 0, 30],  // Max=30
            [5, 5, 100], [5, 5, 200]  // Max=200
        ])
        .types(Integer, Integer, Integer)
        .build()

    def aes = new Aes(x: 'x', y: 'y', fill: 'value')
    def result = GgStat.summary2d(data, aes, [bins: 2, fun: 'max'])

    assertEquals(2, result.rowCount())
    List<BigDecimal> values = result['value'] as List<BigDecimal>
    assertTrue(values.contains(30 as BigDecimal))
    assertTrue(values.contains(200 as BigDecimal))
  }

  @Test
  void testSummary2dWithCustomFunction() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'value'])
        .rows([
            [0, 0, 10], [0, 0, 20],
            [5, 5, 100], [5, 5, 200]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def aes = new Aes(x: 'x', y: 'y', fill: 'value')

    // Custom function: return the count as the value
    def customFun = { List<BigDecimal> vals ->
      return [value: vals.size()]
    }

    def result = GgStat.summary2d(data, aes, [bins: 2, 'fun.data': customFun])

    assertEquals(2, result.rowCount())
    List<BigDecimal> values = result['value'] as List<BigDecimal>

    // Both bins should have count=2
    assertTrue(values.every { it == 2 as BigDecimal }, "Custom function should return count")
  }

  @Test
  void testSummary2dWithColorAesthetic() {
    // Test that summary2d can use color aesthetic when fill is not available
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'z'])
        .rows([
            [0, 0, 10], [0, 0, 20],
            [5, 5, 30], [5, 5, 40]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def aes = new Aes(x: 'x', y: 'y', color: 'z')  // Using color instead of fill
    def result = GgStat.summary2d(data, aes, [bins: 2, fun: 'mean'])

    assertEquals(2, result.rowCount())
    assertTrue(result.columnNames().containsAll(['x', 'y', 'value']))
  }

  @Test
  void testSummary2dEmptyData() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'z'])
        .rows([])
        .types(Double, Double, Double)
        .build()

    def aes = new Aes(x: 'x', y: 'y', fill: 'z')
    def result = GgStat.summary2d(data, aes)

    assertEquals(0, result.rowCount(), "Empty data should produce empty result")
    assertTrue(result.columnNames().containsAll(['x', 'y', 'value']))
  }

  @Test
  void testSummary2dRequiresZVariable() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 2], [3, 4]])
        .types(Integer, Integer)
        .build()

    def aes = new Aes(x: 'x', y: 'y')  // No fill or color

    def ex = assertThrows(IllegalArgumentException) {
      GgStat.summary2d(data, aes)
    }
    assertTrue(ex.message.contains('fill or color aesthetic'))
  }

  @Test
  void testSummary2dRequiresXAndY() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'z'])
        .rows([[1, 2, 10]])
        .types(Integer, Integer, Integer)
        .build()

    // Missing x
    def aes1 = new Aes(y: 'y', fill: 'z')
    def ex1 = assertThrows(IllegalArgumentException) {
      GgStat.summary2d(data, aes1)
    }
    assertTrue(ex1.message.contains('x and y aesthetics'))

    // Missing y
    def aes2 = new Aes(x: 'x', fill: 'z')
    def ex2 = assertThrows(IllegalArgumentException) {
      GgStat.summary2d(data, aes2)
    }
    assertTrue(ex2.message.contains('x and y aesthetics'))
  }

  @Test
  void testSummary2dWithBinwidth() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'value'])
        .rows([
            [0, 0, 10], [1, 1, 20],
            [5, 5, 30], [6, 6, 40]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def aes = new Aes(x: 'x', y: 'y', fill: 'value')
    def result = GgStat.summary2d(data, aes, [binwidth: 3.0, fun: 'mean'])

    assertTrue(result.rowCount() > 0)
    assertTrue(result.columnNames().containsAll(['x', 'y', 'value']))

    // Verify all values are computed correctly
    List<BigDecimal> values = result['value'] as List<BigDecimal>
    values.each { value ->
      assertTrue(value > 0, "All summary values should be positive")
    }
  }

  @Test
  void testSummary2dInvalidBinsZero() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'z'])
        .rows([[0, 0, 10], [1, 1, 20]])
        .types(Integer, Integer, Integer)
        .build()

    def aes = new Aes(x: 'x', y: 'y', fill: 'z')

    def ex = assertThrows(IllegalArgumentException) {
      GgStat.summary2d(data, aes, [bins: 0])
    }
    assertTrue(ex.message.contains('positive') && ex.message.contains('0'))
  }

  @Test
  void testSummary2dInvalidBinsNegative() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'z'])
        .rows([[0, 0, 10], [1, 1, 20]])
        .types(Integer, Integer, Integer)
        .build()

    def aes = new Aes(x: 'x', y: 'y', fill: 'z')

    def ex = assertThrows(IllegalArgumentException) {
      GgStat.summary2d(data, aes, [bins: -5])
    }
    assertTrue(ex.message.contains('positive') && ex.message.contains('-5'))
  }

  @Test
  void testSummary2dInvalidBinwidthNegative() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'z'])
        .rows([[0, 0, 10], [1, 1, 20]])
        .types(Integer, Integer, Integer)
        .build()

    def aes = new Aes(x: 'x', y: 'y', fill: 'z')

    def ex = assertThrows(IllegalArgumentException) {
      GgStat.summary2d(data, aes, [binwidth: -1.0])
    }
    assertTrue(ex.message.contains('positive'))
  }

  @Test
  void testSummary2dCustomFunctionMissingKeys() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'value'])
        .rows([
            [0, 0, 10], [0, 0, 20]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def aes = new Aes(x: 'x', y: 'y', fill: 'value')

    // Custom function that returns wrong keys
    def badFun = { List<BigDecimal> vals ->
      return [wrong_key: vals.size()]  // Neither 'y' nor 'value'
    }

    def ex = assertThrows(IllegalArgumentException) {
      GgStat.summary2d(data, aes, [bins: 2, 'fun.data': badFun])
    }
    assertTrue(ex.message.contains('y') || ex.message.contains('value'),
        "Error message should mention required keys")
  }

  @Test
  void testSummary2dDefaultMean() {
    // Verify default function is mean
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'z'])
        .rows([
            [0, 0, 10], [0, 0, 30]  // Mean=20
        ])
        .types(Integer, Integer, Integer)
        .build()

    def aes = new Aes(x: 'x', y: 'y', fill: 'z')
    def result = GgStat.summary2d(data, aes, [bins: 1])  // No fun specified

    assertEquals(1, result.rowCount())
    assertEquals(20.0, result['value'][0] as double, 0.001, "Default should be mean")
  }

  @Test
  void testSummary2dSinglePoint() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'z'])
        .rows([[5, 5, 100]])
        .types(Integer, Integer, Integer)
        .build()

    def aes = new Aes(x: 'x', y: 'y', fill: 'z')
    def result = GgStat.summary2d(data, aes)

    assertEquals(1, result.rowCount(), "Single point should create one bin")
    assertEquals(100.0, result['value'][0] as double, 0.001, "Single point should have its own value")
  }

  @Test
  void testSummary2dBinCenters() {
    // Test that bin centers are computed correctly
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'z'])
        .rows([
            [0, 0, 10],
            [10, 10, 20]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def aes = new Aes(x: 'x', y: 'y', fill: 'z')
    def result = GgStat.summary2d(data, aes, [bins: 2])

    assertEquals(2, result.rowCount())

    // Bin centers should be within the data range
    List<BigDecimal> xVals = result['x'] as List<BigDecimal>
    List<BigDecimal> yVals = result['y'] as List<BigDecimal>

    xVals.each { x ->
      assertTrue(x >= 0 && x <= 10, "X center should be within data range")
    }
    yVals.each { y ->
      assertTrue(y >= 0 && y <= 10, "Y center should be within data range")
    }
  }

  @Test
  void testSummary2dUnknownFunction() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'z'])
        .rows([[0, 0, 10]])
        .types(Integer, Integer, Integer)
        .build()

    def aes = new Aes(x: 'x', y: 'y', fill: 'z')

    def ex = assertThrows(IllegalArgumentException) {
      GgStat.summary2d(data, aes, [fun: 'unknown_function'])
    }
    assertTrue(ex.message.contains('Unknown summary function'))
  }
}
