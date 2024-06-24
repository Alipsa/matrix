package it

import org.junit.jupiter.api.Test
import se.alipsa.groovy.datasets.Dataset
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.matrix.Matrix


class HsqlDbTest extends DbTest {

  HsqlDbTest() {
    super(DataBaseProvider.HSQLDB, 'hsqlTest', 'HSQLDB', 'DEFAULT_NULL_ORDERING=FIRST')
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
    verifyDbCreation(Dataset.plantGrowth())
  }
}
