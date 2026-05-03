# Migration Guide for matrix-gsheets v0.1.0

This guide documents the breaking changes introduced in v0.1.0 and provides examples of how to update your code.

## Breaking Changes

### 1. GsUtil.deleteSheet() Return Type Change

**What Changed:**
The `deleteSheet()` method now returns `void` instead of `boolean` and throws `SheetOperationException` on failure.

**Before (v0.0.x):**
```groovy
boolean deleted = GsUtil.deleteSheet(spreadsheetId)
if (!deleted) {
  println "Delete failed"
}
```

**After (v0.1.0):**
```groovy
try {
  GsUtil.deleteSheet(spreadsheetId)
  println "Delete successful"
} catch (SheetOperationException e) {
  println "Delete failed: ${e.message}"
  println "Spreadsheet ID: ${e.spreadsheetId}"
  println "Operation: ${e.operation}"
}
```

**Rationale:** This change provides better error handling by:
- Making failures explicit through exceptions
- Providing detailed error context (spreadsheet ID, operation type, underlying cause)
- Following Java best practices for error reporting

---

## New Exception Types

### SheetOperationException

A new exception type has been introduced for Google Sheets operations that fail:

```groovy
class SheetOperationException extends RuntimeException {
  final String spreadsheetId  // The spreadsheet where the operation failed
  final String operation      // The operation type (e.g., "delete", "export", "write data")
}
```

**When is it thrown:**
- When `deleteSheet()` fails to delete a spreadsheet
- When `exportSheet()` fails to create or write to a spreadsheet

**Example Usage:**
```groovy
try {
  String id = GsExporter.exportSheet(matrix)
  println "Created spreadsheet: ${id}"
} catch (SheetOperationException e) {
  println "Failed to ${e.operation}: ${e.message}"
  if (e.spreadsheetId) {
    println "Spreadsheet ID: ${e.spreadsheetId}"
  }
}
```

---

## Improved Error Handling

### GsConverter Error Handling

Date/time conversion methods now provide better error messages and throw exceptions on failure:

**Before:**
```groovy
// Silent fallback on invalid input
def date = GsConverter.asLocalDate("invalid")  // Would attempt numeric conversion silently
```

**After:**
```groovy
// Clear error on invalid input
try {
  def date = GsConverter.asLocalDate("invalid")
} catch (IllegalArgumentException e) {
  println "Cannot convert 'invalid' to LocalDate: ${e.message}"
}
```

**New behavior:**
- Invalid date/time formats throw `IllegalArgumentException` with descriptive messages
- Warnings are logged when fallback conversions are attempted
- Null inputs consistently return null (documented behavior)

---

## Input Validation

All public methods now validate their parameters and throw `IllegalArgumentException` for invalid inputs:

### GsImporter
```groovy
// Throws IllegalArgumentException if sheetId or range is null/empty
GsImporter.importSheet(null, "Sheet1!A1:D10", true)  // ❌ IllegalArgumentException

// Throws IllegalArgumentException if range format is invalid
GsImporter.importSheet("id", "InvalidRange", true)   // ❌ IllegalArgumentException
```

### GsExporter
```groovy
// Throws IllegalArgumentException if matrix is null or has no columns/rows
GsExporter.exportSheet(null)                         // ❌ IllegalArgumentException
GsExporter.exportSheet(emptyMatrix)                  // ❌ IllegalArgumentException
```

### GsUtil
```groovy
// Throws IllegalArgumentException for invalid inputs
GsUtil.deleteSheet("")                               // ❌ IllegalArgumentException
GsUtil.columnCountForRange("A1")                     // ❌ IllegalArgumentException (missing colon)
GsUtil.asColumnNumber("123")                         // ❌ IllegalArgumentException (must be letters)
```

---

## Migration Checklist

When upgrading to v0.1.0, review your code for:

- [ ] **deleteSheet() calls** - Replace boolean checks with try/catch blocks
- [ ] **Error handling** - Add try/catch for SheetOperationException where needed
- [ ] **Null/empty parameter checks** - Remove redundant checks (now done by the library)
- [ ] **Date conversion** - Add try/catch for IllegalArgumentException on invalid inputs
- [ ] **Logging** - Update to handle new warning logs from GsConverter

---

## Benefits of v0.1.0

These breaking changes provide significant improvements:

1. **Better Error Reporting**: Exceptions include context about what failed and why
2. **Fail-Fast Behavior**: Invalid inputs are caught early with clear error messages
3. **Improved Debugging**: Detailed exception messages and logging help identify issues
4. **Consistent API**: All methods validate inputs and handle errors uniformly
5. **Production Ready**: 101 tests (up from 23) ensure reliability

---

## Need Help?

If you encounter issues migrating to v0.1.0:

1. Check this migration guide for examples
2. Review the comprehensive Javadoc documentation in the source code
3. Examine the test files for usage examples:
   - `GsImporterTest.groovy`
   - `GsExporterTest.groovy`
   - `GsConverterTest.groovy`
   - `GsUtilTest.groovy`
4. Report issues at: https://github.com/Alipsa/matrix/issues
