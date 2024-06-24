package it

import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrix.sql.MatrixSql

import static org.junit.jupiter.api.Assertions.assertEquals

class DbTest {

  protected ConnectionInfo ci
  DataBaseProvider db

  DbTest(DataBaseProvider db, String dbName, String mode, String... additionalSettings) {
    ci = new ConnectionInfo()
    ci.setDependency('com.h2database:h2:2.2.224')
    def tmpDb = new File(System.getProperty('java.io.tmpdir'), "$dbName").getAbsolutePath()
    String settings = "MODE=$mode"
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
        def actual = m2[r, c].asType(dataset.columnType(c))
        if (expected != null && actual != null && expected instanceof BigDecimal) {
          actual = (actual as BigDecimal).setScale(expected.scale())
        }
        assertEquals(expected, actual, "Diff detected in $tableName on row $r, column ${dataset.columnName(c)}")
      }
    }
  }
}
