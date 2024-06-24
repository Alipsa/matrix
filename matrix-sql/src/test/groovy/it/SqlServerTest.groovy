package it

import org.junit.jupiter.api.Test
import se.alipsa.groovy.datasets.Dataset
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.matrix.Matrix


class SqlServerTest extends DbTest {

  SqlServerTest() {
    super(DataBaseProvider.MSSQL, 'mssqlTest', 'MSSQLServer', 'DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE')
  }

  @Test
  void testAirQuality() {
    verifyDbCreation(Dataset.airquality())
  }

  @Test
  void testMtCars() {
    verifyDbCreation(Dataset.mtcars())
  }

  @Test
  void testDiamonds() {
    Matrix diamonds = Dataset.diamonds()
    verifyDbCreation(diamonds, diamonds.rowCount())
  }

  @Test
  void testPlantGrowth() {
    Matrix ds = Dataset.plantGrowth()
    verifyDbCreation(ds)
  }
}
