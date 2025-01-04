package it

import se.alipsa.groovy.datautil.DataBaseProvider


class MariaDbTest extends AbstractDbTest {

  MariaDbTest() {
    super(DataBaseProvider.MARIADB, 'mariadbTest', 'MariaDB', 'DATABASE_TO_LOWER=TRUE')
  }
}
