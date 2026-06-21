# Matrix-Json Release history

## v2.3.0, <release date>
- Breaking: remove deprecated `JsonImporter` and `JsonExporter` classes (deprecated since v2.1.2). These were source- and binary-incompatible removals; replace `JsonImporter.parse(...)` calls with `JsonReader.read(...)` and `new JsonExporter(matrix).toJson(...)` calls with `JsonWriter.write(matrix)...asString()`/`.to(...)`. The static `JsonExporter.toJson(matrix, ...)` becomes `JsonWriter.write(matrix).asString()` (add `.indent()` for pretty-printing), and the static `JsonExporter.toJsonFile(matrix, file, ...)` becomes `JsonWriter.write(matrix).to(file)` (again with `.indent()` as needed). The `new JsonExporter(Grid, List<String>)` constructor has no direct equivalent: build a `Matrix` first (`Matrix.builder().data(grid).columnNames(columnNames).build()`), then call `JsonWriter.write(matrix)`
- migrate all internal tests and the `matrix-bom` integration test from `JsonImporter`/`JsonExporter` to `JsonReader`/`JsonWriter`

## v2.2.0, 2026-04-29
- add service registration (`JsonFormatProvider`) so `Matrix.read(file)` / `matrix.write(matrix, file)` auto-detect `.json` files via `ServiceLoader`
- add fluent `WriteBuilder` API for `JsonWriter`: `JsonWriter.write(matrix).indent().to(file)`
- add matrix name derivation from file/URL in `JsonReader`
- add `matrixName` option to `JsonReadOptions` to override file-derived name
- add type-conversion support (`types`, `dateTimeFormat`) to `JsonReadOptions` / `JsonFormatProvider`
- add `readString(String)` convenience method to `JsonReader`
- add missing `Path` and `String` write overloads for the formatter variant of `JsonWriter`
- switch `JsonWriter` from Groovy's `groovy.json.*` to Jackson streaming API (O(columns) peak memory)
- remove `groovy-json` build dependency (Jackson used for both reading and writing)
- enable build-level `@CompileStatic` for all production code
- add module-level CodeNarc configuration with `ignoreFailures = false`
- fix NPE risk in `writeString` when `columnFormatters` is null
- fix resource leak in `JsonReader.read(File, Charset)` (nested `withCloseable`)
- fix missing directory/parent-dir guards in formatter `write(File, ...)` overload
- deprecate all existing static methods on `JsonWriter` in favor of fluent API
- change JSON float parsing to deserialize floating-point numbers as `BigDecimal` instead of `Double` so imported decimal values keep their exact textual precision
- upgrade dependencies
  - com.fasterxml.jackson.core:jackson-core 2.21.0 -> 2.21.2 
  - com.fasterxml.jackson.core:jackson-databind 2.21.0 -> 2.21.2

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
