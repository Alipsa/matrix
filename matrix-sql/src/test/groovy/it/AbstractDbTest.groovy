package it

import org.junit.jupiter.api.Test
import se.alipsa.groovy.datasets.Dataset
import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.datautil.sqltypes.SqlTypeMapper
import se.alipsa.groovy.matrix.ListConverter
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrix.Row
import se.alipsa.groovy.matrix.Stat
import se.alipsa.groovy.matrix.ValueConverter
import se.alipsa.groovy.matrix.sql.MatrixDbUtil
import se.alipsa.groovy.matrix.sql.MatrixSql

import java.sql.Connection
import java.sql.Time
import java.time.LocalDate
import java.time.LocalDateTime

import static org.junit.jupiter.api.Assertions.assertEquals

abstract class AbstractDbTest {

  protected ConnectionInfo ci
  DataBaseProvider db
  MatrixSql matrixSql
  MatrixDbUtil matrixDbUtil

  Matrix dur = Matrix.create('duration', ['name': String, 'measurePoint': String, 'millis': Long])

  AbstractDbTest(DataBaseProvider db, String dbName, String mode, String... additionalSettings) {
    ci = new ConnectionInfo()
    ci.setDependency('com.h2database:h2:2.3.232')
    def tmpDb = new File(System.getProperty('java.io.tmpdir'), "$dbName").getAbsolutePath()
    String settings = ''
    if (mode != null && db != DataBaseProvider.H2) {
      settings += "MODE=$mode"
    }
    additionalSettings.each {
      settings += ";$it"
    }
    ci.setUrl("jdbc:h2:file:${tmpDb};$settings")
    ci.setUser('sa')
    ci.setPassword('123')
    ci.setDriver("org.h2.Driver")
    this.db = db
    matrixSql = new MatrixSql(ci, db)
    matrixDbUtil = new MatrixDbUtil(db)
  }

  @Test
  void createAndVerify() {
    //println Stat.str(dur)
    try (Connection con = matrixSql.connect()) {
      verifyDbCreation(con, getComplexData())
      verifyDbCreation(con, Dataset.airquality())
      verifyDbCreation(con, Dataset.mtcars())
      Matrix diamonds = Dataset.diamonds()
      verifyDbCreation(con, diamonds, diamonds.rowCount())
      verifyDbCreation(con, Dataset.plantGrowth())
    }
    //println dur.content()
  }

  void verifyDbCreation(Connection con, Matrix dataset, int... scanNumRows) {
    long start = System.currentTimeMillis()
    String tableName = matrixDbUtil.tableName(dataset)
    long ctm1 = System.currentTimeMillis()
    dur + [dataset.name, "1. table name", ctm1 - start]
    if (matrixDbUtil.tableExists(con, tableName)) {
      matrixDbUtil.dropTable(con, tableName)
    }
    long ctm2 = System.currentTimeMillis()
    dur + [dataset.name, "2. check and drop table", ctm2 - ctm1]
    if (scanNumRows.length > 0) {
      matrixDbUtil.create(con, dataset, scanNumRows[0])
    } else {
      matrixDbUtil.create(con, dataset)
    }
    long ctm3 = System.currentTimeMillis()
    dur + [dataset.name, "3. create table", ctm3 - ctm2]

    Matrix m2 = matrixDbUtil.select(con,"select * from $tableName")
    long ctm4 = System.currentTimeMillis()
    dur + [dataset.name, "4. select *", ctm4 - ctm3]
    dataset.eachWithIndex { Row row, int r ->
      row.eachWithIndex { Object expected, int c ->
        def actual = ValueConverter.convert(m2[r, c], dataset.columnType(c))
        if (expected != null && actual != null) {
          if (expected instanceof BigDecimal) {
            actual = (actual as BigDecimal).setScale(expected.scale())
          } else if (expected instanceof byte[]) {
            // convert to string representation so we dont have to do a special assertArrayEquals
            expected = "$expected"
            actual = "$actual"
          }
        }
        assertEquals(expected, actual, "Diff detected in $tableName on row $r, column ${dataset.columnName(c)}")
      }
    }
    long ctm5 = System.currentTimeMillis()
    dur + [dataset.name, "5. compare values", ctm5 - ctm4]
    dur + [dataset.name, "6. total round", System.currentTimeMillis() - start]
  }

  Matrix getComplexData() {
    new Matrix([
        'place': [1, 20, 3],
        'firstname': ['Lorena', 'Marianne', 'Lotte'],
        'start': ListConverter.toLocalDates('2021-12-01', '2022-07-10', '2023-05-27'),
        'bin': [[1,2,3] as byte[], [2,3,4] as byte[], [3,4,5] as byte[]],
        'theTime': [Time.valueOf('01:11:41'), Time.valueOf('02:22:42'), Time.valueOf('03:33:43')],
        'local date time': ListConverter.toLocalDateTimes('2021-12-01T01:11:41', '2022-07-10T02:22:42', '2023-05-27T03:33:43')
    ],
        [int, String, LocalDate, byte[], Time, LocalDateTime]
    ).withName('complexData')
  }
}
