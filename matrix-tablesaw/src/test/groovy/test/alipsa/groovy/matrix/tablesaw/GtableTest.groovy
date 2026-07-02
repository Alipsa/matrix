package test.alipsa.groovy.matrix.tablesaw

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.core.ListConverter.toLocalDates
import static tech.tablesaw.api.ColumnType.*

import org.junit.jupiter.api.Test
import tech.tablesaw.api.BigDecimalAggregateFunctions
import tech.tablesaw.api.BigDecimalColumn
import tech.tablesaw.api.ColumnType
import tech.tablesaw.column.numbers.BigDecimalColumnType
import tech.tablesaw.io.csv.CsvReadOptions
import tech.tablesaw.joining.JoinType

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.tablesaw.Normalizer
import se.alipsa.matrix.tablesaw.TableUtil
import se.alipsa.matrix.tablesaw.gtable.GdataFrameJoiner
import se.alipsa.matrix.tablesaw.gtable.Gtable

class GtableTest {

  @Test
  void testProgrammaticCreation() {
    def empData = [
        emp_id: 1..5,
        emp_name: ['Rick', 'Dan', 'Michelle', 'Ryan', 'Gary'],
        salary: [623.3, 515.2, 611.0, 729.0, 843.25],
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
        salary: [50000, 60000]
    ]
    Gtable table = Gtable.create(data, [salary: BigDecimalColumnType.instance()])
    assertEquals(STRING, table.column('name').type())
    assertEquals(INTEGER, table.column('age').type())
    assertEquals(BigDecimalColumnType.instance(), table.column('salary').type())
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
        salary: [623.3, 515.2, 611.0, 729.0, 843.25],
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
        salary: [623.3, 515.2, 611.0, 729.0, 843.25],
        start_date: toLocalDates('2012-01-01', '2013-09-23', '2014-11-15', '2014-05-11', '2015-03-27')
    ]
    Gtable table = Gtable.create(empData, [INTEGER, STRING, DOUBLE, LOCAL_DATE])

    Gtable table2 = Gtable.create([
        employee_id: [1, 2, 3, 4, null],
        performance: [0.76, 0.79, 0.68, 1.10, 0.91]
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
        salary: [623.3, 515.2, 611.0, 729.0, 843.25],
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
        salary: [50000, 60000, 70000, 80000, 90000],
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
        salary: [50000, 60000, 70000, 80000, 90000]
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

}
