# Test Suite Improvement Plan — matrix-charts

## 1. Findings

### 1.1 Current State

| Category                               | Files   | Tests      | Render calls |
|----------------------------------------|---------|------------|--------------|
| `gg/scale/`                            | 30      | 549        | 89           |
| `gg/geom/`                             | 25      | 337        | ~150         |
| `gg/` (core: GgPlotTest, FacetTest, …) | 43      | 756        | ~250         |
| `charm/`                               | 42      | 398        | ~75          |
| `chart/`                               | 7       | 51         | ~25          |
| `export/`                              | 5       | 31         | ~15          |
| **Total**                              | **152** | **~2,115** | **~581**     |

`chart.render()` is invoked **581 times** across 80 test files. Each call traverses the full pipeline (stat engine → scale engine → coord engine → geom renderer → SVG generation). At ~100–300 ms per render this accounts for the majority of total test duration.

### 1.2 Root Causes

**A — Structural duplication (same logic, different parameters)**

`ScaleXBinnedTest` and `ScaleYBinnedTest` each contain **36 tests** that are near-identical: every method name, assertion, and data fixture is the same; the only variation is the aesthetic axis name (`'x'` vs `'y'`) and an axis-orientation label in one assertion. This is the clearest case of pure structural duplication: 36 redundant tests.

The six identity-scale test files (`ScaleColorIdentityTest`, `ScaleAlphaIdentityTest`, `ScaleFillIdentityTest`, `ScaleSizeIdentityTest`, `ScaleShapeIdentityTest`, `ScaleLinetypeIdentityTest`) share an identical test template:

```
testBasicTransform, testNullValue, testCustomNaValue, testName,
testWithChart, testFactoryMethod, testFactoryMethodWithParams, testAesthetic
```

Only the type-specific edge cases differ (colour normalisation for Color, alpha clamping for Alpha, etc.). Combined test count is **~80 tests**. The shared template accounts for ~50 of those.

**B — `testWithChart()` smoke tests embedded in unit test files**

Every scale test file includes one or more tests that build a full chart, call `chart.render()`, and assert only `assertTrue(svgXml.contains('<svg'))` or `assertNotNull(svg)`. GgPlotTest already covers end-to-end rendering of point charts with every scale aesthetic. These per-scale smoke tests (89 render calls in `gg/scale/` alone) verify no additional correctness.

**C — GgPlotTest is a mixed bag of fast API checks and full renders**

`GgPlotTest` has 60 test methods. 36 of them call `chart.render()`. The file mixes:
- API compatibility checks (alias names, parameter passing) — fast
- ggsave file-format tests — slow (render + disk I/O)
- Coordinate transform integration — moderate (one render each)
- Feature smoke tests (hex, dotplot, density2d) — moderate

All 60 tests currently run in a single serial block with no way to skip the slow ones.

**D — No parallelism**

`build.gradle` has no `maxParallelForks` setting. All 2,115 tests run in a single JVM thread, leaving all available CPU cores idle during rendering.

**E — Layered coverage is legitimate but not opt-in**

Point rendering is exercised at three independent levels:
1. `GgPlotTest.testPoint` (gg API integration)
2. `P0GeomRendererTest.testPointRepresentativeAndEmptyEdge` (charm render engine unit)
3. `GeomLineTest` / `GeomPhase1Test` etc. (geom object defaults + render)

These are not true duplicates — they test different abstraction layers. However, when a developer changes only a scale formula, none of the geom render tests are relevant. There is currently no way to skip them selectively.

---

## 2. Strategy

Two complementary approaches:

**Approach A — Eliminate structural duplication.** Merge tests that exercise the same code paths with different parameters into `@ParameterizedTest` variants or shared base tests. This reduces total test count and total render calls without touching coverage.

**Approach B — Introduce a fast/slow tier.** Tag render-heavy tests `@Tag("slow")` and add a `testFast` Gradle task that excludes them. The existing `test` task continues to run everything. Developers who change only scale or stat logic can run `testFast` in seconds; CI always runs the full suite.

These approaches are independent and can be applied in any order. Approach A reduces CI time permanently; Approach B gives developers a fast daily cycle immediately.

---

## 3. Implementation Tasks

### Task 1 — Enable parallel test execution (build.gradle) ✓

**File:** `matrix-charts/build.gradle`

Add inside the `test { ... }` block:

```groovy
maxParallelForks = Math.max(1, Runtime.getRuntime().availableProcessors().intdiv(2))
```

This halves expected wall-clock time with zero test changes. The full render pipeline is CPU-bound and stateless (no shared mutable state, no display required in headless mode), so tests are safe to parallelise. Verify with `./gradlew :matrix-charts:test -Pheadless=true` that no tests become flaky.

**Estimated gain:** 40–55% reduction in wall-clock time at no risk.

**Result:** `./gradlew :matrix-charts:test -Pheadless=true --rerun-tasks` — 2132 tests, 2132 passed, 0 failed. Wall-clock: 2m 29s (total accumulated test time: 924s, ~6× effective parallelism).

---

### Task 2 — Add @Slow meta-annotation and `testFast` Gradle task ✓

**2.1 Create annotation class**

New file: `src/test/groovy/se/alipsa/matrix/charts/test/Slow.groovy`

```groovy
package se.alipsa.matrix.charts.test

import org.junit.jupiter.api.Tag
import java.lang.annotation.*

@Target([ElementType.TYPE, ElementType.METHOD])
@Retention(RetentionPolicy.RUNTIME)
@Tag('slow')
@interface Slow {}
```

**2.2 Add `testFast` task to build.gradle**

```groovy
tasks.register('testFast', Test) {
  description = 'Runs only fast (non-render) tests. Use for quick dev-cycle feedback.'
  group = 'verification'
  testClassesDirs = sourceSets.test.output.classesDirs
  classpath = sourceSets.test.runtimeClasspath
  useJUnitPlatform {
    excludeTags 'slow'
  }
  jvmArgs "-Dheadless=${project.hasProperty('headless') ? project.headless : false}"
  maxParallelForks = Math.max(1, Runtime.getRuntime().availableProcessors().intdiv(2))
}
```

**2.3 Document in AGENTS.md**

Add to the Test Commands section:

```bash
# Fast unit tests only (no chart rendering) — use for quick dev-cycle feedback
./gradlew :matrix-charts:testFast

# Full test suite (always run before merge)
./gradlew :matrix-charts:test -Pheadless=true
```

**Scope of @Slow tagging** — apply to the following categories first:

| Category                                        | Method pattern                 | Approx. count |
|-------------------------------------------------|--------------------------------|---------------|
| `gg/GgPlotTest` — ggsave tests                  | `testGgsave*`                  | 10            |
| `gg/GgPlotTest` — full render tests             | any test with `chart.render()` | 36            |
| `gg/scale/*IdentityTest` — `testWithChart()`    | one per file × 6 files         | 6             |
| `gg/scale/ScaleXBinnedTest` — integration tests | `testBinnedScaleWith*`         | 3             |
| `gg/scale/ScaleYBinnedTest` — integration tests | `testBinnedScaleWith*`         | 3             |
| `gg/scale/ScaleIntegrationTest`                 | all 18 render tests            | 18            |
| `gg/geom/*Test` — per-test renders              | `testRender*`, `test*Chart*`   | ~80           |
| `charm/render/geom/P0GeomRendererTest`          | all                            | 12            |
| `charm/render/geom/P1GeomRendererTest`          | all                            | 4             |
| `charm/render/geom/P2GeomRendererTest`          | all                            | 2             |
| `export/*`                                      | all                            | 31            |

Everything tagged `@Slow` keeps running in CI via `./gradlew test`. The `testFast` task runs the remaining ~1,100 pure-unit tests (stat computations, scale transformations, coordinate math, object-model checks) in a few seconds.

**Result:** `./gradlew :matrix-charts:testFast -Pheadless=true --rerun-tasks` — 1748 tests, 1748 passed, 0 failed. Full suite unchanged: `./gradlew :matrix-charts:test -Pheadless=true` — 2132 tests, 2132 passed, 0 failed. `testFast` covers ~82% of tests and excludes all render-heavy and file-I/O paths.

---

### Task 3 — Merge ScaleXBinnedTest and ScaleYBinnedTest ✓

**Files:** `gg/scale/ScaleXBinnedTest.groovy` and `gg/scale/ScaleYBinnedTest.groovy`

**Replace with:** `gg/scale/ScaleBinnedTest.groovy`

Use `@ParameterizedTest` with an `aesthetic` parameter (`'x'` and `'y'`). The two files are structural mirrors: every test name, assertion, and dataset is identical; only `scale_x_binned()`/`scale_y_binned()` and axis-label strings differ.

```groovy
@ParameterizedTest
@ValueSource(strings = ['x', 'y'])
void testDefaultBins(String aesthetic) {
  // ... same body for both, parameterising the axis name
}
```

For the integration tests (`testBinnedScaleWith*`) that require full chart renders, tag them `@Slow`.

**Net change:** 72 test methods → 36 parameterised test methods (36 eliminated). Each `@ParameterizedTest` runs for both `'x'` and `'y'`, so execution count is unchanged. Full behaviour coverage retained.

**Result:** `./gradlew :matrix-charts:test -Pheadless=true --rerun-tasks` — 2132 tests, 2132 passed, 0 failed. `testFast` — 1748 tests, 1748 passed, 0 failed (integration tests correctly excluded via `@Slow`).

---

### Task 4 — Consolidate identity scale tests ✓

**Files:** six `Scale*IdentityTest.groovy` files in `gg/scale/`

**Approach:** Extract the common template into a `ScaleIdentityContractTest` parameterized base, then keep each type-specific file only for its genuinely unique edge cases.

The shared contract (runs once per aesthetic type via `@ParameterizedTest`):
- `testBasicTransform` — values pass through unchanged
- `testNullValue` — null returns the naValue default
- `testCustomNaValue` — custom naValue is honoured
- `testName` — name parameter stored correctly
- `testAesthetic` — aesthetic field matches expected string
- `testFactoryMethod` — GgPlot static factory returns correct type
- `testFactoryMethodWithParams` — factory honours name/naValue params

Genuinely type-specific tests to keep in individual files:
- `ScaleColorIdentityTest` — hex format pass-through, grey normalisation, RGB/HSL strings, clamping of invalid values
- `ScaleAlphaIdentityTest` — upper/lower bound clamping, numeric string coercion
- `ScaleSizeIdentityTest` — minimum size enforcement, negative value handling
- `ScaleShapeIdentityTest` — shape name validation
- `ScaleLinetypeIdentityTest` — linetype name validation

The `testWithChart()` tests in each file should be merged into a single `ScaleIdentityIntegrationTest` that renders one chart per aesthetic type, tagged `@Slow`.

**Implementation:** Created `ScaleIdentityContractTest` with 6 `@ParameterizedTest @MethodSource` methods (testName, testAesthetic, testNullValue, testCustomNaValue, testFactoryMethod, testFactoryMethodWithParams), each running for all 6 aesthetics (36 executions). Created `ScaleIdentityIntegrationTest` (`@Slow` at class level) with 7 render tests (one per aesthetic + British spelling alias). Updated all 6 individual files to retain only their genuinely type-specific edge-case tests.

**Result:** `./gradlew :matrix-charts:test -Pheadless=true --rerun-tasks` — 2137 tests, 2137 passed, 0 failed. `testFast` — 1753 tests, 1753 passed, 0 failed.

---

### Task 5 — Restructure GgPlotTest ✓

`GgPlotTest` is the single largest contributor to test duration (60 tests, 36 render calls, reported as "several minutes").

**5.1 Extract ggsave tests**

Move all `testGgsave*` methods (10 tests) to `GgSaveTest.groovy` (a file that already exists). Tag the file `@Slow` at the class level. This removes the file I/O tests from the main test class entirely.

**Implementation:** `GgPlotTest` no longer contains any `ggsave`/file-type-detection tests. `GgSaveTest` remains the dedicated `ggsave` test class and is tagged `@Slow` at class level.

**5.2 Split into smoke layer and full layer**

Retain in `GgPlotTest` only the tests that together give one representative render per major surface area:

| Keep in GgPlotTest (smoke) | Remove to GgPlotFullTest                                                       |
|----------------------------|--------------------------------------------------------------------------------|
| `testPoint`                | `testStatBinHex`, `testStatBinHexWithMtcars`                                   |
| `testVerticalBarPlot`      | `testStatSummaryHex*`                                                          |
| `testHistogram`            | `testGeomDotplot`                                                              |
| `testBoxPlot`              | `testGeomDensity2d`, `testGeomDensity2dFilled`                                 |
| `testScatterWithSmooth`    | `testExpressionInAes`                                                          |
| `testCoordTransLog10`      | `testExpressionWithGeomLm`, `testExpressionWithPolynomial`                     |
| `testPieChart`             | `testCoordTrans*` (6 remaining variants)                                       |
| `testGeomFunction`         | `testGeomLm`, `testGeomLmWithPolynomial`, `testDegreeParameter`                |
| `testAes`                  | `testStatEllipse*`, `testStatSummaryBin`, `testStatUnique`, `testStatFunction` |
| `testGeomBin2dAlias`       | API aliases (`testGeomDensity2dAlias` etc.)                                    |

The ~15 tests kept in `GgPlotTest` cover the full API surface for smoke purposes. The 45 moved tests go into `GgPlotFullTest.groovy` and are tagged `@Slow`.

**Net change:** `GgPlotTest` drops from 60 tests to ~15 tests. Full coverage is preserved in the slow group.

**Implementation:** Created `GgPlotFullTest.groovy` by splitting out the non-smoke and render-heavy tests from `GgPlotTest`. `GgPlotFullTest` is `@Slow` at class level. Smoke-overlap tests are retained only in `GgPlotTest` and explicitly `@Disabled('Covered by GgPlotTest smoke layer')` in `GgPlotFullTest` to avoid duplicate execution while preserving test intent.

**Result:**
- `./gradlew :matrix-charts:test -Pheadless=true --tests "gg.GgPlotTest" --tests "gg.GgPlotFullTest" --tests "gg.GgSaveTest"` — SUCCESS (77 tests, 65 passed, 0 failed, 12 skipped).
- `./gradlew test -Pheadless=true` — SUCCESS (full repository suite). `:matrix-charts:test` summary in this run: 2139 tests, 2127 passed, 0 failed, 12 skipped.

---

### Task 6 — Remove per-scale `testWithChart()` redundancy ✓

Every scale unit-test file includes 1–3 tests that render a chart and verify only `assertNotNull(svg)` or `svgXml.contains('<svg')`. `GgPlotTest` (smoke layer after Task 5) and `ScaleIntegrationTest` already cover this integration verification for each aesthetic.

**Action:** audit all 89 render calls in `gg/scale/`. Where a test asserts nothing more than "chart renders without crashing" and the same geom+scale combination is already covered by `ScaleIntegrationTest` or `GgPlotTest`, either:
  - Delete the test outright (verified by coverage analysis), or
  - Tag it `@Slow` to keep it but exclude it from `testFast`.

Priority candidates for deletion (confirmed redundant by audit):
- `ScaleAlphaIdentityTest.testWithChart` — basic point chart with alpha identity; covered by `ScaleIntegrationTest`
- `ScaleFillIdentityTest.testWithChart` — same
- `ScaleSizeIdentityTest.testWithChart` — same
- `ScaleShapeIdentityTest.testWithChart` — same
- `ScaleLinetypeIdentityTest.testWithChart` — same

These five together account for 5 render calls; after Task 4 they will be consolidated anyway.

**Implementation:** Consolidated specialized per-scale render smoke tests into a single slow integration class: `ScaleSpecializedIntegrationTest` (`@Slow` at class level). Removed redundant per-file render-only tests from:
- `ScaleColorStepsTest` (`testWithGeomPoint`, `testWithGeomTile`)
- `ScaleColorStepsNTest` (`testWithGeomPoint`, `testWithGeomTile`)
- `ScaleColorSteps2Test` (`testWithGeomTile`)
- `ScaleColorFermenterTest` (`testWithPlot`, `testWithDivergingData`)

This preserves one representative render per specialized scale family while eliminating duplicated "renders without crashing" checks from unit-focused files.

**Result:**
- `./gradlew :matrix-charts:test -Pheadless=true --tests "gg.scale.ScaleColorStepsTest" --tests "gg.scale.ScaleColorStepsNTest" --tests "gg.scale.ScaleColorSteps2Test" --tests "gg.scale.ScaleColorFermenterTest" --tests "gg.scale.ScaleSpecializedIntegrationTest"` — SUCCESS (64 tests, 64 passed, 0 failed, 0 skipped).
- `./gradlew :matrix-charts:testFast -Pheadless=true` — SUCCESS (slow specialized integration tests excluded by tag).
- `./gradlew test -Pheadless=true` — SUCCESS (full repository suite). `:matrix-charts:test` summary in this run: 2137 tests, 2125 passed, 0 failed, 12 skipped.

---

## 4. Expected Impact

| Task                             | Tests eliminated   | Render calls eliminated | Notes                              |
|----------------------------------|--------------------|-------------------------|------------------------------------|
| 1 — Parallel execution           | 0                  | 0                       | Wall-clock halved, no test changes |
| 2 — @Slow + testFast task        | 0                  | 0                       | Fast path: ~1,100 tests, seconds   |
| 3 — Merge binned tests           | 36                 | 6                       | Full logic coverage preserved      |
| 4 — Identity scale consolidation | ~45                | 6                       | Contract covered by @Parameterized |
| 5 — GgPlotTest split             | 0 (moved)          | 0 (moved)               | Smoke: 15 tests; full: in @Slow    |
| 6 — `testWithChart()` audit      | ~10 confirmed      | ~10                     | After Task 4 mostly resolved       |
| **Combined**                     | **~90 eliminated** | **~20 eliminated**      |                                    |

**After all tasks:**

- Total test count: ~2,115 → ~2,025 (confirmed duplicates removed)
- `testFast` covers ~1,100 tests — pure unit tests run in under 30 seconds with parallel execution
- `./gradlew :matrix-charts:test` still runs everything; parallel execution cuts wall-clock from "several minutes" to roughly half
- `GgPlotTest` smoke layer: ~15 tests, runs in under 20 seconds, good for fast API-surface sanity check

---

## 5. Implementation Order

The tasks are independent but this ordering minimises risk:

```
Task 1 (parallel)          — zero-risk, immediate win, verify no flakiness
  |
Task 2 (@Slow + testFast)  — annotation + Gradle task, no test logic changed
  |
Task 3 (binned merge)      — isolated to 2 files, easy to verify
  |
Task 4 (identity merge)    — isolated to 6 files + 1 new base test
  |
Task 5 (GgPlotTest split)  — highest benefit, moderate effort
  |
Task 6 (testWithChart audit) — audit-first, delete or tag after Task 4 is done
```

Run `./gradlew :matrix-charts:test -Pheadless=true` after each task before proceeding.

---

## 6. Coverage Preservation Principle

No test should be deleted unless at least one of the following is true:
1. The identical code path is exercised by another test in the same Gradle run (confirmed by method-level analysis, not file-level proximity).
2. The test asserts nothing beyond `assertNotNull` or `contains('<svg')` and the render pipeline is already exercised for that combination elsewhere.
3. It is a structural mirror of another test that covers the same inputs/outputs via parameterisation.

Tagging `@Slow` is always safe — it preserves the test in CI.
