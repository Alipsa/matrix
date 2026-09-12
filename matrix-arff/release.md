# Matrix-arff release history

## v0.3.0, in progress
Weka compatibility release. Files written by matrix-arff now interoperate with Weka across the documented supported
behavior and limitations.
Behaviour changes are marked **(changed)**.
- Add `ArffEscapes` and align writer quoting with Weka's `Utils.quote`: tabs, newlines, carriage returns, `"`, `\` and
  `%` anywhere in a value or identifier are now escaped (`\t`, `\n`, `\r`, `\"`, `\\`, `\%`) and quoted **(changed:**
  such values were previously written raw, producing files Weka could not read**)**
- Reader decodes Weka escapes inside quoted tokens: `\n`, `\t`, `\r` become the control character and `\"`, `\%`, `\\`,
  `\'` the literal character, in relation names, attribute names, nominal declarations, dense and sparse values
  **(changed:** `\n` previously decoded to the letter `n`**)**
- Fix `@RELATION '` (lone quote) throwing `StringIndexOutOfBoundsException`; it is now an `IllegalArgumentException`
  with line context
- Fix DATE attribute formats containing escaped quotes (`date 'yyyy-MM-dd\'T\'HH:mm:ss'`, the form Weka and this
  module write for the default pattern) failing to parse; a Date column written with default options now round-trips
- Accept unquoted DATE formats (`date yyyy-MM-dd`) as Weka does; trailing text after the format is now a parse error
- `DATETIME` and other types merely starting with `date` are no longer treated as DATE
- `%` outside a quoted token starts a comment anywhere on a line, as in Weka **(changed:** previously only a `%` at the
  start of a line was a comment; an unquoted value such as `50%` now reads as `50`. Since 0.3.0 the writer quotes and
  escapes every value containing `%` (before, only nominal values *starting* with `%` were quoted)**)**
- Sparse rows follow the ARFF specification as implemented by Weka: an omitted attribute is `0` (NUMERIC/INTEGER `0`,
  first declared nominal value, epoch DATE, and for STRING the first explicit value in that column — Weka's dictionary
  index 0 — or `null` when there is none); only an explicit `?` is missing **(changed:** omitted attributes were
  previously `null`**)**
- Add `ArffReadOptions.omittedStringFallback(value)`: the value of a sparse-omitted STRING cell whose column has no
  explicit value (`'0'` reproduces Weka's raw value and liac-arff's string)
- Fix `LocalDate` and `LocalDateTime` values being shifted by the JVM's zone offset when written (regression from the
  UTC formatter introduced in 0.2.1)
- Write `NaN` and infinite `Double`/`Float` values as `?` (missing) instead of literals Weka and the reader reject
- Reject DATE values with trailing text (`'2026-03-18garbage'`); `SimpleDateFormat.parse` silently ignored it
- Create one date formatter per DATE attribute instead of one per cell when reading and writing; add
  `ArffDateFormats.DEFAULT_PATTERN`

## v0.2.1 - 2026-04-30
- Fix nominal sentinel values (`?`, empty string, `%`-prefixed) being written unquoted, causing lossy ARFF round-trips
- Add `ArffDateFormats` utility to share strict (`lenient=false`), UTC, `Locale.ROOT` date formatter creation between reader and writer
- Fix date parsing to reject invalid dates (e.g. `2026-02-31`) instead of silently normalizing them
- Fix published SCM URL in POM (`matrix-arff/tree/master` → `tree/main/matrix-arff`)
- Apply module-wide `@CompileStatic` via `compileStatic.groovy` build configuration
- Add regression tests for nominal sentinel round-trips, invalid date rejection, and UTC date output
- Fixed all codenarc warnings and change the build to fail on any new warnings.

## v0.2.0 - 2026-03-18
- add sparse ARFF data row read support with validation for duplicate and out-of-range attribute indices
- add `ArffFormatProvider` and service registration so `.arff` files work with the generic `Matrix.read(...)` / `matrix.write(...)` SPI API
- expand `ArffReadOptions` with `fallbackMatrixName` support, strict validation toggles, and richer parser error messages with line context
- expand `ArffWriteOptions` with configurable schema generation, including nominal inference controls, forced per-column ARFF types, and global/per-column DATE formats
- add typed options-first direct API overloads for ARFF reads and writes, and align the SPI provider with those typed paths
- refactor `MatrixArffWriter` so direct API and SPI writes share one typed schema-resolution path
- document the current ARFF API surface and defaults in the README and tutorial, including sparse input, strict mode, explicit schema control, and SPI round-tripping examples

## v0.1.0 - 2026-01-30
Initial release
Support for reading arff files into a matrix and writing a matrix to an arff file
