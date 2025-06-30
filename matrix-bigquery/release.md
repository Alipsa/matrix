# Matrix-bigquery Release History

## v0.3.1, In progress
- upgrade
  - com.google.auth:google-auth-library-bom [1.35.0 -> 1.37.1]
  - com.google.cloud:google-cloud-bigquery [2.50.1 -> 2.51.0]
  - com.google.cloud:google-cloud-resourcemanager [1.65.0 -> 1.68.0]
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
