package it

import se.alipsa.groovy.datautil.DataBaseProvider


class OracleTest extends AbstractDbTest {

  OracleTest() {
    super(DataBaseProvider.ORACLE, 'oracleTest', 'Oracle', 'DEFAULT_NULL_ORDERING=HIGH')
  }
}
