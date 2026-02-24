# Release history

## 0.1.1, 2026-01-31
Move actual implementation for GsheetsReader and GsheetsWriter and utility methods to GsUtil so that GsImporter and GsExporter are just empty wrappers.
- com.google.apis:google-api-services-drive v3-rev20251210-2.0.0 -> v3-rev20260128-2.0.0

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