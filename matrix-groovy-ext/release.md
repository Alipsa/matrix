# Matrix-groovy-ext release history

## v0.4.0-snapshot 2026-09-06
- Improved `sin()`, `cos()`, and `atan()` accuracy by using DECIMAL128 guard arithmetic and returning DECIMAL64-rounded results consistently.
- Made trigonometric range reduction calculate π at a precision derived from the angle magnitude.
- Improved `exp()` accuracy for large exponents by using the higher-precision internal e constant.
- Removed ambiguous crossed `min()` and `max()` overloads so direct dynamic static invocation works alongside extension syntax.
- Added IEEE 754-aware `ulp()` overloads for `Double` and `Float`.
- Updated dependency examples and extension metadata for `0.4.0-snapshot`.

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
