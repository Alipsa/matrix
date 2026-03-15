# Plan: ServiceLoader-based Format Provider SPI

## Context

The matrix project has 6 file-format modules (csv, json, spreadsheet, arff, avro, parquet), each with its own reader/writer classes. Users must know which class to import for each format. This plan adds a unified `Matrix.read(file)` / `matrix.write(file)` API that auto-detects the format by file extension and delegates to the appropriate module via Java's `ServiceLoader`. Adding a format module to the classpath is all that's needed to support that format.

## Design Decisions

- **Java ServiceLoader** (not Groovy Extension Modules) -- SPI is the standard plugin discovery mechanism; extension modules are for adding methods to existing classes
- **`Map<String, ?>` options in the SPI interface** -- matrix-core cannot depend on format modules, so the interface must be generic. Each provider converts the map to its typed Options class internally
- **Static `read(...)`, instance `write(...)` on `Matrix`** -- `Matrix.read(file)` is natural alongside `Matrix.builder()`, while `matrix.write(file)` fits the existing object-oriented export style better than a new static write helper
- **Options classes per module** -- Typed, fluent builders with `describe()` for discoverability and `toMap()` for SPI interop. Users who want type safety use the Options class; script users pass simple map keys
- **`Matrix.listReadOptions(ext)` / `Matrix.listWriteOptions(ext)`** -- convenience to discover format-specific read and write options for a given extension

---

## Phase 1: Core SPI Infrastructure (matrix-core)

### 1.1 [x] Create `OptionDescriptor` data class
**File:** `matrix-core/src/main/groovy/se/alipsa/matrix/core/spi/OptionDescriptor.groovy`

Simple data class describing one option (name, type, defaultValue, description, required). Includes `static String describe(List<OptionDescriptor>)` that formats as a human-readable table.

### 1.2 [x] Create `MatrixFormatProvider` interface
**File:** `matrix-core/src/main/groovy/se/alipsa/matrix/core/spi/MatrixFormatProvider.groovy`

SPI interface with: `supportedExtensions()`, `formatName()`, `canRead()`, `canWrite()`, read/write overloads for File/Path/URL/InputStream, `readOptionDescriptors()`, `writeOptionDescriptors()`.

### 1.3 [x] Create `AbstractFormatProvider` base class
**File:** `matrix-core/src/main/groovy/se/alipsa/matrix/core/spi/AbstractFormatProvider.groovy`

Default implementations: `read(Path)` → `read(File)`, `read(URL)` → `read(InputStream)`, `write(Path)` → `write(File)`, others throw UnsupportedOperationException.

### 1.4 [x] Create `FormatRegistry` singleton
**File:** `matrix-core/src/main/groovy/se/alipsa/matrix/core/spi/FormatRegistry.groovy`

Uses `ServiceLoader.load(MatrixFormatProvider)`. Caches providers by extension. Thread-safe lazy init. Methods: `getProvider(ext)`, `supportedExtensions()`, `describe()`, `listReadOptions(ext)`, `listWriteOptions(ext)`, `reload()`, `extractExtension(fileName)`.

### 1.5 [x] Add static `read()` / instance `write()` / `listReadOptions()` / `listWriteOptions()` methods to `Matrix`
**Modified:** `matrix-core/src/main/groovy/se/alipsa/matrix/core/Matrix.groovy`

Added:
- `static Matrix read(Map options = [:], File file)`
- `static Matrix read(Map options = [:], Path path)`
- `static Matrix read(Map options = [:], URL url)`
- `void write(Map options = [:], File file)`
- `void write(Map options = [:], Path path)`
- `static String listReadOptions(String fileExtension)`
- `static String listWriteOptions(String fileExtension)`

Each extracts extension, looks up provider via FormatRegistry, delegates. Clear error messages when no provider found.

### 1.6 [x] Create tests for SPI infrastructure
**File:** `matrix-core/src/test/groovy/FormatRegistryTest.groovy`

Tests: extension extraction, registry returns null for unknown, OptionDescriptor.describe() table formatting, empty descriptors, listOptions for unknown extension.

**Test command:** `./gradlew :matrix-core:test --tests "*FormatRegistryTest*"` -- PASSED (6/6)

---

## Phase 2: CSV Format Provider (reference implementation)

### 2.1 [x] Create `CsvReadOptions` class
**New file:** `matrix-csv/src/main/groovy/se/alipsa/matrix/csv/CsvReadOptions.groovy`

Wraps all CsvOption enum values as typed fields with fluent setters. Options: `delimiter`, `quote`, `escape`, `commentMarker`, `header`, `firstRowAsHeader`, `charset`, `tableName`, `types`, `dateTimeFormat`, `numberFormat`, `trim`, `ignoreEmptyLines`, `ignoreSurroundingSpaces`, `nullString`, `duplicateHeaderMode`, `recordSeparator`.

Methods: fluent setters, `static String describe()`, `Map<String, ?> toMap()`, `static List<OptionDescriptor> descriptors()`.

### 2.2 [x] Create `CsvWriteOptions` class
**New file:** `matrix-csv/src/main/groovy/se/alipsa/matrix/csv/CsvWriteOptions.groovy`

Options: `delimiter`, `quote`, `withHeader`, `charset`, `recordSeparator`.
Same pattern: fluent setters, `describe()`, `toMap()`, `descriptors()`.

### 2.3 [x] Create `CsvFormatProvider`
**New file:** `matrix-csv/src/main/groovy/se/alipsa/matrix/csv/CsvFormatProvider.groovy`

- Extensions: `['csv', 'tsv', 'tab']`
- Auto-detects TSV for `.tsv`/`.tab` extensions (sets delimiter to `\t` unless overridden)
- Read: passes options map to `CsvReader.read(map, file)` (already supports string keys case-insensitively)
- Write: builds a `WriteBuilder` from `CsvWriter.write(matrix)`, applies options, calls `.to(file)`
- `readOptionDescriptors()` / `writeOptionDescriptors()` delegate to `CsvReadOptions.descriptors()` / `CsvWriteOptions.descriptors()`

### 2.4 [x] Create ServiceLoader registration
**New file:** `matrix-csv/src/main/resources/META-INF/services/se.alipsa.matrix.core.spi.MatrixFormatProvider`
```
se.alipsa.matrix.csv.CsvFormatProvider
```

### 2.5 [x] Create tests
**New file:** `matrix-csv/src/test/groovy/CsvFormatProviderTest.groovy`

- Read CSV via `Matrix.read(file)` (SPI path)
- Read with options: `Matrix.read([delimiter: ','], file)`
- Write then read round-trip
- `CsvReadOptions.describe()` output
- `CsvWriteOptions.describe()` output
- Provider metadata

**Test command:** `./gradlew :matrix-csv:test` -- PASSED (113/113, 6 new + 107 existing, no regressions)

---

## Phase 3: Remaining Format Providers

### 3. JSON Provider (matrix-json)

#### 3.1 [x] Create `JsonReadOptions`
**New file:** `matrix-json/src/main/groovy/se/alipsa/matrix/json/JsonReadOptions.groovy`
Options: `charset` (String, default 'UTF-8')

#### 3.2 [x] Create `JsonWriteOptions`
**New file:** `matrix-json/src/main/groovy/se/alipsa/matrix/json/JsonWriteOptions.groovy`
Options: `indent` (boolean, default false), `dateFormat` (String, default 'yyyy-MM-dd'), `columnFormatters` (Map<String, Closure>)

#### 3.3 [x] Create `JsonFormatProvider`
**New file:** `matrix-json/src/main/groovy/se/alipsa/matrix/json/JsonFormatProvider.groovy`
Extensions: `['json']`. Read delegates to `JsonReader.read(file, charset)`. Write delegates to `JsonWriter.write(matrix, file, columnFormatters, indent, dateFormat)`.

#### 3.4 [x] Create ServiceLoader registration file
**New file:** `matrix-json/src/main/resources/META-INF/services/se.alipsa.matrix.core.spi.MatrixFormatProvider`

#### 3.5 [x] Create `JsonFormatProviderTest`
**New file:** `matrix-json/src/test/groovy/JsonFormatProviderTest.groovy`

### 4. Spreadsheet Provider (matrix-spreadsheet)

#### 4.1 [x] Create `SpreadsheetReadOptions`
**New file:** `matrix-spreadsheet/src/main/groovy/se/alipsa/matrix/spreadsheet/SpreadsheetReadOptions.groovy`
Options: `sheet` (name or number, default 1), `startRow` (int, default 1), `endRow` (int, required), `startColumn` (int or String, default 1), `endColumn` (int or String, required), `firstRowAsColNames` (boolean, default true)

#### 4.2 [x] Create `SpreadsheetWriteOptions`
**New file:** `matrix-spreadsheet/src/main/groovy/se/alipsa/matrix/spreadsheet/SpreadsheetWriteOptions.groovy`
Options: `sheetName` (String), `startPosition` (String, default 'A1')

#### 4.3 [x] Create `SpreadsheetFormatProvider`
**New file:** `matrix-spreadsheet/src/main/groovy/se/alipsa/matrix/spreadsheet/SpreadsheetFormatProvider.groovy`
Extensions: `['xlsx', 'ods']`. Read delegates to `SpreadsheetImporter.importSpreadsheet(Map)`. Write delegates to `SpreadsheetWriter.write(matrix, file, sheetName, startPosition)`.

Note: read requires `endRow` and `endColumn`. When not specified, auto-detect via `SpreadsheetReader.findLastRow()`/`findLastCol()`.

#### 4.4 [x] Create ServiceLoader registration file
**New file:** `matrix-spreadsheet/src/main/resources/META-INF/services/se.alipsa.matrix.core.spi.MatrixFormatProvider`

#### 4.5 [x] Create `SpreadsheetFormatProviderTest`
**New file:** `matrix-spreadsheet/src/test/groovy/SpreadsheetFormatProviderTest.groovy`

### 5. ARFF Provider (matrix-arff)

#### 5.1 [x] Create `ArffReadOptions`
**New file:** `matrix-arff/src/main/groovy/se/alipsa/matrix/arff/ArffReadOptions.groovy`
Minimal (no meaningful config). Options: `matrixName` (String)

#### 5.2 [x] Create `ArffWriteOptions`
**New file:** `matrix-arff/src/main/groovy/se/alipsa/matrix/arff/ArffWriteOptions.groovy`
Options: `nominalMappings` (Map<String, List<String>>)

#### 5.3 [x] Create `ArffFormatProvider`
**New file:** `matrix-arff/src/main/groovy/se/alipsa/matrix/arff/ArffFormatProvider.groovy`
Extensions: `['arff']`. Read delegates to `MatrixArffReader.read(file)`. Write delegates to `MatrixArffWriter.write(matrix, file)` or `write(matrix, file, nominalMappings)`.

#### 5.4 [x] Create ServiceLoader registration file
**New file:** `matrix-arff/src/main/resources/META-INF/services/se.alipsa.matrix.core.spi.MatrixFormatProvider`

#### 5.5 [x] Create `ArffFormatProviderTest`
**New file:** `matrix-arff/src/test/groovy/ArffFormatProviderTest.groovy`

### 6. Avro Provider (matrix-avro)

#### 6.1 [x] Add `describe()`, `toMap()`, `fromMap()` to existing `AvroReadOptions`
**Modify:** `matrix-avro/src/main/groovy/se/alipsa/matrix/avro/AvroReadOptions.groovy`

#### 6.2 [x] Add `describe()`, `toMap()`, `fromMap()` to existing `AvroWriteOptions`
**Modify:** `matrix-avro/src/main/groovy/se/alipsa/matrix/avro/AvroWriteOptions.groovy`

#### 6.3 [x] Create `AvroFormatProvider`
**New file:** `matrix-avro/src/main/groovy/se/alipsa/matrix/avro/AvroFormatProvider.groovy`
Extensions: `['avro']`. Converts map to existing `AvroReadOptions`/`AvroWriteOptions` via `fromMap()`. Read delegates to `MatrixAvroReader.read(file, options)`. Write delegates to `MatrixAvroWriter.write(matrix, file, options)`.

#### 6.4 [x] Create ServiceLoader registration file
**New file:** `matrix-avro/src/main/resources/META-INF/services/se.alipsa.matrix.core.spi.MatrixFormatProvider`

#### 6.5 [x] Create `AvroFormatProviderTest`
**New file:** `matrix-avro/src/test/groovy/AvroFormatProviderTest.groovy`

### 7. Parquet Provider (matrix-parquet)

#### 7.1 [x] Create `ParquetReadOptions`
**New file:** `matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/ParquetReadOptions.groovy`
Options: `matrixName` (String), `zoneId` (String, converted to ZoneId)

#### 7.2 [x] Create `ParquetWriteOptions`
**New file:** `matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/ParquetWriteOptions.groovy`
Options: `inferPrecisionAndScale` (boolean, default true), `precision` (int), `scale` (int), `decimalMeta` (Map<String, int[]>), `zoneId` (String)

#### 7.3 [x] Create `ParquetFormatProvider`
**New file:** `matrix-parquet/src/main/groovy/se/alipsa/matrix/parquet/ParquetFormatProvider.groovy`
Extensions: `['parquet']`. Read delegates to `MatrixParquetReader.read(file, ...)`. Write delegates to `MatrixParquetWriter.write(matrix, file, ...)`.

#### 7.4 [x] Create ServiceLoader registration file
**New file:** `matrix-parquet/src/main/resources/META-INF/services/se.alipsa.matrix.core.spi.MatrixFormatProvider`

#### 7.5 [x] Create `ParquetFormatProviderTest`
**New file:** `matrix-parquet/src/test/groovy/ParquetFormatProviderTest.groovy`

---

## Phase 4: Documentation

### 8.1 [x] Update `matrix-core/readme.md`
Add section documenting `Matrix.read()` / `matrix.write()` / `Matrix.listReadOptions()` / `Matrix.listWriteOptions()`, `FormatRegistry.describe()`, and the SPI mechanism.

### 8.2 [x] Update format module READMEs
Each module's README gets a new section showing usage via `Matrix.read()` / `matrix.write()`, available options, and `*ReadOptions.describe()` / `*WriteOptions.describe()` for scripting discoverability.

Modules: `matrix-csv/README.md`, `matrix-json/README.md`, `matrix-spreadsheet/README.md`, `matrix-arff/README.md`, `matrix-avro/README.md`, `matrix-parquet/readme.md`

---

## File Summary

### New files (matrix-core) -- DONE
- `matrix-core/src/main/groovy/se/alipsa/matrix/core/spi/OptionDescriptor.groovy`
- `matrix-core/src/main/groovy/se/alipsa/matrix/core/spi/MatrixFormatProvider.groovy`
- `matrix-core/src/main/groovy/se/alipsa/matrix/core/spi/AbstractFormatProvider.groovy`
- `matrix-core/src/main/groovy/se/alipsa/matrix/core/spi/FormatRegistry.groovy`
- `matrix-core/src/test/groovy/FormatRegistryTest.groovy`

### New files per format module (6 modules x ~4 files each) -- DONE
- `*FormatProvider.groovy`
- `*ReadOptions.groovy` (new, except avro which modifies existing)
- `*WriteOptions.groovy` (new, except avro which modifies existing)
- `META-INF/services/se.alipsa.matrix.core.spi.MatrixFormatProvider`
- `*FormatProviderTest.groovy`

### Modified files
- `matrix-core/src/main/groovy/se/alipsa/matrix/core/Matrix.groovy` -- DONE (static read, instance write, listReadOptions, and listWriteOptions methods added)
- `matrix-avro/src/main/groovy/se/alipsa/matrix/avro/AvroReadOptions.groovy` -- DONE (describe/toMap/fromMap added)
- `matrix-avro/src/main/groovy/se/alipsa/matrix/avro/AvroWriteOptions.groovy` -- DONE (describe/toMap/fromMap added)
- 7 README files -- DONE

---

## Usage Examples (end result)

```groovy
// Simple read -- just have matrix-csv on classpath
def data = Matrix.read(new File('sales.csv'))

// Read with options
def data = Matrix.read(delimiter: ';', charset: 'ISO-8859-1', new File('data.csv'))

// Read spreadsheet
def data = Matrix.read(sheet: 'Q1 Sales', startRow: 2, new File('report.xlsx'))

// Read from URL
def data = Matrix.read(new URL('https://example.com/data.json'))

// Read from Path
def data = Matrix.read(Path.of('/data/archive.parquet'))

// Write
data.write(new File('output.json'))
data.write(indent: true, new File('output.json'))

// Discover available options
println Matrix.listReadOptions('csv')
println Matrix.listWriteOptions('csv')

// Discover available options (typed, per-module)
println CsvReadOptions.describe()

// List all registered formats
println FormatRegistry.instance.describe()
```

---

## Verification

After implementation, run:
```bash
# Build all to verify compilation
./gradlew build

# Run core SPI tests
./gradlew :matrix-core:test --tests "*FormatRegistryTest*"

# Run each format provider test
./gradlew :matrix-csv:test --tests "*CsvFormatProviderTest*"
./gradlew :matrix-json:test --tests "*JsonFormatProviderTest*"
./gradlew :matrix-spreadsheet:test --tests "*SpreadsheetFormatProviderTest*"
./gradlew :matrix-arff:test --tests "*ArffFormatProviderTest*"
./gradlew :matrix-avro:test --tests "*AvroFormatProviderTest*"
./gradlew :matrix-parquet:test --tests "*ParquetFormatProviderTest*"

# Full test suite to check for regressions
./gradlew test
```

---

## Key Implementation Notes

### API details for each provider delegation

**CSV:** `CsvReader.read(Map, File/URL/InputStream/Path)` already accepts string keys case-insensitively. For TSV auto-detection, check file extension and inject `Delimiter: '\t'` if not overridden. Write uses `CsvWriter.write(matrix)` fluent builder.

**JSON:** `JsonReader.read(File, Charset)`, `JsonReader.read(URL, Charset)`, `JsonReader.read(InputStream, Charset)`. `JsonWriter.write(matrix, File, indent)`, `JsonWriter.write(matrix, File, columnFormatters, indent, dateFormat)`.

**Spreadsheet:** `SpreadsheetImporter.importSpreadsheet(Map params)` accepts: file, sheet, startRow, endRow, startCol, endCol, firstRowAsColNames. `SpreadsheetWriter.write(matrix, file)` or `write(matrix, file, sheetName, startPosition)`. For read, need auto-detect endRow/endCol via SpreadsheetReader when not specified.

**ARFF:** `MatrixArffReader.read(File)`, `read(InputStream)`, `read(URL)`. `MatrixArffWriter.write(matrix, file)` or `write(matrix, file, nominalMappings)`.

**Avro:** `MatrixAvroReader.read(File, AvroReadOptions)`, `read(InputStream, AvroReadOptions)`, `read(URL, AvroReadOptions)`. `MatrixAvroWriter.write(matrix, file, AvroWriteOptions)`.

**Parquet:** `MatrixParquetReader.read(File)`, `read(File, String matrixName)`, `read(File, ZoneId)`. `MatrixParquetWriter.write(matrix, file, boolean inferPrecisionAndScale)`, `write(matrix, file, int precision, int scale)`, `write(matrix, file, Map<String, int[]> decimalMeta)`, `write(matrix, file, ZoneId)`.
