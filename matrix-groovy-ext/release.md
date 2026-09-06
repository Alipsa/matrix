# Matrix-groovy-ext release history

## v0.4.0-SNAPSHOT 2026-09-06
- Improved trigonometric accuracy by retaining DECIMAL128 guard precision in derived functions and rounding only their public results.
- Made trigonometric range reduction calculate π at a precision derived from the angle magnitude, reject requests above 512 digits, and cache eligible values for reuse.
- Preserved exact `double` round-tripping for π/2 inverse-trigonometric special cases and improved `acos()` accuracy near ±1 by avoiding subtractive cancellation.
- Improved `exp()` accuracy for large exponents by using the higher-precision internal e constant.
- **Breaking:** Removed the public `min(Number, BigDecimal)` and `max(Number, BigDecimal)` signatures to eliminate ambiguous dynamic static invocation. Extension syntax remains source-compatible through the `Number, Number` overloads, but already-compiled direct callers must be recompiled.
- **Breaking:** Changed `Double.ulp()` and `Float.ulp()` from decimal-scale semantics to their IEEE 754 ULP. `ulp(Number)` now dispatches by runtime type for consistent behavior under static compilation.
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
