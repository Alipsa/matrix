package it

import org.junit.jupiter.api.Test
import se.alipsa.groovy.datasets.Dataset
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.matrix.Matrix


class MariaDbTest extends AbstractDbTest {

  MariaDbTest() {
    super(DataBaseProvider.MARIADB, 'mariadbTest', 'MariaDB', 'DATABASE_TO_LOWER=TRUE')
  }
}
