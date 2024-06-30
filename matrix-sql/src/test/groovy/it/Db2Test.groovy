package it

import org.junit.jupiter.api.Test
import se.alipsa.groovy.datasets.Dataset
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.matrix.Matrix

import java.sql.Connection


class Db2Test extends AbstractDbTest {

  Db2Test() {
    super(DataBaseProvider.DB2, 'db2Test', 'DB2', 'DEFAULT_NULL_ORDERING=HIGH')
  }

}
