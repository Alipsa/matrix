[![Maven Central](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-sql/badge.svg)](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-sql)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-sql/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-sql)

# Matrix SQL

The Matrix SQL module makes communication between the Matrix library and a relational
database as simple as possible.

To use it, add the following to your Gradle build script:

```groovy
implementation 'org.apache.groovy:groovy:5.0.5'
implementation 'se.alipsa.matrix:matrix-core:3.7.1'
implementation 'se.alipsa.matrix:matrix-sql:2.4.0'
```

or if you use Maven:

```xml
<dependencies>
  <dependency>
    <groupId>org.apache.groovy</groupId>
    <artifactId>groovy</artifactId>
    <version>5.0.5</version>
  </dependency>
  <dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-core</artifactId>
    <version>3.7.1</version>
  </dependency>
  <dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-sql</artifactId>
    <version>2.4.0</version>
  </dependency>
</dependencies>
```

`MatrixSql` is the main high-level API. It can be created from a `ConnectionInfo`
from the [data-utils library](https://github.com/Alipsa/data-utils), or with
`MatrixSqlFactory`, which can infer the JDBC driver and dependency from the JDBC URL.
The driver is downloaded if needed and added to the classpath before connecting.

```groovy
import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.matrix.sql.MatrixSql

ConnectionInfo ci = new ConnectionInfo()
ci.setDependency('org.postgresql:postgresql:42.7.8')
ci.setUrl('jdbc:postgresql://somedb.somedomain.com/mydatabase')
ci.setUser('myuser')
ci.setPassword('123password')
ci.setDriver('org.postgresql.Driver')

try (MatrixSql matrixSql = new MatrixSql(ci)) {
  assert matrixSql.getTableNames() != null
}
```

The dependency string uses Gradle short form. For example, the PostgreSQL driver
coordinate can be found on [mvnrepository](https://mvnrepository.com/artifact/org.postgresql/postgresql)
under the `Gradle (short)` tab.

## Creating a MatrixSql

For common databases, use the factory:

```groovy
import se.alipsa.matrix.sql.MatrixSql
import se.alipsa.matrix.sql.MatrixSqlFactory

String url = 'jdbc:h2:mem:matrixSqlExample;DB_CLOSE_DELAY=-1'

try (MatrixSql matrixSql = MatrixSqlFactory.create(url, 'sa', '123')) {
  assert matrixSql.connectionInfo.driver == 'org.h2.Driver'
  assert matrixSql.connectionInfo.dependency.startsWith('com.h2database:h2:')
}
```

If you already manage the `Connection`, pass it directly. `close()` does not close
externally supplied connections.

```groovy
import se.alipsa.groovy.datautil.DataBaseProvider
import se.alipsa.matrix.sql.MatrixSql
import se.alipsa.matrix.sql.MatrixSqlFactory

String url = 'jdbc:h2:mem:managedConnectionExample;DB_CLOSE_DELAY=-1'

try (MatrixSql owner = MatrixSqlFactory.createH2(url, 'sa', '123')) {
  def connection = owner.connect()

  try (MatrixSql matrixSql = new MatrixSql(connection, DataBaseProvider.H2)) {
    assert !connection.isClosed()
  }

  assert !connection.isClosed()
}
```

If you prefer to manage every connection yourself, use `MatrixDbUtil`. Its API is
similar to `MatrixSql`, but every method takes a `java.sql.Connection`.

## Workflows

The examples below use H2 for brevity, but the same APIs work for other supported
databases.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.sql.MatrixSql
import se.alipsa.matrix.sql.MatrixSqlFactory
import se.alipsa.matrix.sql.SqlIdentifier

String url = 'jdbc:h2:mem:workflowExample;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE'

Matrix people = Matrix.builder('people').data([
    id: [1, 2, 3],
    name: ['Alice', 'Bob', 'Charlie']
]).types(int, String).build()

try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
  String tableName = matrixSql.tableName(people)
  String quotedTable = SqlIdentifier.renderTable(tableName)

  if (matrixSql.tableExists(tableName)) {
    matrixSql.dropTable(tableName)
  }

  Map createResult = matrixSql.create(people, 'id')
  assert createResult.inserted == 3

  Matrix selected = matrixSql.select("select * from $quotedTable order by id")
  assert selected.rowCount() == 3
  assert selected[0, 'name'] == 'Alice'

  Matrix extraRows = Matrix.builder('incoming_people').data([
      id: [4],
      name: ['Diana']
  ]).types(int, String).build()
  assert matrixSql.insert(tableName, extraRows) == 1

  Row row = Matrix.builder('people').data([
      id: [2],
      name: ['Robert']
  ]).types(int, String).build().row(0)
  assert matrixSql.update(tableName, row, 'id') == 1

  Matrix updated = matrixSql.select("select * from $quotedTable where id = 2")
  assert updated[0, 'name'] == 'Robert'

  assert matrixSql.delete("delete from $quotedTable where id = 3") == 1

  Matrix remaining = matrixSql.select("select * from $quotedTable")
  assert remaining.rowCount() == 3
}
```

### Prepared Parameters

Use the prepared-parameter overloads when values come from users or other external
input.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.sql.MatrixSql
import se.alipsa.matrix.sql.MatrixSqlFactory
import se.alipsa.matrix.sql.SqlIdentifier

String url = 'jdbc:h2:mem:preparedExample;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE'

Matrix people = Matrix.builder('people').data([
    id: [1, 2, 3],
    name: ['Alice', 'Bob', 'Charlie']
]).types(int, String).build()

try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
  matrixSql.create(people, 'id')
  String tableName = SqlIdentifier.renderTable(matrixSql.tableName(people))

  Matrix selected = matrixSql.select(
      "select * from $tableName where id > ? order by id",
      [1],
      'selected_people'
  )
  assert selected.matrixName == 'selected_people'
  assert selected.column('name') == ['Bob', 'Charlie']

  int updated = matrixSql.update(
      "update $tableName set name = ? where id = ?",
      ['Bobby', 2]
  )
  assert updated == 1

  int deleted = matrixSql.delete("delete from $tableName where id = ?", [3])
  assert deleted == 1

  Map<Integer, Object> result = matrixSql.execute(
      "select * from $tableName where name = ?",
      ['Bobby']
  )
  assert result[0] instanceof Matrix
  assert ((Matrix) result[0]).rowCount() == 1
}
```

`execute(String, List)` returns a map where each key is the result index. Values are
`Matrix` instances for result sets and `Integer` update counts for update statements.

## LocalDate

When a Matrix contains `LocalDate` values, the database column type is `DATE`.
When reading the data back, Matrix SQL maps `DATE` to `java.sql.Date` because JDBC
does not distinguish between `LocalDate`, `java.util.Date`, and `java.sql.Date` in
the column metadata. Convert the column when you want `LocalDate` values again.

```groovy
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.sql.MatrixSql
import se.alipsa.matrix.sql.MatrixSqlFactory

import java.time.LocalDate

Matrix complexData = Matrix.builder('complexData').data([
    place: [1, 20, 3],
    firstname: ['Lorena', 'Marianne', 'Lotte'],
    start: ListConverter.toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
]).types(int, String, LocalDate).build()

String url = 'jdbc:h2:mem:localDateExample;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE'

try (MatrixSql matrixSql = MatrixSqlFactory.createH2(url, 'sa', '123')) {
  matrixSql.create(complexData)

  Matrix stored = matrixSql.select('select * from "complexData"')
  assert stored.type('start') == java.sql.Date
  assert stored.column('start') == ListConverter.toSqlDates('2021-12-01', '2022-07-10', '2023-05-27')

  Matrix converted = stored.convert('start', LocalDate)
  assert converted.type('start') == LocalDate
  assert converted.column('start') == ListConverter.toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
}
```

# Release Version Compatibility Matrix

The following table illustrates the version compatibility of matrix-sql and matrix core.

| Matrix sql |    Matrix core |
|-----------:|---------------:|
|      1.0.0 |          1.2.4 |
|      1.0.1 | 2.0.0 -> 2.1.1 |
|      1.1.0 |          2.2.0 |
|      2.0.0 |          3.0.0 |
|      2.1.0 |          3.1.0 |
|      2.1.1 | 3.2.0 -> 3.3.0 |
|      2.2.0 | 3.4.0 -> 3.5.0 |
|      2.3.0 |          3.6.0 |
|      2.3.1 |          3.7.1 |
