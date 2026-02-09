# Matrix-bigquery Release History

## v0.5.1, In progress
- com.google.cloud:google-cloud-resourcemanager [1.84.0 -> 1.85.0]
- 
## v0.5.1, 2026-01-30
- Honour bigquery.enable_write_api system property - When set to "false", skips write channel and uses InsertAll directly
- Fixed isConnectionError - Now traverses the full cause chain to detect connection errors wrapped in multiple exception layers
- Updated insertViaInsertAll - Properly handles null originalException when called directly

## v0.5.0, 2026-01-30
- com.google.auth:google-auth-library-bom [1.38.0 -> 1.41.0]
- com.google.auth:google-auth-library-oauth2-http [1.39.0 -> 1.41.0]
- com.google.cloud:google-cloud-bigquery [2.54.2 -> 2.56.0]
- com.google.cloud:google-cloud-bigquerystorage [3.16.3 -> 3.18.0]
- com.google.cloud:google-cloud-resourcemanager [1.75.0 -> 1.82.0]
- add execute method to Bq to allow for execution of update, delete or insert queries
- add configurable sync/async query execution mode (defaults to sync for emulator compatibility, opt-in async for production)
- add configurable waitForTable timeout
- add progress bar when inserting data
- add 60 comprehensive unit tests that run in CI without external dependencies (TypeMapper and Bq utilities)
- fallback to InsertAll for BigQuery data insertion when streaming inserts fail due to connection errors
- refactored insert method into smaller, maintainable methods with single responsibilities
- fixed Timestamp type mapping bug (was incorrectly mapped to DATE instead of TIMESTAMP)
- map Short type to INT64 for better type compatibility
- test coverage using Testcontainers and BigQuery emulator (BqTestContainerTest.groovy)
- comprehensive JavaDoc documentation and code quality improvements

## v0.4.0, 2025-09-06
- Change saveToBigQuery to use the BigQuery Write API instead of the older
  load job api. This should be faster and more reliable for larger datasets.
- upgrade dependencies
  - com.google.cloud:google-cloud-bigquerystorage [3.16.0 -> 3.16.3]
  - com.google.auth:google-auth-library-bom [1.37.1 -> 1.38.0]
  - com.google.cloud:google-cloud-bigquery [2.53.0 -> 2.54.2]
  - com.google.cloud:google-cloud-resourcemanager [1.70.0 -> 1.75.0]
- Improve type mapping, especially for date/time types

Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-bigquery/0.4.4/matrix-bigquery-0.4.0.jar)

## v0.3.2, 2025-07-19
- Upgrade dependencies
  - com.google.cloud:google-cloud-bigquery [2.52.0 -> 2.53.0]
  - com.google.cloud:google-cloud-bigquerystorage [3.15.3 -> 3.16.0]
  - com.google.cloud:google-cloud-resourcemanager [1.69.0 -> 1.70.0]

Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-bigquery/0.3.2/matrix-bigquery-0.3.2.jar)

## v0.3.1, 2025-07-10
- upgrade
  - com.google.auth:google-auth-library-bom [1.35.0 -> 1.37.1]
  - com.google.cloud:google-cloud-bigquery [2.50.1 -> 2.52.0]
  - com.google.cloud:google-cloud-resourcemanager [1.65.0 -> 1.69.0]
  -  com.google.cloud:google-cloud-bigquerystorage [3.15.2 -> 3.15.3]
- Add google-cloud-bigquerystorage:3.15.2
- Convert complex objects such as BigDecimal, Date, LocalDate etc to a String format
  that BigQuery can accept as primitive (otherwise they become structs)


## v0.3.0, 2025-05-25
Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-bigquery/0.3/matrix-bigquery-0.3.jar)
- upgrade 
  - com.google.auth:google-auth-library-bom [1.33.1 -> 1.35.0]
  - com.google.auth:google-auth-library-oauth2-http [1.33.1 -> 1.35.0]
  - com.google.cloud:google-cloud-bigquery [2.49.0 -> 2.50.1]
  - com.google.cloud:google-cloud-resourcemanager [1.62.0 -> 1.65.0]

## v0.2, 2025-03-12
- Require JDK 21
- Implement getProjects()
- Upgrade dependencies

## v0.1, 2025-02-16
- initial release
