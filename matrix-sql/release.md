# Release history
Date format used below is yyyy-MM-dd

## v2.4.0, 2026-04-30

### SQL identifier handling
- New `SqlIdentifier` utility class for safe quoting and rendering of table/column names containing spaces, mixed case, reserved words, punctuation, or embedded quotes.
- All generated SQL (DDL, INSERT, UPDATE, DROP) now routes through `SqlIdentifier`, replacing ad-hoc string concatenation with proper double-quote escaping.
- `MatrixDbUtil.tableName()` sanitises matrix names with a regex-based normaliser instead of targeted character replacements.

### Prepared-statement convenience APIs
- `select(String, List, String)` — parameterised SELECT returning a Matrix.
- `update(String, List)` — parameterised UPDATE/INSERT/DELETE.
- `delete(String, List)` — parameterised DELETE.
- `execute(String, List)` — parameterised arbitrary SQL returning a `Map<Integer, Object>` of result sets and update counts.
- `insert(String, Matrix)` — insert a Matrix into an explicitly named table.
- `update(String, Row)` — overload that always throws `IllegalArgumentException` to prevent accidental unconstrained updates; use `update(String, Row, String...)` with match columns instead.

### Managed (non-owning) connections
- New constructors `MatrixSql(Connection, DataBaseProvider)` and `MatrixSql(Connection, SqlTypeMapper)` wrap an externally supplied connection. `close()` leaves externally supplied connections open and usable.

### Factory offline fallback
- `MatrixSqlFactory.FALLBACK_VERSIONS` map centralises pinned fallback versions for H2 and Derby.
- `createH2()`, `createDerby()`, and generic `create()` fall back to the pinned version when Maven Central is unreachable instead of throwing.
- Error message for unsupported providers now includes the dependency coordinates and a "no fallback version is configured" hint.

### MatrixResultSet hardening
- Guards (`ensureOpen`, `ensureCurrentRow`, `checkedColumnIndex`) prevent operations on closed, unpositioned, or out-of-range result sets.
- All column accessors routed through `readValue()` helper for consistency.
- `updateRow()` is a documented no-op for detached result sets.
- Null-safe primitive getters return JDBC-specified defaults (0, false) when the value is null.
- `unwrap()` and `isWrapperFor()` follow the strict JDBC contract.
- Calendar-aware `getDate`, `getTime`, `getTimestamp` and `getURL` corrected.

### MatrixDbUtil improvements
- Default minimum sizes for VARCHAR (255), DECIMAL precision (38) and scale (10) when column scanning finds no data.
- `insert(Connection, String, Matrix, boolean)` overload passes `addQuotes` through to generated SQL.

### Build and dependency changes
- Remove `dependency-resolver` dependency; replaced by `maven-utils` which now covers the same functionality.
- Migrate inline dependency declarations to Gradle version catalog (`libs.versions.toml`).
- Add `groovier-junit` test dependency for Groovy-friendly JUnit assertions.
- Remove `log4j-to-slf4j` test runtime dependency.
- Dependency upgrades:
  - se.alipsa.groovy:data-utils [2.0.4 -> 2.0.6]
  - se.alipsa:maven-utils (replaces maven-3.9.11-utils 1.1.0) [-> 1.4.1]

### Documentation and test hygiene
- Comprehensive GroovyDoc on all public methods and constructors.
- Expanded README with runnable examples for all public workflows.
- Replaced all `println`/`System.err.println` in tests with assertions or Logger.

## v2.3.0, 2026-01-31
- add option to control whether column names are quoted when creating a table
- add an execute method to MatrixSql to run arbitrary sql (update, delete, insert etc.)
- MatrixSqlFactory.create attempts to infer and set the JDBC driver when missing
- MatrixSql connection lifecycle fixes (reconnect after close)
- safer, prepared-statement updates with match-column validation
- ResultSet improvements: updateRow is a no-op for detached sets, null-safe primitive/stream getters, strict unwrap contract
- close metadata ResultSets for table discovery utilities
- Dependency upgrades:
  - commons-io:commons-io [2.20.0 -> 2.21.0]
  - se.alipsa.groovy:data-utils [2.0.3 -> 2.0.4]
  - se.alipsa:maven-3.9.11-utils [1.0.0 -> 1.1.0]

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
