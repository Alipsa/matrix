package it

import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.matrix.ListConverter
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrix.ValueConverter
import se.alipsa.groovy.matrix.sql.MatrixSql

import java.sql.Time
import java.time.LocalDate
import java.time.LocalDateTime

import static org.junit.jupiter.api.Assertions.assertEquals

abstract class AbstractDbTest {

  protected ConnectionInfo ci
  DataBaseProvider db

  AbstractDbTest(DataBaseProvider db, String dbName, String mode, String... additionalSettings) {
    ci = new ConnectionInfo()
    ci.setDependency('com.h2database:h2:2.2.224')
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
  }

  void verifyDbCreation(Matrix dataset, int... scanNumRows) {
    MatrixSql matrixSql = new MatrixSql(ci, db)
    String tableName = dataset.name
    if (matrixSql.tableExists(tableName)) {
      matrixSql.dropTable(tableName)
    }
    if (scanNumRows.length > 0) {
      matrixSql.create(dataset, scanNumRows[0])
    } else {
      matrixSql.create(dataset)
    }

    Matrix m2 = matrixSql.select("select * from $tableName")
    for (int r = 0; r < dataset.rowCount(); r++) {
      for (int c = 0; c < dataset.columnCount(); c++) {
        def expected = dataset[r, c].asType(dataset.columnType(c))
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
