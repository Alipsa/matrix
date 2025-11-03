# Release history
Date format used below is yyyy-MM-dd

## v2.3.0, in progress
- add option to control whether column names are quoted when creating a table

## v2.2.0, 2025-10-25
- upgrade dependencies
  - commons-io:commons-io [2.19.0 -> 2.20.0]
  - Upgrade maven-3.9.4-utils to maven-3.9.11-utils
  - data-utils [2.0.1 -> 2.0.3]
- remove trying to "fix" sql (adding select etc.) 
- Add Jaas config for mssql+javaKerberos if not set.
- Add a MatrixSqlFactory method to create a MatrixSql from a url string with optional username, password, and versions parameters.

## v2.1.1, 2025-06-02
- upgrade dependencies
  - commons-io:commons-io [2.18.0 -> 2.19.0]
  - org.junit.jupiter:junit-jupiter-api [5.12.2 -> 5.13.0]
  - org.junit.jupiter:junit-jupiter-engine [5.12.2 -> 5.13.0]
  - org.junit.platform:junit-platform-launcher [1.12.2 -> 1.13.0]
  - se.alipsa:maven-3.9.4-utils [1.0.3 -> 1.1.0]
- add release java version to pom

Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-sql/2.1.1/matrix-sql-2.1.1.jar)

## v2.1.0, 2025-04-01
Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-sql/2.1.0/matrix-sql-2.1.0.jar)

- add back MatrixSql.update(String)

## v2.0.1, 2025-03-26
- Compile statically where possible 

## v2.0.0, 2025-03-12
- add MatrixSqlFactory for simpler creation of some MatrixSql instances
- add optional matrix name argument to MatrixSql.select()
- Remove semicolon at the end of the ddl and insert statements to support Derby
- MatrixBuilder: Handle byte[] when building from a result set. 
- MatrixSql now requires Java 21 (hence semver bump to 2.0.0)

## v1.1.1, 2025-02-16
- Remove dependency on gradle-tooling-api

## v1.1.0, 2025-01-08
- adapt to matrix-core 2.2.0

## v1.0.1, 2024-10-31
- return a map of some info from create table. 
- NPE fix in MatrixSql. 
- Upgrade dependencies
- implement support for conversion of a Matrix to a ResultSet
- adapt to matrix 2.0.0

## v1.0.0, 2024-07-04
Initial version