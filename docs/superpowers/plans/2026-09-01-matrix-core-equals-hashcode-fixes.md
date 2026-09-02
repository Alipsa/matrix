# matrix-core equals/hashCode Contract Fixes Implementation Plan

> **For agentic workers:** Follow this plan task-by-task and respect the repository's agent instructions. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix two confirmed `equals()`/`hashCode()` contract violations found in a code review of `matrix-core` — `Row` implementing `List` without overriding `equals()`/`hashCode()`, and `Matrix.hashCode()` being inconsistent with the numeric-tolerant `Matrix.equals(Object)` — and document (without changing) a related, intentional design choice in the multi-arg `equals()`.

**Architecture:** Two isolated, independent bug fixes in existing classes (`Row.groovy`, `Matrix.groovy`) plus one documentation clarification. No new files, no public API signature changes. Each fix is TDD'd with a regression test that fails against current `main` and passes after the fix.

**Tech Stack:** Groovy 5.1.1, JUnit Jupiter 6, Gradle multi-module build (`matrix-core` module only).

**Spec:** No separate spec document — this plan implements the findings from a direct code review (summarized in Background below). There is no `docs/superpowers/specs/` entry for this work.

## Background (review findings this plan fixes)

1. **`Row` breaks the `List` equals/hashCode contract.** `matrix-core/src/main/groovy/se/alipsa/matrix/core/Row.groovy` declares `class Row implements GroovyObject, List<Object>` but never overrides `equals()`/`hashCode()` and doesn't extend `AbstractList`. Two `Row`s with identical content are therefore only `.equals()` when they're the same object (identity fallback from `Object`). This is invisible in this repo's own Groovy code because Groovy's `==` operator special-cases `List`-vs-`List` comparison and never calls `Row.equals()` — but it breaks for plain Java callers, `HashSet`/`HashMap` usage, and any `List.contains()`/`indexOf()` call with a different-but-equal `Row` instance.

2. **`Matrix.hashCode()` is inconsistent with `Matrix.equals(Object)`.** `Matrix.equals(Object o)` (the zero-arg `Object` override) delegates to a numeric-tolerant comparison (`checkValues`) that treats any two `Number` cells as equal whenever their mathematical values match, regardless of runtime type or `BigDecimal` scale (e.g. `1` vs `1.0d` vs `BigDecimal("1.00")`). `Matrix.hashCode()`, however, hashes columns via `ArrayList.hashCode()`, which is sensitive to exact value representation (`BigDecimal.hashCode()` is documented as scale-sensitive). Two matrices that `equals()` reports as equal can therefore have different `hashCode()`s, violating `Object`'s contract and breaking `HashSet`/`HashMap` use of `Matrix`.

3. **Design inconsistency (documentation only, no behavior change).** In `checkValues`, the `Number`-vs-`Number` comparison branch ignores the `ignoreTypes` flag entirely — even when a caller passes `ignoreTypes = false` ("don't ignore types"), two numerically-equal `Number`s of different runtime types still compare equal. Investigation showed this is *intentional and required*: `MatrixAssertions.assertEquals` (used pervasively across the test suite) calls `equals(..., ignoreTypes = false, ...)` specifically to compare computed `BigDecimal` results against plain `int`/`Integer` literals in tests. Making the `Number`-vs-`Number` branch respect `ignoreTypes` strictly would break a large number of existing tests. Decision: **no behavior change** — clarify this in the Javadoc and lock it in with a regression test so a future contributor doesn't "fix" it into a breaking change.

## Global Constraints

- Groovy 5.1.1, Java 21 target. 2-space indentation, existing import style per file.
- Do not add `@CompileStatic` to production classes (project applies it globally via `config/groovy/compileStatic.groovy`); `Row.groovy`/`Matrix.groovy` are already compiled statically by default — new code must satisfy static type-checking.
- Always add GroovyDoc for public methods (per `AGENTS.md`).
- Verification order for each task, per `AGENTS.md`: `./gradlew :matrix-core:codenarcMain` → `./gradlew :matrix-core:spotlessCheck` (or `spotlessApply` to auto-fix) → `./gradlew :matrix-core:test`.
- Never commit directly to `main`/`master`; work on a feature branch and open a PR (per this user's global instructions). Never push or open the PR without the user's explicit go-ahead.
- Commit messages: short, imperative summaries (e.g. "Fix ...", "Add ...").

---

### Task 0: Create the feature branch

**Files:** none (git operation only)

- [x] **Step 1: Confirm the working tree is clean before branching**

Run: `git status --short`
Expected: no output referring to files under `matrix-core/` (an unrelated untracked `tools/` directory is fine and should be left alone).

- [x] **Step 2: Create and switch to a feature branch**

```bash
git checkout -b fix/matrix-core-equals-hashcode
```

---

### Task 1: Fix `Row` to implement `equals()`/`hashCode()` per the `List` contract

**Files:**
- Modify: `matrix-core/src/main/groovy/se/alipsa/matrix/core/Row.groovy:464` (insert after the existing `toString()` method, before `minusColumn(String columnName)`)
- Test: `matrix-core/src/test/groovy/RowTest.groovy`

**Interfaces:**
- Consumes: `Row.content` (existing private `final List<Object> content` field, already defined in `Row.groovy:25`) — no new fields needed.
- Produces: `Row.equals(Object)` and `Row.hashCode()` overrides other tasks/callers can now rely on for value-based comparison.

- [x] **Step 1: Write the failing tests**

Add to the top of `RowTest.groovy`, extend the static imports (currently `assertEquals`, `assertIterableEquals`, `assertNull`, `assertThrows`) to also import `assertNotEquals` and `assertNotSame`:

```groovy
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertIterableEquals
import static org.junit.jupiter.api.Assertions.assertNotEquals
import static org.junit.jupiter.api.Assertions.assertNotSame
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertThrows
```

Add these test methods inside `class RowTest { ... }` (anywhere after the existing tests, before the closing `}`):

```groovy
  @Test
  void testEqualsComparesContentNotIdentity() {
    Matrix table1 = Matrix.builder()
        .columns(id: [1, 2], name: ['Rick', 'Dan'])
        .types(Integer, String)
        .build()
    Matrix table2 = Matrix.builder()
        .columns(id: [1, 2], name: ['Rick', 'Dan'])
        .types(Integer, String)
        .build()

    Row row1 = table1.row(0)
    Row row2 = table2.row(0)

    assertNotSame(row1, row2)
    assertEquals(row1, row2)
    assertEquals(row1.hashCode(), row2.hashCode())
  }

  @Test
  void testEqualsReturnsFalseForDifferentContent() {
    Matrix table = Matrix.builder()
        .columns(id: [1, 2], name: ['Rick', 'Dan'])
        .types(Integer, String)
        .build()

    assertNotEquals(table.row(0), table.row(1))
  }

  @Test
  void testEqualsAgainstPlainList() {
    Matrix table = Matrix.builder()
        .columns(id: [1, 2], name: ['Rick', 'Dan'])
        .types(Integer, String)
        .build()

    Row row = table.row(0)

    assertEquals([1, 'Rick'], row)
    assertEquals(row, [1, 'Rick'])
    assertNotEquals(row, [1, 'Someone Else'])
    assertNotEquals(row, 'not a list at all')
  }
```

- [x] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :matrix-core:test --tests "RowTest"`
Expected: `testEqualsComparesContentNotIdentity` and `testEqualsReturnsFalseForDifferentContent` FAIL (rows compare unequal / hash codes differ because `Row` currently uses identity equality). `testEqualsAgainstPlainList` passes already (it exercises `List.equals(Row)` from the plain-list side, which already works via `AbstractList.equals()`) — that's fine, it's here as a locked-in regression guard for the fix, not a new failure.

- [x] **Step 3: Implement `equals()`/`hashCode()` on `Row`**

In `Row.groovy`, insert immediately after the existing `toString()` method (line 464, right before `List<Object> minusColumn(String columnName)`):

```groovy
    /**
     * Two rows are equal when compared against another {@link List} of the same
     * size with equal corresponding elements, matching the general
     * {@link List#equals(Object)} contract that this class's {@code implements List}
     * declaration promises.
     *
     * @param o the object to compare against
     * @return true if {@code o} is a List with the same elements in the same order
     */
    @Override
    boolean equals(Object o) {
        if (this.is(o)) {
            return true
        }
        if (!(o instanceof List)) {
            return false
        }
        return content.equals(o)
    }

    /**
     * Hash code consistent with {@link #equals(Object)}, matching the general
     * {@link List#hashCode()} contract.
     *
     * @return the hash code derived from this row's current element values
     */
    @Override
    int hashCode() {
        return content.hashCode()
    }

```

- [x] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :matrix-core:test --tests "RowTest"`
Expected: PASS (all `RowTest` methods, including the three added above).

- [x] **Step 5: Run the full module test suite to check for regressions**

Run: `./gradlew :matrix-core:test`
Expected: PASS. (No production or test code in this module relies on `Row` identity-based equality — verified during review by searching for `Set<Row>`/`HashMap`-keyed-by-`Row` usage and finding none.)

- [x] **Step 6: Static analysis and formatting**

Run: `./gradlew :matrix-core:codenarcMain :matrix-core:codenarcTest`
Expected: no new violations.

Run: `./gradlew :matrix-core:spotlessCheck`
Expected: PASS. If it fails on formatting only, run `./gradlew :matrix-core:spotlessApply` and re-verify with `spotlessCheck`.

- [x] **Step 7: Commit**

```bash
git add matrix-core/src/main/groovy/se/alipsa/matrix/core/Row.groovy matrix-core/src/test/groovy/RowTest.groovy
git commit -m "Fix Row to implement equals()/hashCode() per the List contract"
```

---

### Task 2: Fix `Matrix.hashCode()` to be consistent with the numeric-tolerant `Matrix.equals(Object)`

**Files:**
- Modify: `matrix-core/src/main/groovy/se/alipsa/matrix/core/Matrix.groovy:2263-2268` (the `hashCode()` method)
- Test: `matrix-core/src/test/groovy/MatrixTest.groovy`

**Interfaces:**
- Consumes: `Matrix.mColumns` (`List<Column>`), `Column` (extends `ArrayList`, iterable of cell values) — both already exist, no changes to their shape.
- Produces: a new private static helper `Matrix.normalizedValueHash(Object)` that Task 3 does not need but that future maintainers touching `checkValues`/`hashCode()` together should be aware of.

- [x] **Step 1: Write the failing tests**

Add to `MatrixTest.groovy` (anywhere after the existing `testEqualsHashCodeContractIgnoresMatrixName` test around line 1303, so the two contract tests sit together):

```groovy
  @Test
  void testEqualsHashCodeContractWithDifferentBigDecimalScale() {
    Matrix left = Matrix.builder()
        .data(id: [1], amount: [new BigDecimal('1.0')])
        .types(Integer, BigDecimal)
        .build()
    Matrix right = Matrix.builder()
        .data(id: [1], amount: [new BigDecimal('1.00')])
        .types(Integer, BigDecimal)
        .build()

    assertEquals(left, right)
    assertEquals(left.hashCode(), right.hashCode())
  }

  @Test
  void testEqualsHashCodeContractWithDifferentNumberSubtypes() {
    Matrix left = Matrix.builder()
        .data(id: [1], amount: [1])
        .types(Integer, Number)
        .build()
    Matrix right = Matrix.builder()
        .data(id: [1], amount: [1.0d])
        .types(Integer, Number)
        .build()

    assertEquals(left, right)
    assertEquals(left.hashCode(), right.hashCode())
  }
```

(`assertEquals` here is the statically-imported `org.junit.jupiter.api.Assertions.assertEquals`, already imported at the top of `MatrixTest.groovy`; it calls `Matrix.equals(Object)`/`hashCode()` via `Objects.equals`/direct `.hashCode()` calls, not the tolerant multi-arg overload.)

- [x] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :matrix-core:test --tests "MatrixTest.testEqualsHashCodeContractWithDifferentBigDecimalScale" --tests "MatrixTest.testEqualsHashCodeContractWithDifferentNumberSubtypes"`
Expected: both FAIL on the `assertEquals(left.hashCode(), right.hashCode())` line (the `assertEquals(left, right)` line already passes today — `equals()` is already tolerant; it's `hashCode()` that's wrong).

- [x] **Step 3: Implement the normalized hash**

In `Matrix.groovy`, replace the current `hashCode()` (lines 2263-2268):

```groovy
  @Override
  int hashCode() {
    int result = mColumns != null ? mColumns.hashCode() : 0
    result = 31 * result + types().hashCode()
    return result
  }
```

with:

```groovy
  @Override
  int hashCode() {
    int result = 1
    if (mColumns != null) {
      for (Column col : mColumns) {
        int colHash = 1
        for (Object value : col) {
          colHash = 31 * colHash + normalizedValueHash(value)
        }
        result = 31 * result + colHash
      }
    }
    result = 31 * result + types().hashCode()
    return result
  }

  /**
   * Hash a single cell value the same way {@link #checkValues} compares it, so that
   * {@link #hashCode()} stays consistent with the numeric tolerance used by the
   * zero-arg {@link #equals(Object)} override: numbers that are mathematically equal
   * (e.g. {@code 1}, {@code 1.0d}, {@code new BigDecimal("1.00")}) must hash identically
   * regardless of runtime type or {@code BigDecimal} scale.
   *
   * @param value the cell value to hash
   * @return a hash code consistent with {@link #equals(Object)}
   */
  private static int normalizedValueHash(Object value) {
    if (value == null) {
      return 0
    }
    if (value instanceof Number) {
      return (value as Number).toBigDecimal().stripTrailingZeros().hashCode()
    }
    return value.hashCode()
  }
```

Place `normalizedValueHash` directly after `hashCode()` (i.e. before the `grid()` method that currently follows at line 2274).

- [x] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :matrix-core:test --tests "MatrixTest.testEqualsHashCodeContractWithDifferentBigDecimalScale" --tests "MatrixTest.testEqualsHashCodeContractWithDifferentNumberSubtypes" --tests "MatrixTest.testEqualsHashCodeContractIgnoresMatrixName"`
Expected: PASS (including the pre-existing `testEqualsHashCodeContractIgnoresMatrixName`, to confirm no regression on the simple case).

- [x] **Step 5: Run the full module test suite to check for regressions**

Run: `./gradlew :matrix-core:test`
Expected: PASS. Pay particular attention to any test that computes a `Matrix.hashCode()` or relies on `Matrix` in a `Set`/`Map` — none were found in the codebase during review, but re-check the failure output carefully if anything unexpected breaks.

- [x] **Step 6: Static analysis and formatting**

Run: `./gradlew :matrix-core:codenarcMain :matrix-core:codenarcTest`
Expected: no new violations.

Run: `./gradlew :matrix-core:spotlessCheck`
Expected: PASS (or `spotlessApply` then re-check).

- [x] **Step 7: Commit**

```bash
git add matrix-core/src/main/groovy/se/alipsa/matrix/core/Matrix.groovy matrix-core/src/test/groovy/MatrixTest.groovy
git commit -m "Fix Matrix.hashCode() to be consistent with the numeric-tolerant equals()"
```

---

### Task 3: Document the intentional `ignoreTypes` behavior for numeric comparisons (no behavior change)

**Files:**
- Modify: `matrix-core/src/main/groovy/se/alipsa/matrix/core/Matrix.groovy:1876-1878` (Javadoc for the `ignoreTypes` parameter on the multi-arg `equals(...)` method)
- Test: `matrix-core/src/test/groovy/MatrixTest.groovy`

**Interfaces:**
- Consumes: `Matrix.equals(Object, boolean, boolean, boolean, BigDecimal, boolean, String)` — existing signature, unchanged.
- Produces: nothing new for other tasks; this is a documentation + regression-lock task.

- [x] **Step 1: Write the characterization test**

Add to `MatrixTest.groovy`, after the two tests added in Task 2, and ensure `org.junit.jupiter.api.Assertions.assertTrue` is available (it already is, via the wildcard static import `import static org.junit.jupiter.api.Assertions.*` at the top of `MatrixTest.groovy`):

```groovy
  @Test
  void testEqualsIgnoreTypesFalseStillComparesNumbersByMathematicalValue() {
    Matrix left = Matrix.builder()
        .data(id: [1], amount: [5])
        .types(Integer, Number)
        .build()
    Matrix right = Matrix.builder()
        .data(id: [1], amount: [5L])
        .types(Integer, Number)
        .build()

    // ignoreTypes=false guards declared *column* types (checked up front via types())
    // and non-numeric *cell* values; numeric cell comparisons are always done by
    // mathematical value regardless of runtime Number subtype. This is intentional —
    // see the Javadoc on the ignoreTypes parameter — and is locked in here so it is
    // not accidentally "fixed" into a breaking change for MatrixAssertions callers.
    assertTrue(left.equals(right, true, true, false, BigDecimal.ZERO, false))
  }
```

Note this test is not expected to fail before the implementation step below — this task changes documentation, not behavior. Its purpose is to make the current, intentional behavior explicit and protected by a test.

- [x] **Step 2: Run the test to confirm it already passes**

Run: `./gradlew :matrix-core:test --tests "MatrixTest.testEqualsIgnoreTypesFalseStillComparesNumbersByMathematicalValue"`
Expected: PASS (confirms today's behavior matches what we're about to document; if this fails, stop and re-investigate before touching the Javadoc — it would mean the earlier review finding was wrong).

- [x] **Step 3: Update the Javadoc**

In `Matrix.groovy`, the multi-arg `equals` method currently reads (lines 1864-1878):

```groovy
  /**
   * Compare this matrix to another with configurable comparison rules.
   *
   * @param o the matrix to compare against
   * @param ignoreColumnNames whether to ignore column names when comparing
   * @param ignoreMatrixName whether to ignore the matrix name when comparing
   * @param ignoreTypes whether to ignore column types when comparing
   * @param allowedDiff numeric tolerance for numeric values
   * @param throwException whether to throw on mismatch instead of returning false
   * @param message message prefix used when throwException is true
   * @return true if the matrices are considered equal, otherwise false
   */
  @SuppressWarnings('EqualsOverloaded')
  boolean equals(Object o, boolean ignoreColumnNames, boolean ignoreMatrixName, boolean ignoreTypes = true,
                 BigDecimal allowedDiff = 0.0001, boolean throwException = false, String message = '') {
```

Replace the `@param ignoreTypes` line with:

```groovy
   * @param ignoreTypes whether to ignore declared column types and non-numeric cell
   *        value types when comparing. Numeric cell values are always compared by
   *        mathematical value (within {@code allowedDiff}) regardless of this flag —
   *        e.g. {@code 5} and {@code 5L} are always considered equal cell values even
   *        with {@code ignoreTypes = false}. This lets {@link MatrixAssertions} compare
   *        computed {@code BigDecimal} results against plain {@code int}/{@code Integer}
   *        expected values in tests.
```

- [x] **Step 4: Re-run the characterization test**

Run: `./gradlew :matrix-core:test --tests "MatrixTest.testEqualsIgnoreTypesFalseStillComparesNumbersByMathematicalValue"`
Expected: PASS (unchanged — this step only touched a comment).

- [x] **Step 5: Static analysis and formatting**

Run: `./gradlew :matrix-core:codenarcMain :matrix-core:codenarcTest`
Expected: no new violations.

Run: `./gradlew :matrix-core:spotlessCheck`
Expected: PASS (or `spotlessApply` then re-check).

- [x] **Step 6: Commit**

```bash
git add matrix-core/src/main/groovy/se/alipsa/matrix/core/Matrix.groovy matrix-core/src/test/groovy/MatrixTest.groovy
git commit -m "Document that ignoreTypes never affects numeric cell comparisons"
```

---

### Task 4: Full module verification and PR

**Files:** none (verification and VCS only)

- [x] **Step 1: Full verification, in AGENTS.md order**

```bash
./gradlew :matrix-core:codenarcMain
./gradlew :matrix-core:codenarcTest
./gradlew :matrix-core:spotlessCheck
./gradlew :matrix-core:test
```

Expected: all PASS. Record the exact commands and their pass/fail result in the PR description (per `AGENTS.md`'s Commit & Pull Request Guidelines).

- [x] **Step 2: Review the full diff before pushing**

```bash
git log --oneline main..HEAD
git diff main..HEAD --stat
```

Expected: three commits (Task 1, Task 2, Task 3), touching only `Row.groovy`, `Matrix.groovy`, `RowTest.groovy`, `MatrixTest.groovy`.

- [ ] **Step 3: Push and open the PR — only after explicit user confirmation**

This step is intentionally not scripted here. Per this repository's standing instructions, do not push or open a pull request without the user explicitly asking for it in this session. When confirmed:

```bash
git push -u origin fix/matrix-core-equals-hashcode
gh pr create --title "Fix Row/Matrix equals-hashCode contract violations" --body "$(cat <<'EOF'
## Summary
- Row now implements equals()/hashCode() consistent with the List contract it declares (Row.groovy)
- Matrix.hashCode() is now consistent with the numeric-tolerant Matrix.equals(Object) for cells that differ only in Number subtype or BigDecimal scale (Matrix.groovy)
- Documented (no behavior change) that ignoreTypes=false never forces numeric cell comparisons to be type-strict, since MatrixAssertions relies on that

## Test plan
- [x] ./gradlew :matrix-core:codenarcMain
- [x] ./gradlew :matrix-core:codenarcTest
- [x] ./gradlew :matrix-core:spotlessCheck
- [x] ./gradlew :matrix-core:test
EOF
)"
```

---

## Self-Review Notes

- **Spec coverage:** All three Background findings have a task (Task 1 → finding 1, Task 2 → finding 2, Task 3 → finding 3). No gaps.
- **Placeholder scan:** No `TODO`/`TBD`/"add appropriate handling" language; every step has literal code or literal commands.
- **Type consistency:** `Row.content` (private `List<Object>`, declared `Row.groovy:25`) is the only field Task 1 touches — verified against the existing constructors, no rename. `normalizedValueHash` is introduced and consumed only within Task 2's `hashCode()`; no other task references it. Test method names are unique across `RowTest.groovy`/`MatrixTest.groovy` and don't collide with existing tests (`testEqualsHashCodeContractIgnoresMatrixName` already exists and is left untouched, only used as a companion assertion in Task 2 Step 4).
