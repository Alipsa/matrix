# Release history

## 0.2.1, in progress

### Bug Fixes
- Fix `GsConverter.toSerials(List<Object>)` throwing for `java.util.Date` values by dispatching `Date` through `GsConverter.asSerial(Object)`
- Fix `GsUtil.columnCountForRange` rejecting single-cell A1 ranges such as `Sheet1!A1`
- Fix `GsheetsReader.readAsObject`/`readAsStrings` throwing `NullPointerException` on genuinely empty ranges; they now return an empty `Matrix` with generated headers
- Fix interactive login relying on gcloud's shared default OAuth client, which Google now blocks for sensitive scopes (spreadsheets, drive); `matrix-gsheets` now bundles its own OAuth client, injected at build time (see `docs/OAUTH-CLIENT.md`)
- Fix `GsAuthenticator.authenticate()` permanently disabling further login attempts after one failed/cancelled login, and concurrent callers failing instead of waiting for an in-flight login
- Fix `GsAuthenticator.authenticate()` returning credentials without verifying the granted token actually has the requested scopes
- Fix `GsheetsWriter.write()` building an unquoted A1 range for sheet names containing spaces or special characters (e.g. `Employee Data` needs `'Employee Data'!A1`)
- Fix `GsheetsWriter.write()`/`readAsStrings` round-trip losing trailing zeros on whole-number `BigDecimal` values (e.g. `729.0` read back as `729`) by forcing an explicit decimal-place number format on write; `update()` now applies the same fix
- `GsUtil.toCell` now rejects `BigDecimal` values with more significant digits than a double can represent exactly, instead of silently losing precision

### Dependency updates
- com.google.apis:google-api-services-drive v3-rev20260428-2.0.0 -> v3-rev20260624-2.0.0
- com.google.apis:google-api-services-sheets v4-rev20260213-2.0.0 -> v4-rev20260610-2.0.0
- com.google.auth:google-auth-library-bom 1.46.0 -> 1.48.0
- com.google.auth:google-auth-library-oauth2-http 1.46.0 -> 1.48.0
- com.google.http-client:google-http-client-gson 2.1.0 -> 2.1.1

## 0.2.0, 2026-05-03

### Breaking Changes
- **Renamed authentication classes**: `BqAuthenticator` → `GsAuthenticator`, `BqAuthUtils` → `GsAuthUtils`
  - See `docs/0.1-0.2-MIGRATION.md` for detailed migration guide with before/after examples

### New Features
- **`GsheetsWriter.update(String, String, Matrix, ...)`**: Write Matrix data to an existing spreadsheet and range
- **`GsheetsWriter.spreadsheetUrl(String)`**: Convenience helper that returns the Google Sheets edit URL for a spreadsheet ID

### Improvements
- **Eliminated duplicated service-setup code**: Extracted `buildSheetsService()` helpers in both `GsheetsReader` and `GsheetsWriter`
- **Eliminated duplicated utilities in `GsheetsWriter`**: Removed `sanitizeSheetName()`, `toCell()`, and `MAX_SHEET_NAME_LENGTH` — now delegates to `GsUtil`
- **Fixed sheet-name extraction**: When a range has no `!` prefix (e.g., `A1:D10`), the matrix name defaults to `''` instead of the raw range string
- **Narrowed exception handling**: `GsUtil.getSheetNames(String, Sheets)` now catches `IOException` specifically and declares `throws IOException`
- **Improved numeric precision**: `GsConverter.asLocalDate(Number)` uses `longValue()` instead of `intValue()` for day counts
- **Idiomatic Groovy cleanup**: Removed ~20 unnecessary `return` keywords from final expressions; replaced Java-style `new ArrayList<>()` + `.add()` with `[]` literals and `<<` / `collect`
- **Code quality**: Removed commented-out tokeninfo URL from `GsAuthUtils`; converted inline usage docs to proper GroovyDoc
- **Fixed `GsAuthenticator` log indentation**: `log.info` call is now correctly inside the `if (verbose)` block
- **Fixed static field ordering**: Moved `PROP_USER_HOME` above `ADC_FILE_PATH` to eliminate initialization-order hazard
- **Added missing return type declarations**: `GsConverter.asLocalTime(Object)` → `LocalTime`, `GsConverter.asSerial(Date)` → `BigDecimal`
- **Added historical-naming GroovyDoc notes** to `GsAuthenticator` and `GsAuthUtils` explaining the `Bq` prefix

### Test Changes
- **Added 5 positive-case tests** for `GsConverter.toSerials` (LocalDates, LocalDateTimes, LocalTimes, mixed types, empty list)
- **Added 5 validation tests** for `GsheetsWriter.update()` (null spreadsheetId, null range, null matrix, empty matrix, no rows)
- **Moved 16 tests** (`sanitizeSheetName` and `toCell` coverage) from `GsExporterTest` to `GsUtilTest` where they belong
- **Removed brittle reflection-based tests** from `GsheetsReaderTest` and `GsheetsWriterTest`

### Dependency Updates
- com.google.apis:google-api-services-drive v3-rev20260220-2.0.0 → v3-rev20260322-2.0.0
- com.google.apis:google-api-services-sheets v4-rev20251110-2.0.0 → v4-rev20260213-2.0.0
- org.mockito:mockito-core 5.22.0 → 5.23.0
- org.mockito:mockito-junit-jupiter 5.22.0 → 5.23.0
- se.alipsa.nexus-release-plugin:se.alipsa.nexus-release-plugin.gradle.plugin 2.1.1 → 2.1.2 

## 0.1.1, 2026-01-31
Move actual implementation for GsheetsReader and GsheetsWriter and utility methods to GsUtil so that GsImporter and GsExporter are just empty wrappers.
- com.google.apis:google-api-services-drive v3-rev20251210-2.0.0 -> v3-rev20260220-2.0.0
- com.google.api-client:google-api-client 2.8.1 -> 2.9.0

## 0.1.0, 2026-01-31

### Major Improvements
- **Production Readiness**: Comprehensive error handling, input validation, and 101 passing tests (up from 23)
- **Better Error Handling**: New `SheetOperationException` with detailed context; no more silent failures
- **Enhanced Documentation**: Complete Javadoc with usage examples, Google Sheets quirks documented
- **Test Coverage**: 439% increase (23 → 101 tests) covering error cases, utilities, and authentication

### Breaking Changes
- **deleteSheet() API**: Now returns `void` and throws `SheetOperationException` on failure (previously returned `boolean`)
- See `MIGRATION.md` for detailed migration guide with examples

### New Features
- Add method to get sheet names from spreadsheets
- Comprehensive input validation on all public methods with actionable error messages

### Bug Fixes
- Fix silent exception handling in `GsConverter` (now logs warnings and throws on unrecoverable failures)
- Fix import to handle Google's "incomplete rows" and adhere to the range that was defined
- Add proper null handling with documented behavior

### Dependency Updates
- com.google.api-client:google-api-client [2.6.0 -> 2.8.1]
- com.google.apis:google-api-services-drive [v3-rev20220815-2.0.0 -> v3-rev20251210-2.0.0]
- com.google.apis:google-api-services-sheets [v4-rev20220927-2.0.0 -> v4-rev20251110-2.0.0]
- com.google.auth:google-auth-library-oauth2-http [1.38.0 -> 1.41.0]
- com.google.http-client:google-http-client-gson [1.43.3 -> 2.0.3]
- com.google.oauth-client:google-oauth-client-jetty [1.36.0 -> 1.39.0]

## 0.0.1, 2025-09-06
- initial version