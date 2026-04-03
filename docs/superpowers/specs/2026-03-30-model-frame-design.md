# Model Frame Evaluation — Design Spec

**Date:** 2026-03-30
**Roadmap section:** 14 (Model Frame Evaluation)
**Branch:** `stats-2.4.0-p14`
**Package:** `se.alipsa.matrix.stats.formula`

## Overview

ModelFrame evaluates a parsed formula against a Matrix dataset to produce an expanded design matrix suitable for downstream model fitting. It bridges the formula parser (section 13) and the model-fitting classes (sections 15-16).

The pipeline resolves formula variables from data, expands dot (`.`) placeholders, handles deferred dot-power expressions (`.^2`, `(. + x)^2`), encodes categorical columns as treatment contrasts, and applies row-level operations (subset, NA handling, weights, offset).

## Public API

### ModelFrame (builder + evaluator)

```groovy
@CompileStatic
final class ModelFrame {
  static ModelFrame of(String formula, Matrix data)
  static ModelFrame of(NormalizedFormula formula, Matrix data)

  ModelFrame weights(List<? extends Number> weights)
  ModelFrame weights(String columnName)
  ModelFrame offset(List<? extends Number> offset)
  ModelFrame offset(String columnName)
  ModelFrame subset(Closure<Boolean> filter)
  ModelFrame subset(List<Boolean> mask)
  ModelFrame naAction(NaAction action)
  ModelFrame environment(Map<String, List<?>> env)

  ModelFrameResult evaluate()
}
```

All builder methods return `this` for chaining. `evaluate()` is the terminal operation.

### ModelFrameResult (immutable output)

```groovy
@CompileStatic
final class ModelFrameResult {
  Matrix getData()
  List<Number> getResponse()
  String getResponseName()
  boolean getIncludeIntercept()
  List<String> getPredictorNames()
  List<Number> getWeights()
  List<Number> getOffset()
  List<Integer> getDroppedRows()
  NormalizedFormula getFormula()
}
```

- `data`: expanded design matrix containing only predictor columns (categoricals already encoded as BigDecimal indicators).
- `response`: response column values as `List<Number>`.
- `weights` / `offset`: `null` if not specified, otherwise filtered to match surviving rows.
- `droppedRows`: original row indices removed by subset and/or NA omit (empty list if none).
- `formula`: the fully resolved normalized formula (after dot expansion).

### NaAction (enum)

```groovy
@CompileStatic
enum NaAction { OMIT, FAIL }
```

Default is `OMIT`.

## Pipeline Stages

`evaluate()` runs these stages in order. All are private methods inside `ModelFrame`.

### Stage 1 — Parse (do not normalize yet)

If constructed from a `String`, parse via `Formula.parse()` to obtain a `ParsedFormula`. Do **not** normalize at this stage — normalization would reject dot-power expressions like `.^2`.

If constructed from a `NormalizedFormula`, use directly (dot-power formulas cannot reach this path since they are rejected during normalization; simple dot terms like `y ~ .` are preserved as `FormulaTerm(Dot)` and are handled in stage 2).

### Stage 2 — Dot expansion

Compute the set of "dot columns": all data columns except the response, columns explicitly named in the predictor side, the weights column (if specified by name), and the offset column (if specified by name). Uses `Matrix.columnNames()` to discover available columns.

**String path (ParsedFormula):** Walk the raw AST and replace each `Dot` node with a `Binary(+)` chain of `Variable` nodes for the dot columns. A new package-scoped method `FormulaSupport.expandDots(FormulaExpression, List<String> dotColumns)` handles this recursively, similar to the existing `replaceDots()` but building a multi-variable expression instead of a single replacement.

**NormalizedFormula path:** Walk the predictor terms. For any `FormulaTerm` whose sole factor is a `Dot` expression, replace it with one `FormulaTerm` per dot column. For interactions containing a dot (e.g., `.:x`), expand the dot factor into multiple interaction terms.

### Stage 3 — Normalize (after dot expansion)

**String path:** After dots are replaced with concrete variables, normalize the rewritten `ParsedFormula` via `FormulaSupport.normalize()`. The existing `powerTerms()` logic now works because there are no remaining dots — expressions like `(a + b + c)^2` expand normally.

**NormalizedFormula path:** The formula is already normalized; stage 2 replaced dot terms with concrete variable terms. Re-sort terms to maintain canonical ordering.

### Stage 4 — Variable resolution

Validate that every variable in the normalized formula exists in either the data's column names or the optional environment map. Resolution order: data first, then environment fallback. Collect all missing names and throw a single `IllegalArgumentException` listing them all.

### Stage 5 — Subset

Applied after variable resolution but before NA handling.

- `Closure<Boolean>`: receives a `Row` from the data, consistent with `Matrix.subset(Closure)`.
- `List<Boolean>`: must have the same size as `Matrix.rowCount()`.

Original row indices are tracked so `droppedRows` can report indices relative to the original data.

### Stage 6 — NA handling

Scan all formula-referenced columns for `null` values in the (possibly subsetted) data.

- `FAIL`: throw `IllegalArgumentException` listing which columns contain nulls.
- `OMIT` (default): drop rows containing nulls, record their original indices.

### Stage 7 — Categorical encoding (treatment contrasts)

A column is categorical if `Matrix.type(columnName)` is not assignable to `Number`.

Encoding process:
1. Collect unique values, sort with natural ordering (alphabetical for strings).
2. First value is the reference level (dropped).
3. For each remaining level, create a `BigDecimal` indicator column: `1.0` where the row matches, `0.0` otherwise.
4. Column naming: `{originalName}_{level}`.

Interactions involving categoricals (e.g., `species:weight`): expand the categorical first, then compute element-wise products. If `species` has 3 levels, `species:weight` produces 2 columns: `species_versicolor:weight` and `species_virginica:weight`.

Edge cases:
- Single unique value: zero indicator columns, log a warning.
- Boolean columns: treated as categorical (`false` = reference, `true` gets one indicator).
- Nulls: already handled by stage 6.

### Stage 8 — Build result

Extract the response column as `List<Number>`. Build a new `Matrix` with only the expanded predictor columns (BigDecimal type). Wrap in `ModelFrameResult` with all metadata.

## Weights and Offset

**Weights by column name:** extracted from data before subsetting, removed from dot-expansion candidates. After subset and NA-omit, filtered to match surviving rows. All values must be non-negative.

**Weights by list:** must align to original data row count. Filtered after subset and NA-omit.

**Offset:** same extraction and filtering semantics as weights, but no non-negativity constraint.

## Error Handling

All validation follows "fail fast with clear messages."

| Condition | Exception | Message pattern |
|---|---|---|
| Formula variable not in data or environment | `IllegalArgumentException` | `"Unknown variable(s) in formula: [x, z]. Available columns: [a, b, c]"` |
| Response column not in data | `IllegalArgumentException` | `"Response variable 'y' not found in data"` |
| Weights column not in data | `IllegalArgumentException` | `"Weights column 'w' not found in data"` |
| Weights contain negative values | `IllegalArgumentException` | `"Weights must be non-negative"` |
| Weights/offset list size mismatch | `IllegalArgumentException` | `"Weights size (10) does not match data row count (20)"` |
| Subset mask size mismatch | `IllegalArgumentException` | `"Subset mask size (10) does not match data row count (20)"` |
| `NaAction.FAIL` with nulls | `IllegalArgumentException` | `"NA values found in columns: [x, z] (na.action is FAIL)"` |
| Non-numeric response column | `IllegalArgumentException` | `"Response column 'y' must be numeric, found String"` |
| Empty data after subset + NA omit | `IllegalArgumentException` | `"No observations remaining after subset and NA removal"` |
| Null data or formula | `IllegalArgumentException` | `"data cannot be null"` / `"formula cannot be null"` |

## Testing

**Test class:** `FormulaModelFrameTest` in `matrix-stats/src/test/groovy/formula/`

### Core pipeline tests
- Simple formula `y ~ x + z` with numeric data
- Dot expansion `y ~ .`
- Dot with exclusion `y ~ . - z`
- Dot power `y ~ .^2` — main effects and pairwise interactions
- Mixed dot power `y ~ (. + x)^2`
- Intercept control `y ~ 0 + x`

### Categorical encoding tests
- String column with 3 levels — 2 indicator columns, correct reference
- Boolean column — single indicator
- Categorical-numeric interaction — product columns
- Single-level categorical — zero columns, warning

### Weights/offset/subset tests
- Weights by column name — extracted, excluded from dot expansion
- Weights by list — aligned and filtered after NA omit
- Offset by column name and list
- Subset via closure and boolean mask
- Negative weights — exception

### NA action tests
- `OMIT` with nulls — rows dropped, `droppedRows` correct
- `FAIL` with nulls — exception with column names
- No nulls — `droppedRows` empty

### Environment resolution tests
- Variable in environment but not data — resolved
- Variable in neither — exception

### Error case tests
- Missing columns, size mismatches, non-numeric response, empty result

**Test command:** `./gradlew :matrix-stats:test --tests 'formula.FormulaModelFrameTest' -g ./.gradle-user`

## Files to Create

| File | Purpose |
|---|---|
| `formula/ModelFrame.groovy` | Builder and pipeline |
| `formula/ModelFrameResult.groovy` | Immutable result |
| `formula/NaAction.groovy` | Enum |
| `formula/FormulaModelFrameTest.groovy` | Tests |

## Files to Modify

| File | Change |
|---|---|
| `formula/FormulaSupport.groovy` | Add `expandDots()` method (package-scoped, same package — no visibility change needed) |

## Package Dependency Note

`FormulaSupport` is annotated `@PackageScope` in `se.alipsa.matrix.stats.formula`. Since `ModelFrame` lives in the same package, it can call `FormulaSupport.expandDots()` directly without changing visibility. The `Matrix` class from `matrix-core` is accessed via the existing `compileOnly` dependency on `:matrix-core` in `matrix-stats/build.gradle`. The type-check method used for categorical detection is `Matrix.type(String columnName)` which returns `Class`.
