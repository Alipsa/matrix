# Matrix-groovy-ext release history

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
