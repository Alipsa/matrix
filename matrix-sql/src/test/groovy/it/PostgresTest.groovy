package it

import se.alipsa.groovy.datautil.DataBaseProvider


class PostgresTest extends AbstractDbTest {

  PostgresTest() {
    super(DataBaseProvider.POSTGRESQL, 'postgresTest', 'PostgreSQL', 'DATABASE_TO_LOWER=TRUE', 'DEFAULT_NULL_ORDERING=HIGH')
  }
}
