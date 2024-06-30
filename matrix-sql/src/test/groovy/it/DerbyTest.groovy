package it

import se.alipsa.groovy.datautil.DataBaseProvider


class DerbyTest extends AbstractDbTest {

  DerbyTest() {
    super(DataBaseProvider.DERBY, 'derbyTest', 'Derby', 'DEFAULT_NULL_ORDERING=HIGH')
  }
}
