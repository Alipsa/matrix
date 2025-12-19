import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.smile.Gsmile
import se.alipsa.matrix.smile.SmileUtil
import smile.data.DataFrame

import static org.junit.jupiter.api.Assertions.*

class GsmileTest {

  private Matrix createTestMatrix() {
    return Matrix.builder()
        .data(
            name: ['Alice', 'Bob', 'Charlie', 'Diana', 'Eve'],
            age: [25, 30, 35, 28, 42],
            salary: [50000.0, 60000.0, 75000.0, 55000.0, 90000.0],
            active: [true, true, false, true, false]
        )
        .types([String, Integer, Double, Boolean])
        .matrixName('employees')
        .build()
  }

  // ==================== Matrix Extension Tests ====================

  @Test
  void testMatrixToSmileDataFrame() {
    Matrix matrix = createTestMatrix()

    // Using extension method
    DataFrame df = Gsmile.toSmileDataFrame(matrix)

    assertNotNull(df)
    assertEquals(matrix.rowCount(), df.nrow())
    assertEquals(matrix.columnCount(), df.ncol())
  }

  @Test
  void testSmileDescribe() {
    Matrix matrix = createTestMatrix()

    Matrix stats = Gsmile.smileDescribe(matrix)

    assertNotNull(stats)
    assertTrue(stats.columnNames().contains('age'))
    assertTrue(stats.columnNames().contains('salary'))
    // Should have statistics like count, mean, etc.
    assertTrue(stats.rowCount() > 0)
  }

  @Test
  void testSmileSample() {
    Matrix matrix = createTestMatrix()

    Matrix sample = Gsmile.smileSample(matrix, 3)

    assertNotNull(sample)
    assertEquals(3, sample.rowCount())
    assertEquals(matrix.columnCount(), sample.columnCount())
  }

  // ==================== DataFrame Extension Tests ====================

  @Test
  void testDataFrameToMatrix() {
    Matrix original = createTestMatrix()
    DataFrame df = SmileUtil.toDataFrame(original)

    Matrix converted = Gsmile.toMatrix(df)

    assertNotNull(converted)
    assertEquals(original.rowCount(), converted.rowCount())
    assertEquals(original.columnCount(), converted.columnCount())
  }

  @Test
  void testDataFrameToMatrixWithName() {
    Matrix original = createTestMatrix()
    DataFrame df = SmileUtil.toDataFrame(original)

    Matrix converted = Gsmile.toMatrix(df, 'myMatrix')

    assertEquals('myMatrix', converted.matrixName)
  }

  @Test
  void testDataFrameGetAtRowIndex() {
    Matrix matrix = createTestMatrix()
    DataFrame df = SmileUtil.toDataFrame(matrix)

    Map<String, Object> row = Gsmile.getAt(df, 0)

    assertNotNull(row)
    assertEquals('Alice', row['name'])
    assertEquals(25, row['age'])
  }

  @Test
  void testDataFrameGetAtColumnName() {
    Matrix matrix = createTestMatrix()
    DataFrame df = SmileUtil.toDataFrame(matrix)

    def column = Gsmile.getAt(df, 'name')

    assertNotNull(column)
    assertEquals('name', column.name())
  }

  @Test
  void testDataFrameGetAtRange() {
    Matrix matrix = createTestMatrix()
    DataFrame df = SmileUtil.toDataFrame(matrix)

    DataFrame sliced = Gsmile.getAt(df, 0..2)

    assertNotNull(sliced)
    assertEquals(3, sliced.nrow()) // 0, 1, 2 = 3 rows
    assertEquals(df.ncol(), sliced.ncol())
  }

  @Test
  void testDataFrameGetAtColumnList() {
    Matrix matrix = createTestMatrix()
    DataFrame df = SmileUtil.toDataFrame(matrix)

    DataFrame selected = Gsmile.getAt(df, ['name', 'age'])

    assertNotNull(selected)
    assertEquals(df.nrow(), selected.nrow())
    assertEquals(2, selected.ncol())
  }

  @Test
  void testDataFrameGetAtRangeAndColumns() {
    Matrix matrix = createTestMatrix()
    DataFrame df = SmileUtil.toDataFrame(matrix)

    // Slice rows 0-2, then select columns in two steps
    DataFrame sliced = Gsmile.getAt(df, 0..2)
    DataFrame subset = sliced.select('name', 'salary')

    assertNotNull(subset)
    assertEquals(3, sliced.nrow())
    assertEquals(2, subset.ncol())
  }

  @Test
  void testDataFrameRowCount() {
    Matrix matrix = createTestMatrix()
    DataFrame df = SmileUtil.toDataFrame(matrix)

    int count = Gsmile.rowCount(df)

    assertEquals(5, count)
  }

  @Test
  void testDataFrameColumnCount() {
    Matrix matrix = createTestMatrix()
    DataFrame df = SmileUtil.toDataFrame(matrix)

    int count = Gsmile.columnCount(df)

    assertEquals(4, count)
  }

  @Test
  void testDataFrameColumnNames() {
    Matrix matrix = createTestMatrix()
    DataFrame df = SmileUtil.toDataFrame(matrix)

    List<String> names = Gsmile.columnNames(df)

    assertEquals(['name', 'age', 'salary', 'active'], names)
  }

  @Test
  void testDataFrameStructure() {
    Matrix matrix = createTestMatrix()
    DataFrame df = SmileUtil.toDataFrame(matrix)

    String structure = Gsmile.structure(df)

    assertNotNull(structure)
    assertTrue(structure.contains('5 rows'))
    assertTrue(structure.contains('4 columns'))
    assertTrue(structure.contains('name'))
    assertTrue(structure.contains('age'))
  }

  @Test
  void testDataFrameHead() {
    Matrix matrix = createTestMatrix()
    DataFrame df = SmileUtil.toDataFrame(matrix)

    DataFrame head = Gsmile.head(df, 3)

    assertEquals(3, head.nrow())
  }

  @Test
  void testDataFrameHeadDefault() {
    Matrix matrix = createTestMatrix()
    DataFrame df = SmileUtil.toDataFrame(matrix)

    DataFrame head = Gsmile.head(df)

    assertEquals(5, head.nrow()) // Default is 5, but we only have 5 rows
  }

  @Test
  void testDataFrameTail() {
    Matrix matrix = createTestMatrix()
    DataFrame df = SmileUtil.toDataFrame(matrix)

    DataFrame tail = Gsmile.tail(df, 2)

    assertEquals(2, tail.nrow())
    // Check that we got the last rows
    Matrix tailMatrix = Gsmile.toMatrix(tail)
    assertEquals('Diana', tailMatrix[0, 'name'])
    assertEquals('Eve', tailMatrix[1, 'name'])
  }

  @Test
  void testDataFrameFilter() {
    Matrix matrix = createTestMatrix()
    DataFrame df = SmileUtil.toDataFrame(matrix)

    DataFrame filtered = Gsmile.filter(df) { row ->
      (row['age'] as Integer) > 30
    }

    assertNotNull(filtered)
    assertEquals(2, filtered.nrow()) // Charlie (35) and Eve (42)
  }

  @Test
  void testDataFrameFilterNoMatches() {
    Matrix matrix = createTestMatrix()
    DataFrame df = SmileUtil.toDataFrame(matrix)

    DataFrame filtered = Gsmile.filter(df) { row ->
      (row['age'] as Integer) > 100
    }

    assertNotNull(filtered)
    assertEquals(0, filtered.nrow())
  }

  @Test
  void testDataFrameEachRow() {
    Matrix matrix = createTestMatrix()
    DataFrame df = SmileUtil.toDataFrame(matrix)

    List<String> names = []
    Gsmile.eachRow(df) { row ->
      names << row['name']
    }

    assertEquals(['Alice', 'Bob', 'Charlie', 'Diana', 'Eve'], names)
  }

  @Test
  void testDataFrameCollectRows() {
    Matrix matrix = createTestMatrix()
    DataFrame df = SmileUtil.toDataFrame(matrix)

    List<Integer> ages = Gsmile.collectRows(df) { row ->
      row['age'] as Integer
    }

    assertEquals([25, 30, 35, 28, 42], ages)
  }

  // ==================== Integration Tests ====================

  @Test
  void testRoundTripConversion() {
    Matrix original = createTestMatrix()

    // Matrix -> DataFrame -> Matrix
    DataFrame df = Gsmile.toSmileDataFrame(original)
    Matrix converted = Gsmile.toMatrix(df)

    assertEquals(original.rowCount(), converted.rowCount())
    assertEquals(original.columnCount(), converted.columnCount())
    assertEquals(original.columnNames(), converted.columnNames())
  }

  @Test
  void testChainedOperations() {
    Matrix matrix = createTestMatrix()

    // Chain: Matrix -> DataFrame -> filter -> slice -> Matrix
    DataFrame df = Gsmile.toSmileDataFrame(matrix)
    DataFrame filtered = Gsmile.filter(df) { row ->
      (row['salary'] as Double) >= 55000.0
    }
    DataFrame head = Gsmile.head(filtered, 3)
    Matrix result = Gsmile.toMatrix(head)

    assertTrue(result.rowCount() <= 3)
    assertTrue(result.rowCount() > 0)
  }

  @Test
  void testSlicingWithDifferentArguments() {
    Matrix matrix = createTestMatrix()
    DataFrame df = SmileUtil.toDataFrame(matrix)

    // Single row by index
    Map<String, Object> singleRow = Gsmile.getAt(df, 2)
    assertEquals('Charlie', singleRow['name'])

    // Column selection using select method directly
    DataFrame twoCols = df.select('name', 'age')
    assertEquals(2, twoCols.ncol())

    // Range slicing
    DataFrame rangeOnly = Gsmile.getAt(df, 1..3)
    assertEquals(3, rangeOnly.nrow())
  }
}
