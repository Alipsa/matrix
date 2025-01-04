package it

import se.alipsa.groovy.datautil.DataBaseProvider


class MySqlTest extends AbstractDbTest {

  MySqlTest() {
    super(DataBaseProvider.MYSQL, 'mysqlTest', 'MySQL', 'DATABASE_TO_LOWER=TRUE')
  }
}
