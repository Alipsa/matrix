package it

import org.junit.jupiter.api.Test
import se.alipsa.groovy.datasets.Dataset
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.matrix.Matrix


class MySqlTest extends AbstractDbTest {

  MySqlTest() {
    super(DataBaseProvider.MYSQL, 'mysqlTest', 'MySQL', 'DATABASE_TO_LOWER=TRUE')
  }
}
