# Release history
Date format used below is yyyy-MM-dd

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