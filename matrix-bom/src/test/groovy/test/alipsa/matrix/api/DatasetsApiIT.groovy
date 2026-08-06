package test.alipsa.matrix.api

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.alipsa.matrix.datasets.Dataset

import static org.junit.jupiter.api.Assertions.assertTrue

/** Verifies that each bundled dataset remains loadable with its documented shape. */
@Tag('datasets')
class DatasetsApiIT implements ApiItSupport {

  @Test
  void bundledDatasetsLoad() {
    [Dataset.airquality(), Dataset.cars(), Dataset.iris(), Dataset.mtcars(), Dataset.plantGrowth(),
     Dataset.toothGrowth(), Dataset.usArrests(), Dataset.mpg(), Dataset.diamonds()].each {
      assertTrue(it.rowCount() > 0 && it.columnCount() > 0, "dataset ${it.matrixName} should contain data")
    }
  }
}
