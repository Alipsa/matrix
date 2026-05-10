# Implementation Plan: matrix-datasets v2.2.0 Roadmap

## Overview
This plan implements all 13 items from `req/v2.2.0-roadmap.md` across the `matrix-datasets` module. Tasks are grouped by theme and dependency to minimize merge conflicts and enable incremental testing.

---

## Phase 1: Build & Dependency Fixes

### 1.1 Dependency scopes (Issue #2)
**File:** `build.gradle`
- No scope changes. `matrix-core`, `groovy`, and `groovy-ginq` all remain `compileOnly`.
- Rationale: Matrix module dependencies are managed by the BOM (`matrix-bom/bom.xml`). To allow flexibility in choosing module combinations, no matrix modules are transitively included. Consumers declare the matrix modules they need explicitly.

### 1.2 Gate external network tests behind `RUN_EXTERNAL_TESTS` (Issue #3)
**Files:** `build.gradle`, `src/test/groovy/datasets/DatasetTest.groovy`, `src/test/groovy/datasets/RdatasetsTest.groovy`
- In `DatasetTest.groovy`, add `@Tag('external')` to `testFromUrl`.
- In `RdatasetsTest.groovy`, add `@Tag('external')` to `testOverview`, `fetchInfo`, `testFetchData`, and any test for `refresh()` (§5.2).
- **Extend the existing `test { ... }` block** in `build.gradle`: replace the existing `useJUnitPlatform()` call with a single conditional call:
  ```groovy
  test {
      // Allow enabling from CLI: -PrunExternalTests=true (or env RUN_EXTERNAL_TESTS=true)
      def runExternal = (project.findProperty('runExternalTests') ?: System.getenv('RUN_EXTERNAL_TESTS'))?.toString()?.toBoolean() ?: false
      if (runExternal) {
          logger.lifecycle("Running external tests (including @Tag('external') tests)")
      } else {
          logger.lifecycle("Excluding external tests (set -PrunExternalTests=true or RUN_EXTERNAL_TESTS=true to include them)")
      }
      useJUnitPlatform {
          if (!runExternal) {
              excludeTags 'external'
          }
      }
      testLogging {
          // ... existing logging config preserved ...
      }
  }
  ```

---

## Phase 2: Rdatasets Reliability & API Improvements

### 2.1 Convert `Rdatasets` to lazy loading with `refresh()` (Issue #1)
**File:** `src/main/groovy/se/alipsa/matrix/datasets/Rdatasets.groovy`
- Remove the static initializer that downloads `datasets.csv`.
- Add `private static volatile Matrix cachedOverview = null`.
- Make lazy init thread-safe with double-checked locking:
  ```groovy
  static Matrix overview() {
      if (cachedOverview == null) {
          synchronized(Rdatasets) {
              if (cachedOverview == null) {
                  // download, parse, assign to cachedOverview
              }
          }
      }
      return cachedOverview
  }
  ```
  On failure, throw `UncheckedIOException` (or `IllegalStateException`) wrapping the cause, with GroovyDoc documenting that remote access can fail.
- Add `static void refresh()` to clear the cache so the next `overview()` call re-fetches. **GroovyDoc required.**

### 2.2 Add `Rdatasets` convenience overloads (Issue #13)
**File:** `src/main/groovy/se/alipsa/matrix/datasets/Rdatasets.groovy`
- Add `static Matrix fetchData(String packageSlashItem)` that splits on `/` and delegates to the two-arg overload.
  - Validate: reject null/blank input, missing `/`, more than one `/`, or blank package/item after split — throw `IllegalArgumentException` with a clear message for each case.
  - **GroovyDoc required.**
- Add `static Matrix search(String text)` that filters `overview()` rows by `Item` or `Title` containing `text` (case-insensitive), returning a new `Matrix` subset.
  - **GroovyDoc required.**
- **Caveat / Deferral:** Optional type-inference on fetched data is deferred to v2.3.0. It requires scanning every column of arbitrarily large datasets (e.g., 54 k rows for diamonds), adds latency and complexity, and the ROI is lower than the other items. The two-arg `fetchData` can later be extended with `boolean inferTypes` without breaking API.

---

## Phase 3: Dataset Style & Bug Fixes

### 3.1 Remove phantom `Id: Integer` from `iris()` and `descIris()` (Issue #4)
**File:** `src/main/groovy/se/alipsa/matrix/datasets/Dataset.groovy`
- Remove `Id: Integer` from `iris()` `convert()` call.
- Remove the `Id` line from `descIris()` heredoc.

### 3.2 Fix `FileUtil.getResourcePath()` null guard (Issue #5)
**File:** `src/main/groovy/se/alipsa/matrix/datasets/util/FileUtil.groovy`
- Add `if (url == null) throw new FileNotFoundException("Resource not found: $name")` before `url.getFile()`.

### 3.3 Fix explicit `return` statements (Issues #8, #9)
**File:** `src/main/groovy/se/alipsa/matrix/datasets/Dataset.groovy`
- Remove explicit `return` from `mpg()` and `diamonds()` (Issue #8).
- Remove explicit `return` from all `descXxx()` methods: `descMtcars`, `descIris`, `descPlantGrowth`, `descToothGrowth`, `descUsArrests`, `descMpg`, `descDiamonds`, `descMapData` (Issue #9).

### 3.4 Refactor `describe(String)` to map dispatch (Issue #10)
**File:** `src/main/groovy/se/alipsa/matrix/datasets/Dataset.groovy`
- Replace the 10 sequential `if` statements with a `@CompileStatic`-friendly map using per-entry coercion to `java.util.function.Supplier<String>`:
  ```groovy
  private static final Map<String, Supplier<String>> DESCRIBERS = [
      airquality : ({ descAirquality() } as Supplier<String>),
      cars       : ({ descCars() }       as Supplier<String>),
      mtcars     : ({ descMtcars() }     as Supplier<String>),
      iris       : ({ descIris() }       as Supplier<String>),
      plantgrowth: ({ descPlantGrowth() } as Supplier<String>),
      toothgrowth: ({ descToothGrowth() } as Supplier<String>),
      usarrests  : ({ descUsArrests() }  as Supplier<String>),
      mpg        : ({ descMpg() }        as Supplier<String>),
      diamonds   : ({ descDiamonds() }   as Supplier<String>),
      mapdata    : ({ descMapData() }    as Supplier<String>),
      map_data   : ({ descMapData() }    as Supplier<String>)
  ]

  static String describe(String tableName) {
      DESCRIBERS.get(tableName.toLowerCase(Locale.ROOT))?.get() ?: "Unknown table: ${tableName}"
  }
  ```

### 3.5 Add missing `depth` and `table` descriptions to `descDiamonds()` (Issue #6)
**File:** `src/main/groovy/se/alipsa/matrix/datasets/Dataset.groovy`
- Insert `depth` and `table` variable descriptions into `descDiamonds()` heredoc:
  - `depth`: total depth percentage = z / mean(x, y)
  - `table`: width of top of diamond relative to widest point

### 3.6 Fix README version (Issue #7)
**File:** `README.md` and any other docs in the module
- The project uses Groovy `5.0.6` (`gradle/libs.versions.toml`). If any documentation mentions `5.0.5`, update it to `5.0.6`. The README already shows `5.0.6` and is correct; no change needed unless a discrepancy is found.

---

## Phase 4: Usability Enhancements

> **Dependency note:** Phase 4.1 must follow Phase 3.4 because `DATASET_LOADERS` is introduced here and `names()` derives from it.

### 4.1 Add `Dataset.names()`, `Dataset.mapNames()`, `Dataset.load(String)` (Issue #11)
**File:** `src/main/groovy/se/alipsa/matrix/datasets/Dataset.groovy`
- Introduce a separate loader map (distinct from `DESCRIBERS`) so documentation aliases do not pollute the loadable set. Use per-entry coercion to `Supplier<Matrix>`:
  ```groovy
  private static final Map<String, Supplier<Matrix>> DATASET_LOADERS = [
      airquality : ({ airquality() }  as Supplier<Matrix>),
      cars       : ({ cars() }        as Supplier<Matrix>),
      mtcars     : ({ mtcars() }      as Supplier<Matrix>),
      iris       : ({ iris() }        as Supplier<Matrix>),
      plantgrowth: ({ plantGrowth() } as Supplier<Matrix>),
      toothgrowth: ({ toothGrowth() } as Supplier<Matrix>),
      usarrests  : ({ usArrests() }   as Supplier<Matrix>),
      mpg        : ({ mpg() }         as Supplier<Matrix>),
      diamonds   : ({ diamonds() }    as Supplier<Matrix>)
  ]
  ```
- `names()` returns `DATASET_LOADERS.keySet().sort()`. **GroovyDoc required.**
- `mapNames()` returns `MAP_DATA_FILES.keySet().sort()`. **GroovyDoc required.**
- `load(String name)` looks up `name.toLowerCase(Locale.ROOT)` in `DATASET_LOADERS` and calls `get()`. Throws `IllegalArgumentException` for unknown names. `mapdata`/`map_data` are intentionally **not** supported — callers must use `Dataset.mapData(name)` directly. **GroovyDoc required.**

### 4.2 Improve `mapData()` error messages and add `mapRegions()` (Issue #12)
**File:** `src/main/groovy/se/alipsa/matrix/datasets/Dataset.groovy`
- In `mapData()`, trim whitespace from `datasetName`, then lowercase with `Locale.ROOT`.
- When throwing `IllegalArgumentException` for invalid map name, include valid names: `"no map data exists for '$datasetName'. Valid names: ${mapNames().join(', ')}"`.
- Add `static List<String> mapRegions(String datasetName)` that loads the map dataset and returns distinct `region` values sorted. **Performance note:** This loads the full map CSV (e.g., 99 k rows for `world`) into memory to extract distinct regions. Document this in GroovyDoc so callers are aware. **GroovyDoc required.**

---

## Phase 5: Documentation, Release Notes, Testing & Validation

### 5.1 Update `release.md`
**File:** `release.md`
- Add a v2.2.0 bullet list summarizing all changes (lazy Rdatasets, dependency scope fixes, external-test gating, bug fixes, new discoverability helpers, Rdatasets API additions).

### 5.2 Add/update unit tests
**Files:** `src/test/groovy/datasets/DatasetTest.groovy`, `src/test/groovy/datasets/RdatasetsTest.groovy`
- Add tests for `Dataset.names()`, `Dataset.mapNames()`, `Dataset.load(name)` for known and unknown names.
- Add test for `Dataset.mapRegions()`.
- Add test for improved `mapData()` error message (valid names included).
- Add test for `mapData()` with whitespace in name.
- Add tests for `Rdatasets.fetchData(String)` single-arg overload. Tag the **success-path** test as `@Tag('external')` because it calls `overview()` and fetches a remote CSV. Validation-only cases (null, blank, missing slash, multiple slashes, blank parts) can remain untagged.
- Add tests for `Rdatasets.search(String)`. Tag these as `@Tag('external')` because they require `overview()` which triggers network I/O.
- Add test for `Rdatasets.refresh()`. Tag as `@Tag('external')` because it triggers a network download.
- Add test for `FileUtil.getResourcePath('missing')` asserting `FileNotFoundException`.
- Ensure all existing tests still pass.

### 5.3 Run full test suite
```bash
./gradlew :matrix-datasets:test
```

### 5.4 Run external tests explicitly
```bash
RUN_EXTERNAL_TESTS=true ./gradlew :matrix-datasets:test
```

### 5.5 Run Spotless formatting
```bash
./gradlew spotlessApply
```

---

## Task Checklist (mirrors roadmap)

| # | Task | Status |
|---|------|--------|
| 1 | Rdatasets lazy loading + `refresh()` | [ ] |
| 2 | `build.gradle` dependency scopes (`api`, `implementation`) | [x] Decided: no scope changes; all remain `compileOnly` |
| 3 | Gate external tests behind `RUN_EXTERNAL_TESTS` via `@Tag('external')` + parsed boolean | [ ] |
| 4 | Remove phantom `Id: Integer` from `iris()` / `descIris()` | [x] Done during Codenarc cleanup |
| 5 | `FileUtil.getResourcePath()` null guard + test | [x] Null guard done during Codenarc cleanup; test still needed |
| 6 | Add `depth` / `table` to `descDiamonds()` | [x] Done during Codenarc cleanup |
| 7 | Fix any docs mentioning Groovy `5.0.5` → `5.0.6` | [ ] |
| 8 | Remove explicit `return` from `mpg()`, `diamonds()` | [x] Done during Codenarc cleanup |
| 9 | Remove explicit `return` from all `descXxx()` | [x] Done during Codenarc cleanup |
| 10 | Refactor `describe(String)` to per-entry `Supplier<String>` map dispatch | [x] Done during Codenarc cleanup |
| 11 | Add `Dataset.names()`, `mapNames()`, `load(String)` via `DATASET_LOADERS` | [ ] |
| 12 | Improve `mapData()` errors, add `mapRegions()`, trim input, `Locale.ROOT` | [ ] |
| 13 | Add `Rdatasets.fetchData(String)` with validation, `search(String)`; defer type inference | [ ] |
| — | Update `release.md` with v2.2.0 changes | [ ] |
| — | Fix all Codenarc violations + remove `ignoreFailures` | [x] Done |
