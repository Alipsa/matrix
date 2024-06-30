package it

import se.alipsa.groovy.datautil.DataBaseProvider

class SqlServerTest extends AbstractDbTest {

  SqlServerTest() {
    super(DataBaseProvider.MSSQL, 'mssqlTest', 'MSSQLServer', 'DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE')
  }
}
