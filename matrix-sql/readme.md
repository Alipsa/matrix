[![Maven Central](https://maven-badges.herokuapp.com/maven-central/se.alipsa.matrix/matrix-sql/badge.svg)](https://maven-badges.herokuapp.com/maven-central/se.alipsa.matrix/matrix-sql)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-sql/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-sql)
# Matrix SQL
The Matrix SQL module aims to make communication between the Matrix library and a 
relational database as easy as possible.

To use it, add the following to your gradle build script:
```groovy
implementation 'org.apache.groovy:groovy:5.0.2'
implementation 'se.alipsa.matrix:matrix-core:3.3.0'
implementation 'se.alipsa.matrix:matrix-sql:2.1.1'
```
or if you use maven:
```xml
<dependencies>
  <dependency>
      <groupId>org.apache.groovy</groupId>
      <artifactId>groovy</artifactId>
      <version>5.0.2</version>
  </dependency>
  <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-core</artifactId>
      <version>3.3.0</version>
  </dependency>
  <dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-sql</artifactId>
    <version>2.1.1</version>
  </dependency>
</dependencies>
```

The core class is MatrixSql. It is created using a ConnectionInfo from the data-utils library.
A connection info contains the jdbc url, credentials and info of the jdbc driver to use.
The driver will be downloaded, if needed, and added to the classpath enabling dynamic 
instantiation of the driver when connecting to the db.

If you want to manage db connections yourself, you can directly use the MatrixDbUtil class instead.
The api is more or less identical to MatrixSql with the difference that all methods 
requires a java.sql.Connection. You instantiate a MatrixDbUtil with a SqlTypeMapper or a DataBaseProvider enum

Creating a MatrixSql can be done as follows:
```groovy 
ConnectionInfo ci = new ConnectionInfo()
ci.setDependency('org.postgresql:postgresql:42.7.3')
ci.setUrl("jdbc:postgresql://somedb.somedomain.com/mydatabase")
ci.setUser('myuser')
ci.setPassword('123password')
ci.setDriver("org.postgresql.Driver")
MatrixSql matrixSql = new MatrixSql(ci)
```
Note the that the syntax for specifying the Driver dependency follows the
Gradle short form. e.g to get the short form for a PostgreSQL driver you can
look it upp on [mvnrepository](https://mvnrepository.com/artifact/org.postgresql/postgresql/42.7.3)
and find the string in the `Gradle (short)` tab i.e: 'org.postgresql:postgresql:42.7.3'

Using the MatrixSql object you can then interact with the database.
The following operations are supported:

### Create

The simplest way to create a table corresponding to a Matrix and populate it is to do:
`matrixSql.create(myMatrix)`
if you want to define a primary key, you just append the column name(s) to the create method e.g:
`matrixSql.create(myMatrix, 'id')`

The MatrixSql is Closeable and should be used in a try with resources block.
If that is not an option, you must close the connection to the database when you are
finished e.g: `matrixSql.close()`

### Drop
User either the Matrix or the table to drop a table i.e.
either `matrixSql.dropTable(myMatrix)`
or `matrixSql.dropTable('theTableName')`
Typically, you want to check that the table exists before attempting to drop it:
```groovy
if (matrixSql.tableExists(myMatrix)) {
   matrixSql.dropTable(myMatrix)
}
```

### Insert
### Select
### Update
### Delete

## Caveats

### LocalDate
When dealing with a Matrix containing LocalDate's there is an inevitable conversion between
LocalDate and java.sql.Date you need to watch out for. If we have a Matrix that we want to 
store in the database, the datatype for LocalDate is DATE in the DB. Since the datat type for 
a java.util.date and a java.sql.date is also DATE when we retreive it from the database, the
default mapping of the DATE type is java.sql.Date so this is what we will get.
E.g:

1. We have the following Matrix
    ```groovy
    import se.alipsa.matrix.core.Matrix
    import se.alipsa.matrix.core.ListConverter
    import java.time.LocalDate

    Matrix complexData = Matrix.builder('complexData').data([
        'place': [1, 20, 3],
        'firstname': ['Lorena', 'Marianne', 'Lotte'],
        'start': ListConverter.toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
    ]).types([int, String, LocalDate]).build()
    ```
2. We save it to the db
   ```groovy 
   import se.alipsa.groovy.datautil.ConnectionInfo
   import se.alipsa.matrix.sql.MatrixSql
   
   ConnectionInfo ci = new ConnectionInfo()
   ci.setDependency('com.h2database:h2:2.4.240')
   def tmpDb = new File(System.getProperty('java.io.tmpdir'), 'testdb').getAbsolutePath()
   ci.setUrl("jdbc:h2:file:${tmpDb}")
   ci.setUser('sa')
   ci.setPassword('123')
   ci.setDriver("org.h2.Driver")
   MatrixSql matrixSql = new MatrixSql(ci)
   matrixSql.create(complexData)
   ```
3. Retreive it
   ```groovy
   Matrix stored = matrixSql.select('* from complexData')
   println "start column is of type ${stored.type('start')}, values are ${stored.column('start')}"
   ```
   
The output will be
```
start column is of type class java.sql.Date, values are [2021-12-01, 2022-07-10, 2023-05-27]
```
4. We can convert the Date column to a LocalDate
    ```groovy
    stored = stored.convert('start', LocalDate)
    println "start column is of type ${stored.type('start')}, values are ${stored.column('start')}"
    ```
   The output will be
    ```
    start column is of type class java.time.LocalDate, values are [2021-12-01, 2022-07-10, 2023-05-27]
    ```

# Release version compatibility matrix
The following table illustrates the version compatibility of 
matrix-sql and matrix core

| Matrix sql |    Matrix core | 
|-----------:|---------------:|
|      1.0.0 |          1.2.4 |
|      1.0.1 | 2.0.0 -> 2.1.1 |
|      1.1.0 |          2.2.0 |
|      2.0.0 |          3.0.0 |
|      2.1.0 |          3.1.0 |
|      2.1.1 | 3.2.0 -> 3.3.0 |