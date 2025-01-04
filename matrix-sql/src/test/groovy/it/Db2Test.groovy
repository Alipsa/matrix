package it

import se.alipsa.groovy.datautil.DataBaseProvider


class Db2Test extends AbstractDbTest {

  Db2Test() {
    super(DataBaseProvider.DB2, 'db2Test', 'DB2', 'DEFAULT_NULL_ORDERING=HIGH')
  }

}
