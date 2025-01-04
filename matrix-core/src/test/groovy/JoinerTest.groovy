import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Joiner

import static org.junit.jupiter.api.Assertions.*

class JoinerTest {

  @Test
  void testInnerJoin() {
    def e = Matrix.builder('employees').data([
        id       : 1..5,
        firstName: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
        salary   : [623.3, 515.2, 611.0, 729.0, 843.25],
        startDate: ["2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"]
    ]).build()

    def f = Matrix.builder('eln').data( [
        id      : 1..5,
        lastName: ["Smith", "Carpenter", "Bowman", "Carson", "McDougal"]
    ]).build()

    def merged = Joiner.merge(e, f, 'id')
    //println merged.toMarkdown()
    assertIterableEquals([1, 'Rick', 623.3, '2012-01-01', 'Smith'], merged.row(0))
    assertIterableEquals([5, 'Gary', 843.25, '2015-03-27', 'McDougal'], merged.row(4))


    // Make sure the original matrices was unaltered
    assertEquals(Matrix.builder('employees').data([
        id       : 1..5,
        firstName: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
        salary   : [623.3, 515.2, 611.0, 729.0, 843.25],
        startDate: ["2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"]
    ]).build(), e)
  }

  @Test
  void testLeftJoin() {
    def e = Matrix.builder()
        .matrixName('employees')
        .data([
            id       : 1..5,
            firstName: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
            salary   : [623.3, 515.2, 611.0, 729.0, 843.25],
            startDate: ["2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"]
        ])
        .build()
    def g = Matrix.builder()
        .matrixName('eln')
        .data([
            employeeId: 2..4,
            lastName  : ["Carpenter", "Bowman", "Carson"]
        ])
        .build()
    def leftJoined = Joiner.merge(e, g, [x: 'id', y: 'employeeId'], true)
    //println leftJoined.toMarkdown()
    assertEquals([1, 'Rick', 623.3, '2012-01-01', null], leftJoined.row(0))
    assertEquals([2, 'Dan', 515.2, '2013-09-23', 'Carpenter'], leftJoined.row(1))
    assertEquals([5, 'Gary', 843.25, '2015-03-27', null], leftJoined.row(4))

    // Make sure the original matrices was unaltered
    assertEquals(Matrix.builder()
        .matrixName('employees')
        .columns([
            id       : 1..5,
            firstName: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
            salary   : [623.3, 515.2, 611.0, 729.0, 843.25],
            startDate: ["2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"]
        ])
        .build(), e)
    assertEquals(Matrix.builder()
        .matrixName('eln')
        .data([
            employeeId: 2..4,
            lastName  : ["Carpenter", "Bowman", "Carson"]
        ]).build(), g)
  }
}
