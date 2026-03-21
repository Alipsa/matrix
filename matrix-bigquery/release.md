# Matrix-bigquery Release History

## v0.6.0, 2026-03-21
- `com.google.auth:google-auth-library-bom` [1.42.1 -> 1.43.0]
- `com.google.cloud:google-cloud-bigquery` [2.58.0 -> 2.62.0]
- `com.google.cloud:google-cloud-bigquerystorage` [3.20.0 -> 3.24.0]
- `com.google.cloud:google-cloud-resourcemanager` [1.84.0 -> 1.90.0]
- parameterize `getTableInfo(...)` and validate dataset identifiers before building the `INFORMATION_SCHEMA` query
- replace the shared `SimpleDateFormat` with thread-safe `DateTimeFormatter` usage for legacy `Date` handling
- add explicit `Instant` handling in `convertObjectValue(...)`
- validate `matrix.matrixName` and `datasetName` early in `saveToBigQuery(...)`
- preserve microsecond precision when converting BigQuery timestamps to `Instant`
- scope dataset and table IDs consistently to the configured project
- honor `append=false` even when writes go through `InsertAll`, including disabled-write-api and connection-error fallback paths
- stop stripping tabs, newlines, NUL, and format characters from strings before JSON serialization
- auto-disable write progress bars when no interactive terminal is present; add `bigquery.enable_progress_bar` to force enable or disable
- refresh the README, tutorial, and cookbook documentation for the current API

### Behavioral clarifications
- `saveToBigQuery(matrix, datasetName)` uses `matrix.matrixName` as the table name; the second argument is the dataset only
- `saveToBigQuery(..., append)` defaults to `append=false`, which means overwrite existing table data
- valid JSON control characters are escaped by Jackson instead of being removed from string content

### Migration notes
- if you relied on the old fallback bug where `append=false` still appended rows on `InsertAll`, pass `true` explicitly
- if you worked around stripped tabs/newlines by pre-encoding strings before saving, you can remove that workaround in `0.6.0`

## v0.5.1, 2026-01-30
- Honour `bigquery.enable_write_api` system property. When set to `"false"`, skips write channel and uses InsertAll directly
- Fixed `isConnectionError`. Now traverses the full cause chain to detect connection errors wrapped in multiple exception layers
- Updated `insertViaInsertAll`. Properly handles null `originalException` when called directly

## v0.5.0, 2026-01-30
- `com.google.auth:google-auth-library-bom` [1.38.0 -> 1.41.0]
- `com.google.auth:google-auth-library-oauth2-http` [1.39.0 -> 1.41.0]
- `com.google.cloud:google-cloud-bigquery` [2.54.2 -> 2.56.0]
- `com.google.cloud:google-cloud-bigquerystorage` [3.16.3 -> 3.18.0]
- `com.google.cloud:google-cloud-resourcemanager` [1.75.0 -> 1.82.0]
- add `execute(...)` to `Bq` to allow update, delete, or insert queries
- add configurable sync/async query execution mode (defaults to sync for emulator compatibility, opt-in async for production)
- add configurable wait-for-table timeout
- add progress bar when inserting data
- add comprehensive unit tests that run in CI without external dependencies (`TypeMapper` and `Bq` utilities)
- fallback to InsertAll for BigQuery data insertion when streaming inserts fail due to connection errors
- refactor insert method into smaller methods with single responsibilities
- fix Timestamp type mapping bug (was incorrectly mapped to DATE instead of TIMESTAMP)
- map `Short` type to `INT64` for better type compatibility
- add Testcontainers coverage using the BigQuery emulator (`BqTestContainerTest.groovy`)
- add JavaDoc documentation and code quality improvements

## v0.4.0, 2025-09-06
- Change `saveToBigQuery(...)` to use the BigQuery Write API instead of the older load job API. This should be faster and more reliable for larger datasets
- upgrade dependencies
  - `com.google.cloud:google-cloud-bigquerystorage` [3.16.0 -> 3.16.3]
  - `com.google.auth:google-auth-library-bom` [1.37.1 -> 1.38.0]
  - `com.google.cloud:google-cloud-bigquery` [2.53.0 -> 2.54.2]
  - `com.google.cloud:google-cloud-resourcemanager` [1.70.0 -> 1.75.0]
- improve type mapping, especially for date/time types

Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-bigquery/0.4.4/matrix-bigquery-0.4.0.jar)

## v0.3.2, 2025-07-19
- upgrade dependencies
  - `com.google.cloud:google-cloud-bigquery` [2.52.0 -> 2.53.0]
  - `com.google.cloud:google-cloud-bigquerystorage` [3.15.3 -> 3.16.0]
  - `com.google.cloud:google-cloud-resourcemanager` [1.69.0 -> 1.70.0]

Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-bigquery/0.3.2/matrix-bigquery-0.3.2.jar)

## v0.3.1, 2025-07-10
- upgrade
  - `com.google.auth:google-auth-library-bom` [1.35.0 -> 1.37.1]
  - `com.google.cloud:google-cloud-bigquery` [2.50.1 -> 2.52.0]
  - `com.google.cloud:google-cloud-resourcemanager` [1.65.0 -> 1.69.0]
  - `com.google.cloud:google-cloud-bigquerystorage` [3.15.2 -> 3.15.3]
- add `google-cloud-bigquerystorage:3.15.2`
- convert complex objects such as `BigDecimal`, `Date`, and `LocalDate` to string formats that BigQuery can accept as primitives

## v0.3.0, 2025-05-25
Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-bigquery/0.3/matrix-bigquery-0.3.jar)
- upgrade
  - `com.google.auth:google-auth-library-bom` [1.33.1 -> 1.35.0]
  - `com.google.auth:google-auth-library-oauth2-http` [1.33.1 -> 1.35.0]
  - `com.google.cloud:google-cloud-bigquery` [2.49.0 -> 2.50.1]
  - `com.google.cloud:google-cloud-resourcemanager` [1.62.0 -> 1.65.0]

## v0.2, 2025-03-12
- require JDK 21
- implement `getProjects()`
- upgrade dependencies

## v0.1, 2025-02-16
- initial release
