# Release history

## v2.1.2, 2026-01-31
- deprecate JsonImporter and JsonExporter in favor of JsonReader and JsonWriter
- change implementation to use Jackson streaming API instead of JsonSlurper for improved memory efficiency (O(1) memory regardless of JSON size)
- add duplicate key detection in flatten() to prevent silent data loss
- add URL and Path support to JsonImporter (matching matrix-csv API)
- add static export methods to JsonExporter for API consistency
- add comprehensive test coverage for edge cases (empty arrays, single rows, null handling)
- add input validation to prevent writing empty or null matrices
- fix JsonImporter mutation and iteration assumptions
- fix TOCTOU race condition in JsonWriter file creation
- replace broad exception catches with specific exception types
- upgrade dependencies
  - com.fasterxml.jackson.core:jackson-core [2.20.0 -> 2.20.1]
  - com.fasterxml.jackson.core:jackson-databind [2.20.0 -> 2.20.1]
  
## v2.1.1, 2025-09-06
- Upgrade dependencies
  - com.fasterxml.jackson.core:jackson-core [2.18.3 -> 2.20.0]
  - com.fasterxml.jackson.core:jackson-databind [2.18.3 -> 2.20.0]
  - org.jsoup:jsoup [1.21.1 -> 1.21.2]
  
## v2.1.0, 2025-04-01
Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-json/2.1.0/matrix-json-2.1.0.jar)
- rename package from se.alipsa.matrix.matrixjson to se.alipsa.matrix.json

## v2.0.0, 2025-03-12
- require JDK 21

## v1.1.0, 2025-01-08
- adapt to matrix core 2.2.0

## v1.0.0, 2024-10-31
- initial version