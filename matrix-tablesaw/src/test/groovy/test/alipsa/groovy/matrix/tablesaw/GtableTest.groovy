package test.alipsa.groovy.matrix.tablesaw

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.tablesaw.TableUtil
import se.alipsa.matrix.tablesaw.Normalizer
import se.alipsa.matrix.tablesaw.gtable.Gtable
import tech.tablesaw.api.BigDecimalAggregateFunctions
import tech.tablesaw.api.BigDecimalColumn
import tech.tablesaw.api.ColumnType
import tech.tablesaw.column.numbers.BigDecimalColumnType
import tech.tablesaw.io.csv.CsvReadOptions

import static org.junit.jupiter.api.Assertions.assertEquals
import static se.alipsa.matrix.core.ListConverter.toLocalDates
import static tech.tablesaw.api.ColumnType.*

class GtableTest {

  @Test
  void testProgrammaticCreation() {
    def empData = [
        emp_id: 1..5,
        emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
        salary: [623.3,515.2,611.0,729.0,843.25],
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
        ]
    Gtable table = Gtable.create(empData, [INTEGER, STRING, DOUBLE, LOCAL_DATE])
    assertEquals(5, table.rowCount(), "number of rows")
    assertEquals(4, table.columnCount(), "number of columns")
    assertEquals("Gary", table[4, 1])
    assertEquals("Gary", table[4, "emp_name"])
  }

  @Test
  void testCreateFromCsv() {
    def csv = getClass().getResource("/glaciers.csv")
    CsvReadOptions.Builder builder = CsvReadOptions.builder(csv)
        .separator(',' as Character)
        .columnTypes([INTEGER, BigDecimalColumnType.instance(), INTEGER] as ColumnType[])

    Gtable glaciers = Gtable.read().usingOptions(builder.build())

    assertEquals(1946, glaciers[1,0])
    assertEquals(-3.19, glaciers[2,1])
    assertEquals(1, glaciers[3,2])
  }

  @Test
  void testExportToCsv() {
    def empData = [
        emp_id: 1..5,
        emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
        salary: [623.3,515.2,611.0,729.0,843.25],
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
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
        emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
        salary: [623.3,515.2,611.0,729.0,843.25],
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
    ]
    Gtable table = Gtable.create(empData, [INTEGER, STRING, DOUBLE, LOCAL_DATE])

    Gtable table2 = Gtable.create([
        employee_id: [1,2,3,4,null],
        performance: [0.76, 0.79, 0.68, 1.10, 0.91]
    ], [INTEGER, DOUBLE])

    // does not mutate the table
    Gtable joined = table.joinOn("emp_id").inner(table2, 'employee_id')
    //println joined
    assertEquals(4, joined.rowCount(), 'joined rowcount, inner join should have removed a row')
    assertEquals(5, joined.columnCount(), 'joined columnCount')

    // mutates the table!
    table.concat(table2)
    //println(table)
    assertEquals(5, table.rowCount(), 'concat rowcount')
    assertEquals(6, table.columnCount(), 'concat columnCount')
  }

  @Test
  void testShortHandPut() {
    def empData = [
        emp_id: 1..5,
        emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
        salary: [623.3,515.2,611.0,729.0,843.25],
        start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
    ]
    Gtable table = Gtable.create(empData, [INTEGER, STRING, DOUBLE, LOCAL_DATE])
    assertEquals(5, table.rowCount(), "number of rows")
    assertEquals(4, table.columnCount(), "number of columns")
    table[4, 1] = "Sven"
    assertEquals("Sven", table[4, 1])
    table[4, "salary"] = 123.10
    assertEquals(123.10, table[4, "salary"])
  }

  @Test
  void testTutorialExample() {
    // Create a Matrix with sample data
    def matrix = Matrix.builder().data(
        name: ["Alice", "Bob", "Charlie", "David", "Eve"],
        age: [25, 30, 35, 40, 45],
        salary: [50000, 60000, 70000, 80000, 90000],
        department: ["HR", "IT", "Finance", "IT", "HR"]
    ).types(String, Integer, BigDecimal, String)
        .build()

    // Convert Matrix to GTable
    def gTable = TableUtil.fromMatrix(matrix)

    // Calculate average salary by department
    def deptSalary = gTable.summarize("salary", BigDecimalAggregateFunctions.mean)
        .by("department")

    println "Average salary by department:"
    println deptSalary

    // Create a frequency table for the department column
    def deptFreq = TableUtil.frequency(gTable, "department")

    println "\nDepartment frequency:"
    println deptFreq

    // Normalize the salary column
    var salaryCol = gTable.column("salary") as BigDecimalColumn
    def normalizedSalary = Normalizer.minMaxNorm(salaryCol)

    // Replace the original column with the normalized one
    gTable.replaceColumn("salary", normalizedSalary)

    println "\nData with normalized salaries:"
    println gTable

    // Convert back to Matrix for further analysis
    def newMatrix = TableUtil.toMatrix(gTable)

    println "\nConverted back to Matrix:"
    println newMatrix.content()
  }
}
