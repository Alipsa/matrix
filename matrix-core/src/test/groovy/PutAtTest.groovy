import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix

import java.time.LocalDate

import static se.alipsa.matrix.core.ListConverter.toLocalDates

class PutAtTest {

  @CompileStatic
  @Test
  void testList() {
    // These all work
    def list = ['a', 'b', 'c']
    list[0] = 'aa'
    assert list[0] == 'aa'

    list.set(2, null)
    assert list[2] == null

    list.putAt(0, null)
    assert list[0] == null

  }

  // Does not work with compile static
  //@CompileStatic
  @Test
  void testListShortNotation() {
    def list = ['a', 'b', 'c']
    // This Fails when @CompileStatic is enabled
    list[1] = null
    assert list[1] == null : "Short notation not working when assigning null"
  }

  // Works both with compile static and without
  @CompileStatic
  @Test
  void testMap() {
    // These all work
    def map = [a: 'foo', b: 'bar', c: 'baz']
    map['a'] = 'aa'
    assert map['a'] == 'aa'

    map.put('c', null)
    assert map['c'] == null

    map.putAt('a', null)
    assert map['a'] == null

    map.b = null
    assert map['b'] == null  : "Short notation not working when assigning null"
  }

  // Does not work with compile static
  //@CompileStatic
  @Test
  void testMapShortNotation() {
    def map = [a: 'foo', b: 'bar', c: 'baz']
    // This Fails when @CompileStatic is enabled
    map['b'] = null
    assert map['b'] == null  : "Short notation not working when assigning null"
  }

  @CompileStatic
  class TwoD {
    List<List<?>> rows = []

    TwoD(List rows) {
      this.rows = rows
    }

    def putAt(List list, Object value) {
      //println "putAt: $list : $value"
      Integer rowIdx = list[0] as Integer
      Integer colIdx = list[1] as Integer
      // Must add parenthesis for assignment to work
      (rows[rowIdx][colIdx]) = value
    }

    // This is never called but when compiling static it must exists for it to compile
    // [Static type checking] - Cannot find matching method PutAtTest$TwoD#getAt(java.util.List<E>). Please check if the declared type is correct and if the method exists.
    def getAt(List where) {
      //println "getAt: $where"
      Integer rowIdx = where[0] as Integer
      Integer colIdx = where[1] as Integer
      return rows[rowIdx][colIdx]
    }

    String toString() {
      StringBuilder sb = new StringBuilder()
      rows.each {  List row ->
        row.each {sb.append(it).append(', ')}
        sb.append('\n')
      }
      sb
    }
  }

  @CompileStatic
  @Test
  void testOverride() {
    def d = new TwoD([[1,2,3], ['a', 'b', 'c']])
    //println d
    d[0,1] = null
    //println d
    assert d[0,1] == null
    assert d[1,2] == 'c'
  }

  @CompileStatic
  @Test
  void testAssignNullToMatrixElement() {
    def empData = Matrix.builder()
        .matrixName('empData')
        .data(
            emp_id: 1..5,
            emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
            salary: [623.3, 515.2, 611.0, 729.0, 843.25],
            start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
        )
        .types([int, String, Number, LocalDate])
        .build()
    empData[1, 0] = null
    //println(empData.content())
    assert empData[1, 0] == null
    assert empData[1, 1] == "Dan"
    empData["salary"][0] = null
    assert empData["salary"][0] == null

    assert 5 == empData.rowCount()
    assert 4 == empData.columnCount()
  }

  @Test
  void testAssignNullToMatrixElementDynamic() {
    def empData = Matrix.builder()
        .matrixName('empData')
        .data(
            emp_id: 1..5,
            emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
            salary: [623.3, 515.2, 611.0, 729.0, 843.25],
            start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
        )
        .types([int, String, Number, LocalDate])
        .build()
    empData[1, 0] = null
    //println(empData.content())
    assert empData[1, 0] == null
    assert empData[1, 1] == "Dan"
    empData["salary"][0] = null
    assert empData["salary"][0] == null
    // This only works with dynamic compile
    empData.salary[1] = null
    assert empData.salary[1] == null

    assert 5 == empData.rowCount()
    assert 4 == empData.columnCount()
  }
}
