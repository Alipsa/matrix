# Matrix-groovy-ext release history

## v0.4.0-SNAPSHOT 2026-09-06
- Improved trigonometric accuracy by retaining DECIMAL128 guard precision in derived functions and rounding only their public results.
- Made trigonometric range reduction calculate π at a precision derived from the angle magnitude, reject requests above 512 digits, and cache eligible values for reuse.
- Documented the approximate supported angle boundary (through `1E+469`) and added a regression test for the accepted/rejected transition.
- Preserved exact `double` round-tripping for π/2 inverse-trigonometric special cases and improved `acos()` accuracy near ±1 by avoiding subtractive cancellation.
- Improved `exp()` accuracy for large exponents by using the higher-precision internal e constant.
- Improved `hypot()` last-place accuracy by calculating with DECIMAL128 guard precision and rounding every result to DECIMAL64, including zero-side short circuits. Zero-side inputs with more than 16 significant digits can therefore have different precision, scale, and `toString()` output; two zero sides now return canonical `0`.
- Improved `toDegrees()` and `toRadians()` accuracy by using the internal high-precision π and returning DECIMAL64-rounded results. Input-dependent noise digits are removed and zero is canonicalized, so precision, scale, and `toString()` output can change.
- Improved `cbrt()` accuracy and performance by refining at DECIMAL128 guard precision for three iterations and rounding once to DECIMAL64. Exact roots remain numerically equal, but representations can change: `27.0G.cbrt()` from `3` to `3.0`, `0.000001G.cbrt()` from `0.01` to `0.010`, and `1E+300G.cbrt()` from a 101-digit plain integer to `1.000000000000000E+100`.
- **Breaking:** Removed the public `min(Number, BigDecimal)` and `max(Number, BigDecimal)` signatures to eliminate ambiguous dynamic static invocation. Extension syntax remains source-compatible through the `Number, Number` overloads, but already-compiled direct callers must be recompiled.
- **Breaking:** Changed `Double.ulp()` and `Float.ulp()` from decimal-scale semantics to their IEEE 754 ULP and collapsed `ulp()` to a single public `ulp(Number)` overload that dispatches by runtime type. The `ulp(BigDecimal)`, `ulp(Double)` and `ulp(Float)` signatures are gone; direct static invocation with an integral argument now returns the decimal ULP instead of incorrectly selecting the float ULP. Already-compiled direct callers must be recompiled.
- **Breaking:** `sqrt()`, `asin()` and `acos()` now throw `IllegalArgumentException` for mathematical domain violations, matching `log()`, `log10()` and `log1p()`. Callers catching `ArithmeticException` for these domain violations must be updated. `exp()`'s exponent guard and the trigonometric range-reduction guard still throw `ArithmeticException`, since those signal implementation limits rather than domain violations.
- **Breaking:** Non-finite `Double` and `Float` inputs now raise `IllegalArgumentException` with a message identifying the operation and input instead of leaking a JDK `NumberFormatException`.
- Updated dependency examples and extension metadata for `0.4.0-SNAPSHOT`.

## v0.3.0 2026-06-28
- Added `cbrt()` — cube root with DECIMAL64 precision using Newton-Raphson refinement seeded from `Math.cbrt`. Supports negative values and BigDecimal values far outside double range (both `BigDecimal` and `Number` overloads).
- Added `hypot(Number)` — scaled `sqrt(x² + y²)` that avoids overflow/underflow for extreme values (both `BigDecimal` and `Number` overloads).
- Added `acos()` — arccosine via the identity `acos(x) = π/2 − asin(x)`, with exact returns for `x ∈ {−1, 0, 1}` and an `ArithmeticException` guard for values outside `[−1, 1]` (both `BigDecimal` and `Number` overloads).

## v0.2.0 2026-03-23
- Improved `sin()` and `cos()` accuracy for large angles.
- Added missing `Number` overloads for `floor()`, `ceil()`, `sin()`, `cos()`, `toDegrees()`, and `toRadians()`, so these extensions work more consistently with `Integer`, `Long`, and `Double`.
- Made `atan2(0, 0)` return `0` to match `Math.atan2`.
- Updated docs and module metadata for the `0.2.0` release.

## v0.1.0 2026-01-30
Initial version
- Number extensions allowing for more idiomatic groovy code.
