# Release history

## v2.2.0, 2026-05-10
- `Rdatasets.overview()` is now lazy — no network I/O on class loading; added `Rdatasets.refresh()` to clear the cache
- Add `Rdatasets.fetchData(String packageSlashItem)` single-argument overload (e.g. `fetchData('datasets/iris')`)
- Add `Rdatasets.search(String text)` to filter the overview by Item or Title (case-insensitive)
- Add `Dataset.names()`, `Dataset.mapNames()`, and `Dataset.load(String)` discoverability helpers
- Add `Dataset.mapRegions(String)` to list distinct region values for a map dataset
- Fix: `Dataset.iris()` no longer applies a phantom `Id: Integer` conversion (the column does not exist in the CSV)
- Fix: `FileUtil.getResourcePath()` now throws `FileNotFoundException` instead of `NullPointerException` when a resource is not found
- `Dataset.mapData()` now trims whitespace from the dataset name and includes valid names in the error message
- Upgrade dependencies: Groovy 5.0.5 → 5.0.6, jsoup 1.22.1 → 1.22.2

## v2.1.2, 2026-01-31
- fix critical bug: Rdatasets methods now handle missing datasets correctly (no more IndexOutOfBoundsException)
- fix null parameter validation in Rdatasets (now checks both packageName and itemName for null)
- add null safety to mapDataSet() method
- use consistent Matrix naming approach (standardized on matrixName() builder method)
- replace generic Exception with FileNotFoundException in FileUtil
- use string interpolation instead of concatenation for error messages
- add comprehensive test coverage for error cases and edge cases
- add Logger implementation for better diagnostics
- refactor mapData() to use Map lookup for improved performance
- upgrade dependencies
  - org.jsoup:jsoup [1.21.1 -> 1.22.1]

## v2.1.1, 2025-07-19
- Upgrade dependencies
  - org.jsoup:jsoup [1.20.1 -> 1.21.1]

Jar available at https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-datasets/2.1.1/matrix-datasets-2.1.1.jar

## v2.1.0, 2025-05-28
- Add easy access to the 2536 datasets from the R datasets project through the RDatasets class
Jar available at https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-datasets/2.1.0/matrix-datasets-2.1.0.jar

## v2.0.1, 2025-03-26
- set FileUtil to CompileStatic
Jar available at https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-datasets/2.0.1/matrix-datasets-2.0.1.jar

## v2.0.0, 2025-03-12
- Require JDK 21
Jar available at https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-datasets/2.0.0/matrix-datasets-2.0.0.jar

## v1.1.0, 2025-01-06
- adopt to matrix core 2.2.0

## v1.0.4, 2024-10-31
- adapt to new matrix 2x style creation using builders

## v1.0.3, 2024-07-04
- add cars dataset
- change datatype for month and day of airquality to Short

## v 1.0.2, 2023-08-06
- replace iris dataset with a better version
- upgrade matrix to ver 1.1.2

## v1.0.1, 2023-05-18
- renamed to matrix-datasets
- add map data

## v1.0.0, 2023-04-21
- initial release