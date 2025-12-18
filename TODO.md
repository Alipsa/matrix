# Matrix Library TODO

A review across the matrix monorepo revealed several improvement opportunities. Key findings are summarized below with direct references to relevant source files.

---

## High Priority (Bugs & Breaking Issues)

### Fix JsonImporter.jsonToMatrix mutation and iteration assumptions
**File:** `matrix-json/src/main/groovy/se/alipsa/groovy/matrixjson/JsonImporter.groovy:51-59`

The routine removes every element from the parsed root (`it.remove()`), which fails for unmodifiable collections, and calls `iterator()` without confirming the root is iterable (e.g., when the JSON represents a single object). Guarding against non-iterables and avoiding mutation of the parsed structure would make the importer more robust.

### Add row count validation to Matrix.addColumns
**File:** `matrix-core/src/main/groovy/se/alipsa/groovy/matrix/Matrix.groovy`

After the metadata check, the method blindly appends new columns without ensuring their lengths match the existing row count, risking skewed matrices and downstream `IndexOutOfBoundsExceptions`. Adding a length check before mutating the matrix would keep the structure consistent.

### Replace broad exception catches with specific types
**Files:**
- `matrix-bigquery/src/main/groovy/se/alipsa/groovy/matrix/bigquery/Bq.groovy`
- `matrix-charts/src/main/groovy/se/alipsa/groovy/matrix/chart/png/PngConverter.groovy`
- `matrix-core/src/main/groovy/se/alipsa/groovy/matrix/ListConverter.groovy`
- `matrix-sql/src/main/groovy/se/alipsa/groovy/matrixsql/config/JaasConfigLoader.groovy`
- `matrix-sql/src/main/groovy/se/alipsa/groovy/matrixsql/MatrixSqlFactory.groovy`
- `matrix-gsheets/src/main/groovy/se/alipsa/groovy/matrixgsheets/GsConverter.groovy`
- `matrix-gsheets/src/main/groovy/se/alipsa/groovy/matrixgsheets/BqAuthenticator.groovy`

Overly broad `catch(Exception)` or `catch(Throwable)` blocks can hide bugs and security issues. Catch specific exception types and handle appropriately.

### Remove debug println statements from production code
**Files:**
- `matrix-core/src/main/groovy/se/alipsa/groovy/matrix/MatrixBuilder.groovy`
- `matrix-core/src/main/groovy/se/alipsa/groovy/matrix/Matrix.groovy`
- `matrix-core/src/main/groovy/se/alipsa/groovy/matrix/ValueConverter.groovy`
- `matrix-core/src/main/groovy/se/alipsa/groovy/matrix/RowExtension.groovy`
- `matrix-core/src/main/groovy/se/alipsa/groovy/matrix/Stat.groovy`
- `matrix-core/src/main/groovy/se/alipsa/groovy/matrix/util/ClassUtils.groovy`

Replace `println` and `System.out` statements with proper logging via Log4j (already configured in project).

### Document or fix Row iterator mutation behavior
**File:** `matrix-core/src/main/groovy/se/alipsa/groovy/matrix/Row.groovy:81`

TODO comment: "how do we handle modifications?" - The iterator implementation returns `content.iterator()` without handling external mutations. Document the expected behavior or implement safe iteration patterns.

---

## Medium Priority (Performance & Code Quality)

### Refactor Joiner.merge() algorithm
**File:** `matrix-core/src/main/groovy/se/alipsa/groovy/matrix/Joiner.groovy:14`

TODO comment: "investigate if it is possible to use GINQ for this" - The current merge operation is self-described as "embarrassingly crude" with O(n²) complexity. Performance is terrible for large matrices. Refactor using Groovy Integrated Queries (GINQ) or indexed lookup approach.

### Reduce Matrix class size
**File:** `matrix-core/src/main/groovy/se/alipsa/groovy/matrix/Matrix.groovy` (2,814 lines)

The Matrix class is very big. Consider extracting functionality into utilities or helper classes, such as:
- Matrix transformation operations (separate class)
- Matrix I/O operations (separate class)
- Matrix statistics operations (leverage matrix-stats more)

Target: reduce to <1,500 lines.

### Extract spreadsheet header building logic (DRY)
**Files:**
- `matrix-spreadsheet/src/main/groovy/se/alipsa/groovy/spreadsheet/ExcelImporter.groovy:128,166,207`
- `matrix-spreadsheet/src/main/groovy/se/alipsa/groovy/spreadsheet/FExcelImporter.groovy:264`

TODO comment: "refactor build header to either use first row or create column names" - Same logic duplicated in 3+ locations. Extract to shared helper method.

### Optimize FOdsImporter sheet parsing
**File:** `matrix-spreadsheet/src/main/groovy/se/alipsa/groovy/spreadsheet/FOdsImporter.groovy:258`

Comment: "There is room for optimization here" - Currently parses entire sheets repeatedly for each sheet definition. Avoid redundant full parses.

### Optimize KMeansPlusPlus algorithm
**File:** `matrix-stats/src/main/groovy/se/alipsa/groovy/stats/cluster/KMeansPlusPlus.groovy:531`

TODO comment: "review and try to optimize" - Performance improvement opportunity. The existing performance test may fail on slower machines.

---

## Medium Priority (Dependencies)

### Plan migration away from Apache Commons Math 3.6.1
**Files:**
- `matrix-stats/src/main/groovy/se/alipsa/groovy/stats/Student.groovy:129,160` - TODO: "implement p value function"
- `matrix-stats/src/main/groovy/se/alipsa/groovy/stats/Anova.groovy:36` - TODO: "implement me! using commons math for now..."

Commons Math 3.6.1 was released in 2016 and is no longer actively maintained. Current TODOs indicate intent to implement native functions. Plan to replace Commons Math with native implementations or alternative libraries such as Apache Commons Statistics if implementation is not feasible.

### Document Java version constraints
The project has a JDK 21 ceiling due to dependency constraints:
- matrix-parquet & matrix-avro: Cannot use JDK > 21 (Hadoop dependency)
- Smile library: Cannot upgrade to v5+ (requires Java 25)
- JavaFX: Cannot upgrade to v24+ (Java 21 limit)

Document these constraints in main readme.md for contributor awareness.

---

## Low Priority (Documentation)

### Add README and documentation for matrix-smile
**Directory:** `matrix-smile/`

New module lacks README and documentation. Document:
- Purpose and features
- Smile library integration details
- Usage examples
- Known limitations

### Create architecture/design documentation
Add high-level architecture documentation:
- Module dependency diagram
- Core class relationships
- Extension points for contributors
- Design decisions and rationale

### Document experimental modules
Set expectations for modules marked as experimental:
- matrix-charts (including `matrix-charts/todo.md` reference to chart libraries)
- matrix-tablesaw
- matrix-avro

### Extend tutorial coverage
**Directory:** `docs/tutorial/`

Add tutorial sections for:
- matrix-smile (once stable)
- Advanced matrix operations
- Performance best practices

### Complete or remove matrix-ai-* placeholder modules
**Directories:**
- `matrix-ai-ga/`
- `matrix-ai-ml/`
- `matrix-ai-nn/`

These modules have build.gradle files but no implementation. Either implement or remove from repository.

---

## Low Priority (Performance Optimization)

### Implement caching for column lookups
**File:** `matrix-core/src/main/groovy/se/alipsa/groovy/matrix/Matrix.groovy`

Column lookups by name are repeated frequently. Consider caching column indices for performance improvement on large matrices.

### Optimize slow spreadsheet tests
Tests tagged with `@Tag("slow")` are excluded from regular runs:
- `SpreadsheetReaderTest`
- `LargeFileImportTest`

Consider parallelization or chunking strategies to include in regular test runs.

---

## Low Priority (Nice to Have)

### Improve date handling in matrix-charts
**File:** `matrix-charts/src/main/groovy/se/alipsa/groovy/matrix/chart/DataType.groovy:12`

TODO comment: "maybe do something with dates" - Date type handling is incomplete.

### Clarify groupCol behavior in AreaChart
**File:** `matrix-charts/src/main/groovy/se/alipsa/groovy/matrix/chart/AreaChart.groovy:41`

TODO comment: "figure out how groupCol works" - Grouping behavior is unclear.

### Make tick label rotation a styling option
**File:** `matrix-charts/src/main/groovy/se/alipsa/groovy/matrix/chart/JfxBarChartConverter.groovy:17,26`

TODO comment: "make this a styling option" - Currently hardcoded.

### Use CorrelationHeatmapChart in whiskey example
**File:** `matrix-examples/whiskey/src/main/groovy/WhiskeyAnalysis.groovy:87`

TODO comment: "use a CorrelationHeatmapChart instead of a homegrown HeatmapChart"

### Complete HousePrices example
**File:** `matrix-examples/HousePrices/Explore.groovy:50`

TODO comment: "continue matrix conversion here" - Example is incomplete.

---

## Chart Libraries to Investigate (matrix-charts)

### JavaFX chart libraries
- [chartfx](https://github.com/fair-acc/chart-fx) - Nice scientific charts
- [Han Solo Charts](https://github.com/HanSolo/charts) - Beautiful charts not in standard JavaFX
- [PureSol Charts](https://github.com/PureSolTechnologies/javafx/tree/master/charts) - BoxChart, OHLC chart, timeseries

### Swing chart libraries
- [JFree Charts](https://www.jfree.org/jfreechart/) - Comprehensive chart library

---

## Statistics Summary

- **Total lines of code:** ~75,450
- **Number of modules:** 20+
- **Test files:** 111
- **Largest class:** Matrix.groovy (2,814 lines)
- **TODO/FIXME comments found:** 15+
- **Modules marked experimental:** 4
- **Incomplete placeholder modules:** 3 (matrix-ai-*)
