package test.alipsa.groovy.matrix.tablesaw

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertThrowsExactly
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.core.ListConverter.toLocalDates
import static tech.tablesaw.api.ColumnType.*

import org.junit.jupiter.api.Test
import tech.tablesaw.api.BigDecimalAggregateFunctions
import tech.tablesaw.api.BigDecimalColumn
import tech.tablesaw.api.BooleanColumn
import tech.tablesaw.api.ColumnType
import tech.tablesaw.api.DateColumn
import tech.tablesaw.api.DoubleColumn
import tech.tablesaw.api.IntColumn
import tech.tablesaw.api.ShortColumn
import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table
import tech.tablesaw.column.numbers.BigDecimalColumnType
import tech.tablesaw.io.csv.CsvReadOptions
import tech.tablesaw.joining.JoinType

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.tablesaw.Normalizer
import se.alipsa.matrix.tablesaw.TableUtil
import se.alipsa.matrix.tablesaw.gtable.GdataFrameJoiner
import se.alipsa.matrix.tablesaw.gtable.Gtable

import java.time.LocalDate

class GtableTest {

  @Test
  void testProgrammaticCreation() {
    def empData = [
        emp_id: 1..5,
        emp_name: ['Rick', 'Dan', 'Michelle', 'Ryan', 'Gary'],
        salary: [623.3d, 515.2d, 611.0d, 729.0d, 843.25d],
        start_date: toLocalDates('2012-01-01', '2013-09-23', '2014-11-15', '2014-05-11', '2015-03-27')
        ]
    Gtable table = Gtable.create(empData, [INTEGER, STRING, DOUBLE, LOCAL_DATE])
    assertEquals(5, table.rowCount(), 'number of rows')
    assertEquals(4, table.columnCount(), 'number of columns')
    assertEquals('Gary', table[4, 1])
    assertEquals('Gary', table[4, 'emp_name'])
  }

  @Test
  void testCreateWithInferredTypes() {
    def data = [
        name: ['Alice', 'Bob'],
        age: [25, 30],
        salary: [50000.0, 60000.0]
    ]
    Gtable table = Gtable.create(data)
    assertEquals(2, table.rowCount())
    assertEquals(3, table.columnCount())
    assertEquals(STRING, table.column('name').type())
    assertEquals(INTEGER, table.column('age').type())
    assertEquals(BigDecimalColumnType.instance(), table.column('salary').type())
  }

  @Test
  void testCreateWithTypeOverrides() {
    def data = [
        name: ['Alice', 'Bob'],
        age: [25, 30],
        salary: [50000.0, 60000.0]
    ]
    Gtable table = Gtable.create(data, [salary: BigDecimalColumnType.instance()])
    assertEquals(STRING, table.column('name').type())
    assertEquals(INTEGER, table.column('age').type())
    assertEquals(BigDecimalColumnType.instance(), table.column('salary').type())
  }

  @Test
  void testCreateAcceptsGStringAndWidenedValues() {
    def who = 'Alice'
    Gtable table = Gtable.create(
        [name: ["${who}", 'Ann'], salary: [50000, 60000]],
        [STRING, BigDecimalColumnType.instance()])
    assertEquals('Alice', table.column('name').getString(0))
    assertEquals(new BigDecimal('60000'), table.column('salary').getBigDecimal(1))
  }

  @Test
  void testCreateValidatesDeclaredTypes() {
    def data = [name: ['Alice'], age: [25]]
    assertThrows(IllegalArgumentException) { Gtable.create(data, [STRING]) }
    assertThrows(IllegalArgumentException) { Gtable.create(data, [STRING, INTEGER, DOUBLE]) }
    assertThrows(IllegalArgumentException) { Gtable.create(data, [STRING, null]) }
    assertThrows(IllegalArgumentException) { Gtable.create(data, [STRING, SKIP]) }
    assertThrows(IllegalArgumentException) { Gtable.create(data, null as List<ColumnType>) }
  }

  @Test
  void testCreateValidatesTypeOverrides() {
    def data = [name: ['Alice'], age: [25]]
    assertThrows(IllegalArgumentException) {
      Gtable.create(data, [unknown: STRING] as LinkedHashMap<String, ColumnType>)
    }
    assertThrows(IllegalArgumentException) {
      Gtable.create(data, [age: null] as LinkedHashMap<String, ColumnType>)
    }
    assertThrows(IllegalArgumentException) {
      Gtable.create(data, [age: SKIP] as LinkedHashMap<String, ColumnType>)
    }
    def valid = Gtable.create(data, [age: INTEGER] as LinkedHashMap<String, ColumnType>)
    assertEquals(INTEGER, valid.column('age').type())
  }

  @Test
  void testCreateRejectsMismatchedColumnLengths() {
    def data = [
        name: ['Alice', 'Bob'],
        age: [25, 30, 35]
    ]
    def ex = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException) { ->
      Gtable.create(data)
    }
    assertTrue(ex.message.contains('age'))
    assertTrue(ex.message.contains('3 rows'))
  }

  @Test
  void testCreateFromCsv() {
    def csv = getClass().getResource('/glaciers.csv')
    CsvReadOptions.Builder builder = CsvReadOptions.builder(csv)
        .separator(',' as Character)
        .columnTypes([INTEGER, BigDecimalColumnType.instance(), INTEGER] as ColumnType[])

    Gtable glaciers = Gtable.read().usingOptions(builder.build())

    assertEquals(1946, glaciers[1, 0])
    assertEquals(-3.19, glaciers[2, 1])
    assertEquals(1, glaciers[3, 2])
  }

  @Test
  void testCsvReaderReturnsGtableForPathAndFile() {
    File csv = new File(getClass().getResource('/glaciers.csv').toURI())

    def fromPath = Gtable.read().csv(csv.absolutePath)
    def fromFile = Gtable.read().csv(csv)

    assertTrue(fromPath instanceof Gtable)
    assertTrue(fromFile instanceof Gtable)
    assertEquals(70, fromPath.rowCount())
    assertEquals(70, fromFile.rowCount())
  }

  @Test
  void testCsvReaderReturnsGtableForOptions() {
    def csv = getClass().getResource('/glaciers.csv')
    CsvReadOptions options = CsvReadOptions.builder(csv)
        .separator(',' as Character)
        .columnTypes([INTEGER, BigDecimalColumnType.instance(), INTEGER] as ColumnType[])
        .build()

    def fromOptions = Gtable.read().csv(options)
    def fromBuilder = Gtable.read().csv(CsvReadOptions.builder(csv))

    assertTrue(fromOptions instanceof Gtable)
    assertTrue(fromBuilder instanceof Gtable)
    assertEquals(1946, fromOptions[1, 0])
    assertEquals(70, fromBuilder.rowCount())
  }

  @Test
  void testExportToCsv() {
    def empData = [
        emp_id: 1..5,
        emp_name: ['Rick', 'Dan', 'Michelle', 'Ryan', 'Gary'],
        salary: [623.3d, 515.2d, 611.0d, 729.0d, 843.25d],
        start_date: toLocalDates('2012-01-01', '2013-09-23', '2014-11-15', '2014-05-11', '2015-03-27')
    ]
    Gtable table = Gtable.create(empData, [INTEGER, STRING, DOUBLE, LOCAL_DATE])

    File file = File.createTempFile('empData', '.csv')
    table.write().toFile(file)
    List<String> lines = file.readLines()
    assertEquals(6, lines.size())
    assertEquals('emp_id,emp_name,salary,start_date', lines[0])
    assertEquals('1,Rick,623.3,2012-01-01', lines[1])
    file.delete()
  }

  @Test
  void testJoinAndConcat() {
    def empData = [
        emp_id: 1..5,
        emp_name: ['Rick', 'Dan', 'Michelle', 'Ryan', 'Gary'],
        salary: [623.3d, 515.2d, 611.0d, 729.0d, 843.25d],
        start_date: toLocalDates('2012-01-01', '2013-09-23', '2014-11-15', '2014-05-11', '2015-03-27')
    ]
    Gtable table = Gtable.create(empData, [INTEGER, STRING, DOUBLE, LOCAL_DATE])

    Gtable table2 = Gtable.create([
        employee_id: [1, 2, 3, 4, null],
        performance: [0.76d, 0.79d, 0.68d, 1.10d, 0.91d]
    ], [INTEGER, DOUBLE])

    // does not mutate the table
    Gtable joined = table.joinOn('emp_id').inner(table2, 'employee_id')
    assertEquals(4, joined.rowCount(), 'joined rowcount, inner join should have removed a row')
    assertEquals(5, joined.columnCount(), 'joined columnCount')

    // mutates the table!
    table.concat(table2)
    assertEquals(5, table.rowCount(), 'concat rowcount')
    assertEquals(6, table.columnCount(), 'concat columnCount')
  }

  @Test
  void testFluentJoinReturnsGtable() {
    Gtable employees = Gtable.create([
        id: [1, 2, 3],
        name: ['Rick', 'Dan', 'Michelle']
    ])
    Gtable performance = Gtable.create([
        id: [1, 3, 4],
        score: [0.76, 0.68, 0.91]
    ])

    def joined = employees.joinOn('id')
        .type(JoinType.INNER)
        .with(performance)
        .join()

    assertTrue(joined instanceof Gtable)
    assertEquals(2, joined.rowCount())
    assertEquals(['id', 'name', 'score'], joined.columnNames())
  }

  @Test
  void testFluentJoinOptionsReturnGdataFrameJoiner() {
    Gtable employees = Gtable.create([
        emp_id: [1, 2, 3],
        name: ['Rick', 'Dan', 'Michelle']
    ])
    Gtable performance = Gtable.create([
        employee_id: [1, 3, 4],
        score: [0.76, 0.68, 0.91]
    ])

    def joiner = employees.joinOn('emp_id')
    assertTrue(joiner.type(JoinType.LEFT_OUTER) instanceof GdataFrameJoiner)
    assertTrue(joiner.keepAllJoinKeyColumns(true) instanceof GdataFrameJoiner)
    assertTrue(joiner.allowDuplicateColumnNames(true) instanceof GdataFrameJoiner)
    assertTrue(joiner.rightJoinColumns('employee_id') instanceof GdataFrameJoiner)
    assertTrue(joiner.with(performance) instanceof GdataFrameJoiner)

    def joined = joiner.join()

    assertTrue(joined instanceof Gtable)
    assertEquals(3, joined.rowCount())
    assertTrue(joined.columnNames().contains('employee_id'))
    assertTrue(joined.columnNames().contains('score'))
  }

  @Test
  void testShortHandPut() {
    def empData = [
        emp_id: 1..5,
        emp_name: ['Rick', 'Dan', 'Michelle', 'Ryan', 'Gary'],
        salary: [623.3d, 515.2d, 611.0d, 729.0d, 843.25d],
        start_date: toLocalDates('2012-01-01', '2013-09-23', '2014-11-15', '2014-05-11', '2015-03-27')
    ]
    Gtable table = Gtable.create(empData, [INTEGER, STRING, DOUBLE, LOCAL_DATE])
    assertEquals(5, table.rowCount(), 'number of rows')
    assertEquals(4, table.columnCount(), 'number of columns')
    table[4, 1] = 'Sven'
    assertEquals('Sven', table[4, 1])
    table[4, 'salary'] = 123.10
    assertEquals(123.10, table[4, 'salary'] as BigDecimal, 1e-9)
  }

  @Test
  void testPutAtNullSetsMissing() {
    def table = Gtable.create([name: ['Alice', 'Bob'], age: [25, 30]])
    table[1, 'age'] = null
    assertTrue(table.column('age').isMissing(1), 'age column should be missing at row 1')
    table[0, 'name'] = null
    def nameCol = table.column('name')
    assertTrue(nameCol.isMissing(0), 'name column should be missing at row 0')
  }

  @Test
  void testTutorialExample() {
    // Create a Matrix with sample data
    def matrix = Matrix.builder().data(
        name: ['Alice', 'Bob', 'Charlie', 'David', 'Eve'],
        age: [25, 30, 35, 40, 45],
        salary: [50000.0, 60000.0, 70000.0, 80000.0, 90000.0],
        department: ['HR', 'IT', 'Finance', 'IT', 'HR']
    ).types(String, Integer, BigDecimal, String)
        .build()

    // Convert Matrix to GTable
    def gTable = TableUtil.fromMatrix(matrix)

    // Calculate average salary by department
    def deptSalary = gTable.summarize('salary', BigDecimalAggregateFunctions.mean)
        .by('department')

    // Create a frequency table for the department column
    def deptFreq = TableUtil.frequency(gTable, 'department')

    // Normalize the salary column
    var salaryCol = gTable.column('salary') as BigDecimalColumn
    def normalizedSalary = Normalizer.minMaxNorm(salaryCol)

    // Replace the original column with the normalized one
    gTable.replaceColumn('salary', normalizedSalary)

    // Convert back to Matrix for further analysis
    def newMatrix = TableUtil.toMatrix(gTable)
  }

  @Test
  void testTableLevelNormalization() {
    def matrix = Matrix.builder().data(
        name: ['Alice', 'Bob', 'Charlie', 'David', 'Eve'],
        salary: [50000.0, 60000.0, 70000.0, 80000.0, 90000.0]
    ).types(String, BigDecimal).build()

    def gTable = TableUtil.fromMatrix(matrix)

    // Replace source column (non-destructive: returns new table)
    def normalized = gTable.normalizeMinMax('salary', null, 8)
    assertEquals(0.00000000, normalized.getAt(0, 'salary'))
    assertEquals(1.00000000, normalized.getAt(4, 'salary'))

    // Original gTable should be unchanged (non-destructive)
    assertEquals(50000, gTable.getAt(0, 'salary'))

    // Add as new column
    def withNewCol = gTable.normalizeMinMax('salary', 'salary_norm', 8)
    assertTrue(withNewCol.columnNames().contains('salary_norm'))
    assertTrue(withNewCol.columnNames().contains('salary'))
    assertEquals(0.00000000, withNewCol.getAt(0, 'salary_norm'))
  }

  @Test
  void testTableLevelNormalizationUnsupportedType() {
    def table = Gtable.create([name: ['Alice', 'Bob']])
    def ex = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException) { ->
      table.normalizeMinMax('name')
    }
    assertTrue(ex.message.contains('name'))
    assertTrue(ex.message.contains('STRING'))
  }

  @Test
  void testTableLevelMeanNorm() {
    def gTable = Gtable.create([value: [10.0, 20.0, 30.0, 40.0]])
    def normalized = gTable.normalizeMean('value', null, 7)
    assertEquals(-0.5000000, normalized.getAt(0, 'value') as BigDecimal, 1e-7)
  }

  @Test
  void testTableLevelStdScaleNorm() {
    def gTable = Gtable.create([value: [10.0, 20.0, 30.0, 40.0]])
    def normalized = gTable.normalizeStdScale('value', null, 7)
    assertEquals(-1.1618950, normalized.getAt(0, 'value') as BigDecimal, 1e-7)
  }

  @Test
  void testTableLevelLogNorm() {
    def gTable = Gtable.create([value: [1.0, 2.0, 3.0, 4.0]])
    def normalized = gTable.normalizeLog('value', null, 7)
    assertEquals(0.0000000, normalized.getAt(0, 'value') as BigDecimal, 1e-7)
  }

  @Test
  void testTableLevelNormalizationUnsupportedTypeForMean() {
    def table = Gtable.create([name: ['Alice', 'Bob']])
    def ex = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException) { -> table.normalizeMean('name') }
    assertTrue(ex.message.contains('name'))
    assertTrue(ex.message.contains('STRING'))
  }

  @Test
  void testAddColumnHelpers() {
    def table = Gtable.create()
        .addStringColumn('s', ['a', 'b'])
        .addIntColumn('i', [1, 2])
        .addDoubleColumn('d', [1.1, 2.2])
    assertEquals(2, table.rowCount())
    assertEquals(['s', 'i', 'd'], table.columnNames())
  }

  @Test
  void testPutAtNullOnSharedStringColumnDoesNotResizeSource() {
    def source = Table.create('src',
        StringColumn.create('name', ['Alice', 'Bob']),
        IntColumn.create('age', [25, 30] as int[]))
    Gtable table = Gtable.create(source)

    table[0, 'name'] = null

    assertTrue(table.column('name').isMissing(0), 'gtable name should be missing at row 0')
    assertEquals(2, table.rowCount(), 'gtable row count')
    assertEquals(2, source.column('name').size(), 'source name column must keep its size')
    assertEquals(2, source.rowCount(), 'source row count must be unchanged')
    assertEquals('Alice', source.column('name').get(0), 'source value must be untouched')
    // the null put detached the name column from the source; later puts on it are local to the Gtable
    table[1, 'name'] = 'Bobby'
    assertEquals('Bobby', table[1, 'name'])
    assertEquals('Bob', source.column('name').get(1), 'detached column must not write through')
    // other columns are still shared with the source
    table[1, 'age'] = 31
    assertEquals(31, source.column('age').get(1), 'shared column still writes through')
  }

  @Test
  void testAddIntegerColumnsTreatNullAsMissing() {
    Gtable table = Gtable.create('nulls')
    table.addIntColumn('i', [1, null, '3'])
    table.addLongColumn('l', [10L, null, '30'])
    table.addShortColumn('s', [1 as short, null, '3'])

    ['i', 'l', 's'].each { String name ->
      assertEquals(3, table.column(name).size(), "$name size")
      assertTrue(table.column(name).isMissing(1), "$name row 1 should be missing")
      assertFalse(table.column(name).isMissing(0), "$name row 0 should be present")
    }
    // numeric strings keep working, as they did with the old primitive-array coercion
    assertEquals(3, table.column('i').get(2))
    assertEquals(30L, table.column('l').get(2))
    assertEquals(3 as short, table.column('s').get(2))
    // overflow is rejected through ValueConverter's exact narrowing (matrix-core 3.9.0), never wrapped
    assertThrows(IllegalArgumentException) { Gtable.create('o').addShortColumn('s', [70000]) }
    assertThrows(IllegalArgumentException) { Gtable.create('o').addIntColumn('i', [3_000_000_000L]) }
    // garbage strings are rejected by the same strict pre-parse putAt uses (0.3.x also threw)
    assertThrows(NumberFormatException) { Gtable.create('g').addIntColumn('i', ['12abc']) }
    // an empty string element becomes missing, exactly like putAt('') (0.3.x threw NumberFormatException)
    Gtable blanks = Gtable.create('e')
    blanks.addIntColumn('i', [''])
    assertTrue(blanks.column('i').isMissing(0))
  }

  @Test
  void testPutAtBooleanStringsFollowValueConverter() {
    Gtable table = Gtable.create('flags', BooleanColumn.create('flag', [true, true, true, true] as Boolean[]))
    table[0, 'flag'] = 'false'
    table[1, 'flag'] = 'no'
    table[2, 'flag'] = 'yes'
    table[3, 'flag'] = false
    assertFalse(table[0, 'flag'] as boolean, "'false' must store false")
    assertFalse(table[1, 'flag'] as boolean, "'no' must store false")
    assertTrue(table[2, 'flag'] as boolean, "'yes' must store true")
    assertFalse(table[3, 'flag'] as boolean)
  }

  @Test
  void testPutAtParsesStringsForTypedColumns() {
    Gtable table = Gtable.create('typed',
        IntColumn.create('i', [1, 2] as int[]),
        DoubleColumn.create('d', [1.0d, 2.0d] as double[]),
        DateColumn.create('date', [LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 2)] as LocalDate[]))
    table[0, 'i'] = '7'
    table[0, 'd'] = '2.5'
    table[0, 'date'] = '2024-01-05'
    assertEquals(7, table[0, 'i'])
    assertEquals(2.5d, table[0, 'd'] as double, 1e-12)
    assertEquals(LocalDate.of(2024, 1, 5), table[0, 'date'])
  }

  @Test
  void testPutAtKeepsExistingNumericCoercions() {
    Gtable table = Gtable.create('nums',
        IntColumn.create('i', [1, 2] as int[]),
        DoubleColumn.create('d', [1.0d, 2.0d] as double[]),
        StringColumn.create('s', ['a', 'b']))
    table[0, 'd'] = 123.10          // BigDecimal literal into DOUBLE
    table[1, 'd'] = 5               // Integer into DOUBLE
    table[0, 'i'] = 5.0             // integral BigDecimal into INTEGER
    table[0, 's'] = 42              // Integer into STRING
    assertEquals(123.1d, table[0, 'd'] as double, 1e-12)
    assertEquals(5.0d, table[1, 'd'] as double, 1e-12)
    assertEquals(5, table[0, 'i'])
    assertEquals('42', table[0, 's'])
  }

  @Test
  void testPutAtEmptyStringBecomesMissing() {
    // Empty strings consistently mark cells missing, including StringColumn where set("") would
    // otherwise bypass ByteDictionaryMap's missing marker.
    Gtable table = Gtable.create('blanks',
        IntColumn.create('i', [1, 2] as int[]),
        BooleanColumn.create('flag', [true, true] as Boolean[]),
        DateColumn.create('date', [LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 2)] as LocalDate[]),
        StringColumn.create('string', ['present', 'also present']))
    table[0, 'i'] = ''
    table[0, 'flag'] = ''
    table[0, 'date'] = ''
    table[0, 'string'] = ''
    assertTrue(table.column('i').isMissing(0), "'' into INTEGER is missing")
    assertTrue(table.column('flag').isMissing(0), "'' into BOOLEAN is missing")
    assertTrue(table.column('date').isMissing(0), "'' into LOCAL_DATE is missing")
    assertTrue(table.column('string').isMissing(0), "'' into STRING is missing")
  }

  @Test
  void testPutAtRejectsGarbageNumericStrings() {
    // strictness guard: ValueConverter.asInteger/asShort/asBigDecimal scrape digits out of garbage
    // ('12abc' -> 12), so putAt pre-parses CharSequence into Number columns strictly (as Groovy's
    // asType did in 0.3.x) and uniform with asLong/asFloat/asDouble
    Gtable table = Gtable.create('strict',
        IntColumn.create('i', [1, 2] as int[]),
        ShortColumn.create('s', [1 as short, 2 as short] as short[]))
    assertThrows(NumberFormatException) { table[0, 'i'] = 'abc' }
    assertThrows(NumberFormatException) { table[0, 'i'] = '12abc' }
    assertThrows(NumberFormatException) { table[1, 'i'] = '1,234' }
    // a fractional numeric string truncates like the equivalent number does (documented)
    table[0, 'i'] = '5.7'
    assertEquals(5, table[0, 'i'])
    table[1, 'i'] = 5.7
    assertEquals(5, table[1, 'i'])
    // out-of-range values are rejected by matrix-core's exact narrowing (3.9.0), never wrapped;
    // assertThrowsExactly is required (not assertThrows): NumberFormatException also extends
    // IllegalArgumentException, so a garbage-parse failure would otherwise satisfy the assertion
    assertThrowsExactly(IllegalArgumentException) { table[0, 'i'] = '3000000000' }
    assertThrowsExactly(IllegalArgumentException) { table[0, 'i'] = 3_000_000_000L }
    assertThrowsExactly(IllegalArgumentException) { table[0, 's'] = '70000' }
    assertThrowsExactly(IllegalArgumentException) { table[0, 's'] = 70000 }
    assertEquals(5, table[0, 'i'], 'a rejected put must leave the cell unchanged')
  }

  @Test
  void testPutAtNanAndInfinitySemantics() {
    // two deliberate 0.4.0 behaviour changes (documented breaking changes):
    // - 'NaN'/'Infinity' into DOUBLE/FLOAT: 0.3.x parsed via Double.valueOf and stored NaN (= missing);
    //   the strictness guard now rejects them with NumberFormatException from new BigDecimal(...)
    // - NaN into an integral column: 0.3.x 'NaN' as Integer stored 0; matrix-core 3.9.0 maps NaN to
    //   no-value, so the cell becomes missing
    Gtable table = Gtable.create('nan',
        DoubleColumn.create('d', [1.0d, 2.0d] as double[]),
        IntColumn.create('i', [1, 2] as int[]))
    assertThrows(NumberFormatException) { table[0, 'd'] = 'NaN' }
    assertThrows(NumberFormatException) { table[0, 'd'] = 'Infinity' }
    table[1, 'i'] = Double.NaN
    assertTrue(table.column('i').isMissing(1), 'NaN into INTEGER must be missing (0.3.x stored 0)')
    assertFalse(table.column('i').isMissing(0), 'other rows untouched')
  }

  @Test
  void testAsJavaClassMatchesTableUtil() {
    Gtable table = Gtable.create('types',
        StringColumn.create('s', ['a']),
        DoubleColumn.create('d', [1.0d] as double[]),
        BigDecimalColumn.create('b', [1.0] as BigDecimal[]),
        BooleanColumn.create('bool', [true] as Boolean[]))
    (0..<table.columnCount()).each { int i ->
      assertEquals(TableUtil.classForColumnType(table.column(i).type()), table.asJavaClass(i))
    }
  }

}
