# Matrix-groovy-ext release history

## v0.2.0 2026-03-23
- Improved `sin()` and `cos()` accuracy for large angles.
- Added missing `Number` overloads for `floor()`, `ceil()`, `sin()`, `cos()`, `toDegrees()`, and `toRadians()`, so these extensions work more consistently with `Integer`, `Long`, and `Double`.
- Made `atan2(0, 0)` return `0` to match `Math.atan2`.
- Updated docs and module metadata for the `0.2.0` release.

## v0.1.0 2026-01-30
Initial version
- Number extensions allowing for more idiomatic groovy code.
