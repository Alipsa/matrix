# Idiomatic Groovy Refactor Plan (matrix-charts + targeted matrix-stats)

Goals
- Move numeric-heavy chart code away from Java-style primitives and `Math.*` into idiomatic, @CompileStatic-friendly Groovy (BigDecimal-first where reasonable).
- Standardize numeric coercion, domain/range typing, and NA handling across scales so transforms are consistent and readable.
- Update only matrix-stats methods used by matrix-charts (leave the rest untouched) while preserving behavior and performance boundaries.

## Primary targets in matrix-charts
- Numeric scale implementations: `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleContinuous.groovy`, `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleXLog10.groovy`, `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleXSqrt.groovy`, `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleXReverse.groovy`, `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleXDate.groovy`.
- Size/alpha scales: `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleSizeContinuous.groovy`, `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleSizeBinned.groovy`, `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleSizeArea.groovy`, `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleSizeDiscrete.groovy`, `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleAlphaContinuous.groovy`, `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleAlphaBinned.groovy`, `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleAlphaDiscrete.groovy`.
- Shared scale helpers: `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleUtils.groovy`, `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/Scale.groovy`.
- Non-scale math-heavy spots to convert to Groovy number methods where safe: `matrix-charts/src/main/groovy/se/alipsa/matrix/charts/jfx/JfXScatterChartConverter.groovy`, `matrix-charts/src/main/groovy/se/alipsa/matrix/charts/util/ColorUtil.groovy`, `matrix-charts/src/main/groovy/se/alipsa/matrix/chartexport/SvgPanel.groovy`.

## Targeted matrix-stats methods used by matrix-charts
- Regression: `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/regression/LinearRegression.groovy`, `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/regression/PolynomialRegression.groovy`, `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/regression/RegressionUtils.groovy`.
- Distributions/KDE: `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/distribution/TDistribution.groovy`, `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/kde/KernelDensity.groovy` (and any helpers it uses, e.g. bandwidth selection, kernels).

## Plan
1. Inventory and boundary decisions
1.1 Audit numeric conversions in the above files and classify data-space vs render-space math. Data-space should prefer BigDecimal; render-space (pixels, JavaFX, SVG) can stay double with explicit conversions at boundaries.
1.2 Identify API expectations in charts (e.g., `transform` return types used as `double` in tests) so return types can be adjusted without breaking callers.

2. Unify numeric coercion and NA handling
2.1 [x] Update `ScaleUtils.coerceToNumber` in `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleUtils.groovy` to return `BigDecimal` (or `null`), treating `NaN`, `null`, and blank values consistently.
2.2 [x] Remove duplicate `coerceToNumber` implementations in `ScaleContinuous`, `ScaleXLog10`, `ScaleXSqrt`, `ScaleXReverse` and route all conversions through `ScaleUtils`.
2.3 [x] Standardize `naValue` in scales to `BigDecimal` (typed nullable) and document the contract in each scale class GroovyDoc.

3. Convert scale domains/ranges to BigDecimal-first
3.1 Update numeric scale properties to typed `List<BigDecimal>` (computed domain, range, limits) in `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleContinuous.groovy` and derived scales that override those fields.
3.2 Use destructuring (`def (dMin, dMax) = computedDomain`, `def (rMin, rMax) = range`) and BigDecimal arithmetic (`**`, `.abs()`, `.min()`, `.max()`, `.round()`/`.setScale()` as needed).
3.3 Replace `Math.min/Math.max/Math.floor/Math.ceil/Math.round` with Groovy number methods when operating on `BigDecimal` or `Number` (e.g., `normalized = normalized.max(0G).min(1G)`, `num.floor()`), and retain `Math` only for trig/log operations where there is no Groovy equivalent.

4. Refactor transform/inverse implementations to idiomatic Groovy
4.1 Rewrite `transform`/`inverse` methods in all affected scales to follow the BigDecimal approach in the example, returning `BigDecimal` where feasible and only converting to `double` for downstream rendering needs.
4.2 Consolidate repeated edge-case handling (zero-range, empty domain) into small private helpers in the scale base class or `ScaleUtils`.

5. Update break generation and formatting
5.1 In `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleContinuous.groovy`, decide which pieces stay in double (nice number algorithm, log operations) and explicitly convert to/from BigDecimal at the boundary to keep API consistent.
5.2 For log/sqrt scales (`ScaleXLog10`, `ScaleXSqrt`), keep log/exp operations in double but return BigDecimal for final mapped values; document any precision caveats.

6. Targeted matrix-stats adjustments
6.1 In `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/regression/LinearRegression.groovy`, replace `Math.sqrt` on BigDecimal-derived values with BigDecimal-aware equivalents (e.g., `BigDecimal.sqrt(MathContext)` or GDK helpers), and use Groovy collection ops (`collect`, `withIndex`) for readability.
6.2 In `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/regression/PolynomialRegression.groovy`, avoid `double` temporaries where not required, and return BigDecimal predictions without passing through `double` unless demanded by the underlying fitter.
6.3 In `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/distribution/TDistribution.groovy` and `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/kde/KernelDensity.groovy`, keep double math where numerical algorithms require it, but swap `Math.abs`/`Math.sqrt`/`Math.pow` to Groovy number methods when operating on Groovy `Number` or `BigDecimal` inputs.

7. Tests and compatibility
7.1 Update affected tests in `matrix-charts/src/test/groovy/gg` to accept BigDecimal return types (use `as double` only where needed, or compare BigDecimal directly for exactness).
7.2 Add/adjust tests for `ScaleUtils.coerceToNumber` to validate BigDecimal parsing and NA handling.
7.3 Ensure any matrix-stats changes still satisfy matrix-charts usage paths (kernel density, regression stats in `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/stat/GgStat.groovy`).

8. Validation
8.1 Run targeted tests: `./gradlew :matrix-charts:test` and, if matrix-stats behavior changes, `./gradlew :matrix-stats:test`.
8.2 Manually verify a small set of scales with known expected values (continuous, log10, sqrt, size-binned) to ensure precision and NA behavior match the current semantics.

## Notes and constraints
- Keep `@CompileStatic` on all touched classes; avoid dynamic `def` where it harms type safety.
- Use BigDecimal for data-space computations but allow explicit `toDouble()` when crossing into rendering APIs (JavaFX, SVG) that require primitives.
- Avoid sweeping refactors outside the listed files; focus on readability and idiomatic Groovy without altering the public API surface more than necessary.
