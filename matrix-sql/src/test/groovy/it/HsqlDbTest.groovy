package it

import org.junit.jupiter.api.Test
import se.alipsa.groovy.datasets.Dataset
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.matrix.Matrix


class HsqlDbTest extends AbstractDbTest {

  HsqlDbTest() {
    super(DataBaseProvider.HSQLDB, 'hsqlTest', 'HSQLDB', 'DEFAULT_NULL_ORDERING=FIRST')
  }
}
