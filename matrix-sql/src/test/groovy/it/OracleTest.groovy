package it

import org.junit.jupiter.api.Test
import se.alipsa.groovy.datasets.Dataset
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.matrix.Matrix


class OracleTest extends AbstractDbTest {

  OracleTest() {
    super(DataBaseProvider.ORACLE, 'oracleTest', 'Oracle', 'DEFAULT_NULL_ORDERING=HIGH')
  }
}
