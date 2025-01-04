package it

import se.alipsa.groovy.datautil.DataBaseProvider


class HsqlDbTest extends AbstractDbTest {

  HsqlDbTest() {
    super(DataBaseProvider.HSQLDB, 'hsqlTest', 'HSQLDB', 'DEFAULT_NULL_ORDERING=FIRST')
  }
}
