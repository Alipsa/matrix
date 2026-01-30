# Release history

## v2.2.2, In progress
- deprecate CsvImporter and CsvExporter in favor of CsvReader and CsvWriter
- upgrade commons-csv from 1.14.0 to 1.14.1
- fix bug: empty CSV files now handled correctly (no more IndexOutOfBoundsException)
- fix typos in error messages ("extected" → "expected")
- add comprehensive test coverage for edge cases (empty CSV, header-only, single row/column, mismatched columns)
- add null validation to CsvExporter methods
- add GroovyDoc documentation to CsvImporter and CsvExporter classes and methods

## v2.2.1, 2025-07-10
- Add default value for CSV importer when importing from a file to make the api mirror the exporter.

## v2.2.0, 2025-06-03
- add CsvImporter.importCsv method from a url string
- add CsvImporter.importCsv method from a Path
- add CsvImporter.importCsv method from a Reader
- add release java version to pom

Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-csv/2.2.0/matrix-csv-2.2.0.jar)

## v2.1.0, 2025-04-01
Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-csv/2.1.0/matrix-csv-2.1.0.jar)

- change package name from `se.alipsa.matrix.matrixcsv` to `se.alipsa.matrix.csv`
- upgrade commons-csv from 1.13.0 to 1.14.0

## v2.0.0, 2025-03-12
- Require JDK 21

## v1.1.0, 2025-01-08
- adapt to matrix-core 2.2.0

## v1.0.1, 2024-10-31
- adapt to matrix-core 2.0.0

## v1.0.0, 2023-05-18
- initial release