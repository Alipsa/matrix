import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.smile.SmileUtil
import smile.data.DataFrame

import static org.junit.jupiter.api.Assertions.*

class SmileUtilTest {

  @Test
  void testToDataFrameAndBack() {
    def matrix = Matrix.builder()
        .data(
            id: [1, 2, 3],
            name: ['Alice', 'Bob', 'Charlie'],
            salary: [50000.0, 60000.0, 70000.0]
        )
        .types([Integer, String, Double])
        .build()

    DataFrame df = SmileUtil.toDataFrame(matrix)
    Matrix result = SmileUtil.toMatrix(df)

    assertEquals(3, result.rowCount())
    assertEquals(3, result.columnCount())
    assertEquals(['id', 'name', 'salary'], result.columnNames() as List)
  }

  @Test
  void testDescribeWithNumericColumns() {
    def matrix = Matrix.builder()
        .data(
            a: [1.0, 2.0, 3.0, 4.0, 5.0],
            b: [10, 20, 30, 40, 50],
            name: ['a', 'b', 'c', 'd', 'e']
        )
        .types([Double, Integer, String])
        .build()

    Matrix desc = SmileUtil.describe(matrix)

    assertEquals(8, desc.rowCount()) // count, mean, std, min, 25%, 50%, 75%, max
    assertTrue(desc.columnNames().contains('statistic'))
    assertTrue(desc.columnNames().contains('a'))
    assertTrue(desc.columnNames().contains('b'))
    assertFalse(desc.columnNames().contains('name')) // String columns excluded

    // Check count row
    assertEquals('count', desc[0, 'statistic'])
    assertEquals(5, desc[0, 'a'])
    assertEquals(5, desc[0, 'b'])

    // Check mean row
    assertEquals('mean', desc[1, 'statistic'])
    assertEquals(3.0, desc[1, 'a'] as double, 0.001)
    assertEquals(30.0, desc[1, 'b'] as double, 0.001)
  }

  @Test
  void testDescribeWithNoNumericColumns() {
    def matrix = Matrix.builder()
        .data(
            name: ['Alice', 'Bob'],
            city: ['NYC', 'LA']
        )
        .types([String, String])
        .build()

    Matrix desc = SmileUtil.describe(matrix)

    assertEquals(0, desc.rowCount())
    assertEquals(['statistic'], desc.columnNames() as List)
  }

  @Test
  void testDescribeWithNulls() {
    def matrix = Matrix.builder()
        .data(
            a: [1.0, null, 3.0, null, 5.0]
        )
        .types([Double])
        .build()

    Matrix desc = SmileUtil.describe(matrix)

    assertEquals(3, desc[0, 'a']) // count should be 3 (non-null values)
    assertEquals(3.0, desc[1, 'a'] as double, 0.001) // mean of 1, 3, 5
  }

  @Test
  void testSampleByCount() {
    def matrix = Matrix.builder()
        .data(
            id: (1..100).toList()
        )
        .types([Integer])
        .build()

    Matrix sample = SmileUtil.sample(matrix, 10)

    assertEquals(10, sample.rowCount())
    assertEquals(['id'], sample.columnNames() as List)
  }

  @Test
  void testSampleByCountWithSeed() {
    def matrix = Matrix.builder()
        .data(
            id: (1..100).toList()
        )
        .types([Integer])
        .build()

    Matrix sample1 = SmileUtil.sample(matrix, 10, new Random(42))
    Matrix sample2 = SmileUtil.sample(matrix, 10, new Random(42))

    // Same seed should produce same results
    for (int i = 0; i < sample1.rowCount(); i++) {
      assertEquals(sample1[i, 'id'], sample2[i, 'id'])
    }
  }

  @Test
  void testSampleByFraction() {
    def matrix = Matrix.builder()
        .data(
            id: (1..100).toList()
        )
        .types([Integer])
        .build()

    Matrix sample = SmileUtil.sample(matrix, 0.1)

    assertEquals(10, sample.rowCount())
  }

  @Test
  void testSampleInvalidCount() {
    def matrix = Matrix.builder()
        .data(id: [1, 2, 3])
        .types([Integer])
        .build()

    assertThrows(IllegalArgumentException) {
      SmileUtil.sample(matrix, 10) // More than matrix size
    }

    assertThrows(IllegalArgumentException) {
      SmileUtil.sample(matrix, 0) // Zero
    }

    assertThrows(IllegalArgumentException) {
      SmileUtil.sample(matrix, -1) // Negative
    }
  }

  @Test
  void testSampleInvalidFraction() {
    def matrix = Matrix.builder()
        .data(id: [1, 2, 3])
        .types([Integer])
        .build()

    assertThrows(IllegalArgumentException) {
      SmileUtil.sample(matrix, 0.0) // Zero
    }

    assertThrows(IllegalArgumentException) {
      SmileUtil.sample(matrix, 1.5) // Greater than 1
    }
  }

  @Test
  void testHead() {
    def matrix = Matrix.builder()
        .data(id: (1..10).toList())
        .types([Integer])
        .build()

    Matrix head = SmileUtil.head(matrix, 3)

    assertEquals(3, head.rowCount())
    assertEquals(1, head[0, 'id'])
    assertEquals(2, head[1, 'id'])
    assertEquals(3, head[2, 'id'])
  }

  @Test
  void testHeadDefault() {
    def matrix = Matrix.builder()
        .data(id: (1..10).toList())
        .types([Integer])
        .build()

    Matrix head = SmileUtil.head(matrix)

    assertEquals(5, head.rowCount()) // Default is 5
  }

  @Test
  void testHeadMoreThanRows() {
    def matrix = Matrix.builder()
        .data(id: [1, 2, 3])
        .types([Integer])
        .build()

    Matrix head = SmileUtil.head(matrix, 10)

    assertEquals(3, head.rowCount()) // Should return all rows
  }

  @Test
  void testTail() {
    def matrix = Matrix.builder()
        .data(id: (1..10).toList())
        .types([Integer])
        .build()

    Matrix tail = SmileUtil.tail(matrix, 3)

    assertEquals(3, tail.rowCount())
    assertEquals(8, tail[0, 'id'])
    assertEquals(9, tail[1, 'id'])
    assertEquals(10, tail[2, 'id'])
  }

  @Test
  void testTailDefault() {
    def matrix = Matrix.builder()
        .data(id: (1..10).toList())
        .types([Integer])
        .build()

    Matrix tail = SmileUtil.tail(matrix)

    assertEquals(5, tail.rowCount()) // Default is 5
  }

  @Test
  void testHasNulls() {
    assertTrue(SmileUtil.hasNulls([1, null, 3]))
    assertFalse(SmileUtil.hasNulls([1, 2, 3]))
    assertFalse(SmileUtil.hasNulls([]))
  }

  @Test
  void testCountNulls() {
    assertEquals(2, SmileUtil.countNulls([1, null, 3, null]))
    assertEquals(0, SmileUtil.countNulls([1, 2, 3]))
    assertEquals(0, SmileUtil.countNulls([]))
  }

  @Test
  void testInfo() {
    def matrix = Matrix.builder()
        .data(
            id: [1, 2, 3, null, 5],
            name: ['Alice', 'Bob', 'Alice', 'Charlie', null]
        )
        .types([Integer, String])
        .build()

    Matrix info = SmileUtil.info(matrix)

    assertEquals(2, info.rowCount())
    assertEquals(['column', 'type', 'non-null', 'null', 'unique'], info.columnNames() as List)

    // Check id column info
    assertEquals('id', info[0, 'column'])
    assertEquals('Integer', info[0, 'type'])
    assertEquals(4, info[0, 'non-null'])
    assertEquals(1, info[0, 'null'])
    assertEquals(4, info[0, 'unique'])

    // Check name column info
    assertEquals('name', info[1, 'column'])
    assertEquals('String', info[1, 'type'])
    assertEquals(4, info[1, 'non-null'])
    assertEquals(1, info[1, 'null'])
    assertEquals(3, info[1, 'unique']) // Alice, Bob, Charlie
  }

  @Test
  void testFrequency() {
    def matrix = Matrix.builder()
        .data(
            color: ['red', 'blue', 'red', 'green', 'red', 'blue']
        )
        .types([String])
        .build()

    Matrix freq = SmileUtil.frequency(matrix, 'color')

    assertEquals(3, freq.rowCount())
    assertEquals(['value', 'frequency', 'percent'], freq.columnNames() as List)

    // Should be sorted by frequency descending
    assertEquals('red', freq[0, 'value'])
    assertEquals(3, freq[0, 'frequency'])
    assertEquals(50.0, freq[0, 'percent'] as double, 0.01)

    assertEquals('blue', freq[1, 'value'])
    assertEquals(2, freq[1, 'frequency'])
    assertEquals(33.33, freq[1, 'percent'] as double, 0.01)

    assertEquals('green', freq[2, 'value'])
    assertEquals(1, freq[2, 'frequency'])
    assertEquals(16.67, freq[2, 'percent'] as double, 0.01)
  }

  @Test
  void testFrequencyWithNulls() {
    def matrix = Matrix.builder()
        .data(
            value: ['a', null, 'a', null]
        )
        .types([String])
        .build()

    Matrix freq = SmileUtil.frequency(matrix, 'value')

    assertEquals(2, freq.rowCount())
    // Both 'a' and null appear twice
    assertEquals(2, freq[0, 'frequency'])
    assertEquals(2, freq[1, 'frequency'])
  }

  @Test
  void testRound() {
    assertEquals(3.14, SmileUtil.round(3.14159, 2), 0.001)
    assertEquals(3.1416, SmileUtil.round(3.14159, 4), 0.00001)
    assertEquals(3.0, SmileUtil.round(3.14159, 0), 0.001)
    assertTrue(Double.isNaN(SmileUtil.round(Double.NaN, 2)))
  }

  @Test
  void testRoundInvalidDecimals() {
    assertThrows(IllegalArgumentException) {
      SmileUtil.round(3.14, -1)
    }
  }
}
