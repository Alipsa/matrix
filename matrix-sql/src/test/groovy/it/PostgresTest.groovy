package it

import org.junit.jupiter.api.Test
import se.alipsa.groovy.datasets.Dataset
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.groovy.matrix.Matrix


class PostgresTest extends AbstractDbTest {

  PostgresTest() {
    super(DataBaseProvider.POSTGRESQL, 'postgresTest', 'PostgreSQL', 'DATABASE_TO_LOWER=TRUE', 'DEFAULT_NULL_ORDERING=HIGH')
  }
}
