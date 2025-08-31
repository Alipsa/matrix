# Matrix SQL Module

The Matrix SQL module aims to make communication between the Matrix library and a relational database as easy as possible. It provides a straightforward way to create tables from Matrix objects, insert data, query databases, and convert the results back to Matrix objects.

## Installation

To use the matrix-sql module, you need to add it as a dependency to your project.

### Gradle Configuration

```groovy
implementation 'org.apache.groovy:groovy:5.0.0'
implementation platform('se.alipsa.matrix:matrix-bom:2.2.3')
implementation 'se.alipsa.matrix:matrix-core'
implementation 'se.alipsa.matrix:matrix-sql'
```

### Maven Configuration

```xml
<project>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-bom</artifactId>
        <version>2.2.3</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
    <dependencies>
      <dependency>
          <groupId>org.apache.groovy</groupId>
          <artifactId>groovy</artifactId>
          <version>5.0.0</version>
      </dependency>
      <dependency>
          <groupId>se.alipsa.matrix</groupId>
          <artifactId>matrix-core</artifactId>
      </dependency>
      <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-sql</artifactId>
      </dependency>
    </dependencies>
</project>
```

## Core Classes

The matrix-sql module provides two main classes for database interaction:

1. **MatrixSql** - The primary class for database operations, which manages connections automatically.
2. **MatrixDbUtil** - A utility class for when you want to manage database connections yourself.

## Creating a MatrixSql Instance

The `MatrixSql` class is created using a `ConnectionInfo` object from the data-utils library. The `ConnectionInfo` contains the JDBC URL, credentials, and information about the JDBC driver to use. The driver will be downloaded if needed and added to the classpath, enabling dynamic instantiation of the driver when connecting to the database.

```groovy
import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.matrix.sql.MatrixSql

// Create a ConnectionInfo object
ConnectionInfo ci = new ConnectionInfo()
ci.setDependency('org.postgresql:postgresql:42.7.3')
ci.setUrl("jdbc:postgresql://somedb.somedomain.com/mydatabase")
ci.setUser('myuser')
ci.setPassword('123password')
ci.setDriver("org.postgresql.Driver")

// Create a MatrixSql instance
MatrixSql matrixSql = new MatrixSql(ci)
```

Note that the syntax for specifying the driver dependency follows the Gradle short form. For example, to get the short form for a PostgreSQL driver, you can look it up on [mvnrepository.com](https://mvnrepository.com) and find the string in the "Gradle (short)" tab, e.g., 'org.postgresql:postgresql:42.7.3'.

## Database Operations

The MatrixSql class provides several methods for interacting with databases:

### Creating Tables

The simplest way to create a table corresponding to a Matrix and populate it is to use the `create` method:

```groovy
// Create a table from a Matrix
matrixSql.create(myMatrix)

// Create a table with a primary key
matrixSql.create(myMatrix, 'id')
```

### Checking if a Table Exists

Before performing operations on a table, you might want to check if it exists:

```groovy
if (matrixSql.tableExists(myMatrix)) {
    // Table exists, perform operations
}

// Or check by table name
if (matrixSql.tableExists('employee_data')) {
    // Table exists, perform operations
}
```

### Dropping Tables

You can drop tables using either the Matrix object or the table name:

```groovy
// Drop a table using a Matrix
matrixSql.dropTable(myMatrix)

// Drop a table using its name
matrixSql.dropTable('employee_data')

// Check if table exists before dropping
if (matrixSql.tableExists(myMatrix)) {
   matrixSql.dropTable(myMatrix)
}
```

### Inserting Data

You can insert data into an existing table:

```groovy
// Insert data from a Matrix into a table
matrixSql.insert(myMatrix, 'employee_data')
```

### Selecting Data

You can execute SQL SELECT queries and get the results as a Matrix:

```groovy
// Select all data from a table
Matrix result = matrixSql.select('* from employee_data')

// Select with a WHERE clause
Matrix filteredResult = matrixSql.select('* from employee_data WHERE department = ?', ['Engineering'])

// Select specific columns
Matrix nameAndSalary = matrixSql.select('name, salary from employee_data')
```

### Updating Data

You can execute SQL UPDATE statements:

```groovy
// Update data in a table
int rowsAffected = matrixSql.update("UPDATE employee_data SET salary = salary * 1.1 WHERE department = 'Engineering'"")
```

### Deleting Data

You can execute SQL DELETE statements:

```groovy
// Delete data from a table
int rowsAffected = matrixSql.delete("FROM employee_data WHERE department = 'Engineering'")
```

### Closing the Connection

The `MatrixSql` class implements `Closeable` and should be used in a try-with-resources block:

```groovy
try (MatrixSql matrixSql = new MatrixSql(ci)) {
    // Perform database operations
    Matrix result = matrixSql.select('* from employee_data')
    // Process the result
} // Connection is automatically closed
```

If you're not using a try-with-resources block, you must explicitly close the connection:

```groovy
MatrixSql matrixSql = new MatrixSql(ci)
try {
    // Perform database operations
} finally {
    matrixSql.close()
}
```

## Working with Different Database Types

The matrix-sql module supports various database types. Here are examples for some common databases:

### PostgreSQL

```groovy
ConnectionInfo ci = new ConnectionInfo()
ci.setDependency('org.postgresql:postgresql:42.7.3')
ci.setUrl("jdbc:postgresql://localhost:5432/mydb")
ci.setUser('postgres')
ci.setPassword('password')
ci.setDriver("org.postgresql.Driver")
MatrixSql matrixSql = new MatrixSql(ci)
```

### MySQL

```groovy
ConnectionInfo ci = new ConnectionInfo()
ci.setDependency('mysql:mysql-connector-java:8.0.28')
ci.setUrl("jdbc:mysql://localhost:3306/mydb")
ci.setUser('root')
ci.setPassword('password')
ci.setDriver("com.mysql.cj.jdbc.Driver")
MatrixSql matrixSql = new MatrixSql(ci)
```

### SQLite

```groovy
ConnectionInfo ci = new ConnectionInfo()
ci.setDependency('org.xerial:sqlite-jdbc:3.36.0.3')
ci.setUrl("jdbc:sqlite:/path/to/database.db")
ci.setDriver("org.sqlite.JDBC")
MatrixSql matrixSql = new MatrixSql(ci)
```

### H2 Database (In-Memory)

```groovy
ConnectionInfo ci = new ConnectionInfo()
ci.setDependency('com.h2database:h2:2.3.232')
ci.setUrl("jdbc:h2:mem:testdb")
ci.setUser('sa')
ci.setPassword('')
ci.setDriver("org.h2.Driver")
MatrixSql matrixSql = new MatrixSql(ci)
```

### H2 Database (File-Based)

```groovy
ConnectionInfo ci = new ConnectionInfo()
ci.setDependency('com.h2database:h2:2.3.232')
def tmpDb = new File(System.getProperty('java.io.tmpdir'), 'testdb').getAbsolutePath()
ci.setUrl("jdbc:h2:file:${tmpDb}")
ci.setUser('sa')
ci.setPassword('123')
ci.setDriver("org.h2.Driver")
MatrixSql matrixSql = new MatrixSql(ci)
```

## Working with Date Types

When dealing with a Matrix containing `LocalDate` objects, there's an inevitable conversion between `LocalDate` and `java.sql.Date` that you need to be aware of. When you store a Matrix with `LocalDate` columns in a database, the datatype for `LocalDate` is mapped to `DATE` in the database. When you retrieve it, the default mapping of the `DATE` type is `java.sql.Date`.

Here's an example of this conversion process:

### 1. Create a Matrix with LocalDate

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ListConverter
import java.time.LocalDate

// Create a Matrix with a LocalDate column
Matrix complexData = Matrix.builder('complexData').data([
    'place': [1, 20, 3],
    'firstname': ['Lorena', 'Marianne', 'Lotte'],
    'start': ListConverter.toLocalDates('2021-12-01', '2022-07-10', '2023-05-27')
]).types([int, String, LocalDate]).build()
```

### 2. Save it to the Database

```groovy
import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.matrix.sql.MatrixSql

// Create a connection to an H2 database
ConnectionInfo ci = new ConnectionInfo()
ci.setDependency('com.h2database:h2:2.3.232')
def tmpDb = new File(System.getProperty('java.io.tmpdir'), 'testdb').getAbsolutePath()
ci.setUrl("jdbc:h2:file:${tmpDb}")
ci.setUser('sa')
ci.setPassword('123')
ci.setDriver("org.h2.Driver")

// Create a MatrixSql instance and save the Matrix
MatrixSql matrixSql = new MatrixSql(ci)
matrixSql.create(complexData)
```

### 3. Retrieve the Data

```groovy
// Select all data from the table
Matrix stored = matrixSql.select('* from complexData')

// Check the type of the 'start' column
println "start column is of type ${stored.type('start')}, values are ${stored.column('start')}"
// Output: start column is of type class java.sql.Date, values are [2021-12-01, 2022-07-10, 2023-05-27]
```

### 4. Convert the Date Column to LocalDate

```groovy
// Convert the 'start' column to LocalDate
stored = stored.convert('start', LocalDate)

// Check the type after conversion
println "start column is of type ${stored.type('start')}, values are ${stored.column('start')}"
// Output: start column is of type class java.time.LocalDate, values are [2021-12-01, 2022-07-10, 2023-05-27]
```

## Using MatrixDbUtil

If you want to manage database connections yourself, you can use the `MatrixDbUtil` class instead of `MatrixSql`. The API is similar, but all methods require a `java.sql.Connection` parameter.

```groovy
import se.alipsa.matrix.sql.MatrixDbUtil
import se.alipsa.matrix.sql.SqlTypeMapper
import java.sql.Connection
import java.sql.DriverManager

// Create a connection
Connection connection = DriverManager.getConnection("jdbc:h2:mem:testdb", "sa", "")

// Create a MatrixDbUtil instance
MatrixDbUtil dbUtil = new MatrixDbUtil(SqlTypeMapper.H2)

// Perform operations
dbUtil.create(connection, myMatrix)
Matrix result = dbUtil.select(connection, "* from myTable")

// Close the connection when done
connection.close()
```

## Complete Example

Here's a complete example that demonstrates creating a Matrix, saving it to a database, and retrieving it:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.groovy.datautil.ConnectionInfo
import se.alipsa.matrix.sql.MatrixSql
import java.time.LocalDate

// Create a Matrix with employee data
Matrix employees = Matrix.builder('employees').data([
    id: [1, 2, 3, 4],
    name: ['Alice', 'Bob', 'Charlie', 'Diana'],
    department: ['HR', 'Engineering', 'Engineering', 'Marketing'],
    salary: [60000, 75000, 72000, 65000],
    hire_date: [
        LocalDate.of(2020, 3, 15),
        LocalDate.of(2019, 7, 10),
        LocalDate.of(2021, 1, 5),
        LocalDate.of(2022, 5, 20)
    ]
]).types([int, String, String, int, LocalDate]).build()

// Set up a connection to an H2 in-memory database
ConnectionInfo ci = new ConnectionInfo()
ci.setDependency('com.h2database:h2:2.3.232')
ci.setUrl("jdbc:h2:mem:employeedb;CASE_INSENSITIVE_IDENTIFIERS=true")
ci.setUser('sa')
ci.setPassword('')
ci.setDriver("org.h2.Driver")

// Use try-with-resources to ensure the connection is closed
try (MatrixSql matrixSql = new MatrixSql(ci)) {
  // Create a table and insert the data
  matrixSql.create(employees, 'id')

  // Query all employees
  Matrix allEmployees = matrixSql.select('* from employees')
  println "All employees:"
  println allEmployees.content()

  // Query employees in the Engineering department
  Matrix engineers = matrixSql.select("* from employees WHERE department = 'Engineering'")
  println "\nEngineers:"
  println engineers.content()

  // Update salaries for the Engineering department
  int updated = matrixSql.update("employees SET salary = salary * 1.1 WHERE department = 'Engineering'")
  println "\nUpdated ${updated} employee salaries"

  // Query the updated data
  Matrix updatedEmployees = matrixSql.select('* from employees')
  println "\nEmployees after salary update:"
  println updatedEmployees.content()

  // Convert the hire_date column from java.sql.Date to LocalDate
  updatedEmployees = updatedEmployees.convert('hire_date', LocalDate)
  println "\nAfter converting hire_date to LocalDate:"
  println "hire_date column is of type ${updatedEmployees.type('hire_date')}"
}
```

## Best Practices

1. **Use Try-With-Resources**: Always use a try-with-resources block with `MatrixSql` to ensure that database connections are properly closed.

2. **Check Table Existence**: Before performing operations on a table, check if it exists to avoid errors.

3. **Handle Date Conversions**: Be aware of the conversion between `LocalDate` and `java.sql.Date` when working with date columns.

4. **Use Parameterized Queries**: Always use parameterized queries with the `?` placeholder instead of string concatenation to avoid SQL injection.

5. **Manage Large Result Sets**: When dealing with large result sets, consider using pagination or limiting the number of rows returned.

6. **Handle Exceptions**: Implement proper exception handling to catch and handle database-related exceptions.

## Conclusion

The matrix-sql module provides a convenient way to interact with relational databases using Matrix objects. It simplifies common database operations like creating tables, inserting data, and querying, while handling the conversion between Matrix data types and SQL data types.

In the next section, we'll explore the matrix-bom module, which provides a Bill of Materials for dependency management.

Go to [previous section](8-matrix-xchart.md) | Go to [next section](10-matrix-bom.md) | Back to [outline](outline.md)