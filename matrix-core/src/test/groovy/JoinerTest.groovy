import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.JoinType
import se.alipsa.matrix.core.Joiner
import se.alipsa.matrix.core.Matrix

class JoinerTest {

  @Test
  void testInnerJoin() {
    def e = Matrix.builder('employees').data([
        id       : 1..5,
        firstName: ['Rick', 'Dan', 'Michelle', 'Ryan', 'Gary'],
        salary   : [623.3, 515.2, 611.0, 729.0, 843.25],
        startDate: ['2012-01-01', '2013-09-23', '2014-11-15', '2014-05-11', '2015-03-27']
    ]).build()

    def f = Matrix.builder('eln').data([
        id      : 1..5,
        lastName: ['Smith', 'Carpenter', 'Bowman', 'Carson', 'McDougal']
    ]).build()

    def merged = Joiner.merge(e, f, 'id')
    assertIterableEquals([1, 'Rick', 623.3, '2012-01-01', 'Smith'], merged.row(0))
    assertIterableEquals([5, 'Gary', 843.25, '2015-03-27', 'McDougal'], merged.row(4))

    assertEquals(Matrix.builder('employees').data([
        id       : 1..5,
        firstName: ['Rick', 'Dan', 'Michelle', 'Ryan', 'Gary'],
        salary   : [623.3, 515.2, 611.0, 729.0, 843.25],
        startDate: ['2012-01-01', '2013-09-23', '2014-11-15', '2014-05-11', '2015-03-27']
    ]).build(), e)
  }

  @Test
  void testLeftJoin() {
    def e = Matrix.builder()
        .matrixName('employees')
        .data([
            id       : 1..5,
            firstName: ['Rick', 'Dan', 'Michelle', 'Ryan', 'Gary'],
            salary   : [623.3, 515.2, 611.0, 729.0, 843.25],
            startDate: ['2012-01-01', '2013-09-23', '2014-11-15', '2014-05-11', '2015-03-27']
        ])
        .build()
    def g = Matrix.builder()
        .matrixName('eln')
        .data([
            employeeId: 2..4,
            lastName  : ['Carpenter', 'Bowman', 'Carson']
        ])
        .build()
    def leftJoined = Joiner.merge(e, g, [x: 'id', y: 'employeeId'], true)
    assertEquals([1, 'Rick', 623.3, '2012-01-01', null], leftJoined.row(0))
    assertEquals([2, 'Dan', 515.2, '2013-09-23', 'Carpenter'], leftJoined.row(1))
    assertEquals([5, 'Gary', 843.25, '2015-03-27', null], leftJoined.row(4))

    assertEquals(Matrix.builder()
        .matrixName('employees')
        .columns([
            id       : 1..5,
            firstName: ['Rick', 'Dan', 'Michelle', 'Ryan', 'Gary'],
            salary   : [623.3, 515.2, 611.0, 729.0, 843.25],
            startDate: ['2012-01-01', '2013-09-23', '2014-11-15', '2014-05-11', '2015-03-27']
        ])
        .build(), e)
    assertEquals(Matrix.builder()
        .matrixName('eln')
        .data([
            employeeId: 2..4,
            lastName  : ['Carpenter', 'Bowman', 'Carson']
        ]).build(), g)
  }

  @Test
  void testMergeRejectsMissingJoinColumn() {
    def employees = Matrix.builder()
        .matrixName('employees')
        .data([
            id  : 1..2,
            name: ['Rick', 'Dan']
        ])
        .build()
    def names = Matrix.builder()
        .matrixName('names')
        .data([
            employeeId: 1..2,
            lastName  : ['Smith', 'Carpenter']
        ])
        .build()

    def error = assertThrows(IllegalArgumentException) {
      Joiner.merge(employees, names, [x: 'missing', y: 'employeeId'])
    }

    assertEquals('Join column \'missing\' does not exist in matrix \'employees\'', error.message)
  }

  @Test
  void testOneToManyInnerJoin() {
    def orders = Matrix.builder('orders').data([
        orderId   : [1, 2, 3],
        customerId: [10, 20, 10]
    ]).types([Integer, Integer]).build()

    def items = Matrix.builder('items').data([
        orderId: [1, 1, 2, 3, 3, 3],
        product: ['Pen', 'Paper', 'Ink', 'Tape', 'Glue', 'Clip']
    ]).types([Integer, String]).build()

    def result = Joiner.merge(orders, items, 'orderId')
    assertEquals(6, result.rowCount())
    assertEquals([1, 1, 2, 3, 3, 3], result.column('orderId') as List)
    assertEquals(['Pen', 'Paper', 'Ink', 'Tape', 'Glue', 'Clip'], result.column('product') as List)
    assertEquals([10, 10, 20, 10, 10, 10], result.column('customerId') as List)
  }

  @Test
  void testOneToManyLeftJoin() {
    def x = Matrix.builder('x').data([
        id  : [1, 2, 3],
        name: ['A', 'B', 'C']
    ]).types([Integer, String]).build()

    def y = Matrix.builder('y').data([
        id  : [1, 1, 2],
        item: ['X', 'Y', 'Z']
    ]).types([Integer, String]).build()

    def result = Joiner.merge(x, y, 'id', true)
    assertEquals(4, result.rowCount())
    assertEquals([1, 1, 2, 3], result.column('id') as List)
    assertEquals(['A', 'A', 'B', 'C'], result.column('name') as List)
    assertEquals(['X', 'Y', 'Z', null], result.column('item') as List)
  }

  @Test
  void testTypePreservation() {
    def x = Matrix.builder('x').data([
        id   : [1, 2, 3],
        value: [10.5, 20.3, 30.1]
    ]).types([Integer, BigDecimal]).build()

    def y = Matrix.builder('y').data([
        id  : [1, 2, 3],
        name: ['A', 'B', 'C']
    ]).types([Integer, String]).build()

    def result = Joiner.merge(x, y, 'id')
    assertEquals([Integer, BigDecimal, String], result.types())
  }

  @Test
  void testRightJoin() {
    def x = Matrix.builder('x').data([
        id  : [1, 2, 3],
        name: ['A', 'B', 'C']
    ]).types([Integer, String]).build()

    def y = Matrix.builder('y').data([
        id   : [2, 3, 4],
        score: [80, 90, 70]
    ]).types([Integer, Integer]).build()

    def result = Joiner.merge(x, y, 'id', JoinType.RIGHT)
    assertEquals(3, result.rowCount())
    assertEquals([2, 3, 4], result.column('id') as List)
    assertEquals(['B', 'C', null], result.column('name') as List)
    assertEquals([80, 90, 70], result.column('score') as List)
  }

  @Test
  void testFullOuterJoin() {
    def x = Matrix.builder('x').data([
        id  : [1, 2, 3],
        name: ['A', 'B', 'C']
    ]).types([Integer, String]).build()

    def y = Matrix.builder('y').data([
        id   : [2, 3, 4],
        score: [80, 90, 70]
    ]).types([Integer, Integer]).build()

    def result = Joiner.merge(x, y, 'id', JoinType.FULL)
    assertEquals(4, result.rowCount())
    assertEquals([1, 2, 3, 4], result.column('id') as List)
    assertEquals(['A', 'B', 'C', null], result.column('name') as List)
    assertEquals([null, 80, 90, 70], result.column('score') as List)
  }

  @Test
  void testMultiColumnJoinKeys() {
    def x = Matrix.builder('x').data([
        dept : ['Sales', 'Sales', 'Eng'],
        empId: [1, 2, 1],
        name : ['Alice', 'Bob', 'Carol']
    ]).types([String, Integer, String]).build()

    def y = Matrix.builder('y').data([
        department: ['Sales', 'Eng', 'Sales'],
        employeeId: [1, 1, 2],
        rating    : [5, 4, 3]
    ]).types([String, Integer, Integer]).build()

    def result = Joiner.merge(x, y,
        [x: ['dept', 'empId'], y: ['department', 'employeeId']],
        JoinType.INNER)
    assertEquals(3, result.rowCount())
    assertEquals(['Alice', 'Bob', 'Carol'], result.column('name') as List)
    assertEquals([5, 3, 4], result.column('rating') as List)
  }

  @Test
  void testListOfStringByOverload() {
    def x = Matrix.builder('x').data([
        dept : ['Sales', 'Eng'],
        empId: [1, 2],
        name : ['A', 'B']
    ]).types([String, Integer, String]).build()

    def y = Matrix.builder('y').data([
        dept  : ['Sales', 'Eng'],
        empId : [1, 2],
        rating: [5, 4]
    ]).types([String, Integer, Integer]).build()

    def result = Joiner.merge(x, y, ['dept', 'empId'], JoinType.INNER)
    assertEquals(2, result.rowCount())
    assertEquals([5, 4], result.column('rating') as List)
  }

  @Test
  void testDuplicateColumnNames() {
    def x = Matrix.builder('x').data([
        id   : [1, 2],
        value: [10, 20]
    ]).types([Integer, Integer]).build()

    def y = Matrix.builder('y').data([
        id   : [1, 2],
        value: [100, 200]
    ]).types([Integer, Integer]).build()

    def result = Joiner.merge(x, y, 'id')
    assertTrue(result.columnNames().contains('value_x'))
    assertTrue(result.columnNames().contains('value_y'))
    assertEquals([10, 20], result.column('value_x') as List)
    assertEquals([100, 200], result.column('value_y') as List)
  }

  @Test
  void testNullKeys() {
    def x = Matrix.builder('x').data([
        id  : [1, null, 3],
        name: ['A', 'B', 'C']
    ]).build()

    def y = Matrix.builder('y').data([
        id   : [1, null, 4],
        score: [80, 90, 70]
    ]).build()

    def result = Joiner.merge(x, y, 'id')
    assertEquals(2, result.rowCount())
    assertEquals([1, null], result.column('id') as List)
    assertEquals([80, 90], result.column('score') as List)
  }

  @Test
  void testEmptyLeftMatrix() {
    def x = Matrix.builder('x')
        .columnNames(['id', 'name'])
        .types([Integer, String])
        .rows([])
        .build()

    def y = Matrix.builder('y').data([
        id   : [1, 2],
        score: [80, 90]
    ]).types([Integer, Integer]).build()

    def result = Joiner.merge(x, y, 'id')
    assertEquals(0, result.rowCount())
    assertEquals(['id', 'name', 'score'], result.columnNames())
  }

  @Test
  void testEmptyRightMatrix() {
    def x = Matrix.builder('x').data([
        id  : [1, 2],
        name: ['A', 'B']
    ]).types([Integer, String]).build()

    def y = Matrix.builder('y')
        .columnNames(['id', 'score'])
        .types([Integer, Integer])
        .rows([])
        .build()

    def result = Joiner.merge(x, y, 'id', true)
    assertEquals(2, result.rowCount())
    assertEquals([null, null], result.column('score') as List)
  }

  @Test
  void testKeySizeMismatchThrows() {
    def x = Matrix.builder('x').data([a: [1], b: [2]]).build()
    def y = Matrix.builder('y').data([c: [1], d: [2]]).build()

    def error = assertThrows(IllegalArgumentException) {
      Joiner.merge(x, y, [x: ['a', 'b'], y: ['c']], JoinType.INNER)
    }
    assertTrue(error.message.contains('same size'))
  }

  @Test
  void testMultiKeyRejectsMissingColumn() {
    def x = Matrix.builder('x').data([a: [1], b: [2]]).build()
    def y = Matrix.builder('y').data([c: [1], d: [2]]).build()

    def error = assertThrows(IllegalArgumentException) {
      Joiner.merge(x, y, [x: ['a', 'missing'], y: ['c', 'd']], JoinType.INNER)
    }
    assertTrue(error.message.contains('missing'))
  }

  @Test
  void testJoinTypeEnumInnerJoin() {
    def e = Matrix.builder('employees').data([
        id       : 1..5,
        firstName: ['Rick', 'Dan', 'Michelle', 'Ryan', 'Gary']
    ]).types([Integer, String]).build()

    def f = Matrix.builder('eln').data([
        id      : 1..5,
        lastName: ['Smith', 'Carpenter', 'Bowman', 'Carson', 'McDougal']
    ]).types([Integer, String]).build()

    def result = Joiner.merge(e, f, 'id', JoinType.INNER)
    assertEquals(5, result.rowCount())
    assertIterableEquals([1, 'Rick', 'Smith'], result.row(0))
  }

  @Test
  void testRightJoinKeyValuesPreserved() {
    def x = Matrix.builder('x').data([
        id  : [1, 2],
        name: ['A', 'B']
    ]).types([Integer, String]).build()

    def y = Matrix.builder('y').data([
        id   : [3, 4],
        score: [70, 80]
    ]).types([Integer, Integer]).build()

    def result = Joiner.merge(x, y, 'id', JoinType.RIGHT)
    assertEquals(2, result.rowCount())
    assertEquals([3, 4], result.column('id') as List)
    assertEquals([null, null], result.column('name') as List)
    assertEquals([70, 80], result.column('score') as List)
  }

  @Test
  void testMissingByKeysThrows() {
    def x = Matrix.builder('x').data([id: [1]]).build()
    def y = Matrix.builder('y').data([id: [1]]).build()

    assertThrows(IllegalArgumentException) {
      Joiner.merge(x, y, [x: 'id'], JoinType.INNER)
    }
  }

  @Test
  void testCrossJoinTypeThroughMergeDelegatesToCrossJoin() {
    def x = Matrix.builder('x').data([id: [1, 2]]).build()
    def y = Matrix.builder('y').data([id: [10, 20]]).build()

    def result = Joiner.merge(x, y, 'id', JoinType.CROSS)

    assertEquals(4, result.rowCount())
    assertEquals(['id_x', 'id_y'], result.columnNames())
    assertEquals([1, 1, 2, 2], result.column('id_x') as List)
    assertEquals([10, 20, 10, 20], result.column('id_y') as List)
  }

  @Test
  void testCrossJoin() {
    def x = Matrix.builder('x').data([
        color: ['red', 'blue']
    ]).types([String]).build()

    def y = Matrix.builder('y').data([
        size: ['S', 'M', 'L']
    ]).types([String]).build()

    def result = Joiner.crossJoin(x, y)
    assertEquals(6, result.rowCount())
    assertEquals(['color', 'size'], result.columnNames())
    assertEquals(['red', 'red', 'red', 'blue', 'blue', 'blue'], result.column('color') as List)
    assertEquals(['S', 'M', 'L', 'S', 'M', 'L'], result.column('size') as List)
    assertEquals([String, String], result.types())
  }

  @Test
  void testCrossJoinDuplicateColumnNames() {
    def x = Matrix.builder('x').data([
        id: [1, 2], name: ['A', 'B']
    ]).types([Integer, String]).build()

    def y = Matrix.builder('y').data([
        id: [10, 20], name: ['X', 'Y']
    ]).types([Integer, String]).build()

    def result = Joiner.crossJoin(x, y)
    assertEquals(4, result.rowCount())
    assertEquals(['id_x', 'name_x', 'id_y', 'name_y'], result.columnNames())
    assertEquals([1, 1, 2, 2], result.column('id_x') as List)
    assertEquals([10, 20, 10, 20], result.column('id_y') as List)
  }

  @Test
  void testCrossJoinWithEmptyMatrix() {
    def x = Matrix.builder('x').data([a: [1, 2]]).types([Integer]).build()
    def y = Matrix.builder('y')
        .columnNames(['b'])
        .types([Integer])
        .rows([])
        .build()

    def result = Joiner.crossJoin(x, y)
    assertEquals(0, result.rowCount())
    assertEquals(['a', 'b'], result.columnNames())
  }

  @Test
  void testSemiJoin() {
    def x = Matrix.builder('x').data([
        id  : [1, 2, 3, 4],
        name: ['A', 'B', 'C', 'D']
    ]).types([Integer, String]).build()

    def y = Matrix.builder('y').data([
        id   : [2, 3, 5],
        score: [80, 90, 70]
    ]).types([Integer, Integer]).build()

    def result = Joiner.merge(x, y, 'id', JoinType.SEMI)
    assertEquals(2, result.rowCount())
    assertEquals(['id', 'name'], result.columnNames())
    assertEquals([2, 3], result.column('id') as List)
    assertEquals(['B', 'C'], result.column('name') as List)
  }

  @Test
  void testSemiJoinNoDuplicateRows() {
    def x = Matrix.builder('x').data([
        id  : [1, 2],
        name: ['A', 'B']
    ]).types([Integer, String]).build()

    def y = Matrix.builder('y').data([
        id   : [1, 1, 1],
        score: [80, 90, 70]
    ]).types([Integer, Integer]).build()

    def result = Joiner.merge(x, y, 'id', JoinType.SEMI)
    assertEquals(1, result.rowCount())
    assertEquals([1], result.column('id') as List)
  }

  @Test
  void testAntiJoin() {
    def x = Matrix.builder('x').data([
        id  : [1, 2, 3, 4],
        name: ['A', 'B', 'C', 'D']
    ]).types([Integer, String]).build()

    def y = Matrix.builder('y').data([
        id   : [2, 3, 5],
        score: [80, 90, 70]
    ]).types([Integer, Integer]).build()

    def result = Joiner.merge(x, y, 'id', JoinType.ANTI)
    assertEquals(2, result.rowCount())
    assertEquals(['id', 'name'], result.columnNames())
    assertEquals([1, 4], result.column('id') as List)
    assertEquals(['A', 'D'], result.column('name') as List)
  }

  @Test
  void testAntiJoinNoMatches() {
    def x = Matrix.builder('x').data([
        id: [1, 2]
    ]).types([Integer]).build()

    def y = Matrix.builder('y').data([
        id: [3, 4]
    ]).types([Integer]).build()

    def result = Joiner.merge(x, y, 'id', JoinType.ANTI)
    assertEquals(2, result.rowCount())
    assertEquals([1, 2], result.column('id') as List)
  }

  @Test
  void testAntiJoinAllMatch() {
    def x = Matrix.builder('x').data([
        id: [1, 2]
    ]).types([Integer]).build()

    def y = Matrix.builder('y').data([
        id: [1, 2]
    ]).types([Integer]).build()

    def result = Joiner.merge(x, y, 'id', JoinType.ANTI)
    assertEquals(0, result.rowCount())
  }

  @Test
  void testSelfJoin() {
    def employees = Matrix.builder('employees').data([
        id       : [1, 2, 3],
        name     : ['Alice', 'Bob', 'Carol'],
        managerId: [null, 1, 1]
    ]).types([Integer, String, Integer]).build()

    def result = Joiner.merge(employees, employees,
        [x: 'managerId', y: 'id'], JoinType.INNER)
    assertEquals(2, result.rowCount())
    assertEquals(['id', 'name_x', 'managerId', 'name_y', 'managerId_y'], result.columnNames())
    assertEquals([1, 1], result.column('managerId') as List)
    assertEquals([null, null], result.column('managerId_y') as List)
    assertEquals(['Bob', 'Carol'], result.column('name_x') as List)
    assertEquals(['Alice', 'Alice'], result.column('name_y') as List)
  }

}
