# Section 4 Charm Core Domain Model Implementation

This document records implementation outcomes for Section 4 in `matrix-charts/charm-plan.md`.

## 4.1-4.4 Core Model and Transform Strategy

Implemented:

- Immutable/mostly-immutable core compilation boundary via defensive-copy `Chart` model (`build()` output is isolated from mutable `PlotSpec` state).
- Typed spec model classes:
  - `AesSpec`
  - `LayerSpec`
  - `ScaleSpec`
  - `ThemeSpec`
  - `FacetSpec`
  - `CoordSpec`
  - `LabelsSpec`
  - existing `AnnotationSpec` hierarchy and `ColumnRef`/`ColumnExpr`
- Layer DSL and mapping expansion:
  - `points {}`, `line {}`, `tile {}`, `smooth {}`
  - `LayerDsl` supports layer `aes {}`/`aes(...)`, `inheritAes`, `position`, and free-form params.
- Parameterized scale transform strategy (replacing one-class-per-axis variation in Charm core):
  - `ScaleTransform` interface
  - built-ins: `log10`, `sqrt`, `reverse`, `date`, `time`
  - `CustomScaleTransform`
  - `ScaleTransforms` registry/factory
- `Scale` now stores transform strategy objects while preserving string-friendly `transform` property access.

## 4.5 Prototype Review (commit `115bd930`)

Command used:

- `git show --name-only --oneline 115bd930`

Classification:

| Removed type | Classification | Rationale |
|---|---|---|
| `ChartBuilder.groovy` | discard | Placeholder/fluent shell without rendering/spec semantics; replaced by `PlotSpec -> build() -> Chart`. |
| `CoordinateSystem.groovy` | partial reuse | Concept retained as typed `CoordSpec` + `CoordType`; old class had no behavior. |
| `Graph.groovy` | discard | Empty placeholder; replaced by typed layer model (`LayerSpec`, `Geom`, `Stat`). |
| `GridLines.groovy` | discard | Empty placeholder; grid concerns belong to theme/render stages. |
| `Legend.groovy` | partial reuse | Concept retained under theme/guide concerns; old class had no behavior. |
| `Style.groovy` | partial reuse | Concept retained as `ThemeSpec`; old class had no behavior. |
| `SubTitle.groovy` | partial reuse | Semantic retained as `LabelsSpec.subtitle`. |
| `Text.groovy` | discard | Abstract placeholder without useful behavior. |
| `Title.groovy` | partial reuse | Semantic retained as `LabelsSpec.title`. |
| `ChartBuilderTest.groovy` | discard | Non-functional scaffold; replaced with executable `charm.api` and `charm.core` tests. |

## 4.6 Prototype Review (`se.alipsa.matrix.charts.charmfx`)

Reviewed removed files:

- `CharmChartFx`
- `ChartPane`
- `LegendPane`
- `PlotPane`
- `TitlePane`
- `Position`
- `HorizontalLegendPane`
- `VerticalLegendPane`

Classification:

| Removed type | Classification | Rationale |
|---|---|---|
| `CharmChartFx` | discard | JavaFX layout container prototype; conflicts with SVG-first architecture target. |
| `ChartPane` | discard | UI layout scaffolding, not reusable for core spec model. |
| `LegendPane` + horizontal/vertical variants | partial reuse | Layout concept retained abstractly; implementation discarded due JavaFX coupling. |
| `PlotPane` | discard | JavaFX canvas binding; replaced by render-pipeline model. |
| `TitlePane` | discard | JavaFX node wrapper; label/title now modeled in `LabelsSpec`. |
| `Position` (charmfx) | partial reuse | Position concept retained as `charm.Position` enum; old type tied to JavaFX `Side/Pos`. |

Intentionally not carried forward:

- JavaFX node graph composition in Charm core.
- UI toolkit dependencies in spec/model layer.
- backend-specific pane orchestration in the canonical plotting model.

## 4.8 DRY Utility Consolidation

Moved reusable numeric coercion logic into shared Charm utility:

- Added `se.alipsa.matrix.charm.util.NumberCoercionUtil`.
- Updated `se.alipsa.matrix.gg.scale.ScaleUtils.coerceToNumber` to delegate to this shared utility.

This removes duplicate coercion behavior and keeps Charm/gg numeric coercion semantics aligned.

## 4.9, 4.11, 4.12, 4.17, 4.18 Validation/Immutability/Error Wiring

Implemented:

- Strict build-time validation with clear diagnostics for:
  - unknown column mappings
  - missing required aesthetics per geom
  - unsupported `geom/stat` combos (e.g., `SMOOTH` with non-`SMOOTH` stat)
  - invalid facet wrap/grid mixing
- Explicit compile boundary with `CharmCompilationException` for non-domain failures.
- Defensive-copy immutable chart state on `build()` output.
- Per-chart state only; no global mutable theme state introduced.

## 4.10, 4.19 Core Conformance Tests

Added:

- `matrix-charts/src/test/groovy/charm/core/CharmCoreModelTest.groovy`

Coverage includes:

- closure `=` semantics (colon syntax does not apply mappings)
- named-arg coercion rules
- `col['name']` and `col.name`
- aes inheritance behavior via `inheritAes`
- layer mapping semantics (`smooth`, `tile`)
- transform strategy mapping
- mutable spec -> immutable chart lifecycle guarantees
- validation and diagnostic messaging

## Command Log

- `git show --name-only --oneline 115bd930`
  - Result: listed removed Charm prototype files (`ChartBuilder`, `CoordinateSystem`, `Graph`, `GridLines`, `Legend`, `Style`, `SubTitle`, `Text`, `Title`, `ChartBuilderTest`).
- `git status --short`
  - Result: showed Section 4 source/test/doc changes staged in working tree.
- `./gradlew :matrix-charts:test --tests "charm.core.*" -Pheadless=true`
  - Result: SUCCESS
  - Summary: 13 tests, 13 passed, 0 failed, 0 skipped.
- `./gradlew :matrix-charts:test --tests "charm.api.*" -Pheadless=true`
  - Result: SUCCESS
  - Summary: 6 tests, 6 passed, 0 failed, 0 skipped.
- `./gradlew :matrix-charts:test -Pheadless=true`
  - Result: SUCCESS
  - Summary: 1716 tests, 1716 passed, 0 failed, 0 skipped.
