# matrix-jupyter release history

## v0.1.0, In progress

- Add Jupyter MIME renderers for Matrix tables and optional Charm, ggplot, and pict SVG charts.
- Report renderer failures and plain-only bundles in the `text/plain` payload instead of a misleading "restart the kernel" stale-MIME diagnostic.
- Preserve `Row` and `Column` types when rendering them as tables so numeric columns align and carry their type class.
- Reject negative `maxRows`/`maxColumns` and non-positive `width`/`height` in `RenderOptions`.
- Skip renderer providers whose `supportedTypes()` is null or contains null instead of failing kernel installation.
- Use `Matrix.select` instead of the deprecated `Matrix.selectColumns`.
