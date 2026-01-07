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
3.1 [x] Update numeric scale properties to typed `List<BigDecimal>` (computed domain, range, limits) in `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleContinuous.groovy` and derived scales that override those fields.
3.2 [x] Use BigDecimal arithmetic (`.divide()`, `.abs()`, `.min()`, `.max()`, `.setScale(), `.floor()`, `.ceil()`, `.log10()`) in scale calculations. Note: Destructuring with typed variables is not supported in `@CompileStatic` mode, so indexed access is used instead.
3.3 [x] Replace `Math.min/Math.max` with Groovy BigDecimal methods (`.min()`, `.max()`) when operating on `BigDecimal`, and retain `Math` for primitives (required in `@CompileStatic` mode) and trig/log operations. Tests passed: `./gradlew :matrix-charts:test -Pheadless=true`

4. Refactor transform/inverse implementations to idiomatic Groovy
4.1 [x] Rewrite `transform`/`inverse` methods in all affected scales to follow the BigDecimal approach in the example, returning `BigDecimal` where feasible.
4.2 [x] Consolidate repeated edge-case handling (zero-range, empty domain) into small private helpers in the scale base class or `ScaleUtils`.

**Section 4 Completed:**
- Added helper methods to `ScaleUtils`:
  - `linearTransform()`: Performs linear interpolation with zero-range domain handling
  - `linearInverse()`: Performs inverse linear interpolation with zero-range output handling
  - `linearTransformReversed()`: Reversed linear interpolation for ScaleXReverse
  - `linearInverseReversed()`: Reversed inverse linear interpolation for ScaleXReverse
  - `midpoint()`: Utility method for calculating midpoint of two BigDecimal values
  - `niceNum()`: Extracted duplicate "nice number" algorithm from ScaleContinuous and ScaleXSqrt (Wilkinson's algorithm for readable axis tick values)
- Updated all scale classes to use these helpers:
  - `ScaleContinuous`: Now uses `linearTransform()` and `linearInverse()`, calls `ScaleUtils.niceNum()` for break generation
  - `ScaleXLog10`: Uses helpers after log10 transformation (transform to log space → linearTransform), uses Groovy `**` power operator
  - `ScaleXSqrt`: Uses helpers after sqrt transformation (transform to sqrt space → linearTransform), calls `ScaleUtils.niceNum()` for break generation
  - `ScaleXReverse`: Uses reversed variants of the helpers
  - `ScaleSizeContinuous`: Uses `linearTransform()` with naValue fallback
  - `ScaleAlphaContinuous`: Uses `linearTransform()` with clamping and naValue fallback
- Added comprehensive GroovyDoc to `ScaleContinuous.transform()` and `inverse()` explaining why return type must be `Object` (API compatibility with diverse subclasses: numeric scales return BigDecimal, color scales return String, etc.)
- All tests passed: `./gradlew :matrix-charts:test -Pheadless=true`
- Transform/inverse methods are now more concise and idiomatic, with edge cases and common algorithms centralized in ScaleUtils

5. Update break generation and formatting
5.1 [x] In `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/scale/ScaleContinuous.groovy`, change double to BigDecimal and handle the downstream effects of api changes.
5.2 [x] For log/sqrt scales (`ScaleXLog10`, `ScaleXSqrt`), use BigDecimal instead of double where possible, return BigDecimal for final mapped values; document any precision caveats.

**Section 5 Completed:**
- Updated `ScaleContinuous.generateNiceBreaks()`: Changed signature from `double min, double max` to `BigDecimal min, BigDecimal max`, eliminating unnecessary conversions to/from double
- Updated `ScaleContinuous.formatNumber()`: Now accepts BigDecimal-first formatting, properly handles integer detection using `scale()` and `stripTrailingZeros()`, returns clean integer strings for whole numbers
- Updated `ScaleXLog10.formatLogNumber()`: Added comprehensive documentation explaining precision behavior, uses BigDecimal for consistent processing, documents that double precision is acceptable for log scale display
- Added class-level precision documentation to `ScaleXLog10` and `ScaleXSqrt`: Explains that transform/inverse return BigDecimal, notes where Math.log10/Math.sqrt use double precision, clarifies that this is sufficient for visual display purposes
- Changed `BREAK_TOLERANCE_RATIO` constant from `double` (0.001d) to `BigDecimal` (0.001G) for consistency with the BigDecimal migration, avoiding implicit conversions
- All break generation methods now work with BigDecimal end-to-end: `ScaleContinuous`, `ScaleXLog10`, and `ScaleXSqrt` all generate breaks as BigDecimal values
- All tests passed: `./gradlew :matrix-charts:test -Pheadless=true`

6. Targeted matrix-stats adjustments
6.1 [x] In `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/regression/LinearRegression.groovy`, replace `Math.sqrt` on BigDecimal-derived values with BigDecimal-aware equivalents (e.g., `BigDecimal.sqrt(MathContext)` or GDK helpers), and use Groovy collection ops (`collect`, `withIndex`) for readability.
6.2 [x] In `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/regression/PolynomialRegression.groovy`, avoid `double` temporaries where not required, and return BigDecimal predictions without passing through `double` unless demanded by the underlying fitter.
6.3 [x] In `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/distribution/TDistribution.groovy` and `matrix-stats/src/main/groovy/se/alipsa/matrix/stats/kde/KernelDensity.groovy`, keep double math where numerical algorithms require it, but swap `Math.abs`/`Math.sqrt`/`Math.pow` to Groovy number methods when operating on Groovy `Number` or `BigDecimal` inputs.

**Section 6 Completed:**
- **LinearRegression.groovy**:
  - Replaced `Math.sqrt(slopeVar)` with `slopeVar.sqrt(MathContext.DECIMAL128)` for BigDecimal-aware square root calculation
  - Replaced `Math.sqrt(svar/numberOfDataValues + xBar * xBar * slopeVar)` with `(svar/numberOfDataValues + xBar * xBar * slopeVar).sqrt(MathContext.DECIMAL128)`
  - Changed `def slopeVar` to explicitly typed `BigDecimal slopeVar` for clarity
  - Replaced `.each` loops with `.collect` in both `predict(List<Number>)` methods for more idiomatic Groovy
  - Added `import java.math.MathContext` for BigDecimal.sqrt() support
- **PolynomialRegression.groovy**:
  - Replaced `.each` loops with `.collect` in both `predict(List<Number>)` methods for more idiomatic Groovy
  - Note: `predict()` method must use double internally because Apache Commons Math `PolynomialCurveFitter` produces `double[]` coefficients; this is unavoidable and acceptable for polynomial fitting
- **TDistribution.groovy** and **KernelDensity.groovy**:
  - No changes needed - all Math.sqrt/Math.abs/Math.pow calls operate on double primitives, which is appropriate for numerical algorithms
  - These classes correctly use primitive types throughout for performance-critical numerical computations
- All tests passed:
  - `./gradlew :matrix-stats:test` (70 tests passed)
  - `./gradlew :matrix-charts:test -Pheadless=true` (all tests passed)
- The changes maintain full backward compatibility while improving code idiomaticity and precision where BigDecimal is already in use

7. Tests and compatibility
7.1 [x] Update affected tests in `matrix-charts/src/test/groovy/gg` to accept BigDecimal return types (compare BigDecimal directly for exactness).
7.2 [x] Add/adjust tests for `ScaleUtils.coerceToNumber` to validate BigDecimal parsing and NA handling.
7.3 [x] Ensure any matrix-stats changes still satisfy matrix-charts usage paths (kernel density, regression stats in `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/stat/GgStat.groovy`).

**Section 7 Completed:**
- **Existing test compatibility**: Reviewed `ScaleContinuousTest.groovy` and `ScaleTransformTest.groovy` - both already compatible with BigDecimal return types because they cast results to `double` for numeric comparisons using tolerance-based assertions
- **ScaleUtils tests**: Already comprehensive from Section 4 work - `ScaleUtilsTest.groovy` has extensive tests for:
  - `coerceToNumber`: validates BigDecimal parsing, null handling, NaN/NA handling, empty strings, invalid strings
  - `linearTransform` and `linearInverse`: tests with various domain/range combinations, edge cases, round-trip verification
  - `niceNum`: tests with positive, negative, large, small values in both round and ceiling modes (added in earlier sections)
- **GgStat.groovy compatibility**: Verified matrix-stats usage in GgStat.groovy:465-469, 1115-1149:
  - `LinearRegression` and `PolynomialRegression` usage: calls `regression.predict(xi).doubleValue()` - compatible with BigDecimal return type
  - `KernelDensity` usage: calls `kde.toMatrix()`, `kde.getX()`, `kde.getDensity()` - returns primitive arrays and Matrix objects, not affected by our changes
  - All matrix-stats integrations remain fully compatible with BigDecimal changes

8. Validation
8.1 [x] Run targeted tests: `./gradlew :matrix-charts:test` and, if matrix-stats behavior changes, `./gradlew :matrix-stats:test`.
8.2 [x] Manually verify a small set of scales with known expected values (continuous, log10, sqrt, size-binned) to ensure precision and NA behavior match the current semantics.

**Section 8 Completed:**
- **All tests passed**:
  - matrix-stats: 70/70 tests passed ✓
  - matrix-charts: All tests passed (including transform tests, scale tests, geom tests, stat tests) ✓
  - Combined run: `./gradlew :matrix-stats:test :matrix-charts:test` - all successful
- **Manual verification**: Created `ScaleBigDecimalVerificationTest.groovy` with 6 comprehensive tests:
  - `testScaleContinuousPrecision`: Verified transform/inverse return BigDecimal, correct values, NA handling
  - `testScaleLog10Precision`: Verified log10 transform/inverse return BigDecimal (or exact Integer for powers), breaks are BigDecimal
  - `testScaleSqrtPrecision`: Verified sqrt transform/inverse return BigDecimal, round-trip accuracy, breaks are BigDecimal
  - `testScaleSizeContinuousPrecision`: Verified size mapping returns BigDecimal with correct range values
  - `testBreakGenerationWithBigDecimal`: Verified all generated breaks are BigDecimal, labels are properly formatted Strings
  - `testNAValueHandling`: Verified null, "NA", "NaN", and Double.NaN all correctly return null
- **Precision verified**: All scale transformations maintain BigDecimal precision for data-space calculations while allowing double conversion at rendering boundaries
- **Behavior preserved**: NA handling, break generation, label formatting all work identically to pre-refactor behavior

## Notes and constraints
- Keep `@CompileStatic` on all touched classes; avoid dynamic `def` where it harms type safety.
- Use BigDecimal for data-space computations but allow explicit `toDouble()` when crossing into rendering APIs (JavaFX, SVG) that require primitives.
- Avoid sweeping refactors outside the listed files; focus on readability and idiomatic Groovy without altering the public API surface more than necessary.
