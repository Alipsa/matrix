# Matrix-bigquery Release History

## v0.5.0, In progress
- com.google.auth:google-auth-library-bom [1.38.0 -> 1.40.0]
- com.google.auth:google-auth-library-oauth2-http [1.39.0 -> 1.40.0]
- com.google.cloud:google-cloud-bigquery [2.54.2 -> 2.55.3]
- com.google.cloud:google-cloud-bigquerystorage [3.16.3 -> 3.17.3]
- com.google.cloud:google-cloud-resourcemanager [1.75.0 -> 1.80.0]
- add execute method to Bq to allow for execution of update, delete or insert queries
- add progress bar when inserting data
- Added a fallback to InsertAll for BigQuery data insertion in Bq.groovy when streaming inserts fail due to connection errors, ensuring more robust handling with the emulator and production environments. 
- Changed query execution in Bq.groovy to use synchronous queries instead of job polling, improving reliability with the BigQuery emulator. 
- Added new test coverage using Testcontainers and the BigQuery emulator, including detailed configuration for reliable integration testing in BqTestContainerTest.groovy. 
- Map Short type to INT64 in the SQL type mapper for better type compatibility with BigQuery.

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
