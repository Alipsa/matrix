package spreadsheet

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.spreadsheet.fastods.FOdsImporter
import se.alipsa.matrix.spreadsheet.sods.SOdsImporter

import java.math.RoundingMode
import java.time.LocalDate

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertIterableEquals

class FOdsImporterTest {

  @Test
  void testOdsImport() {
    def table = FOdsImporter.create().importOds(
        this.class.getResource("/Book1.ods"),
        'Sheet1',
        1, 12,
        1, 4,
        true)
    assert ['id', 'foo', 'bar', 'baz'] == table.columnNames()
    table = table.convert(id: Integer, bar: LocalDate, baz: BigDecimal, 'yyyy-MM-dd HH:mm:ss.SSS')
    //println(table.content())
    assertEquals(3, table[2, 0])
    assertEquals(LocalDate.parse("2023-05-06"), table[6, 2])
    assertEquals(17.4, table['baz'][table.rowCount()-1])
    assertEquals(['id', 'foo', 'bar', 'baz'], table.columnNames())
  }

  @Test
  void TestImportSection() {
    def table = FOdsImporter.create().importOds(
        this.class.getResource("/positions.ods"), 1,
        2, 9,
        'B', 'E',
        false
    )
    //println(table.content())
    assert ['b2',	'c2',	'd2',	'e2'] == table.row(0).toList()
    assert ['b2', 'b3','b4','b5', 'b6','b7', 'b8', 'b9'] == table[0]
  }

  @Test
  void TestImportWithColnames() {
    def table = FOdsImporter.create().importOds(
        this.class.getResource("/Book2.ods"), 2,
        6, 10,
        'AC', 'BH',
        false
    )
    assert 32 == table.columnCount()
    //println(table.content())
    assert ['Component', 'Roof', 'Door', 'Window', 'Side wall'] == table[0]
    assert [1, 2043, 2044, 2045, 2040] == table[1]
  }

  @Test
  void testImportFromStream() {
    try(InputStream is = this.getClass().getResourceAsStream("/Book1.ods")) {
      Matrix table = FOdsImporter.create().importOds(
          is, "Sheet1", 1, 12, 'A', 'D', true
      )
      assert ['id', 'foo', 'bar', 'baz'] == table.columnNames()
      //println(table.content())
      assertEquals(3, table[2, 0, Integer])
      def date = table[6, 2]
      assertEquals(LocalDate.parse("2023-05-06"), date)
      assertEquals(17.4, table['baz'][table.rowCount()-1])
      assertEquals(['id', 'foo', 'bar', 'baz'], table.columnNames())
    }
  }

  @Test
  void testImportFromStreamOffset() {
    try(InputStream is = this.getClass().getResourceAsStream("/Book2.ods")) {
      Matrix m = FOdsImporter.create().importOds(is, "Sheet1", 3, 11, 2, 6, true)
      m.convert(id: Integer)
      //println m.content()
      assert 8 == m.rowCount()
      assert 5 == m.columnCount()
      assert ['id','OB','IB','deferred_interest_amount','percentdiff'] == m.columnNames()
      assert [710381,
              710792,
              712353,
              712472,
              712561,
              712835,
              713523,
              752810] == m['id']
    }
  }

  @Test
  void testImportMultipleSheets() {
    URL url = this.getClass().getResource("/Book2.ods")
    Map<Object, Matrix> sheets = FOdsImporter.create().importOdsSheets(url,
     [
         [sheetNumber: 1, startRow: 3, endRow: 11, startCol: 2, endCol: 6, firstRowAsColNames: true],
         [sheetNumber: 2, startRow: 2, endRow: 12, startCol: 'A', endCol: 'D', firstRowAsColNames: false],
         ['key': 'comp', sheetNumber: 2, startRow: 6, endRow: 10, startCol: 'AC', endCol: 'BH', firstRowAsColNames: false],
         ['key': 'comp2', sheetNumber: 2, startRow: 6, endRow: 10, startCol: 'AC', endCol: 'BH', firstRowAsColNames: true]
     ])
    assertEquals(4, sheets.size())
    Matrix table2 = sheets[2]
    table2.columnNames(['id', 'foo', 'bar', 'baz'])
    assertEquals(3, table2[2, 0, Integer])
    def date = table2[6, 2, LocalDate]
    assertEquals(LocalDate.parse("2023-05-06"), date)
    assertEquals(17.4 as Double, table2['baz'][table2.rowCount()-1] as Double, 0.00001)
    assertEquals(['id', 'foo', 'bar', 'baz'], table2.columnNames())

    Matrix table1 = sheets[1]
    assertEquals(710381, table1[0,0, Integer])
    assertEquals(103599.04, table1[1,1, BigDecimal].setScale(2, RoundingMode.HALF_UP))
    assertEquals(66952.95, table1[2,2, Double])
    assertEquals(0.0G, table1[3,3, BigDecimal].setScale(1, RoundingMode.HALF_UP))
    assertEquals(-0.00982 as Double, table1[6, 'percentdiff', Double], 0.00001d)
    assertIterableEquals(['id',	'OB',	'IB',	'deferred_interest_amount', 'percentdiff'], table1.columnNames())

    Matrix comp = sheets.comp.clone()
    //println comp.content()
    assertEquals('Component', comp[0,0])
    assertEquals(5, comp.rowCount())
    assertEquals(32, comp.columnCount())
    assertEquals(31, comp[0,31, Integer])

    def headerRow = comp.row(0)
    List<String> names = headerRow.collect{
      if (it == "Component") {
        String.valueOf(it)
      } else {
        String.valueOf(ValueConverter.asDouble(it).intValue())
      }
    }
    assertEquals(32, names.size())
    comp.columnNames(names)
    comp.removeRows(0)
    comp = comp.convert([String] + [Integer]*31 as List<Class<?>>)
    assertEquals(2043, comp[0,1])
    assertEquals(2790, comp[3,31])

    Matrix comp2 = sheets.comp2
    assertIterableEquals(sheets.comp.typeNames(), comp2.typeNames(), "column types differ")
    comp2.columnNames(comp2.columnNames().collect{
      if (it == "Component") {
        String.valueOf(it)
      } else {
        String.valueOf(ValueConverter.asDouble(it).intValue())
      }
    })
    assertIterableEquals(comp.columnNames(), comp2.columnNames(), "column names differ")
    comp2 = comp2.convert([String] + [Integer]*31 as List<Class<?>>)
    assertIterableEquals(comp.typeNames(), comp2.typeNames(), "column types differ after column name setting")
  }

  @Test
  void testFormulas() {
    URL file = this.getClass().getResource("/Book3.ods")
    Matrix m = SOdsImporter.importOds(file, 1, 2, 8, 'A', 'G', false)
        .convert('c3': LocalDate)
    //println m.content()
    assertEquals(21, m[6, 0, Integer])
    assertEquals(LocalDate.parse('2025-03-20'), m[5, 2])
    assertEquals(m['c4'].subList(0..5).average() as double, m[6,'c4'] as double, 0.000001d)
    assertEquals('foobar1', m[0, 'c7'])

    m = FOdsImporter.create().importOds(file, 1, 2, 8, 'A', 'G', false)
        .convert('c3': LocalDate)
    //println m.content()
    assertEquals(21, m[6, 0, Integer])
    assertEquals(LocalDate.parse('2025-03-20'), m[5, 2])
    assertEquals(m['c4'].subList(0..5).average() as double, m[6,'c4'] as double, 0.000001d)
    assertEquals('foobar1', m[0, 'c7'])
  }
}
