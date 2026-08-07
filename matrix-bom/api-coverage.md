# BOM API coverage checklist

This checklist records the documented API checkpoints exercised by the BOM consumer tests. The
tests run against the resolved artifacts from the isolated repository, so a passing entry proves
both the API call and the BOM dependency graph that supplies it. It is intentionally a checkpoint
list rather than a claim that one integration test replaces a module's unit-test suite.

## Cross-cutting checks

- [x] `BomResolutionIT.resolvesTheBomVersionForEveryModule` resolves one representative class per
  module, checks its code-source version, rejects unexpected snapshots, and asserts Groovy 5.0.8.
- [x] `CrossModuleWorkflowIT.importsTransformsModelsChartsAndExports` covers the documented
  import → core transform → statistics → chart → CSV export workflow.
- [x] `JavaConsumerIT.javaMapApiRemainsSourceCompatible` compiles and runs the Java map-builder,
  `Columns`, `CollectionUtils`, `Matrix.and`, and chart entry-point calls.

## Release-gate modules

### matrix-core (`CoreApiIT`)

- [x] Matrix builders, typed columns, rows, `dimensions`, and Java map-shaped overloads —
  `buildersColumnsRowsAndJavaMapShapes`.
- [x] Column arithmetic returning `Column`, `top`, `bottom`, `rename`, `merge`, and `sample` —
  `columnArithmeticAndCoreViews`.
- [x] Join, cross-join, grouping, rolling windows, and summary —
  `joinsGroupingRollingAndSummary`.
- [x] Converter, value-converter, list-converter, assertion APIs, and the Grid
  checked `data` view (write-through values, rejected structural mutation) —
  `convertersAndAssertionsRemainUsable`.

### matrix-charts (`ChartsApiIT`)

- [x] `Charts.plot`/`Charts.chart`, aesthetics, geoms, facets, scales, and stylesheet —
  `charmDslBuildsFacetedStyledCharts`.
- [x] Spec/build/render lifecycle and `PlotGrid` — `chartsAndPlotGridRender`.
- [x] SVG, PNG, PDF, JPEG, image exporters, and `ExportFormat.fromExtension` validation —
  `nativeExportersAndFormatValidation`.
- [x] JavaFX export is intentionally not part of the default suite; it remains excluded by the
  `jfx` tag because it requires a toolkit.

### matrix-ggplot (`GgplotApiIT`)

- [x] `ggplot`/`aes`, point and line geoms, scales, facets, labels, and theme —
  `ggplotGeomsScalesFacetsLabelsAndThemeRender`.
- [x] Released ggplot artifact running against the BOM-selected charts artifact —
  `BomResolutionIT.resolvesTheBomVersionForEveryModule`.

### matrix-pict (`PictApiIT`)

- [x] Scatter, line, and bar chart builders — `chartBuildersAndExporters`.
- [x] `Plot.svg` and `Plot.png` file export plus CSS styling — `chartBuildersAndExporters`.

### matrix-xchart (`XChartApiIT`)

- [x] XY, category, and pie chart builders — `chartBuildersExportAllDocumentedFormats`.
- [x] PNG, SVG, PDF, and bitmap export paths — `chartBuildersExportAllDocumentedFormats`.
- [x] XChart's transitive dependency remains available when charts no longer supplies it —
  `chartBuildersExportAllDocumentedFormats`.

### matrix-stats (`StatsApiIT`)

- [x] Correlation, normalization, and linear/logistic regression —
  `correlationNormalizationAndRegression`.
- [x] Linear algebra and K-means clustering — `linearAlgebraAndClustering`.
- [x] The cross-module workflow also exercises model fitting against imported Matrix data —
  `CrossModuleWorkflowIT.importsTransformsModelsChartsAndExports`.

## Remaining module checkpoints

### matrix-datasets (`DatasetsApiIT`)

- [x] Bundled datasets load with expected row/column shape and representative column types —
  `bundledDatasetsLoad`.

### matrix-sql (`SqlApiIT`)

- [x] `MatrixSql` create, `tableName`, `tableExists`, and read —
  `createsReadsUpdatesAndDropsTables`.
- [x] Update and drop operations against an in-memory H2 database —
  `createsReadsUpdatesAndDropsTables`.

### matrix-spreadsheet (`SpreadsheetApiIT`)

- [x] XLSX write/read, sheet selection, range selection, and multi-sheet output —
  `xlsxAndMultiSheetRoundTrip`.

### matrix-csv (`CsvApiIT`)

- [x] `CsvImporter`, `CsvExporter`, UTF-8 charset, matrix names, and the fluent `CsvReader` —
  `csvFormatsAndFluentReaderRoundTrip`.
- [x] Header/format handling through Commons CSV — `csvFormatsAndFluentReaderRoundTrip`.

### matrix-json (`JsonApiIT`)

- [x] `JsonReader`/`JsonWriter`, indentation, nested List/Map values, and zero-column matrices —
  `jsonRoundTripsNestedValuesAndGridData`.
- [x] Grid-to-Matrix conversion — `jsonRoundTripsNestedValuesAndGridData`.

### matrix-parquet (`ParquetApiIT`)

- [x] Byte and file/path round trips preserve values, names, types, and nulls —
  `parquetRoundTripsValuesTypesNamesAndNulls`.
- [x] Documented compression and missing-file validation —
  `parquetRoundTripsValuesTypesNamesAndNulls`; the test writes with explicit GZIP compression and
  verifies the Parquet footer codec.

### matrix-avro (`AvroApiIT`)

- [x] Inferred schema, explicit matrix name, optional fields, and typed round trip —
  `avroRoundTripsTypedAndOptionalColumns`.
- [x] Byte-array writer/reader overloads — `avroRoundTripsTypedAndOptionalColumns`.

### matrix-arff (`ArffApiIT`)

- [x] Numeric, nominal, string, date-compatible values, relation names, and missing values —
  `arffRoundTripAndValidation`.
- [x] Quoted separators and malformed-input validation — `arffRoundTripAndValidation`.

### matrix-tablesaw (`TablesawApiIT`)

- [x] Matrix/Tablesaw conversion with row and column fidelity —
  `convertsTablesAndReadsGtableCsv`.
- [x] Frequency, rounding, and Gtable CSV reader support — `convertsTablesAndReadsGtableCsv`.

### matrix-smile (`SmileApiIT`)

- [x] `SmileUtil` DataFrame conversion and description — `smileConversionsAndStatistics`.
- [x] Normal distribution, fitting, and correlation test APIs — `smileConversionsAndStatistics`.

### matrix-groovy-ext (`GroovyExtApiIT`)

- [x] Registered `NumberExtension` catalog and runtime extension dispatch —
  `numberExtensionCatalogAndRuntimeRegistration`.

### matrix-logging (`LoggingApiIT`)

- [x] Optional `MatrixLogging` backend and core `Logger` routing are available —
  `matrixLoggerUsesTheOptionalBackend`.
- [x] The JaCoCo XML has no instrumented production instructions for this marker-only logging
  module; its zero is expected and is not treated as an uncovered API.

### matrix-gsheets (`GsheetsApiIT`)

- [x] Offline-safe A1 range parsing, column counting, and column-number conversion —
  `parsesA1RangesWithoutCredentials`.
- [x] Credentialed calls remain external-only and are not attempted by this offline test.

### matrix-bigquery (`BigQueryApiIT`)

- [x] The test-container/emulator wiring is retained and selected by `bigquery,external` tags —
  `bigQueryRoundTrip`.
- [x] The external test creates a dataset, saves a Matrix, queries it back, and verifies the
  round-trip content using the BigQuery emulator — `bigQueryRoundTrip`.

## JaCoCo baseline

The following baseline was produced by the isolated verifier. Instruction and branch figures are
`covered/total`; they are reported for trend review and do not gate a release. The per-module XML
files are authoritative because the combined HTML report is grouped by package.

| Module | Instructions | Branches |
|---|---:|---:|
| matrix-arff | 2,571/8,189 (31.4%) | 291/984 (29.6%) |
| matrix-avro | 1,550/11,443 (13.5%) | 182/1,488 (12.2%) |
| matrix-bigquery | 0/5,536 (0.0%) | 0/614 (0.0%) |
| matrix-charts | 23,695/130,658 (18.1%) | 1,510/12,054 (12.5%) |
| matrix-core | 9,811/42,379 (23.2%) | 835/4,658 (17.9%) |
| matrix-csv | 651/6,415 (10.1%) | 61/716 (8.5%) |
| matrix-datasets | 1,107/3,322 (33.3%) | 2/156 (1.3%) |
| matrix-ggplot | 7,018/107,687 (6.5%) | 476/12,738 (3.7%) |
| matrix-groovy-ext | 505/1,676 (30.1%) | 32/146 (21.9%) |
| matrix-gsheets | 205/5,269 (3.9%) | 20/616 (3.2%) |
| matrix-json | 625/2,357 (26.5%) | 69/282 (24.5%) |
| matrix-logging | 0/0 (n/a) | 0/0 (n/a) |
| matrix-parquet | 2,959/10,826 (27.3%) | 211/1,220 (17.3%) |
| matrix-pict | 1,114/6,579 (16.9%) | 96/532 (18.0%) |
| matrix-smile | 1,383/15,156 (9.1%) | 114/1,274 (8.9%) |
| matrix-spreadsheet | 1,908/21,819 (8.7%) | 167/2,728 (6.1%) |
| matrix-sql | 1,625/8,334 (19.5%) | 95/570 (16.7%) |
| matrix-stats | 4,878/83,468 (5.8%) | 422/7,932 (5.3%) |
| matrix-tablesaw | 1,012/7,446 (13.6%) | 87/728 (12.0%) |
| matrix-xchart | 407/4,349 (9.4%) | 27/256 (10.5%) |

Reports:

- Combined HTML: `matrix-bom/target/site/jacoco-bom-api/index.html`
- Combined XML: `matrix-bom/target/jacoco-bom-api.xml`
- Per-module XML: `matrix-bom/target/jacoco-per-module/<module>.xml`
- japicmp report: `matrix-bom/japicmp/target/japicmp/cmp.html` (findings are warnings)
