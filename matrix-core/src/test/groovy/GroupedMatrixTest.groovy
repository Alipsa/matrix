import se.alipsa.matrix.core.GroupedMatrix
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat

import static org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GroupedMatrixTest {

  @Test
  void testGroupByReturnsGroupedMatrix() {
    def m = Matrix.builder().data(
        dept: ['IT', 'OPS', 'IT', 'OPS'],
        name: ['Alice', 'Bob', 'Carol', 'Dan'],
        salary: [100, 200, 150, 250]
    ).types(String, String, Integer).build()

    def grouped = Stat.groupBy(m, 'dept')
    assertNotNull(grouped)
    assertEquals(2, grouped.groupCount())
    assertEquals(['dept'], grouped.groupColumns())
  }

  @Test
  void testGetWithSingleKey() {
    def m = Matrix.builder().data(
        dept: ['IT', 'OPS', 'IT'],
        salary: [100, 200, 150]
    ).types(String, Integer).build()

    def grouped = Stat.groupBy(m, 'dept')
    Matrix it = grouped.get('IT')
    assertEquals(2, it.rowCount())
    assertEquals([100, 150], it['salary'] as List)

    Matrix ops = grouped.get('OPS')
    assertEquals(1, ops.rowCount())
  }

  @Test
  void testGetWithCompoundKey() {
    def m = Matrix.builder().data(
        dept: ['IT', 'IT', 'OPS', 'OPS'],
        type: ['FT', 'PT', 'FT', 'PT'],
        salary: [100, 80, 200, 150]
    ).types(String, String, Integer).build()

    def grouped = Stat.groupBy(m, 'dept', 'type')
    assertEquals(4, grouped.groupCount())
    assertEquals(1, grouped.get('IT', 'FT').rowCount())
    assertEquals([100], grouped.get('IT', 'FT')['salary'] as List)
  }

  @Test
  void testGetMissingKeyReturnsNull() {
    def m = Matrix.builder().data(
        dept: ['IT', 'OPS'],
        salary: [100, 200]
    ).types(String, Integer).build()

    def grouped = Stat.groupBy(m, 'dept')
    assertNull(grouped.get('HR'))
  }

  @Test
  void testGetWrongKeyCountThrows() {
    def m = Matrix.builder().data(
        dept: ['IT', 'OPS'],
        salary: [100, 200]
    ).types(String, Integer).build()

    def grouped = Stat.groupBy(m, 'dept')
    assertThrows(IllegalArgumentException) { grouped.get('IT', 'extra') }
  }

  @Test
  void testKeys() {
    def m = Matrix.builder().data(
        a: [1, 1, 2],
        b: ['X', 'Y', 'X']
    ).types(Integer, String).build()

    def grouped = Stat.groupBy(m, 'a', 'b')
    def keys = grouped.keys()
    assertTrue(keys.contains([1, 'X']))
    assertTrue(keys.contains([1, 'Y']))
    assertTrue(keys.contains([2, 'X']))
  }

  @Test
  void testLevel() {
    def m = Matrix.builder().data(
        country: ['USA', 'UK', 'USA', 'DE'],
        quarter: ['Q1', 'Q1', 'Q2', 'Q1'],
        sales: [100, 200, 300, 400]
    ).types(String, String, Integer).build()

    def grouped = Stat.groupBy(m, 'country', 'quarter')
    def countries = grouped.level('country')
    assertEquals(3, countries.size())
    assertTrue(countries.containsAll(['USA', 'UK', 'DE']))

    def quarters = grouped.level('quarter')
    assertTrue(quarters.containsAll(['Q1', 'Q2']))
  }

  @Test
  void testLevelInvalidColumnThrows() {
    def m = Matrix.builder().data(
        dept: ['IT', 'OPS'],
        salary: [100, 200]
    ).types(String, Integer).build()

    def grouped = Stat.groupBy(m, 'dept')
    assertThrows(IllegalArgumentException) { grouped.level('salary') }
  }

  @Test
  void testEach() {
    def m = Matrix.builder().data(
        dept: ['IT', 'OPS', 'IT'],
        salary: [100, 200, 150]
    ).types(String, Integer).build()

    def grouped = Stat.groupBy(m, 'dept')
    List<String> visited = []
    grouped.each { key, group ->
      visited.add(key[0] as String)
    }
    assertTrue(visited.containsAll(['IT', 'OPS']))
  }

  @Test
  void testAggWithNamedClosures() {
    def m = Matrix.builder().data(
        dept: ['IT', 'IT', 'OPS', 'OPS'],
        salary: [100, 150, 200, 250]
    ).types(String, Integer).build()

    def grouped = Stat.groupBy(m, 'dept')
    Matrix result = grouped.agg(salary: { Stat.sum(it) })
    assertEquals(2, result.rowCount())
    assertEquals(['dept', 'salary'], result.columnNames())

    // Find IT row
    def itRow = result.subset('dept', 'IT')
    assertEquals(250, itRow[0, 'salary'])

    def opsRow = result.subset('dept', 'OPS')
    assertEquals(450, opsRow[0, 'salary'])
  }

  @Test
  void testAggWithSingleClosure() {
    def m = Matrix.builder().data(
        dept: ['IT', 'IT', 'OPS'],
        salary: [100, 150, 200],
        bonus: [10, 15, 20]
    ).types(String, Integer, Integer).build()

    def grouped = Stat.groupBy(m, 'dept')
    Matrix result = grouped.agg { Stat.sum(it) }

    assertEquals(2, result.rowCount())
    assertEquals(['dept', 'salary', 'bonus'], result.columnNames())
  }

  @Test
  void testAggRejectsMissingAggregationColumn() {
    def m = Matrix.builder().data(
        dept: ['IT', 'OPS'],
        salary: [100, 200]
    ).types(String, Integer).build()

    def grouped = Stat.groupBy(m, 'dept')
    def ex = assertThrows(IllegalArgumentException) {
      grouped.agg(bonus: { Stat.sum(it) })
    }
    assertTrue(ex.message.contains('do not exist'))
  }

  @Test
  void testAggRejectsGroupColumnAggregationKey() {
    def m = Matrix.builder().data(
        dept: ['IT', 'OPS'],
        salary: [100, 200]
    ).types(String, Integer).build()

    def grouped = Stat.groupBy(m, 'dept')
    def ex = assertThrows(IllegalArgumentException) {
      grouped.agg(dept: { Stat.count(it) })
    }
    assertTrue(ex.message.contains('overlap group columns'))
  }

  @Test
  void testToStringKeyMap() {
    def m = Matrix.builder().data(
        a: [1, 1, 2],
        b: ['X', 'Y', 'X'],
        val: [10, 20, 30]
    ).types(Integer, String, Integer).build()

    def grouped = Stat.groupBy(m, 'a', 'b')
    Map<String, Matrix> map = grouped.toStringKeyMap()
    assertNotNull(map['1_X'])
    assertNotNull(map['1_Y'])
    assertNotNull(map['2_X'])
    assertEquals(1, map['1_X'].rowCount())
  }

  @Test
  void testToMap() {
    def m = Matrix.builder().data(
        dept: ['IT', 'OPS'],
        salary: [100, 200]
    ).types(String, Integer).build()

    def grouped = Stat.groupBy(m, 'dept')
    Map<List<?>, Matrix> map = grouped.toMap()
    assertNotNull(map[['IT']])
    assertNotNull(map[['OPS']])
  }

  @Test
  void testConstructorDefensivelyCopiesMutableKeys() {
    def m = Matrix.builder().data(
        dept: ['IT'],
        salary: [100]
    ).types(String, Integer).build()
    List<String> mutableKey = ['IT']
    Map<List<?>, Matrix> groups = [(mutableKey): m]

    GroupedMatrix grouped = new GroupedMatrix(m, ['dept'], groups)
    mutableKey[0] = 'OPS'

    assertNotNull(grouped.get('IT'))
    assertNull(grouped.get('OPS'))
    assertThrows(UnsupportedOperationException) { (grouped.keys()[0] as List).add('extra') }
  }

  @Test
  void testConstructorValidatesInputs() {
    def m = Matrix.builder().data(
        dept: ['IT'],
        salary: [100]
    ).types(String, Integer).build()

    assertThrows(IllegalArgumentException) {
      new GroupedMatrix(null, ['dept'], [(['IT']): m])
    }
    assertThrows(IllegalArgumentException) {
      new GroupedMatrix(m, [], [(['IT']): m])
    }
    assertThrows(IllegalArgumentException) {
      new GroupedMatrix(m, ['dept'], [(['IT', 'OPS']): m])
    }
    assertThrows(IllegalArgumentException) {
      new GroupedMatrix(m, ['dept'], [(['IT']): null])
    }
  }

  @Test
  void testSource() {
    def m = Matrix.builder().data(
        dept: ['IT', 'OPS'],
        salary: [100, 200]
    ).types(String, Integer).build()

    def grouped = Stat.groupBy(m, 'dept')
    assertSame(m, grouped.source())
  }

  @Test
  void testMatrixGroupByConvenience() {
    def m = Matrix.builder().data(
        dept: ['IT', 'OPS', 'IT'],
        salary: [100, 200, 150]
    ).types(String, Integer).build()

    def grouped = m.groupBy('dept')
    assertEquals(2, grouped.groupCount())
    assertEquals(2, grouped.get('IT').rowCount())
  }

  @Test
  void testToString() {
    def m = Matrix.builder().data(
        dept: ['IT', 'OPS'],
        salary: [100, 200]
    ).types(String, Integer).build()

    def grouped = Stat.groupBy(m, 'dept')
    assertTrue(grouped.toString().contains('groups=2'))
  }
}
