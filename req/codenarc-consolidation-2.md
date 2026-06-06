# Remove CodeNarc `ignoreFailures` Exceptions Implementation Plan

> Steps use checkbox (`- [ ]`) syntax for tracking. Mark complete with `- [x]` when done. Always read the XML reports for accurate violation data; do not rely on stdout patterns.

**Goal:** Remove all `ignoreFailures = true` CodeNarc overrides from 5 modules by fixing the underlying violations, and also strip now-redundant `@SuppressWarnings` annotations whose rules are globally excluded in the consolidated root ruleset.

**Architecture:** The root ruleset at `config/codenarc/ruleset.groovy` globally excludes `Instanceof`, `NestedForLoop`, `CloseWithoutCloseable`, and several other rules. Five modules still have build-level `ignoreFailures = true` overrides added as technical debt. This plan fixes the violations so the overrides can be removed, and cleans up redundant suppressions made obsolete by the recent ruleset consolidation.

**Tech Stack:** Groovy 5.0.6, CodeNarc 3.7, Gradle multi-module build

**Audit note:** CodeNarc writes its source-of-truth violation data to `$module/build/reports/codenarc/main.xml` and `test.xml` even when `ignoreFailures = true`. Always read the XML reports to audit violations — do not rely on stdout/stderr grep patterns. The audit snippets below use stdlib `xml.etree.ElementTree`; the XML files are locally generated build artifacts (no external entities) so XXE exposure is not a concern here.

---

## Scope — actual violation counts from current reports (Jun 2026)

| Module | Scope | Violations | Top violation types |
|---|---|---|---|
| `matrix-parquet` | test | 107 | UnnecessaryGString (107) |
| `matrix-json` | test | 170 | UnnecessaryGString (170) |
| `matrix-csv` | test | 325 | UnnecessaryGString (320), UnnecessaryCallForLastElement (4), UnnecessaryGroovyImport (1) |
| `matrix-stats` | main | 615 | UnnecessaryGString (430), UnnecessaryObjectReferences (46), DuplicateStringLiteral (46), UnnecessaryElseStatement (39), UnnecessaryCollectCall (30), DuplicateNumberLiteral (22), DuplicateListLiteral (2) |
| `matrix-stats` | test | 394 | UnnecessaryGString (380), UnnecessaryCollectCall (14) |
| `matrix-ggplot` | main | 2370 | IfStatementBraces (943), DuplicateStringLiteral (638), DuplicateNumberLiteral (343), UnnecessaryObjectReferences (78), UnnecessaryCast (76), UnnecessaryGString (75), DuplicateMapLiteral (47), UnusedMethodParameter (21), UnusedImport (18), UnnecessaryElseStatement (10), ReturnsNullInsteadOfEmptyCollection (10), GetterMethodCouldBeProperty (9), PropertyName (9), ClassJavadoc (9), MethodSize (8) |
| `matrix-ggplot` | test | 913 | UnnecessaryGString (646), UnnecessaryCast (76), ClosureAsLastMethodParameter (45), UnnecessaryDotClass (30), UnnecessaryBigDecimalInstantiation (15), UnusedMethodParameter (15), UnnecessarySelfAssignment (12), UnusedImport (12), ExplicitCallToCompareToMethod (10), UnnecessaryPackageReference (10), AssignCollectionSort (6), UnnecessaryCollectCall (5), UnnecessaryToString (5), IfStatementBraces (4), ExplicitCallToPlusMethod (4) |

---

## Strategy for violation categories

| Violation | Fix approach |
|---|---|
| **UnnecessaryGString** | Replace `"literal"` with `'literal'` when no `${...}` interpolation is present |
| **UnnecessaryObjectReferences** | Use a `with()` block to consolidate repeated property assignments or method calls on the same object: `obj.a = x; obj.b = y` → `obj.with { a = x; b = y }` |
| **UnnecessaryElseStatement** | Remove `else` branch when the preceding `if` block always returns |
| **UnnecessaryCollectCall** | Remove no-op `.collect { it }` calls |
| **UnnecessaryCast** | Remove explicit casts that are unnecessary under `@CompileStatic` flow typing |
| **UnnecessaryDotClass** | Use `SomeClass` instead of `SomeClass.class` in non-Java contexts |
| **UnnecessarySelfAssignment** | Remove `a = a` |
| **UnnecessaryToString** | Remove `.toString()` calls where Groovy coerces automatically |
| **UnnecessaryPackageReference** | Use simple class name (already imported) instead of fully qualified name |
| **UnnecessaryBigDecimalInstantiation** | Use `1.0` literal instead of `new BigDecimal("1.0")` |
| **UnnecessaryCallForLastElement** | Use `list.last()` instead of `list[list.size()-1]` |
| **UnnecessaryGroovyImport** | Remove explicit imports of `java.lang.*` or `groovy.lang.*` classes |
| **DuplicateNumberLiteral** | Extract to a `private static final` constant with a descriptive name. Statistical p-values and thresholds have meaningful names (`CONFIDENCE_95_Z = 1.96`, `DEFAULT_TOLERANCE = 0.001`). Use a shared constants class when the same value appears across files. |
| **DuplicateStringLiteral** | Extract to a `private static final String` constant. Even short strings like `'none'` or `'auto'` have a semantic role and benefit from a named constant (`NONE = 'none'`). |
| **DuplicateMapLiteral** | Extract to a `static final` constant or helper method |
| **IfStatementBraces** | Add `{ }` around every single-statement if/else/for body |
| **ClosureAsLastMethodParameter** | Move trailing closure argument outside parentheses: `foo(a, { ... })` → `foo(a) { ... }` |
| **AssignCollectionSort** | Replace `list = list.sort()` with `list.sort()` (in-place) or `list = list.toSorted()` (non-destructive) |
| **ExplicitCallToCompareToMethod** | Use `<=>` spaceship operator instead of `.compareTo()` |
| **ExplicitCallToPlusMethod** | Use `a + b` instead of `a.plus(b)` |
| **UnusedMethodParameter** | Either use the parameter, remove it (if the method signature allows), or prefix with `_` if required by an interface/override |
| **UnusedImport** | Remove the unused import statement |
| **ReturnsNullInsteadOfEmptyCollection** | **Verify semantics first.** If `null` is an intentional sentinel (e.g., "use default breaks" in coord/scale code), add `@SuppressWarnings('ReturnsNullInsteadOfEmptyCollection')` at the method level and keep the null. Only replace with `[]`/`[:]` when null genuinely means "empty result" with no downstream semantic difference. |
| **GetterMethodCouldBeProperty** | Replace `T getSomething() { something }` with Groovy property access pattern |
| **PropertyName** | Fix naming convention violations (camelCase for instance fields, UPPER_SNAKE for static finals) |
| **ClassJavadoc** | Add a brief GroovyDoc comment before the class declaration |
| **MethodSize** | Split into private helper methods by identifying distinct phases (data preparation, computation, output). Every method over 100 lines has separable concerns — look for them. Do not add `@SuppressWarnings` and do not change the global threshold. |

---

## Task 1: Fix matrix-parquet test violations (107 × UnnecessaryGString)

**Files to modify:**
- `matrix-parquet/build.gradle` (remove lines 57–60)
- `matrix-parquet/src/test/groovy/ParquetFormatProviderTest.groovy`
- `matrix-parquet/src/test/groovy/MatrixParquetTest.groovy`

- [x] **1.1 Audit violations from the XML report**

  ```bash
  # Generate fresh report (ignoreFailures is still true so the task succeeds)
  ./gradlew :matrix-parquet:codenarcTest

  python3 -c "
  import xml.etree.ElementTree as ET
  from collections import Counter
  tree = ET.parse('matrix-parquet/build/reports/codenarc/test.xml')
  vs = tree.getroot().findall('.//Violation')
  for r,c in Counter(v.get('ruleName') for v in vs).most_common():
      print(f'{c:4d}  {r}')
  for v in vs:
      src = v.findtext('SourceLine', '').strip()
      msg = v.findtext('Message', '')
      print(f'  line {v.get(\"lineNumber\")}: {src}  — {msg}')
  "
  ```

- [x] **1.2 Fix all UnnecessaryGString violations**

  In both test files, replace every `"string literal"` that contains no `${...}` interpolation with a single-quoted `'string literal'`. Example:
  ```groovy
  // Before
  def path = "src/test/resources/data.parquet"
  // After
  def path = 'src/test/resources/data.parquet'
  ```

- [x] **1.3 Remove the codenarcTest override from matrix-parquet/build.gradle**

  Delete lines 57–60:
  ```groovy
  codenarcTest {
      // TODO: Fix UnnecessaryGString violations in test sources and remove this override
      ignoreFailures = true
  }
  ```

- [x] **1.4 Verify**

  ```bash
  ./gradlew :matrix-parquet:codenarcTest
  ./gradlew :matrix-parquet:test
  ```
  Expected: both pass with 0 violations.

  Completed verification:
  ```bash
  ./gradlew :matrix-parquet:codenarcTest
  ./gradlew :matrix-parquet:codenarcMain
  ./gradlew :matrix-parquet:spotlessCheck
  ./gradlew :matrix-parquet:test
  ```

- [ ] **1.5 Commit (after user confirmation)**

  ```bash
  git add matrix-parquet/build.gradle matrix-parquet/src/test/groovy/
  git commit -m "Fix UnnecessaryGString in matrix-parquet tests, remove ignoreFailures"
  ```

---

## Task 2: Fix matrix-json test violations (170 × UnnecessaryGString)

**Files to modify:**
- `matrix-json/build.gradle` (remove lines 47–50)
- `matrix-json/src/test/groovy/` (6 test files)

- [ ] **2.1 Audit violations from the XML report**

  ```bash
  ./gradlew :matrix-json:codenarcTest

  python3 -c "
  import xml.etree.ElementTree as ET
  from collections import Counter
  tree = ET.parse('matrix-json/build/reports/codenarc/test.xml')
  vs = tree.getroot().findall('.//Violation')
  for r,c in Counter(v.get('ruleName') for v in vs).most_common():
      print(f'{c:4d}  {r}')
  "
  ```

- [ ] **2.2 Fix UnnecessaryGString violations**

  Replace uninterpolated `"string"` → `'string'` in all 6 test files:
  - `JsonWriterTest.groovy`, `JsonFormatProviderTest.groovy`, `JsonImporterTest.groovy`
  - `DuplicateKeyTest.groovy`, `JsonExporterTest.groovy`, `JsonReaderTest.groovy`

- [ ] **2.3 Remove the codenarcTest override from matrix-json/build.gradle**

  Delete lines 47–50:
  ```groovy
  codenarcTest {
    // TODO: Fix UnnecessaryGString violations in test sources and remove this override
    ignoreFailures = true
  }
  ```

- [ ] **2.4 Verify**

  ```bash
  ./gradlew :matrix-json:codenarcTest
  ./gradlew :matrix-json:test
  ```

- [ ] **2.5 Commit (after user confirmation)**

  ```bash
  git add matrix-json/build.gradle matrix-json/src/test/groovy/
  git commit -m "Fix UnnecessaryGString in matrix-json tests, remove ignoreFailures"
  ```

---

## Task 3: Fix matrix-csv test violations (325 violations)

**Files to modify:**
- `matrix-csv/build.gradle` (remove the `codenarcTest { ignoreFailures = true }` block near line 49)
- `matrix-csv/src/test/groovy/` (7 test files + 1 internal)

- [ ] **3.1 Audit violations from the XML report**

  ```bash
  ./gradlew :matrix-csv:codenarcTest

  python3 -c "
  import xml.etree.ElementTree as ET
  from collections import Counter
  tree = ET.parse('matrix-csv/build/reports/codenarc/test.xml')
  vs = tree.getroot().findall('.//Violation')
  for r,c in Counter(v.get('ruleName') for v in vs).most_common():
      print(f'{c:4d}  {r}')
  "
  ```

- [ ] **3.2 Fix UnnecessaryGString violations (320)**

  Replace uninterpolated `"string"` → `'string'` in all test files.

- [ ] **3.3 Fix UnnecessaryCallForLastElement violations (4)**

  Replace `list[list.size() - 1]` with `list.last()`.

- [ ] **3.4 Fix UnnecessaryGroovyImport violation (1)**

  Remove the explicit import of a `java.lang.*` or `groovy.lang.*` class (e.g., `import java.lang.String`).

- [ ] **3.5 Remove the codenarcTest override**

  In `matrix-csv/build.gradle`, delete the `codenarcTest { ignoreFailures = true }` block (near line 49).

- [ ] **3.6 Verify**

  ```bash
  ./gradlew :matrix-csv:codenarcTest
  ./gradlew :matrix-csv:test
  ```

- [ ] **3.7 Commit (after user confirmation)**

  ```bash
  git add matrix-csv/build.gradle matrix-csv/src/test/groovy/
  git commit -m "Fix CodeNarc violations in matrix-csv tests, remove ignoreFailures"
  ```

---

## Task 4: Fix matrix-stats violations (1009 violations across main + test)

matrix-stats has `ignoreFailures = true` on the entire `codenarc` block. The violations are dominated by `UnnecessaryGString` (810 total) with a handful of structural issues.

**Files to modify:**
- `matrix-stats/build.gradle` (remove lines 16–19)
- `matrix-stats/src/main/groovy/**/*.groovy` (fix violations per category)
- `matrix-stats/src/test/groovy/**/*.groovy` (fix violations per category)

### 4.1 Audit

- [ ] **4.1.1 Generate fresh reports and read them**

  ```bash
  # Generate reports (ignoreFailures is still true so both tasks succeed)
  ./gradlew :matrix-stats:codenarcMain :matrix-stats:codenarcTest

  python3 -c "
  import xml.etree.ElementTree as ET
  from collections import Counter

  for scope in ['main', 'test']:
      tree = ET.parse(f'matrix-stats/build/reports/codenarc/{scope}.xml')
      vs = tree.getroot().findall('.//Violation')
      print(f'\n=== {scope}: {len(vs)} violations ===')
      for r,c in Counter(v.get('ruleName') for v in vs).most_common():
          print(f'  {c:4d}  {r}')
  "
  ```

  Cross-reference against the counts in the scope table above to confirm alignment; discrepancies indicate the source changed since this plan was written.

### 4.2 Fix UnnecessaryGString (430 main + 380 test)

- [ ] **4.2.1 Fix in main sources**

  For each file, replace uninterpolated `"string"` → `'string'`. Use the HTML report (`matrix-stats/build/reports/codenarc/main.html`) to navigate to exact line numbers.

- [ ] **4.2.2 Fix in test sources**

  Same mechanical change across all test files.

### 4.3 Fix UnnecessaryObjectReferences (46 main)

- [ ] **4.3.1 Consolidate repeated property assignments using `with()`**

  CodeNarc fires when consecutive statements call methods or set properties on the same object. Refactor to a `with()` block:
  ```groovy
  // Before — repeated 'obj.' references
  obj.foo = value1
  obj.bar = value2
  obj.baz = value3

  // After — with() block
  obj.with {
    foo = value1
    bar = value2
    baz = value3
  }
  ```
  For `rowMap.put(...)` chains, the same pattern applies:
  ```groovy
  // Before
  rowMap.put('__sf_id', idx)
  rowMap.put('__sf_part', partIndex)

  // After
  rowMap.with {
    put('__sf_id', idx)
    put('__sf_part', partIndex)
  }
  ```

  **Scope warning:** Inside a `with()` closure the delegate is the target object, so bare names — on both sides of an assignment — resolve against the delegate first. An unqualified source read like `copyList(items)` reads `delegate.items`, not the enclosing object's `items`. Qualify every access to the enclosing object with `this.`:
  ```groovy
  // Unsafe — 'items' on the right resolves against delegate, not the source
  obj.with {
    items = this.copyList(items)        // BUG: reads delegate.items
  }

  // Safe — both sides qualified
  obj.with {
    items = this.copyList(this.items)   // reads source, writes delegate
  }
  ```
  Alternatively, capture source values in locals before the block to avoid any ambiguity:
  ```groovy
  def srcItems = items
  obj.with {
    items = copyList(srcItems)
  }
  ```
  Always run `./gradlew :<module>:compileGroovy && ./gradlew :<module>:test` after converting each cluster.

### 4.4 Fix DuplicateStringLiteral (46 main)

- [ ] **4.4.1 Extract repeated string labels to `static final` constants**

  For each file, identify which string is repeated and extract to a constant at the top of the class:
  ```groovy
  private static final String COLUMN_LABEL = 'value'
  ```
  Even short or generic strings repeated in test data tables have a role and should be named.

### 4.5 Fix UnnecessaryElseStatement (39 main)

- [ ] **4.5.1 Remove unnecessary else branches**

  ```groovy
  // Before
  if (condition) {
    return earlyValue
  } else {
    doSomething()
  }
  // After
  if (condition) {
    return earlyValue
  }
  doSomething()
  ```

### 4.6 Fix UnnecessaryCollectCall (30 main + 14 test)

- [ ] **4.6.1 Remove no-op collect calls**

  ```groovy
  // Before
  list.collect { it }.each { ... }
  // After
  list.each { ... }
  ```

### 4.7 Fix DuplicateNumberLiteral (22 main)

- [ ] **4.7.1 Extract repeated numeric literals to named constants**

  Statistical and mathematical constants all have meaningful names. Extract each repeated literal to a `private static final` (or shared constants class for cross-file values):
  ```groovy
  private static final BigDecimal CONFIDENCE_95_Z = 1.96
  private static final BigDecimal DEFAULT_SIGNIFICANCE = 0.05
  private static final BigDecimal HALF = 0.5
  ```
  Use the HTML report to navigate to each affected file. Where the same constant is needed in multiple classes under the same package, extract to a package-level `*Constants.groovy` class rather than duplicating the field.

### 4.8 Fix DuplicateListLiteral (2 main)

- [ ] **4.8.1 Extract the repeated list to a constant or inline it**

  ```groovy
  // Before - same literal twice
  def a = [1, 2, 3]
  // ...
  def b = [1, 2, 3]
  // After — either extract to constant or use a single definition
  ```

- [ ] **4.9 Remove the codenarc override from matrix-stats/build.gradle**

  Delete lines 16–19:
  ```groovy
  codenarc {
    // TODO: Fix ~1000 CodeNarc violations and remove this override
    ignoreFailures = true
  }
  ```

- [ ] **4.10 Verify — confirm 0 violations remain**

  ```bash
  ./gradlew :matrix-stats:codenarcMain
  ./gradlew :matrix-stats:codenarcTest
  ./gradlew :matrix-stats:test
  ```
  Expected: all tasks pass.

- [ ] **4.11 Commit (after user confirmation)**

  ```bash
  git add matrix-stats/build.gradle matrix-stats/src/
  git commit -m "Fix CodeNarc violations in matrix-stats, remove ignoreFailures"
  ```

---

## Task 5: Fix matrix-ggplot violations (3283 violations across main + test)

matrix-ggplot has `ignoreFailures = true` on the entire `codenarc` block. The dominant violation is `IfStatementBraces` (943 in main), followed by duplicate-literal rules and string/cast cleanups. This task is the largest in the plan.

**Files to modify:**
- `matrix-ggplot/build.gradle` (remove lines 16–19)
- `matrix-ggplot/src/main/groovy/**/*.groovy`
- `matrix-ggplot/src/test/groovy/**/*.groovy`

### 5.1 Audit

- [ ] **5.1.1 Generate fresh reports and read them**

  ```bash
  # Generate reports (ignoreFailures is still true so both tasks succeed)
  ./gradlew :matrix-ggplot:codenarcMain :matrix-ggplot:codenarcTest

  python3 -c "
  import xml.etree.ElementTree as ET
  from collections import Counter

  for scope in ['main', 'test']:
      tree = ET.parse(f'matrix-ggplot/build/reports/codenarc/{scope}.xml')
      vs = tree.getroot().findall('.//Violation')
      print(f'\n=== {scope}: {len(vs)} violations ===')
      for r,c in Counter(v.get('ruleName') for v in vs).most_common():
          print(f'  {c:4d}  {r}')
  "
  ```

### 5.2 Fix IfStatementBraces (943 main + 4 test)

- [ ] **5.2.1 Add braces to every single-statement if/else body**

  ```groovy
  // Before
  if (condition)
    doSomething()
  
  // After
  if (condition) {
    doSomething()
  }
  ```
  With 943 violations across 310 source files, process file-by-file using the HTML report (`matrix-ggplot/build/reports/codenarc/main.html`) to navigate to each affected file.

### 5.3 Fix UnnecessaryGString (75 main + 646 test)

- [ ] **5.3.1 Replace uninterpolated double-quoted strings with single-quoted strings**

  Same mechanical change as in Tasks 1–4.

### 5.4 Fix DuplicateStringLiteral (638 main)

- [ ] **5.4.1 Extract repeated SVG/CSS strings to `private static final String` constants**

  For SVG attribute names, CSS property names, and rendering strings repeated across methods, extract to constants at the top of the class:
  ```groovy
  private static final String FILL_ATTR = 'fill'
  private static final String STROKE_ATTR = 'stroke'
  private static final String NONE = 'none'
  private static final String AUTO = 'auto'
  ```
  Even single-word strings should be named — it removes typo risk across render methods and makes intent explicit. Where the same strings appear across multiple renderer classes, extract to a shared `SvgConstants` class rather than duplicating the constants.

### 5.5 Fix DuplicateNumberLiteral (343 main)

- [ ] **5.5.1 Extract repeated numeric literals to named constants**

  Chart coordinate values, proportions, and scaling factors all have semantic meaning. Extract each repeated literal to a `private static final` constant:
  ```groovy
  private static final BigDecimal MIDPOINT = 0.5
  private static final BigDecimal PADDING_RATIO = 0.9
  private static final BigDecimal DEFAULT_STROKE_WIDTH = 1.0
  ```
  Where the same value is needed across multiple renderer classes, extract to a shared `ChartConstants` class. Use the HTML report to identify which files and literals are affected.

### 5.6 Fix UnnecessaryObjectReferences (78 main)

- [ ] **5.6.1 Consolidate repeated property assignments using `with()`**

  The violations are patterns like repeated `copy.foo = ...` / `copy.bar = ...` assignments and `rowMap.put(...)` call chains (confirmed in `Theme.groovy`, `GgStat.groovy`, `Themes.groovy`). Refactor each cluster to a `with()` block:
  ```groovy
  // Before — copy.* assignments in a clone/builder method
  copy.plotMargin = copyList(plotMargin)
  copy.panelSpacing = copyList(panelSpacing)
  copy.legendKeySize = copyList(legendKeySize)

  // After — both sides qualified with this.
  copy.with {
    plotMargin = this.copyList(this.plotMargin)
    panelSpacing = this.copyList(this.panelSpacing)
    legendKeySize = this.copyList(this.legendKeySize)
  }
  ```

  **Scope warning:** Inside `with()` the delegate is `copy`, so bare names on both sides of an assignment resolve against `copy` first. `plotMargin` on the right-hand side would read `copy.plotMargin` (the destination), not the source object's field, silently copying stale or default values. Qualify every access to the enclosing source object with `this.` — both the helper method call and the source field read. Apply the same discipline to every `with()` block in this task. If the number of `this.` qualifiers makes the block hard to read, capture the source fields in locals before entering:
  ```groovy
  def srcPlotMargin = plotMargin
  def srcPanelSpacing = panelSpacing
  copy.with {
    plotMargin = copyList(srcPlotMargin)
    panelSpacing = copyList(srcPanelSpacing)
  }
  ```

  Follow the HTML report for the full list of affected files and line numbers. Run `./gradlew :matrix-ggplot:compileGroovy && ./gradlew :matrix-ggplot:test -Pheadless=true` after converting each cluster.

### 5.7 Fix UnnecessaryCast (76 main + 76 test)

- [ ] **5.7.1 Remove unnecessary explicit casts**

  Under `@CompileStatic`, explicit casts are often redundant when the type is already inferred. Remove the cast and confirm the code still compiles:
  ```groovy
  // Before
  String s = (String) value
  // After (when value is already String)
  String s = value
  ```

### 5.8 Fix DuplicateMapLiteral (47 main)

- [ ] **5.8.1 Extract repeated map literals to constants**

  ```groovy
  // Before — same map repeated
  def opts = [color: 'black', size: 1]
  // ...
  def opts2 = [color: 'black', size: 1]
  // After
  private static final Map<String, Object> DEFAULT_OPTS = [color: 'black', size: 1]
  ```

### 5.9 Fix ClosureAsLastMethodParameter (45 test)

- [ ] **5.9.1 Move trailing closure outside parentheses**

  ```groovy
  // Before
  someMethod(arg1, { x -> x * 2 })
  // After
  someMethod(arg1) { x -> x * 2 }
  ```

### 5.10 Fix UnusedMethodParameter (21 main + 15 test)

- [ ] **5.10.1 Address each unused parameter**

  Options in priority order:
  1. Use the parameter if it should be used (may indicate a bug).
  2. Remove the parameter if the method signature permits (no interface/override constraint).
  3. Prefix with `_` if the parameter is required by an interface or override: `def _unusedParam`.

### 5.11 Fix UnusedImport (18 main + 12 test)

- [ ] **5.11.1 Remove unused import statements**

  Remove each flagged import. Confirm the class is not used anywhere in the file (check IDE or run `./gradlew :matrix-ggplot:compileGroovy` to catch reference errors).

### 5.12 Fix UnnecessaryElseStatement (10 main)

- [ ] **5.12.1 Remove else after return** — same pattern as Task 4.5.

### 5.13 Fix ReturnsNullInsteadOfEmptyCollection (10 main)

Violations are in `Transformations.groovy` (9) and `Theme.groovy` (1). **Verify semantics before changing any return value.**

- [ ] **5.13.1 Check `Transformations.groovy` — null is an intentional sentinel**

  `se/alipsa/matrix/gg/coord/Transformations.groovy` returns `null` to signal "use default breaks" (explicitly documented in comments like `return null // Use default breaks`). These are part of the coordinate-transform contract; callers distinguish `null` (no override) from `[]` (override with empty). Add `@SuppressWarnings('ReturnsNullInsteadOfEmptyCollection')` at the method level for each:
  ```groovy
  @SuppressWarnings('ReturnsNullInsteadOfEmptyCollection')
  List<Number> breaks(List<Number> limits) {
    ...
    return null // Use default breaks
  }
  ```

- [ ] **5.13.2 Check `Theme.groovy` line 192 — verify semantics**

  Open `se/alipsa/matrix/gg/theme/Theme.groovy` and read the method at line 192. If `null` means "unset/inherit" (a sentinel), add `@SuppressWarnings('ReturnsNullInsteadOfEmptyCollection')`. Only replace with `[]`/`[:]` if null genuinely means "empty result" with no semantic difference to callers.

### 5.14 Fix UnnecessaryDotClass (30 test)

- [ ] **5.14.1 Remove `.class` suffix in Groovy contexts**

  ```groovy
  // Before
  assertEquals(String.class, result.getClass())
  // After
  assertEquals(String, result.getClass())
  ```

### 5.15 Fix UnnecessaryBigDecimalInstantiation (15 test)

- [ ] **5.15.1 Use literal notation**

  ```groovy
  // Before
  new BigDecimal("1.5")
  // After
  1.5
  ```

### 5.16 Fix remaining test violations

- [ ] **5.16.1 Fix UnnecessarySelfAssignment (12)** — remove `a = a` assignments.
- [ ] **5.16.2 Fix ExplicitCallToCompareToMethod (10)** — replace `.compareTo()` with `<=>`.
- [ ] **5.16.3 Fix UnnecessaryPackageReference (10)** — use simple class name (already imported).
- [ ] **5.16.4 Fix AssignCollectionSort (6)** — use `.sort()` in-place or `.toSorted()`.
- [ ] **5.16.5 Fix UnnecessaryCollectCall (5)** — remove `.collect { it }` no-ops.
- [ ] **5.16.6 Fix UnnecessaryToString (5)** — remove `.toString()` in GString or `println` contexts.
- [ ] **5.16.7 Fix ExplicitCallToPlusMethod (4)** — replace `.plus(b)` with `+ b`.

### 5.17 Fix GetterMethodCouldBeProperty (9 main)

- [ ] **5.17.1 Convert explicit getter methods to Groovy properties**

  ```groovy
  // Before
  String getTitle() { title }
  // After — remove the getter; Groovy generates it from the field
  String title
  ```

### 5.18 Fix PropertyName violations (9 main)

- [ ] **5.18.1 Fix naming convention violations** per AGENTS.md: instance fields camelCase, static finals UPPER_SNAKE.

### 5.19 Fix ClassJavadoc violations (9 main)

- [ ] **5.19.1 Add brief GroovyDoc to each class missing it**

  ```groovy
  /** Renders violin geometry for a ggplot chart layer. */
  class ViolinRenderer { ... }
  ```

### 5.20 Fix MethodSize violations (8 main)

- [ ] **5.20.1 Address oversized methods (> 100 lines)**

  For each flagged method:
  - If the method has distinct phases (data preparation, rendering, output), extract private helper methods for each phase.
  Every method over 100 lines has separable phases — identify them and extract private helpers (e.g. separate data-collection, computation, and SVG-emission phases). Do not add `@SuppressWarnings` and do not change the global threshold.

- [ ] **5.21 Remove the codenarc override from matrix-ggplot/build.gradle**

  Delete lines 16–19:
  ```groovy
  codenarc {
    // TODO: Fix ~3000 CodeNarc violations and remove this override
    ignoreFailures = true
  }
  ```

- [ ] **5.22 Verify — confirm 0 violations remain**

  ```bash
  ./gradlew :matrix-ggplot:codenarcMain
  ./gradlew :matrix-ggplot:codenarcTest
  ./gradlew :matrix-ggplot:test -Pheadless=true
  ```

- [ ] **5.23 Commit (after user confirmation)**

  ```bash
  git add matrix-ggplot/build.gradle matrix-ggplot/src/
  git commit -m "Fix CodeNarc violations in matrix-ggplot, remove ignoreFailures"
  ```

---

## Task 6: Remove redundant @SuppressWarnings

The consolidated root ruleset globally excludes these rules from `config/codenarc/ruleset.groovy`. Any `@SuppressWarnings` referencing them is now dead code:

**From `basic.xml`:** `EmptyCatchBlock`, `EmptyIfStatement`, `EmptyElseBlock`

**From `design.xml`:** `AbstractClassWithoutAbstractMethod`, `BuilderMethodWithSideEffects`, `CloseWithoutCloseable`, `Instanceof`, `NestedForLoop`, `PrivateFieldCouldBeFinal`

**From `exceptions.xml`:** `CatchException`, `CatchThrowable`

**From `imports.xml`:** `NoWildcardImports`

**From `naming.xml`:** `FactoryMethodName`, `ConfusingMethodName`

**From `size.xml`:** `CrapMetric`

**From `unnecessary.xml`:** `UnnecessaryGetter`, `UnnecessarySetter`, `UnnecessaryPublicModifier`, `UnnecessaryReturnKeyword`

**From `unused.xml`:** `UnusedVariable`

**From `formatting.xml` (selected rules disabled in favour of Spotless — others remain active):** `BlockEndsWithBlankLine`, `BlockStartsWithBlankLine`, `ClassEndsWithBlankLine`, `ClassStartsWithBlankLine`, `ConsecutiveBlankLines`, `Indentation`, `LineLength`, `SpaceAfterComma`, `SpaceAfterMethodCallName`, `SpaceAfterOpeningBrace`, `SpaceAroundMapEntryColon`, `SpaceAroundOperator`, `SpaceBeforeClosingBrace`, `SpaceInsideParentheses`. Formatting rules not in this list (e.g. `SpaceBeforeOpeningBrace`) are still enforced; suppressions for those must be kept.

**From `comments.xml`:** `SpaceAfterCommentDelimiter`

Confirmed redundant suppressions found in the codebase (representative list; step 6.1 script is authoritative):

| File | Annotation to clean |
|---|---|
| `matrix-smile/src/main/groovy/**` | Multiple `@SuppressWarnings('NestedForLoop')` and `@SuppressWarnings('Instanceof')` |
| `matrix-smile/src/test/groovy/SmileDataTest.groovy` | `@SuppressWarnings('NestedForLoop')` ×3 |
| `matrix-core/src/main/groovy/se/alipsa/matrix/core/Matrix.groovy:47` | Large multi-rule annotation — remove `ClassStartsWithBlankLine`, `SpaceAroundOperator`, `SpaceAfterMethodCallName`, `SpaceAfterOpeningBrace`, `SpaceBeforeClosingBrace`, `ConsecutiveBlankLines`, `BlockStartsWithBlankLine`, `SpaceAfterComma`, `SpaceAfterCommentDelimiter`, `Instanceof`, `NestedForLoop`; keep remaining active rules |
| `matrix-core/src/main/groovy/se/alipsa/matrix/core/Grid.groovy:16` | `['Instanceof', 'NestedForLoop']` → remove whole annotation |
| `matrix-core/src/main/groovy/se/alipsa/matrix/core/RollingMatrix.groovy:130` | `['Instanceof', 'NestedForLoop']` → remove whole annotation |
| `matrix-core/src/main/groovy/se/alipsa/matrix/core/Joiner.groovy:238` | `['Instanceof', 'DuplicateStringLiteral']` → remove only `Instanceof`, keep `DuplicateStringLiteral` |
| `matrix-core/src/main/groovy/se/alipsa/matrix/core/util/CumulativeHelper.groovy:154` | `['ExplicitCallToCompareToMethod', 'Instanceof']` → remove only `Instanceof` |
| `matrix-core/src/main/groovy/se/alipsa/matrix/core/Column.groovy:23` et al. | `@SuppressWarnings('Instanceof')` → remove whole annotation |
| `matrix-core/src/test/groovy/MatrixBuilderTest.groovy:22` | Remove `ConsecutiveBlankLines`, `SpaceAfterComma`, `SpaceAroundOperator`, `SpaceAfterCommentDelimiter`, `ClassStartsWithBlankLine`, `ClassEndsWithBlankLine`; keep active rules |
| `matrix-core/src/test/groovy/MatrixTest.groovy:22` | Remove `ConsecutiveBlankLines`, `SpaceAfterCommentDelimiter`, `BlockEndsWithBlankLine`, `ClassEndsWithBlankLine`; keep active rules |
| `matrix-core/src/test/groovy/GridTest.groovy:12` | Remove `SpaceAfterComma`, `BlockEndsWithBlankLine`, `SpaceBeforeClosingBrace`, `SpaceAfterCommentDelimiter`, `ClassEndsWithBlankLine`; keep active rules |
| `matrix-core/src/test/groovy/StatTest.groovy:18` | Remove `SpaceAfterComma`, `SpaceAroundOperator`, `SpaceAfterCommentDelimiter`, `ClassEndsWithBlankLine`; keep active rules |
| `matrix-core/src/test/groovy/LoggerTest.groovy:11` | Remove `ClassEndsWithBlankLine`; keep active rules |
| `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/Chart.groovy:16` | Remove `Instanceof` from multi-rule annotation; keep remaining active rules |
| `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/LayerDsl.groovy:14` | `['IfStatementBraces', 'Instanceof', 'UnnecessaryOverridingMethod']` → remove only `Instanceof` |
| `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/geom/LayerBuilder.groovy:24` | `['Instanceof', 'DuplicateStringLiteral']` → remove only `Instanceof` |
| `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/theme/ElementBlank.groovy:9` | `['EmptyClass', 'ClassEndsWithBlankLine', 'ClassStartsWithBlankLine']` → remove `ClassEndsWithBlankLine` and `ClassStartsWithBlankLine`, keep `EmptyClass` |
| `matrix-charts/src/main/groovy/se/alipsa/matrix/charm/**` | Multiple separate `@SuppressWarnings('Instanceof')` and `@SuppressWarnings('NestedForLoop')` lines |
| `matrix-pict/src/main/groovy/se/alipsa/matrix/pict/Histogram.groovy:11–12` | Two separate annotations for `Instanceof` and `NestedForLoop` → remove both |
| `matrix-bigquery/src/main/groovy/se/alipsa/matrix/bigquery/Bq.groovy:97` | `['ClassSize', 'Instanceof', 'NestedForLoop']` → remove `Instanceof` and `NestedForLoop`, keep `ClassSize` |
| `matrix-bigquery/src/test/groovy/**` | Multiple `@SuppressWarnings('ClassEndsWithBlankLine')` annotations → remove whole annotation each time, or remove only `ClassEndsWithBlankLine` when mixed with active rules (e.g. `ClassName`) |
| `matrix-bigquery/src/test/groovy/test/alipsa/matrix/bigquery/BqDataTypesTest.groovy:30` | Remove `BlockEndsWithBlankLine`, `ClassEndsWithBlankLine`, `ConsecutiveBlankLines`, `SpaceAfterComma`, `SpaceAfterCommentDelimiter`, `SpaceAroundOperator`; keep active rules |
| `matrix-bigquery/src/test/groovy/test/alipsa/matrix/bigquery/BqTest.groovy:29` | Remove `ClassEndsWithBlankLine`, `SpaceAfterComma`, `SpaceAfterCommentDelimiter`, `SpaceAfterOpeningBrace`, `SpaceBeforeClosingBrace`; keep active rules |
| `matrix-sql/src/main/groovy/se/alipsa/matrix/sql/MatrixResultSet.groovy:175` | `@SuppressWarnings('CloseWithoutCloseable')` → remove whole annotation |
| `matrix-sql/src/main/groovy/se/alipsa/matrix/sql/SqlGenerator.groovy:9` | `@SuppressWarnings('SpaceInsideParentheses')` → remove whole annotation |
| `matrix-spreadsheet/src/main/groovy/.../FExcelReader.groovy:18` | `['UnusedObject', 'CloseWithoutCloseable']` → remove only `CloseWithoutCloseable`, keep `UnusedObject` |
| `matrix-spreadsheet/src/main/groovy/.../FOdsReader.groovy:17` | `@SuppressWarnings('CloseWithoutCloseable')` → remove whole annotation |
| `matrix-spreadsheet/src/test/groovy/spreadsheet/SpreadsheetFormatProviderTest.groovy:244` | `@SuppressWarnings('CloseWithoutCloseable')` → remove whole annotation |
| `matrix-tablesaw/src/main/groovy/.../Gtable.groovy:348` | `['unchecked', 'Instanceof']` → remove only `Instanceof`, keep `unchecked` |

- [ ] **6.1 Find all redundant suppressions (multiline-aware)**

  `@SuppressWarnings` annotations in this codebase sometimes span multiple lines (e.g. `Matrix.groovy:47`, `Stat.groovy:17` put the rule names on separate lines), so a simple two-stage grep misses them. Use the Python script below which reads each file as a whole and matches across newlines:

  ```bash
  python3 - << 'EOF'
  import re, glob

  EXCLUDED = {
      # basic.xml
      'EmptyCatchBlock', 'EmptyIfStatement', 'EmptyElseBlock',
      # design.xml
      'AbstractClassWithoutAbstractMethod', 'BuilderMethodWithSideEffects', 'CloseWithoutCloseable',
      'Instanceof', 'NestedForLoop', 'PrivateFieldCouldBeFinal',
      # exceptions.xml
      'CatchException', 'CatchThrowable',
      # imports.xml
      'NoWildcardImports',
      # naming.xml
      'FactoryMethodName', 'ConfusingMethodName',
      # size.xml
      'CrapMetric',
      # unnecessary.xml
      'UnnecessaryGetter', 'UnnecessarySetter', 'UnnecessaryPublicModifier', 'UnnecessaryReturnKeyword',
      # unused.xml
      'UnusedVariable',
      # formatting.xml — selected rules disabled in favour of Spotless; others (e.g. SpaceBeforeOpeningBrace) remain active
      'BlockEndsWithBlankLine', 'BlockStartsWithBlankLine', 'ClassEndsWithBlankLine',
      'ClassStartsWithBlankLine', 'ConsecutiveBlankLines', 'Indentation', 'LineLength',
      'SpaceAfterComma', 'SpaceAfterMethodCallName', 'SpaceAfterOpeningBrace',
      'SpaceAroundMapEntryColon', 'SpaceAroundOperator', 'SpaceBeforeClosingBrace',
      'SpaceInsideParentheses',
      # comments.xml
      'SpaceAfterCommentDelimiter',
  }

  for path in sorted(glob.glob('matrix-*/src/**/*.groovy', recursive=True)):
      with open(path) as f:
          content = f.read()
      for m in re.finditer(r'@SuppressWarnings\([^)]*\)', content, re.DOTALL):
          rules = re.findall(r"'([^']+)'", m.group())
          redundant = [r for r in rules if r in EXCLUDED]
          if redundant:
              line_no = content[:m.start()].count('\n') + 1
              annotation = ' '.join(m.group().split())  # normalise whitespace for display
              print(f'{path}:{line_no}:  {annotation}')
              print(f'  → redundant: {redundant}')
  EOF
  ```

- [ ] **6.2 For each hit: remove only the globally-excluded rule from the annotation**

  - If the annotation suppresses only globally-excluded rules: delete the whole `@SuppressWarnings(...)` line.
  - If the annotation is a list that mixes excluded and still-active rules: remove only the excluded entries from the list. When a list is reduced to a single entry, convert from list syntax to string syntax:
    ```groovy
    // Before
    @SuppressWarnings(['Instanceof', 'DuplicateStringLiteral'])
    // After — single remaining rule uses string form
    @SuppressWarnings('DuplicateStringLiteral')
    ```

- [ ] **6.3 Verify CodeNarc passes on each affected module**

  ```bash
  ./gradlew :matrix-smile:codenarcMain :matrix-smile:codenarcTest
  ./gradlew :matrix-core:codenarcMain :matrix-core:codenarcTest
  ./gradlew :matrix-charts:codenarcMain :matrix-charts:codenarcTest
  ./gradlew :matrix-pict:codenarcMain :matrix-pict:codenarcTest
  ./gradlew :matrix-bigquery:codenarcMain :matrix-bigquery:codenarcTest
  ./gradlew :matrix-sql:codenarcMain :matrix-sql:codenarcTest
  ./gradlew :matrix-spreadsheet:codenarcMain :matrix-spreadsheet:codenarcTest
  ./gradlew :matrix-tablesaw:codenarcMain :matrix-tablesaw:codenarcTest
  # Re-run the step 6.1 script verbatim; expected output: "OK — no redundant @SuppressWarnings remaining."
  python3 - << 'EOF'
  import re, glob

  EXCLUDED = {
      # basic.xml
      'EmptyCatchBlock', 'EmptyIfStatement', 'EmptyElseBlock',
      # design.xml
      'AbstractClassWithoutAbstractMethod', 'BuilderMethodWithSideEffects', 'CloseWithoutCloseable',
      'Instanceof', 'NestedForLoop', 'PrivateFieldCouldBeFinal',
      # exceptions.xml
      'CatchException', 'CatchThrowable',
      # imports.xml
      'NoWildcardImports',
      # naming.xml
      'FactoryMethodName', 'ConfusingMethodName',
      # size.xml
      'CrapMetric',
      # unnecessary.xml
      'UnnecessaryGetter', 'UnnecessarySetter', 'UnnecessaryPublicModifier', 'UnnecessaryReturnKeyword',
      # unused.xml
      'UnusedVariable',
      # formatting.xml — selected rules disabled in favour of Spotless; others (e.g. SpaceBeforeOpeningBrace) remain active
      'BlockEndsWithBlankLine', 'BlockStartsWithBlankLine', 'ClassEndsWithBlankLine',
      'ClassStartsWithBlankLine', 'ConsecutiveBlankLines', 'Indentation', 'LineLength',
      'SpaceAfterComma', 'SpaceAfterMethodCallName', 'SpaceAfterOpeningBrace',
      'SpaceAroundMapEntryColon', 'SpaceAroundOperator', 'SpaceBeforeClosingBrace',
      'SpaceInsideParentheses',
      # comments.xml
      'SpaceAfterCommentDelimiter',
  }

  found = False
  for path in sorted(glob.glob('matrix-*/src/**/*.groovy', recursive=True)):
      with open(path) as f:
          content = f.read()
      for m in re.finditer(r'@SuppressWarnings\([^)]*\)', content, re.DOTALL):
          rules = re.findall(r"'([^']+)'", m.group())
          redundant = [r for r in rules if r in EXCLUDED]
          if redundant:
              line_no = content[:m.start()].count('\n') + 1
              annotation = ' '.join(m.group().split())
              print(f'{path}:{line_no}:  {annotation}')
              print(f'  → redundant: {redundant}')
              found = True

  if not found:
      print('OK — no redundant @SuppressWarnings remaining.')
  EOF
  ```

- [ ] **6.4 Verify tests pass for affected modules**

  ```bash
  ./gradlew :matrix-smile:test :matrix-core:test
  ./gradlew :matrix-charts:test -Pheadless=true
  ./gradlew :matrix-pict:test :matrix-sql:test
  ./gradlew :matrix-bigquery:test :matrix-spreadsheet:test :matrix-tablesaw:test
  ```

- [ ] **6.5 Commit (after user confirmation)**

  ```bash
  git add matrix-smile/ matrix-core/ matrix-charts/ matrix-pict/ matrix-bigquery/ \
          matrix-sql/ matrix-spreadsheet/ matrix-tablesaw/
  git commit -m "Remove redundant @SuppressWarnings for globally-excluded CodeNarc rules"
  ```

---

## Final verification

After all tasks complete:

```bash
# Full build — CodeNarc runs as part of check
./gradlew build

# Confirm no ignoreFailures overrides remain in module build files
grep -rn "ignoreFailures" --include="build.gradle" .
```

Expected: Build succeeds. The only match is `ignoreFailures = false` in the root `build.gradle` (the global default). No module-level overrides remain.
